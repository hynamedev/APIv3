package net.frozenorb.apiv3.util;

import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

@UtilityClass
public class IpUtils {

	private static final Pattern VALID_IP_PATTERN = Pattern.compile(
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

	public static boolean isValidIp(String ip) {
		if (ip == null) {
			return false;
		}

		// :(
		if (ip.equals("::1")) {
			return true;
		} else {
			return VALID_IP_PATTERN.matcher(ip).matches();
		}
	}

}