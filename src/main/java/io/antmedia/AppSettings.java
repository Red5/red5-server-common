package io.antmedia;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class AppSettings {
	

	private boolean mp4MuxingEnabled = true;
	
	private boolean addDateTimeToMp4FileName = false;



	public boolean isAddDateTimeToMp4FileName() {
		return addDateTimeToMp4FileName;
	}

	public void setAddDateTimeToMp4FileName(boolean addDateTimeToMp4FileName) {
		this.addDateTimeToMp4FileName = addDateTimeToMp4FileName;
	}

	public boolean isMp4MuxingEnabled() {
		return mp4MuxingEnabled;
	}

	public void setMp4MuxingEnabled(boolean mp4MuxingEnabled) {
		this.mp4MuxingEnabled = mp4MuxingEnabled;
	}

}
