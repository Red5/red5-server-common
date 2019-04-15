package io.antmedia.datastore.db;

import java.io.File;
import java.util.List;

import io.antmedia.cluster.StreamInfo;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;



public abstract class DataStore {


	//Do not forget to write function descriptions especially if you are adding new functions

	public static final int MAX_ITEM_IN_ONE_LIST = 50;
	
	private boolean writeStatsToDatastore = true;

	public abstract String save(Broadcast broadcast);

	/**
	 * Return the broadcast in data store
	 * @param id
	 * @return broadcast
	 */
	public abstract Broadcast get(String id);

	/**
	 * Return the vod by id
	 * @param id
	 * @return Vod object
	 */
	public abstract VoD getVoD(String id);

	public abstract boolean updateName(String id, String name, String description);

	public abstract boolean updateStatus(String id, String status);

	public boolean updateSourceQualityParameters(String id, String quality, double speed,  int pendingPacketQueue) {
		if(writeStatsToDatastore) {
			return updateSourceQualityParametersLocal(id, quality, speed, pendingPacketQueue);
		}
		return false;
	}

	protected abstract boolean updateSourceQualityParametersLocal(String id, String quality, double speed,  int pendingPacketQueue);

	public abstract boolean updateDuration(String id, long duration);

	public abstract boolean addEndpoint(String id, Endpoint endpoint);

	public abstract String addVod(VoD vod);

	public abstract long getBroadcastCount();

	public abstract boolean delete(String id);

	public abstract boolean deleteVod(String id);

	public abstract List<Broadcast> getBroadcastList(int offset, int size);

	public abstract List<Broadcast> filterBroadcastList(int offset, int size, String type);

	public abstract boolean removeEndpoint(String id, Endpoint endpoint);

	public abstract List<Broadcast> getExternalStreamsList();

	public abstract void close();

	public abstract List<VoD> getVodList(int offset, int size);

	public abstract boolean removeAllEndpoints(String id);

	public abstract long getTotalVodNumber();

	public abstract long getTotalBroadcastNumber();

	public abstract void saveDetection(String id,long timeElapsed,List<TensorFlowObject> detectedObjects);

	public abstract List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize);

	public abstract List<TensorFlowObject> getDetection(String id);


	/**
	 * saves token to store
	 * @param token - created token
	 * @return  true/false
	 */
	public abstract boolean saveToken (Token token);


	/**
	 * Lists all tokens of requested stream
	 * @param streamId
	 * @param offset
	 * @param size
	 * @return lists of tokens
	 */
	public abstract List<Token> listAllTokens (String streamId, int offset, int size);


	/**
	 * Validates token
	 * @param token
	 * @param streamId
	 * @return token if validated, null if not
	 */
	public abstract Token validateToken (Token token);

	/**
	 * Delete all tokens of the stream
	 * @param streamId
	 *
	 */

	public abstract boolean revokeTokens (String streamId);

	/**
	 * enables or disables mp4 muxing for the stream
	 * @param streamId- id of the stream
	 * @param enabled- 1 means enabled, -1 means disabled, 0 means no setting for the stream
	 * @return- true if set, false if not
	 */
	public abstract boolean setMp4Muxing(String streamId, int enabled);


	/**
	 * Gets the video files under the {@code fileDir} directory parameter
	 * and saves them to the datastore as USER_VOD in {@code Vod} class
	 * @param file
	 * @return number of files that are saved to datastore
	 */
	public abstract int fetchUserVodList(File filedir);

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
	public abstract SocialEndpointCredentials addSocialEndpointCredentials(SocialEndpointCredentials credentials);

	/**
	 * Get list of social endpoints
	 * 
	 * @param offset
	 * @param size
	 * 
	 * @return list of social endpoints
	 */
	public abstract List<SocialEndpointCredentials> getSocialEndpoints(int offset, int size);

	/**
	 * Remove social endpoint from data store
	 * @param id , this is the id of the credential
	 * 
	 * @return true if it is removed from datastore
	 * false if it is not removed
	 */
	public abstract boolean removeSocialEndpointCredentials(String id);

	/**
	 * Return social endpoint credential that having the id
	 * 
	 * @param id the id of the credential to be returns
	 * @return {@link SocialEndpointCredentials} if there is a matching credential with the id
	 * <code>null</code> if there is no matching id
	 */
	public abstract SocialEndpointCredentials getSocialEndpointCredentials(String id);

	/**
	 * Return the number of active broadcasts in the server
	 * @return
	 */
	public abstract long getActiveBroadcastCount();

	/**
	 * Updates the stream source
	 * @param broadcast
	 * @return
	 */
	public abstract boolean editStreamSourceInfo(Broadcast broadcast);

	/**
	 * Add or subtract the HLS viewer count from current value
	 * @param streamId
	 * @param diffCount
	 */
	public boolean updateHLSViewerCount(String streamId, int diffCount) {
		if (writeStatsToDatastore) {
			return updateHLSViewerCountLocal(streamId, diffCount);
		}
		return false;
	}

	protected abstract boolean updateHLSViewerCountLocal(String streamId, int diffCount);


	/**
	 * Returns the total number of detected objects in the stream
	 * @param id is the stream id
	 * @return total number of detected objects
	 */
	public abstract long getObjectDetectedTotal(String streamId);

	/**
	 * Update the WebRTC viewer count
	 * @param streamId
	 * @param increment if it is true, increment viewer count by one
	 * if it is false, decrement viewer count by one
	 */
	public boolean updateWebRTCViewerCount(String streamId, boolean increment) {
		if (writeStatsToDatastore) {
			return updateWebRTCViewerCountLocal(streamId, increment);
		}
		return false;
	}

	protected abstract boolean updateWebRTCViewerCountLocal(String streamId, boolean increment);


	/**
	 * Update the RTMP viewer count
	 * @param streamId
	 * @param increment if it is true, increment viewer count by one
	 * if it is false, decrement viewer count by one
	 */
	public boolean updateRtmpViewerCount(String streamId, boolean increment) {
		if (writeStatsToDatastore) {
			return updateRtmpViewerCountLocal(streamId, increment);
		}
		return false;
	}

	protected abstract boolean updateRtmpViewerCountLocal(String streamId, boolean increment);

	
	/**
	 * Saves the stream info to the db
	 * @param streamInfo
	 */
	public abstract void saveStreamInfo(StreamInfo streamInfo);

	/**
	 * Add stream info list to db
	 * @param streamInfoList
	 */
	public abstract  void addStreamInfoList(List<StreamInfo> streamInfoList);

	/**
	 * Returns stream info list added to db
	 * @param streamId
	 * @return
	 */
	public abstract  List<StreamInfo> getStreamInfoList(String streamId);

	/**
	 * Remove the stream info list from db
	 * @param streamId
	 */
	public abstract  void clearStreamInfoList(String streamId);

	public boolean isWriteStatsToDatastore() {
		return writeStatsToDatastore;
	}

	public void setWriteStatsToDatastore(boolean writeStatsToDatastore) {
		this.writeStatsToDatastore = writeStatsToDatastore;
	}
	
}
//**************************************
//ATTENTION: Write function descriptions while adding new functions
//**************************************