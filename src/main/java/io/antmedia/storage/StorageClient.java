package io.antmedia.storage;

import java.io.File;

import io.antmedia.storage.StorageClient.FileType;

public abstract class StorageClient {
	
	
	public static final String BEAN_NAME = "app.storageClient";
	
	public enum FileType {
		TYPE_PREVIEW("previews"),
		TYPE_STREAM("streams");
		
		private String value;

		private FileType(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	private String accessKey;
	private String secretKey;
	private String region;
	private String storageName;

	
	/**
	 * Delete file from storage
	 * 
	 * @param fileName
	 * @param type
	 */
	public abstract void delete(String fileName, FileType type);
	
	/**
	 * Save file to storage and delete the local file
	 * 
	 * @param file
	 * File to be saved to storage
	 * @param type
	 * type of the file
	 */
	public abstract void save(final File file, FileType type);

	/**
	 * Checks file exists on storage
	 * 
	 * @param fileName
	 * @param type
	 * @return
	 */
	public abstract boolean fileExist(String fileName, FileType type);

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getStorageName() {
		return storageName;
	}

	public void setStorageName(String storageName) {
		this.storageName = storageName;
	}

}
