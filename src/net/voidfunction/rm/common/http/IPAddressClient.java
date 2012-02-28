package net.voidfunction.rm.common.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class IPAddressClient {

	public URL url;
	
	public IPAddressClient(URL url) {
		this.url = url;
	}
	
	public IPAddressClient(String url) {
		try {
			this.url = new URL(url);
		} catch (MalformedURLException e) {
			//do nothing
		}
	}
	
	public void setURL(URL url) {
		this.url = url;
	}
	
	public String getMyIP() {
		try {
			InputStream ips = url.openStream();
			Scanner s = new Scanner(ips).useDelimiter("\\A");
			if (s.hasNext()) return s.next().trim();
			return null;
		} catch (IOException e) {
			//TODO logging
			e.printStackTrace();
			return null;
		}
	}
	
}
