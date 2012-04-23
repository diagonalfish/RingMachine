/*
 * --------------------------
 * |    Ring Machine 2      |
 * |                        |
 * |         /---\          |
 * |         |   |          |
 * |         \---/          |
 * |                        |
 * | The Crowdsourced CDN   |
 * --------------------------
 * 
 * Copyright (C) 2012 Eric Goodwin
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package net.voidfunction.rm.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Extension of Properties object that provides safe config file loading
 * and methods to retrieve config values of different types.
 */
public class RMConfig extends Properties {

	private static final long serialVersionUID = 1L;

	private Node node;

	public RMConfig(Node node) {
		this.node = node;
	}

	/**
	 * Load properties from the given file. Catch errors and log
	 * them to the console, if any.
	 * @param filename
	 */
	public void safeLoad(String filename) {
		try {
			this.load(new FileInputStream(filename));
		} catch (FileNotFoundException e) {
			node.getLog().severe("Cannot find configuration file " + filename + "!");
		} catch (IOException e) {
			node.getLog().severe("Error loading configuration file " + filename + ": " + e.getMessage());
		}
	}

	/**
	 * Retrieve the given configuration property as a string.
	 * If the property doesn't exist or is a different type, the given
	 * default value will be returned instead (a default of null is allowed).
	 * @param prop
	 * @param def
	 * @return
	 */
	public String getString(String prop, String def) {
		String pval = getProperty(prop);
		if (pval == null)
			return def;
		return pval;
	}

	/**
	 * Retrieve the given configuration property as a boolean.
	 * If the property doesn't exist or is a different type, the given
	 * default value will be returned instead (a default of null is allowed).
	 * @param prop
	 * @param def
	 * @return
	 */
	public boolean getBool(String prop, boolean def) {
		String pval = getProperty(prop);
		if (pval == null)
			return def;
		return Boolean.valueOf(getProperty(prop));
	}

	/**
	 * Retrieve the given configuration property as an integer.
	 * If the property doesn't exist or is a different type, the given
	 * default value will be returned instead (a default of null is allowed).
	 * @param prop
	 * @param def
	 * @return
	 */
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

	/**
	 * Retrieve the given configuration property as a double.
	 * If the property doesn't exist or is a different type, the given
	 * default value will be returned instead (a default of null is allowed).
	 * @param prop
	 * @param def
	 * @return
	 */
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
