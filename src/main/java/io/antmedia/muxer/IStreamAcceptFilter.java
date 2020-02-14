package io.antmedia.muxer;

public interface IStreamAcceptFilter {
	
	public static final String BEAN_NAME = "streamAcceptFilter";
	
	
	/**
	 * Check below parameters in streams
	 * Max Bitrate Check
	 * Max FPS Check
	 * Max Resolution Check
	 * 
	 * 
	 * @param parameter is the information of the streams 
	 * @return 
	 */
	public IStreamAcceptFilter checkStreamParameters(String parameters);
	
	
	
}
