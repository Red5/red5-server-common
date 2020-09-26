package io.antmedia.muxer;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_FLAG_GLOBAL_HEADER;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MP3;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_get_by_name;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_init;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_receive_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_send_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_init_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_ref;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_copy;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_from_context;
import static org.bytedeco.ffmpeg.global.avformat.AVFMT_GLOBALHEADER;
import static org.bytedeco.ffmpeg.global.avformat.AVFMT_NOFILE;
import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avformat.av_write_frame;
import static org.bytedeco.ffmpeg.global.avformat.av_write_trailer;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_new_stream;
import static org.bytedeco.ffmpeg.global.avformat.avformat_write_header;
import static org.bytedeco.ffmpeg.global.avformat.avio_closep;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q_rnd;
import static org.bytedeco.ffmpeg.global.avutil.av_strerror;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bytedeco.ffmpeg.avcodec.AVBSFContext;
import org.bytedeco.ffmpeg.avcodec.AVBitStreamFilter;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.javacpp.BytePointer;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;

public class DASHMuxer extends Muxer {


	private AVBSFContext bsfContext;
	private long lastDTS = -1; 

	private List<Integer> registeredStreamIndexList = new ArrayList<>();

	protected static Logger logger = LoggerFactory.getLogger(DASHMuxer.class);
	private String fragmentTime = "0.5";
	private String dashTime = "2";

	private AVRational avRationalTimeBase;
	private long totalSize;
	private long partialTotalSize;
	private long startTime;
	private long currentTime;
	private long bitrate;
	private long bitrateReferenceTime;

	int videoWidth;
	int videoHeight;
	private AVPacket tmpPacket;
	private boolean deleteFileOnExit = true;
	private int audioIndex;
	private int videoIndex;
	private String targetLatency;
	
	
	private Map<Integer, AVRational> codecTimeBaseMap = new HashMap<>();
	private AVPacket videoPkt;


	public DASHMuxer(Vertx vertx, String fragmentTime, String dashTime, String targetLatency) {
		super(vertx);
		extension = ".mpd";
		format = "dash";

		if (fragmentTime != null) {
			this.fragmentTime = fragmentTime;
		}

		if (dashTime != null) {
			this.dashTime = dashTime;
		}
		
		if (targetLatency != null) {
			this.targetLatency = targetLatency;
		}

		avRationalTimeBase = new AVRational();
		avRationalTimeBase.num(1);
		avRationalTimeBase.den(1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IScope scope, String name, int resolutionHeight) {
		if (!isInitialized) {
			super.init(scope, name, resolutionHeight);
			
			options.put("frag_duration", fragmentTime);
			options.put("seg_duration", dashTime);
			
			logger.info("DASH Segment Duration: {}, Fragment Duration: {}", dashTime, fragmentTime);
			
			if (this.targetLatency != null && !this.targetLatency.isEmpty()) {
				options.put("target_latency", this.targetLatency);
			}
			
			if (isDeleteFileOnExit()) {
				options.put("remove_at_exit", "1");
			}

			//init stream syntax
			String initStreamFilename = name +"_" + "$RepresentationID$" + "_" + resolutionHeight +"p"+ ".$ext$";
			
			//chunk stream syntax
			String chunkStreamFilename = name +"_" + "$RepresentationID$" + "_" + resolutionHeight +"p"+ "$Number%05d$" + ".$ext$";
			
			options.put("init_seg_name", initStreamFilename);
			options.put("media_seg_name", chunkStreamFilename);
			
			options.put("use_timeline", "1");
			options.put("utc_timing_url", "https://time.akamai.com/?iso");
			
			options.put("export_side_data", "1");
			
			options.put("write_prft", "1");
			
			options.put("format_options", "movflags=cmaf");
			options.put("frag_type", "duration");
			
			options.put("strict", "experimental");
			
			options.put("ldash", "1");
			
			options.put("streaming", "1");

			tmpPacket = avcodec.av_packet_alloc();
			av_init_packet(tmpPacket);
			
			
			videoPkt = avcodec.av_packet_alloc();
			av_init_packet(videoPkt);
			
			isInitialized = true;
		}

	}

	private AVFormatContext getOutputFormatContext() {
		if (outputFormatContext == null) {

			outputFormatContext= new AVFormatContext(null);
			int ret = avformat_alloc_output_context2(outputFormatContext, null, format, file.getAbsolutePath());
			if (ret < 0) {
				logger.info("Could not create output context for {}", file.getName());
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

		AVFormatContext context = getOutputFormatContext();

		int streamIndex = 0;
		for (int i=0; i < inputFormatContext.nb_streams(); i++) 
		{
			AVStream inStream = inputFormatContext.streams(i);
			if (isCodecSupported(inStream.codecpar().codec_id())) 
			{
				registeredStreamIndexList.add(i);

				AVStream outStream = avformat_new_stream(context, null);
				int codecType = inStream.codecpar().codec_type();
				if (codecType == AVMEDIA_TYPE_VIDEO) 
				{
					videoIndex = streamIndex;
					AVBitStreamFilter h264bsfc = av_bsf_get_by_name("h264_mp4toannexb");
					bsfContext = new AVBSFContext(null);

					int ret = av_bsf_alloc(h264bsfc, bsfContext);
					if (ret < 0) {
						logger.info("cannot allocate bsf context for {}", file.getName());
						return false;
					}

					ret = avcodec_parameters_copy(bsfContext.par_in(), inStream.codecpar());
					if (ret < 0) {
						logger.info("cannot copy input codec parameters for {}", file.getName());
						return false;
					}
					bsfContext.time_base_in(inStream.time_base());

					ret = av_bsf_init(bsfContext);
					if (ret < 0) {
						logger.info("cannot init bit stream filter context for {}", file.getName());
						return false;
					}

					ret = avcodec_parameters_copy(outStream.codecpar(), bsfContext.par_out());
					if (ret < 0) {
						logger.info("cannot copy codec parameters to output for {}", file.getName());
						return false;
					}
					videoWidth = outStream.codecpar().width();
					videoHeight = outStream.codecpar().height();
					outStream.time_base(bsfContext.time_base_out());
				}
				else {
					
					audioIndex = streamIndex;
					int ret = avcodec_parameters_copy(outStream.codecpar(), inStream.codecpar());
					if (ret < 0) {
						logger.info("Cannot get codec parameters for {}", file.getName());
						return false;
					}

					if (codecType != AVMEDIA_TYPE_AUDIO) {
						logger.warn("codec type should be audio but it is {}" , codecType);

					}

				}
				outStream.codecpar().codec_tag(0);

				
				
				streamIndex++;

				if ((context.oformat().flags() & AVFMT_GLOBALHEADER) != 0)
					outStream.codec().flags( outStream.codec().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
			}
			else {
				logger.warn("Codec({}) is not supported for stream: {}", inStream.codecpar().codec_id(), file.getName());
			}
		}

		prepareIO();

		return true;
	}

	private boolean isCodecSupported(int codecId) {
		return (codecId == AV_CODEC_ID_H264 || codecId == AV_CODEC_ID_AAC || codecId == AV_CODEC_ID_MP3);
	}

	/**
	 * 
	 * @return the bitrate in last 1 second
	 */
	public long getBitrate() {
		return bitrate;
	}

	public long getAverageBitrate() {

		long duration = (currentTime - startTime) ;

		if (duration > 0) 
		{
			return (totalSize / duration) * 8;
		}
		return 0;
	}


	private  void writePacket(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase, int codecType) 
	{

		if (outputFormatContext == null || !isRunning.get())  {
			logger.error("OutputFormatContext is not initialized correctly for {}", file.getName());
			return;
		}


		long pts = pkt.pts();
		long dts = pkt.dts();
		long duration = pkt.duration();
		long pos = pkt.pos();
		
		totalSize += pkt.size();
		partialTotalSize += pkt.size();
		currentTime = av_rescale_q(dts, inputTimebase, avRationalTimeBase);
		if (startTime == 0) {
			startTime = currentTime;
			bitrateReferenceTime = currentTime;
		}

		if ((currentTime - bitrateReferenceTime) >= 1) {
			bitrate = partialTotalSize * 8;
			partialTotalSize = 0;
			bitrateReferenceTime = currentTime;
		}
		
		int ret;
		pkt.pts(av_rescale_q_rnd(pkt.pts(), inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.dts(av_rescale_q_rnd(pkt.dts(), inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.duration(av_rescale_q(pkt.duration(), inputTimebase, outputTimebase));
		pkt.pos(-1);
		

		if (codecType ==  AVMEDIA_TYPE_VIDEO) 
		{
			ret = av_packet_ref(tmpPacket , pkt);
			if (ret < 0) {
				logger.error("Cannot copy packet for {}", file.getName());
				return;
			}

			if (bsfContext != null) 
			{
				ret = av_bsf_send_packet(bsfContext, tmpPacket);
				if (ret < 0)
					return;

				while (av_bsf_receive_packet(bsfContext, tmpPacket) == 0) 
				{
					ret = av_write_frame(outputFormatContext, tmpPacket);
					if (ret < 0 && logger.isInfoEnabled()) {
						byte[] data = new byte[64];
						av_strerror(ret, data, data.length);
						logger.info("cannot write video frame to muxer. Error: {} stream: {}", new String(data, 0, data.length), file.getName());
					}
				}
			}
			else {
				ret = av_write_frame(outputFormatContext, tmpPacket);
				if (ret < 0 && logger.isInfoEnabled()) {
					byte[] data = new byte[64];
					av_strerror(ret, data, data.length);
					logger.info("cannot write video frame to muxer. Error: {} stream: {}", new String(data, 0, data.length), file.getName());
				}
			}

			av_packet_unref(tmpPacket);
		}
		else {
			ret = av_write_frame(outputFormatContext, pkt);
			if (ret < 0 && logger.isInfoEnabled()) {
				byte[] data = new byte[64];
				av_strerror(ret, data, data.length);
				logger.info("cannot write frame(not video) to muxer. Error is {} ", new String(data, 0, data.length));
			}
		}
		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);

	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writeTrailer() {
		if (!isRunning.get() || outputFormatContext == null) {
			logger.warn("DASHMuxer trailer is returning because it's not correct state. Isrunning: {}, outputformatContext: {}", isRunning.get(), outputFormatContext);
			return;
		}
		isRunning.set(false);
		if (avRationalTimeBase != null) {
			avRationalTimeBase.close();
			avRationalTimeBase = null;
		}

		if (bsfContext != null) {
			av_bsf_free(bsfContext);
			bsfContext = null;
		}
		if (tmpPacket != null) {
			av_packet_free(tmpPacket);
			tmpPacket = null;
		}
		
		if (videoPkt != null) {
			av_packet_free(videoPkt);
			videoPkt = null;
		}

		av_write_trailer(outputFormatContext);

		/* close output */
		if ((outputFormatContext.oformat().flags() & AVFMT_NOFILE) == 0)
			avio_closep(outputFormatContext.pb());

		avformat_free_context(outputFormatContext);

		outputFormatContext = null;
		
		isRecording = false;	
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writePacket(AVPacket pkt) {
		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index()))  {
			logger.trace("not registered stream index {}", file.getName());
			return;
		}
		AVStream outStream = outputFormatContext.streams(pkt.stream_index());
		AVRational codecTimebase = codecTimeBaseMap.get(pkt.stream_index());
		writePacket(pkt, codecTimebase,  outStream.time_base(), outStream.codecpar().codec_type()); 

	}
	
	
	@Override
	public boolean addVideoStream(int width, int height, AVRational videoTimebase, int codecId, int streamIndex,
			boolean isAVC, AVCodecParameters codecpar) {
		boolean result = false;
		AVFormatContext outputContext = getOutputFormatContext();
		if (outputContext != null && isCodecSupported(codecId)) 
		{
			registeredStreamIndexList.add(streamIndex);
			videoIndex = streamIndex;
			AVStream outStream = avformat_new_stream(outputContext, null);
			
			outStream.codecpar().width(width);
			outStream.codecpar().height(height);
			outStream.codecpar().codec_id(codecId);
			outStream.codecpar().codec_type(AVMEDIA_TYPE_VIDEO);
			outStream.codecpar().format(AV_PIX_FMT_YUV420P);
			outStream.codecpar().codec_tag(0);
			
			AVRational timeBase = new AVRational();
			timeBase.num(1).den(1000);
			codecTimeBaseMap.put(streamIndex, timeBase);
			videoWidth = width;
			videoHeight = height;
			result = true;
		}
		return result;
	
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean addStream(AVCodec codec, AVCodecContext codecContext, int streamIndex) {

		AVFormatContext context = getOutputFormatContext();

		if (context == null) {
			return false;
		}
		if (isCodecSupported(codecContext.codec_id())) {
			registeredStreamIndexList.add(streamIndex);
			AVStream outStream = avformat_new_stream(context, codec);
			outStream.index(streamIndex);
			if (codecContext.codec_type() == AVMEDIA_TYPE_VIDEO) {
				videoWidth = codecContext.width();
				videoHeight = codecContext.height();

				avcodec_parameters_from_context(outStream.codecpar(), codecContext);
				outStream.time_base(codecContext.time_base());
				codecTimeBaseMap.put(streamIndex, codecContext.time_base());
			}
			else {

				int ret = avcodec_parameters_from_context(outStream.codecpar(), codecContext);
				codecTimeBaseMap.put(streamIndex, codecContext.time_base());
				logger.info("copy codec parameter from context {} stream index: {}", ret,  streamIndex);
				if (codecContext.codec_type() != AVMEDIA_TYPE_AUDIO) {
					logger.warn("This should be audio codec for {}", file.getName());
				}
				outStream.time_base(codecContext.time_base());
				codecTimeBaseMap.put(streamIndex, codecContext.time_base());
			}
			outStream.codecpar().codec_tag(0);

			if ((context.oformat().flags() & AVFMT_GLOBALHEADER) != 0)
				codecContext.flags( codecContext.flags() | AV_CODEC_FLAG_GLOBAL_HEADER);

		}
		return true;
	}
	


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean prepareIO() {
		AVFormatContext context = getOutputFormatContext();
		if (isRunning.get()) {
			//return false if it is already prepared
			return false;
		}
		
		int ret = 0;
		
		if ((context.oformat().flags() & AVFMT_NOFILE) == 0) {
			AVIOContext pb = new AVIOContext(null);
	
			ret = avformat.avio_open(pb,  file.getAbsolutePath(), AVIO_FLAG_WRITE);
			if (ret < 0) {
				logger.warn("Could not open output file: {} ", file.getAbsolutePath());
				return false;
			}
			context.pb(pb);
		}

		AVDictionary optionsDictionary = null;

		if (!options.isEmpty()) {
			optionsDictionary = new AVDictionary();
			Set<String> keySet = options.keySet();
			for (String key : keySet) {
				av_dict_set(optionsDictionary, key, options.get(key), 0);
			}
		}
		ret = avformat_write_header(context, optionsDictionary);		
		if (ret < 0 && logger.isWarnEnabled()) {
			byte[] data = new byte[1024];
			av_strerror(ret, data, data.length);
			logger.warn("could not write header. File: {} Error: {}", file.getAbsolutePath(), new String(data, 0, data.length));
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
	public synchronized void writePacket(AVPacket avpacket, AVStream inStream) {
		if (!isRunning.get() || !registeredStreamIndexList.contains(avpacket.stream_index()))  {
			logger.trace("not registered stream index {}", file.getName());
			return;
		}
		int streamIndex;
		if (inStream.codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
			streamIndex = videoIndex;
		}
		else if (inStream.codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
			streamIndex = audioIndex;
		}
		else {
			logger.error("Undefined codec type for {}", file.getName());
			return;
		}

		AVStream outStream = getOutputFormatContext().streams(streamIndex);
		int index = avpacket.stream_index();
		avpacket.stream_index(streamIndex);
		writePacket(avpacket, inStream.time_base(),  outStream.time_base(), outStream.codecpar().codec_type()); 
		avpacket.stream_index(index);

	}
	
	@Override
	public void writeVideoBuffer(ByteBuffer encodedVideoFrame, long timestamp, int frameRotation, int streamIndex,
								 boolean isKeyFrame,long firstFrameTimeStamp) {
		/*
		 * this control is necessary to prevent server from a native crash 
		 * in case of initiation and preparation takes long.
		 * because native objects like videoPkt can not be initiated yet
		 */
		if (!isRunning.get()) {
			logger.warn("Not writing to VideoBuffer for {} because Is running:{}", file.getName(), isRunning.get());
			return;
		}
		
		videoPkt.stream_index(streamIndex);
		videoPkt.pts(timestamp);
		videoPkt.dts(timestamp);
		
		encodedVideoFrame.rewind();
		if (isKeyFrame) {
			videoPkt.flags(videoPkt.flags() | AV_PKT_FLAG_KEY);
		}
		
		BytePointer bytePointer = new BytePointer(encodedVideoFrame);
		videoPkt.data(bytePointer);
		videoPkt.size(encodedVideoFrame.limit());
		videoPkt.position(0);
		
	
		writePacket(videoPkt);
		
		av_packet_unref(videoPkt);
	}


	
	public int getVideoWidth() {
		return videoWidth;
	}

	public int getVideoHeight() {
		return videoHeight;
	}

	public String getDashTime() {
		return dashTime;
	}

	public void setDashTime(String dashTime) {
		this.dashTime = dashTime;
	}

	public boolean isDeleteFileOnExit() {
		return deleteFileOnExit;
	}

	public void setDeleteFileOnExit(boolean deleteFileOnExist) {
		this.deleteFileOnExit = deleteFileOnExist;
	}
}