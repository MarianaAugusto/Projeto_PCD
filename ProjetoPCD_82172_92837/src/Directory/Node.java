package Directory;

public class Node {
	private int nodePort;
	private String nodeAddress;

	public Node(int nodePort, String nodeAddress) {
		this.nodePort = nodePort;
		this.nodeAddress = nodeAddress;
	}

	public int getNodePort() {
		return nodePort;
	}

	public String getNodeAddress() {
		return nodeAddress;
	}
}