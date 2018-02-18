package io.antmedia.datastore.db;

import java.util.List;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;

public interface IDataStore {

	String save(Broadcast broadcast);

	Broadcast get(String id);

	boolean updateName(String id, String name, String description);

	boolean updateStatus(String id, String status);

	boolean updateDuration(String id, long duration);

	boolean updatePublish(String id, boolean publish);

	boolean addEndpoint(String id, Endpoint endpoint);

	long getBroadcastCount();

	boolean delete(String id);

	List<Broadcast> getBroadcastList(int offset, int size);

	boolean removeEndpoint(String id, Endpoint endpoint);
	
	boolean removeAllEndpoints(String id);
	
}