package io.antmedia.muxer;

import org.bytedeco.ffmpeg.global.*;
import org.bytedeco.ffmpeg.avcodec.*;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.avutil.*;
import org.bytedeco.ffmpeg.swresample.*;
import org.bytedeco.ffmpeg.swscale.*;

import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avdevice.*;
import static org.bytedeco.ffmpeg.global.swresample.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

public interface IStreamAcceptFilter {
	
	public static final String BEAN_NAME = "streamAcceptFilter";
	
	public boolean isValidStreamParameters(AVFormatContext inputFormatContext,AVPacket pkt);
	
	
	
}
