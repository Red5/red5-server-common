package io.antmedia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import javax.management.MBeanServer;

import org.apache.tika.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.management.ManagementFactory.getThreadMXBean;
import com.sun.management.HotSpotDiagnosticMXBean;
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
 * 	System.getProperty("_____")
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
public class SystemUtils {

	public static final String HEAPDUMP_HPROF = "heapdump.hprof";

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


	private static final String HOTSPOT_BEAN_NAME =
			"com.sun.management:type=HotSpotDiagnostic";

	private static final String LINUX_MEMINFO = "/proc/meminfo";

	private static HotSpotDiagnosticMXBean hotspotMBean;

	private static String memoryFile = LINUX_MEMINFO;
	
	public static final Pattern whitespaces = Pattern.compile("\\s+");
	
	protected static final Logger logger = LoggerFactory.getLogger(SystemUtils.class);
	
	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;
	
	public static final int OS_TYPE;
	
	static {
		String osName = SystemUtils.osName.toLowerCase();
		if (osName.startsWith("mac os x") || osName.startsWith("darwin")) {
			OS_TYPE = MAC_OS_X;
		} else if (osName.startsWith("windows")) {
			OS_TYPE = WINDOWS;
		} else if (osName.startsWith("linux")) {
			OS_TYPE = LINUX;
		}
		else {
			OS_TYPE = -1;
		}
	}


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
	public static long jvmMaxMemory() {
		return Runtime.getRuntime().maxMemory();
	}

	/**
	 * Obtain JVM's Total Memory.
	 *
	 * @return bytes size
	 * 
	 */
	public static long jvmTotalMemory() {
		return Runtime.getRuntime().totalMemory();
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
	public static long jvmFreeMemory() {
		return Runtime.getRuntime().freeMemory();
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
	public static long jvmInUseMemory() {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
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
	public static long osCommittedVirtualMemory() {
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getCommittedVirtualMemorySize");
			m.setAccessible(true);
			return (Long) m.invoke(osBean);
		} catch (Exception e) {
			error(e);
			return -1L;
		}
	}

	/**
	 * Obtain Total Physical Memory from Operating System's RAM.
	 * @return bytes size
	 */
	public static long osTotalPhysicalMemory() {
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getTotalPhysicalMemorySize");
			m.setAccessible(true);
			return (Long) m.invoke(osBean);
		} catch (Exception e) {
			error(e);
			return -1L;
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
	public static long osFreePhysicalMemory() {
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getFreePhysicalMemorySize");
			m.setAccessible(true);
			return (Long) m.invoke(osBean);
		} catch (Exception e) {
			error(e);
			return -1L;
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
	public static long osInUsePhysicalMemory() {
		return osTotalPhysicalMemory() - osFreePhysicalMemory();
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
	public static long osTotalSwapSpace() {
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getTotalSwapSpaceSize");
			m.setAccessible(true);
			return (Long) m.invoke(osBean);
		} catch (Exception e) {
			error(e);
			return -1L;
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
	public static long osFreeSwapSpace() {

		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			Method m = osBean.getClass().getDeclaredMethod("getFreeSwapSpaceSize");
			m.setAccessible(true);
			return (Long) m.invoke(osBean);
		} catch (Exception e) {
			error(e);
			return -1L;
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
	public static long osInUseSwapSpace() {
		return osTotalSwapSpace() - osFreeSwapSpace();
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
	public static long osHDUsableSpace(String path) {
		if (path == null)
			path = File.listRoots()[0].getPath();
		File f = new File(path);
		if (f.getTotalSpace() == 0) {
			error(0, f.getPath());
		} else {
			return f.getUsableSpace();
		}
		return -1L;
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
	public static long osHDTotalSpace(String path) {
		if (path == null)
			path = File.listRoots()[0].getPath();
		File f = new File(path);
		if (f.getTotalSpace() == 0) {
			error(0, f.getPath());
		} else {
			return f.getTotalSpace();
		}
		return -1L;
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
	public static long osHDFreeSpace(String path) {
		if (path == null)
			path = File.listRoots()[0].getPath();
		File f = new File(path);
		if (f.getTotalSpace() == 0) {
			error(0, f.getPath());
		} else {
			return f.getFreeSpace();
		}
		return -1L;
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
	public static long osHDInUseSpace(String path) {
		if (path == null)
			path = File.listRoots()[0].getPath();
		File f = new File(path);
		if (f.getTotalSpace() == 0) {
			error(0, f.getPath());
			return -1L;
		} else {
			return f.getTotalSpace() - f.getFreeSpace();
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


	public static long convertByteSize(long bytes, String size) {
		Long num = 1024L;
		long convertB;
		size = size.toUpperCase();

		if (size.equals("PB")) {
			convertB = bytes / (num * num * num * num * num);
		} else if (size.equals("TB")) {
			convertB = bytes / (num * num * num * num);
		} else if (size.equals("GB")) {
			convertB = bytes / (num * num * num);
		} else if (size.equals("MB")) {
			convertB = bytes / (num * num);
		} else if (size.equals("KB")) {
			convertB = bytes / num ;
		} else {
			convertB = bytes;
		}

		return convertB;
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

		System.out.println(preError + e);
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
	
	public static long osLinuxAvailableMemory() {
		long memFree = 0L;
        long activeFile = 0L;
        long inactiveFile = 0L;
        long sReclaimable = 0L;

        long memTotal = 0L;
        long memAvailable;
        FileReader fr;
		try {
			fr = new FileReader(getLinuxMemoryFile());
		
	        BufferedReader br = new BufferedReader(fr);  
	        String line;  
	        while ((line = br.readLine()) != null) {
	        	String[] memorySplit = whitespaces.split(line, 2);
	        	if (memorySplit.length > 1) {
	        		  switch (memorySplit[0]) {
	                  case "MemTotal:":
	                      memTotal = parseDecimalMemorySizeToBinary(memorySplit[1]);
	                      break;
	                  case "MemAvailable:":
	                      memAvailable = parseDecimalMemorySizeToBinary(memorySplit[1]);
	                      // We're done!
	                      return memAvailable;
	                  case "MemFree:":
	                      memFree = parseDecimalMemorySizeToBinary(memorySplit[1]);
	                      break;
	                  case "Active(file):":
	                      activeFile = parseDecimalMemorySizeToBinary(memorySplit[1]);
	                      break;
	                  case "Inactive(file):":
	                      inactiveFile = parseDecimalMemorySizeToBinary(memorySplit[1]);
	                      break;
	                  case "SReclaimable:":
	                      sReclaimable = parseDecimalMemorySizeToBinary(memorySplit[1]);
	                      break;
	                  default:
	                      // do nothing with other lines
	                      break;
	                  }
	        	}
	        }
	        fr.close();
		} catch (FileNotFoundException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		} 
       
        return memFree + activeFile + inactiveFile + sReclaimable;
	}
	
	public static long parseDecimalMemorySizeToBinary(String size) {
        String[] mem = whitespaces.split(size);
        long capacity = parseLongOrDefault(mem[0], 0L);
        if (mem.length == 2 && mem[1].length() > 1) {
            switch (mem[1].charAt(0)) {
            case 'T':
                capacity <<= 40;
                break;
            case 'G':
                capacity <<= 30;
                break;
            case 'M':
                capacity <<= 20;
                break;
            case 'K':
            case 'k':
                capacity <<= 10;
                break;
            default:
                break;
            }
        }
        return capacity;
    }
	
	 public static long parseLongOrDefault(String s, long defaultLong) {
	        try {
	            return Long.parseLong(s);
	        } catch (NumberFormatException e) {
	            return defaultLong;
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
	
	public static void getHeapDump(String filepath) {
		try {
			getHotspotMBean().dumpHeap(filepath, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private static HotSpotDiagnosticMXBean getHotspotMBean() {
		try {
			synchronized (SystemUtils.class) {
				if (hotspotMBean == null) {
					MBeanServer server = ManagementFactory.getPlatformMBeanServer();
					hotspotMBean = ManagementFactory.newPlatformMXBeanProxy(server,
							HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class);
				}
			}
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
		return hotspotMBean;
	}
	
	public static String getLinuxMemoryFile() {
		return memoryFile;
	}

	public static void setLinuxMemoryFile(String filepath) {
		memoryFile = filepath;
		
	}
}
