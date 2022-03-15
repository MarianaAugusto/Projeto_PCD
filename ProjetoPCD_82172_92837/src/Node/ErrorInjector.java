package Node;

import java.util.Scanner;

import Util.CloudByte;

public class ErrorInjector extends Thread {
	private CloudByte[] data;

	public ErrorInjector(CloudByte[] data) {
		this.data = data;
	}

	@SuppressWarnings("resource")
	@Override
	public void run() {
		Scanner scanner = new Scanner(System.in);
		while (true) {
			String input = scanner.nextLine();
			String[] inputSplit = input.split(" ");
			if (inputSplit[0].equals("ERROR")) {
				int byteIndex = Integer.parseInt(inputSplit[1]);
				data[byteIndex].makeByteCorrupt();
				System.out.println("ERROR: " + data[byteIndex].toString());
			}
		}
	}
}