package io.antmedia.datastore.db.types;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity("token")

@Indexes({ @Index(fields = @Field("tokenId")) })
public class Token {
	
	
	@JsonIgnore
	@Id
	private ObjectId dbId;
	
	public static final String PUBLISH_TOKEN = "publish";
	public static final String PLAY_TOKEN = "play";
	
	/**
	 * random tokenID
	 */
	
	private String tokenId;
	
	/**
	 * related streamId with token
	 */
	private String streamId;
	
	/**
	 * expiration date of the token
	 */
	private long expireDate;
	
	/**
	 * type of the token, such as publish, play etc.
	 */
	private String type;
	
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTokenId() {
		return tokenId;
	}

	public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public long getExpireDate() {
		return expireDate;
	}

	public void setExpireDate(long expireDate) {
		this.expireDate = expireDate;
	}
	

}
