package io.antmedia.datastore.db.types;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity("licence")

@Indexes({ @Index(fields = @Field("licenceId")) })
public class Licence {
	
	@JsonIgnore
	@Id
	private ObjectId dbId;
	
	private String licenceId;
	
	private String startDate;
	
	private String endDate;
	
	private String type;
	
	private String licenceCount;
	
	public Licence() {
		
		this.type = "regular";
	}

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public String getLicenceId() {
		return licenceId;
	}

	public void setLicenceId(String licenceId) {
		this.licenceId = licenceId;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLicenceCount() {
		return licenceCount;
	}

	public void setLicenceCount(String licenceCount) {
		this.licenceCount = licenceCount;
	}
	
	

}
