package io.antmedia.statistic.type;

import java.math.BigInteger;

public class WebRTCVideoSendStats 
{
	long videoFirCount;
	long videoPliCount;
	long videoNackCount;
	long videoPacketsSent;
	private long videoFramesEncoded;
	BigInteger videoBytesSent = BigInteger.ZERO;
	private double videoPacketsSentPerSecond;
	private BigInteger videoBytesSentPerSecond;
	private double videoFramesEncodedPerSecond;
	private long timeMs;
	
	public long getVideoFirCount() {
		return videoFirCount;
	}

	public void setVideoFirCount(long videoFirCount) {
		this.videoFirCount = videoFirCount;
	}

	public long getVideoPliCount() {
		return videoPliCount;
	}

	public void setVideoPliCount(long videoPliCount) {
		this.videoPliCount = videoPliCount;
	}

	public long getVideoNackCount() {
		return videoNackCount;
	}

	public void setVideoNackCount(long videoNackCount) {
		this.videoNackCount = videoNackCount;
	}

	public long getVideoPacketsSent() {
		return videoPacketsSent;
	}

	public void setVideoPacketsSent(long videoPacketsSent) {
		this.videoPacketsSent = videoPacketsSent;
	}


	public BigInteger getVideoBytesSent() {
		return videoBytesSent;
	}

	public void setVideoBytesSent(BigInteger videoBytesSent) {
		this.videoBytesSent = videoBytesSent;
	}

	public long getVideoFramesEncoded() {
		return videoFramesEncoded;
	}

	public void setVideoFramesEncoded(long videoFramesEncoded) {
		this.videoFramesEncoded = videoFramesEncoded;
	}
	
	public void addVideoStats(WebRTCVideoSendStats videoStats) 
	{
		this.videoFirCount += videoStats.getVideoFirCount();
		this.videoPliCount = videoStats.getVideoPliCount();
		this.videoNackCount += videoStats.getVideoNackCount();
		this.videoPacketsSent += videoStats.getVideoPacketsSent();
		this.videoBytesSent = this.videoBytesSent.add(videoStats.getVideoBytesSent());
		this.videoFramesEncoded += videoStats.getVideoFramesEncoded();
	}

	public void setVideoPacketsSentPerSecond(double d) {
		this.videoPacketsSentPerSecond = d;
	}
	
	public double getVideoPacketsSentPerSecond() {
		return videoPacketsSentPerSecond;
	}

	public void setVideoBytesSentPerSecond(BigInteger bytesPerSecond) {
		this.videoBytesSentPerSecond = bytesPerSecond;
	}
	
	public BigInteger getVideoBytesSentPerSecond() {
		return videoBytesSentPerSecond;
	}

	public void setVideoFramesEncodedPerSecond(double d) {
		this.videoFramesEncodedPerSecond = d;
	}
	
	public double getVideoFramesEncodedPerSecond() {
		return videoFramesEncodedPerSecond;
	}

	public void setTimeMs(long timeMs) {
		this.timeMs = timeMs;
	}
	
	public long getTimeMs() {
		return timeMs;
	}
}
