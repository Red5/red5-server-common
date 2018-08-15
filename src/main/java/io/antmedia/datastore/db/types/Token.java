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
