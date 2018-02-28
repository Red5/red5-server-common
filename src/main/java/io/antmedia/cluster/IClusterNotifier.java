package io.antmedia.cluster;

import java.io.Serializable;
import java.util.List;

import io.antmedia.EncoderSettings;

public interface IClusterNotifier {
	
	public enum StreamEvent {
		STREAM_PUBLISHED,
		STREAM_UNPUBLISHED,
		PLAY,	
	}
	
	public static class StreamInfo implements Serializable {
		public int width;
		public int height;
		public int bitrate;
		public int rtimebase;
	}
	
	public void sendStreamNotification(String streamName, String contextName, StreamEvent event) ;

	public void sendStreamNotification(String streamId, String scopeName, List<StreamInfo> videoStreamInfo, List<StreamInfo> audioStreamInfo,
			StreamEvent streamPublished);

}
