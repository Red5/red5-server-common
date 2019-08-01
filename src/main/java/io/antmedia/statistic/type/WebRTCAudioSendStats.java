package io.antmedia.statistic.type;

import java.math.BigInteger;

public class WebRTCAudioSendStats 
{
	private long audioPacketsSent;
	private BigInteger audioBytesSent = BigInteger.ZERO;
	private double audioPacketsPerSecond;
	private BigInteger audioBytesSentPerSecond;
	/**
	 * The moment in these stats is captured in milliSeconds
	 */
	private long timeMs;
	
	public long getAudioPacketsSent() {
		return audioPacketsSent;
	}

	public void setAudioPacketsSent(long audioPacketsSent) {
		this.audioPacketsSent = audioPacketsSent;
	}

	public BigInteger getAudioBytesSent() {
		return audioBytesSent;
	}


	public void setAudioBytesSent(BigInteger bigInteger) {
		this.audioBytesSent = bigInteger;
	}
	
	public void addAudioStats(WebRTCAudioSendStats audioStats) 
	{
		this.audioPacketsSent += audioStats.getAudioPacketsSent();
		this.audioBytesSent = this.audioBytesSent.add(audioStats.getAudioBytesSent());
	}

	public void setAudioPacketsSentPerSecond(double d) {
		this.audioPacketsPerSecond = d;
	}
	
	public double getAudioPacketsPerSecond() {
		return audioPacketsPerSecond;
	}

	public void setAudioBytesSentPerSecond(BigInteger audioBytesPerSecond) {
		this.audioBytesSentPerSecond = audioBytesPerSecond;
	}
	
	public BigInteger getAudioBytesSentPerSecond() {
		return audioBytesSentPerSecond;
	}

	public void setTimeMs(long timeMs) {
		this.timeMs = timeMs;
	}
	
	public long getTimeMs() {
		return timeMs;
	}
}
