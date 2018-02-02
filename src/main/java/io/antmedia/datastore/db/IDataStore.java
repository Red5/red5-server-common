package io.antmedia.datastore.db;

import java.util.List;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.Vod;

public interface IDataStore {

	String save(Broadcast broadcast);

	Broadcast get(String id);

	boolean updateName(String id, String name, String description);

	boolean updateStatus(String id, String status);

	boolean updateDuration(String id, long duration);

	boolean updatePublish(String id, boolean publish);

	boolean addEndpoint(String id, Endpoint endpoint);

	boolean addVod(String id, Vod vod);

	long getBroadcastCount();

	boolean delete(String id);

	boolean deleteVod(String id);

	List<Broadcast> getBroadcastList(int offset, int size);

	List<Broadcast> filterBroadcastList(int offset, int size, String type);

	boolean removeEndpoint(String id, Endpoint endpoint);

	boolean addCamera(Broadcast camera);

	boolean editCameraInfo(String name, String ipAddr, String username, String password, String rtspUrl);

	boolean deleteCamera(String ipAddr);

	Broadcast getCamera(String ip);

	List<Broadcast> getCameraList();

	void close();

	List<Vod> getVodList(int offset, int size);

	List<Vod> filterVoDList(int offset, int size, String keyword, long startdate, long endDate);
}