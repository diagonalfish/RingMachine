package net.voidfunction.rm.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class RMConfig extends Properties {

	private static final long serialVersionUID = 1L;
	
	private Node node;
	
	public RMConfig(Node node) {
		this.node = node;
	}

	public void safeLoad(String filename) {
		try {
			this.load(new FileInputStream(filename));
		} catch (FileNotFoundException e) {
			node.getLog().severe("Cannot find configuration file " + filename + "!");
		} catch (IOException e) {
			node.getLog().severe("Error loading configuration file " + filename + ": " + e.getMessage());
		}
	}

	public String getString(String prop, String def) {
		String pval = getProperty(prop);
		if (pval == null)
			return def;
		return pval;
	}

	public boolean getBool(String prop, boolean def) {
		String pval = getProperty(prop);
		if (pval == null)
			return def;
		return Boolean.valueOf(getProperty(prop));
	}

	public int getInt(String prop, int def) {
		String pval = getProperty(prop);
		if (pval == null)
			return def;
		try {
			return Integer.valueOf(pval);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public double getDouble(String prop, double def) {
		String pval = getProperty(prop);
		if (pval == null)
			return def;
		try {
			return Double.valueOf(pval);
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
