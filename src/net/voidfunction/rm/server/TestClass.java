package net.voidfunction.rm.server;

import java.io.IOException;

public class TestClass {

	public static void main(String[] args) {
		
		IPAddressServer ipserver = new IPAddressServer(1661);
		try {
			ipserver.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
