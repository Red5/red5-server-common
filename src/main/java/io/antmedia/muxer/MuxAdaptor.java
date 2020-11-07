package io.antmedia.muxer;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_FLTP;
import static org.bytedeco.ffmpeg.global.avutil.av_get_default_channel_layout;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.javacpp.BytePointer;
import org.red5.codec.AACAudio;
import org.red5.codec.AVCVideo;
import org.red5.codec.IAudioStreamCodec;
import org.red5.codec.IVideoStreamCodec;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.CachedEvent;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.IRecordingListener;
import org.red5.server.stream.consumer.FileConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
import io.antmedia.RecordType;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.parser.AACConfigParser;
import io.antmedia.muxer.parser.AACConfigParser.AudioObjectTypes;
import io.antmedia.muxer.parser.SpsParser;
import io.antmedia.storage.StorageClient;
import io.vertx.core.Vertx;


public class MuxAdaptor implements IRecordingListener {

	
	public static final String ADAPTIVE_SUFFIX = "_adaptive";
	private static Logger logger = LoggerFactory.getLogger(MuxAdaptor.class);
	
	protected ConcurrentLinkedQueue<IStreamPacket> streamPacketQueue = new ConcurrentLinkedQueue<>();
	protected AtomicBoolean isPipeReaderJobRunning = new AtomicBoolean(false);
	private   AtomicBoolean isBufferedWriterRunning = new AtomicBoolean(false);

	protected List<Muxer> muxerList =  Collections.synchronizedList(new ArrayList<Muxer>());
	protected boolean deleteHLSFilesOnExit = true;
	protected boolean deleteDASHFilesOnExit = true;


	protected int videoStreamIndex;
	protected int audioStreamIndex;

	protected boolean previewOverwrite = false;

	protected volatile boolean enableVideo = false;
	protected volatile boolean enableAudio = false;
	
	boolean firstAudioPacketSkipped = false;

	private long packetPollerId = -1;

	private Queue<IStreamPacket> bufferQueue = new ConcurrentLinkedQueue<>();
	
	private volatile boolean stopRequestExist = false;

	public static final int RECORDING_ENABLED_FOR_STREAM = 1;
	public static final int RECORDING_DISABLED_FOR_STREAM = -1;
	public static final int RECORDING_NO_SET_FOR_STREAM = 0;
	protected static final long WAIT_TIME_MILLISECONDS = 5;
	protected volatile boolean isRecording = false;
	protected ClientBroadcastStream broadcastStream;
	protected boolean mp4MuxingEnabled;
	protected boolean webMMuxingEnabled;
	protected boolean addDateTimeToMp4FileName;
	protected boolean hlsMuxingEnabled;
	protected boolean dashMuxingEnabled;
	protected boolean objectDetectionEnabled;
	protected boolean webRTCEnabled = false;
	protected StorageClient storageClient;
	protected String hlsTime;
	protected String hlsListSize;
	protected String hlsPlayListType;
	protected String dashSegDuration;
	protected String dashFragmentDuration;
	protected String targetLatency;
	List<EncoderSettings> adaptiveResolutionList = null;
	
	protected DataStore dataStore;

	/**
	 * By default first video key frame should be checked
	 * and below flag should be set to true
	 * If first video key frame should not be checked,
	 * then below should be flag in advance
	 */
	private boolean firstKeyFrameReceivedChecked = false;
	protected String streamId;
	protected long startTime;

	protected IScope scope;

	private String oldQuality;
	public static final  AVRational TIME_BASE_FOR_MS;
	private IAntMediaStreamHandler appAdapter;

	private String mp4Filtername;
	protected List<EncoderSettings> encoderSettingsList;
	protected long elapsedTime;
	protected static boolean isStreamSource = false;

	private int previewCreatePeriod;
	private double oldspeed;
	private long firstPacketTime = -1;
	private long lastQualityUpdateTime = 0;
	protected Broadcast broadcast;
	protected AppSettings appSettings;
	private int previewHeight;
	private int lastFrameTimestamp;
	private int maxAnalyzeDurationMS = 1000;
	protected boolean generatePreview = true;
	private int firstReceivedFrameTimestamp = -1;
	protected int totalIngestedVideoPacketCount = 0;
	private long bufferTimeMs = 0;

	protected Vertx vertx;

	/**
	 * Accessed from multiple threads so make it volatile
	 */
	private volatile boolean buffering;
	private int bufferLogCounter;

	/**
	 * The time when buffering has been finished. It's volatile because it's accessed from multiple threads
	 */
	private volatile long bufferingFinishTimeMs = 0;
	
	/**
	 * Mux adaptor is generally used in RTMP. 
	 * However it can be also used to stream RTSP Pull so that isAVC can be false
	 */
	private boolean avc = true;

	private long bufferedPacketWriterId = -1;
	private volatile long lastPacketTimeMsInQueue = 0;
	private volatile long firstPacketReadyToSentTimeMs = 0;
	protected String dataChannelWebHookURL = null;
	protected long absoluteTotalIngestTime = 0;
	
	/**
	 * It's defined here because EncoderAdaptor should access it directly to add new streams
	 */
	private Muxer dashMuxer = null;

	private long checkStreamsStartTime = -1;
	private byte[] videoDataConf;
	private byte[] audioDataConf;
	//private AVFormatContext inputFormatContext;
	private AtomicInteger queueSize = new AtomicInteger(0);
	private long startTimeMs;
	protected long totalIngestTime;
	private int fps = 0;
	private int width;
	private int height;
	
	private static final int COUNT_TO_LOG_BUFFER = 500;

	static {
		TIME_BASE_FOR_MS = new AVRational();
		TIME_BASE_FOR_MS.num(1);
		TIME_BASE_FOR_MS.den(1000);
	}

	public static MuxAdaptor initializeMuxAdaptor(ClientBroadcastStream clientBroadcastStream, boolean isSource, IScope scope) {
		MuxAdaptor muxAdaptor = null;
		ApplicationContext applicationContext = scope.getContext().getApplicationContext();
		boolean tryEncoderAdaptor = false;
		if (applicationContext.containsBean(AppSettings.BEAN_NAME)) {
			AppSettings appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
			List<EncoderSettings> list = appSettings.getEncoderSettings();
			if ((list != null && !list.isEmpty()) || appSettings.isWebRTCEnabled()) {
				/*
				 * enable encoder adaptor if webrtc enabled because we're supporting forwarding video to end user
				 * without transcoding. We need encoder adaptor because we need to transcode audio
				 */
				tryEncoderAdaptor = true;
			}
		}

		if (tryEncoderAdaptor) {
			//if adaptive bitrate enabled, take a look at encoder adaptor exists
			//if it is not enabled, then initialize only mux adaptor
			try {
				Class transraterClass = Class.forName("io.antmedia.enterprise.adaptive.EncoderAdaptor");

				muxAdaptor = (MuxAdaptor) transraterClass.getConstructor(ClientBroadcastStream.class)
						.newInstance(clientBroadcastStream);

			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		if (muxAdaptor == null) {
			muxAdaptor = new MuxAdaptor(clientBroadcastStream);
		}
		muxAdaptor.setStreamSource(isSource);

		return muxAdaptor;
	}


	protected MuxAdaptor(ClientBroadcastStream clientBroadcastStream) {

		this.broadcastStream = clientBroadcastStream;
	}

	public void addMuxer(Muxer muxer) 
	{
		muxerList.add(muxer);
	}

	@Override
	public boolean init(IConnection conn, String name, boolean isAppend) {

		return init(conn.getScope(), name, isAppend);
	}

	protected void enableSettings() {
		AppSettings appSettingsLocal = getAppSettings();
		hlsMuxingEnabled = appSettingsLocal.isHlsMuxingEnabled();
		dashMuxingEnabled = appSettingsLocal.isDashMuxingEnabled();
		mp4MuxingEnabled = appSettingsLocal.isMp4MuxingEnabled();
		webMMuxingEnabled = appSettingsLocal.isWebMMuxingEnabled();
		objectDetectionEnabled = appSettingsLocal.isObjectDetectionEnabled();

		addDateTimeToMp4FileName = appSettingsLocal.isAddDateTimeToMp4FileName();
		mp4Filtername = null;
		webRTCEnabled = appSettingsLocal.isWebRTCEnabled();
		deleteHLSFilesOnExit = appSettingsLocal.isDeleteHLSFilesOnEnded();
		deleteDASHFilesOnExit = appSettingsLocal.isDeleteDASHFilesOnEnded();
		hlsListSize = appSettingsLocal.getHlsListSize();
		hlsTime = appSettingsLocal.getHlsTime();
		hlsPlayListType = appSettingsLocal.getHlsPlayListType();
		dashSegDuration = appSettingsLocal.getDashSegDuration();
		dashFragmentDuration = appSettingsLocal.getDashFragmentDuration();
		targetLatency = appSettingsLocal.getTargetLatency();

		previewOverwrite = appSettingsLocal.isPreviewOverwrite();
		encoderSettingsList = appSettingsLocal.getEncoderSettings();
		previewCreatePeriod = appSettingsLocal.getCreatePreviewPeriod();
		maxAnalyzeDurationMS = appSettingsLocal.getMaxAnalyzeDurationMS();
		generatePreview = appSettingsLocal.isGeneratePreview();
		previewHeight = appSettingsLocal.getPreviewHeight();
		bufferTimeMs = appSettingsLocal.getRtmpIngestBufferTimeMs();
		dataChannelWebHookURL = appSettingsLocal.getDataChannelWebHook();
	}

	public void initStorageClient() {
		if (scope.getContext().getApplicationContext().containsBean(StorageClient.BEAN_NAME)) {
			storageClient = (StorageClient) scope.getContext().getApplicationContext().getBean(StorageClient.BEAN_NAME);
		}
	}

	@Override
	public boolean init(IScope scope, String streamId, boolean isAppend) {

		this.streamId = streamId;
		this.scope = scope;

		getDataStore();
		enableSettings();
		initStorageClient();
		enableMp4Setting();
		enableWebMSetting();
		initVertx();

		if (mp4MuxingEnabled) {
			addMp4Muxer();
			logger.info("adding MP4 Muxer, add datetime to file name {}", addDateTimeToMp4FileName);
		}
		
		if (hlsMuxingEnabled) {
			HLSMuxer hlsMuxer = new HLSMuxer(vertx, hlsListSize, hlsTime, hlsPlayListType, getAppSettings().getHlsFlags());
			hlsMuxer.setDeleteFileOnExit(deleteHLSFilesOnExit);
			addMuxer(hlsMuxer);
			logger.info("adding HLS Muxer for {}", streamId);
		}
		
		getDashMuxer();
		if (dashMuxer != null) {
			addMuxer(dashMuxer);
		}

		for (Muxer muxer : muxerList) {
			muxer.init(scope, streamId, 0);
		}
		getStreamHandler().muxAdaptorAdded(this);
		return true;
	}

	public Muxer getDashMuxer() 
	{
		if (dashMuxingEnabled && dashMuxer == null) {
			try {
				Class dashMuxerClass = Class.forName("io.antmedia.enterprise.muxer.DASHMuxer");
			
				logger.info("adding DASH Muxer for {}", streamId);

				dashMuxer = (Muxer) dashMuxerClass.getConstructors()[0]
					.newInstance(vertx, dashFragmentDuration, dashSegDuration, targetLatency, deleteDASHFilesOnExit, !appSettings.getEncoderSettings().isEmpty(),
							appSettings.getDashWindowSize(), appSettings.getDashExtraWindowSize());
			}
			catch (ClassNotFoundException e) {
				logger.info("DashMuxer class not found for stream:{}", streamId);
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			
		}
		return dashMuxer;
	}

	private void initVertx() {
		if (scope.getContext().getApplicationContext().containsBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)) 
		{
			vertx = (Vertx)scope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);
			logger.info("vertx exist {}", vertx);
		}
		else {
			logger.info("No vertx bean for stream {}", streamId);
		}
	}

	protected void enableMp4Setting() {
		broadcast = getBroadcast();

		if (broadcast != null) 
		{
			if (broadcast.getMp4Enabled() == RECORDING_DISABLED_FOR_STREAM) 
			{
				// if stream specific mp4 setting is disabled
				mp4MuxingEnabled = false;
			} 
			else if (broadcast.getMp4Enabled() == RECORDING_ENABLED_FOR_STREAM) 
			{
				// if stream specific mp4 setting is enabled
				mp4MuxingEnabled = true;
			}
		}
	}
	
	protected void enableWebMSetting() {
		broadcast = getBroadcast();

		if (broadcast != null) 
		{
			if (broadcast.getWebMEnabled() == RECORDING_DISABLED_FOR_STREAM) 
			{
				// if stream specific WebM setting is disabled
				webMMuxingEnabled = false;
			} 
			else if (broadcast.getWebMEnabled() == RECORDING_ENABLED_FOR_STREAM) 
			{
				// if stream specific WebM setting is enabled
				webMMuxingEnabled = true;
			}
		}
	}
	
	
	public boolean prepare() throws Exception {
		
		if (enableVideo) {
			IVideoStreamCodec videoCodec = broadcastStream.getCodecInfo().getVideoCodec();
			if (videoCodec instanceof AVCVideo) 
			{
				IoBuffer videoBuffer = videoCodec.getDecoderConfiguration();
				videoDataConf = new byte[videoBuffer.limit()-5];
				videoBuffer.position(5).get(videoDataConf);
			}
			else {
				 logger.warn("Video codec is not AVC(H264) for stream: {}", streamId);
			}
		}
		
		if (enableAudio) {
			 IAudioStreamCodec audioCodec = broadcastStream.getCodecInfo().getAudioCodec();
			 if (audioCodec instanceof AACAudio) 
			 {
				IoBuffer audioBuffer = audioCodec.getDecoderConfiguration();
				audioDataConf = new byte[audioBuffer.limit()-2];
				audioBuffer.position(2).get(audioDataConf);
			 }
			 else {
				 logger.warn("Audio codec is not AAC for stream: {}", streamId);
			 }
		}
		
		int streamIndex = 0;
		AVCodecParameters codecParameters = getVideoCodecParameters();
		if (codecParameters != null) {
			addStream2Muxers(codecParameters, TIME_BASE_FOR_MS);
			videoStreamIndex = streamIndex;
			streamIndex++;
		}
		
		
		AVCodecParameters parameters = getAudioCodecParameters();
		if (parameters != null) {
			addStream2Muxers(parameters, TIME_BASE_FOR_MS);
			audioStreamIndex = streamIndex;
		}
		
		prepareMuxerIO();
		
		return true;
	}


	private AVCodecParameters getAudioCodecParameters() {
		
		AVCodecParameters audioCodecParameters = null;
		if (audioDataConf != null) 
		{
			AACConfigParser aacParser = new AACConfigParser(audioDataConf, 0);
			
			audioCodecParameters = new AVCodecParameters();
			audioCodecParameters.sample_rate(aacParser.getSampleRate());
			audioCodecParameters.channels(aacParser.getChannelCount());
			audioCodecParameters.channel_layout(av_get_default_channel_layout(aacParser.getChannelCount()));
			audioCodecParameters.codec_id(AV_CODEC_ID_AAC);
			audioCodecParameters.codec_type(AVMEDIA_TYPE_AUDIO);
			
			if (aacParser.getObjectType() == AudioObjectTypes.AAC_LC) {
			
				audioCodecParameters.profile(AVCodecContext.FF_PROFILE_AAC_LOW);
			}
			else if (aacParser.getObjectType() == AudioObjectTypes.AAC_LTP) {
			
				audioCodecParameters.profile(AVCodecContext.FF_PROFILE_AAC_LTP);
			}
			else if (aacParser.getObjectType() == AudioObjectTypes.AAC_MAIN) {
				
				audioCodecParameters.profile(AVCodecContext.FF_PROFILE_AAC_MAIN);
			}
			else if (aacParser.getObjectType() == AudioObjectTypes.AAC_SSR) {
				
				audioCodecParameters.profile(AVCodecContext.FF_PROFILE_AAC_SSR);
			}
			
			audioCodecParameters.frame_size(aacParser.getFrameSize());
			audioCodecParameters.format(AV_SAMPLE_FMT_FLTP);
			BytePointer extraDataPointer = new BytePointer(audioDataConf);
			audioCodecParameters.extradata(extraDataPointer);
			audioCodecParameters.extradata_size(audioDataConf.length);		
			audioCodecParameters.codec_tag(0);
		}
		return audioCodecParameters;
	}


	private AVCodecParameters getVideoCodecParameters() 
	{
		AVCodecParameters videoCodecParameters = null;
		if (videoDataConf != null) {
			SpsParser spsParser = new SpsParser(getAnnexbExtradata(videoDataConf), 5);
			
			videoCodecParameters = new AVCodecParameters();
			logger.info("Incoming video width: {} height:{}", spsParser.getWidth(), spsParser.getHeight());
			width = spsParser.getWidth();
			height = spsParser.getHeight();
			videoCodecParameters.width(spsParser.getWidth());
			videoCodecParameters.height(spsParser.getHeight());
			videoCodecParameters.codec_id(AV_CODEC_ID_H264);
			videoCodecParameters.codec_type(AVMEDIA_TYPE_VIDEO);
			videoCodecParameters.extradata_size(videoDataConf.length);
			BytePointer extraDataPointer = new BytePointer(videoDataConf);
			videoCodecParameters.extradata(extraDataPointer);
			videoCodecParameters.format(AV_PIX_FMT_YUV420P);
			videoCodecParameters.codec_tag(0);
		}
		return videoCodecParameters;
	}


	public boolean prepareInternal(AVFormatContext inputFormatContext) throws Exception {

		// Dump information about file onto standard error
		int streamCount = inputFormatContext.nb_streams();
		int width = -1;
		int height = -1;
		for (int i=0; i < streamCount; i++) 
		{
			AVStream stream = inputFormatContext.streams(i);
			AVCodecParameters codecpar = stream.codecpar();
			if (codecpar.codec_type() == AVMEDIA_TYPE_VIDEO) {
				logger.info("Video format width:{} height:{} for stream: {}", codecpar.width(), codecpar.height(), streamId);
				width = codecpar.width();
				height = codecpar.height();
				
			}
			else if (codecpar.codec_type() == AVMEDIA_TYPE_AUDIO) {
				logger.info("Audio format sample rate:{} bitrate:{} for stream: {}",codecpar.sample_rate(), codecpar.bit_rate(), streamId);
			}
		}
		
		if (width == 0 || height == 0) {
			logger.info("Width or height is zero so returning for stream: {}", streamId);
			return false;
		}
	
		return prepareMuxers(inputFormatContext);
	}
	
	
	public static byte[] getAnnexbExtradata(byte[] avcExtradata){
		IoBuffer buffer = IoBuffer.wrap(avcExtradata);
		
		for (int i = 0; i < avcExtradata.length; i++) {
			logger.info(" data[{}]: {}" ,i, avcExtradata[i]);
		}
		buffer.skip(6); //skip first 6 bytes for avc
		short spsSize = buffer.getShort();
		byte[] sps = new byte[spsSize];
		
		buffer.get(sps);
		
		buffer.skip(1); //skip one byte for pps number
		
		short ppsSize = buffer.getShort();
		
		
		byte[] pps = new byte[ppsSize];
		buffer.get(pps);
		
		byte[] extradataAnnexb = new byte[8 + spsSize + ppsSize];
		extradataAnnexb[0] = 0x00;
		extradataAnnexb[1] = 0x00;
		extradataAnnexb[2] = 0x00;
		extradataAnnexb[3] = 0x01;
		
		System.arraycopy(sps, 0, extradataAnnexb, 4, spsSize);
		
		extradataAnnexb[4 + spsSize] = 0x00;
		extradataAnnexb[5 + spsSize] = 0x00;
		extradataAnnexb[6 + spsSize] = 0x00;
		extradataAnnexb[7 + spsSize] = 0x01;
		
		System.arraycopy(pps, 0, extradataAnnexb, 8 + spsSize, ppsSize);
		return extradataAnnexb;
	}
	
	
	public void addStream2Muxers(AVCodecParameters codecParameters, AVRational rat) 
	{
		Iterator<Muxer> iterator = muxerList.iterator();
		while (iterator.hasNext()) 
		{
			Muxer muxer = iterator.next();
			if (!muxer.addStream(codecParameters, rat)) 
			{
				iterator.remove();
				logger.warn("addStream returns false {} for stream: {}", muxer.getFormat(), streamId);
			}
		}
		
		startTime = System.currentTimeMillis();
	}
	
	public void prepareMuxerIO() 
	{
		Iterator<Muxer> iterator = muxerList.iterator();
		while (iterator.hasNext()) 
		{
			Muxer muxer = iterator.next();
			if (!muxer.prepareIO()) 
			{
				iterator.remove();
				logger.error("prepareIO returns false {} for stream: {}", muxer.getFormat(), streamId);
			}
		}
		
		startTime = System.currentTimeMillis();
		
	}
	
	public boolean prepareMuxers(AVFormatContext inputFormatContext) throws Exception {

		
		Iterator<Muxer> iterator = muxerList.iterator();
		while (iterator.hasNext()) {
			Muxer muxer = iterator.next();
			if (!muxer.prepare(inputFormatContext)) {
				iterator.remove();
				logger.warn("muxer prepare returns false {}", muxer.getFormat());
			}
		}

		startTime = System.currentTimeMillis();

		return true;
	}

	/**
	 * @param streamId        id of the stream
	 * @param quality,        quality string
	 * @param packetTime,     time of the packet in milliseconds
	 * @param duration,       the total elapsed time in milliseconds
	 * @param inputQueueSize, input queue size of the packets that is waiting to be processed
	 */
	public void changeStreamQualityParameters(String streamId, String quality, double speed, int inputQueueSize) {
		long now = System.currentTimeMillis();
		if ((now - lastQualityUpdateTime) > 1000 &&
				((quality != null && !quality.equals(oldQuality)) || oldspeed == 0 || Math.abs(speed - oldspeed) > 0.05)) {

			lastQualityUpdateTime = now;
			getStreamHandler().setQualityParameters(streamId, quality, speed, inputQueueSize);
			oldQuality = quality;
			oldspeed = speed;
		}
	}

	public IAntMediaStreamHandler getStreamHandler() {
		if (appAdapter == null) {
			IContext context = MuxAdaptor.this.scope.getContext();
			ApplicationContext appCtx = context.getApplicationContext();
			//this returns the StreamApplication instance 
			appAdapter = (IAntMediaStreamHandler) appCtx.getBean("web.handler");
		}
		return appAdapter;
	}

	public AppSettings getAppSettings() {

		if (appSettings == null && scope.getContext().getApplicationContext().containsBean(AppSettings.BEAN_NAME)) {
			appSettings = (AppSettings) scope.getContext().getApplicationContext().getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}


	public DataStore getDataStore() {
		if (dataStore == null) {

			IDataStoreFactory dsf = (IDataStoreFactory) scope.getContext().getBean(IDataStoreFactory.BEAN_NAME);
			dataStore = dsf.getDataStore();
		}
		return dataStore;
	}
	
	public void writeStreamPacket(IStreamPacket packet) {
		
		
		if (packet.getDataType() == Constants.TYPE_VIDEO_DATA) 
		{
			int bodySize = packet.getData().limit();
			byte frameType = packet.getData().position(0).get();
			
			//we get 5 less bytes because first 5 bytes is related to the video tag. It's not part of the generic packet
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bodySize-5);
			byteBuffer.put(packet.getData().buf().position(5));
			
			synchronized (muxerList) 
			{
				for (Muxer muxer : muxerList) {
					muxer.writeVideoBuffer(byteBuffer, packet.getTimestamp(), 0, videoStreamIndex, (frameType & 0xF0) == IVideoStreamCodec.FLV_FRAME_KEY, 0);
				}
			}
			
		
		}
		else if (packet.getDataType() == Constants.TYPE_AUDIO_DATA) {
			
			if (!firstAudioPacketSkipped) {
				firstAudioPacketSkipped = true;
				return;
				
			}
			int bodySize = packet.getData().limit();
			//we get 2 less bytes because first 2 bytes is related to the audio tag. It's not part of the generic packet
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bodySize-2);
			byteBuffer.put(packet.getData().buf().position(2));
			
			
			synchronized (muxerList) 
			{
				for (Muxer muxer : muxerList) {
					muxer.writeAudioBuffer(byteBuffer, audioStreamIndex, packet.getTimestamp());
				}
			}
			
		}
	}
	
	
	
	public void execute() 
	{
		
		if (isPipeReaderJobRunning.compareAndSet(false, true)) 
		{
			if (!isRecording) {				
				
				if (checkStreamsStartTime == -1) {
					checkStreamsStartTime  = System.currentTimeMillis();
				}
				

				if (stopRequestExist) {
					logger.info("Stop request exists for stream:{}", streamId);
					broadcastStream.removeStreamListener(MuxAdaptor.this);
					logger.warn("closing adaptor for {} ", streamId);
					closeResources();
					logger.warn("closed adaptor for {}", streamId);
					isPipeReaderJobRunning.compareAndSet(true, false);
					return;
					
				}
				
				enableVideo = broadcastStream.getCodecInfo().hasVideo();
				enableAudio = broadcastStream.getCodecInfo().hasAudio();
				if (enableVideo && enableAudio) 
				{			
					logger.info("Video and audio is enabled in stream:{} queue size: {}", streamId, queueSize.get());
					try {
						prepare();
						isRecording = true;
					}
					catch(Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
						closeRtmpConnection();
					}
				}
				else {
					
					long totalTime = System.currentTimeMillis() - checkStreamsStartTime;
					int elapsedFrameTimeStamp = lastFrameTimestamp - firstReceivedFrameTimestamp;
										
					if ( (totalTime >= (2* maxAnalyzeDurationMS)) || (elapsedFrameTimeStamp >= maxAnalyzeDurationMS)) {
						logger.error("Total max time({}) is spent to determine video and audio existence for stream:{}. It's skipped waiting", (2*maxAnalyzeDurationMS), streamId);
						logger.info("Streams for {} enableVideo:{} enableAudio:{} total spend time: {} elapsed frame timestamp:{} stop request exists: {}", streamId, enableVideo, enableAudio, totalTime, elapsedFrameTimeStamp, stopRequestExist);
						
						//TODO: what if there is no audio and video
						try {
							prepare();
							isRecording = true;
						}
						catch(Exception e) {
							logger.error(ExceptionUtils.getStackTrace(e));
							closeRtmpConnection();
						}
					}
					else {
						//wait for total time
						//logger.warn("Returning from current task because it's checking if audio and video are enabled for stream: {}", streamId);
						isPipeReaderJobRunning.compareAndSet(true, false);
						//TODO: It's not to good leave in several places
						return;
					}
				}
			}
			
			
			IStreamPacket packet;
			while ((packet = streamPacketQueue.poll()) != null) {
				
				queueSize.decrementAndGet();
				updateQualityParameters(packet.getTimestamp(), TIME_BASE_FOR_MS);
				measureIngestTime(packet.getTimestamp(), ((CachedEvent)packet).getReceivedTime());
								
				if (!firstKeyFrameReceivedChecked && packet.getDataType() == Constants.TYPE_VIDEO_DATA) {
					
					
					byte frameType = packet.getData().position(0).get();
					
					if ((frameType & 0xF0) == IVideoStreamCodec.FLV_FRAME_KEY) {
						firstKeyFrameReceivedChecked = true;				
						if(!appAdapter.isValidStreamParameters(width, height, fps, 0, streamId)) {
							logger.info("Stream({}) has not passed specified validity checks so it's stopping", streamId);
							closeRtmpConnection();
							return;
						}
					} else {
						logger.warn("First video packet is not key frame. It will drop for direct muxing. Stream {}", streamId);
						// return if firstKeyFrameReceived is not received
						// below return is important otherwise it does not work with like some encoders(vidiu)
						return;
					}
				}
				
				
				
				if (bufferTimeMs == 0) 
				{
					writeStreamPacket(packet);
				}
				else if (bufferTimeMs > 0) 
				{
					bufferQueue.add(packet);
					IStreamPacket pktHead = bufferQueue.peek();
					
					if (pktHead != null) {
						int bufferedDuration = packet.getTimestamp() - pktHead.getTimestamp();
						
						if (bufferedDuration > bufferTimeMs*5) {
							//if buffered duration is more than 5 times of the buffer time, remove packets from the head until it reach bufferTimeMs * 2
	
							//set buffering true to not let writeBufferedPacket method work
							buffering = true;
							while ( (pktHead = bufferQueue.poll()) != null) {
								
								bufferedDuration = packet.getTimestamp() - pktHead.getTimestamp();
								if (bufferedDuration < bufferTimeMs * 2) {
									break;
								}
							}
						}
						
						if (pktHead != null) {
						
							bufferedDuration = packet.getTimestamp() - pktHead.getTimestamp();
							
							if (bufferedDuration > bufferTimeMs) 
							{ 
								if (buffering) 
								{
									//have the buffering finish time ms
									bufferingFinishTimeMs = System.currentTimeMillis();
									//have the first packet sent time
									firstPacketReadyToSentTimeMs  = packet.getTimestamp();
									logger.info("Switching buffering from true to false for stream: {}", streamId);
								}
								//make buffering false whenever bufferDuration is bigger than bufferTimeMS
								//buffering is set to true when there is no packet left in the queue
								buffering = false;
							}
							
							bufferLogCounter++;
							if (bufferLogCounter % COUNT_TO_LOG_BUFFER == 0) {
								logger.info("ReadPacket -> Buffering status {}, buffer duration {}ms buffer time {}ms stream: {}", buffering, bufferedDuration, bufferTimeMs, streamId);
								bufferLogCounter = 0;
							}
						}
					}
					
					
				}
				
			}
			
			if (stopRequestExist) {
				broadcastStream.removeStreamListener(MuxAdaptor.this);
				logger.warn("closing adaptor for {} ", streamId);
				closeResources();
				logger.warn("closed adaptor for {}", streamId);
			}
			
			
			isPipeReaderJobRunning.compareAndSet(true, false);
		}
	}
	

	private void measureIngestTime(int pktTimeStamp, long receivedTime) {
		
			totalIngestedVideoPacketCount++;
			
			totalIngestTime += (System.currentTimeMillis() - receivedTime);
			
			absoluteTotalIngestTime  += System.currentTimeMillis() - broadcastStream.getAbsoluteStartTimeMs() - pktTimeStamp;
		
	}
	
	public long getAbsoluteTimeMs() {
		if (broadcastStream != null) {
			return broadcastStream.getAbsoluteStartTimeMs();
		}
		return 0;
	}

	private void updateQualityParameters(long pts, AVRational timebase) {

		if (firstPacketTime == -1) {
			firstPacketTime = av_rescale_q(pts, timebase, TIME_BASE_FOR_MS);
			logger.info("first packet time original: {} scaled: {}", pts, firstPacketTime);
		}

		long currentTime = System.currentTimeMillis();
		long packetTime = av_rescale_q(pts, timebase, TIME_BASE_FOR_MS);

		elapsedTime = currentTime - startTime;

		double speed = 0L;
		if (elapsedTime > 0) {
			speed = (double) (packetTime - firstPacketTime) / elapsedTime;
			if (logger.isWarnEnabled() && Double.isNaN(speed)) {
				logger.warn("speed is NaN, packetTime: {}, first packetTime: {}, elapsedTime:{}", packetTime, firstPacketTime, elapsedTime);
			}
		}
		changeStreamQualityParameters(this.streamId, null, speed, getInputQueueSize());
	}

	private void closeRtmpConnection() {
		getBroadcastStream().stop();
		IStreamCapableConnection connection = getBroadcastStream().getConnection();
		if (connection != null) {
			connection.close();
		}
	}

	public void writePacket(AVStream stream, AVPacket pkt) {


		updateQualityParameters(pkt.pts(), stream.time_base());

		if (!firstKeyFrameReceivedChecked && stream.codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
			int keyFrame = pkt.flags() & AV_PKT_FLAG_KEY;
			if (keyFrame == 1) {
				firstKeyFrameReceivedChecked = true;				
				if(!appAdapter.isValidStreamParameters(width, height, fps, 0, streamId)) {
					logger.info("Stream({}) has not passed specified validity checks so it's stopping", streamId);
					closeRtmpConnection();
					return;
				}
			} else {
				logger.warn("First video packet is not key frame. It will drop for direct muxing. Stream {}", streamId);
				// return if firstKeyFrameReceived is not received
				// below return is important otherwise it does not work with like some encoders(vidiu)
				return;
			}
		}

		synchronized (muxerList) 
		{
			for (Muxer muxer : muxerList) {
				muxer.writePacket(pkt, stream);
			}
		}
	}

	public synchronized void writeTrailer() {
		for (Muxer muxer : muxerList) {
			muxer.writeTrailer();
		}
	}


	public synchronized void closeResources() {
		logger.info("close resources for streamId -> {}", streamId);


		if (packetPollerId != -1) {
			vertx.cancelTimer(packetPollerId);
			logger.info("Cancelling packet poller task(id:{}) for streamId: {}", packetPollerId, streamId);
			packetPollerId = -1;

		}

		if (bufferedPacketWriterId != -1) {
			logger.info("Removing buffered packet writer id {} for stream: {}", bufferedPacketWriterId, streamId);
			vertx.cancelTimer(bufferedPacketWriterId);
			bufferedPacketWriterId = -1;
			writeAllBufferedPackets();
		}

		writeTrailer();

		isRecording = false;

		changeStreamQualityParameters(this.streamId, null, 0, getInputQueueSize());
		getStreamHandler().muxAdaptorRemoved(this);
	}


	@Override
	public void start() {
		isRecording = false;
		logger.info("Number of items in the queue while adaptor is being started to prepare is {}", getInputQueueSize());
		startTimeMs = System.currentTimeMillis();

		vertx.executeBlocking(b -> {
			logger.info("before prepare for {}", streamId);
			Boolean successful = false;
			try {
					packetPollerId = vertx.setPeriodic(10, t-> 
						vertx.executeBlocking(p-> {
							try {
								execute();
							}
							catch (Exception e) {
								logger.error(ExceptionUtils.getStackTrace(e));
							}
							p.complete();
						}, false, r -> {
							//no care
						})
					);
					
					
					if (bufferTimeMs > 0)  
					{
						//this is just a simple hack to run in different context(different thread).
						logger.info("Scheduling the buffered packet writer for stream: {} buffer duration:{}ms", streamId, bufferTimeMs);
						bufferedPacketWriterId = vertx.setPeriodic(10, k -> 

								vertx.executeBlocking(p-> {
										try {
											writeBufferedPacket();
										}
										catch (Exception e) {
											logger.error(ExceptionUtils.getStackTrace(e));
										}
										p.complete();
									}, false, r -> {
										//no care
									})
						);

					}

					logger.info("Number of items in the queue while starting: {} for stream: {}", 
							getInputQueueSize(), streamId);
					
					successful = true;

			} 
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			b.complete(successful);
		}, 
		false,  // run unordered
		r -> 
			logger.info("muxadaptor start has finished with {} for stream: {}", r.result(), streamId)
		);
	}

	@Override
	public void stop() {
		logger.info("Calling stop for {} input queue size:{}", streamId, getInputQueueSize());
		stopRequestExist = true;
	}

	public int getInputQueueSize() {
		return queueSize .get();
	}


	/**
	 * This method is called when rtmpIngestBufferTime is bigger than zero
	 */
	public void writeBufferedPacket() 
	{
		synchronized (this) {
			
			if (isBufferedWriterRunning.compareAndSet(false, true)) {
				if (!buffering) 
				{
					while(!bufferQueue.isEmpty()) 
					{
						IStreamPacket tempPacket = bufferQueue.peek(); 
						long now = System.currentTimeMillis();
						long pktTimeDifferenceMs = tempPacket.getTimestamp() - firstPacketReadyToSentTimeMs; 
						long passedTime = now - bufferingFinishTimeMs;
						if (pktTimeDifferenceMs < passedTime) 
						{
							writeStreamPacket(tempPacket);
						
							bufferQueue.remove(); //remove the packet from the queue
						}
						else {
							break;
						}
	
					}
	
					//update buffering. If bufferQueue is empty, it should start buffering
					buffering = bufferQueue.isEmpty();
	
				}
				bufferLogCounter++; //we use this parameter in execute method as well 
				if (bufferLogCounter % COUNT_TO_LOG_BUFFER  == 0) {
					IStreamPacket streamPacket = bufferQueue.peek();
					int bufferedDuration = 0;
					if (streamPacket != null) {
						bufferedDuration = lastFrameTimestamp - streamPacket.getTimestamp();
					}
					logger.info("WriteBufferedPacket -> Buffering status {}, buffer duration {}ms buffer time {}ms stream: {}", buffering, bufferedDuration, bufferTimeMs, streamId);
					bufferLogCounter = 0;
				}
				isBufferedWriterRunning.compareAndSet(true, false);
			}
		}
	}

	private void writeAllBufferedPackets() 
	{
		synchronized (this) {
			logger.info("write all buffered packets for stream: {} ", streamId);
			while (!bufferQueue.isEmpty()) {
	
				IStreamPacket tempPacket = bufferQueue.poll();
				writeStreamPacket(tempPacket);
			}
		}
		
	}
	
	@Override
	public void packetReceived(IBroadcastStream stream, IStreamPacket packet) 
	{

		lastFrameTimestamp = packet.getTimestamp();
		if (firstReceivedFrameTimestamp  == -1) {
			logger.info("first received frame timestamp: {} for stream:{} ", lastFrameTimestamp, streamId);
			firstReceivedFrameTimestamp = lastFrameTimestamp;
		}
		queueSize.incrementAndGet();

		CachedEvent event = new CachedEvent();
		event.setData(packet.getData().duplicate());
		event.setDataType(packet.getDataType());
		event.setReceivedTime(System.currentTimeMillis());
		event.setTimestamp(packet.getTimestamp());

		streamPacketQueue.add(event);
	}

	@Override
	public boolean isRecording() {
		return isRecording;
	}

	@Override
	public boolean isAppending() {
		return false;
	}

	@Override
	public FileConsumer getFileConsumer() {
		return null;
	}

	@Override
	public void setFileConsumer(FileConsumer recordingConsumer) {
		//No need to implement
	}

	@Override
	public String getFileName() {
		return null;
	}

	@Override
	public void setFileName(String fileName) {
		//No need to implement
	}

	public List<Muxer> getMuxerList() {
		return muxerList;
	}


	public void setStorageClient(StorageClient storageClient) {
		this.storageClient = storageClient;
	}

	public boolean isWebRTCEnabled() {
		return webRTCEnabled;
	}

	public void setWebRTCEnabled(boolean webRTCEnabled) {
		this.webRTCEnabled = webRTCEnabled;
	}

	public void setHLSFilesDeleteOnExit(boolean deleteHLSFilesOnExit) {
		this.deleteHLSFilesOnExit = deleteHLSFilesOnExit;
	}

	public void setPreviewOverwrite(boolean overwrite) {
		this.previewOverwrite = overwrite;
	}


	public boolean isPreviewOverwrite() {
		return previewOverwrite;
	}

	public long getStartTime() {
		return startTime;
	}


	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public List<EncoderSettings> getEncoderSettingsList() {
		return encoderSettingsList;
	}

	public void setEncoderSettingsList(List<EncoderSettings> encoderSettingsList) {
		this.encoderSettingsList = encoderSettingsList;
	}

	public boolean isStreamSource() {
		return isStreamSource;
	}

	public void setStreamSource(boolean isStreamSource) {
		this.isStreamSource = isStreamSource;
	}

	public boolean isObjectDetectionEnabled() {
		return objectDetectionEnabled;
	}

	public void setObjectDetectionEnabled(Boolean objectDetectionEnabled) {
		this.objectDetectionEnabled = objectDetectionEnabled;
	}

	public int getPreviewCreatePeriod() {
		return previewCreatePeriod;
	}

	public void setPreviewCreatePeriod(int previewCreatePeriod) {
		this.previewCreatePeriod = previewCreatePeriod;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public long getFirstPacketTime() {
		return firstPacketTime;
	}

	public StorageClient getStorageClient() {
		return storageClient;
	}

	/**
	 * Setter for {@link #firstKeyFrameReceivedChecked}
	 *
	 * @param firstKeyFrameReceivedChecked
	 */
	public void setFirstKeyFrameReceivedChecked(boolean firstKeyFrameReceivedChecked) {
		this.firstKeyFrameReceivedChecked = firstKeyFrameReceivedChecked;
	}

	public Broadcast getBroadcast() {

		if (broadcast == null) {

			broadcast = dataStore.get(this.streamId);
		}
		return broadcast;
	}

	// this is for test cases
	public void setBroadcast(Broadcast broadcast) {
		this.broadcast = broadcast;
	}

	public int getPreviewHeight() {
		return previewHeight;
	}

	public void setPreviewHeight(int previewHeight) {
		this.previewHeight = previewHeight;
	}

	private Mp4Muxer createMp4Muxer() {
		Mp4Muxer mp4Muxer = new Mp4Muxer(storageClient, vertx);
		mp4Muxer.setAddDateTimeToSourceName(addDateTimeToMp4FileName);
		mp4Muxer.setBitstreamFilter(mp4Filtername);
		return mp4Muxer;
	}
	
	private WebMMuxer createWebMMuxer() {
		WebMMuxer webMMuxer = new WebMMuxer(storageClient, vertx);
		webMMuxer.setAddDateTimeToSourceName(addDateTimeToMp4FileName);
		return webMMuxer;
	}
		
	private Muxer addMp4Muxer() {
		Mp4Muxer mp4Muxer = createMp4Muxer();
		addMuxer(mp4Muxer);
		getDataStore().setMp4Muxing(streamId, RECORDING_ENABLED_FOR_STREAM);
		return mp4Muxer;
	}
	
	public boolean startRecording(RecordType recordType) {
		
		if (!isRecording) {
			logger.warn("Starting recording return false for stream:{} because stream is being prepared", streamId);
			return false;
		}
		
		if(isAlreadyRecording(recordType)) {
			logger.warn("Record is called while {} is already recording.", streamId);
			return true;
		}
		
		Muxer muxer = null;
		if(recordType == RecordType.MP4) {
			Mp4Muxer mp4Muxer = createMp4Muxer();
			mp4Muxer.setDynamic(true);
			muxer = mp4Muxer;
		} 
		else if(recordType == RecordType.WEBM) {
			WebMMuxer webMMuxer = createWebMMuxer();
			webMMuxer.setDynamic(true);
			muxer = webMMuxer;
		}
		else {
			logger.error("Unrecognized record type: {}", recordType);
		}
		
		boolean prepared = false;
		if (muxer != null) {
			muxer.init(scope, streamId, 0);
		
			AVCodecParameters videoParameters = getVideoCodecParameters();
			if (videoParameters != null) {
				muxer.addStream(videoParameters, TIME_BASE_FOR_MS);
			}
			
			AVCodecParameters audioParameters = getAudioCodecParameters();
			if (audioParameters != null) {
				muxer.addStream(audioParameters, TIME_BASE_FOR_MS);
			}
			
			prepared = muxer.prepareIO();
			if (prepared) {
				addMuxer(muxer);
			}
			else {
					logger.error("{} prepare method returned false. Recording is not started for {}", recordType, streamId);
			}
		}
		return prepared;
	}

	private boolean isAlreadyRecording(RecordType recordType) {
		for (Muxer muxer : muxerList) {
			if((muxer instanceof Mp4Muxer && recordType == RecordType.MP4)
					|| (muxer instanceof WebMMuxer && recordType == RecordType.WEBM)) {
				return true;
			}
		}
		return false;
	}


	public Muxer findDynamicRecordMuxer(RecordType recordType) {
		synchronized (muxerList) 
		{
			Iterator<Muxer> iterator = muxerList.iterator();
			while (iterator.hasNext()) 
			{
				Muxer muxer = iterator.next();
				if ((recordType == RecordType.MP4 && muxer instanceof Mp4Muxer)
						|| (recordType == RecordType.WEBM && muxer instanceof WebMMuxer)) {
					return muxer;
				}
			}
		}
		return null;
	}

	public boolean stopRecording(RecordType recordType) 
	{
		boolean result = false;
		Muxer muxer = findDynamicRecordMuxer(recordType);
		if (muxer != null) 
		{
			muxerList.remove(muxer);
			muxer.writeTrailer();
			result = true;
		}
		return result;
	}

	public ClientBroadcastStream getBroadcastStream() {
		return broadcastStream;
	}


	public boolean startRtmpStreaming(String rtmpUrl) 
	{
		if (!isRecording) {
			logger.warn("Start rtmp streaming return false for stream:{} because stream is being prepared", streamId);
			return false;
		}
		
		RtmpMuxer rtmpMuxer = new RtmpMuxer(rtmpUrl);
		rtmpMuxer.init(scope, streamId, 0);
		
		AVCodecParameters videoParameters = getVideoCodecParameters();
		if (videoParameters != null) {
			rtmpMuxer.addStream(videoParameters, TIME_BASE_FOR_MS);
		}
		
		AVCodecParameters audioParameters = getAudioCodecParameters();
		if (audioParameters != null) {
			rtmpMuxer.addStream(audioParameters, TIME_BASE_FOR_MS);
		}
		

		boolean prepared = rtmpMuxer.prepareIO();
		if (prepared) {
			addMuxer(rtmpMuxer);
		}
		else {
			logger.error("RTMP prepare returned false so that rtmp pushing to {} for {} didn't started ", rtmpUrl, streamId);
		}
		return prepared;
	}


	public RtmpMuxer getRtmpMuxer(String rtmpUrl) 
	{
		RtmpMuxer rtmpMuxer = null;
		synchronized (muxerList) 
		{
			Iterator<Muxer> iterator = muxerList.iterator();
			while (iterator.hasNext()) 
			{
				Muxer muxer = iterator.next();
				if (muxer instanceof RtmpMuxer &&
						((RtmpMuxer)muxer).getURL().equals(rtmpUrl)) 
				{
					rtmpMuxer = (RtmpMuxer) muxer;
					break;
				}
			}
		}
		return rtmpMuxer;
	}

	public boolean stopRtmpStreaming(String rtmpUrl) 
	{	
		RtmpMuxer rtmpMuxer = getRtmpMuxer(rtmpUrl);
		boolean result = false;
		if (rtmpMuxer != null) {
			muxerList.remove(rtmpMuxer);
			rtmpMuxer.writeTrailer();
			result = true;
		}
		return result;
	}

	public boolean isEnableVideo() {
		return enableVideo;
	}


	public void setEnableVideo(boolean enableVideo) {
		this.enableVideo = enableVideo;
	}


	public boolean isEnableAudio() {
		return enableAudio;
	}


	public void setEnableAudio(boolean enableAudio) {
		this.enableAudio = enableAudio;
	}


	public int getLastFrameTimestamp() {
		return lastFrameTimestamp;
	}


	public void setLastFrameTimestamp(int lastFrameTimestamp) {
		this.lastFrameTimestamp = lastFrameTimestamp;
	}

	public void setAppSettings(AppSettings appSettings) {
		this.appSettings = appSettings;
	}

	public long getBufferTimeMs() {
		return bufferTimeMs;
	}

	public boolean isBuffering() {
		return buffering;
	}

	public void setBuffering(boolean buffering) {
		this.buffering = buffering;
	}
	
	public String getDataChannelWebHookURL() {
		return dataChannelWebHookURL;
	}
	
	public boolean isDeleteDASHFilesOnExit() {
		return deleteDASHFilesOnExit;
	}


	public void setDeleteDASHFilesOnExit(boolean deleteDASHFilesOnExit) {
		this.deleteDASHFilesOnExit = deleteDASHFilesOnExit;
	}

	public boolean isAvc() {
		return avc;
	}
	
	public void setAvc(boolean avc) {
		this.avc = avc;
	}
	
	public Queue<IStreamPacket> getBufferQueue() {
		return bufferQueue;
	}
	
	public void setBufferingFinishTimeMs(long bufferingFinishTimeMs) {
		this.bufferingFinishTimeMs = bufferingFinishTimeMs;
	}
}


