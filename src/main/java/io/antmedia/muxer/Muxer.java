package io.antmedia.muxer;

import static org.bytedeco.javacpp.avformat.AVFMT_NOFILE;
import static org.bytedeco.javacpp.avformat.av_write_trailer;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_free_context;
import static org.bytedeco.javacpp.avformat.avio_closep;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avformat.Read_packet_Pointer_BytePointer_int;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.IConnection;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;
import org.red5.server.net.rtmp.event.CachedEvent;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.stream.DefaultStreamFilenameGenerator;
import org.red5.server.stream.IRecordingListener;
import org.red5.server.stream.consumer.FileConsumer;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


/**
 * Muxer base class, one muxer can be used by multiple encoder
 * so some functions(init, writeTrailer) may be called multiple times, correct functions with guards and sync blocks
 * @author mekya
 *
 */
public abstract class Muxer {

	protected String extension;
	protected String format;
	protected boolean isInitialized = false;
	//protected boolean isFinished = false;

	protected Map<String, String> options = new HashMap();
	protected static Logger logger = LoggerFactory.getLogger(Muxer.class);

	protected AVFormatContext outputFormatContext;

	protected File file;

	protected boolean isRecording;

	protected QuartzSchedulingService scheduler;

	protected IScope scope;

	public Muxer(QuartzSchedulingService scheduler) {
		this.scheduler = scheduler;
	}


	public static File getPreviewFile(IScope scope, String name, String extension) {
		String appScopeName = ScopeUtils.findApplication(scope).getName();
		File file = new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, "previews/"+name + extension));
		return file;
	}


	public static File getRecordFile(IScope scope, String name, String extension) {
		// get stream filename generator
		IStreamFilenameGenerator generator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope, IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);
		// generate filename
		String fileName = generator.generateFilename(scope, name, extension, GenerationType.RECORD);
		File file = null;
		if (generator.resolvesToAbsolutePath()) {
			file = new File(fileName);
		} else {
			Resource resource = scope.getContext().getResource(fileName);
			if (resource.exists()) {
				try {
					file = resource.getFile();
					logger.debug("File exists: {} writable: {}", file.exists(), file.canWrite());
				} catch (IOException ioe) {
					logger.error("File error: {}", ioe);
				}
			} else {
				String appScopeName = ScopeUtils.findApplication(scope).getName();
				file = new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, fileName));
			}
		}
		return file;
	}

	/**
	 * All in one function, it is great for transmuxing.
	 * Just call prepare and then write packets. 
	 * Use {@link #writePacket(AVPacket, AVStream)} to write packets
	 * @param inputFormatContext
	 * @return
	 */
	public abstract boolean prepare(AVFormatContext inputFormatContext);

	/**
	 * Add a new stream with this codec, codecContext and stream Index parameters.
	 * After adding streams with funcitons need to call prepareIO()
	 * @param codec
	 * @param codecContext
	 * @param streamIndex
	 * @return
	 */
	public abstract boolean addStream(AVCodec codec, AVCodecContext codecContext, int streamIndex);

	/**
	 * This function may be called by multiple encoders. 
	 * Make sure that it is called once.
	 * @return
	 */
	public abstract boolean prepareIO();

	/**
	 * This function may be called by multiple encoders. 
	 * Make sure that it is called once.
	 * @return
	 */
	public abstract void writeTrailer();

	public abstract void writePacket(AVPacket avpacket,  AVStream inStream);

	public abstract void writePacket(AVPacket pkt);


	public File getFile() {
		return file;
	}

	public String getFileName() {
		if (file != null) {
			return file.getName();
		}
		return null;
	}



	public String getFormat() {
		return format;
	}


	/**
	 * Inits the file to write. 
	 * Multiple encoders can init the muxer. 
	 * It is redundant to init multiple times.
	 */
	public void init(IScope scope, String name, int resolution) {
		if (!isInitialized) {
			isInitialized = true;
			if (resolution != 0) {
				file = getRecordFile(scope, name + "_" + resolution + "p", extension);
			}
			else {
				file = getRecordFile(scope, name, extension);
			}

			File parentFile = file.getParentFile();
			if (!parentFile.exists()) {
				parentFile.mkdir();
			}
			
		}
	}

}
