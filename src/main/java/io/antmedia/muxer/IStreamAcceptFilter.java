package io.antmedia.muxer;

public interface IStreamAcceptFilter {
	
	public static final String BEAN_NAME = "streamAcceptFilter";
	
	
	/**
	 * Check below parameters in streams
	 * [0] -> Current FPS value
	 * [1] -> Current Resolution value
	 * [2] -> Current Bitrate value
	 * 
	 * @param parameter is the information of the streams 
	 * @return 
	 */
	public int checkStreamParameters(String[] parameters);
	
	
	
}
