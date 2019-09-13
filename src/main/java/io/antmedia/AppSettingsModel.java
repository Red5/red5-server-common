package io.antmedia;

import java.util.List;

public class AppSettingsModel {
	
	private boolean mp4MuxingEnabled;
	private boolean addDateTimeToMp4FileName;
	private boolean hlsMuxingEnabled;
	private boolean objectDetectionEnabled;
	private int hlsListSize;
	private int hlsTime;
	private String hlsPlayListType;
	private String facebookClientId;
	private String facebookClientSecret;
	private String youtubeClientId;
	private String youtubeClientSecret;
	private String periscopeClientId;
	private String periscopeClientSecret;
	private boolean acceptOnlyStreamsInDataStore;
	private boolean tokenControlEnabled ;
	private List<EncoderSettings> encoderSettings;
	private String vodFolder;
	private boolean previewOverwrite;
	private boolean webRTCEnabled;
	private int webRTCFrameRate;
	private String tokenHashSecret;
	private boolean hashControlPublishEnabled;
	private boolean hashControlPlayEnabled;
	
	/**
	 * Comma separated allowed CIDR
	 */
	private String remoteAllowedCIDR;

	

	
	public String getTokenHashSecret() {
		return tokenHashSecret;
	}

	public void setTokenHashSecret(String tokenHashSecret) {
		this.tokenHashSecret = tokenHashSecret;
	}

	public boolean isHashControlPublishEnabled() {
		return hashControlPublishEnabled;
	}

	public void setHashControlPublishEnabled(boolean hashControlPublishEnabled) {
		this.hashControlPublishEnabled = hashControlPublishEnabled;
	}

	public boolean isHashControlPlayEnabled() {
		return hashControlPlayEnabled;
	}

	public void setHashControlPlayEnabled(boolean hashControlPlayEnabled) {
		this.hashControlPlayEnabled = hashControlPlayEnabled;
	}
	
	public boolean isWebRTCEnabled() {
		return webRTCEnabled;
	}

	public void setWebRTCEnabled(boolean webRTCEnabled) {
		this.webRTCEnabled = webRTCEnabled;
	}

	public boolean isMp4MuxingEnabled() {
		return mp4MuxingEnabled;
	}

	public void setMp4MuxingEnabled(boolean mp4MuxingEnabled) {
		this.mp4MuxingEnabled = mp4MuxingEnabled;
	}

	public boolean isAddDateTimeToMp4FileName() {
		return addDateTimeToMp4FileName;
	}

	public void setAddDateTimeToMp4FileName(boolean addDateTimeToMp4FileName) {
		this.addDateTimeToMp4FileName = addDateTimeToMp4FileName;
	}

	public boolean isHlsMuxingEnabled() {
		return hlsMuxingEnabled;
	}

	public void setHlsMuxingEnabled(boolean hlsMuxingEnabled) {
		this.hlsMuxingEnabled = hlsMuxingEnabled;
	}

	public boolean isObjectDetectionEnabled() {
		return objectDetectionEnabled;
	}

	public void setObjectDetectionEnabled(boolean objectDetectionEnabled) {
		this.objectDetectionEnabled = objectDetectionEnabled;
	}

	public int getHlsListSize() {
		return hlsListSize;
	}

	public void setHlsListSize(int hlsListSize) {
		this.hlsListSize = hlsListSize;
	}

	public int getHlsTime() {
		return hlsTime;
	}

	public void setHlsTime(int hlsTime) {
		this.hlsTime = hlsTime;
	}

	public String getHlsPlayListType() {
		return hlsPlayListType;
	}

	public void setHlsPlayListType(String hlsPlayListType) {
		this.hlsPlayListType = hlsPlayListType;
	}

	public String getFacebookClientId() {
		return facebookClientId;
	}

	public void setFacebookClientId(String facebookClientId) {
		this.facebookClientId = facebookClientId;
	}

	public String getFacebookClientSecret() {
		return facebookClientSecret;
	}

	public void setFacebookClientSecret(String facebookClientSecret) {
		this.facebookClientSecret = facebookClientSecret;
	}

	public String getYoutubeClientId() {
		return youtubeClientId;
	}

	public void setYoutubeClientId(String youtubeClientId) {
		this.youtubeClientId = youtubeClientId;
	}

	public String getYoutubeClientSecret() {
		return youtubeClientSecret;
	}

	public void setYoutubeClientSecret(String youtubeClientSecret) {
		this.youtubeClientSecret = youtubeClientSecret;
	}

	public String getPeriscopeClientId() {
		return periscopeClientId;
	}

	public void setPeriscopeClientId(String periscopeClientId) {
		this.periscopeClientId = periscopeClientId;
	}

	public String getPeriscopeClientSecret() {
		return periscopeClientSecret;
	}

	public void setPeriscopeClientSecret(String periscopeClientSecret) {
		this.periscopeClientSecret = periscopeClientSecret;
	}

	public boolean isAcceptOnlyStreamsInDataStore() {
		return acceptOnlyStreamsInDataStore;
	}

	public void setAcceptOnlyStreamsInDataStore(boolean acceptOnlyStreamsInDataStore) {
		this.acceptOnlyStreamsInDataStore = acceptOnlyStreamsInDataStore;
	}

	public List<EncoderSettings> getEncoderSettings() {
		return encoderSettings;
	}

	public void setEncoderSettings(List<EncoderSettings> encoderSettings) {
		this.encoderSettings = encoderSettings;
	}

	public String getVodFolder() {
		return vodFolder;
	}

	public void setVodFolder(String vodFolder) {
		this.vodFolder = vodFolder;
	}

	public boolean isPreviewOverwrite() {
		return previewOverwrite;
	}

	public void setPreviewOverwrite(boolean previewOverwrite) {
		this.previewOverwrite = previewOverwrite;
	}
	
	public boolean isTokenControlEnabled() {
		return tokenControlEnabled;
	}

	public void setTokenControlEnabled(boolean tokenControlEnabled) {
		this.tokenControlEnabled = tokenControlEnabled;
	}

	public int getWebRTCFrameRate() {
		return webRTCFrameRate;
	}

	public void setWebRTCFrameRate(int webRTCFrameRate) {
		this.webRTCFrameRate = webRTCFrameRate;
	}

	public String getRemoteAllowedCIDR() {
		return remoteAllowedCIDR;
	}

	public void setRemoteAllowedCIDR(String remoteAllowedCIDR) {
		this.remoteAllowedCIDR = remoteAllowedCIDR;
	}
}
