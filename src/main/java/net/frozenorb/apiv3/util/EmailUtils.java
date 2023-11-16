package net.frozenorb.apiv3.util;

import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class EmailUtils {

	private static final Pattern VALID_EMAIL_PATTERN = Pattern.compile(
		// this pattern is a slight modification (remove + as a valid char due to 'duplicate emails') of the standard one
		"^[A-Z0-9._%-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
		Pattern.CASE_INSENSITIVE
	);

	public static boolean isValidEmail(String email) {
		return email != null && VALID_EMAIL_PATTERN.matcher(email).matches();
	}

}