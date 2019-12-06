package io.antmedia.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

public class AmazonS3StorageClient extends StorageClient {

	private AmazonS3 amazonS3;
	
	protected static Logger logger = LoggerFactory.getLogger(AmazonS3StorageClient.class);

	private AmazonS3 getAmazonS3() {
		if (amazonS3 == null) {
			AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
			// Inject credentials if provided in the configuration file
			if (getAccessKey() != null) {
				BasicAWSCredentials awsCredentials = new BasicAWSCredentials(getAccessKey(), getSecretKey());
				builder = builder.withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
			}
			
			// Inject region if provided in the configuration file
			if (getRegion() != null) {
				builder = builder.withRegion(Regions.fromName(getRegion()));
			}
			builder.withClientConfiguration(new ClientConfiguration().withMaxConnections(100)
	                .withConnectionTimeout(120 * 1000)
	                .withMaxErrorRetry(15));
			
			//.withConnectionTimeout(120 * 1000)
            //.withMaxErrorRetry(15))
			amazonS3 = builder.build();
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
		
		TransferManager tm = TransferManagerBuilder.standard()
                .withS3Client(s3)
                .build();
		
		PutObjectRequest putRequest = new PutObjectRequest(getStorageName(), key, file);
		
		putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
	
		
		
		Upload upload = tm.upload(putRequest);
        // TransferManager processes all transfers asynchronously,
        // so this call returns immediately.
        //Upload upload = tm.upload(getStorageName(), key, file);
        logger.info("Mp4 {} upload has started", file.getName());
        
        upload.addProgressListener((ProgressListener) event -> {
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
		});
        

        // Optionally, wait for the upload to finish before continuing.
        try {  
			upload.waitForCompletion();
			
			logger.info("Mp4 {} upload completed", file.getName());
		} catch (AmazonServiceException e1) {
			logger.error(ExceptionUtils.getStackTrace(e1));
		} catch (InterruptedException e1) {
			logger.error(ExceptionUtils.getStackTrace(e1));
			Thread.currentThread().interrupt();
		}
        
	}

}
