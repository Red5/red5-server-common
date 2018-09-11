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
	
	/**
	 * Broadcast id in the end point, Social endpoints has this field 
	 * but generic endpoint does not have
	 */
	private  String broadcastId;
	/**
	 * Stream id in the endpoint if exists, it may be null
	 */
	private  String streamId;
	
	/**
	 * RTMP URL of the endpoint
	 */
	private  String rtmpUrl;
	
	/**
	 * Name of the stream
	 */
	private  String name;
	
	/**
	 * Endpoint service id, this field holds the id of the endpoint
	 */
	private 	String endpointServiceId;
	
	/**
	 * Stream id in the server
	 */
	private String serverStreamId;

	/**
	 * Default constructor used in BroadcastRestService.addEndpoint
	 */
	public Endpoint() {

	}
	
	public Endpoint(String broadcastId, String streamId, String name, String rtmpUrl, String type, String endpointServiceId, String serverStreamId) {
		this.broadcastId = broadcastId;
		this.streamId = streamId;
		this.rtmpUrl = rtmpUrl;
		this.name = name;
		this.type = type;
		this.endpointServiceId = endpointServiceId;
		this.serverStreamId = serverStreamId;
	}
	public String getBroadcastId() {
		return broadcastId;
	}

	public void setBroadcastId(String broadcastId) {
		this.broadcastId = broadcastId;
	}
	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}
	public String getRtmpUrl() {
		return rtmpUrl;
	}

	public void setRtmpUrl(String rtmpUrl) {
		this.rtmpUrl = rtmpUrl;
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getEndpointServiceId() {
		return endpointServiceId;
	}

	public void setEndpointServiceId(String endpointServiceId) {
		this.endpointServiceId = endpointServiceId;
	}

	public String getServerStreamId() {
		return serverStreamId;
	}

	public void setServerStreamId(String serverStreamId) {
		this.serverStreamId = serverStreamId;
	}

}