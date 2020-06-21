package io.antmedia.muxer;

import java.io.File;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;

public interface IAntMediaStreamHandler {
	
	public static final String VERTX_BEAN_NAME = "vertxCore";
	
	/**
	 * Called by some muxer like MP4Muxer
	 * 
	 * id actually is the name of the file however in some cases file name and the id may be different
	 * in some cases like there is already a file with that name
	 * 
	 * @param id is the name of the stream published 
	 * @param file video file that muxed is finished
	 * @param duration of the video in milliseconds
	 * @param resolution height of the video 
	 */
	public void muxingFinished(String id, File file, long duration , int resolution);
	
	
	/**
	 * Update stream quality, speed and number of pending packet size 
	 * in datastore
	 * 
	 * @param id this is the id of the stream
	 * 
	 * @param quality, quality of the stream values can be 
	 * {@link MuxAdaptor#QUALITY_GOOD, MuxAdaptor#QUALITY_AVERAGE, MuxAdaptor#QUALITY_POOR, MuxAdaptor#QUALITY_NA}
	 * 
	 * @param speed
	 * Speed of the stream. It should 1x
	 * 
	 * @param pendingPacketSize
	 * Number of packets pending to be processed
	 */
	public void setQualityParameters(String id, String quality, double speed, int pendingPacketSize);

    /***
     * Adds a MuxAdaptor when a muxAdaptor is created
     *
     * @param muxAdaptor
     */
    public void muxAdaptorAdded(MuxAdaptor muxAdaptor);

    /***
     * Removes a MuxAdaptor when a muxAdaptor is closingResources
     *
     * @param muxAdaptor
     */
    public void muxAdaptorRemoved(MuxAdaptor muxAdaptor);

    /***
     * Checks a Stream parameters is valid.
     *
     * @param inputFormatContext, pkt, streamId
     */
	public boolean isValidStreamParameters(AVFormatContext inputFormatContext, AVPacket pkt);
}
