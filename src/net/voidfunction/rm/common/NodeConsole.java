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

import java.io.IOException;

import jline.console.ConsoleReader;

/**
 * Console which provides command-line input for a node using JLine.
 */
public class NodeConsole {

	private ConsoleReader reader;
	private NodeConsoleHandler handler;

	/**
	 * Creates a new console object, which will pass any input lines
	 * to the given handler for processing.
	 * @param handler
	 * @throws IOException
	 */
	public NodeConsole(NodeConsoleHandler handler) throws IOException {
		this.handler = handler;

		reader = new ConsoleReader(System.in, System.out);
		reader.setPrompt("> ");
	}

	/**
	 * Start this console's input loop.
	 * @throws IOException
	 */
	public void run() throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.trim().equals(""))
				continue;
			String result = handler.handle(line);
			if (result == null)
				printLine("Unknown command.");
			else
				printLine(result);
		}
	}

	/**
	 * Print a line of output to the console.
	 * @param line
	 */
	public void printLine(String line) {
		try {
			reader.print(ConsoleReader.RESET_LINE + line + "\n");
			reader.flush();
		} catch (IOException e) {
		}
	}

}
