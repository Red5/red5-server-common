package io.antmedia.datastore.db.types;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;

public class Endpoint 
{
	/**
	 * Service name like facebook, periscope, youtube or generic
	 * it should match the VideoServiceEndpoint names or it can be generic
	 */
	public String type;
	
	public Endpoint() {
		
	}
	public Endpoint(String broadcastId, String streamId, String name, String rtmpUrl, String type) {
		this.broadcastId = broadcastId;
		this.streamId = streamId;
		this.rtmpUrl = rtmpUrl;
		this.name = name;
		this.type = type;
	}
	public  String broadcastId;
	public  String streamId;
	public  String rtmpUrl;
	public  String name;
}