package net.voidfunction.rm.master;

import net.voidfunction.rm.common.NodeConsoleHandler;

public class MasterConsoleHandler implements NodeConsoleHandler {

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
