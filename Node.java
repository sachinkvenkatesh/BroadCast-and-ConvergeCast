import java.util.ArrayList;

public class Node {
	
	ArrayList<Integer> neighbors;
	int ID;
	int portNum;
	String hostName;
	Boolean visited;
	public Node(int id, String host, int port, ArrayList<Integer> n){
		ID = id;
		hostName = host;
		portNum = port;
		neighbors = n;
		visited = false;
	}
}
