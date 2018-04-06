package io.antmedia.muxer;

import static org.bytedeco.javacpp.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.javacpp.avcodec.av_packet_free;
import static org.bytedeco.javacpp.avcodec.av_packet_unref;
import static org.bytedeco.javacpp.avformat.av_dump_format;
import static org.bytedeco.javacpp.avformat.av_read_frame;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avformat.avio_alloc_context;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.javacpp.avutil.AV_LOG_INFO;
import static org.bytedeco.javacpp.avutil.av_free;
import static org.bytedeco.javacpp.avutil.av_log_get_level;
import static org.bytedeco.javacpp.avutil.av_rescale_q;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avformat.Read_packet_Pointer_BytePointer_int;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVRational;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.IRecordingListener;
import org.red5.server.stream.consumer.FileConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.storage.StorageClient;

public class MuxAdaptor implements IRecordingListener, IScheduledJob {

	private final static byte[] DEFAULT_STREAM_ID = new byte[] { (byte) (0 & 0xff), (byte) (0 & 0xff),
			(byte) (0 & 0xff) };
	private final static int HEADER_LENGTH = 9;
	private final static int TAG_HEADER_LENGTH = 11;

	protected QuartzSchedulingService scheduler;

	private static Logger logger = LoggerFactory.getLogger(MuxAdaptor.class);

	protected String packetFeederJobName = null;
	protected ConcurrentLinkedQueue<byte[]> inputQueue = new ConcurrentLinkedQueue<>();

	// private ReadCallback readCallback;

	protected AtomicBoolean isPipeReaderJobRunning = new AtomicBoolean(false);
	protected AVIOContext avio_alloc_context;
	protected AVFormatContext inputFormatContext;

	protected ArrayList<Muxer> muxerList = new ArrayList<Muxer>();

	protected boolean deleteHLSFilesOnExit = true;

	public static class InputContext {
		public ConcurrentLinkedQueue<byte[]> queue;
		public volatile boolean isHeaderWritten = false;
		public volatile boolean stopRequestExist = false;

		public InputContext(ConcurrentLinkedQueue<byte[]> queue) {
			this.queue = queue;
		}
	}

	protected static Map<Pointer, InputContext> queueReferences = new ConcurrentHashMap<>();

	protected static final int BUFFER_SIZE = 4096;
	
	public static final String QUALITY_GOOD = "good";
	public static final String QUALITY_AVERAGE  = "average";
	public static final String QUALITY_POOR ="poor";
	public static final String QUALITY_NA ="NA";
	

	protected boolean isRecording = false;
	protected ClientBroadcastStream broadcastStream;
	protected boolean mp4MuxingEnabled;
	protected boolean addDateTimeToMp4FileName;
	protected boolean hlsMuxingEnabled;
	protected boolean webRTCEnabled = false;
	protected StorageClient storageClient;
	protected String hlsTime;
	protected String hlsListSize;
	protected String hlsPlayListType;
	protected AVPacket pkt = avcodec.av_packet_alloc(); // new AVPacket();

	protected boolean firstKeyFrameReceived = false;
	private String name;
	protected long startTime;
	
	private String objectDetectionModelDir = null;
	private IScope scope;
	private String oldQuality;
	private String newQuality;
	private AVRational timeBaseForMS;
	private int speedCounter=0;
	private int previewCreatePeriod;

	private static Read_packet_Pointer_BytePointer_int readCallback = new Read_packet_Pointer_BytePointer_int() {
		@Override

		public int call(Pointer opaque, BytePointer buf, int buf_size) {
			int length = -1;
			try {
				InputContext inputContext = queueReferences.get(opaque);
				if (inputContext.isHeaderWritten) {
					byte[] packet = null;

					if (inputContext.queue != null) {
						while ((packet = inputContext.queue.poll()) == null) {
							if (inputContext.stopRequestExist) {
								logger.info("stop request ");
								break;
							}
							Thread.sleep(5);
						}
					} else {
						logger.error("input queue null");
					}

					if (packet != null) {
						// ** this setting critical..
						length = packet.length;
						buf.put(packet, 0, length);
					} else // if (stopRequestExist)
					{
						logger.info("packet is null and return length is:" + length);
					}
				} else {
					inputContext.isHeaderWritten = true;
					logger.info("writing header...");
					byte[] flvHeader = getFLVHeader();
					length = flvHeader.length;

					buf.put(flvHeader, 0, length);
				}

			} catch (Exception e) {
				logger.error("Exception handling queue", e);
			}

			return length;
		}

	};

	public MuxAdaptor(ClientBroadcastStream clientBroadcastStream) {
		this.broadcastStream = clientBroadcastStream;

		timeBaseForMS = new AVRational();
		timeBaseForMS.num(1);
		timeBaseForMS.den(1000);
	}

	public void addMuxer(Muxer muxer) {
		muxerList.add(muxer);
	}

	@Override
	public boolean init(IConnection conn, String name, boolean isAppend) {

		return init(conn.getScope(), name, isAppend);
	}

	@Override
	public boolean init(IScope scope, String name, boolean isAppend) {
		this.name = name;
		scheduler = (QuartzSchedulingService) scope.getParent().getContext().getBean(QuartzSchedulingService.BEAN_NAME);
		this.scope=scope;
		if (scheduler == null) {
			logger.warn("scheduler is not available in beans");
			return false;
		}

		if (mp4MuxingEnabled) {
			Mp4Muxer mp4Muxer = new Mp4Muxer(storageClient, scheduler);
			mp4Muxer.setAddDateTimeToSourceName(addDateTimeToMp4FileName);
			addMuxer(mp4Muxer);
		}
		if (hlsMuxingEnabled) {
			HLSMuxer hlsMuxer = new HLSMuxer(scheduler, hlsListSize, hlsTime, hlsPlayListType);
			hlsMuxer.setDeleteFileOnExit(deleteHLSFilesOnExit);
			addMuxer(hlsMuxer);
		}

		for (Muxer muxer : muxerList) {
			muxer.init(scope, name, 0);
		}
		return true;
	}

	public boolean prepare() throws Exception {

		inputFormatContext = avformat.avformat_alloc_context();
		if (inputFormatContext == null) {
			logger.info("cannot allocate input context");
			return false;
		}

		// readCallback = new ReadCallback();

		avio_alloc_context = avio_alloc_context(new BytePointer(avutil.av_malloc(BUFFER_SIZE)), BUFFER_SIZE, 0,
				inputFormatContext, getReadCallback(), null, null);

		inputFormatContext.pb(avio_alloc_context);

		queueReferences.put(inputFormatContext, new InputContext(inputQueue));

		int ret;

		logger.info("before avformat_open_input.............");

		if ((ret = avformat_open_input(inputFormatContext, (String) null, avformat.av_find_input_format("flv"),
				(AVDictionary) null)) < 0) {
			logger.info("cannot open input context");
			return false;
		}

		logger.info("after avformat_open_input............. before avformat_find_stream");

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			logger.info("Could not find stream information\n");
			return false;
		}

		logger.info("after avformat_find_sream_info............. before av_log_get_level");

		if (av_log_get_level() >= AV_LOG_INFO) {
			// Dump information about file onto standard error
			av_dump_format(inputFormatContext, 0, name, 0);
		}

		Iterator<Muxer> iterator = muxerList.iterator();
		while (iterator.hasNext()) {
			Muxer muxer = (Muxer) iterator.next();
			if (!muxer.prepare(inputFormatContext)) {
				iterator.remove();
				logger.warn("muxer prepare returns false " + muxer.getFormat());
			}
		}

		if (muxerList.isEmpty()) {
			return false;
		}

		startTime = System.currentTimeMillis();
		return true;
	}

	protected Read_packet_Pointer_BytePointer_int getReadCallback() {
		return readCallback;
	}

	public void changeSourceQuality(String id, String quality) {

		if(oldQuality!=quality) {

			IContext context = MuxAdaptor.this.scope.getContext(); 
			ApplicationContext appCtx = context.getApplicationContext(); 
			Object bean = appCtx.getBean("web.handler");
			if (bean instanceof IMuxerListener) {
				((IMuxerListener)bean).sourceQualityChanged(id, quality);
			}

			oldQuality=quality;
		}
	}

	public void changeSourceSpeed(String id, double speed) {
		speedCounter++;

		if(speedCounter % 600==0) {
			IContext context = MuxAdaptor.this.scope.getContext(); 
			ApplicationContext appCtx = context.getApplicationContext(); 
			Object bean = appCtx.getBean("web.handler");
			if (bean instanceof IMuxerListener) {
				((IMuxerListener)bean).sourceSpeedChanged(id, speed);
			}
		}
	}


	@Override
	public void execute(ISchedulingService service) throws CloneNotSupportedException {

		if (isPipeReaderJobRunning.compareAndSet(false, true)) {
			// logger.info("pipe reader job in running");
			while (true) {
				if (inputFormatContext == null) {
					break;
				}
				int ret = av_read_frame(inputFormatContext, pkt);

				// logger.info("input read...");

				if (ret >= 0) {

					long currentTime = System.currentTimeMillis();
					AVStream stream = inputFormatContext.streams(pkt.stream_index());

					long packetTime = av_rescale_q(pkt.pts(), stream.time_base(), timeBaseForMS);
					long timeDiff=(currentTime-startTime)-packetTime;
					long duration =currentTime-startTime;

					double speed= (double)packetTime/duration;

					//logger.info("time difference :  "+String.valueOf((currentTime-startTime)-packetTime));

					if(timeDiff<1800) {

						changeSourceQuality(this.name, QUALITY_GOOD);

					}else if(timeDiff>1799 && timeDiff<3499 ) {

						changeSourceQuality(this.name, QUALITY_AVERAGE);
					}else {

						changeSourceQuality(this.name, QUALITY_POOR);
					}

					changeSourceSpeed(this.name, speed);


					if (!firstKeyFrameReceived && stream.codec().codec_type() == AVMEDIA_TYPE_VIDEO) {
						int keyFrame = pkt.flags() & AV_PKT_FLAG_KEY;
						if (keyFrame == 1) {
							firstKeyFrameReceived = true;
						} else {
							logger.warn("First video packet is not key frame. Dropping...");
							av_packet_unref(pkt);
							continue;
						}
					}

					for (Muxer muxer : muxerList) {
						muxer.writePacket(pkt, stream);
					}

					av_packet_unref(pkt);

				} else {
					System.out.println("removing scheduled job " + MuxAdaptor.this);
					scheduler.removeScheduledJob(packetFeederJobName);
					for (Muxer muxer : muxerList) {
						muxer.writeTrailer();
					}
					logger.info("closing input");

					queueReferences.remove(inputFormatContext);

					av_packet_free(pkt);
					avformat_close_input(inputFormatContext);

					if (avio_alloc_context != null) {
						if (avio_alloc_context.buffer() != null) {
							av_free(avio_alloc_context.buffer());
							avio_alloc_context.buffer(null);
						}
						av_free(avio_alloc_context);
						avio_alloc_context = null;
					}

					inputFormatContext = null;
					isRecording = false;

					changeSourceQuality(this.name, QUALITY_NA);
					changeSourceSpeed(this.name, 0);

				}

				// if there is not element in the qeueue,
				// break the loop
				if (inputQueue.peek() == null || inputFormatContext == null) {
					break;
				}
				// break;
			}

			isPipeReaderJobRunning.compareAndSet(true, false);
		}

	}

	public void closeResources() {

		System.out.println("removing scheduled job " + MuxAdaptor.this);
		if (packetFeederJobName != null) {
			scheduler.removeScheduledJob(packetFeederJobName);
		}
		for (Muxer muxer : muxerList) {
			muxer.writeTrailer();
		}
		logger.info("closing input");

		queueReferences.remove(inputFormatContext);

		av_packet_free(pkt);
		avformat_close_input(inputFormatContext);

		if (avio_alloc_context != null) {
			if (avio_alloc_context.buffer() != null) {
				av_free(avio_alloc_context.buffer());
				avio_alloc_context.buffer(null);
			}
			av_free(avio_alloc_context);
			avio_alloc_context = null;
		}

		inputFormatContext = null;
		isRecording = false;
	}

	public static byte[] getFLVFrame(IStreamPacket packet) throws IOException {
		/*
		 * Tag header = 11 bytes |-|---|----|---| 0 = type 1-3 = data size 4-7 =
		 * timestamp 8-10 = stream id (always 0) Tag data = variable bytes
		 * Previous tag = 4 bytes (tag header size + tag data size)
		 * 
		 * ITag tag = new Tag(); tag.setDataType(packet.getDataType());
		 * tag.setBodySize(data.limit());
		 * tag.setTimestamp(packet.getTimestamp());
		 */
		// skip tags with no data
		int bodySize = packet.getData().limit();
		// ensure that the channel is still open
		// get the data type
		byte dataType = packet.getDataType(); // tag.getDataType();
		// if we're writing non-meta tags do seeking and tag size update

		// set a var holding the entire tag size including the previous tag
		// length
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
			// tag.getBody().get(bodyBuf);
			packet.getData().position(0);
			packet.getData().get(bodyBuf);
			// get the audio or video codec identifier
			packet.getData().position(0);

		}

		// Data Type
		IOUtils.writeUnsignedByte(tagBuffer, dataType); // 1
		// Body Size - Length of the message. Number of bytes after StreamID to
		// end of tag
		// (Equal to length of the tag - 11)
		IOUtils.writeMediumInt(tagBuffer, bodySize); // 3
		// Timestamp
		IOUtils.writeExtendedMediumInt(tagBuffer, timestamp); // 4
		// Stream id
		tagBuffer.put(DEFAULT_STREAM_ID); // 3
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
		ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH + 4); // FLVHeader
		// (9 bytes)
		// +
		// PreviousTagSize0
		// (4 bytes)
		flvHeader.write(header);
		return header.array();
	}

	@Override
	public void start() {
		isRecording = false;
		scheduler.addScheduledOnceJob(0, new IScheduledJob() {

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {
				logger.info("before prepare");
				try {
					if (prepare()) {
						logger.info("after prepare");
						isRecording = true;
						startTime = System.currentTimeMillis();
						packetFeederJobName = scheduler.addScheduledJob(10, MuxAdaptor.this);
					} else {
						logger.warn("input format context cannot be created");
						if (broadcastStream != null) {
							broadcastStream.removeStreamListener(MuxAdaptor.this);
						}
						logger.warn("closing adaptor");
						closeResources();
						// stop();
						logger.warn("closed adaptor");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	@Override
	public void stop() {
		logger.info("Calling stop");
		if (inputFormatContext == null) {
			logger.warn("Mux adaptor stopped returning");
			return;
		}
		InputContext inputContext = queueReferences.get(inputFormatContext);
		if (inputContext != null) {
			inputContext.stopRequestExist = true;
		}

	}

	@Override
	public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {

		byte[] flvFrame;
		try {
			flvFrame = getFLVFrame(packet);

			if (flvFrame.length <= BUFFER_SIZE) {
				inputQueue.add(flvFrame);
			} else {
				int numberOfBytes = flvFrame.length;
				int startIndex = 0;
				int copySize = 0;
				while (numberOfBytes != 0) {
					if (numberOfBytes > BUFFER_SIZE) {
						copySize = BUFFER_SIZE;
					} else {
						copySize = numberOfBytes;
					}
					byte[] data = Arrays.copyOfRange(flvFrame, startIndex, startIndex + copySize);
					inputQueue.add(data);
					numberOfBytes -= copySize;
					startIndex += copySize;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
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

	public List<Muxer> getMuxerList() {
		return muxerList;
	}

	public void setMp4MuxingEnabled(boolean mp4Enabled, boolean addDateTimeToMp4FileName) {
		this.mp4MuxingEnabled = mp4Enabled;
		this.addDateTimeToMp4FileName = addDateTimeToMp4FileName;
	}

	public void setHLSMuxingEnabled(boolean hlsMuxingEnabled) {
		this.hlsMuxingEnabled = hlsMuxingEnabled;
	}

	public void setStorageClient(StorageClient storageClient) {
		this.storageClient = storageClient;
	}

	public void setHlsTime(String hlsTime) {
		this.hlsTime = hlsTime;
	}

	public void setHlsListSize(String hlsListSize) {
		this.hlsListSize = hlsListSize;
	}

	public void setHlsPlayListType(String hlsPlayListType) {
		this.hlsPlayListType = hlsPlayListType;
	}

	public boolean isWebRTCEnabled() {
		return webRTCEnabled;
	}

	public void setWebRTCEnabled(boolean webRTCEnabled) {
		this.webRTCEnabled = webRTCEnabled;
	}

	public void setHLSFilesDeleteOnExit(boolean deleteHLSFilesOnExit) {
		this.deleteHLSFilesOnExit = deleteHLSFilesOnExit;

	}

	public String getObjectDetectionModelDir() {
		return objectDetectionModelDir;
	}

	public void setObjectDetectionModelDir(String objectDetectionModelDir) {
		this.objectDetectionModelDir = objectDetectionModelDir;
	}

	public int getPreviewCreatePeriod() {
		return previewCreatePeriod;
	}
	
	public void setPreviewCreatePeriod(int previewCreatePeriod) {
		this.previewCreatePeriod = previewCreatePeriod;
	}

}
