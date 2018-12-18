package io.antmedia.cluster;

import org.apache.catalina.ha.ClusterMessageBase;

public class AppSettingsMessage extends ClusterMessageBase {


	private String settings;
	private String contextName;
	
	public AppSettingsMessage(String appName, String appSettings) {
		this.setContextName(appName);
		this.settings= appSettings;
	}
	
	
	@Override
	public String getUniqueId() {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append(address.getHost());
		strBuilder.append(getTimestamp());
		return strBuilder.toString();
	}


	public String getSettings() {
		return settings;
	}


	public void setSettings(String settings) {
		this.settings = settings;
	}


	public String getContextName() {
		return contextName;
	}


	public void setContextName(String contextName) {
		this.contextName = contextName;
	}
}
