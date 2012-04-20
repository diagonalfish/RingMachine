package net.voidfunction.rm.worker;

import net.voidfunction.rm.common.NodeConsoleHandler;

public class WorkerConsoleHandler implements NodeConsoleHandler {

	@Override
	public String handle(String line) {
		String[] parts = line.split(" ");
		if (parts[0].startsWith("q") || parts[0].equals("stop")) {
			// TODO: Log?
			System.exit(1);
		}
		return null;
	}

}
