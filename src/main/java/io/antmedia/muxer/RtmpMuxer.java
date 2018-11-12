package io.antmedia.muxer;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_FLAG_GLOBAL_HEADER;
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
import static org.bytedeco.javacpp.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.javacpp.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.javacpp.avutil.av_dict_free;
import static org.bytedeco.javacpp.avutil.av_dict_set;
import static org.bytedeco.javacpp.avutil.av_rescale_q;
import static org.bytedeco.javacpp.avutil.av_rescale_q_rnd;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVPacket;
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

	public RtmpMuxer(String url) {
		super(null);
		format = "flv";
		this.url = url;
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
		if (context != null && context.pb() != null) {
			//return false if it is already prepared
			return false;
		}

		AVIOContext pb = new AVIOContext(null);

		logger.info("rtmp muxer opening: {}" , url);
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
		AVStream outStream = outputFormatContext.streams(pkt.stream_index());
		writePacket(pkt, stream.time_base(),  outStream.time_base()); 
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writePacket(AVPacket pkt) {
		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index())) {
			return;
		}
		AVStream outStream = outputFormatContext.streams(pkt.stream_index());
		writePacket(pkt, outStream.codec().time_base(),  outStream.time_base()); 
	}


	private void writePacket(AVPacket pkt, final AVRational inputTimebase, final AVRational outputTimebase) 
	{

		final AVFormatContext context = getOutputFormatContext();

		if (context == null || context.pb() == null) {
			//return if it is already null
			logger.warn("output context or .pb field is null for {}", url);
			return;
		}

		int packetIndex = pkt.stream_index();
		//TODO: find a better frame to check if stream exists in outputFormatContext
		if (!registeredStreamIndexList.contains(packetIndex)) {
			return;
		}

		writeFrameInternal(pkt, inputTimebase, outputTimebase, context);

	}

	public void writeFrameInternal(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase,
			AVFormatContext context) {
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
			logger.warn("cannot write frame to muxer for {}", url); 
		}

		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);
	}

}
