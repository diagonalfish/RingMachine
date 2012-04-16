package net.voidfunction.rm.common;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility functions for HTTP servers
 */
public class HTTPUtils {
	private static SimpleDateFormat dateFormat;
	static {
		dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public static String getServerTime(long offset) {
		Calendar calendar = Calendar.getInstance();
		Date outDate = new Date(calendar.getTime().getTime() + (offset * 1000));
		return dateFormat.format(outDate);
	}

}
