package io.antmedia.datastore.db;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.Playlist;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.StreamInfo;
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

	/**
	 * Returns the VoD List in order
	 * 
	 * @param offset: the number of items to skip
	 * @param size: batch size
	 * @param sortBy can get "name" or "date" values
	 * @param orderBy can get "desc" or "asc"
	 * @return
	 */
	public abstract List<VoD> getVodList(int offset, int size, String sortBy, String orderBy);

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
	 */

	public abstract boolean revokeTokens (String streamId);
	
	/**
	 * Delete specific token
	 * @param tokenId id of the token
	 */

	public abstract boolean deleteToken (String tokenId);
	
	/**
	 * retrieve specific token
	 * @param tokenId id of the token
	 */

	public abstract Token getToken (String tokenId);

	/**
	 * enables or disables mp4 muxing for the stream
	 * @param streamId- id of the stream
	 * @param enabled 1 means enabled, -1 means disabled, 0 means no setting for the stream
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
	 * Updates the Broadcast objects fields if it's not null.
	 * The updated fields are as follows
	 * name, description, userName, password, IP address, streamUrl
	
	 * @param broadcast
	 * @return
	 */
	public abstract boolean updateBroadcastFields(String streamId, Broadcast broadcast);

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
	
	/**
	 * This method is called at startup
	 * It checks any hanging Broadcast and StreamInfo entry in datastore in case of unexpected restart
	 */
	public void clearStreamsOnThisServer(String hostAddress) {
		//no default implementation
	}

	/**
	 * Creates a conference room with the parameters. 
	 * The room name is key so if this is called with the same room name 
	 * then new room is overwritten to old one.
	 * @param room - conference room 
	 * @return true if successfully created, false if not
	 */
	public abstract boolean createConferenceRoom(ConferenceRoom room);
	
	/**
	 * Edits previously saved conference room
	 * @param room - conference room 
	 * @return true if successfully edited, false if not
	 */
	public abstract boolean editConferenceRoom(String roomId, ConferenceRoom room);

	/**
	 * Deletes previously saved conference room
	 * @param roomName- name of the conference room
	 * @return true if successfully deleted, false if not 
	 */
	public abstract boolean deleteConferenceRoom(String roomId);
	
	/**
	 * Retrieves previously saved conference room
	 * @param roomName- name of the conference room
	 * @return room - conference room  
	 */
	public abstract ConferenceRoom getConferenceRoom(String roomId);
	
	/**
	 * Updates the stream fields if it's not null
	 * @param broadcast
	 * @param name
	 * @param description
	 * @param userName
	 * @param password
	 * @param ipAddr
	 * @param streamUrl
	 */
	protected void updateStreamInfo(Broadcast broadcast, Broadcast newBroadcast)
	{
		if (newBroadcast.getName() != null) {
			broadcast.setName(newBroadcast.getName());
		}
		
		if (newBroadcast.getDescription() != null) {
			broadcast.setDescription(newBroadcast.getDescription());
		}
		
		if (newBroadcast.getUsername() != null) {
			broadcast.setUsername(newBroadcast.getUsername());
		}
		
		if (newBroadcast.getPassword() != null) {
			broadcast.setPassword(newBroadcast.getPassword());
		}
		
		if (newBroadcast.getIpAddr() != null) {
			broadcast.setIpAddr(newBroadcast.getIpAddr());
		}
		
		if (newBroadcast.getStreamUrl() != null) {
			broadcast.setStreamUrl(newBroadcast.getStreamUrl());
		}
		
		if (newBroadcast.getLatitude() != null) {
			broadcast.setLatitude(newBroadcast.getLatitude());
		}
		
		if (newBroadcast.getLongitude() != null) {
			broadcast.setLongitude(newBroadcast.getLongitude());
		}
		
		if (newBroadcast.getAltitude() != null) {
			broadcast.setAltitude(newBroadcast.getAltitude());
		}
		
		if (newBroadcast.getMainTrackStreamId() != null) {
			broadcast.setMainTrackStreamId(newBroadcast.getMainTrackStreamId());
		}
		
		broadcast.setReceivedBytes(newBroadcast.getReceivedBytes());
		broadcast.setDuration(newBroadcast.getDuration());
		broadcast.setBitrate(newBroadcast.getBitrate());
		broadcast.setUserAgent(newBroadcast.getUserAgent());
	}

	/**
	 * This method returns the local active broadcast count. 
	 * Mongodb implementation is different because of cluster. 
	 * Other implementations just return active broadcasts in db
	 * @return
	 */
	public long getLocalLiveBroadcastCount(String hostAddress) {
		return getActiveBroadcastCount();
	}
	
	protected List<VoD> sortAndCropVodList(List<VoD> vodList, int offset, int size, String sortBy, String orderBy) {
		if(sortBy != null && orderBy != null && !sortBy.isEmpty() && !orderBy.isEmpty()) {
			Collections.sort(vodList, new Comparator<VoD>() {
				@Override
				public int compare(VoD vod1, VoD vod2) {
					Comparable c1 = null;
					Comparable c2 = null;
					if(sortBy.contentEquals("name")) {
						c1 = vod1.getVodName().toLowerCase();
						c2 = vod2.getVodName().toLowerCase();
					}
					else if(sortBy.contentEquals("date")) {
						c1 = new Long(vod1.getCreationDate());
						c2 = new Long(vod2.getCreationDate());
					}
					
					if(orderBy.contentEquals("desc")) {
						return c2.compareTo(c1);
					}
					return c1.compareTo(c2);
				}
			});
		}
		
		if (size > MAX_ITEM_IN_ONE_LIST) {
			size = MAX_ITEM_IN_ONE_LIST;
		}
		if (offset < 0) {
			offset = 0;
		}
		
		return vodList.subList(offset, Math.min(offset+size, vodList.size()));
	}

	/**
	 * Creates new P2PConnection
	 * @param conn - P2PConnection object
	 * @return boolean - success 
	 */
	public abstract boolean createP2PConnection(P2PConnection conn);
	
	/**
	 * Get the P2PConnection by streamId
	 * @param streamId - stream id for P2PConnection
	 * @return P2PConnection - if exist else null 
	 */
	public abstract P2PConnection getP2PConnection(String streamId);
	
	/**
	 * Deletes a P2PConnection
	 * @param conn - P2PConnection object
	 * @return boolean - success 
	 */
	public abstract boolean deleteP2PConnection(String streamId);
	
	/**
	 * Add a subtrack id to a main track (broadcast)
	 * @param mainTrackId - main track id
	 * @param subTrackId - main track id
	 * @return boolean - success 
	 */
	public abstract boolean addSubTrack(String mainTrackId, String subTrackId);

	/**
	 * Creates new Playlist
	 * @param playlist - Playlist object
	 * @return boolean - success 
	 */
	public abstract boolean createPlaylist(Playlist playlist);
	
	/**
	 * Get the Playlist by playlistId
	 * @param playlistId - playlist id for Playlist
	 * @return Playlist - if exist else null 
	 */
	public abstract Playlist getPlaylist(String playlistId);
	
	/**
	 * Deletes a Playlist
	 * @param playlistId - Playlist object
	 * @return boolean - success 
	 */
	public abstract boolean deletePlaylist(String playlistId);	

	/**
	 * Edits previously saved Playlist
	 * @param playlist - Playlist 
	 * @return true if successfully edited, false if not
	 */
	public abstract boolean editPlaylist(String playlistId, Playlist playlist);
	
//**************************************
//ATTENTION: Write function descriptions while adding new functions
//**************************************	
}
