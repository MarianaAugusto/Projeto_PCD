package Util;

import java.util.ArrayList;

public class SynchronizedList {
	private ArrayList<ByteBlockRequest> list = new ArrayList<ByteBlockRequest>();

	public SynchronizedList() {
	}

	public void add(ByteBlockRequest request) {
		list.add(request);
	}

	public synchronized ByteBlockRequest remove() {
		return list.remove(list.size() - 1);
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}
}