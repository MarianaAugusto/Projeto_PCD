package Directory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Directory {
	private int port;
	private ArrayList<Node> nodes;

	public Directory(int port) {
		this.port = port;
		nodes = new ArrayList<Node>();
	}

	private void initialize() throws IOException {
		ServerSocket s = new ServerSocket(port);
		System.err.println(">> Diretório online <<");
		try {
			while (true) {
				Socket socket = s.accept();
				new HandleClients(socket, nodes).start();
			}
		} finally {
			s.close();
		}
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		new Directory(Integer.parseInt(args[0])).initialize();
	}
}