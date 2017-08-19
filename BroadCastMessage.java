import java.io.Serializable;

public class BroadCastMessage extends Message{

	private static final long serialVersionUID = 1L;
	transient int count;
	String msg;

	public BroadCastMessage(int srcid, int nodeid,String msg) {
		super(srcid,nodeid);
		this.msg = msg;
		this.count = 0;
	}

	public void incrementCount() {
		count++;
	}
	
	@Override
	public boolean equals(Object o){
		BroadCastMessage m = (BroadCastMessage) o;
		if(m.srcId == this.srcId)
			return true;
		return false;
	}
}
