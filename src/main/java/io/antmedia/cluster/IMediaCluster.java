package io.antmedia.cluster;

public interface IMediaCluster {
	
	public static final String BEAN_NAME = "tomcat.cluster";
	
	
	public StreamNotificationMessage getStreamNotification(String streamName, String contextName);

}
