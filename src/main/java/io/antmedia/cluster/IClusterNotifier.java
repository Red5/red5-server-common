package io.antmedia.cluster;


public interface IClusterNotifier {
	
	public enum StreamEvent {
		STREAM_PUBLISHED,
		STREAM_UNPUBLISHED, 
		STREAM_IDENTIFIER_NOTIFICATION,
	}
	
	public void sendStreamNotification(String streamName, String contextName, StreamEvent event) ;

}
