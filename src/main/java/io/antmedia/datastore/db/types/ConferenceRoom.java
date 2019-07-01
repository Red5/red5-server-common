package io.antmedia.datastore.db.types;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity("ConferenceRoom")

@Indexes({ @Index(fields = @Field("roomId")) })
@ApiModel(value="ConferenceRoom", description="The Conference Room class")
public class ConferenceRoom {
	
	
	@JsonIgnore
	@Id
	@ApiModelProperty(value = "the db id of the Conference Room")
	private ObjectId dbId;
	
	@ApiModelProperty(value = "the id of the Conference Room")
	private String roomId; 
	
	@ApiModelProperty(value = "the start date of the Conference Room")
	private long startDate;
	
	@ApiModelProperty(value = "the end date of the Conference Room")
	private long endDate;

	
	
	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public long getStartDate() {
		return startDate;
	}

	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}

	public long getEndDate() {
		return endDate;
	}

	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}

}

