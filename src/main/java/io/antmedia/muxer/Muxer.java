package io.antmedia.muxer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;
import org.red5.server.stream.DefaultStreamFilenameGenerator;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import io.vertx.core.Vertx;

/**
 * PLEASE READ HERE BEFORE YOU IMPLEMENT A MUXER THAT INHERITS THIS CLASS
 * 
 *
 * One muxer can be used by multiple encoder so some functions(init,
 * writeTrailer) may be called multiple times, save functions with guards and
 * sync blocks
 * 
 * Muxer MUST NOT changed packet content somehow, data, stream index, pts, dts,
 * duration, etc. because packets are shared with other muxers. If packet
 * content changes, other muxer cannot do their job correctly.
 * 
 * Muxers generally run in multi-thread environment so that writePacket
 * functions can be called by different thread at the same time. Protect
 * writePacket with synchronized keyword
 * 
 * 
 * @author mekya
 *
 */
public abstract class Muxer {

	protected String extension;
	protected String format;
	protected boolean isInitialized = false;

	protected Map<String, String> options = new HashMap();
	private static Logger logger = LoggerFactory.getLogger(Muxer.class);

	protected AVFormatContext outputFormatContext;

	protected File file;

	protected boolean isRecording;

	protected Vertx vertx;

	protected IScope scope;

	private boolean addDateTimeToResourceName = false;

	protected AtomicBoolean isRunning = new AtomicBoolean(false);
	
	public static final String TEMP_EXTENSION = ".tmp_extension";

	/**
	 * Bitstream filter name that will be applied to packets
	 */
	protected String bsfName = null;

	public Muxer(Vertx vertx) {
		this.vertx = vertx;
	}

	public static File getPreviewFile(IScope scope, String name, String extension) {
		String appScopeName = ScopeUtils.findApplication(scope).getName();
		File file = new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName,
				"previews/" + name + extension));
		return file;
	}

	public static File getRecordFile(IScope scope, String name, String extension) {
		// get stream filename generator
		IStreamFilenameGenerator generator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope,
				IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);
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
				file = new File(
						String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, fileName));
			}
		}
		return file;
	}
	
	public static File getUserRecordFile(IScope scope, String userVoDFolder, String name) {
		String appScopeName = ScopeUtils.findApplication(scope).getName();
		File file = new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName,
				"streams/" + userVoDFolder + "/" + name ));
	
			
		return file;
	}

	/**
	 * All in one function, it is great for transmuxing. Just call prepare and
	 * then write packets.
	 * 
	 * Use {@link #writePacket(AVPacket, AVStream)} to write packets
	 * 
	 * @param inputFormatContext
	 * @return true if it succeeds, return false if it fails
	 */
	public abstract boolean prepare(AVFormatContext inputFormatContext);

	/**
	 * Add a new stream with this codec, codecContext and stream Index
	 * parameters. After adding streams, need to call prepareIO()
	 * 
	 * This method is generally called when an transcoding is required
	 * 
	 * @param codec
	 * @param codecContext
	 * @param streamIndex
	 * @return
	 */
	public abstract boolean addStream(AVCodec codec, AVCodecContext codecContext, int streamIndex);

	/**
	 * This function may be called by multiple encoders. Make sure that it is
	 * called once.
	 *
	 * See the sample implementations how it is being protected
	 *
	 * Implement this function with synchronized keyword as the subclass
	 *
	 * @return
	 */
	public abstract boolean prepareIO();

	/**
	 * This function may be called by multiple encoders. Make sure that it is
	 * called once.
	 *
	 * See the sample implementations how it is being protected
	 *
	 * Implement this function with synchronized keyword as the subclass
	 *
	 * @return
	 */
	public abstract void writeTrailer();

	/**
	 * Write packets to the output. This function is used in by MuxerAdaptor
	 * which is in community edition
	 *
	 * Check if outputContext.pb is not null for the ffmpeg base Muxers
	 *
	 * Implement this function with synchronized keyword as the subclass
	 *
	 * @param pkt
	 *            The content of the data as a AVPacket object
	 */
	public abstract void writePacket(AVPacket avpacket, AVStream inStream);
	
	
	/**
	 * Write packets to the output. This function is used in transcoding.
	 * Previously, It's the replacement of {link {@link #writePacket(AVPacket)}
	 * @param avpacket
	 * @param codecContext
	 */
	public abstract void writePacket(AVPacket avpacket, AVCodecContext codecContext);


	public void setBitstreamFilter(String bsfName) {
		this.bsfName = bsfName;
	}

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
	 * Inits the file to write. Multiple encoders can init the muxer. It is
	 * redundant to init multiple times.
	 */
	public void init(IScope scope, String name, int resolution) {
		init(scope, name, resolution, true);
	}

	/**
	 * Init file name
	 *
	 * file format is NAME[-{DATETIME}][_{RESOLUTION_HEIGHT}p].{EXTENSION}
	 *
	 * Datetime format is yyyy-MM-dd_HH-mm
	 *
	 * We are using "-" instead of ":" in HH:mm -> Stream filename must not contain ":" character.
	 * 
	 * sample naming -> stream1-yyyy-MM-dd_HH-mm_480p.mp4 if datetime is added
	 * stream1_480p.mp4 if no datetime
	 *
	 * @param scope
	 * @param name,
	 *            name of the stream
	 * @param resolution
	 *            height of the stream, if it is zero, then no resolution will
	 *            be added to resource name
	 * @param overrideIfExist
	 *            whether override if a file exists with the same name
	 */
	public void init(IScope scope, final String name, int resolution, boolean overrideIfExist) {

		if (!isInitialized) {
			isInitialized = true;
			this.scope = scope;

			// set default name
			String resourceName = name;

			// add date time parameter to resource name if it is set
			if (addDateTimeToResourceName) {

				LocalDateTime ldt =  LocalDateTime.now();

				resourceName = name + "-" + ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
				if (logger.isInfoEnabled()) {
					logger.info("Date time resource name: {} local date time: {}", resourceName, ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")));
				}
			}

			// add resolution height parameter if it is different than 0
			if (resolution != 0) {
				resourceName += "_" + resolution + "p";
			}

			file = getResourceFile(scope, resourceName, extension);

			File parentFile = file.getParentFile();

			if (!parentFile.exists()) {
				// check if parent file does not exist
				parentFile.mkdirs();
			} else {
				// if parent file exists,
				// check overrideIfExist and file.exists
				File tempFile = getResourceFile(scope, resourceName, extension+TEMP_EXTENSION);
				
				if (!overrideIfExist && (file.exists() || tempFile.exists())) {
					String tmpName = resourceName;
					int i = 1;
					do {
						tempFile = getResourceFile(scope, tmpName, extension+TEMP_EXTENSION);
						file = getResourceFile(scope, tmpName, extension);
						tmpName = resourceName + "_" + i;
						i++;
					} while (file.exists() || tempFile.exists());
				}
			}

		}
	}

	public File getResourceFile(IScope scope, String name, String extension) {
		return getRecordFile(scope, name, extension);
	}

	public boolean isAddDateTimeToSourceName() {
		return addDateTimeToResourceName;
	}

	public void setAddDateTimeToSourceName(boolean addDateTimeToSourceName) {
		this.addDateTimeToResourceName = addDateTimeToSourceName;
	}

    /**
     * Write encoded video buffer to muxer
     *
     * @param buffer
     * @param timestamp
     * @param streamIndex
     * @param isKeyFrame
     */
    public void writeVideoBuffer(ByteBuffer encodedVideoFrame, long timestamp, int frameRotation, int streamIndex,
								 boolean isKeyFrame,long firstFrameTimeStamp) {
    }
	
	/**
	 * Add video stream to the muxer with direct parameters. Not all muxers support this feature so that
	 * default implementation does nothing and returns false
	 * 
	 * @param width, video width
	 * @param height, video height
	 * @param codecId, codec id of the stream
	 * @param streamIndex, stream index
	 * @param isAVC, true if packets are in AVC format, false if in annexb format
	 * @return true if successful, 
	 * false if failed
	 */
	public boolean addVideoStream(int width, int height, AVRational videoTimebase, int codecId, int streamIndex, boolean isAVC, AVCodecParameters codecpar) {
		return false;
	}

}
