package io.antmedia.statistic.type;

import java.math.BigInteger;

public class WebRTCAudioSendStats 
{
	private long audioPacketsSent;
	private BigInteger audioBytesSent = BigInteger.ZERO;
	
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
}
