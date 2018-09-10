package io.antmedia.datastore.db.types;

import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity("broadcast")

@Indexes({ @Index(fields = @Field("name")), @Index(fields = @Field("streamId")) })

public class Broadcast {

	/**
	 * id of the broadcast
	 */
	@JsonIgnore
	@Id
	private ObjectId dbId;

	private String streamId;

	/**
	 * "finished", "broadcasting", "created"
	 */
	private String status;

	/**
	 * "liveStream", "ipCamera", "streamSource", "vod"
	 */

	private String type;

	/**
	 * name of the broadcast
	 */
	private String name;

	/**
	 * description of the broadcast
	 */
	private String description;

	/**
	 * It is a video filter for the service, this value is controlled by the
	 * user, default value is true in the db
	 */
	private boolean publish = true;

	/**
	 * date when record is created in milliseconds
	 */
	private Long date;

	/**
	 * Planned start date
	 */
	private Long plannedStartDate;

	/**
	 * duration of the stream in milliseconds
	 */
	private Long duration;

	@Embedded
	private List<Endpoint> endPointList;

	/**
	 * is public
	 */
	private boolean publicStream = true;

	/**
	 * If this stream is a 360 degree video
	 */
	private boolean is360 = false;

	/**
	 * This is the url that will be notified when stream is published, ended and
	 * muxing finished
	 * 
	 * It sends some variables with POST UrlEncodedForm
	 * 
	 * variables are "id" mandatory This is the id of the broadcast
	 * 
	 * "action" mandatory This parameter defines what happened. Values can be
	 * "liveStreamStarted" this parameter is sent when stream is started
	 * 
	 * "liveStreamEnded" this parameter is sent when stream is finished
	 * 
	 * "vodReady" this parameter is sent when vod(mp4) file ready. It is
	 * typically a few seconds later after "liveStreamEnded"
	 * 
	 * 
	 * "vodName" It is sent with "vodReady" action. This is the name of the file
	 * physicall recorded file
	 * 
	 * "streamName" optional It is sent with above parameters if stream name
	 * exists
	 * 
	 * "category" optional It is sent if category exists
	 * 
	 */
	private String listenerHookURL;

	private String category;

	private String ipAddr;
	
	private String username;
	
	private String password;

	private String quality;
	private double speed;
	
	/**
	 * This is the stream url for fetching stream. 
	 * It has a value for IPCameras and streams in the cloud
	 */
	private String streamUrl;

	public Broadcast() {
		this.type = "liveStream";
	}

	/**
	 * This is the expire time in milliseconds For instance if this value is
	 * 10000 then broadcast should be started in 10 seconds after it is created.
	 * 
	 * If expire duration is 0, then stream will never expire
	 */
	private int expireDurationMS;

	/**
	 * RTMP URL where to publish live stream to
	 */
	private String rtmpURL;

	/**
	 * zombi It is true, if a broadcast that is not added to data store through
	 * rest service or management console It is false by default
	 * 
	 */
	private boolean zombi = false;
	
	/**

	 * Number of audio and video packets that is being pending to be encoded 
	 * in the queue
	 */
	private int pendingPacketSize = 0;

	/**
	 * number of hls viewers of the stream
	 */
	private int hlsViewerCount = 0;

	private int webRTCViewerCount = 0;
	private int rtmpViewerCount = 0;

	public Broadcast(String status, String name) {
		this.setStatus(status);
		this.setName(name);
		this.type = "liveStream";
	}

	public Broadcast(String name) {

		this.name = name;
		this.type = "liveStream";
	}

	public Broadcast(String name, String ipAddr, String username, String password, String rtspUrl, String type) {

		this.name = name;
		this.ipAddr = ipAddr;
		this.username = username;
		this.password = password;
		this.streamUrl = rtspUrl;
		this.type = type;
	}

	public String getStreamId() {

		if (streamId != null) {
			return streamId;
		}
		if (dbId == null) {
			return null;
		}
		return dbId.toString();

	}

	public void setStreamId(String id) throws Exception {
		if (id == null) {
			throw new Exception("stream id cannot be null");
		}
		this.streamId = id;
	}
	

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isPublish() {
		return publish;
	}

	public void setPublish(boolean publish) {
		this.publish = publish;
	}

	public Long getDate() {
		return date;
	}

	public void setDate(Long date) {
		this.date = date;
	}

	public Long getPlannedStartDate() {
		return plannedStartDate;
	}

	public void setPlannedStartDate(Long plannedStartDate) {
		this.plannedStartDate = plannedStartDate;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public List<Endpoint> getEndPointList() {
		return endPointList;
	}

	public void setEndPointList(List<Endpoint> endPointList) {
		this.endPointList = endPointList;
	}

	public boolean isIs360() {
		return is360;
	}

	public void setIs360(boolean is360) {
		this.is360 = is360;
	}

	public boolean isPublicStream() {
		return publicStream;
	}

	public void setPublicStream(boolean publicStream) {
		this.publicStream = publicStream;
	}

	public String getListenerHookURL() {
		return listenerHookURL;
	}

	public void setListenerHookURL(String listenerHookURL) {
		this.listenerHookURL = listenerHookURL;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getIpAddr() {
		return ipAddr;
	}

	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}


	public int getExpireDurationMS() {
		return expireDurationMS;
	}

	public void setExpireDurationMS(int expireDurationMS) {
		this.expireDurationMS = expireDurationMS;
	}

	public String getRtmpURL() {
		return rtmpURL;
	}

	public void setRtmpURL(String rtmpURL) {
		this.rtmpURL = rtmpURL;
	}

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public boolean isZombi() {
		return zombi;
	}

	public void setZombi(boolean zombi) {
		this.zombi = zombi;
	}

	public void resetStreamId() {
		this.streamId = null;
	}
	
	public String getStreamUrl() {
		return streamUrl;
	}

	public void setStreamUrl(String streamUrl) {
		this.streamUrl = streamUrl;
	}
	public int getHlsViewerCount() {
		return hlsViewerCount;
	}

	public void setHlsViewerCount(int hlsViewerCount) {
		this.hlsViewerCount = hlsViewerCount;
	}

	public int getWebRTCViewerCount() {
		return webRTCViewerCount;
	}

	public void setWebRTCViewerCount(int webRTCViewerCount) {
		this.webRTCViewerCount = webRTCViewerCount;
	}

	public int getRtmpViewerCount() {
		return rtmpViewerCount;
	}

	public void setRtmpViewerCount(int rtmpViewerCount) {
		this.rtmpViewerCount = rtmpViewerCount;
	}

	public int getPendingPacketSize() {
		return pendingPacketSize;
	}

	public void setPendingPacketSize(int pendingPacketSize) {
		this.pendingPacketSize = pendingPacketSize;
	}

}