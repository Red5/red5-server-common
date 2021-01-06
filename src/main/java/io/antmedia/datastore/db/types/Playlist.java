package io.antmedia.datastore.db.types;

import java.util.List;

import org.bson.types.ObjectId;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;	

@Entity("playlist")	
@Indexes({ @Index(fields = @Field("playlistId")) })	
@ApiModel(value="Playlist", description="The playlist in video list object class")	
public class Playlist {	

	@JsonIgnore
	@Id
	@ApiModelProperty(value = "the db id of the Playlist")	
	private ObjectId dbId;	

	@ApiModelProperty(value = "the object id of the Playlist")
	private String playlistId;	

	@ApiModelProperty(value = "the current play index of the Playlist")	
	private int currentPlayIndex;	

	@ApiModelProperty(value = "the name of the Playlist")	
	private String playlistName;	

	/**	
	 * "finished", "broadcasting", "created"	
	 */	

	@ApiModelProperty(value = "the status of the playlist", allowableValues = "finished, broadcasting, created")	
	private String playlistStatus;	

	@ApiModelProperty(value = "the list broadcasts of Playlist Items")
	@Reference
	private List<Broadcast> broadcastItemList;

	@ApiModelProperty(value = "the creation of the Playlist")	
	private long creationDate;	

	@ApiModelProperty(value = "the duration of the Playlist")	
	private long duration;	

	public Playlist() {	
		//default constructor is used to return not found playlist in rest service 	
	}	

	public Playlist(String playlistId, int currentPlayIndex, String playlistName, String playlistStatus, long creationDate, long duration, List<Broadcast> broadcastItemList) {	

		this.playlistId = playlistId;	
		this.currentPlayIndex = currentPlayIndex;	
		this.playlistName = playlistName;	
		this.playlistStatus = playlistStatus;	
		this.creationDate = creationDate;	
		this.duration = duration;	
		this.broadcastItemList = broadcastItemList;	

	}	

	public String getPlaylistId() {	
		return playlistId;	
	}	

	public void setPlaylistId(String playlistId) {	
		this.playlistId = playlistId;	
	}	

	public int getCurrentPlayIndex() {	
		return currentPlayIndex;	
	}	

	public void setCurrentPlayIndex(int currentPlayIndex) {	
		this.currentPlayIndex = currentPlayIndex;	
	}	

	public String getPlaylistName() {	
		return playlistName;	
	}	

	public void setPlaylistName(String playlistName) {	
		this.playlistName = playlistName;	
	}	

	public String getPlaylistStatus() {	
		return playlistStatus;	
	}	

	public void setPlaylistStatus(String playlistStatus) {	
		this.playlistStatus = playlistStatus;	
	}	

	public List<Broadcast> getBroadcastItemList() {	
		return broadcastItemList;	
	}	

	public void setBroadcastItemList(List<Broadcast> broadcastItemList) {	
		this.broadcastItemList = broadcastItemList;	
	}	

	public long getCreationDate() {	
		return creationDate;	
	}	

	public void setCreationDate(long creationDate) {	
		this.creationDate = creationDate;	
	}	

	public long getDuration() {	
		return duration;	
	}	

	public void setDuration(long duration) {	
		this.duration = duration;	
	}	

}