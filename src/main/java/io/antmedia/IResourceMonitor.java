package io.antmedia;

public interface IResourceMonitor {
	public static final String BEAN_NAME = "resourceMonitor";

	public int getAvgCpuUsage();

	public int getCpuLimit();
	
	public boolean checkSystemResources();
}
