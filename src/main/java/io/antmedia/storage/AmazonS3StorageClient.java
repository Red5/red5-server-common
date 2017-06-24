package io.antmedia.storage;

import java.io.File;
import java.io.IOException;

import javax.management.MXBean;

import org.red5.io.IStreamableFile;
import org.red5.io.ITagReader;
import org.red5.io.ITagWriter;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.stream.DefaultStreamFilenameGenerator;
import org.red5.server.util.ScopeUtils;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class AmazonS3StorageClient extends StorageClient {

	private AmazonS3Client amazonS3;

	private AmazonS3 getAmazonS3() {
		if (amazonS3 == null) {
			
			BasicAWSCredentials awsCredentials = new BasicAWSCredentials(getAccessKey(), getSecretKey());
			amazonS3 = new AmazonS3Client(awsCredentials);

			//AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
			//amazonS3 = AmazonS3ClientBuilder.standard().withCredentials(credentialsProvider).build();
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
					System.out.println("s3-error: upload failed. Stream: " + file.getName());
				}
				else if (event.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT){
					file.delete();
					System.out.println(file.getName() + " uploaded");
				}
			}
		});
		s3.putObject(putRequest);
	}

}
