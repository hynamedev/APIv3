package net.frozenorb.apiv3.service.sms;

import io.vertx.core.json.JsonObject;
import lombok.Getter;

public final class SmsCarrierInfo {

	@Getter private String phoneNumber;
	@Getter private String countryCode;
	@Getter private int carrierId;
	@Getter private String network;
	@Getter private boolean mobile;

	private SmsCarrierInfo() {} // For Jackson

	SmsCarrierInfo(JsonObject legacy) {
		this.phoneNumber = legacy.getString("phone_number");
		this.countryCode = legacy.getString("country_code");
		this.carrierId = legacy.getInteger("carrier_id", -1);
		this.network = legacy.getString("network");
		this.mobile = Boolean.parseBoolean(legacy.getString("mobile"));
	}

}