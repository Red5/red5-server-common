package com.antstreaming.muxer;

import static org.bytedeco.javacpp.avformat.AVFMT_NOFILE;
import static org.bytedeco.javacpp.avformat.av_write_trailer;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_free_context;
import static org.bytedeco.javacpp.avformat.avio_closep;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
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


public abstract class AbstractMuxer {

	protected String extension;
	protected String format;

	protected Map<String, String> options = new HashMap();
	protected static Logger logger = LoggerFactory.getLogger(AbstractMuxer.class);

	protected AVFormatContext outputFormatContext;

	protected File file;

	protected boolean isRecording;
	


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


	public abstract boolean prepare(AVFormatContext inputFormatContext);

	public abstract void writeTrailer();
	
	public abstract void writePacket(AVPacket avpacket,  AVStream inStream);


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


	public void init(IScope scope, String name) {
		file = getRecordFile(scope, name, extension);
	}

}
