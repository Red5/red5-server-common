package io.antmedia.cluster;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

@Entity("clusternode")

@Indexes({ @Index(fields = @Field("id"))})

public class ClusterNode {
	
	public static final String ALIVE = "alive";
	public static final String DEAD = "dead";
	public static final long NODE_UPDATE_PERIOD = 30000;
	
	@Id
	private String id;
	private String ip;
	private String status;
	private long lastUpdateTime;
	private boolean inTheCluster;
	
	public ClusterNode() {
	}
	
	public ClusterNode(String ip) {
		super();
		this.ip = ip;
		this.id = ip.replace(".", "_");
		this.lastUpdateTime= System.currentTimeMillis();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getStatus() {
		return status;
	}
	
	public String updateStatus() {
		if(System.currentTimeMillis() - lastUpdateTime > NODE_UPDATE_PERIOD*2) {
			status = ClusterNode.DEAD;
		}
		else {
			status = ClusterNode.ALIVE;
		}
		return status;
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public boolean isInTheCluster() {
		return inTheCluster;
	}

	public void setInTheCluster(boolean inTheCluster) {
		this.inTheCluster = inTheCluster;
	}
}
