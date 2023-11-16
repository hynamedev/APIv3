package net.frozenorb.apiv3.util;

import java.time.Instant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeUtils {

	public static String formatIntoDetailedString(int secs) {
		if (secs == 0) {
			return "0 seconds";
		}

		int remainder = secs % 86400;

		int days = secs / 86400;
		int hours = remainder / 3600;
		int minutes = (remainder / 60) - (hours * 60);
		int seconds = (remainder % 3600) - (minutes * 60);

		String fDays = (days > 0 ? " " + days + " day" + (days > 1 ? "s" : "") : "");
		String fHours = (hours > 0 ? " " + hours + " hour" + (hours > 1 ? "s" : "") : "");
		String fMinutes = (minutes > 0 ? " " + minutes + " minute" + (minutes > 1 ? "s" : "") : "");
		String fSeconds = (seconds > 0 ? " " + seconds + " second" + (seconds > 1 ? "s" : "") : "");

		return (fDays + fHours + fMinutes + fSeconds).trim();
	}

	public static int getSecondsBetween(Instant a, Instant b) {
		return (Math.abs((int) (a.toEpochMilli() - b.toEpochMilli()) / 1000));
	}

}