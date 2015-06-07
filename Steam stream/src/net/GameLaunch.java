package net;
import java.io.IOException;
import java.util.Scanner;


public class GameLaunch implements Runnable{
	private Scanner scanner = new Scanner(System.in);
	private TCPControlStream stream = null;
	public GameLaunch(TCPControlStream stream){
		this.stream = stream;
	}
	
	@Override
	public void run() {
		while(true){
			String line = scanner.nextLine();
			try {
				stream.startStream(Integer.parseInt(line));
				System.out.println("Read String " + line);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("Not an integer");
			}
		}
	}

}
