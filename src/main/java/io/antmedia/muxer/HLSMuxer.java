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


public class HLSMuxer extends Muxer  {


	private AVBSFContext bsfContext;
	private long lastDTS = -1; 

	private List<Integer> registeredStreamIndexList = new ArrayList<>();

	protected static Logger logger = LoggerFactory.getLogger(HLSMuxer.class);
	private String  hlsListSize = "20";
	private String hlsTime = "5";
	private String hlsPlayListType = null; 

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
	private String hlsFlags;
	
	private Map<Integer, AVRational> codecTimeBaseMap = new HashMap<>();
	private AVPacket videoPkt;
	private int time2log = 0;


	public HLSMuxer(Vertx vertx, String hlsListSize, String hlsTime, String hlsPlayListType, String hlsFlags) {
		super(vertx);
		extension = ".m3u8";
		format = "hls";

		if (hlsListSize != null) {
			this.hlsListSize = hlsListSize;
		}

		if (hlsTime != null) {
			this.hlsTime = hlsTime;
		}

		if (hlsPlayListType != null) {
			this.hlsPlayListType = hlsPlayListType;
		}
		
		if (hlsFlags != null) {
			this.hlsFlags = hlsFlags;
		}
		else {
			this.hlsFlags = "";
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

			options.put("hls_list_size", hlsListSize);
			options.put("hls_time", hlsTime);
			
			
			logger.info("hls time: {}, hls list size: {}", hlsTime, hlsListSize);

			String segmentFilename = file.getParentFile() + "/" + name +"_" + resolutionHeight +"p"+ "%04d.ts";
			options.put("hls_segment_filename", segmentFilename);

			if (hlsPlayListType != null && (hlsPlayListType.equals("event") || hlsPlayListType.equals("vod"))) {
				options.put("hls_playlist_type", hlsPlayListType);
			}

			if (this.hlsFlags != null && !this.hlsFlags.isEmpty()) {
				options.put("hls_flags", this.hlsFlags);
			}
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
			logger.warn("HLSMuxer trailer is returning because it's not correct state. Isrunning: {}, outputformatContext: {}", isRunning.get(), outputFormatContext);
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

		logger.info("Delete File onexit:{}", deleteFileOnExit);
		if (vertx != null && deleteFileOnExit ) {

			logger.info("Scheduling the task to delete. HLS time: {}, hlsListSize:{}", hlsTime, hlsListSize);
			vertx.setTimer(Integer.parseInt(hlsTime) * Integer.parseInt(hlsListSize) * 1000, l -> {
				logger.info("Deleting HLS files on exit");

				final String filenameWithoutExtension = file.getName().substring(0, file.getName().lastIndexOf(extension));

				File[] files = file.getParentFile().listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.contains(filenameWithoutExtension) && name.endsWith(".ts");
					}
				});

				if (files != null) 
				{

					for (int i = 0; i < files.length; i++) {
						try {
							if (!files[i].exists()) {
								continue;
							}
							Files.delete(files[i].toPath());
						} catch (IOException e) {
							logger.error(e.getMessage());
						}
					}
				}
				if (file.exists()) {
					try {
						Files.delete(file.toPath());
					} catch (IOException e) {
						logger.error(e.getMessage());
					}
				}
			});
		}

		isRecording = false;	
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writePacket(AVPacket pkt, AVCodecContext codecContext) {
		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index()))  {
			if (time2log % 100 == 0) {
				logger.warn("not registered stream index {}", file.getName());
				time2log++;
			}
			time2log++;
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
			if (time2log % 100 == 0) {
				logger.warn("not registered stream index {}", file.getName());
				time2log = 0;
			}
			time2log++;
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
			if (time2log % 100 == 0) {
				logger.warn("Not writing to VideoBuffer for {} because Is running:{}", file.getName(), isRunning.get());
				time2log = 0;
			}
			time2log++;
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
		
	
		writePacket(videoPkt, (AVCodecContext)null);
		
		av_packet_unref(videoPkt);
	}


	
	public int getVideoWidth() {
		return videoWidth;
	}

	public int getVideoHeight() {
		return videoHeight;
	}

	public String getHlsListSize() {
		return hlsListSize;
	}

	public void setHlsListSize(String hlsListSize) {
		this.hlsListSize = hlsListSize;
	}

	public String getHlsTime() {
		return hlsTime;
	}

	public void setHlsTime(String hlsTime) {
		this.hlsTime = hlsTime;
	}

	public String getHlsPlayListType() {
		return hlsPlayListType;
	}

	public void setHlsPlayListType(String hlsPlayListType) {
		this.hlsPlayListType = hlsPlayListType;
	}

	public boolean isDeleteFileOnExit() {
		return deleteFileOnExit;
	}

	public void setDeleteFileOnExit(boolean deleteFileOnExist) {
		this.deleteFileOnExit = deleteFileOnExist;
	}
}
