package io.antmedia.cluster;


public interface IClusterNotifier {
	
	public enum StreamEvent {
		STREAM_PUBLISHED,
		STREAM_UNPUBLISHED,
	}
	
	public void sendStreamNotification(String streamName, String contextName, StreamEvent event) ;

}
