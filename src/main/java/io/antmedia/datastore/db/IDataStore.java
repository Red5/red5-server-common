package io.antmedia.datastore.db;

import java.io.File;
import java.util.List;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.VoD;



public interface IDataStore {
	

	//Do not forget to write function descriptions especially if you are adding new functions
 
	public static final int MAX_ITEM_IN_ONE_LIST = 50;
	
	

	/**
	 * This is the bean name that implements IDataStore
	 */
	public static final String BEAN_NAME = "db.datastore";


	String save(Broadcast broadcast);

	/**
	 * Return the broadcast in data store
	 * @param id
	 * @return broadcast
	 */
	Broadcast get(String id);
	
	/**
	 * Return the vod by id
	 * @param id
	 * @return Vod object
	 */
	VoD getVoD(String id);

	boolean updateName(String id, String name, String description);

	boolean updateStatus(String id, String status);
	
	boolean updateSourceQualityParameters(String id, String quality, double speed,  int pendingPacketQueue);
	
	boolean updateDuration(String id, long duration);

	boolean addEndpoint(String id, Endpoint endpoint);

	String addVod(VoD vod);

	long getBroadcastCount();

	boolean delete(String id);

	boolean deleteVod(String id);

	List<Broadcast> getBroadcastList(int offset, int size);

	List<Broadcast> filterBroadcastList(int offset, int size, String type);

	boolean removeEndpoint(String id, Endpoint endpoint);

	List<Broadcast> getExternalStreamsList();

	void close();

	List<VoD> getVodList(int offset, int size);

	boolean removeAllEndpoints(String id);

	long getTotalVodNumber();
	
	long getTotalBroadcastNumber();

	void saveDetection(String id,long timeElapsed,List<TensorFlowObject> detectedObjects);
	
	List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize);
	
	List<TensorFlowObject> getDetection(String id);
	

	/**
	 * Creates token for stream
	 * @param streamId
	 * @param expireDate
	 * @param type
	 * @return  token
	 */
	
	Token createToken (String streamId, long expireDate, String type);
	

	/**
	 * Lists all tokens of requested stream
	 * @param streamId
	 * @param offset
	 * @param size
	 * @return lists of tokens
	 */
	List<Token> listAllTokens (String streamId, int offset, int size);
	
	
	/**
	 * Validates token
	 * @param token
	 * @param streamId
	 * @return token if validated, null if not
	 */
	Token validateToken (Token token);
	
	/**
	 * Delete all tokens of the stream
	 * @param streamId
	 *
	 */
	
	boolean revokeTokens (String streamId);


	/**
	 * Gets the video files under the {@code fileDir} directory parameter
	 * and saves them to the datastore as USER_VOD in {@code Vod} class
	 * @param file
	 * @return number of files that are saved to datastore
	 */
	int fetchUserVodList(File filedir);

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
	
	/**
	 * Return the number of active broadcasts in the server
	 * @return
	 */
	long getActiveBroadcastCount();

	/**
	 * Updates the stream source
	 * @param broadcast
	 * @return
	 */
	boolean editStreamSourceInfo(Broadcast broadcast);

	/**
	 * Add or subtract the HLS viewer count from current value
	 * @param streamId
	 * @param diffCount
	 */
	boolean updateHLSViewerCount(String streamId, int diffCount);
	
	
	/**
	 * Returns the total number of detected objects in the stream
	 * @param id is the stream id
	 * @return total number of detected objects
	 */
	long getObjectDetectedTotal(String streamId);
	
	/**
	 * Update the WebRTC viewer count
	 * @param streamId
	 * @param increment if it is true, increment viewer count by one
	 * if it is false, decrement viewer count by one
	 */
	boolean updateWebRTCViewerCount(String streamId, boolean increment);
	
	/**
	 * Update the RTMP viewer count
	 * @param streamId
	 * @param increment if it is true, increment viewer count by one
	 * if it is false, decrement viewer count by one
	 */
	boolean updateRtmpViewerCount(String streamId, boolean increment);


}