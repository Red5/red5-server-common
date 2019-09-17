package io.antmedia.cluster;

import io.antmedia.IAppSettingsUpdateListener;

public interface IClusterNotifier {
	
	public static final String BEAN_NAME = "tomcat.cluster";
	
	public IClusterStore getClusterStore();
	
	public void registerSettingUpdateListener(String appName, IAppSettingsUpdateListener listener);
		
}
