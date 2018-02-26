package io.antmedia.cluster;

import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.ha.ClusterMessageBase;
import org.apache.catalina.tribes.Member;

import io.antmedia.cluster.IClusterNotifier.StreamEvent;
import io.antmedia.cluster.StreamNotificationMessage.StreamIdentifier;

public class StreamNotificationMessage extends ClusterMessageBase {


	private StreamEvent event;
	private String contextName;
	private String streamName;
	
	public static class StreamIdentifier {
		public int videoBitrate;
		public String videoMulticastAddr;
		public int videoMulticastPort;
		
		public int audioBitrate;
		public String audioMulticastAddr;
		public int audioMulticastPort;
	}
	
	private List<StreamIdentifier> streamIdentifier = new ArrayList<StreamIdentifier>();
	
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


	public List<StreamIdentifier> getStreamIdentifier() {
		return streamIdentifier;
	}


	public void setStreamIdentifier(List<StreamIdentifier> streamIdentifier) {
		this.streamIdentifier = streamIdentifier;
	}
	
	

}
