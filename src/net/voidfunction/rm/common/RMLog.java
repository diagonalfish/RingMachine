package net.voidfunction.rm.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RMLog {

	private NodeConsole console;

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
