package org.red5.server.stream;

import static org.bytedeco.javacpp.avformat.AVFMT_GLOBALHEADER;
import static org.bytedeco.javacpp.avformat.AVFMT_NOFILE;
import static org.bytedeco.javacpp.avformat.av_read_frame;
import static org.bytedeco.javacpp.avformat.av_write_frame;
import static org.bytedeco.javacpp.avformat.av_write_trailer;
import static org.bytedeco.javacpp.avformat.avformat_alloc_output_context2;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_free_context;
import static org.bytedeco.javacpp.avformat.avformat_new_stream;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avformat.avio_alloc_context;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.avcodec.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.tika.utils.ExceptionUtils;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avformat.Read_packet_Pointer_BytePointer_int;
import org.bytedeco.javacpp.avformat.Write_packet_Pointer_BytePointer_int;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.red5.io.flv.meta.MetaData;
import org.red5.server.BaseConnection;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.net.rtmp.RTMPUtils;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.BaseEvent;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.ClientBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.muxer.MuxAdaptor.InputContext;

public class RemoteBroadcastStream extends ClientBroadcastStream implements IScheduledJob {

	public String url;
	//protected AVIOContext avio_alloc_context;
	protected AVFormatContext inputFormatContext;

	protected static Logger logger = LoggerFactory.getLogger(RemoteBroadcastStream.class);
	protected static final int BUFFER_SIZE = 4096;
	protected static Map<Pointer, RemoteBroadcastStream> queueReferences = new ConcurrentHashMap<>();
	protected ConcurrentLinkedQueue<byte[]> inputQueue = new ConcurrentLinkedQueue<>();
	private ISchedulingService scheduler;
	protected String packetFeederJobName;
	protected AVPacket pkt = avcodec.av_packet_alloc();
	protected AtomicBoolean isPipeReaderJobRunning = new AtomicBoolean(false);
	private String remoteStreamUrl;
	private AVFormatContext outputFormatContext;
	private AVIOContext avio;

	public static final int TAG_HEADER_LENGTH = 11;
	public static final int PREVIOUS_TAG_DATA_SIZE_LENGTH = 4;


	public boolean prepare() throws Exception 
	{

		inputFormatContext = avformat.avformat_alloc_context();
		if (inputFormatContext == null) {
			logger.info("cannot allocate input context {}", remoteStreamUrl);
			return false;
		}

		logger.info("input format context: {}" , inputFormatContext);

		int ret;
		if ((ret = avformat_open_input(inputFormatContext, remoteStreamUrl, avformat.av_find_input_format("flv"), (AVDictionary)null)) < 0) {
			if (logger.isWarnEnabled()) {
				byte[] data = new byte[2048];
				av_strerror(ret, data, data.length);
				logger.warn("cannot open input context: {}  error: {}", remoteStreamUrl, new String(data, 0, data.length));
			}

			return false;
		}

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary)null);
		if (ret < 0) {
			logger.info("Could not find stream information {}", remoteStreamUrl);
			return false;
		}

		AVFormatContext context = getOutputFormatContext();

		for (int i=0; i < inputFormatContext.nb_streams(); i++) {
			AVStream inStream = inputFormatContext.streams(i);

			AVStream outStream = avformat_new_stream(context, inStream.codec().codec());

			ret = avcodec_parameters_copy(outStream.codecpar(), inStream.codecpar());
			if (ret < 0) {
				logger.info("Cannot get codec parameters {}", remoteStreamUrl);
				return false;
			}

			outStream.codec().codec_tag(0);

			if ((context.oformat().flags() & AVFMT_GLOBALHEADER) != 0)
				outStream.codec().flags( outStream.codec().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);

		}

		avformat_write_header(context, (AVDictionary)null);


		return true;
	}


	@Override
	public void start() {
		super.start();

		scheduler.addScheduledOnceJob(0, new IScheduledJob() {

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {
				logger.info("before prepare {}", remoteStreamUrl);
				try {
					if (prepare()) {
						logger.info("after prepare {}", remoteStreamUrl);

						packetFeederJobName = scheduler.addScheduledJob(10, RemoteBroadcastStream.this);
					}
					else {
						logger.warn("input format context cannot be created for {}", remoteStreamUrl);
						close();
					}
				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		});

	}

	/**
	 * read 3 byte length
	 * @param in
	 * @return
	 */
	public static int getSize(byte a, byte b, byte c) {
		int val = 0;
		val += (a & 0xff) << 16;
		val += (b & 0xff) << 8;
		val += (c & 0xff);
		return val;
	}

	public static int getTimeStamp(byte a, byte b, byte c, byte d) {
		int val = getSize(a, b, c);
		val += (d & 0xff) << 24;
		return val;

	}

	/*
	 * header
	 * 1 byte data type
	 * 3 byte data size
	 * 4 byte timestamp
	 * 3 byte stream id
	 * 
	 * data
	 * 
	 * 4 byte trailer
	 */



	int packetDataType;
	int packetDataSize;
	int packetTimestamp;
	IoBuffer buffer = IoBuffer.allocate(BUFFER_SIZE);
	byte[] tempData = new byte[BUFFER_SIZE];
	boolean headerWritten = false;


	public static class WriteCallback extends Write_packet_Pointer_BytePointer_int {

		int offset = 0;

		/**
		 * buf is the flv packet 
		 */
		@Override 
		public int call(Pointer opaque, BytePointer buf, int bufSize) {
			int size = 0;
			try {

				offset = 0;

				RemoteBroadcastStream os = queueReferences.get(opaque);
				buf.get(os.tempData, 0, os.tempData.length);
				if (!os.headerWritten) {
					os.headerWritten = true;
					offset = 13;
					//first 13 byte are flv header
				}
				int dataPosition = offset;
				do {
					dataPosition = sendData(os.tempData, dataPosition, bufSize, os);
				} while (dataPosition != 0 && dataPosition < bufSize);

				return bufSize;
			}
			catch (Exception t) {
				logger.error("Error on OutputStream.write(): {} size {}" , t , size);
				logger.error(ExceptionUtils.getStackTrace(t));
				return -1;
			}
		}
	}

	static WriteCallback writeCallback;
	private static boolean exceptionExist = false;

	/**
	 * 
	 * @param data
	 * @param offset
	 * @param datalimit
	 * @param rbs
	 * @return the new position in data
	 */
	public static int sendData(byte[] data, int offset, int datalimit, RemoteBroadcastStream rbs) {

		/**
		 * FLV Packet is like this
		 * 11 byte header
		 * DATA
		 * 4 byte trailer
		 * 
		 * we need to get only DATA.
		 * The Challenge in here is that a video or audio packet must be bigger than buffer size
		 * in order to have complete packet, extra chekcs should be done as below 
		 */

		try {
			logger.trace("buffer position {} offset {} datalimit {}", rbs.buffer.position(), offset, datalimit);
			int bufferPosition = rbs.buffer.position();
			int remainingBytes = (datalimit - offset);
			if (datalimit <= 4) {
				return datalimit;
			}
			if (remainingBytes <= 4) {
				return datalimit;
			}
			
			if (bufferPosition == 0) {
				
				// a new packet starts

				rbs.packetDataType = data[offset];
				rbs.packetDataSize = getSize(data[offset + 1], data[offset + 2], data[offset + 3]);

				//only store data, not header or trailer
				if (rbs.buffer.capacity() < rbs.packetDataSize) {
					rbs.buffer = IoBuffer.allocate(rbs.packetDataSize);
				}

				rbs.packetTimestamp = getTimeStamp(data[offset + 4], data[offset + 5], data[offset + 6], data[offset + 7]); // 4 byte

				//total length is packet data size + 11 packet header + 4 byte trailer
				int totalPacketLength = rbs.packetDataSize + TAG_HEADER_LENGTH + PREVIOUS_TAG_DATA_SIZE_LENGTH; 

				if (datalimit >= (totalPacketLength + offset - 4)) {
					//if total data limit is bigger than length + offset
					//it means that packet is complete


					if (rbs.packetDataType == 9) {
						rbs.buffer.put(data, offset + TAG_HEADER_LENGTH, rbs.packetDataSize);
						rbs.buffer.rewind();
						rbs.buffer.limit(rbs.packetDataSize);
						VideoData videoData = new VideoData(rbs.buffer);
						videoData.setTimestamp(rbs.packetTimestamp);
						videoData.setSourceType(Constants.SOURCE_TYPE_LIVE);
						rbs.dispatchEvent(videoData);
					}
					else if (rbs.packetDataType == 8) {
						rbs.buffer.put(data, offset + TAG_HEADER_LENGTH, rbs.packetDataSize);
						rbs.buffer.rewind();
						rbs.buffer.limit(rbs.packetDataSize);
						AudioData audiodata = new AudioData(rbs.buffer);
						audiodata.setTimestamp(rbs.packetTimestamp);
						audiodata.setSourceType(Constants.SOURCE_TYPE_LIVE);
						rbs.dispatchEvent(audiodata);
					}
					else if (rbs.packetDataType == 18) {
						//discard it

					}
					rbs.buffer.clear();
					if (datalimit < (totalPacketLength + offset)) {
						return totalPacketLength + offset - PREVIOUS_TAG_DATA_SIZE_LENGTH;
					}
					//new data position
					return totalPacketLength + offset; 
				}
				else {
					//packet is not complete, just write the packet to buffer
					//it only writes data not header or trailer this is why offset+11
					//data contains header data as well so that remove header size this is why datalimit -11
					rbs.buffer.put(data, offset + TAG_HEADER_LENGTH, datalimit - offset - TAG_HEADER_LENGTH);
					return datalimit;
				}
			}
			else {
				//enters this block, if some data of the packet has already been written to buffer
				// in this block it may complete the packet and dispatch it 
				// or it write all packet to in buffer and wait another data as well

				int size = datalimit;
				int position = datalimit;

				// if buffer size is perfectly matches the size
				if ((rbs.buffer.position() + size - PREVIOUS_TAG_DATA_SIZE_LENGTH) == rbs.packetDataSize) {
					size = datalimit - 4;
				}
				else if (rbs.packetDataSize <= (rbs.buffer.position() + size - 4)) {
					size = rbs.packetDataSize - rbs.buffer.position();
					position = size;
				}

				rbs.buffer.put(data, 0, size);
				if (rbs.buffer.position() == rbs.packetDataSize) {
					rbs.buffer.rewind();
					rbs.buffer.limit(rbs.packetDataSize);
					if (rbs.packetDataType == 9) 
					{
						VideoData videoData = new VideoData(rbs.buffer);
						videoData.setTimestamp(rbs.packetTimestamp);
						videoData.setSourceType(Constants.SOURCE_TYPE_LIVE);
						rbs.dispatchEvent(videoData);
					}
					else if (rbs.packetDataType == 8) 
					{
						AudioData audiodata = new AudioData(rbs.buffer);
						audiodata.setTimestamp(rbs.packetTimestamp);
						audiodata.setSourceType(Constants.SOURCE_TYPE_LIVE);
						rbs.dispatchEvent(audiodata);
					}
					rbs.buffer.clear();
				}

				return position;
			}	
		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			exceptionExist  = true;
		}
		return -1;
	}

	private AVFormatContext getOutputFormatContext() {
		if (outputFormatContext == null) {
			outputFormatContext= avformat.avformat_alloc_context();

			if (writeCallback == null) {
				writeCallback = new WriteCallback();
			}
			queueReferences.put(outputFormatContext, this); 
			avio = avio_alloc_context(new BytePointer(av_malloc(BUFFER_SIZE)), BUFFER_SIZE, 1, outputFormatContext, null, writeCallback, null);
			outputFormatContext.pb(avio);

			outputFormatContext.oformat(av_guess_format("flv", null, null));

		}
		return outputFormatContext;
	} 

	@Override
	public void close() 
	{

		if (outputFormatContext != null) {
			av_write_trailer(outputFormatContext);

			queueReferences.remove(outputFormatContext);

			avformat_free_context(outputFormatContext);
			outputFormatContext = null;
		}

		if (avio != null) {
			if (avio.buffer() != null) {
				av_free(avio.buffer());
				avio.buffer(null);
			}
			av_free(avio);
			avio = null;
		}	

		if (inputFormatContext != null) {
			avformat_close_input(inputFormatContext);
			inputFormatContext = null;
		}

		/*
		 * Remote Broacdast Stream does not have a real local connection so that
		 * It cannot send unpublished notification to the cluster and this is expected because
		 * only origin should send published and unpublished notification to the cluster
		 */
		super.close();
	}

	@Override
	public void execute(ISchedulingService service) throws CloneNotSupportedException {


		if (isPipeReaderJobRunning.compareAndSet(false, true)) 
		{
			while (true) {
				if (inputFormatContext == null) {
					break;
				}
				int ret = av_read_frame(inputFormatContext, pkt);
				//

				if (ret >= 0) {
					AVStream stream = inputFormatContext.streams(pkt.stream_index());

					AVStream outStream = outputFormatContext.streams(pkt.stream_index());

					pkt.pts(av_rescale_q_rnd(pkt.pts(), stream.time_base(), outStream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
					pkt.dts(av_rescale_q_rnd(pkt.dts(), stream.time_base(), outStream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
					pkt.duration(av_rescale_q(pkt.duration(),  stream.time_base(), outStream.time_base()));
					pkt.pos(-1);

					ret = av_write_frame(outputFormatContext, pkt);
					if (ret < 0 && logger.isWarnEnabled()) {
						byte[] data = new byte[1024];
						av_strerror(ret, data, data.length);
						logger.warn("Cannot write frame error: {}", new String(data, 0, data.length));
					}

					av_packet_unref(pkt);
				}
				else {
					logger.info("removing scheduled job {} and closing {}" , packetFeederJobName, remoteStreamUrl);
					scheduler.removeScheduledJob(packetFeederJobName);
					close();
				}

				//if there is not element in the qeueue,
				//break the loop
				if (inputQueue.peek() == null || inputFormatContext == null) 
				{
					break;
				}
				//break;
			}

			isPipeReaderJobRunning.compareAndSet(true, false);
		}

	}

	public int getReferenceCountInQueue() {
		return queueReferences.size();
	}


	public String getRemoteStreamUrl() {
		return remoteStreamUrl;
	}


	public void setRemoteStreamUrl(String remoteStreamUrl) {
		this.remoteStreamUrl = remoteStreamUrl;
	}


	public ISchedulingService getScheduler() {
		return scheduler;
	}


	public void setScheduler(ISchedulingService scheduler) {
		this.scheduler = scheduler;
	}


	public static boolean isExceptionExist() {
		return exceptionExist;
	}


	public static void setExceptionExist(boolean exceptionExist) {
		RemoteBroadcastStream.exceptionExist = exceptionExist;
	}


}
