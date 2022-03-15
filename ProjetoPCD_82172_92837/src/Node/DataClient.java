package Node;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import Util.ByteBlockRequest;
import Util.CloudByte;
import Util.GUI;

public class DataClient {
	private ByteBlockRequest request;
	private Socket socket;
	private GUI gui;
	private JFrame frame;
	private JTextField positionText;
	private JTextField lengthText;
	private JButton consult;
	private JTextArea answers;

	public DataClient(String nodeAdress, int nodePort) throws IOException {

		frame = new JFrame("Cliente");
		positionText = new JTextField();
		lengthText = new JTextField();
		answers = new JTextArea();
		
		consult = new JButton("Search");
		consult.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				try {
					socket = new Socket(nodeAdress, nodePort);
				} catch (IOException e1) {
					System.err.println("Erro a abrir a socket do DataClient, para procura");
				}
				
				try {
					
					//do Connections
					ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
					ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
					
					//send Requests
					request = new ByteBlockRequest(Integer.parseInt(positionText.getText()), Integer.parseInt(lengthText.getText()));
					out.writeObject(request);
					
					//receive info and display
					CloudByte[] data = (CloudByte[]) in.readObject();
					String dados = "";
					for (int i = 0; i != data.length; i++) {
						if (i % 5 == 0 && i != 0)
							dados += "\n";
						dados += data[i].toString() + " ";
					}
					answers.setText(dados);
					
					out.writeObject(null);
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				} finally {
					try {
						socket.close();
					} catch (IOException e) {
						System.err.println("Erro no fecho da socket do DataClient, após procura");
					}
				}
			}
		});

		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				if (JOptionPane.showConfirmDialog(frame, "Tem a certeza que quer fechar a janela?", "Fechar Janela?",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					System.exit(0);
				}
			}
		});
		
		gui = new GUI(positionText, lengthText, consult, answers, frame);
		gui.open();
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length == 2)
			new DataClient(args[0], Integer.parseInt(args[1]));
	}
}