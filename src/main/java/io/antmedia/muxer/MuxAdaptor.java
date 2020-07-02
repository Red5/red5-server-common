package io.antmedia.muxer;

import static org.bytedeco.javacpp.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.javacpp.avcodec.av_packet_free;
import static org.bytedeco.javacpp.avcodec.av_packet_move_ref;
import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.av_dump_format;
import static org.bytedeco.javacpp.avformat.av_read_frame;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avformat.avio_alloc_context;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.javacpp.avutil.AV_LOG_INFO;
import static org.bytedeco.javacpp.avutil.av_free;
import static org.bytedeco.javacpp.avutil.av_log_get_level;
import static org.bytedeco.javacpp.avutil.av_rescale_q;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVInputFormat;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avformat.Read_packet_Pointer_BytePointer_int;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVRational;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.scheduling.QuartzSchedulingService;
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
import io.antmedia.storage.StorageClient;
import io.vertx.core.Vertx;

public class MuxAdaptor implements IRecordingListener {

	private static final byte[] DEFAULT_STREAM_ID = new byte[]{(byte) (0 & 0xff), (byte) (0 & 0xff),
			(byte) (0 & 0xff)};
	private static final int HEADER_LENGTH = 9;
	private static final int TAG_HEADER_LENGTH = 11;
	public static final String ADAPTIVE_SUFFIX = "_adaptive";
	private static Logger logger = LoggerFactory.getLogger(MuxAdaptor.class);
	protected ConcurrentLinkedQueue<byte[]> inputQueue = new ConcurrentLinkedQueue<>();
	protected AtomicBoolean isPipeReaderJobRunning = new AtomicBoolean(false);
	private   AtomicBoolean isBufferedWriterRunning = new AtomicBoolean(false);
	protected AVIOContext avio_alloc_context;
	protected AVFormatContext inputFormatContext;

	protected List<Muxer> muxerList =  Collections.synchronizedList(new ArrayList<Muxer>());
	protected boolean deleteHLSFilesOnExit = true;

	protected boolean previewOverwrite = false;

	protected volatile boolean enableVideo = false;
	protected volatile boolean enableAudio = false;

	private long packetPollerId = -1;

	private Queue<AVPacket> bufferQueue = new ConcurrentLinkedQueue<>();

	public static class InputContext {
		public Queue<byte[]> queue;
		volatile boolean isHeaderWritten = false;
		volatile boolean stopRequestExist = false;
		public AtomicInteger queueSize = new AtomicInteger(0);
		public MuxAdaptor muxAdaptor;

		/*
		 * primitive enableVideo, enableAudio must be set in static getFLVHeader
		 * so we need to pass the muxAdaptor which contains these primitive fields
		 */
		public InputContext(MuxAdaptor muxAdaptor) {
			this.queue = muxAdaptor.inputQueue;
			this.muxAdaptor = muxAdaptor;
		}
	}

	private static Map<Pointer, InputContext> queueReferences = new ConcurrentHashMap<>();
	protected static final int BUFFER_SIZE = 4096;
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
	protected boolean objectDetectionEnabled;
	protected boolean webRTCEnabled = false;
	protected StorageClient storageClient;
	protected String hlsTime;
	protected String hlsListSize;
	protected String hlsPlayListType;
	List<EncoderSettings> adaptiveResolutionList = null;
	protected AVPacket pkt = avcodec.av_packet_alloc();
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
	private InputContext inputContext;
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
	private long streamInfoFindTime;
	protected boolean generatePreview = true;
	private int firstReceivedFrameTimestamp = -1;
	protected int totalIngestedVideoPacketCount = 0;
	protected long totalIngestTime = 0;
	private long bufferTimeMs = 0;

	private Queue<PacketTs> packetTsQueue = new ConcurrentLinkedQueue<>();
	protected Vertx vertx;
	private Queue<AVPacket> availableBufferQueue = new ConcurrentLinkedQueue<>();

	/**
	 * Accessed from multiple threads so make it volatile
	 */
	private volatile boolean buffering;
	private int bufferLogCounter;

	/**
	 * The time when buffering has been finished. It's volatile because it's accessed from multiple threads
	 */
	private volatile long bufferingFinishTimeMs = 0;

	private long bufferedPacketWriterId = -1;
	private volatile long lastPacketTimeMsInQueue = 0;
	private volatile long firstPacketReadyToSentTimeMs = 0;
	protected String dataChannelWebHookURL = null;
	private static final int COUNT_TO_LOG_BUFFER = 500;

	class PacketTs {
		int dts;
		long time;
		public PacketTs(int dts, long time) {
			this.dts = dts;
			this.time = time;
		}
	}

	static {
		TIME_BASE_FOR_MS = new AVRational();
		TIME_BASE_FOR_MS.num(1);
		TIME_BASE_FOR_MS.den(1000);
	}


	/*
	 * This callback has to be static. 
	 * Because it is set as a callback in native side.
	 * Unless it is static, the last one is called every time.
	 */
	private static Read_packet_Pointer_BytePointer_int readCallback = new Read_packet_Pointer_BytePointer_int() {

		@Override
		public int call(Pointer opaque, BytePointer buf, int bufSize) {
			int length = -1;
			try {
				InputContext inputContextLocal = queueReferences.get(opaque);
				if (inputContextLocal.isHeaderWritten) {
					byte[] packet = null;
					if (inputContextLocal.queue != null) {
						long waitCount = 0;
						while ((packet = inputContextLocal.queue.poll()) == null) {
							if (inputContextLocal.stopRequestExist) 
							{
								logger.info("stop request for stream id : {} ", inputContextLocal.muxAdaptor.getStreamId());
								break;
							}
							Thread.sleep(WAIT_TIME_MILLISECONDS);
							waitCount++;
							if (waitCount % 50 == 0) {
								long totalWaitTime = waitCount * WAIT_TIME_MILLISECONDS;
								logger.warn("Stream: {} does not get packet for {} ms",inputContextLocal.muxAdaptor.getStreamId(), totalWaitTime);
								long maxWaitTime = 10*inputContextLocal.muxAdaptor.maxAnalyzeDurationMS;
								if (totalWaitTime > maxWaitTime) {
									logger.warn("Total wait time:{} for receiving packet exceeds maxWaitTime(10* max analyze duration):{} breaking the loop for stream:{}", totalWaitTime, maxWaitTime, inputContextLocal.muxAdaptor.getStreamId());
									inputContextLocal.stopRequestExist = true;
									break;
								}
							}
							
						}
						inputContextLocal.queueSize.decrementAndGet();

					} else {
						logger.error("input queue null for stream id: {}", inputContextLocal.muxAdaptor.getStreamId());
					}

					if (packet != null) {
						// ** this setting critical..
						length = packet.length;
						buf.put(packet, 0, length);
					} else {
						logger.info("packet is null and return length is {}", length);
					}
				} 
				else {

					
					if (inputContextLocal.stopRequestExist) 
					{
						logger.warn("Stop request is received for stream:{} before it checks streams", inputContextLocal.muxAdaptor.streamId);
					}
					else {
						logger.info("Checking streams for stream: {}", inputContextLocal.muxAdaptor.streamId);
						if (inputContextLocal.muxAdaptor.checkStreams()) {
							inputContextLocal.isHeaderWritten = true;
							byte[] flvHeader = getFLVHeader(inputContextLocal.muxAdaptor);
							length = flvHeader.length;
	
							buf.put(flvHeader, 0, length);
						}
					}
				}

			} catch (Exception e) {
				logger.error("Exception handling queue", e);
			}

			return length;
		}

	};

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
		inputContext = new InputContext(this);
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
		mp4MuxingEnabled = appSettingsLocal.isMp4MuxingEnabled();
		webMMuxingEnabled = appSettingsLocal.isWebMMuxingEnabled();
		objectDetectionEnabled = appSettingsLocal.isObjectDetectionEnabled();

		addDateTimeToMp4FileName = appSettingsLocal.isAddDateTimeToMp4FileName();
		mp4Filtername = null;
		webRTCEnabled = appSettingsLocal.isWebRTCEnabled();
		deleteHLSFilesOnExit = appSettingsLocal.isDeleteHLSFilesOnEnded();
		hlsListSize = appSettingsLocal.getHlsListSize();
		hlsTime = appSettingsLocal.getHlsTime();
		hlsPlayListType = appSettingsLocal.getHlsPlayListType();
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

		initializeDataStore();
		enableSettings();
		initStorageClient();
		enableMp4Setting();
		//TODO enableWebMSetting();
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

		for (Muxer muxer : muxerList) {
			muxer.init(scope, streamId, 0);
		}
		getStreamHandler().muxAdaptorAdded(this);
		return true;
	}


	private void initVertx() {
		if (scope.getContext().getApplicationContext().containsBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)) {
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

		inputFormatContext = avformat.avformat_alloc_context();
		if (inputFormatContext == null) 
		{
			logger.info("cannot allocate input context");
			return false;
		}

		Pointer pointer = avutil.av_malloc(BUFFER_SIZE);
		avio_alloc_context = avio_alloc_context(new BytePointer(pointer), BUFFER_SIZE, 0,
				inputFormatContext, getReadCallback(), null, null);

		inputFormatContext.pb(avio_alloc_context);

		queueReferences.put(inputFormatContext, inputContext);

		logger.info("before avformat_open_input for stream {}", streamId);

		AVInputFormat findInputFormat = avformat.av_find_input_format("flv");
		if (avformat_open_input(inputFormatContext, (String) null, findInputFormat,
				(AVDictionary) null) < 0) {
			logger.error("cannot open input context for stream: {}", streamId);
			findInputFormat.close();
			pointer.close();
			return false;
		}

		long startFindStreamInfoTime = System.currentTimeMillis();

		logger.info("before avformat_find_stream_info for stream: {}", streamId);
		int ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			logger.info("Could not find stream information for stream {}", streamId);
			findInputFormat.close();
			pointer.close();
			return false;
		}
		

		logger.info("avformat_find_stream_info takes {}ms for stream:{}", System.currentTimeMillis() - startFindStreamInfoTime, streamId);

		return prepareInternal(inputFormatContext);
	}

	public boolean prepareInternal(AVFormatContext inputFormatContext) throws Exception {
		//StreamFetcher Worker Thread only calls prepareInternal so that inputFormatContext is set here
		this.inputFormatContext = inputFormatContext;
		return prepareMuxers(inputFormatContext);
	}

	public boolean prepareMuxers(AVFormatContext inputFormatContext) throws Exception {

		if (av_log_get_level() >= AV_LOG_INFO) {
			// Dump information about file onto standard error
			av_dump_format(inputFormatContext, 0, streamId, 0);
		}

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

	protected Read_packet_Pointer_BytePointer_int getReadCallback() {
		return readCallback;
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


	private DataStore initializeDataStore() {
		if (dataStore == null) {

			IDataStoreFactory dsf = (IDataStoreFactory) scope.getContext().getBean(IDataStoreFactory.BEAN_NAME);
			dataStore = dsf.getDataStore();
		}
		return dataStore;
	}

	public void execute()  
	{
		if (isPipeReaderJobRunning.compareAndSet(false, true)) 
		{
			while (true) 
			{
				if (inputFormatContext == null) {
					break;
				}
				int ret = av_read_frame(inputFormatContext, pkt);
				if (ret >= 0) 
				{

					measureIngestTime();

					if (bufferTimeMs == 0) 
					{ //if there is no buffer 
						writePacket(inputFormatContext.streams(pkt.stream_index()), pkt);
						av_packet_unref(pkt);
					}
					else if (bufferTimeMs > 0)
					{
						AVPacket packet = getAVPacket();
						av_packet_ref(packet, pkt);
						av_packet_unref(pkt);
						bufferQueue.add(packet);

						
						AVPacket pktHead = bufferQueue.peek();
						lastPacketTimeMsInQueue = av_rescale_q(packet.pts(), inputFormatContext.streams(packet.stream_index()).time_base(), TIME_BASE_FOR_MS);
						if (getBufferedDurationMs() > bufferTimeMs*5) 
						{
							//set buffering true to not let writeBufferedPacket method work
							
							buffering = true;
							//if buffer duration somehow is more than 5 times bufferTimeMs
							logger.warn("Buffer is increased({}) too much for stream: {}", getBufferedDurationMs() ,streamId);
							AVPacket pkt;
							int i = 0;
							while ((pkt = bufferQueue.poll()) != null) 
							{
								pkt.close();
								if (i % 10 == 0) 
								{
									i = 0;
									if (getBufferedDurationMs() < bufferTimeMs*2) {
										break;
									}
								}
								i++;
							}
							
							logger.warn("Buffer duration is decreased to {} for stream: {}", getBufferedDurationMs(), streamId);
							pktHead = bufferQueue.peek();
						}

						/**
						 * BufferQueue is polled in writer thread. 
						 * It's a very rare case to happen so that check if it's null
						 */
						if (pktHead != null) {
							long firstPacketTimeMsInQueue = av_rescale_q(pktHead.pts(), inputFormatContext.streams(pktHead.stream_index()).time_base(), TIME_BASE_FOR_MS);
							long bufferDuration = (lastPacketTimeMsInQueue - firstPacketTimeMsInQueue);
							
							
							if (bufferDuration > bufferTimeMs) 
							{ 
								if (buffering) 
								{
									//have the buffering finish time ms
									bufferingFinishTimeMs = System.currentTimeMillis();
									//have the first packet sent time
									firstPacketReadyToSentTimeMs  = firstPacketTimeMsInQueue;
									logger.info("Switching buffering from true to false for stream: {}", streamId);
								}
								//make buffering false whenever bufferDuration is bigger than bufferTimeMS
								//buffering is set to true when there is no packet left in the queue
								buffering = false;

							}
							
							bufferLogCounter++;
							if (bufferLogCounter % COUNT_TO_LOG_BUFFER == 0) {
								logger.info("ReadPacket -> Buffering status {}, buffer duration {}ms buffer time {}ms stream: {} bufferQueue size: {}", buffering, bufferDuration, bufferTimeMs, streamId, bufferQueue.size());
								bufferLogCounter = 0;
							}
						}	
					}
					else {
						logger.error("Wrong buffertimeMs {} for stream: {}", bufferTimeMs, streamId);
					}


				} 
				else {
					if (broadcastStream != null) {
						broadcastStream.removeStreamListener(MuxAdaptor.this);
					}
					logger.warn("closing adaptor for {}", streamId);
					closeResources();
					logger.warn("closed adaptor for {}", streamId);
					closeRtmpConnection();
					
				}
				// if there is not element in the qeueue,
				// break the loop
				if (inputQueue.peek() == null || inputFormatContext == null) {
					break;
				}
			}


			isPipeReaderJobRunning.compareAndSet(true, false);
		}

	}


	private void measureIngestTime() {
		if (inputFormatContext.streams(pkt.stream_index()).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) 
		{
			totalIngestedVideoPacketCount++;
			int dts = (int) pkt.dts();
			PacketTs packetTs = packetTsQueue.poll();

			if (packetTs.dts != dts) 
			{
				logger.warn("Packet dts({}) queue value does not match with the native dts({}) for stream:{} try one more attempt", 
						packetTs.dts, dts, streamId);
				packetTs = packetTsQueue.poll();
				if (packetTs.dts != dts) 
				{
					logger.warn("Packet dts({}) and nativ dts({}) does not match. "
							+ " Total ingest time stats may not be correct for stream:{}", packetTs.dts, dts, streamId);
				}

			}
			long queueEntranceTime = packetTs.time;
			totalIngestTime += (System.currentTimeMillis() - queueEntranceTime);
		}
	}

	public long getBufferedDurationMs() {
		AVPacket pktHead = bufferQueue.peek();
		if (pktHead != null) {
			long firstPacketInQueueTime = av_rescale_q(pktHead.pts(), inputFormatContext.streams(pktHead.stream_index()).time_base(), TIME_BASE_FOR_MS);
			return lastPacketTimeMsInQueue - firstPacketInQueueTime;
		}
		return 0;
	}

	private void updateQualityParameters(long pts, AVRational timebase) {

		if (firstPacketTime == -1) {
			firstPacketTime = av_rescale_q(pts, timebase, TIME_BASE_FOR_MS);
			logger.info("first packet time {}", firstPacketTime);
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
				if(!appAdapter.isValidStreamParameters(inputFormatContext, pkt)) {
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
		//This is allocated and needs to free for every case
		if (pkt != null) {
			av_packet_free(pkt);
			pkt.close();
			pkt = null;
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

		if (inputFormatContext != null) {
			queueReferences.remove(inputFormatContext);
		}

		avformat_close_input(inputFormatContext);

		if (avio_alloc_context != null) {
			if (avio_alloc_context.buffer() != null) {
				av_free(avio_alloc_context.buffer());
				avio_alloc_context.buffer(null);
			}
			av_free(avio_alloc_context);
			avio_alloc_context = null;
		}

		inputFormatContext = null;
		isRecording = false;

		changeStreamQualityParameters(this.streamId, null, 0, getInputQueueSize());
		getStreamHandler().muxAdaptorRemoved(this);
	}


	public static byte[] getFLVFrame(IStreamPacket packet) throws IOException {
		/**
		 * Tag header = 11 bytes |-|---|----|---| 0 = type 1-3 = data size 4-7 =
		 * timestamp 8-10 = stream id (always 0) Tag data = variable bytes
		 * Previous tag = 4 bytes (tag header size + tag data size)
		 *
		 * ITag tag = new Tag(); tag.setDataType(packet.getDataType());
		 * tag.setBodySize(data.limit());
		 * tag.setTimestamp(packet.getTimestamp());
		 */
		// skip tags with no data
		int bodySize = packet.getData().limit();
		// ensure that the channel is still open
		// get the data type
		byte dataType = packet.getDataType();
		// if we're writing non-meta tags do seeking and tag size update

		// set a var holding the entire tag size including the previous tag
		// length
		int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
		// resize
		// create a buffer for this tag
		ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
		// get the timestamp
		int timestamp = packet.getTimestamp();
		// allow for empty tag bodies
		byte[] bodyBuf = null;
		if (bodySize > 0) {
			// create an array big enough
			bodyBuf = new byte[bodySize];
			// put the bytes into the array
			packet.getData().position(0);
			packet.getData().get(bodyBuf);
			// get the audio or video codec identifier
			packet.getData().position(0);

		}

		// Data Type
		IOUtils.writeUnsignedByte(tagBuffer, dataType); // 1
		// Body Size - Length of the message. Number of bytes after StreamID to
		// end of tag
		// (Equal to length of the tag - 11)
		IOUtils.writeMediumInt(tagBuffer, bodySize); // 3
		// Timestamp
		IOUtils.writeExtendedMediumInt(tagBuffer, timestamp); // 4
		// Stream id
		tagBuffer.put(DEFAULT_STREAM_ID); // 3
		// get the body if we have one
		if (bodyBuf != null) {
			tagBuffer.put(bodyBuf);
		}
		// we add the tag size
		tagBuffer.putInt(TAG_HEADER_LENGTH + bodySize);
		// flip so we can process from the beginning
		tagBuffer.flip();
		// write the tag

		return tagBuffer.array();

	}

	public static byte[] getFLVHeader(MuxAdaptor muxAdaptor) {
		org.red5.io.flv.FLVHeader flvHeader = new org.red5.io.flv.FLVHeader();
		flvHeader.setFlagVideo(muxAdaptor.isEnableVideo());
		flvHeader.setFlagAudio(muxAdaptor.isEnableAudio());
		// create a buffer
		ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH + 4); // FLVHeader
		// (9 bytes)
		// +
		// PreviousTagSize0
		// (4 bytes)
		flvHeader.write(header);
		return header.array();
	}

	public boolean checkStreams() throws InterruptedException 
	{
		if(broadcastStream != null) 
		{
			long checkStreamsStartTime = System.currentTimeMillis();
			long totalTime = 0;
			long frameElapsedTimestamp = 0;
			while( frameElapsedTimestamp < maxAnalyzeDurationMS
					&& totalTime < (2* maxAnalyzeDurationMS) && !inputContext.stopRequestExist)
			{
				enableVideo = broadcastStream.getCodecInfo().hasVideo();
				enableAudio = broadcastStream.getCodecInfo().hasAudio();
				if (enableVideo && enableAudio) {
					logger.info("Video and Audio is detected in the incoming stream for stream: {}", streamId);
					break;
				}

				//sleeping is not something we like. But it seems the best option for this case
				Thread.sleep(5);
				totalTime = System.currentTimeMillis() - checkStreamsStartTime;
				frameElapsedTimestamp = lastFrameTimestamp - firstReceivedFrameTimestamp;
			}

			if ( totalTime >= (2* maxAnalyzeDurationMS)) {
				logger.error("Total max time({}) is spent to determine video and audio existence for stream:{}. It's skipped waiting", (2*maxAnalyzeDurationMS), streamId);
			}

			logger.info("Streams for {} enableVideo:{} enableAudio:{} total spend time: {} elapsed frame timestamp: {} stop request exists: {}", streamId, enableVideo, enableAudio, totalTime, frameElapsedTimestamp, inputContext.stopRequestExist);
		}
		else {
			logger.warn("broadcastStream is null while checking streams for {}", streamId);
		}

		//return true if video or audio tracks enable
		return enableVideo || enableAudio;
	}


	@Override
	public void start() {
		isRecording = false;
		logger.info("Number of items in the queue while adaptor is being started to prepare is {}", getInputQueueSize());


		vertx.setTimer(1, h -> {
			logger.info("before prepare for {}", streamId);
			try {
				if (prepare()) {

					logger.info("after prepare for {}", streamId);
					isRecording = true;

					packetPollerId = vertx.setPeriodic(10, t-> 
						vertx.executeBlocking(p-> {
							execute();
							p.complete();
						}, false, r -> {
							//no care
						})
					);
					
					
					if (bufferTimeMs > 0)  
					{
						//this is just a simple hack to run in different context(different thread).
						//TODO: Eventually we need to get rid of avformat_find_streaminfo and {@link#readCallback}	
						logger.info("Scheduling the buffered packet writer for stream: {} buffer duration:{}ms", streamId, bufferTimeMs);
						bufferedPacketWriterId = vertx.setPeriodic(10, k -> 

								vertx.executeBlocking(p-> {
										writeBufferedPacket();
										p.complete();
									}, false, r -> {
										//no care
									})
						);

					}

					logger.info("Number of items in the queue while starting: {} for stream: {}", 
							getInputQueueSize(), streamId);

				} else {
					logger.warn("input format context cannot be created for stream -> {}", streamId);
					if (broadcastStream != null) {
						broadcastStream.removeStreamListener(MuxAdaptor.this);
					}
					logger.warn("closing adaptor for {}", streamId);
					closeResources();
					logger.warn("closed adaptor for {}", streamId);
					closeRtmpConnection();
				}
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		});
	}

	@Override
	public void stop() {
		logger.info("Calling stop for {}", streamId);
		if (inputFormatContext == null) {
			logger.warn("Mux adaptor stopped returning for {}", streamId);
			return;
		}
		InputContext inputContextRef = queueReferences.get(inputFormatContext);
		if (inputContextRef != null) {
			inputContextRef.stopRequestExist = true;
		}
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
						AVPacket tempPacket = bufferQueue.peek(); 
						long pktTime = av_rescale_q(tempPacket.pts(), inputFormatContext.streams(tempPacket.stream_index()).time_base(), TIME_BASE_FOR_MS);
						long now = System.currentTimeMillis();
						long pktTimeDifferenceMs = pktTime - firstPacketReadyToSentTimeMs; 
						long passedTime = now - bufferingFinishTimeMs;
						if (pktTimeDifferenceMs < passedTime) 
						{
							writePacket(inputFormatContext.streams(tempPacket.stream_index()), tempPacket);
							av_packet_unref(tempPacket);
							bufferQueue.remove(); //remove the packet from the queue
							availableBufferQueue.offer(tempPacket); //make packet available for new incoming packets
						}
						else {
							//break the loop and don't block the thread because it's not correct time to send the packet
							break;
						}
	
					}
	
					//update buffering. If bufferQueue is empty, it should start buffering
					buffering = bufferQueue.isEmpty();
	
				}
				bufferLogCounter++; //we use this parameter in execute method as well 
				if (bufferLogCounter % COUNT_TO_LOG_BUFFER  == 0) {
					logger.info("WriteBufferedPacket -> Buffering status {}, buffer duration {}ms buffer time {}ms stream: {}", buffering, getBufferedDurationMs(), bufferTimeMs, streamId);
					bufferLogCounter = 0;
				}
				isBufferedWriterRunning.compareAndSet(true, false);
			}
		}
	}

	private void writeAllBufferedPackets() 
	{
		synchronized (this) {
			logger.info("write all buffered packets for stream: {} buffered queue size: {} available buffer size:{}", streamId, bufferQueue.size(), availableBufferQueue.size());
			while (!bufferQueue.isEmpty()) {
	
				AVPacket tempPacket = bufferQueue.poll();
				writePacket(inputFormatContext.streams(tempPacket.stream_index()), tempPacket);
				av_packet_unref(tempPacket);
			}
	
			AVPacket pkt;
			while ((pkt = bufferQueue.poll()) != null) {
				pkt.close();
			}
			
			while ((pkt = availableBufferQueue.poll()) != null) {
				pkt.close();
			}
		}
		
	}

	int packetCount = 0;
	@Override
	public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
		if(packet.getDataType() == Constants.TYPE_VIDEO_DATA) {
			packetCount++;
			packetTsQueue.add(new PacketTs(packet.getTimestamp(), System.currentTimeMillis()));
		}
		byte[] flvFrame;
		try {
			flvFrame = getFLVFrame(packet);

			lastFrameTimestamp = packet.getTimestamp();
			if (firstReceivedFrameTimestamp  == -1) {
				firstReceivedFrameTimestamp = lastFrameTimestamp;
			}
			if (flvFrame.length <= BUFFER_SIZE) {
				addPacketToQueue(flvFrame);
			} else {
				int numberOfBytes = flvFrame.length;
				int startIndex = 0;
				int copySize = 0;
				while (numberOfBytes != 0) {
					if (numberOfBytes > BUFFER_SIZE) {
						copySize = BUFFER_SIZE;
					} else {
						copySize = numberOfBytes;
					}
					byte[] data = Arrays.copyOfRange(flvFrame, startIndex, startIndex + copySize);
					addPacketToQueue(data);
					numberOfBytes -= copySize;
					startIndex += copySize;
				}
			}

		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}


	private void addPacketToQueue(byte[] data) {
		inputQueue.add(data);
		inputContext.queueSize.incrementAndGet();
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

	public int getInputQueueSize() {
		return inputContext.queueSize.get();
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

	private Muxer addMp4Muxer() {
		Mp4Muxer mp4Muxer = createMp4Muxer();
		addMuxer(mp4Muxer);
		return mp4Muxer;
	}

	public boolean startRecording(RecordType recordType) {
		
		if (!isRecording) {
			logger.warn("Starting recording return false for stream:{} because stream is being prepared", streamId);
			return false;
		}
		Muxer muxer = null;
		if(recordType == RecordType.MP4) {
			Mp4Muxer mp4Muxer = createMp4Muxer();
			mp4Muxer.setDynamic(true);
			muxer = mp4Muxer;
		} 
		else if(recordType == RecordType.WEBM) {
			WebMMuxer webMMuxer = new WebMMuxer(storageClient, vertx);
			webMMuxer.setAddDateTimeToSourceName(addDateTimeToMp4FileName);
			webMMuxer.setDynamic(true);
			muxer = webMMuxer;
		}
		muxer.init(scope, streamId, 0);
		
		boolean prepared = muxer.prepare(inputFormatContext);
		if (prepared) {
			addMuxer(muxer);
		}
		else {
				logger.error("{} prepare method returned false. Recording is not started for {}", recordType.toString(), streamId);
		}
		return prepared;
	}

	public AVPacket getAVPacket() {
		if (!availableBufferQueue.isEmpty()) {
			return availableBufferQueue.poll();
		}
		return new AVPacket();
	}


	public Muxer findDynamicRecordMuxer(RecordType recordType) {
		synchronized (muxerList) 
		{
			Iterator<Muxer> iterator = muxerList.iterator();
			while (iterator.hasNext()) 
			{
				Muxer muxer = iterator.next();
				if ((recordType == RecordType.MP4 && muxer instanceof Mp4Muxer && ((Mp4Muxer) muxer).isDynamic())
						|| (recordType == RecordType.WEBM && muxer instanceof WebMMuxer && ((WebMMuxer) muxer).isDynamic())) {
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
			
		boolean prepared = rtmpMuxer.prepare(inputFormatContext);
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

	public AVFormatContext getInputFormatContext() {
		return inputFormatContext;
	}


	public int getLastFrameTimestamp() {
		return lastFrameTimestamp;
	}


	public void setLastFrameTimestamp(int lastFrameTimestamp) {
		this.lastFrameTimestamp = lastFrameTimestamp;
	}


	public long getStreamInfoFindTime() {
		return streamInfoFindTime;
	}


	public static Map<Pointer, InputContext> getQueueReferences() {
		return queueReferences;
	}


	public static void setQueueReferences(Map<Pointer, InputContext> queueReferences) {
		MuxAdaptor.queueReferences = queueReferences;
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

	public Queue<AVPacket> getBufferQueue() {
		return bufferQueue;
	}

	public void setInputFormatContext(AVFormatContext inputFormatContext) {
		this.inputFormatContext = inputFormatContext;
	}
	
	public String getDataChannelWebHookURL() {
		return dataChannelWebHookURL;
	}
}


