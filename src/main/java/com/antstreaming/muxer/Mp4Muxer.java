package com.antstreaming.muxer;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_FLAG_GLOBAL_HEADER;
import static org.bytedeco.javacpp.avcodec.av_packet_unref;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_from_context;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_to_context;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.buffer.IoBuffer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.avformat;
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

public class Mp4Muxer extends AbstractMuxer {

	protected static Logger logger = LoggerFactory.getLogger(Mp4Muxer.class);


	public Mp4Muxer() {
		extension = ".mp4";
		format = "mp4";
		options.put("movflags", "faststart+rtphint");	
	}


	@Override
	public boolean prepare(AVFormatContext inputFormatContext) {
		outputFormatContext= new AVFormatContext(null);
		int ret = avformat_alloc_output_context2(outputFormatContext, null, format, file.getAbsolutePath());
		if (ret < 0) {
			logger.info("Could not create output context\n");
			return false;
		}

		for (int i=0; i < inputFormatContext.nb_streams(); i++) {
			AVStream in_stream = inputFormatContext.streams(i);
			AVStream out_stream = avformat_new_stream(outputFormatContext, in_stream.codec().codec());
			AVCodecParameters avCodecParameters = new AVCodecParameters();

			ret = avcodec_parameters_from_context(avCodecParameters,  in_stream.codec());
			if (ret < 0) {
				logger.info("Cannot get codec parameters\n");
				return false;
			}
			ret  = avcodec_parameters_to_context(out_stream.codec(), avCodecParameters);
			if (ret < 0) {
				logger.info("Cannot set codec parameters\n");
				return false;
			}
			out_stream.codec().codec_tag(0);

			if ((outputFormatContext.oformat().flags() & AVFMT_GLOBALHEADER) != 0)
				out_stream.codec().flags( out_stream.codec().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
		}

		AVIOContext pb = new AVIOContext(null);

		ret = avformat.avio_open(pb,  file.getAbsolutePath(), AVIO_FLAG_WRITE);
		if (ret < 0) {
			System.out.println("Could not open output file");
			return false;
		}
		outputFormatContext.pb(pb);

		AVDictionary optionsDictionary = null;

		if (!options.isEmpty()) {
			optionsDictionary = new AVDictionary();
			Set<String> keySet = options.keySet();
			for (String key : keySet) {
				av_dict_set(optionsDictionary, key, options.get(key), 0);
			}
		}
		ret = avformat_write_header(outputFormatContext, optionsDictionary);		
		if (ret < 0) {
			logger.warn("could not write header");
			return false;
		}

		return true;
	}

	@Override
	public void writeTrailer() {
		logger.info("write trailer...");

		av_write_trailer(outputFormatContext);

		/* close output */
		if ((outputFormatContext.flags() & AVFMT_NOFILE) == 0)
			avio_closep(outputFormatContext.pb());

		avformat_free_context(outputFormatContext);
		isRecording = false;	
	}

	@Override
	public void writePacket(AVPacket pkt, AVStream inStream) 
	{
		int packetIndex = pkt.stream_index();
		AVStream out_stream = outputFormatContext.streams(packetIndex);

		pkt.pts(av_rescale_q_rnd(pkt.pts(), inStream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.dts(av_rescale_q_rnd(pkt.dts(), inStream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.duration(av_rescale_q(pkt.duration(), inStream.time_base(), out_stream.time_base()));
		pkt.pos(-1);

		int ret = av_write_frame(outputFormatContext, pkt);
		if (ret < 0) {
			logger.info("cannot write frame to muxer");
		}

	}






}
