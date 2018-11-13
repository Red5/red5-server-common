package io.antmedia.cluster;

import java.util.List;


public interface IClusterNotifier {
	
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
}
