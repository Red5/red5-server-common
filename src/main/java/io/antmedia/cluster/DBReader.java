package io.antmedia.cluster;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.antmedia.cluster.IClusterStore;
import io.antmedia.cluster.StreamInfo;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;

public class DBReader {

	public static final DBReader instance = new DBReader();
	
	ConcurrentHashMap<String, DataStore> dbMap = new ConcurrentHashMap<>();	
	IClusterStore clusterStore;
	private IClusterNotifier cluster;
	
	public String getHost(String streamName, String appName) {
		Broadcast broadcast = dbMap.get(appName).get(streamName);
		String host = null;
		if(broadcast != null) {
			host = broadcast.getOriginAdress();
		}
		return host;
	}
	
	public void addDataStore(String appName, DataStore store) {
		dbMap.put(appName, store);
	}

	public void setClusterStore(IClusterStore store) {
		this.clusterStore = store;
	}
	
	public IClusterStore getClusterStore() {
		return clusterStore;
	}

	public List<StreamInfo> getWebRTCStreamInfo(String streamId, String appName) {
		return dbMap.get(appName).getStreamInfoList(streamId);
	}
	
	public boolean isNodeInTheCluster(ClusterNode node) {
		return getCluster().isNodeInTheCluster(node);
	}

	public IClusterNotifier getCluster() {
		return cluster;
	}

	public void setCluster(IClusterNotifier cluster) {
		this.cluster = cluster;
	}
}
