package Node;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;

import Util.ByteBlockRequest;
import Util.CloudByte;
import Util.SynchronizedList;

public class StorageNode {
	private Socket socket;
	private String dirIp;
	private int localPort;
	private int dirPort;
	private BufferedReader in;
	private PrintWriter out;
	private File file;
	private CloudByte[] data;
	private SynchronizedList requestList = new SynchronizedList();
	private ArrayList<CloudByte> correctData = new ArrayList<CloudByte>();
	private CountDownLatch cdl;
	private int countErrorNodes;
	private ServerSocket serverSocket;

	public StorageNode(String dirIP, int dirPort, int localPort, File file) throws IOException, InterruptedException {
		this.dirIp = dirIP;
		this.dirPort = dirPort;
		this.localPort = localPort;
		data = new CloudByte[1000000];
		serverSocket = new ServerSocket(localPort);

		connectToServer();
		registerNode();

		if (file != null) {
			this.file = file;
			writeNodeFromFile();
		} else
			writeFromNodes();
	}

	// Iniciar a socket para a ligação com o diretório
	private void connectToServer() throws IOException {
		socket = new Socket(dirIp, dirPort);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
	}

	// Registar o nó no diretório
	private void registerNode() throws IOException {
		String address = socket.getInetAddress().getHostAddress();
		out.println("INSC " + address + " " + localPort);
		System.out.println("INSC " + address + " " + localPort);
	}

	// Inicializar thread para inserir erros
	private void injectErrors() {
		new ErrorInjector(data).start();
	}

	// Inicializar threads para procura de erros
	private void startSearcherThreads() {
		new SearcherThread().start();
		new SearcherThread().start();
	}

	// Escrever data no nó a partir de um ficheiro existente
	private void writeNodeFromFile() throws IOException {
		byte[] fileContent = Files.readAllBytes(file.toPath());
		for (int i = 0; i < fileContent.length; i++)
			data[i] = new CloudByte(fileContent[i]);
		System.out.println("Download dos dados do ficheiro completo!");
		acceptConnections();
	}

	// Escrever data no nó a partir da data contida noutros nós
	private void writeFromNodes() throws IOException, InterruptedException {

		int countNodes = 0;

		for (int i = 0; i != 10000; i += 1) {
			requestList.add(new ByteBlockRequest(i * 100, 100));
		}

		out.println("nodes");
		System.out.println("A obter nós do diretório...");

		String line = in.readLine();

		while (!line.equals("end")) {
			String[] splitLine = line.split(" ");
			if (Integer.parseInt(splitLine[2]) != this.localPort) {
				countNodes++;
				new DownloadFromNode(splitLine[1], Integer.parseInt(splitLine[2])).start();
			}
			line = in.readLine();
		}

		if (countNodes == 0) {
			System.err.println("ERRO: Não há nós com informação.");
			return;
		}

		cdl = new CountDownLatch(countNodes);
		downloadComplete();
	}

	// Iniciar espera ativa de conexões, sinalizada pelo CDL
	private void downloadComplete() throws InterruptedException, IOException {
		cdl.await();
		acceptConnections();
	}

	// Inicializar uma espera ativa para conectar a outros Nodes
	private void acceptConnections() throws IOException {
		System.out.println("A aceitar novas conexões... :)");
		injectErrors();
		startSearcherThreads();
		try {
			while (true) {
				Socket acceptSocket = serverSocket.accept();
				new DealWithClient(acceptSocket).start();
			}
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				System.out.println("Erro ao fechar a socket - SN acceptConnections");
				;
			}
		}
	}

	// Tratar da correção de erros, iniciando threads para cada nó
	public void correctError(ByteBlockRequest error) {
		CountDownLatch cdlError = new CountDownLatch(2);
		countErrorNodes = 0;

		try {
			System.out.println("A obter nós para a correção do Byte corrupto...");
			out.println("nodes");

			String line = in.readLine();
			while (!line.equals("end")) {
				String[] splitLine = line.split(" ");
				if (Integer.parseInt(splitLine[2]) != localPort) {
					new DownloadError(splitLine[1], Integer.parseInt(splitLine[2]), error, cdlError).start();
					countErrorNodes++;
				}
				line = in.readLine();
			}

			if (countErrorNodes < 2) {
				System.err.println("ERRO: Não há nós suficientes com informação para ser possível corrigir o erro.");
				return;
			}

			cdlError.await();
			while (!correctData.isEmpty()) {
				CloudByte c = correctData.remove(0);
				for (CloudByte c1 : correctData) {
					if (c.equals(c1) && c.isParityOk()) {
						System.err.println("Novo valor na posição: " + error.getIndex() + " = " + c);
						data[error.getIndex()] = c;
						correctData.clear();
						return;
					}
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/////////////////////////// DOWNLOAD FROM NODE ///////////////////////////

	// Thread usada para transferir dados a partir de um nó
	private class DownloadFromNode extends Thread {
		private Socket socket;
		private ObjectInputStream in;
		private ObjectOutputStream out;
		private int nodePort;
		private String nodeAdress;
		private int count;
		private long initTime;

		private DownloadFromNode(String nodeAdress, int nodePort) throws UnknownHostException, IOException {
			this.nodePort = nodePort;
			this.nodeAdress = nodeAdress;
			this.count = 0;
			initTime = System.currentTimeMillis();
		}

		@Override
		public void run() {
			System.out.println("A obter dados do nó: " + nodePort);
			try {
				doConnections();
				sendMessages();
			} catch (IOException | ClassNotFoundException | InterruptedException e) {
				e.printStackTrace();
			} finally {
				try {
					out.writeObject(null);
					socket.close();
				} catch (IOException e) {
					System.err.println("Erro fecho da socket - DownloadFromNode");
				}
			}
		}

		private void doConnections() throws UnknownHostException, IOException {
			socket = new Socket(nodeAdress, nodePort);
			in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());
		}

		private void sendMessages() throws InterruptedException, IOException, ClassNotFoundException {
			while (!requestList.isEmpty()) {
				ByteBlockRequest request = requestList.remove();
				out.writeObject(request);

				CloudByte[] newData = (CloudByte[]) in.readObject();
				for (int i = request.getIndex(), j = 0; i != request.getIndex() + request.getLength(); i++, j++) {
					data[i] = newData[j];
				}
				count++;
			}
			cdl.countDown();
			long endTime = System.currentTimeMillis();
			System.out.println(
					"O nó: " + nodePort + " descarregou: " + count + " blocos em " + (endTime - initTime) + "ms.");
		}
	}

/////////////////////////// DOWNLOAD ERROR ///////////////////////////

	private class DownloadError extends Thread {
		private Socket socket;
		private ObjectInputStream in;
		private ObjectOutputStream out;
		private ByteBlockRequest request;
		private CountDownLatch cdlError;
		private String nodeAdress;
		private int nodePort;

		private DownloadError(String nodeAdress, int nodePort, ByteBlockRequest request, CountDownLatch cdlError) {
			this.nodeAdress = nodeAdress;
			this.nodePort = nodePort;
			this.request = request;
			this.cdlError = cdlError;
		}

		@Override
		public void run() {
			try {
				doConnections();

				System.out.println("A consultar nó: " + nodePort + " para correção de erros.");

				out.writeObject(request);
				CloudByte[] newData = (CloudByte[]) in.readObject();

				correctData.add(newData[0]);
				cdlError.countDown();

			} catch (IOException e) {
				System.err.println("Não é possível ligar ao nó: " + nodePort);
				countErrorNodes--;
				this.interrupt();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				try {
					out.writeObject(null);
					socket.close();
				} catch (IOException e) {
					System.err.println("Erro no fecho da socket - DownloadError");
				}
			}
		}

		private void doConnections() throws UnknownHostException, IOException {
			socket = new Socket(nodeAdress, nodePort);
			in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());
		}
	}

/////////////////////////// DEAL WITH CLIENT ///////////////////////////

	public class DealWithClient extends Thread {
		private ObjectInputStream in;
		private ObjectOutputStream out;
		private Socket s;
		private Lock lock;

		public DealWithClient(Socket s) {
			this.s = s;
		}

		@Override
		// Obtem o pedido de dados e envia-o pela stream
		public void run() {
			try {
				doConnections();
				serve();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				try {
					s.close();
				} catch (IOException e) {
					System.err.println("Erro ao fechar socket - DealWithClient");
				}
			}
		}

		private void serve() throws IOException, ClassNotFoundException {
			while (true) {
				ByteBlockRequest request = (ByteBlockRequest) in.readObject();
				if (request == null)
					break;
				for (int i = request.getIndex(); i != request.getIndex() + request.getLength(); i++) {
					if (!data[i].isParityOk()) {
						lock = data[i].getLock();
						if (lock.tryLock()) {
							try {
								System.err.println("Foi detetado um erro na posição " + i + ":" + data[i]);
								correctError(new ByteBlockRequest(i, 1));
							} finally {
								lock.unlock();
							}
						}
					}
				}
				out.writeObject(Arrays.copyOfRange(data, request.getIndex(), request.getIndex() + request.getLength()));
			}
		}

		private void doConnections() throws IOException {
			out = new ObjectOutputStream(s.getOutputStream());
			in = new ObjectInputStream(s.getInputStream());
		}
	}

/////////////////////////// SHEARCHER THREAD ///////////////////////////

	private class SearcherThread extends Thread {
		private Lock lock;

		public SearcherThread() {
		}

		@Override
		public void run() {
			while (true) {
				for (int i = 0; i != data.length; i++) {
					if (!data[i].isParityOk()) {
						lock = data[i].getLock();
						if (lock.tryLock()) {
							try {
								System.err.println("Foi detetado um erro na posição " + i + ":" + data[i]);
								correctError(new ByteBlockRequest(i, 1));
							} finally {
								lock.unlock();
							}
						}
					}
				}
				try {
					sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length == 4 && (new File(args[3])).canRead())
			new StorageNode(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), new File(args[3]));
		else
			new StorageNode(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), null);
	}
}