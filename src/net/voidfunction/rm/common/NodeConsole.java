package net.voidfunction.rm.common;

import java.io.IOException;

import jline.console.ConsoleReader;

public class NodeConsole {

	private ConsoleReader reader;
	private NodeConsoleHandler handler;

	public NodeConsole(NodeConsoleHandler handler) throws IOException {
		this.handler = handler;

		reader = new ConsoleReader(System.in, System.out);
		reader.setPrompt("> ");
	}

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

	public void printLine(String line) {
		try {
			reader.print(ConsoleReader.RESET_LINE + line + "\n");
			reader.flush();
		} catch (IOException e) {
		}
	}

}
