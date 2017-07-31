package io.antmedia.cluster;

import org.apache.catalina.ha.ClusterMessageBase;
import org.apache.catalina.tribes.Member;

import io.antmedia.cluster.IClusterNotifier.StreamEvent;

public class StreamNotificationMessage extends ClusterMessageBase {


	private StreamEvent event;
	private String contextName;
	private String streamName;
	
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
	
	

}
