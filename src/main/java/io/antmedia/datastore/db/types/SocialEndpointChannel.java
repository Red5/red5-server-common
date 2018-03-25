package io.antmedia.datastore.db.types;


/**
 * This class is for specifying different channel options in social media. 
 * It is useful for facebook so far. Type can be user, page, event, group
 * 
 * @author mekya
 *
 */
public class SocialEndpointChannel {
	
	public SocialEndpointChannel(String accountId, String accountName, String type) {
		this.id = accountId;
		this.name = accountName;
		this.type = type;
	}

	public SocialEndpointChannel() {
	}

	public String type = null;
	
	public String name = null;
	
	public String id = null;
	
}
