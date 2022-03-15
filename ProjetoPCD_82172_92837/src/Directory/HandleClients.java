package Directory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class HandleClients extends Thread {
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private ArrayList<Node> nodes;
	private String nodeAddress;
	private int nodePort;

	public HandleClients(Socket socket, ArrayList<Node> nodes) throws IOException {
		this.nodes = nodes;
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			doConnections();
			deal();
		} catch (IOException e) {
			System.out.println("Erro ao establecer canais de ligacao - HandleClients");
		} finally {
			try {
				socket.close();
				removeNode();
			} catch (IOException e) {
				System.out.println("Erro no fecho da socket - HandleClients");
			}
		}
	}

	void doConnections() throws IOException {
		in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
		out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream())), true);
	}

	private void deal() {
		String[] msg;
		try {
			msg = in.readLine().split(" ");
			if (msg.length < 3 || !msg[0].equals("INSC")) {
				System.err.println("Erro na inscri��o do n�: Mensagem Inv�lida.");
				return;
			}
			nodeAddress = msg[1];
			nodePort = Integer.parseInt(msg[2]);
			addNode();
			System.out.println("N� inscrito: " + nodeAddress + " " + nodePort);
		} catch (IOException e) {
			System.err.println("Erro na inscri��o do n�.");
		}
		try {
			while (true) {
				if (in.readLine().equals("nodes")) {
					System.out.println("Mensagem recebida: nodes - A enviar n�s inscritos...");
					for (Node n : nodes)
						out.println("node " + n.getNodeAddress() + " " + n.getNodePort());
					out.println("end");
				}
			}
		} catch (IOException e) {
			System.err.println("Erro nos canais de comunica��o com o n�: " + nodePort);
			removeNode();
		}
	}

	// Verificar se j� existe o n� no diret�rio
	private synchronized boolean existsNode(String nodeAddress, int nodePort) {
		for (Node n : nodes) {
			if (n.getNodeAddress().equals(nodeAddress) && n.getNodePort() == nodePort)
				return true;
		}
		return false;
	}

	// Adicionar um n� ao diret�rio
	private synchronized void addNode() {
		if (!existsNode(nodeAddress, nodePort))
			nodes.add(new Node(nodePort, nodeAddress));
	}

	// Remover um n� do diret�rio
	private synchronized void removeNode() {
		for (Node n : nodes) {
			if (n.getNodeAddress().equals(nodeAddress) && n.getNodePort() == nodePort) {
				nodes.remove(n);
				return;
			}
		}
	}
}