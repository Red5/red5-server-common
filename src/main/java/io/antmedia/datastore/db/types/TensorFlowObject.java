package io.antmedia.datastore.db.types;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity("detection")

@Indexes({ @Index(fields = @Field("dbId")) })

public class TensorFlowObject {
	
	@JsonIgnore
	@Id
	private ObjectId dbId;
	
	/**
	 * Name of the object
	 */
	public String objectName;
	
	/**
	 * % percent of the recognition probability
	 */
	public float probability;
	
	/**
	 * Detection time
	 */
	public long detectionTime;
	
	private String imageId;
	
	public TensorFlowObject(String name, float probability, String imageId) {
		this.objectName = name;
		this.probability = probability;
		this.imageId = imageId;
	}

	public TensorFlowObject(String objectName, float probability, long detectionTime) {
		this.objectName = objectName;
		this.probability = probability;
		this.detectionTime = detectionTime;
	}
	
	public TensorFlowObject(){
		
	}

	public long getDetectionTime() {
		return detectionTime;
	}

	public void setDetectionTime(long detectionTime) {
		this.detectionTime = detectionTime;
	}



	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public float getProbability() {
		return probability;
	}

	public void setProbability(float probability) {
		this.probability = probability;
	}

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}
}