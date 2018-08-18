package io.antmedia.datastore.db.types;

import java.io.Serializable;

import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;


@Entity("vod")

@Indexes({ @Index(fields = @Field("vodId")) })

public class VoD implements Serializable {

	private static final long serialVersionUID = 1L;

	
	/**
	 * The vod files that is under a folder which is set in Settings
	 */
	public static final String USER_VOD = "userVod";
	
	/**
	 * The vod files that is generated from live streams
	 */
	public static final String STREAM_VOD = "streamVod";
	
	/**
	 * The vod file user is uploaded 
	 */
	public static final String UPLOADED_VOD = "uploadedVod";
	
	@JsonIgnore
	@Id
	private ObjectId dbId;

	private String streamName;
	private String vodName;
	private String streamId;
	private long creationDate;
	private long duration;
	private long fileSize;
	private String filePath;
	private String vodId;
	private String type;
	

	public VoD() {
		//default constructor is used to return not found vod in rest service 
	}
	
	public VoD(String streamName, String streamId, String filePath, String vodName, long creationDate, long duration,
			long fileSize, String type, String vodId) {

		this.streamName = streamName;
		this.streamId = streamId;
		this.vodName = vodName;
		this.creationDate = creationDate;
		this.duration = duration;
		this.filePath = filePath;
		this.fileSize = fileSize;
		this.type = type;
		this.vodId = vodId;

	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public String getStreamName() {
		return streamName;
	}

	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}

	public String getVodName() {
		return vodName;
	}

	public void setVodName(String vodName) {
		this.vodName = vodName;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getVodId() {
		return vodId;
	}

	public void setVodId(String vodId) {
		this.vodId = vodId;
	}

}