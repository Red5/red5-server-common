package com.antstreaming.muxer;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_FLAG_GLOBAL_HEADER;
import static org.bytedeco.javacpp.avcodec.av_packet_unref;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_from_context;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_to_context;
import static org.bytedeco.javacpp.avformat.AVFMT_GLOBALHEADER;
import static org.bytedeco.javacpp.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.javacpp.avformat.av_read_frame;
import static org.bytedeco.javacpp.avformat.av_write_frame;
import static org.bytedeco.javacpp.avformat.avformat_alloc_output_context2;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_new_stream;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avformat.avformat_write_header;
import static org.bytedeco.javacpp.avformat.avio_alloc_context;
import static org.bytedeco.javacpp.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.javacpp.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.javacpp.avutil.av_dict_set;
import static org.bytedeco.javacpp.avutil.av_rescale_q;
import static org.bytedeco.javacpp.avutil.av_rescale_q_rnd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avformat.Read_packet_Pointer_BytePointer_int;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.IConnection;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.CachedEvent;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.stream.IRecordingListener;
import org.red5.server.stream.consumer.FileConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuxAdaptor implements IRecordingListener {

	private final static byte[] DEFAULT_STREAM_ID = new byte[] { (byte) (0 & 0xff), (byte) (0 & 0xff), (byte) (0 & 0xff) };
	private final static int HEADER_LENGTH = 9;
	private final static int TAG_HEADER_LENGTH = 11;

	protected QuartzSchedulingService scheduler;

	protected static Logger logger = LoggerFactory.getLogger(MuxAdaptor.class);
	protected ConcurrentLinkedQueue<CachedEvent> inputQueue = new ConcurrentLinkedQueue<>();

	protected AtomicBoolean isPipeReaderJobRunning = new AtomicBoolean(false);
	String packetFeederJobName;
	protected AVFormatContext inputFormatContext;

	protected boolean stopRequestExist = false;

	protected ArrayList<AbstractMuxer> muxerList = new ArrayList();
	protected static final int BUFFER_SIZE = 1024 * 200;
	private ReadCallback readCallback;
	private PacketFeederJob packetFeederJob;
	private boolean isRecording = false;

	public MuxAdaptor() {
		
	}
	
	
	public void addMuxer(AbstractMuxer muxer) {
		muxerList.add(muxer);
	}


	@Override
	public boolean init(IConnection conn, String name, boolean isAppend) {
		return init(conn.getScope(), name, isAppend);
	}


	@Override
	public boolean init(IScope scope, String name, boolean isAppend) {
		scheduler = (QuartzSchedulingService) scope.getParent().getContext().getBean(QuartzSchedulingService.BEAN_NAME);
		if (scheduler == null) {
			logger.warn("scheduler is not available in beans");
			return false;
		}
		packetFeederJob = new PacketFeederJob();
		for(AbstractMuxer muxer : muxerList) {
			muxer.init(scope, name);
		}
		return true;
	}


	class ReadCallback extends Read_packet_Pointer_BytePointer_int {
		private AtomicBoolean isRunning = new AtomicBoolean(false);

		boolean firstCall = true;

		@Override public synchronized int call(Pointer opaque, BytePointer buf, int buf_size) {
			int length = -1;
			try {
				if (!firstCall) {
					IStreamPacket packet = null;
					//System.out.println("queue size:" + inputQueue.size());

					while ((packet = inputQueue.poll()) == null) {
						if (stopRequestExist) {
							logger.info("stop request");
							break;
						}
						Thread.sleep(50);
					}

					if (packet != null) {
						//make sure buf size is larger than below data length

						byte[] data = getFLVFrame(packet);
						length = data.length;
						buf.put(data, 0, length);
					}
				}
				else {
					firstCall = false;
					logger.info("writing header...");
					byte[] flvHeader = getFLVHeader();
					length = flvHeader.length;

					buf.put(flvHeader, 0, length);
				}

			} catch (Exception e) {
				logger.error("Exception handling queue", e);
			} finally {
				isRunning.compareAndSet(true, false);
			}

			return length;
		}

		public boolean isFirstCall() {
			return firstCall;
		}
	}


	class PacketFeederJob implements IScheduledJob {

		@Override
		public void execute(ISchedulingService service) throws CloneNotSupportedException {

			//logger.info("pipe reader job running");
			if (isPipeReaderJobRunning.compareAndSet(false, true)) 
			{
				//logger.info("pipe reader job in running");
				while (true) {
					AVPacket pkt = new AVPacket();
					int ret = av_read_frame(inputFormatContext, pkt);
					//logger.info("input read...");
					
					if (ret >= 0) {
						AVStream stream = inputFormatContext.streams(pkt.stream_index());
						for(AbstractMuxer muxer : muxerList) {
							muxer.writePacket(pkt, stream);
						}
					}
					else {
						
						for(AbstractMuxer muxer : muxerList) {
							muxer.writeTrailer();
						}
						avformat_close_input(inputFormatContext);
						scheduler.removeScheduledJob(packetFeederJobName);
						isRecording = false;
					}
					
					
					av_packet_unref(pkt);

					//if there is not element in the qeueue,
					//break the loop
					if (inputQueue.peek() == null) 
					{
						break;
					}
				}

				isPipeReaderJobRunning.compareAndSet(true, false);
			}

		}

	}; 


	public static byte[] getFLVFrame(IStreamPacket packet) throws IOException {
		/*
		 * Tag header = 11 bytes
		 * |-|---|----|---|
		 *    0 = type
		 *  1-3 = data size
		 *  4-7 = timestamp
		 * 8-10 = stream id (always 0)
		 * Tag data = variable bytes
		 * Previous tag = 4 bytes (tag header size + tag data size)
		 * 
		 * ITag tag = new Tag();
		tag.setDataType(packet.getDataType());
		tag.setBodySize(data.limit());
		tag.setTimestamp(packet.getTimestamp());
		 */
		// skip tags with no data
		int bodySize = packet.getData().limit();
		// ensure that the channel is still open
		// get the data type
		byte dataType = packet.getDataType(); //tag.getDataType();
		// if we're writing non-meta tags do seeking and tag size update

		// set a var holding the entire tag size including the previous tag length
		int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
		// resize
		// create a buffer for this tag
		ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
		// get the timestamp
		int timestamp = packet.getTimestamp(); // tag.getTimestamp();
		// allow for empty tag bodies
		byte[] bodyBuf = null;
		if (bodySize > 0) {
			// create an array big enough
			bodyBuf = new byte[bodySize];
			// put the bytes into the array
			//tag.getBody().get(bodyBuf);
			packet.getData().get(bodyBuf);
			// get the audio or video codec identifier

		}
		// Data Type
		IOUtils.writeUnsignedByte(tagBuffer, dataType); //1
		// Body Size - Length of the message. Number of bytes after StreamID to end of tag 
		// (Equal to length of the tag - 11) 
		IOUtils.writeMediumInt(tagBuffer, bodySize); //3
		// Timestamp
		IOUtils.writeExtendedMediumInt(tagBuffer, timestamp); //4
		// Stream id
		tagBuffer.put(DEFAULT_STREAM_ID); //3
		// get the body if we have one
		if (bodyBuf != null) {
			tagBuffer.put(bodyBuf);
		}
		// we add the tag size
		tagBuffer.putInt(TAG_HEADER_LENGTH + bodySize);
		// flip so we can process from the beginning
		tagBuffer.flip();
		// write the tag

		return tagBuffer.array();

	}

	public static byte[] getFLVHeader() {
		org.red5.io.flv.FLVHeader flvHeader = new org.red5.io.flv.FLVHeader();
		flvHeader.setFlagVideo(true);
		flvHeader.setFlagAudio(true);
		// create a buffer
		ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH + 4); // FLVHeader (9 bytes) + PreviousTagSize0 (4 bytes)
		flvHeader.write(header);
		return header.array();
	}


	@Override
	public void start() {
		isRecording = false;
		stopRequestExist = false;
		scheduler.addScheduledOnceJob(10, new IScheduledJob() {
			
			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {
				if (prepare()) {
					isRecording = true;
					packetFeederJobName = scheduler.addScheduledJob(100, packetFeederJob);
				}
				else {
					logger.warn("input format context cannot be created");
				}
			}
		});

	}
	
	public boolean prepare() 
	{

		inputFormatContext = avformat.avformat_alloc_context();
		if (inputFormatContext == null) {
			logger.info("cannot allocate input context");
			return false;
		}

		readCallback = new ReadCallback();

		AVIOContext avio_alloc_context = avio_alloc_context(new BytePointer(avutil.av_malloc(BUFFER_SIZE)), BUFFER_SIZE, 0, inputFormatContext, readCallback, null, null);
		inputFormatContext.pb(avio_alloc_context);


		int ret;
		if ((ret = avformat_open_input(inputFormatContext, (String)null, avformat.av_find_input_format("flv"), (AVDictionary)null)) < 0) {
			logger.info("cannot open input context");
			return false;
		}

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary)null);
		if (ret < 0) {
			logger.info("Could not find stream information\n");
			return false;
		}
		
		for(AbstractMuxer muxer : muxerList) {
			if (!muxer.prepare(inputFormatContext)) {
				logger.warn("muxer prepare returns false " + muxer.getFormat());
			}
		}
		return true;
	}


	@Override
	public void stop() {
		stopRequestExist = true;

	}


	@Override
	public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
		CachedEvent event = new CachedEvent();
		event.setData(packet.getData().duplicate());
		event.setDataType(packet.getDataType());
		event.setReceivedTime(System.currentTimeMillis());
		event.setTimestamp(packet.getTimestamp());
		inputQueue.add(event);
	}


	@Override
	public boolean isRecording() {
		return isRecording;
	}


	@Override
	public boolean isAppending() {
		return false;
	}


	@Override
	public FileConsumer getFileConsumer() {
		return null;
	}


	@Override
	public void setFileConsumer(FileConsumer recordingConsumer) {

	}


	@Override
	public String getFileName() {
		return null;
	}


	@Override
	public void setFileName(String fileName) {

	}


	public List<AbstractMuxer> getMuxerList() {
		return muxerList;
	}

}
