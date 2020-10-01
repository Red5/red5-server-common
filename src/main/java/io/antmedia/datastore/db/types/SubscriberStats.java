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

@Entity("subscriberStats")
@Indexes({ @Index(fields = @Field("subscriberId")) })
@ApiModel(value="SubscriberStats", description="Statistics for each subsciber to the stream")
public class SubscriberStats {
	@JsonIgnore
	@Id
	@ApiModelProperty(value = "the db id of the subscriber stats")
	private ObjectId dbId;
	
	/**
	 * subscriber id to which this statistic belongs to
	 */
	@ApiModelProperty(value = "the subscriber id of the subscriber")
	private String subscriberId;
	
	
	/**
	 * related streamId with subscriber
	 */
	@ApiModelProperty(value = "the stream id of the token")
	private String streamId;
	
	public String getSubscriberId() {
		return subscriberId;
	}

	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
	}
	
	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}	
	
}
