package io.antmedia.muxer;

import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;

public interface IStreamAcceptFilter {
	
	public static final String BEAN_NAME = "streamAcceptFilter";
	
	public boolean isValidStreamParameters(AVFormatContext inputFormatContext,AVPacket pkt, String streamId);
	
	
	
}
