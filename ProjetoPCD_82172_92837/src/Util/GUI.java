package Util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class GUI {
	private final Dimension DIMENSION = new Dimension(150, 20);
	private JFrame frame;
	private JTextField positionText;
	private JTextField lengthText;
	private JButton consult;
	private JTextArea answers;

	public GUI(JTextField positionText, JTextField lengthText, JButton consult, JTextArea answers, JFrame frame) {
		if (positionText == null || lengthText == null || consult == null || answers == null || frame == null)
			throw new NullPointerException();
		this.frame = frame;
		this.positionText = positionText;
		this.lengthText = lengthText;
		this.consult = consult;
		this.answers = answers;

		windowOptions();
		addFrameContent();
		frame.pack();
	}

	private void windowOptions() {
		frame.setResizable(false);
		frame.setLayout(new BorderLayout());
	}

	public void open() {
		frame.setVisible(true);
	}

	private void addFrameContent() {
		JPanel flow = new JPanel();
		flow.setLayout(new FlowLayout());

		JLabel position = new JLabel("Posição a consultar: ");
		JLabel length = new JLabel("Comprimento: ");

		positionText.setPreferredSize(DIMENSION);
		lengthText.setPreferredSize(DIMENSION);

		flow.add(position);
		flow.add(positionText);
		flow.add(length);
		flow.add(lengthText);
		flow.add(consult);

		answers.setEditable(false);
		answers.setPreferredSize(new Dimension(0, 300));

		frame.add(flow, BorderLayout.NORTH);
		frame.add(answers, BorderLayout.CENTER);
	}
}