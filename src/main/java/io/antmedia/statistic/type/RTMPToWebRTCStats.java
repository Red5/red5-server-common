package io.antmedia.statistic.type;

public class RTMPToWebRTCStats 
{
	private String streamId;

	private long avgRtmpIngestionTime;
	
	private long totalVideoDecodeTime;
	private long totalDecodedVideoFrameCount;

	private long totalVideoEncodeTime;
	private long totalEncodedVideoPacketCount;
	
	private long totalVideoDeliveryTime;
	private long totalDeliveredVideoPacketCount;
	
	public RTMPToWebRTCStats(String streamId) {
		this.setStreamId(streamId);
	}
	public long getAvgRtmpIngestionTime() {
		return avgRtmpIngestionTime;
	}
	public void setAvgRtmpIngestionTime(long avgRtmpIngestionTime) {
		this.avgRtmpIngestionTime = avgRtmpIngestionTime;
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
	public String getReport() {
		return "avgRtmpIngestionTime:"+avgRtmpIngestionTime+"\n"+
				"totalVideoDecodeTime:"+totalVideoDecodeTime+"\n"+
				"totalDecodedVideoFrameCount:"+totalDecodedVideoFrameCount+"\n"+
				"totalVideoEncodeTime:"+totalVideoEncodeTime+"\n"+
				"totalEncodedVideoPacketCount:"+totalEncodedVideoPacketCount+"\n"+
				"totalVideoDeliveryTime:"+totalVideoDeliveryTime+"\n"+
				"totalDeliveredVideoPacketCount:"+totalDeliveredVideoPacketCount+"\n";
	}
	public String getStreamId() {
		return streamId;
	}
	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}
	
}
