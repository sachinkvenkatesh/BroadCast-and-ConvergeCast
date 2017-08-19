import java.io.Serializable;

public class Message implements Serializable {

	private static final long serialVersionUID = 1L;
	int srcId;
	int parentNodeId;
	String msg;
	
	public Message(){
		
	}
	
	public Message(int src, int parent) {
		srcId = src;
		parentNodeId = parent;
	}
}
