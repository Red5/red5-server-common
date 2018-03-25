package io.antmedia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.jmx.export.annotation.ManagedResource;

public class AppSettings {
	
	public static final String BEAN_NAME = "app.settings";


	private boolean mp4MuxingEnabled = true;

	private boolean addDateTimeToMp4FileName = false;

	private boolean hlsMuxingEnabled = true;

	private List<EncoderSettings> adaptiveResolutionList;

	private String hlsListSize;

	private String hlsTime;

	private boolean webRTCEnabled = false;
	
	private boolean deleteHLSFilesOnEnded = true;
	
	private String listenerHookURL;
	
	
	private boolean acceptOnlyStreamsInDataStore;

	/**
	 * Fully qualified server name
	 */
	private String serverName;
	
	/**
	 * event or vod
	 */
	private String hlsPlayListType;
	
	/**
	 * Facebook client id
	 */
	private String facebookClientId;
	
	/**
	 * Facebook client secret
	 */
	private String facebookClientSecret;
  	
	/**
	 * Periscope app client id
	 */
	private String  periscopeClientId;
	
	/**
	 * Periscope app client secret
	 */
	private String  periscopeClientSecret;
	
  	/**
  	 * Youtube client id
  	 */
	private String youtubeClientId;
	
	/**
	 * Youtube client secret
	 */
	private String youtubeClientSecret;


	public boolean isAddDateTimeToMp4FileName() {
		return addDateTimeToMp4FileName;
	}

	public void setAddDateTimeToMp4FileName(boolean addDateTimeToMp4FileName) {
		this.addDateTimeToMp4FileName = addDateTimeToMp4FileName;
	}

	public boolean isMp4MuxingEnabled() {
		return mp4MuxingEnabled;
	}

	public void setMp4MuxingEnabled(boolean mp4MuxingEnabled) {
		this.mp4MuxingEnabled = mp4MuxingEnabled;
	}

	public boolean isHlsMuxingEnabled() {
		return hlsMuxingEnabled;
	}

	public void setHlsMuxingEnabled(boolean hlsMuxingEnabled) {
		this.hlsMuxingEnabled = hlsMuxingEnabled;
	}

	public List<EncoderSettings> getAdaptiveResolutionList() {
		return adaptiveResolutionList;
	}


	public void setAdaptiveResolutionList(List<EncoderSettings> adaptiveResolutionList) {
		this.adaptiveResolutionList = adaptiveResolutionList;
	}

	public String getHlsPlayListType() {
		return hlsPlayListType;
	}

	public void setHlsPlayListType(String hlsPlayListType) {
		this.hlsPlayListType = hlsPlayListType;
	}

	public String getHlsTime() {
		return hlsTime;
	}

	public void setHlsTime(String hlsTime) {
		this.hlsTime = hlsTime;
	}

	public String getHlsListSize() {
		return hlsListSize;
	}

	public void setHlsListSize(String hlsListSize) {
		this.hlsListSize = hlsListSize;
	}

	public boolean isWebRTCEnabled() {
		return webRTCEnabled;
	}

	public void setWebRTCEnabled(boolean webRTCEnabled) {
		this.webRTCEnabled = webRTCEnabled;
	}

	public static String getEncoderSettingsString(List<EncoderSettings> encoderSettingsList) 
	{
		String encoderSettingsString = "";

		for (EncoderSettings encoderSettings : encoderSettingsList) {
			if (encoderSettingsString.length() != 0) {
				encoderSettingsString += ",";
			}
			encoderSettingsString += encoderSettings.getHeight() + "," + encoderSettings.getVideoBitrate() + "," + encoderSettings.getAudioBitrate();
		}
		return encoderSettingsString;
	}

	public static List<EncoderSettings> getEncoderSettingsList(String encoderSettingsString) {
		if (encoderSettingsString == null) {
			return null;
		}
		String[] values = encoderSettingsString.split(",");

		List<EncoderSettings> encoderSettingsList = new ArrayList();
		if (values.length >= 3){
			for (int i = 0; i < values.length; i++) {
				int height = Integer.parseInt(values[i]);
				i++;
				int videoBitrate = Integer.parseInt(values[i]);
				i++;
				int audioBitrate = Integer.parseInt(values[i]);
				encoderSettingsList.add(new EncoderSettings(height, videoBitrate, audioBitrate));
			}
		}
		return encoderSettingsList;
	}

	public String getEncoderSettingsString() {
		return getEncoderSettingsString(adaptiveResolutionList);
		//return encoderSettingsString;
	}

	public void setEncoderSettingsString(String encoderSettingsString) {
		adaptiveResolutionList = getEncoderSettingsList(encoderSettingsString);
		//this.encoderSettingsString = encoderSettingsString;
	}

	public boolean isDeleteHLSFilesOnExit() {
		return deleteHLSFilesOnEnded;
	}

	public void setDeleteHLSFilesOnEnded(boolean deleteHLSFilesOnEnded) {
		this.deleteHLSFilesOnEnded = deleteHLSFilesOnEnded;
	}

	public String getListenerHookURL() {
		return listenerHookURL;
	}

	public void setListenerHookURL(String listenerHookURL) {
		this.listenerHookURL = listenerHookURL;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public boolean isAcceptOnlyStreamsInDataStore() {
		return acceptOnlyStreamsInDataStore;
	}

	public void setAcceptOnlyStreamsInDataStore(boolean acceptOnlyStreamsInDataStore) {
		this.acceptOnlyStreamsInDataStore = acceptOnlyStreamsInDataStore;
	}

	public String getYoutubeClientSecret() {
		return youtubeClientSecret;
	}

	public void setYoutubeClientSecret(String youtubeClientSecret) {
		this.youtubeClientSecret = youtubeClientSecret;
	}

	public String getYoutubeClientId() {
		return youtubeClientId;
	}

	public void setYoutubeClientId(String youtubeClientId) {
		this.youtubeClientId = youtubeClientId;
	}

	public String getPeriscopeClientSecret() {
		return periscopeClientSecret;
	}

	public void setPeriscopeClientSecret(String periscopeClientSecret) {
		this.periscopeClientSecret = periscopeClientSecret;
	}

	public String getPeriscopeClientId() {
		return periscopeClientId;
	}

	public void setPeriscopeClientId(String periscopeClientId) {
		this.periscopeClientId = periscopeClientId;
	}

	public String getFacebookClientSecret() {
		return facebookClientSecret;
	}

	public void setFacebookClientSecret(String facebookClientSecret) {
		this.facebookClientSecret = facebookClientSecret;
	}

	public String getFacebookClientId() {
		return facebookClientId;
	}

	public void setFacebookClientId(String facebookClientId) {
		this.facebookClientId = facebookClientId;
	}



}
