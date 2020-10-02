package io.antmedia.datastore.db.types;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity("subscriber")
@Indexes({ @Index(fields = @Field("subscriberId")), @Index(fields = @Field("streamId")) })
@ApiModel(value="Subscriber", description="The time based token subscriber class")
public class Subscriber {


	@JsonIgnore
	@Id
	@ApiModelProperty(value = "the db id of the subscriber")
	private ObjectId dbId;
	
	/**
	 * random subscriber id
	 */
	@ApiModelProperty(value = "the subscriber id of the subscriber")
	private String subscriberId;
	
	/**
	 * related streamId with subscriber
	 */
	@ApiModelProperty(value = "the stream id of the token")
	private String streamId;	
	
	/**
	 * statistics for this subscriber
	 */
	@ApiModelProperty(value = "stats for this subscriber")
	@Embedded
	private SubscriberStats stats;
	
	/**
	 * secret code of the Subscriber
	 */
	@ApiModelProperty(value = "secret code of the subscriber")
	private String b32Secret;
	
	/**
	 * is subscriber connected
	 */
	@ApiModelProperty(value = "is subscriber connected")
	private boolean connected;
	
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

	public String getB32Secret() {
		return b32Secret;
	}

	public void setB32Secret(String b32Secret) {
		this.b32Secret = b32Secret;
	}

	public SubscriberStats getStats() {
		return stats;
	}

	public void setStats(SubscriberStats stats) {
		if(stats != null)  {
			stats.setStreamId(streamId);
			stats.setSubscriberId(subscriberId);
		}
		this.stats = stats;
	}
	
	// database key of a subscriber consists both the stream id and subscriber id
	public String getSubscriberKey() {
		return getDBKey(streamId, subscriberId);
		
	}
	
	public static String getDBKey(String streamId, String subscriberId) {
		return streamId + "-" +subscriberId;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}
	
}
