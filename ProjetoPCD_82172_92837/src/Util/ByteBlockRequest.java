package Util;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ByteBlockRequest implements Serializable {
	private int index;
	private int length;

	public ByteBlockRequest(int index, int length) {
		this.index = index;
		this.length = length;
	}

	public int getIndex() {
		return index;
	}

	public int getLength() {
		return length;
	}
}