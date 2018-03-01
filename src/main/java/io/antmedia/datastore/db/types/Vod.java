package io.antmedia.datastore.db.types;

import java.io.Serializable;

public class Vod implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * describes vod properties it should be seperated from broadcast object
	 */

	private String streamName;
	private String vodName;
	private String streamId;
	private long creationDate;
	private long duration;
	private long fileSize;
	private String filePath;
	private String vodId;
	private String type;

	public Vod(String streamName, String streamId, String filePath, String vodName, long creationDate, long duration,
			long fileSize, String type) {

		this.streamName = streamName;
		this.streamId = streamId;
		this.vodName = vodName;
		this.creationDate = creationDate;
		this.duration = duration;
		this.filePath = filePath;
		this.fileSize = fileSize;
		this.type = type;

	}

	public Vod() {

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