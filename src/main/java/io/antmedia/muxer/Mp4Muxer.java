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
import java.nio.file.Files;
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
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avcodec.AVBSFContext;
import org.bytedeco.javacpp.avcodec.AVBitStreamFilter;
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

import io.antmedia.storage.StorageClient;
import io.antmedia.storage.StorageClient.FileType;

public class Mp4Muxer extends Muxer {

	protected static Logger logger = LoggerFactory.getLogger(Mp4Muxer.class);
	private List<Integer> registeredStreamIndexList = new ArrayList<>();
	private File fileTmp;
	private int totalSize = 0;
	private StorageClient storageClient = null;
	private String streamId;
	private int videoIndex;
	private int audioIndex;
	private AVBitStreamFilter adtsToAscBsf;
	private AVBSFContext bsfContext;

	private AVPacket tmpPacket;



	private static String TEMP_EXTENSION = ".tmp_extension";

	public Mp4Muxer(StorageClient storageClient, QuartzSchedulingService scheduler) {
		super(scheduler);
		extension = ".mp4";
		format = "mp4";
		//options.put("movflags", "faststart+rtphint");  	
		options.put("movflags", "faststart");  
		this.storageClient = storageClient;
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IScope scope, final String name, int resolutionHeight) {
		super.init(scope, name, resolutionHeight, false);

		this.streamId = name;

		tmpPacket = avcodec.av_packet_alloc();
		av_init_packet(tmpPacket);
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


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean prepare(AVFormatContext inputFormatContext) {
		AVFormatContext context = getOutputFormatContext();

		int streamIndex = 0;
		for (int i=0; i < inputFormatContext.nb_streams(); i++) {
			AVStream in_stream = inputFormatContext.streams(i);
			if (isCodecSupported(in_stream.codecpar())) {

				int codec_type = in_stream.codecpar().codec_type();
				AVStream out_stream = avformat_new_stream(context, in_stream.codec().codec());
				logger.info(" in_stream.index() : " + in_stream.index());

				if ( codec_type == AVMEDIA_TYPE_VIDEO) {
					videoIndex = streamIndex;

					int ret = avcodec_parameters_copy(out_stream.codecpar(), in_stream.codecpar());
					if (ret < 0) {
						logger.info("Cannot get codec parameters\n");
						return false;
					}
				}
				else if (codec_type == AVMEDIA_TYPE_AUDIO) {
					audioIndex = streamIndex;


					if (bsfName != null) {
						adtsToAscBsf = av_bsf_get_by_name(this.bsfName);
						bsfContext = new AVBSFContext(null);

						int ret = av_bsf_alloc(adtsToAscBsf, bsfContext);
						if (ret < 0) {
							logger.info("cannot allocate bsf context");
							return false;
						}

						ret = avcodec_parameters_copy(bsfContext.par_in(), in_stream.codecpar());
						if (ret < 0) {
							logger.info("cannot copy input codec parameters");
							return false;
						}
						bsfContext.time_base_in(in_stream.time_base());

						ret = av_bsf_init(bsfContext);
						if (ret < 0) {
							logger.info("cannot init bit stream filter context");
							return false;
						}

						ret = avcodec_parameters_copy(out_stream.codecpar(), bsfContext.par_out());
						if (ret < 0) {
							logger.info("cannot copy codec parameters to output");
							return false;
						}

						out_stream.time_base(bsfContext.time_base_out());
					}
					else {
						int ret = avcodec_parameters_copy(out_stream.codecpar(), in_stream.codecpar());
						if (ret < 0) {
							logger.info("Cannot get codec parameters\n");
							return false;
						}
					}
				}
				else {
					logger.error("undefined codec type: " + codec_type);
					continue;
				}

				streamIndex++;
				registeredStreamIndexList.add(i);

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
	public synchronized boolean prepareIO() {

		AVFormatContext context = getOutputFormatContext();
		if (context.pb() != null) {
			//return false if it is already prepared
			return false;
		}

		AVIOContext pb = new AVIOContext(null);

		int ret = avformat.avio_open(pb, fileTmp.getAbsolutePath(), AVIO_FLAG_WRITE);
		if (ret < 0) {
			logger.warn("Could not open output file: " + fileTmp.getAbsolutePath() + 
					" parent file exists:" + fileTmp.getParentFile().exists());
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

		logger.info("before writing header");
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
	public synchronized void writeTrailer() {

		if (!isRunning.get() || outputFormatContext == null || outputFormatContext.pb() == null) {
			//return if it is already null
			logger.warn("OutputFormatContext is not initialized or it is freed");
			return;
		}

		isRunning.set(false);

		av_write_trailer(outputFormatContext);

		clearResource();

		isRecording = false;
		String absolutePath = fileTmp.getAbsolutePath();

		String origFileName = absolutePath.replace(TEMP_EXTENSION, "");

		final File f = new File(origFileName);

		try {
			Files.move(fileTmp.toPath(),f.toPath());

			IContext context = Mp4Muxer.this.scope.getContext(); 
			ApplicationContext appCtx = context.getApplicationContext(); 
			Object bean = appCtx.getBean("web.handler");
			if (bean instanceof IMuxerListener) {
				((IMuxerListener)bean).muxingFinished(this.streamId, f, getDuration(f));
			}

			if (storageClient != null) {
				scheduler.addScheduledOnceJob(1000, new IScheduledJob() {

					@Override
					public void execute(ISchedulingService service) throws CloneNotSupportedException {
						storageClient.save(f, FileType.TYPE_STREAM);
					}
				});

			}
		} catch (IOException e) {

			e.printStackTrace();
		}
	}


	public long getDuration(File f) {
		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();
		int ret;
		if ((ret = avformat_open_input(inputFormatContext, f.getAbsolutePath(), null, (AVDictionary)null)) < 0) {
			logger.info("cannot open input context");
			avformat_close_input(inputFormatContext);
			return -1L;
		}

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary)null);
		if (ret < 0) {
			logger.info("Could not find stream information\n");
			avformat_close_input(inputFormatContext);
			return -1L;
		}
		long durationInMS = -1;
		if (inputFormatContext.duration() != AV_NOPTS_VALUE) 
		{
			durationInMS = inputFormatContext.duration() / 1000;
		}
		avformat_close_input(inputFormatContext);
		return durationInMS;
	}

	private void clearResource() {

		if (bsfContext != null) {
			av_bsf_free(bsfContext);
			bsfContext = null;
		}
		if (tmpPacket != null) {
			av_packet_free(tmpPacket);
			tmpPacket = null;
		}

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
			logger.trace("not registered stream index");
			return;
		}
		int streamIndex;
		if (stream.codec().codec_type() == AVMEDIA_TYPE_VIDEO) {
			streamIndex = videoIndex;
		}
		else if (stream.codec().codec_type() == AVMEDIA_TYPE_AUDIO) {
			streamIndex = audioIndex;
		}
		else {
			logger.error("Undefined codec type ");
			return;
		}

		AVStream out_stream = outputFormatContext.streams(streamIndex);
		int index = pkt.stream_index();
		pkt.stream_index(streamIndex);
			
		writePacket(pkt, stream.time_base(),  out_stream.time_base(), out_stream.codecpar().codec_type()); 
	
		pkt.stream_index(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writePacket(AVPacket pkt) {
		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index())) {
			logger.trace("not registered stream index");
			return;
		}

		AVStream out_stream = outputFormatContext.streams(pkt.stream_index());

		writePacket(pkt, out_stream.codec().time_base(),  out_stream.time_base(), out_stream.codecpar().codec_type()); 
	}


	/**
	 * All other writePacket functions call this function to make the job
	 * 
	 * @param pkt 
	 * Content of the data in AVPacket class
	 * 
	 * @param inputTimebase
	 * input time base is required to calculate the correct dts and pts values for the container
	 * 
	 * @param outputTimebase
	 * output time base is required to calculate the correct dts and pts values for the container
	 */
	private void writePacket(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase, int codecType) 
	{

		AVFormatContext context = getOutputFormatContext();
		if (context == null || context.pb() == null) {
			logger.warn("output context.pb field is null");
			return;
		}

		totalSize += pkt.size();

		long pts = pkt.pts();
		long dts = pkt.dts();
		long duration = pkt.duration();
		long pos = pkt.pos();
		


		pkt.pts(av_rescale_q_rnd(pkt.pts(), inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.dts(av_rescale_q_rnd(pkt.dts(), inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.duration(av_rescale_q(pkt.duration(), inputTimebase, outputTimebase));
		pkt.pos(-1);

		if (codecType == AVMEDIA_TYPE_AUDIO) {
			
			/*
			int ret = av_copy_packet(tmpPacket , pkt);
			if (ret < 0) {
				logger.error("Cannot copy packet!!!");
				return;
			}

			if (bsfContext != null) {

				ret = av_bsf_send_packet(bsfContext, tmpPacket);
				if (ret < 0)
					return;

				while ((ret = av_bsf_receive_packet(bsfContext, tmpPacket)) == 0) 
				{

					ret = av_write_frame(context, tmpPacket);
					if (ret < 0) {
						logger.info("cannot write frame to muxer in av_bsf_receive_packet");
						
						logger.info("input timebase den {}", inputTimebase.den());
						logger.info("input timebase num {}", inputTimebase.num());
						logger.info("output timebase den {}", outputTimebase.den());
						logger.info("output timebase num {}", outputTimebase.num());
						
						logger.info("received dts {}", dts);
						logger.info("calculated dts {}", pkt.dts());
						
					}

				}
			}
			else {
				ret = av_write_frame(context, tmpPacket);
				if (ret < 0) {
					logger.info("cannot write frame to muxer in av_write_frame");
				}
			}

			av_packet_unref(tmpPacket);
			
			*/
			
			int ret = av_write_frame(context, pkt);
			if (ret < 0) {
				logger.info("cannot write frame to muxer in av_write_frame");
			}
		}
		else {
			int ret = av_write_frame(context, pkt);
			if (ret < 0) {
				logger.warn("cannot write frame to muxer, not audio"); 
			}
		}


		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);

	}

}
