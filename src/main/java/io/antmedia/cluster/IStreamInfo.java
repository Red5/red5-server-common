package io.antmedia.cluster;

public interface IStreamInfo {

	/**
	 * Returns the height of the video
	 * @return
	 */
	public int getHeight();

	/**
	 * Returns the width of the video
	 * @return
	 */
	public int getWidth();


	/**
	 * Returns the video bitrate 
	 * @return bps
	 */
	public int getVideoBitrate();


	/**
	 * Returns the audio bitrate
	 * @return
	 */
	public int getAudioBitrate();

}
