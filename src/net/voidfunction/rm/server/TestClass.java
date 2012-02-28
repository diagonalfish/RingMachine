package net.voidfunction.rm.server;

import java.io.IOException;

import net.voidfunction.rm.common.http.IPAddressClient;

public class TestClass {

	public static void main(String[] args) {
		
		IPAddressServer ipserver = new IPAddressServer(1661);
		try {
			ipserver.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(new IPAddressClient("http://localhost:1661/").getMyIP());
		System.out.println(new IPAddressClient("http://whatismyip.org/").getMyIP());
		System.out.println(new IPAddressClient("http://ifconfig.me/ip").getMyIP());
		System.out.println(new IPAddressClient("http://myip.dnsomatic.com/").getMyIP());
		
	}
	
}
