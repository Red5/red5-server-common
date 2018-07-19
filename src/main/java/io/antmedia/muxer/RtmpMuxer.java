package io.antmedia.muxer;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVOutputFormat;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avformat.Read_packet_Pointer_BytePointer_int;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil;
import org.red5.codec.IStreamCodecInfo;
import org.red5.io.ITag;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;
import org.red5.server.net.rtmp.event.CachedEvent;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.stream.DefaultStreamFilenameGenerator;
import org.red5.server.stream.IRecordingListener;
import org.red5.server.stream.consumer.FileConsumer;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;

import com.google.common.io.Files;

import io.antmedia.muxer.Muxer;
import io.antmedia.storage.StorageClient;
import io.antmedia.storage.StorageClient.FileType;

public class RtmpMuxer extends Muxer {

	protected static Logger logger = LoggerFactory.getLogger(RtmpMuxer.class);
	private List<Integer> registeredStreamIndexList = new ArrayList<>();
	public long totalSize;
	private String url;

	public RtmpMuxer(String url) {
		super(null);
		format = "flv";
		this.url = url;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean addStream(AVCodec codec, AVCodecContext codecContext, int streamIndex) {

		AVFormatContext outputContext = getOutputFormatContext();

		if (outputContext == null) {
			return false;
		}
		//if (isCodecSupported(codecContext.codec_id())) 
		{
			registeredStreamIndexList.add(streamIndex);
			AVStream out_stream = avformat_new_stream(outputContext, codec);

			out_stream.codec().time_base(codecContext.time_base());
			int ret = avcodec_parameters_from_context(out_stream.codecpar(), codecContext);

			if (ret < 0) {
				System.out.println("codec context cannot be copied");
			}
			out_stream.codec().codec_tag(0);
			if ((outputContext.oformat().flags() & AVFMT_GLOBALHEADER) != 0)
				out_stream.codec().flags( out_stream.codec().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);

		}
		return true;
	}

	private AVFormatContext getOutputFormatContext() {
		if (outputFormatContext == null) {
			outputFormatContext= new AVFormatContext(null);
			int ret = avformat_alloc_output_context2(outputFormatContext, null, format, null);
			if (ret < 0) {
				logger.info("Could not create output context\n");
				return null;
			}
		}
		return outputFormatContext;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean prepare(AVFormatContext inputFormatContext) {
		
		logger.info("preparing rtmp muxer");
		AVFormatContext context = getOutputFormatContext();

		for (int i=0; i < inputFormatContext.nb_streams(); i++) {
			AVStream in_stream = inputFormatContext.streams(i);
			{
				registeredStreamIndexList.add(i);

				AVStream out_stream = avformat_new_stream(context, in_stream.codec().codec());

				int ret = avcodec_parameters_copy(out_stream.codecpar(), in_stream.codecpar());
				if (ret < 0) {
					logger.info("Cannot get codec parameters\n");
					return false;
				}

				out_stream.codec().codec_tag(0);
				out_stream.codecpar().codec_tag(0);

				if ((context.oformat().flags() & AVFMT_GLOBALHEADER) != 0)
					out_stream.codec().flags( out_stream.codec().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
			}
		}

		prepareIO();
		
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public  boolean prepareIO() {
		AVFormatContext context = getOutputFormatContext();
		if (context.pb() != null) {
			//return false if it is already prepared
			return false;
		}
		
		AVIOContext pb = new AVIOContext(null);

		logger.info("rtmp muxer opening: " + url);
		int ret = avformat.avio_open(pb,  url, AVIO_FLAG_WRITE);
		if (ret < 0) {
			logger.warn("Could not open output file");
			return false;
		}
		context.pb(pb);
		
		

		AVDictionary optionsDictionary = null;

		if (!options.isEmpty()) {
			optionsDictionary = new AVDictionary();
			Set<String> keySet = options.keySet();
			for (String key : keySet) {
				av_dict_set(optionsDictionary, key, options.get(key), 0);
			}
		}

		logger.warn("before writing rtmp muxer header");
		ret = avformat_write_header(context, optionsDictionary);		
		if (ret < 0) {
			logger.warn("could not write header");

			clearResource();
			return false;
		}
		if (optionsDictionary != null) {
			av_dict_free(optionsDictionary);
		}
		isRunning.set(true);

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeTrailer() {
		
		if (!isRunning.get() || outputFormatContext == null || outputFormatContext.pb() == null) {
			//return if it is already null
			return;
		}
		isRunning.set(false);


		av_write_trailer(outputFormatContext);
		clearResource();

		isRecording = false;
	}


	private void clearResource() {
		/* close output */
		if ((outputFormatContext.flags() & AVFMT_NOFILE) == 0)
			avio_closep(outputFormatContext.pb());

		avformat_free_context(outputFormatContext);
		outputFormatContext = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writePacket(AVPacket pkt, AVStream stream) {
		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index())) {
			return;
		}
		AVStream out_stream = outputFormatContext.streams(pkt.stream_index());
		writePacket(pkt, stream.time_base(),  out_stream.time_base()); 
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writePacket(AVPacket pkt) {
		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index())) {
			return;
		}
		AVStream out_stream = outputFormatContext.streams(pkt.stream_index());
		writePacket(pkt, out_stream.codec().time_base(),  out_stream.time_base()); 
	}


	private void writePacket(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase) 
	{

		AVFormatContext context = getOutputFormatContext();
		
		if (context == null || context.pb() == null) {
			//return if it is already null
			logger.warn("output context or .pb field is null");
			return;
		}

		totalSize += pkt.size();

		int packetIndex = pkt.stream_index();
		//TODO: find a better frame to check if stream exists in outputFormatContext

		if (!registeredStreamIndexList.contains(packetIndex)) {
			return;
		}

		long pts = pkt.pts();
		long dts = pkt.dts();
		long duration = pkt.duration();
		long pos = pkt.pos();
	

		pkt.pts(av_rescale_q_rnd(pkt.pts(), inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.dts(av_rescale_q_rnd(pkt.dts(), inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.duration(av_rescale_q(pkt.duration(), inputTimebase, outputTimebase));
		pkt.pos(-1);

		int ret = av_write_frame(context, pkt);
		if (ret < 0) {
			logger.warn("cannot write frame to muxer"); 
		}

		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);

	}

}
