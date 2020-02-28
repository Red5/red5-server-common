package io.antmedia.statistic.type;

public class RTMPToWebRTCStats 
{
	private String streamId;
	private int encoderCount;

	private long totalVideoIngestTime;
	private long totalIngestedVideoPacketCount;
	
	private long totalVideoDecodeTime;
	private long totalDecodedVideoFrameCount;

	private long totalVideoEncodeQueueTime;
	private long totalVideoEncodeTime;
	private long totalEncodedVideoPacketCount;
	
	private long totalVideoDeliveryTime;
	private long totalDeliveredVideoPacketCount;
	
	public RTMPToWebRTCStats(String streamId) {
		this.setStreamId(streamId);
	}
	public long getTotalVideoDecodeTime() {
		return totalVideoDecodeTime;
	}
	public void setTotalVideoDecodeTime(long totalVideoDecodeTime) {
		this.totalVideoDecodeTime = totalVideoDecodeTime;
	}
	public long getTotalDecodedVideoFrameCount() {
		return totalDecodedVideoFrameCount;
	}
	public void setTotalDecodedVideoFrameCount(long totalDecodedVideoFrameCount) {
		this.totalDecodedVideoFrameCount = totalDecodedVideoFrameCount;
	}
	public long getTotalVideoEncodeTime() {
		return totalVideoEncodeTime;
	}
	public void setTotalVideoEncodeTime(long totalVideoEncodeTime) {
		this.totalVideoEncodeTime = totalVideoEncodeTime;
	}
	public long getTotalEncodedVideoPacketCount() {
		return totalEncodedVideoPacketCount;
	}
	public void setTotalEncodedVideoPacketCount(long totalEncodedVideoPacketCount) {
		this.totalEncodedVideoPacketCount = totalEncodedVideoPacketCount;
	}
	public long getTotalVideoDeliveryTime() {
		return totalVideoDeliveryTime;
	}
	public void setTotalVideoDeliveryTime(long totalVideoDeliveryTime) {
		this.totalVideoDeliveryTime = totalVideoDeliveryTime;
	}
	public long getTotalDeliveredVideoPacketCount() {
		return totalDeliveredVideoPacketCount;
	}
	public void setTotalDeliveredVideoPacketCount(long totalDeliveredVideoPacketCount) {
		this.totalDeliveredVideoPacketCount = totalDeliveredVideoPacketCount;
	}
	public String getStreamId() {
		return streamId;
	}
	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}
	public long getTotalVideoIngestTime() {
		return totalVideoIngestTime;
	}
	public void setTotalVideoIngestTime(long totalVideoIngestTime) {
		this.totalVideoIngestTime = totalVideoIngestTime;
	}
	public long getTotalIngestedVideoPacketCount() {
		return totalIngestedVideoPacketCount;
	}
	public void setTotalIngestedVideoPacketCount(long totalIngestedVideoPacketCount) {
		this.totalIngestedVideoPacketCount = totalIngestedVideoPacketCount;
	}
	public int getEncoderCount() {
		return encoderCount;
	}
	public void setEncoderCount(int encoderCount) {
		this.encoderCount = encoderCount;
	}
	public long getTotalVideoEncodeQueueTime() {
		return totalVideoEncodeQueueTime;
	}
	public void setTotalVideoEncodeQueueTime(long totalVideoEncodeQueueTime) {
		this.totalVideoEncodeQueueTime = totalVideoEncodeQueueTime;
	}
	
}
