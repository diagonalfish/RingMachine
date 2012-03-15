package net.voidfunction.rm.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RMLog {

	public static void raw(String message) {
		log(null, message);
	}

	public static void debug(String message) {
		log("DEBUG", message);
	}

	public static void info(String message) {
		log("INFO", message);
	}

	public static void warn(String message) {
		log("WARN", message);
	}

	public static void severe(String message) {
		log("SEVERE", message);
	}

	public static void fatal(String message) {
		log("FATAL", message);
	}

	private static void log(String type, String message) {
		SimpleDateFormat dformat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		StringBuilder builder = new StringBuilder();
		builder.append("[" + dformat.format(new Date()) + "] ");
		if (type != null)
			builder.append("[" + type + "] ");
		builder.append(message);
		System.out.println(builder.toString());
	}

}
