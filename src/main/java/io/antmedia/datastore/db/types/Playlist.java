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

	@ApiModelProperty(value = "the name of the Playlist")
	private String playlistName;
	
	@ApiModelProperty(value = "the list of the selected VoD lists")
	private String playlistVod;
	
	@ApiModelProperty(value = "the list of VoDs")
	@Embedded
	private List<VoD> playlistVodList;

	@ApiModelProperty(value = "the creation of the Playlist")
	private long creationDate;
	
	@ApiModelProperty(value = "the duration of the Playlist")
	private long duration;
	
	public Playlist() {
		//default constructor is used to return not found playlist in rest service 
	}
	
	public Playlist(String playlistId, String playlistName, long creationDate, long duration, List<VoD> playlistVodList) {

		this.playlistId = playlistId;
		this.playlistName = playlistName;
		this.creationDate = creationDate;
		this.duration = duration;
		this.playlistVodList = playlistVodList;
	}
	
	public String getPlaylistId() {
		return playlistId;
	}

	public void setPlaylistId(String playlistId) {
		this.playlistId = playlistId;
	}

	public String getPlaylistName() {
		return playlistName;
	}

	public void setPlaylistName(String playlistName) {
		this.playlistName = playlistName;
	}
	
	public List<VoD> getPlaylistVodList() {
		return playlistVodList;
	}

	public void setPlaylistVodList(List<VoD> playlistVodList) {
		this.playlistVodList = playlistVodList;
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
