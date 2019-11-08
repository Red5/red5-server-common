package io.antmedia.muxer;

import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.storage.StorageClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.avcodec.*;
import org.bytedeco.javacpp.avformat.*;
import org.bytedeco.javacpp.avutil.*;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.IRecordingListener;
import org.red5.server.stream.consumer.FileConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;

public class MuxAdaptor implements IRecordingListener, IScheduledJob {

	private static final byte[] DEFAULT_STREAM_ID = new byte[]{(byte) (0 & 0xff), (byte) (0 & 0xff),
			(byte) (0 & 0xff)};
	private static final int HEADER_LENGTH = 9;
	private static final int TAG_HEADER_LENGTH = 11;
	public static final String ADAPTIVE_SUFFIX = "_adaptive";
	protected QuartzSchedulingService scheduler;
	private static Logger logger = LoggerFactory.getLogger(MuxAdaptor.class);
	protected String packetFeederJobName = null;
	protected ConcurrentLinkedQueue<byte[]> inputQueue = new ConcurrentLinkedQueue<>();
	protected AtomicBoolean isPipeReaderJobRunning = new AtomicBoolean(false);
	protected AVIOContext avio_alloc_context;
	protected AVFormatContext inputFormatContext;

	protected List<Muxer> muxerList =  Collections.synchronizedList(new ArrayList<Muxer>());
	protected boolean deleteHLSFilesOnExit = true;

	protected int receivedPacketCount;
	protected boolean previewOverwrite = false;
	
	protected boolean enableVideo = false;
	protected boolean enableAudio = false;

	public static class InputContext {
		public final Queue<byte[]> queue;
		volatile boolean isHeaderWritten = false;
		volatile boolean stopRequestExist = false;
		public final AtomicInteger queueSize = new AtomicInteger(0);

		public InputContext(Queue<byte[]> inputQueue) {
			this.queue = inputQueue;
		}
	}

	protected static Map<Pointer, InputContext> queueReferences = new ConcurrentHashMap<>();
	protected static final int BUFFER_SIZE = 4096;
	public static final String QUALITY_GOOD = "good";
	public static final String QUALITY_AVERAGE = "average";
	public static final String QUALITY_POOR = "poor";
	public static final String QUALITY_NA = "NA";
	public static final int MP4_ENABLED_FOR_STREAM = 1;
	public static final int MP4_DISABLED_FOR_STREAM = -1;
	public static final int MP4_NO_SET_FOR_STREAM = 0;
	protected boolean isRecording = false;
	protected ClientBroadcastStream broadcastStream;
	protected boolean mp4MuxingEnabled;
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
	private AVRational timeBaseForMS;
	private InputContext inputContext;
	private IAntMediaStreamHandler appAdapter;

	private String mp4Filtername;
	protected List<EncoderSettings> encoderSettingsList;
	protected long timeDiffBetweenVideoandElapsed;
	protected long elapsedTime;
	protected static boolean isStreamSource = false;

	private int previewCreatePeriod;
	private double oldspeed;
	private long firstPacketTime = -1;
	private boolean audioOnly = false;
	private long lastQualityUpdateTime = 0;
	private Broadcast broadcast;
	private AppSettings appSettings;
	private int previewHeight;
	private long streamInfoFindTime;


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
						while ((packet = inputContextLocal.queue.poll()) == null) {
							if (inputContextLocal.stopRequestExist) {
								logger.info("stop request ");
								break;
							}
							Thread.sleep(5);
						}
						inputContextLocal.queueSize.decrementAndGet();

					} else {
						logger.error("input queue null");
					}

					if (packet != null) {
						// ** this setting critical..
						length = packet.length;
						buf.put(packet, 0, length);
					} else {
						logger.info("packet is null and return length is {}", length);
					}
				} else {
					inputContextLocal.isHeaderWritten = true;
					logger.info("writing header");
					byte[] flvHeader = getFLVHeader();
					length = flvHeader.length;

					buf.put(flvHeader, 0, length);
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
		
		timeBaseForMS = new AVRational();
		timeBaseForMS.num(1);
		timeBaseForMS.den(1000);
		inputContext = new InputContext(inputQueue);
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
		setPreviewHeight(appSettingsLocal.getPreviewHeight());
	}

	public void initStorageClient() {
		if (scope.getContext().getApplicationContext().containsBean(StorageClient.BEAN_NAME)) {
			storageClient = (StorageClient) scope.getContext().getApplicationContext().getBean(StorageClient.BEAN_NAME);
		}
	}

	protected void initScheduler() {
		scheduler = (QuartzSchedulingService) scope.getParent().getContext().getBean(QuartzSchedulingService.BEAN_NAME);

	}

	@Override
	public boolean init(IScope scope, String streamId, boolean isAppend) {

		this.streamId = streamId;
		this.scope = scope;
		initScheduler();
		if (scheduler == null) {
			logger.warn("scheduler is not available in beans for {}", streamId);
			return false;
		}

		initializeDataStore();
		enableSettings();
		initStorageClient();
		enableMp4Setting();

		if (mp4MuxingEnabled) {
			addMp4Muxer();
			logger.info("adding MP4 Muxer, add datetime to file name {}", addDateTimeToMp4FileName);
		}

		if (hlsMuxingEnabled) {
			HLSMuxer hlsMuxer = new HLSMuxer(scheduler, hlsListSize, hlsTime, hlsPlayListType, getAppSettings().getHlsFlags());
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


	protected void enableMp4Setting() {
		broadcast = getBroadcast();

		if (broadcast != null) 
		{
			if (broadcast.getMp4Enabled() == MP4_DISABLED_FOR_STREAM) 
			{
				// if stream specific mp4 setting is disabled
				mp4MuxingEnabled = false;
			} 
			else if (broadcast.getMp4Enabled() == MP4_ENABLED_FOR_STREAM) 
			{
				// if stream specific mp4 setting is enabled
				mp4MuxingEnabled = true;
			}
		}
	}


	public boolean prepare() throws Exception {

		AVFormatContext inputFormatContextLocal = avformat.avformat_alloc_context();
		if (inputFormatContextLocal == null) 
		{
			logger.info("cannot allocate input context");
			return false;
		}

		avio_alloc_context = avio_alloc_context(new BytePointer(avutil.av_malloc(BUFFER_SIZE)), BUFFER_SIZE, 0,
				inputFormatContextLocal, getReadCallback(), null, null);

		inputFormatContextLocal.pb(avio_alloc_context);
		
		inputFormatContextLocal.probesize(getAppSettings().getProbeSize());

		queueReferences.put(inputFormatContextLocal, inputContext);

		int ret;
		logger.debug("before avformat_open_input for stream {}", streamId);

		if (avformat_open_input(inputFormatContextLocal, (String) null, avformat.av_find_input_format("flv"),
				(AVDictionary) null) < 0) {
			logger.error("cannot open input context for stream: {}", streamId);
			return false;
		}

		long now = System.currentTimeMillis();

		logger.info("before avformat_find_sream_info for stream: {}", streamId);
		ret = avformat_find_stream_info(inputFormatContextLocal, (AVDictionary) null);
		if (ret < 0) {
			logger.info("Could not find stream information for stream {}", streamId);
			return false;
		}
		streamInfoFindTime = System.currentTimeMillis() - now;
		logger.info("avformat_find_stream_info takes {}ms for stream:{}"
				+ " number of streams: {}", streamInfoFindTime, streamId, inputFormatContextLocal.nb_streams());

		
		

		return prepareInternal(inputFormatContextLocal);
	}

	public boolean prepareInternal(AVFormatContext inputFormatContext) throws Exception {
		this.inputFormatContext = inputFormatContext;
		
		for (int i=0; i < inputFormatContext.nb_streams(); i++) 
		{
			AVStream inStream = inputFormatContext.streams(i);
			int codecType = inStream.codecpar().codec_type();
			if ( codecType == AVMEDIA_TYPE_VIDEO) {
				enableVideo = true;
			}
			else if (codecType == AVMEDIA_TYPE_AUDIO) {
				enableAudio = true;
			}
		}
		
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
			appAdapter = (IAntMediaStreamHandler) appCtx.getBean("web.handler");

		}
		return appAdapter;
	}

	public AppSettings getAppSettings() {

		if (appSettings == null) {
			if (scope.getContext().getApplicationContext().containsBean(AppSettings.BEAN_NAME)) {
				appSettings = (AppSettings) scope.getContext().getApplicationContext().getBean(AppSettings.BEAN_NAME);
			}
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

	@Override
	public void execute(ISchedulingService service) throws CloneNotSupportedException {

		if (isPipeReaderJobRunning.compareAndSet(false, true)) {
			while (true) {
				if (inputFormatContext == null) {
					break;
				}
				int ret = av_read_frame(inputFormatContext, pkt);

				if (ret >= 0) {

					writePacket(inputFormatContext.streams(pkt.stream_index()), pkt);

					av_packet_unref(pkt);
				} else {
					closeResources();
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


	public void writePacket(AVStream stream, AVPacket pkt) {

		long currentTime = System.currentTimeMillis();
		long packetTime = av_rescale_q(pkt.pts(), stream.time_base(), timeBaseForMS);

		if (firstPacketTime == -1) {
			firstPacketTime = packetTime;
			logger.info("first packet time {}", firstPacketTime);
		}

		timeDiffBetweenVideoandElapsed = (currentTime - startTime) - packetTime;
		elapsedTime = currentTime - startTime;

		double speed = 0L;
		if (elapsedTime > 0) {
			speed = (double) (packetTime - firstPacketTime) / elapsedTime;
			if (Double.isNaN(speed) && logger.isWarnEnabled()) {
				logger.warn("speed is NaN, packetTime: {}, first packetTime: {}, elapsedTime:{}", packetTime, firstPacketTime, elapsedTime);
			}
		}
		String quality = QUALITY_POOR;

		if (timeDiffBetweenVideoandElapsed < 1800) {
			quality = QUALITY_GOOD;
		} else if (timeDiffBetweenVideoandElapsed > 1799) {
			quality = QUALITY_AVERAGE;
		}

		receivedPacketCount++;
		int inputQueueSize = getInputQueueSize();
		if (receivedPacketCount % 500 == 0) {
			//	logger.info("Number of items in the queue {}", inputQueueSize);
		}

		changeStreamQualityParameters(this.streamId, quality, speed, inputQueueSize);

		if (!firstKeyFrameReceivedChecked && stream.codec().codec_type() == AVMEDIA_TYPE_VIDEO) {
			int keyFrame = pkt.flags() & AV_PKT_FLAG_KEY;
			if (keyFrame == 1) {
				firstKeyFrameReceivedChecked = true;
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

	public void writeTrailer() {
		for (Muxer muxer : muxerList) {
			muxer.writeTrailer();
		}
		//This is allocated and needs to free for every case
		av_packet_free(pkt);

		if (timeBaseForMS != null) {
			timeBaseForMS.close();
			timeBaseForMS = null;
		}
	}


	public void closeResources() {
		logger.info("close resources for streamId -> {}", streamId);

		if (packetFeederJobName != null) {
			logger.info("removing scheduled job {} ", packetFeederJobName);
			scheduler.removeScheduledJob(packetFeederJobName);
		}

		writeTrailer();

		queueReferences.remove(inputFormatContext);

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

		changeStreamQualityParameters(this.streamId, QUALITY_NA, 0, getInputQueueSize());
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

	public static byte[] getFLVHeader() {
		org.red5.io.flv.FLVHeader flvHeader = new org.red5.io.flv.FLVHeader();
		flvHeader.setFlagVideo(true);
		flvHeader.setFlagAudio(true);
		// create a buffer
		ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH + 4); // FLVHeader
		// (9 bytes)
		// +
		// PreviousTagSize0
		// (4 bytes)
		flvHeader.write(header);
		return header.array();
	}


	@Override
	public void start() {
		isRecording = false;
		logger.info("Number of items in the queue while adaptor is being started to prepare is {}", getInputQueueSize());
		scheduler.addScheduledOnceJob(0, new IScheduledJob() {

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {
				logger.info("before prepare for {}", streamId);
				try {
					if (prepare()) {
						logger.info("after prepare for {}", streamId);
						isRecording = true;
						startTime = System.currentTimeMillis();
						packetFeederJobName = scheduler.addScheduledJob(10, MuxAdaptor.this);
						logger.info("Number of items in the queue while adaptor is scheduled to process incoming packets is {}", getInputQueueSize());

						logger.info("Packet Feeder Job Name {}", packetFeederJobName);
					} else {
						logger.warn("input format context cannot be created for stream -> {}", streamId);
						if (broadcastStream != null) {
							broadcastStream.removeStreamListener(MuxAdaptor.this);
						}
						logger.warn("closing adaptor for {}", streamId);
						closeResources();
						logger.warn("closed adaptor for {}", streamId);
					}
				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
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

	@Override
	public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {

		byte[] flvFrame;
		try {
			flvFrame = getFLVFrame(packet);

			if (flvFrame.length <= BUFFER_SIZE) {
				inputQueue.add(flvFrame);
				inputContext.queueSize.incrementAndGet();
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
					inputQueue.add(data);
					inputContext.queueSize.incrementAndGet();
					numberOfBytes -= copySize;
					startIndex += copySize;
				}
			}

		} catch (IOException e) {
			logger.error(e.getMessage());
		}
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


	private Muxer addMp4Muxer() {
		Mp4Muxer mp4Muxer = createMp4Muxer();
		addMuxer(mp4Muxer);
		return mp4Muxer;
	}

	private Mp4Muxer createMp4Muxer() {
		Mp4Muxer mp4Muxer = new Mp4Muxer(storageClient, scheduler);
		mp4Muxer.setAddDateTimeToSourceName(addDateTimeToMp4FileName);
		mp4Muxer.setBitstreamFilter(mp4Filtername);
		return mp4Muxer;
	}

	public boolean startRecording() {
		Mp4Muxer muxer = createMp4Muxer();
		muxer.init(scope, streamId, 0);
		muxer.setDynamic(true);
		boolean prepared = muxer.prepare(inputFormatContext);
		if (prepared) {
			addMuxer(muxer);
		}
		else {
			logger.error("Mp4 prepare method returned false. Recording is not started for {}", streamId);
		}
		return prepared;

	}
	
	
	public Muxer findDynamicMp4Muxer() {
		synchronized (muxerList) 
		{
			Iterator<Muxer> iterator = muxerList.iterator();
			while (iterator.hasNext()) 
			{
				Muxer muxer = iterator.next();
				if (muxer instanceof Mp4Muxer && ((Mp4Muxer) muxer).isDynamic()) {
					return muxer;
				}
			}
		}
		return null;
	}

	public boolean stopRecording() 
	{
		boolean result = false;
		Muxer muxer = findDynamicMp4Muxer();
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


	public long getStreamInfoFindTime() {
		return streamInfoFindTime;
	}


	public void setStreamInfoFindTime(long streamInfoFindTime) {
		this.streamInfoFindTime = streamInfoFindTime;
	}
}


