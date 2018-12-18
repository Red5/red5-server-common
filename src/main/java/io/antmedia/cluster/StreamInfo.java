package io.antmedia.cluster;

import java.io.Serializable;

public class StreamInfo implements IStreamInfo, Serializable {
	
	private String streamId;
	private int height;
	private int width;
	private int videoBitrate;
	private int audioBitrate;
	private int videoRTimebase;
	private int audioRTimebase;
	private String host;
	private int videoPort;
	private int audioPort;

	public StreamInfo(int height, int width, int videobitrate, int audiobitrate, int videoRTimebase, int audioRTimebase) {
		this.height = height;
		this.width = width;
		this.videoBitrate = videobitrate;
		this.audioBitrate = audiobitrate;
		this.videoRTimebase = videoRTimebase;
		this.audioRTimebase = audioRTimebase;
	}
	
	public StreamInfo() {
		
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getVideoBitrate() {
		return videoBitrate;
	}

	@Override
	public int getAudioBitrate() {
		return audioBitrate;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setVideoBitrate(int videoBitrate) {
		this.videoBitrate = videoBitrate;
	}

	public void setAudioBitrate(int audioBitrate) {
		this.audioBitrate = audioBitrate;
	}

	public void setVideoRTimebase(int rtimebase) {
		this.videoRTimebase = rtimebase;
	}

	public void setAudioRTimebase(int rtimebase) {
		this.audioRTimebase = rtimebase;
	}
	
	public int getVideoRTimebase() {
		return videoRTimebase;
	}
	
	public int getAudioRTimebase() {
		return audioRTimebase;
	}
	
	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public int getVideoPort() {
		return videoPort;
	}

	public void setVideoPort(int videoPort) {
		this.videoPort = videoPort;
	}

	public int getAudioPort() {
		return audioPort;
	}

	public void setAudioPort(int audioPort) {
		this.audioPort = audioPort;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

}
