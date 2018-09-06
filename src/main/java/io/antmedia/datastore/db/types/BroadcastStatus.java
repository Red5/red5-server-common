package io.antmedia.datastore.db.types;

import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class BroadcastStatus {
	
	public static final String UNPUBLISHED = "UNPUBLISHED";
	public static final String LIVE_NOW = "LIVE";
	public static final String LIVE_STOPPED = "LIVE_STOPPED";
	public static final String VOD = "VOD";
}