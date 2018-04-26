package io.antmedia.datastore.db;

import java.io.File;
import java.util.List;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.Vod;

public interface IDataStore {
	
	public static final String BEAN_NAME = "db.datastore"; 
	public static final int MAX_ITEM_IN_ONE_LIST = 50;
	
	
	String save(Broadcast broadcast);

	Broadcast get(String id);

	boolean updateName(String id, String name, String description);

	boolean updateStatus(String id, String status);
	
	boolean updateSourceQuality(String id, String quality);
	
	boolean updateSourceSpeed(String id, double speed);

	boolean updateDuration(String id, long duration);

	boolean updatePublish(String id, boolean publish);

	boolean addEndpoint(String id, Endpoint endpoint);

	boolean addVod(Vod vod);

	long getBroadcastCount();

	boolean delete(String id);

	boolean deleteVod(String id);

	List<Broadcast> getBroadcastList(int offset, int size);

	List<Broadcast> filterBroadcastList(int offset, int size, String type);

	boolean removeEndpoint(String id, Endpoint endpoint);


	boolean editCameraInfo(Broadcast camera);

	boolean deleteStream(String ipAddr);

	List<Broadcast> getExternalStreamsList();

	void close();

	List<Vod> getVodList(int offset, int size);



	boolean removeAllEndpoints(String id);

	long getTotalVodNumber();
	
	long getTotalBroadcastNumber();
	

	/**
	 * Gets the video files under the {@code fileDir} directory parameter
	 * and saves them to the datastore as USER_VOD in {@code Vod} class
	 * @param file
	 * @return number of files that are saved to datastore
	 */
	int fetchUserVodList(File filedir);

	boolean addUserVod(Vod vod);

	
	/**
	 * Add social endpoint credentials to data store
	 * Do not add id to the credentials, it will be added by data store
	 * @param credentials 
	 * The credentials that will be stored to datastore
	 * 
	 * @return SocialEndpointCredentials by settings id of the credentials
	 * null if it is not saved to datastore
	 * 
	 */
	SocialEndpointCredentials addSocialEndpointCredentials(SocialEndpointCredentials credentials);
	
	/**
	 * Get list of social endpoints
	 * 
	 * @param offset
	 * @param size
	 * 
	 * @return list of social endpoints
	 */
	List<SocialEndpointCredentials> getSocialEndpoints(int offset, int size);
	
	/**
	 * Remove social endpoint from data store
	 * @param id , this is the id of the credential
	 * 
	 * @return true if it is removed from datastore
	 * false if it is not removed
	 */
	boolean removeSocialEndpointCredentials(String id);

	/**
	 * Return social endpoint credential that having the id
	 * 
	 * @param id the id of the credential to be returns
	 * @return {@link SocialEndpointCredentials} if there is a matching credential with the id
	 * <code>null</code> if there is no matching id
	 */
	SocialEndpointCredentials getSocialEndpointCredentials(String id);


}