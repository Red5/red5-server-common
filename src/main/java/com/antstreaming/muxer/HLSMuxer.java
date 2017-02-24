package com.antstreaming.muxer;

import org.red5.server.api.IConnection;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.CachedEvent;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.AVFMT_GLOBALHEADER;
import static org.bytedeco.javacpp.avformat.AVFMT_NOFILE;
import static org.bytedeco.javacpp.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.javacpp.avformat.av_read_frame;
import static org.bytedeco.javacpp.avformat.av_write_frame;
import static org.bytedeco.javacpp.avformat.av_write_trailer;
import static org.bytedeco.javacpp.avformat.avformat_alloc_output_context2;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_free_context;
import static org.bytedeco.javacpp.avformat.avformat_new_stream;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avformat.avformat_write_header;
import static org.bytedeco.javacpp.avformat.avio_alloc_context;
import static org.bytedeco.javacpp.avformat.avio_closep;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;

import static org.bytedeco.javacpp.avutil.*;

import org.bytedeco.javacpp.avcodec.AVBSFContext;
import org.bytedeco.javacpp.avcodec.AVBitStreamFilter;
import org.bytedeco.javacpp.avcodec.AVBitStreamFilterContext;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVInputFormat;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avformat.Read_packet_Pointer_BytePointer_int;
import org.bytedeco.javacpp.avutil.AVDictionary;


public class HLSMuxer extends AbstractMuxer  {


	private AVBitStreamFilter h264bsfc;
	private AVBSFContext bsfContext;
	private long lastDTS = -1; 

	private List<Integer> registeredStreamIndexList = new ArrayList<>();

	protected static Logger logger = LoggerFactory.getLogger(HLSMuxer.class);



	public HLSMuxer() {
		extension = ".m3u8";
		format = "hls";
		options.put("hls_wrap", "40");

	}

	public boolean prepare(AVFormatContext inputFormatContext) {
		h264bsfc = av_bsf_get_by_name("h264_mp4toannexb");
		bsfContext = new AVBSFContext();
		int ret = av_bsf_alloc(h264bsfc, bsfContext);
		if (ret < 0) {
			logger.info("cannot allocate bsf context");
			return false;
		}

		outputFormatContext= new AVFormatContext(null);
		ret = avformat_alloc_output_context2(outputFormatContext, null, format, file.getAbsolutePath());
		if (ret < 0) {
			logger.info("Could not create output context\n");
			return false;
		}

		for (int i=0; i < inputFormatContext.nb_streams(); i++) {
			AVStream in_stream = inputFormatContext.streams(i);
			if (isCodecSupported(in_stream.codecpar())) {
				registeredStreamIndexList.add(i);
				AVStream out_stream = avformat_new_stream(outputFormatContext, in_stream.codec().codec());

				if (in_stream.codec().codec_type() == AVMEDIA_TYPE_VIDEO) {
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

					ret = avcodec_parameters_copy(out_stream.codecpar(), in_stream.codecpar());
					if (ret < 0) {
						logger.info("Cannot get codec parameters\n");
						return false;
					}

				}
				out_stream.codec().codec_tag(0);

				if ((outputFormatContext.oformat().flags() & AVFMT_GLOBALHEADER) != 0)
					out_stream.codec().flags( out_stream.codec().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
			}
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

	private boolean isCodecSupported(AVCodecParameters codecpar) {
		if (codecpar.codec_id() == AV_CODEC_ID_H264 || 
				codecpar.codec_id() == AV_CODEC_ID_AAC) {
			return true;
		}
		return false;
	}

	@Override
	public void writePacket(AVPacket pkt, AVStream inStream) 
	{
		int packetIndex = pkt.stream_index();
		
		//TODO: find a better frame to check if stream exists in outputFormatContext
		if (!registeredStreamIndexList.contains(packetIndex))  {
			return;
		}
		
		AVStream out_stream = outputFormatContext.streams(packetIndex);

		int ret;

		if (inStream.codec().codec_type() ==  AVMEDIA_TYPE_VIDEO) 
		{
			IntPointer intPointer = new IntPointer();
			BytePointer bytePointer = new BytePointer();

			ret = av_bsf_send_packet(bsfContext, pkt);
			if (ret < 0)
				return;

			while ((ret = av_bsf_receive_packet(bsfContext, pkt)) == 0) 
			{
				pkt.pts(av_rescale_q_rnd(pkt.pts(), inStream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
				pkt.dts(av_rescale_q_rnd(pkt.dts(), inStream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
				pkt.duration(av_rescale_q(pkt.duration(), inStream.time_base(), out_stream.time_base()));
				pkt.pos(-1);

				if (lastDTS >= pkt.dts()) {
					pkt.dts(lastDTS + 1);
				}
				if (pkt.dts() > pkt.pts()) {
					pkt.pts(pkt.dts());
				}
				lastDTS = pkt.dts();

				ret = av_write_frame(outputFormatContext, pkt);
				if (ret < 0) {
					logger.info("cannot write frame to muxer");
				}
			}
		}
		else {
			pkt.pts(av_rescale_q_rnd(pkt.pts(), inStream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.dts(av_rescale_q_rnd(pkt.dts(), inStream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.duration(av_rescale_q(pkt.duration(), inStream.time_base(), out_stream.time_base()));
			pkt.pos(-1);

			if (lastDTS >= pkt.dts()) {
				pkt.dts(lastDTS + 1);
			}
			if (pkt.dts() > pkt.pts()) {
				pkt.pts(pkt.dts());
			}
			lastDTS = pkt.dts();

			ret = av_write_frame(outputFormatContext, pkt);
			if (ret < 0) {
				logger.info("cannot write frame to muxer");
			}
		}

	}


	@Override
	public void writeTrailer() {
		logger.info("write trailer...");

		av_bsf_free(bsfContext);

		av_write_trailer(outputFormatContext);

		/* close output */
		if ((outputFormatContext.flags() & AVFMT_NOFILE) == 0)
			avio_closep(outputFormatContext.pb());

		avformat_free_context(outputFormatContext);
		isRecording = false;	
	}



}
