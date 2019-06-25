package io.antmedia;

public interface IResourceMonitor {
	public static final String BEAN_NAME = "resourceMonitor";

	/**
	 * It calculates the average CPU usage for a specified time.
	 * @return the current CPU usage
	 */
	public int getCpuUsage();

	/** 
	 * It's configurable and it's based on percentage. 
	 * Max value is 100.
	 * @return the CPU limit that server does not exceed.
	 */
	public int getCpuLimit();
	
	/**
	 * It's configurable
	 * In MB
	 * @return the free RAM size that server should have all the time
	 */
	public int getRamLimit();
	
	/**
	 * In MB
	 * @return the free RAM that server can use
	 */
	public int getFreeRam();
	
	/**
	 * Check if cpu usage and ram usage does not exceed the limit
	 * @return true if not exceeding the limit, false if exceeding limit
	 */
	public boolean enoughResource();
}
