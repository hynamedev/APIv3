package net.frozenorb.apiv3.service.geoip;

import io.vertx.core.json.JsonObject;
import lombok.Getter;

public final class GeoIpPostal {

	@Getter private String code;
	@Getter private int confidence;

	private GeoIpPostal() {} // For Jackson

	public GeoIpPostal(JsonObject legacy) {
		this.code = legacy.getString("code", "");
		this.confidence = legacy.getInteger("confidence", -1);
	}

}