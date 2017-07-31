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
	protected AVPacket pkt = avcodec.av_packet_alloc(); // new AVPacket();
	protected AtomicBoolean isPipeReaderJobRunning = new AtomicBoolean(false);
	private String remoteStreamUrl;
	private AVFormatContext outputFormatContext;
	private AVIOContext avio;



	public boolean prepare() throws Exception 
	{

		inputFormatContext = avformat.avformat_alloc_context();
		if (inputFormatContext == null) {
			logger.info("cannot allocate input context");
			return false;
		}

		logger.info("input format context: " + inputFormatContext);

		int ret;
		if ((ret = avformat_open_input(inputFormatContext, remoteStreamUrl, avformat.av_find_input_format("flv"), (AVDictionary)null)) < 0) {
			logger.info("cannot open input context");
			return false;
		}

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary)null);
		if (ret < 0) {
			logger.info("Could not find stream information\n");
			return false;
		}

		AVFormatContext context = getOutputFormatContext();

		for (int i=0; i < inputFormatContext.nb_streams(); i++) {
			AVStream in_stream = inputFormatContext.streams(i);
			{	

				AVStream out_stream = avformat_new_stream(context, in_stream.codec().codec());

				ret = avcodec_parameters_copy(out_stream.codecpar(), in_stream.codecpar());
				if (ret < 0) {
					logger.info("Cannot get codec parameters\n");
					return false;
				}

				out_stream.codec().codec_tag(0);

				if ((context.oformat().flags() & AVFMT_GLOBALHEADER) != 0)
					out_stream.codec().flags( out_stream.codec().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
			}
		}

		avformat_write_header(context, (AVDictionary)null);


		//avformat.av_dump_format(inputFormatContext, 0, "stream", 0);


		return true;
	}


	@Override
	public void start() {
		super.start();

		scheduler.addScheduledOnceJob(0, new IScheduledJob() {

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {
				logger.info("before prepare");
				try {
					if (prepare()) {
						logger.info("after prepare");

						packetFeederJobName = scheduler.addScheduledJob(10, RemoteBroadcastStream.this);
					}
					else {
						logger.warn("input format context cannot be created");
						//if (broadcastStream != null) 
						//{
							//	removeStreamListener(RemoteBroadcastStream.this);
						//}
						close();
					}
				} catch (Exception e) {
					e.printStackTrace();
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
	
	
	static class WriteCallback extends Write_packet_Pointer_BytePointer_int {
		
		public int sendData(byte[] data, int offset, int datalimit, RemoteBroadcastStream rbs) {
			
			if (rbs.buffer.position() == 0) {
				rbs.packetDataType = data[offset];
				rbs.packetDataSize = getSize(data[offset + 1], data[offset + 2], data[offset + 3]);
				if (rbs.buffer.capacity() < rbs.packetDataSize) {
					rbs.buffer = IoBuffer.allocate(rbs.packetDataSize);
				}
				
				rbs.packetTimestamp = getTimeStamp(data[offset + 4], data[offset + 5], data[offset + 6], data[offset + 7]); // 4 byte
				
				int length = rbs.packetDataSize + 11 + 4; // 11 packet header + 4 trailer;
				
				if (datalimit >= (length + offset)) {
					
					if (rbs.packetDataType == 9) {
						rbs.buffer.put(data, offset+11, rbs.packetDataSize);
						rbs.buffer.rewind();
						rbs.buffer.limit(rbs.packetDataSize);
						VideoData videoData = new VideoData(rbs.buffer);
						videoData.setTimestamp(rbs.packetTimestamp);
						videoData.setSourceType(Constants.SOURCE_TYPE_LIVE);
						rbs.dispatchEvent(videoData);
					}
					else if (rbs.packetDataType == 8) {
						rbs.buffer.put(data, offset+11, rbs.packetDataSize);
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
					return length + offset;
				}
				else {
					rbs.buffer.put(data, offset+11, datalimit - 11);
					return datalimit;
				}
				
			}
			else {
				int size = datalimit;
				// if buffer size is perfectly matches the size
				if (size == BUFFER_SIZE) {
					if ((rbs.buffer.position() + size - 4) == rbs.packetDataSize) {
						size = datalimit - 4;
					}
				}
				else if (datalimit < BUFFER_SIZE) {
					size = datalimit - 4; // 4 byte trailer lenght of previous stream
				}
				rbs.buffer.put(data, 0, size);
				if (rbs.buffer.position() == rbs.packetDataSize) {
					rbs.buffer.rewind();
					rbs.buffer.limit(rbs.packetDataSize);
					if (rbs.packetDataType == 9) {
					
						VideoData videoData = new VideoData(rbs.buffer);
						videoData.setTimestamp(rbs.packetTimestamp);
						videoData.setSourceType(Constants.SOURCE_TYPE_LIVE);
						rbs.dispatchEvent(videoData);
					}
					else if (rbs.packetDataType == 8) {
						AudioData audiodata = new AudioData(rbs.buffer);
						audiodata.setTimestamp(rbs.packetTimestamp);
						audiodata.setSourceType(Constants.SOURCE_TYPE_LIVE);
						rbs.dispatchEvent(audiodata);
					}
					rbs.buffer.clear();
				}
				return datalimit;
			}
			
		}
		
		int offset = 0;
		
		@Override public int call(Pointer opaque, BytePointer buf, int buf_size) {
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
					dataPosition = sendData(os.tempData, dataPosition, buf_size, os);
				} while (dataPosition != 0 && dataPosition < buf_size);

				return buf_size;
			}
			catch (Throwable t) {
				System.err.println("Error on OutputStream.write(): " + t + " size:" + size);
				t.printStackTrace();
				return -1;
			}
		}
	}

	static WriteCallback writeCallback;

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

	public void close() 
	{
		
		av_write_trailer(outputFormatContext);
		
		queueReferences.remove(outputFormatContext);
		
		avformat_free_context(outputFormatContext);
		outputFormatContext = null;

		if (avio != null) {
			if (avio.buffer() != null) {
				av_free(avio.buffer());
				avio.buffer(null);
			}
			av_free(avio);
			avio = null;
		}	

		avformat_close_input(inputFormatContext);
		inputFormatContext = null;
		
		super.close();
	}
	
	@Override
	public void execute(ISchedulingService service) throws CloneNotSupportedException {


		if (isPipeReaderJobRunning.compareAndSet(false, true)) 
		{
			//logger.info("pipe reader job in running");
			while (true) {
				if (inputFormatContext == null) {
					break;
				}
				int ret = av_read_frame(inputFormatContext, pkt);
				//

				if (ret >= 0) {
					//logger.info("input read...");
					AVStream stream = inputFormatContext.streams(pkt.stream_index());
					
					AVStream out_stream = outputFormatContext.streams(pkt.stream_index());
					
					pkt.pts(av_rescale_q_rnd(pkt.pts(), stream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
					pkt.dts(av_rescale_q_rnd(pkt.dts(), stream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
					pkt.duration(av_rescale_q(pkt.duration(),  stream.time_base(), out_stream.time_base()));
					pkt.pos(-1);

					ret = av_write_frame(outputFormatContext, pkt);


					av_packet_unref(pkt);

				}
				else {
					logger.info("removing scheduled job " + RemoteBroadcastStream.this);
					scheduler.removeScheduledJob(packetFeederJobName);
					//for(Muxer muxer : muxerList) 
					{
						//	muxer.writeTrailer();
					}
					logger.info("closing input");


					av_packet_free(pkt);

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


}
