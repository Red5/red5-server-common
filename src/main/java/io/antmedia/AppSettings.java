package io.antmedia;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("/WEB-INF/red5-web.properties")
public class AppSettings {

	public static final String SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME = "settings.addDateTimeToMp4FileName";
	public static final String SETTINGS_HLS_MUXING_ENABLED = "settings.hlsMuxingEnabled";
	public static final String SETTINGS_ENCODER_SETTINGS_STRING = "settings.encoderSettingsString";
	public static final String SETTINGS_HLS_LIST_SIZE = "settings.hlsListSize";
	public static final String SETTINGS_HLS_TIME = "settings.hlsTime";
	public static final String SETTINGS_WEBRTC_ENABLED = "settings.webRTCEnabled";
	public static final String SETTINGS_DELETE_HLS_FILES_ON_ENDED = "settings.deleteHLSFilesOnEnded";
	private static final String SETTINGS_LISTENER_HOOK_URL = "settings.listenerHookURL";
	public static final String SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE = "settings.acceptOnlyStreamsInDataStore";
	public static final String SETTINGS_TOKEN_CONTROL_ENABLED = "settings.tokenControlEnabled";
	public static final String SETTINGS_HLS_PLAY_LIST_TYPE = "settings.hlsPlayListType";
	public static final String FACEBOOK_CLIENT_ID = "facebook.clientId";
	public static final String FACEBOOK_CLIENT_SECRET = "facebook.clientSecret";
	public static final String PERISCOPE_CLIENT_ID = "periscope.clientId";
	public static final String PERISCOPE_CLIENT_SECRET = "periscope.clientSecret";
	public static final String YOUTUBE_CLIENT_ID = "youtube.clientId";
	public static final String YOUTUBE_CLIENT_SECRET = "youtube.clientSecret";
	public static final String SETTINGS_VOD_FOLDER = "settings.vodFolder";
	public static final String SETTINGS_PREVIEW_OVERWRITE = "settings.previewOverwrite";
	private static final String SETTINGS_STALKER_DB_SERVER = "settings.stalkerDBServer";
	private static final String SETTINGS_STALKER_DB_USER_NAME = "settings.stalkerDBUsername";
	private static final String SETTINGS_STALKER_DB_PASSWORD = "settings.stalkerDBPassword";
	public static final String SETTINGS_OBJECT_DETECTION_ENABLED = "settings.objectDetectionEnabled";
	private static final String SETTINGS_CREATE_PREVIEW_PERIOD = "settings.createPreviewPeriod";
	public static final String SETTINGS_MP4_MUXING_ENABLED = "settings.mp4MuxingEnabled";
	private static final String SETTINGS_STREAM_FETCHER_BUFFER_TIME = "settings.streamFetcherBufferTime";
	private static final String SETTINGS_STREAM_FETCHER_RESTART_PERIOD = "settings.streamFetcherRestartPeriod";
	private static final String SETTINGS_MUXER_FINISH_SCRIPT = "settings.muxerFinishScript";
	public static final String SETTINGS_WEBRTC_FRAME_RATE = "settings.webRTCFrameRate";
	public static final String SETTINGS_HASH_CONTROL_PUBLISH_ENABLED = "settings.hashControlPublishEnabled";
	public static final String SETTINGS_HASH_CONTROL_PLAY_ENABLED = "settings.hashControlPlayEnabled";
	public static final String TOKEN_HASH_SECRET = "tokenHashSecret";



	public static final String BEAN_NAME = "app.settings";



	@Value( "${"+SETTINGS_MP4_MUXING_ENABLED+":false}" )
	private boolean mp4MuxingEnabled;
	@Value( "${"+SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME+":false}" )
	private boolean addDateTimeToMp4FileName;
	@Value( "${"+SETTINGS_HLS_MUXING_ENABLED+":true}" )
	private boolean hlsMuxingEnabled;
	@Value( "${"+SETTINGS_ENCODER_SETTINGS_STRING+"}" )
	private String encoderSettingsString;

	private List<EncoderSettings> adaptiveResolutionList;
	@Value( "${"+SETTINGS_HLS_LIST_SIZE+":#{null}}" )
	private String hlsListSize;
	@Value( "${"+SETTINGS_HLS_TIME+":#{null}}" )
	private String hlsTime;
	@Value( "${"+SETTINGS_WEBRTC_ENABLED+":false}" )
	private boolean webRTCEnabled;
	@Value( "${"+SETTINGS_DELETE_HLS_FILES_ON_ENDED+":true}" )
	private boolean deleteHLSFilesOnEnded = true;

	/**
	 * The secret string used for creating hash based tokens
	 */

	@Value( "${"+TOKEN_HASH_SECRET+":#{null}}" )
	private String tokenHashSecret;


	/**
	 * enable hash control as token for publishing operations using shared secret
	 */

	@Value( "${"+SETTINGS_HASH_CONTROL_PUBLISH_ENABLED+":false}" )
	private boolean hashControlPublishEnabled;


	/**
	 * enable hash control as token for playing operations using shared secret
	 */

	@Value( "${"+SETTINGS_HASH_CONTROL_PLAY_ENABLED+":false}" )
	private boolean hashControlPlayEnabled;



	/**
	 * The URL for action callback
	 */
	@Value( "${"+SETTINGS_LISTENER_HOOK_URL+":}" )
	private String listenerHookURL;

	/**
	 * The control for publishers
	 */
	@Value( "${"+SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE+":false}" )
	private boolean acceptOnlyStreamsInDataStore;

	/**
	 * The settings for enabling one-time token control mechanism for accessing resources and publishing
	 */
	@Value( "${"+SETTINGS_TOKEN_CONTROL_ENABLED+":false}" )
	private boolean tokenControlEnabled ;


	/**
	 * Fully qualified server name
	 */
	//@Value( "#{ @'ant.media.server.settings'.serverName }" )
	private String serverName;

	/**
	 * event or vod
	 */
	@Value( "${"+SETTINGS_HLS_PLAY_LIST_TYPE+":#{null}}" )
	private String hlsPlayListType;

	/**
	 * Facebook client id
	 */
	@Value( "${"+FACEBOOK_CLIENT_ID+"}" )
	private String facebookClientId;

	/**
	 * Facebook client secret
	 */
	@Value( "${"+FACEBOOK_CLIENT_SECRET+"}" )
	private String facebookClientSecret;

	/**
	 * Periscope app client id
	 */
	@Value( "${"+PERISCOPE_CLIENT_ID+"}" )
	private String  periscopeClientId;

	/**
	 * Periscope app client secret
	 */
	@Value( "${"+PERISCOPE_CLIENT_SECRET+"}" )
	private String  periscopeClientSecret;

	/**
	 * Youtube client id
	 */
	@Value( "${"+YOUTUBE_CLIENT_ID+"}" )
	private String youtubeClientId;

	/**
	 * Youtube client secret
	 */
	@Value( "${"+YOUTUBE_CLIENT_SECRET+"}" )
	private String youtubeClientSecret;

	/**
	 * The path for manually saved used VoDs
	 */
	@Value( "${"+SETTINGS_VOD_FOLDER+"}" )
	private String vodFolder;

	/**
	 * Overwrite preview files if exist, default value is false
	 */
	@Value( "${"+SETTINGS_PREVIEW_OVERWRITE+":false}" )
	private boolean previewOverwrite;

	/**
	 * Address of the Stalker Portal DB server 
	 */

	@Value( "${"+SETTINGS_STALKER_DB_SERVER+"}" )
	private String stalkerDBServer;

	/**
	 * Username of stalker portal DB
	 */
	@Value( "${"+SETTINGS_STALKER_DB_USER_NAME+"}" )
	private String stalkerDBUsername;

	/**
	 * Password of the stalker portal DB User
	 */
	@Value( "${"+SETTINGS_STALKER_DB_PASSWORD+"}" )
	private String stalkerDBPassword;

	/**
	 * The directory contains the tensorflow object detection model
	 */
	@Value( "${"+SETTINGS_OBJECT_DETECTION_ENABLED+":false}" )
	private boolean objectDetectionEnabled;


	@Value( "${"+SETTINGS_CREATE_PREVIEW_PERIOD+":5000}" )
	private int createPreviewPeriod;

	/**
	 * Restart stream fetcher period in seconds
	 */
	@Value( "${"+SETTINGS_STREAM_FETCHER_RESTART_PERIOD+":0}" )
	private int restartStreamFetcherPeriod;

	/**
	 * Stream fetcher buffer time in milliseconds. 
	 * Stream is buffered for this duration and after that it will be started.
	 */
	//@Value( "${"+SETTINGS_STREAM_FETCHER_BUFFER_TIME+"}" )
	private int streamFetcherBufferTime = 0;


	/**
	 * HLS Flags for FFmpeg HLS Muxer
	 */
	private String hlsflags;

	private String mySqlClientPath = "/usr/local/antmedia/mysql";

	/**
	 * This is a script file path that is called by Runtime when muxing is finished
	 */
	@Value( "${"+SETTINGS_MUXER_FINISH_SCRIPT+":}" )
	private String muxerFinishScript;

	/**
	 * Framerate parameter for WebRTC encoder 
	 */
	@Value( "${"+SETTINGS_WEBRTC_FRAME_RATE+":20}" )
	private int webRTCFrameRate;

	/**
	 * If it's enabled, interactivity(like, comment,) is collected from social media channel
	 */
	private boolean collectSocialMediaActivity = false;


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
		if(adaptiveResolutionList != null) {
			return adaptiveResolutionList;
		}
		else if( encoderSettingsString != null && !encoderSettingsString.isEmpty()) {
			adaptiveResolutionList = encodersStr2List(encoderSettingsString);
		}
		return adaptiveResolutionList;
	}


	public void setAdaptiveResolutionList(List<EncoderSettings> adaptiveResolutionList) {
		this.adaptiveResolutionList = adaptiveResolutionList;
		setEncoderSettingsString(encodersList2Str(adaptiveResolutionList));
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

	public static String encodersList2Str(List<EncoderSettings> encoderSettingsList) 
	{
		if(encoderSettingsList == null) {
			return "";
		}
		String encoderSettingsString = "";

		for (EncoderSettings encoderSettings : encoderSettingsList) {
			if (encoderSettingsString.length() != 0) {
				encoderSettingsString += ",";
			}
			encoderSettingsString += encoderSettings.getHeight() + "," + encoderSettings.getVideoBitrate() + "," + encoderSettings.getAudioBitrate();
		}
		return encoderSettingsString;
	}

	public static List<EncoderSettings> encodersStr2List(String encoderSettingsString) {
		if(encoderSettingsString == null) {
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
		return encoderSettingsString;
	}

	public void setEncoderSettingsString(String encoderSettingsString) {
		this.encoderSettingsString = encoderSettingsString;
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

	public boolean isObjectDetectionEnabled() {
		return objectDetectionEnabled;
	}

	public void setObjectDetectionEnabled(Boolean objectDetectionEnabled) {
		this.objectDetectionEnabled = objectDetectionEnabled;
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


	public String getVodFolder() {
		return vodFolder;
	}

	public void setVodFolder(String vodFolder) {
		this.vodFolder = vodFolder;
	}

	public int getCreatePreviewPeriod() {
		return createPreviewPeriod;
	}

	public void setCreatePreviewPeriod(int period) {
		this.createPreviewPeriod = period;
	}

	public boolean isPreviewOverwrite() {
		return previewOverwrite;
	}

	public void setPreviewOverwrite(boolean previewOverwrite) {
		this.previewOverwrite = previewOverwrite;
	}

	public String getStalkerDBServer() {
		return stalkerDBServer;
	}

	public void setStalkerDBServer(String stalkerDBServer) {
		this.stalkerDBServer = stalkerDBServer;
	}

	public String getStalkerDBUsername() {
		return stalkerDBUsername;
	}

	public void setStalkerDBUsername(String stalkerDBUsername) {
		this.stalkerDBUsername = stalkerDBUsername;
	}

	public String getStalkerDBPassword() {
		return stalkerDBPassword;
	}

	public void setStalkerDBPassword(String stalkerDBPassword) {
		this.stalkerDBPassword = stalkerDBPassword;
	}

	public int getRestartStreamFetcherPeriod() {
		return this.restartStreamFetcherPeriod ;
	}

	public void setRestartStreamFetcherPeriod(int restartStreamFetcherPeriod) {
		this.restartStreamFetcherPeriod = restartStreamFetcherPeriod;
	}

	public int getStreamFetcherBufferTime() {
		return streamFetcherBufferTime;
	}

	public void setStreamFetcherBufferTime(int streamFetcherBufferTime) {
		this.streamFetcherBufferTime = streamFetcherBufferTime;
	}

	public String getHlsFlags() {
		return hlsflags;
	}

	public void setHlsflags(String hlsflags) {
		this.hlsflags = hlsflags;
	}

	public String getMySqlClientPath() {
		return this.mySqlClientPath;

	}

	public void setMySqlClientPath(String mySqlClientPath) {
		this.mySqlClientPath = mySqlClientPath;
	}


	public boolean isTokenControlEnabled() {
		return tokenControlEnabled;
	}

	public void setTokenControlEnabled(boolean tokenControlEnabled) {
		this.tokenControlEnabled = tokenControlEnabled;
	}

	public String getMuxerFinishScript() {
		return muxerFinishScript;
	}

	public void setMuxerFinishScript(String muxerFinishScript) {
		this.muxerFinishScript = muxerFinishScript;
	}

	public int getWebRTCFrameRate() {
		return webRTCFrameRate;
	}

	public void setWebRTCFrameRate(int webRTCFrameRate) {
		this.webRTCFrameRate = webRTCFrameRate;
	}

	public boolean isCollectSocialMediaActivity() {
		return collectSocialMediaActivity;
	}

	public void setCollectSocialMediaActivity(boolean collectSocialMediaActivity) {
		this.collectSocialMediaActivity = collectSocialMediaActivity;
	}


	public String getTokenHashSecret() {
		return tokenHashSecret;
	}

	public void setTokenHashSecret(String tokenHashSecret) {
		this.tokenHashSecret = tokenHashSecret;
	}


	public boolean isHashControlPlayEnabled() {
		return hashControlPlayEnabled;
	}

	public void setHashControlPlayEnabled(boolean hashControlPlayEnabled) {
		this.hashControlPlayEnabled = hashControlPlayEnabled;
	}

	public boolean isHashControlPublishEnabled() {
		return hashControlPublishEnabled;
	}

	public void setHashControlPublishEnabled(boolean hashControlPublishEnabled) {
		this.hashControlPublishEnabled = hashControlPublishEnabled;
	}

	public void resetDefaults() {
		mp4MuxingEnabled = false;
		addDateTimeToMp4FileName = false;
		hlsMuxingEnabled = true;
		adaptiveResolutionList = null;
		hlsListSize = null;
		hlsTime = null;
		webRTCEnabled = false;
		deleteHLSFilesOnEnded = true;
		acceptOnlyStreamsInDataStore = false;
		tokenControlEnabled = false;
		hlsPlayListType = null;
		previewOverwrite = false;
		objectDetectionEnabled = false;
		createPreviewPeriod = 5000;
		restartStreamFetcherPeriod = 0;
		webRTCFrameRate = 20;
	}
}
