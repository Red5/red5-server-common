package io.antmedia.muxer;

import java.io.File;

public interface IMuxerListener {
	
	/**
	 * Called by some muxer like MP4Muxer
	 * 
	 * id actually is the name of the file however in some cases file name and the id may be different
	 * in some cases like there is already a file with that name
	 * 
	 * @param id is the name of the stream published 
	 * @param file video file that muxed is finished
	 * @param duration of the video in milliseconds
	 */
	public void muxingFinished(String id, File file, long duration);
	
	public void sourceQualityChanged(String id,String quality);

	public void sourceSpeedChanged(String id, String speed);
}
