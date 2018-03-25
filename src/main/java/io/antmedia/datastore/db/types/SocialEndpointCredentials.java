package io.antmedia.datastore.db.types;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * POJO to store security credentils for social endpoints.
 * @author mekya
 *
 */
@Entity("SocialEndpointCredentials")

@Indexes({ @Index(fields = @Field("id"))})

public class SocialEndpointCredentials {
	
	/**
	 * Access token to make the service calls
	 */
	@JsonIgnore
	private String accessToken;
	
	/**
	 * Refresh token to refresh the access token
	 */
	@JsonIgnore
	private String refreshToken;
	
	/**
	 * Token type
	 */
	@JsonIgnore
	private String tokenType;
	
	/**
	 * Expire time in seconds
	 */
	@JsonIgnore
	private String expireTimeInSeconds;
	
	/**
	 * Authentication time in milli seconds
	 */
	@JsonIgnore
	private String authTimeInMilliseconds;
	
	/**
	 * Id of the record that is stored in db
	 */
	@Id
	private String id;
	
	/**
	 * Account or Page name
	 */
	private String accountName;
	
	/**
	 * Id of the account if exists
	 */
	@JsonIgnore
	private String accountId;
	
	/**
	 * Name of the service like facebook, youtube, periscope, twitch
	 */
	private String serviceName;
	
	/**
	 * User account, page account, etc.
	 */
	private String accountType;
	
	public SocialEndpointCredentials(String name, String serviceName, String authTimeInMillisecoonds, String expireTimeInSeconds, String tokenType, String accessToken, String refreshToken) {
		this.accountName = name;
		this.serviceName = serviceName;
		this.authTimeInMilliseconds = authTimeInMillisecoonds;
		this.expireTimeInSeconds = expireTimeInSeconds;
		this.tokenType = tokenType;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}
	
	public SocialEndpointCredentials() {
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public String getExpireTimeInSeconds() {
		return expireTimeInSeconds;
	}

	public void setExpireTimeInSeconds(String expireTimeInSeconds) {
		this.expireTimeInSeconds = expireTimeInSeconds;
	}

	public String getAuthTimeInMilliseconds() {
		return authTimeInMilliseconds;
	}

	public void setAuthTimeInMilliseconds(String authTimeInMillisecoonds) {
		this.authTimeInMilliseconds = authTimeInMillisecoonds;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String name) {
		this.accountName = name;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

}
