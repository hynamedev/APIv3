package net.frozenorb.apiv3.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PhoneUtils {

	public static final String DEFAULT_COUNTRY_CODE = "US";
	private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

	public static boolean isValidPhone(String phoneNumber) {
		try {
			return phoneNumber != null && phoneUtil.isValidNumber(phoneUtil.parse(phoneNumber, DEFAULT_COUNTRY_CODE));
		} catch (NumberParseException ex) {
			return false;
		}
	}

	public static String toE164(String phoneNumber) {
		try {
			Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNumber, DEFAULT_COUNTRY_CODE);
			return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
		} catch (NumberParseException ex) {
			return "";
		}
	}

}