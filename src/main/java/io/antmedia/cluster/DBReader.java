package io.antmedia.cluster;

import java.util.concurrent.ConcurrentHashMap;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;

public class DBReader {

	public static final DBReader instance = new DBReader();
	
	ConcurrentHashMap<String, DataStore> dbMap = new ConcurrentHashMap<>();	
	IClusterStore clusterStore;
	
	private DBReader() {
		//make private constructor so that nobody initialize it
	}
	//TODO move this method to datastore
	public String getHost(String streamName, String appName) {
		String host = null;
		if(dbMap.containsKey(appName)) {
			Broadcast broadcast = dbMap.get(appName).get(streamName);
			if(broadcast != null) {
				host = broadcast.getOriginAdress();
			}
		}
		return host;
	}
	

	



	
}
