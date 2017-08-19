import java.io.Serializable;

public class ReplyMessage extends Message implements Serializable {

	private static final long serialVersionUID = 1L;

	public ReplyMessage(int src, int parent) {
		super(src, parent);
	}
}
