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
import org.apache.mina.core.buffer.IoBuffer;
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

public class Mp4Muxer extends Muxer {

	protected static Logger logger = LoggerFactory.getLogger(Mp4Muxer.class);
	private List<Integer> registeredStreamIndexList = new ArrayList<>();
	private File fileTmp;
	private boolean addDateTimeToMp4FileName;
	private int totalSize = 0;

 
	private static String TEMP_EXTENSION = ".tmp_extension";

	public Mp4Muxer() {

		extension = ".mp4";
		format = "mp4";
		//options.put("movflags", "faststart+rtphint");  	
		options.put("movflags", "faststart");  
	}

	public static int[] mp4_supported_codecs = {
			AV_CODEC_ID_MOV_TEXT     ,
			AV_CODEC_ID_MPEG4        ,
			AV_CODEC_ID_H264         ,
			AV_CODEC_ID_HEVC         ,
			AV_CODEC_ID_AAC          ,
			AV_CODEC_ID_MP4ALS       , /* 14496-3 ALS */
			AV_CODEC_ID_MPEG2VIDEO  , /* MPEG-2 Main */
			AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 Simple */
			AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 SNR */
			AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 Spatial */
			AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 High */
			AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 422 */
			AV_CODEC_ID_AAC          , /* MPEG-2 AAC Main */
			AV_CODEC_ID_AAC          , /* MPEG-2 AAC Low */
			AV_CODEC_ID_AAC          , /* MPEG-2 AAC SSR */
			AV_CODEC_ID_MP3          , /* 13818-3 */
			AV_CODEC_ID_MP2          , /* 11172-3 */
			AV_CODEC_ID_MPEG1VIDEO   , /* 11172-2 */
			AV_CODEC_ID_MP3          , /* 11172-3 */
			AV_CODEC_ID_MJPEG        , /* 10918-1 */
			AV_CODEC_ID_PNG          ,
			AV_CODEC_ID_JPEG2000     , /* 15444-1 */
			AV_CODEC_ID_VC1          ,
			AV_CODEC_ID_DIRAC        ,
			AV_CODEC_ID_AC3          ,
			AV_CODEC_ID_EAC3         ,
			AV_CODEC_ID_DTS          , /* mp4ra.org */
			AV_CODEC_ID_VP9          , /* nonstandard, update when there is a standard value */
			AV_CODEC_ID_TSCC2        , /* nonstandard, camtasia uses it */
			AV_CODEC_ID_VORBIS       , /* nonstandard, gpac uses it */
			AV_CODEC_ID_DVD_SUBTITLE , /* nonstandard, see unsupported-embedded-subs-2.mp4 */
			AV_CODEC_ID_QCELP        ,
			AV_CODEC_ID_MPEG4SYSTEMS ,
			AV_CODEC_ID_MPEG4SYSTEMS ,
			AV_CODEC_ID_NONE        
	};


	private boolean isCodecSupported(AVCodecParameters avCodecParameters) {
		return isCodecSupported(avCodecParameters.codec_id());
	}

	private boolean isCodecSupported(int codecId) {
		for (int i=0; i< mp4_supported_codecs.length; i++) {
			if (codecId == mp4_supported_codecs[i]) {
				return true;
			}
		}
		return false;

	}

	@Override
	public void init(IScope scope, String name) {
		if (addDateTimeToMp4FileName) {
			SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm");
			Date dt = new Date();
			name = name + "-" + dtFormat.format(dt);
		}

		file = getRecordFile(scope, name, extension);

		File parentFile = file.getParentFile();
		if (!parentFile.exists()) {
			parentFile.mkdir();
		}
	}


	public boolean addStream(AVCodec codec, AVCodecContext codecContext, int streamIndex) {

		AVFormatContext outputContext = getOutputFormatContext();

		if (outputContext == null) {
			return false;
		}
		if (isCodecSupported(codecContext.codec_id())) {
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
			fileTmp = new File(file.getAbsolutePath() + TEMP_EXTENSION);
			int ret = avformat_alloc_output_context2(outputFormatContext, null, format, fileTmp.getAbsolutePath());
			if (ret < 0) {
				logger.info("Could not create output context\n");
				return null;
			}
		}
		return outputFormatContext;
	}


	@Override
	public boolean prepare(AVFormatContext inputFormatContext) {
		AVFormatContext context = getOutputFormatContext();

		for (int i=0; i < inputFormatContext.nb_streams(); i++) {
			AVStream in_stream = inputFormatContext.streams(i);
			if (isCodecSupported(in_stream.codecpar())) {

				registeredStreamIndexList.add(i);

				AVStream out_stream = avformat_new_stream(context, in_stream.codec().codec());

				int ret = avcodec_parameters_copy(out_stream.codecpar(), in_stream.codecpar());
				if (ret < 0) {
					logger.info("Cannot get codec parameters\n");
					return false;
				}

				out_stream.codec().codec_tag(0);

				if ((context.oformat().flags() & AVFMT_GLOBALHEADER) != 0)
					out_stream.codec().flags( out_stream.codec().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
			}
		}

		prepareIO();
		return true;
	}

	public boolean prepareIO() {
		AVIOContext pb = new AVIOContext(null);

		int ret = avformat.avio_open(pb,  fileTmp.getAbsolutePath(), AVIO_FLAG_WRITE);
		if (ret < 0) {
			logger.warn("Could not open output file");
			return false;
		}
		getOutputFormatContext().pb(pb);

		AVDictionary optionsDictionary = null;

		if (!options.isEmpty()) {
			optionsDictionary = new AVDictionary();
			Set<String> keySet = options.keySet();
			for (String key : keySet) {
				av_dict_set(optionsDictionary, key, options.get(key), 0);
			}
		}

		logger.warn("before writing header");
		ret = avformat_write_header(getOutputFormatContext(), optionsDictionary);		
		if (ret < 0) {
			logger.warn("could not write header");

			clearResource();
			return false;
		}
		if (optionsDictionary != null) {
			av_dict_free(optionsDictionary);
		}

		return true;

	}

	@Override
	public void writeTrailer() {
		
		av_write_trailer(outputFormatContext);
		
		clearResource();
		
		isRecording = false;
		String absolutePath = fileTmp.getAbsolutePath();

		String origFileName = absolutePath.replace(TEMP_EXTENSION, "");

		File f = new File(origFileName);
		boolean renameTo = fileTmp.renameTo(f);
		if (!renameTo) {
			logger.error("Renaming from {} to {} failed" , absolutePath, origFileName);
		}
	}


	private void clearResource() {
		/* close output */
		if ((outputFormatContext.flags() & AVFMT_NOFILE) == 0)
			avio_closep(outputFormatContext.pb());

		avformat_free_context(outputFormatContext);
		outputFormatContext = null;
	}

	@Override
	public void writePacket(AVPacket pkt, AVStream stream) {
		AVStream out_stream = outputFormatContext.streams(pkt.stream_index());
		writePacket(pkt, stream.time_base(),  out_stream.time_base()); 
	}

	@Override
	public void writePacket(AVPacket pkt) {
		AVStream out_stream = outputFormatContext.streams(pkt.stream_index());
		writePacket(pkt, out_stream.codec().time_base(),  out_stream.time_base()); 
	}


	public void writePacket(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase) 
	{
		
		AVFormatContext context = getOutputFormatContext();
		
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
	public void setAddDateTimeToFileNames(boolean addDateTimeToMp4FileName) {
		this.addDateTimeToMp4FileName = addDateTimeToMp4FileName;
	}
}
