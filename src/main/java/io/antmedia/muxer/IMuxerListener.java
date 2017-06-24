package io.antmedia.muxer;

import java.io.File;

public interface IMuxerListener {
	
	/**
	 * 
	 * @param file video file that muxed is finished
	 * @param duration of the video in milliseconds
	 */
	public void muxingFinished(File file, long duration);

}
