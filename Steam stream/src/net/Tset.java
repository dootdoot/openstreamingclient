package net;

import java.io.ByteArrayInputStream;

public class Tset {

	public static void main(String[] args) {
		byte[] array = new byte[]{
			1, 2, 3, 4, 5	
		};
		
		ByteArrayInputStream bais = new ByteArrayInputStream(array);
		byte next = 0;
		while((next = (byte) bais.read()) != -1){
			System.out.println(next);
		}
	}

}
