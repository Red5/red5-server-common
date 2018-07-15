package io.antmedia.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.catalina.ha.ClusterMessageBase;
import org.apache.catalina.tribes.Member;

import io.antmedia.EncoderSettings;
import io.antmedia.cluster.IClusterNotifier.StreamEvent;

public class StreamNotificationMessage extends ClusterMessageBase {


	private StreamEvent event;
	private String contextName;
	private String streamName;
	private boolean multicastEnabled = false;
	
	private Map<Integer, Integer[]> streamPortMap = null;
	private List<StreamInfo> streamInfo;
	
	public StreamNotificationMessage(Member source, String streamName, String contextName, StreamEvent event ) {
		this.address = source;
		this.setTimestamp(System.currentTimeMillis());
		this.setStreamName(streamName);
		this.setContextName(contextName);
		this.setEvent(event);
	}
	
	
	@Override
	public String getUniqueId() {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append(address.getHost());
		strBuilder.append(getTimestamp());
		return strBuilder.toString();
	}



	public StreamEvent getEvent() {
		return event;
	}



	public void setEvent(StreamEvent event) {
		this.event = event;
	}



	public String getContextName() {
		return contextName;
	}



	public void setContextName(String contextName) {
		this.contextName = contextName;
	}



	public String getStreamName() {
		return streamName;
	}



	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}




	public boolean isMulticastEnabled() {
		return multicastEnabled;
	}


	public void setMulticastEnabled(boolean multicastEnabled) {
		this.multicastEnabled = multicastEnabled;
	}


	public void setStreamPortMap(Map<Integer, Integer[]> streamPortMap) {
		this.streamPortMap = streamPortMap;
	}
	
	public Map<Integer, Integer[]> getStreamPortMap() {
		return streamPortMap;
	}


	public void setStreamInfo(List<StreamInfo> streamInfo) {
		this.streamInfo = streamInfo;
	}


	public List<StreamInfo> getStreamInfo() {
		return streamInfo;
	}
	

}
