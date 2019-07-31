package io.antmedia.statistic.type;

import java.math.BigInteger;

public class WebRTCVideoReceiveStats 
{
	long videoFirCount;
	long videoPliCount;
	long videoNackCount;
	long videoPacketsReceived;
	int videoPacketsLost;
	double videoFractionLost;
	long videoFrameReceived;
	BigInteger videoBytesReceived = BigInteger.ZERO;
	
	public long getVideoFirCount() {
		return videoFirCount;
	}
	public void setVideoFirCount(long videoFirCountDelta) {
		this.videoFirCount = videoFirCountDelta;
	}
	public long getVideoPliCount() {
		return videoPliCount;
	}
	public void setVideoPliCount(long videoPliCountDelta) {
		this.videoPliCount = videoPliCountDelta;
	}
	public long getVideoNackCount() {
		return videoNackCount;
	}
	public void setVideoNackCount(long videoNackCountDelta) {
		this.videoNackCount = videoNackCountDelta;
	}
	public long getVideoPacketsReceived() {
		return videoPacketsReceived;
	}
	public void setVideoPacketsReceived(long videoPacketsReceivedDelta) {
		this.videoPacketsReceived = videoPacketsReceivedDelta;
	}
	public int getVideoPacketsLost() {
		return videoPacketsLost;
	}
	public void setVideoPacketsLost(int videoPacketsLostDelta) {
		this.videoPacketsLost = videoPacketsLostDelta;
	}
	public double getVideoFractionLost() {
		return videoFractionLost;
	}
	public void setVideoFractionLost(double videoFractionLostDelta) {
		this.videoFractionLost = videoFractionLostDelta;
	}
	public long getVideoFrameReceived() {
		return videoFrameReceived;
	}
	public void setVideoFrameReceived(long videFrameReceivedDelta) {
		this.videoFrameReceived = videFrameReceivedDelta;
	}
	public BigInteger getVideoBytesReceived() {
		return videoBytesReceived;
	}
	public void setVideoBytesReceived(BigInteger videoBytesReceivedDelta) {
		this.videoBytesReceived = videoBytesReceivedDelta;
	}
	public void addVideoStats(WebRTCVideoReceiveStats videoStats) 
	{
		this.videoFirCount += videoStats.getVideoFirCount();
		this.videoPliCount = videoStats.getVideoPliCount();
		this.videoNackCount += videoStats.getVideoNackCount();
		this.videoPacketsReceived += videoStats.getVideoPacketsReceived();
		this.videoPacketsLost += videoStats.getVideoPacketsLost();
		this.videoFractionLost += videoStats.getVideoFractionLost();
		this.videoFrameReceived += videoStats.getVideoFrameReceived();
		this.videoBytesReceived = this.videoBytesReceived.add(videoStats.getVideoBytesReceived());
	}
}
