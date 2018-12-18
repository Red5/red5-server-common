package io.antmedia.cluster;

import java.util.List;

public interface IClusterStore {
	public List<ClusterNode> getClusterNodes();
	public ClusterNode getClusterNode(String nodeId);
	public boolean addNode(ClusterNode node);
	public boolean updateNode(String nodeId, ClusterNode node);
	public boolean deleteNode(String nodeId);
	public boolean registerAsNode();
}
