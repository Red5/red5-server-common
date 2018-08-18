package io.antmedia.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class AmazonS3StorageClient extends StorageClient {

	private AmazonS3Client amazonS3;
	
	protected static Logger logger = LoggerFactory.getLogger(AmazonS3StorageClient.class);

	private AmazonS3 getAmazonS3() {
		if (amazonS3 == null) {
			
			BasicAWSCredentials awsCredentials = new BasicAWSCredentials(getAccessKey(), getSecretKey());
			amazonS3 = new AmazonS3Client(awsCredentials);

			amazonS3.setRegion(Region.getRegion(Regions.fromName(getRegion())));
			
			
		}
		return amazonS3; 
	}

	
	public void delete(String fileName, FileType type) {
		AmazonS3 s3 = getAmazonS3();
		s3.deleteObject(getStorageName(), type.getValue() + "/" + fileName);
		
	}
	
	public boolean fileExist(String fileName, FileType type) {
		AmazonS3 s3 = getAmazonS3();
		return s3.doesObjectExist(getStorageName(), type.getValue() + "/" + fileName); 
	}
	
	public void save(final File file, FileType type) 
	{
		
		String key = type.getValue() + "/" + file.getName();

		AmazonS3 s3 = getAmazonS3();
		PutObjectRequest putRequest = new PutObjectRequest(getStorageName(), key, file);
		
		putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
		putRequest.setGeneralProgressListener(new ProgressListener() {

			@Override
			public void progressChanged(ProgressEvent event) {
				if (event.getEventType() == ProgressEventType.TRANSFER_FAILED_EVENT){
					logger.error("S3 - Error: Upload failed for {}", file.getName());
				}
				else if (event.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT){
					try {
						Files.delete(file.toPath());
					} catch (IOException e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
					logger.info("File {} uploaded to S3", file.getName());
				}
			}
		});
		s3.putObject(putRequest);
	}

}
