package io.antmedia.muxer;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_FLAG_GLOBAL_HEADER;
import static org.bytedeco.javacpp.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.javacpp.avcodec.av_bsf_alloc;
import static org.bytedeco.javacpp.avcodec.av_bsf_free;
import static org.bytedeco.javacpp.avcodec.av_bsf_get_by_name;
import static org.bytedeco.javacpp.avcodec.av_bsf_init;
import static org.bytedeco.javacpp.avcodec.av_bsf_receive_packet;
import static org.bytedeco.javacpp.avcodec.av_bsf_send_packet;
import static org.bytedeco.javacpp.avcodec.av_init_packet;
import static org.bytedeco.javacpp.avcodec.av_packet_free;
import static org.bytedeco.javacpp.avcodec.av_packet_ref;
import static org.bytedeco.javacpp.avcodec.av_packet_unref;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_copy;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_from_context;
import static org.bytedeco.javacpp.avformat.AVFMT_GLOBALHEADER;
import static org.bytedeco.javacpp.avformat.AVFMT_NOFILE;
import static org.bytedeco.javacpp.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.javacpp.avformat.av_write_frame;
import static org.bytedeco.javacpp.avformat.av_write_trailer;
import static org.bytedeco.javacpp.avformat.avformat_alloc_output_context2;
import static org.bytedeco.javacpp.avformat.avformat_free_context;
import static org.bytedeco.javacpp.avformat.avformat_new_stream;
import static org.bytedeco.javacpp.avformat.avformat_write_header;
import static org.bytedeco.javacpp.avformat.avio_closep;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.javacpp.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.javacpp.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.javacpp.avutil.av_dict_free;
import static org.bytedeco.javacpp.avutil.av_dict_set;
import static org.bytedeco.javacpp.avutil.av_rescale_q;
import static org.bytedeco.javacpp.avutil.av_rescale_q_rnd;
import static org.bytedeco.javacpp.avutil.av_strerror;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_AUDIO;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bytedeco.javacpp.avcodec.AVBSFContext;
import org.bytedeco.javacpp.avcodec.AVBitStreamFilter;
import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVRational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RtmpMuxer extends Muxer {

	protected static Logger logger = LoggerFactory.getLogger(RtmpMuxer.class);
	private List<Integer> registeredStreamIndexList = new ArrayList<>();
	private String url;
	private AVPacket videoPkt;
	private Map<Integer, AVRational> codecTimeBaseMap = new HashMap<>();
	private AVBSFContext bsfExtractdataContext = null;
	private AVPacket tmpPacket;

	public RtmpMuxer(String url) {
		super(null);
		format = "flv";
		this.url = url;
		
		videoPkt = avcodec.av_packet_alloc();
		av_init_packet(videoPkt);
		
		tmpPacket = avcodec.av_packet_alloc();
		av_init_packet(tmpPacket);
	}

	public String getURL() {
		return url;
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
		registeredStreamIndexList.add(streamIndex);
		AVStream outStream = avformat_new_stream(outputContext, codec);		
		outStream.time_base(codecContext.time_base());
		
		int ret = avcodec_parameters_from_context(outStream.codecpar(), codecContext);

		if (ret < 0) {
			logger.info("codec context cannot be copied for url: {}", url);
		}
		outStream.codecpar().codec_tag(0);
		codecTimeBaseMap.put(streamIndex, codecContext.time_base());
		return true;
		
	}

	private AVFormatContext getOutputFormatContext() {
		if (outputFormatContext == null) {
			outputFormatContext= new AVFormatContext(null);
			int ret = avformat_alloc_output_context2(outputFormatContext, null, format, null);
			if (ret < 0) {
				logger.info("Could not create output context for url {}", url);
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

		logger.info("preparing rtmp muxer for {}", url);
		AVFormatContext context = getOutputFormatContext();

		for (int i=0; i < inputFormatContext.nb_streams(); i++) {
			AVStream inStream = inputFormatContext.streams(i);
			registeredStreamIndexList.add(i);

			AVStream outStream = avformat_new_stream(context, inStream.codec().codec());

			int ret = avcodec_parameters_copy(outStream.codecpar(), inStream.codecpar());
			if (ret < 0) {
				logger.info("Cannot get codec parameters {}", url);
				return false;
			}

			outStream.codec().codec_tag(0);
			outStream.codecpar().codec_tag(0);

			if ((context.oformat().flags() & AVFMT_GLOBALHEADER) != 0)
				outStream.codec().flags( outStream.codec().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
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
		if (context != null && context.pb() != null) {
			//return false if it is already prepared
			return false;
		}

		AVIOContext pb = new AVIOContext(null);

		logger.info("rtmp muxer opening: {} time:{}" , url, System.currentTimeMillis());
		int ret = avformat.avio_open(pb,  url, AVIO_FLAG_WRITE);
		if (ret < 0) {
			logger.error("Could not open output file for rtmp url {}", url);
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

		logger.info("before writing rtmp muxer header to {}", url);
		ret = avformat_write_header(context, optionsDictionary);		
		if (ret < 0) {
			logger.warn("could not write header to rtmp url {}", url);

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
		if ((outputFormatContext.flags() & AVFMT_NOFILE) == 0) {
			avio_closep(outputFormatContext.pb());
		}
		
		if (videoPkt != null) {
			av_packet_free(videoPkt);
			videoPkt = null;
		}
		
		if (tmpPacket != null) {
			av_packet_free(tmpPacket);
			tmpPacket = null;
		}
		
		if (bsfExtractdataContext != null) {
			av_bsf_free(bsfExtractdataContext);
			bsfExtractdataContext = null;
		}

		avformat_free_context(outputFormatContext);
		outputFormatContext = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addVideoStream(int width, int height, AVRational timebase, int codecId, int streamIndex, boolean isAVC, AVCodecParameters codecpar) {
		boolean result = false;
		AVFormatContext outputContext = getOutputFormatContext();
		if (outputContext != null) 
		{
			registeredStreamIndexList.add(streamIndex);
			AVStream outStream = avformat_new_stream(outputContext, null);
			outStream.codecpar().width(width);
			outStream.codecpar().height(height);
			outStream.codecpar().codec_id(codecId);
			outStream.codecpar().codec_type(AVMEDIA_TYPE_VIDEO);
			outStream.codecpar().format(AV_PIX_FMT_YUV420P);
			outStream.codecpar().codec_tag(0);
			
			AVRational timeBase = new AVRational();
			timeBase.num(1).den(1000);
						

			AVBitStreamFilter h264bsfc = av_bsf_get_by_name("extract_extradata");
			bsfExtractdataContext = new AVBSFContext(null);

			int ret = av_bsf_alloc(h264bsfc, bsfExtractdataContext);
			if (ret < 0) {
				logger.info("cannot allocate bsf context for {}", file.getName());
				return false;
			}

			ret = avcodec_parameters_copy(bsfExtractdataContext.par_in(), outStream.codecpar());
			if (ret < 0) {
				logger.info("cannot copy input codec parameters for {}", file.getName());
				return false;
			}
			bsfExtractdataContext.time_base_in(timeBase);

			ret = av_bsf_init(bsfExtractdataContext);
			if (ret < 0) {
				logger.info("cannot init bit stream filter context for {}", file.getName());
				return false;
			}

			ret = avcodec_parameters_copy(outStream.codecpar(), bsfExtractdataContext.par_out());
			if (ret < 0) {
				logger.info("cannot copy codec parameters to output for {}", file.getName());
				return false;
			}
			outStream.time_base(bsfExtractdataContext.time_base_out());
			
			codecTimeBaseMap.put(streamIndex, timeBase);
			result = true;
		}

		return result;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writePacket(AVPacket pkt, AVStream stream) {
		AVStream outStream = outputFormatContext.streams(pkt.stream_index());
		writePacket(pkt, stream.time_base(),  outStream.time_base(), outStream.codecpar().codec_type()); 
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writePacket(AVPacket pkt) {
		AVStream outStream = outputFormatContext.streams(pkt.stream_index());
		AVRational codecTimebase = codecTimeBaseMap.get(pkt.stream_index());
		writePacket(pkt, codecTimebase,  outStream.time_base(), outStream.codecpar().codec_type()); 
	}


	private void writePacket(AVPacket pkt, final AVRational inputTimebase, final AVRational outputTimebase, int codecType) 
	{

		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index())) {
			logger.info("Not writing to muxer because it's not started for {}", url);
			return;
		}
		final AVFormatContext context = getOutputFormatContext();

		if (context == null || context.pb() == null) {
			//return if it is already null
			logger.warn("output context or .pb field is null for {}", url);
			return;
		}

		writeFrameInternal(pkt, inputTimebase, outputTimebase, context, codecType);

	}

	public void writeFrameInternal(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase,
			AVFormatContext context, int codecType) {
		
		long pts = pkt.pts();
		long dts = pkt.dts();
		long duration = pkt.duration();
		long pos = pkt.pos();
		
		pkt.pts(av_rescale_q_rnd(pkt.pts(), inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.dts(av_rescale_q_rnd(pkt.dts(), inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.duration(av_rescale_q(pkt.duration(), inputTimebase, outputTimebase));
		pkt.pos(-1);
		
		int ret = 0;

		if (codecType == AVMEDIA_TYPE_VIDEO) {
			ret = av_packet_ref(tmpPacket , pkt);
			if (ret < 0) {
				logger.error("Cannot copy packet for {}", file.getName());
				return;
			}

			if (bsfExtractdataContext != null) {

				ret = av_bsf_send_packet(bsfExtractdataContext, tmpPacket);
				if (ret < 0) {
					logger.warn("cannot send packet to the filter");
					return;
				}

				while (av_bsf_receive_packet(bsfExtractdataContext, tmpPacket) == 0) 
				{
					ret = av_write_frame(context, tmpPacket);
					if (ret < 0 && logger.isInfoEnabled()) {
						byte[] data = new byte[128];
						av_strerror(ret, data, data.length);
						logger.info("cannot write video frame to muxer. Error: {} stream: {}", new String(data, 0, data.length), file.getName());
					}	
				}
			}
			else 
			{
				ret = av_write_frame(context, tmpPacket);
				if (ret < 0 && logger.isInfoEnabled()) {
					byte[] data = new byte[128];
					av_strerror(ret, data, data.length);
					logger.info("cannot write video frame to muxer. Error: {} stream: {}", new String(data, 0, data.length), file.getName());
				}
			}

			av_packet_unref(tmpPacket);
		}
		else {
			ret = av_write_frame(context, pkt);
			if (ret < 0 && logger.isInfoEnabled()) {
				byte[] data = new byte[128];
				av_strerror(ret, data, data.length);
				logger.info("cannot write frame(not video) to muxer. Error is {} ", new String(data, 0, data.length));
			}
		}

		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);
	}
	
	boolean keyFrameReceived = false;

	@Override
	public synchronized void writeVideoBuffer(ByteBuffer encodedVideoFrame, long timestamp, int frameRotation, int streamIndex,
								 boolean isKeyFrame,long firstFrameTimeStamp) 
	{
		
		if (!isRunning.get() || !registeredStreamIndexList.contains(streamIndex)) {
			logger.info("Not writing to muxer because it's not started for {}", url);
			return;
		}
		
		if (!keyFrameReceived) {
			
			if (isKeyFrame) {
				keyFrameReceived = true;
			}
			else {
				logger.info("Frame is not key frame to start");
			}
		}
		
		if (keyFrameReceived) {
			videoPkt.stream_index(streamIndex);
			videoPkt.pts(timestamp);
			videoPkt.dts(timestamp);
			
			encodedVideoFrame.rewind();
			if (isKeyFrame) {
				videoPkt.flags(videoPkt.flags() | AV_PKT_FLAG_KEY);
			}
			videoPkt.data(new BytePointer(encodedVideoFrame));
			videoPkt.size(encodedVideoFrame.limit());
			videoPkt.position(0);
			
			AVStream outStream = outputFormatContext.streams(videoPkt.stream_index());
			AVRational codecTimebase = codecTimeBaseMap.get(videoPkt.stream_index());
			writePacket(videoPkt, codecTimebase,  outStream.time_base(), outStream.codecpar().codec_type()); 
			
			av_packet_unref(videoPkt);
		}
		
	}


	
	

}
