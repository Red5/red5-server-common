package io.antmedia.cluster;

import java.util.List;

public interface IClusterNotifier {
	
	public enum StreamEvent {
		STREAM_PUBLISHED,
		STREAM_UNPUBLISHED,
		PLAY, STOP,	
	}

	
	public void sendStreamNotification(String streamName, String contextName, StreamEvent event) ;

	public void sendStreamNotification(String streamId, String scopeName, List<StreamInfo> streamInfo, 
			StreamEvent streamPublished);
	
	public void stopActiveSenders(String contextName, String streamId);

}
