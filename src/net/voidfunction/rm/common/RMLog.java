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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple logger object that outputs to STDOUT or to a NodeConsole, with basic
 * timestamps and various levels.
 */
public class RMLog {

	private NodeConsole console;

	/**
	 * Log a message without a level.
	 * @param message
	 */
	public void raw(String message) {
		log(null, message);
	}

	public void debug(String message) {
		log("DEBUG", message);
	}

	public void info(String message) {
		log("INFO", message);
	}

	public void warn(String message) {
		log("WARN", message);
	}

	public void severe(String message) {
		log("SEVERE", message);
	}

	public void fatal(String message) {
		log("FATAL", message);
	}

	private void log(String type, String message) {
		SimpleDateFormat dformat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		StringBuilder builder = new StringBuilder();
		builder.append("[" + dformat.format(new Date()) + "] ");
		if (type != null)
			builder.append("[" + type + "] ");
		builder.append(message);

		if (console == null)
			System.out.println(builder.toString());
		else
			console.printLine(builder.toString());
	}

	public void setConsole(NodeConsole console) {
		this.console = console;
	}

}
