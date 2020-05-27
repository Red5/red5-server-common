package io.antmedia.muxer;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_AV1;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_OPUS;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_VORBIS;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_VP8;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_VP9;

import org.red5.server.scheduling.QuartzSchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.storage.StorageClient;

public class WebMMuxer extends RecordMuxer {

	protected static Logger logger = LoggerFactory.getLogger(WebMMuxer.class);
	
	private static int[] WEBM_SUPPORTED_CODECS = {
			AV_CODEC_ID_VP8			 ,
			AV_CODEC_ID_VP9			 ,
			AV_CODEC_ID_AV1			 ,
			AV_CODEC_ID_VORBIS       , 
			AV_CODEC_ID_OPUS
	};

	public WebMMuxer(StorageClient storageClient, QuartzSchedulingService scheduler) {
		super(storageClient, scheduler);
		extension = ".webm";
		format = "webm";
		SUPPORTED_CODECS = WEBM_SUPPORTED_CODECS;
	}
}
