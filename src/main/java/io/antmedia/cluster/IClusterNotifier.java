package io.antmedia.cluster;

import java.util.List;

import io.antmedia.AppSettingsModel;


public interface IClusterNotifier {
	
	public static final String BEAN_NAME = "tomcat.cluster";
	
	public enum StreamEvent {
		STREAM_UNPUBLISHED,
		STOP,	
	}

	
	public void sendStreamNotification(String streamName, String contextName, StreamEvent event) ;

	public void sendStreamNotification(String streamId, String scopeName, List<StreamInfo> streamInfo, 
			StreamEvent streamPublished);
	
	public void stopActiveSenders(String contextName, String streamId);

	public void addMembers(List<ClusterNode> clusterNodes);

	public boolean isNodeInTheCluster(ClusterNode node);

	public void sendAppSettings(String appName, AppSettingsModel settingsModel);
}
