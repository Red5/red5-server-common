package io.antmedia;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

/**
 * This utility is designed for accessing server's
 * system information more easier.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author DZS|All-In-One (coolest2guy@gmail.com)
 * 
 */
/*
 * 
 * 	Server-side Status for Red5
 * 	-------------------------------
 * 	System.getProperty("_____");
 * 	===============================
 * 	os.name						:Operating System Name
 * 	os.arch						: x86/x64/...
 * 	java.specification.version	: Java Version (Required 1.5 or 1.6 and higher to run Red5)
 * 	-------------------------------
 * 	Runtime.getRuntime()._____  (Java Virtual Machine Memory)
 * 	===============================
 * 	maxMemory()					: Maximum limitation
 * 	totalMemory()				: Total can be used
 * 	freeMemory()				: Availability
 * 	totalMemory()-freeMemory()	: In Use
 * 	availableProcessors()		: Total Processors available
 * 	-------------------------------
 *  getOperatingSystemMXBean()	(Actual Operating System RAM)
 *	===============================
 *  osCommittedVirtualMemory()	: Virtual Memory
 *  osTotalPhysicalMemory()		: Total Physical Memory
 *  osFreePhysicalMemory()		: Available Physical Memory
 *  osInUsePhysicalMemory()		: In Use Physical Memory
 *  osTotalSwapSpace()			: Total Swap Space
 *  osFreeSwapSpace()			: Available Swap Space
 *  osInUseSwapSpace()			: In Use Swap Space
 *  -------------------------------
 *  File						(Actual Harddrive Info: Supported for JRE 1.6)
 *	===============================
 *	osHDUsableSpace()			: Usable Space
 *	osHDTotalSpace()			: Total Space
 *	osHDFreeSpace()				: Available Space
 *	osHDInUseSpace()			: In Use Space
 *  -------------------------------
 *  
 */
public final class SystemUtils {

	//private static Logger log = Red5LoggerFactory.getLogger(SystemUtils.class, "SystemUtils");

	/**
	 * Current SystemUtils version.
	 * 
	 * @return SystemUtils version
	 */
	public static final String VERSION = "0.1.0";

	/**
	 * Obtain Operating System's name.
	 * 
	 * @return OS's name
	 */
	public static final String osName = System.getProperty("os.name");

	/**
	 * Obtain Operating System's Architecture.
	 * 
	 * @return x86 (32-bit), x64 (64-bit), ...
	 */
	public static final String osArch = System.getProperty("os.arch");

	/**
	 * Obtain Java JRE version.
	 * 
	 * @return version
	 */
	public static final String jvmVersion = System.getProperty("java.specification.version");

	/**
	 * Obtain Operating System's processor.
	 * 
	 * @return Number of Processor(s)
	 */
	public static final int osProcessorX = Runtime.getRuntime().availableProcessors();

	/**
	 * These functions below are used for Java Virtual Machine (JVM)
	 * RAM usage base on Runtime.getRuntime().______.
	 * -------------------------------
	 * jvmMaxMemory()
	 * jvmTotalMemory()
	 * jvmFreeMemory()
	 * jvmInUseMemory()
	 * -------------------------------
	 * 
	 */
	/**
	 * Obtain JVM's Maximum Memory.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String jvmMaxMemory(String size, Boolean txtByte) {
		return convertByteSize(Runtime.getRuntime().maxMemory(), size, txtByte);
	}

	/**
	 * Obtain JVM's Total Memory.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String jvmTotalMemory(String size, Boolean txtByte) {
		return convertByteSize(Runtime.getRuntime().totalMemory(), size, txtByte);
	}

	/**
	 * Obtain JVM's Free Memory.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String jvmFreeMemory(String size, Boolean txtByte) {
		return convertByteSize(Runtime.getRuntime().freeMemory(), size, txtByte);
	}

	/**
	 * Obtain JVM's In Use Memory.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String jvmInUseMemory(String size, Boolean txtByte) {
		return convertByteSize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(), size, txtByte);
	}

	/**
	 * These functions below are used for Operating System's
	 * RAM usage base on getOperatingSystemMXBean.
	 * -------------------------------
	 * osCommittedVirtualMemory()
	 * osTotalPhysicalMemory()
	 * osFreePhysicalMemory()
	 * osInUsePhysicalMemory()
	 * osTotalSwapSpace()
	 * osFreeSwapSpace()
	 * osInUseSwapSpace()
	 * -------------------------------
	 */
	/**
	 * Obtain Virtual Memory from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String osCommittedVirtualMemory(String size, Boolean txtByte) {
		if (txtByte == null)
			txtByte = true;
		if (size == null)
			size = "AUTO";
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getCommittedVirtualMemorySize");
			m.setAccessible(true);
			return convertByteSize((Long) m.invoke(osBean), size, txtByte);
		} catch (Exception e) {
			error(e);
			return null;
		}
	}

	/**
	 * Obtain Total Physical Memory from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String osTotalPhysicalMemory(String size, Boolean txtByte) {
		if (txtByte == null)
			txtByte = true;
		if (size == null)
			size = "AUTO";
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getTotalPhysicalMemorySize");
			m.setAccessible(true);
			return convertByteSize((Long) m.invoke(osBean), size, txtByte);
		} catch (Exception e) {
			error(e);
			return null;
		}

	}

	/**
	 * Obtain Free Physical Memory from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String osFreePhysicalMemory(String size, Boolean txtByte) {
		if (txtByte == null)
			txtByte = true;
		if (size == null)
			size = "AUTO";
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getFreePhysicalMemorySize");
			m.setAccessible(true);
			return convertByteSize((Long) m.invoke(osBean), size, txtByte);
		} catch (Exception e) {
			error(e);
			return null;
		}
	}

	/**
	 * Obtain In Use Physical Memory from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String osInUsePhysicalMemory(String size, Boolean txtByte) {
		if (txtByte == null)
			txtByte = true;
		if (size == null)
			size = "AUTO";
		return convertByteSize(Long.parseLong(osTotalPhysicalMemory("NONE", false)) - Long.parseLong(osFreePhysicalMemory("NONE", false)), size, txtByte);
	}

	/**
	 * Obtain Total Swap Space from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String osTotalSwapSpace(String size, Boolean txtByte) {
		if (txtByte == null)
			txtByte = true;
		if (size == null)
			size = "AUTO";
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getTotalSwapSpaceSize");
			m.setAccessible(true);
			return convertByteSize((Long) m.invoke(osBean), size, txtByte);
		} catch (Exception e) {
			error(e);
			return null;
		}
	}

	/**
	 * Obtain Free Swap Space from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String osFreeSwapSpace(String size, Boolean txtByte) {
		if (txtByte == null)
			txtByte = true;
		if (size == null)
			size = "AUTO";
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getFreeSwapSpaceSize");
			m.setAccessible(true);
			return convertByteSize((Long) m.invoke(osBean), size, txtByte);
		} catch (Exception e) {
			error(e);
			return null;
		}
	}

	/**
	 * Obtain In Use Swap Space from Operating System's RAM.
	 * 
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String osInUseSwapSpace(String size, Boolean txtByte) {
		if (txtByte == null)
			txtByte = true;
		if (size == null)
			size = "AUTO";
		return convertByteSize(Long.parseLong(osTotalSwapSpace("NONE", false)) - Long.parseLong(osFreeSwapSpace("NONE", false)), size, txtByte);
	}

	/**
	 * These functions below are used for Operating System's
	 * Harddrive usage base on File.
	 * -------------------------------
	 * osHDUseableSpace()
	 * osTotalSwapSpace()
	 * osFreeSwapSpace()
	 * osHDInUseSpace()
	 * -------------------------------
	 * 
	 */
	/**
	 * Obtain Harddrive's Usable Space.
	 * 
	 * @param path actual path
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String osHDUsableSpace(String path, String size, Boolean txtByte) {
		if (path == null)
			path = File.listRoots()[0].getPath();
		File f = new File(path);
		if (f.getTotalSpace() == 0) {
			error(0, f.getPath());
		} else {
			return convertByteSizeToDisk(f.getUsableSpace(), size, txtByte);
		}
		return null;
	}

	/**
	 * Obtain Harddrive's Overall Space.
	 * 
	 * @param path actual path
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String osHDTotalSpace(String path, String size, Boolean txtByte) {
		if (path == null)
			path = File.listRoots()[0].getPath();
		File f = new File(path);
		if (f.getTotalSpace() == 0) {
			error(0, f.getPath());
		} else {
			return convertByteSizeToDisk(f.getTotalSpace(), size, txtByte);
		}
		return null;
	}

	/**
	 * Obtain Harddrive's Available Space.
	 * 
	 * @param path actual path
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String osHDFreeSpace(String path, String size, Boolean txtByte) {
		if (path == null)
			path = File.listRoots()[0].getPath();
		File f = new File(path);
		if (f.getTotalSpace() == 0) {
			error(0, f.getPath());
		} else {
			return convertByteSizeToDisk(f.getFreeSpace(), size, txtByte);
		}
		return null;
	}

	/**
	 * Obtain Harddrive's In Use Space.
	 * 
	 * @param path actual path
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 * 
	 */
	public static String osHDInUseSpace(String path, String size, Boolean txtByte) {
		if (path == null)
			path = File.listRoots()[0].getPath();
		File f = new File(path);
		if (f.getTotalSpace() == 0) {
			error(0, f.getPath());
			return null;
		} else {
			return convertByteSizeToDisk(f.getTotalSpace() - f.getFreeSpace(), size, txtByte);
		}
	}

	/**
	 * Requirement support for...
	 * -------------------------------
	 * jvmMaxMemory()
	 * jvmTotalMemory()
	 * jvmFreeMemory()
	 * jvmInUseMemory()
	 * osCommittedVirtualMemory()
	 * osFreePhysicalMemory()
	 * osTotalPhysicalMemory()
	 * osInUsePhysicalMemory()
	 * osFreeSwapSpace()
	 * osTotalSwapSpace()
	 * osInUseSwapSpace()
	 * osHDUsableSpace()
	 * osHDTotalSpace()
	 * osHDFreeSpace()
	 * osHDInUseSpace()
	 * -------------------------------
	 */
	
	
	
	
	public static String convertByteSize(Long bytes, String size, boolean txtByte) {
		return convertByteSize( bytes,  size,  txtByte,  false);
	}
	
	public static String convertByteSizeToDisk(Long bytes, String size, boolean txtByte) {
		return convertByteSize( bytes,  size,  txtByte,  true);
	}
	/**
	 * Permit to convert bytes to ALMOST any upper bytes with/without extension
	 * (Currently at existing TeraByte but one step ahead, PetaByte)
	 * 
	 * @param bytes length of bytes
	 * @param size null, AUTO, B, KB, MB, GB, TB, or PB
	 * (PetaByte does not exist yet)
	 * Is not case sensitive.
	 * @param txtByte true if include byte extension, false exclude extension
	 * @return bytes size
	 */
	public static String convertByteSize(Long bytes, String size, Boolean txtByte, boolean isDisk) {
		String convertB = null;
		if (bytes != null) {
			if (size != null)
				size = size.toUpperCase();
			if (txtByte == null)
				txtByte = true;
			Long num = 1024L;
			if (isDisk) {
				num = 1000L;//DO NOT CHANGE THIS VALUE!
			}
			if (size == null || size.equals("AUTO")) {
				if (bytes > (num * num * num * num * num)) {
					convertB = bytes / (num * num * num * num * num) + "";
					size = "PB";
				} else if (bytes > (num * num * num * num)) {
					convertB = bytes / (num * num * num * num) + "";
					size = "TB";
				} else if (bytes > (num * num * num)) {
					convertB = bytes / (num * num * num) + "";
					size = "GB";
				} else if (bytes > (num * num)) {
					convertB = bytes / (num * num) + "";
					size = "MB";
				} else if (bytes > num) {
					convertB = bytes / num + "";
					size = "KB";
				} else {
					convertB = bytes + "";
					size = "B";
				}
			} else if (size.equals("PB")) {
				convertB = bytes / (num * num * num * num * num) + "";
			} else if (size.equals("TB")) {
				convertB = bytes / (num * num * num * num) + "";
			} else if (size.equals("GB")) {
				convertB = bytes / (num * num * num) + "";
			} else if (size.equals("MB")) {
				convertB = bytes / (num * num) + "";
			} else if (size.equals("KB")) {
				convertB = bytes / num + "";
			} else {
				convertB = bytes + "";
			}
			if (txtByte) {
				if (size.equals("PB")) {
					convertB += "PB";
				} else if (size.equals("TB")) {
					convertB += "TB";
				} else if (size.equals("GB")) {
					convertB += "GB";
				} else if (size.equals("MB")) {
					convertB += "MB";
				} else if (size.equals("KB")) {
					convertB += "KB";
				} else {
					convertB += "B";
				}
			}
		}
		return convertB;
	}

	/**
	 * Throws error switch support between IDE errors and Red5 errors
	 */
	/**
	 * Error Exception issued switch.
	 * 
	 * @param e Throws exception errors
	 */
	protected static void error(Exception e) {
		String preError = "SystemUtils: ";
		//Red5 logs
		/*/
		log.error(preError+e);
		/*/
		//Local debug logs
		System.out.println(preError + e);
		//*/
	}

	/**
	 * Custom Error issued switch.
	 * 
	 * @param error Error #
	 * @param info Extra info from error executed
	 */
	protected static void error(int error, String info) {
		String preError = "SystemUtils: ";
		//Red5 logs
		/*/
		if (error==0) {
			log.error(preError+"Harddrive: "+info+", doesn't appears to exist!");
		} else if (error==1) {
			log.error(preError+"Your current JVM Version is "+info
						+", this function required 1.6 or above!");
		} else {
			log.error(preError+"Unknown error");
		}
		/*/
		//IDE debug logs
		if (error == 0) {
			System.out.println(preError + "Harddrive: " + info + ", doesn't appears to exist!");
		} else if (error == 1) {
			System.out.println(preError + "Your current JVM Version is " + info + ", this function required 1.6 or above!");
		} else {
			System.out.println(preError + "Unknown error #" + error);
		}
		//*/
	}
	
	
	
	/**
	 * Returns the "% recent cpu usage" for the whole system. 
	 * @return
	 */
	public static Integer getSystemCpuLoad() {
		
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getSystemCpuLoad");
			m.setAccessible(true);
			return (int)(((Double)m.invoke(osBean))*100);
		} catch (Exception e) {
			error(e);
			return -1;
		}
	}
	
	/**
	 * Returns the "% recent cpu usage" for the Java Virtual Machine process. 
	 *  the method returns a negative value.
	 * @return
	 */
	public static Integer getProcessCpuLoad() {
		
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getProcessCpuLoad");
			m.setAccessible(true);
			return (int)(((Double) m.invoke(osBean)) * 100);
		} catch (Exception e) {
			error(e);
			return -1;
		}
	}
	
	/**
	 * Returns the CPU time used by the process on which the Java virtual machine 
	 * is running in microseconds.
	 * @return
	 */
	public static Long getProcessCpuTime() {
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getProcessCpuTime");
			m.setAccessible(true);
			return (Long) m.invoke(osBean)/1000;
		} catch (Exception e) {
			error(e);
			return -1l;
		}
	}
}
