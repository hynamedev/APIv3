package net.frozenorb.apiv3.service.geoip;

import io.vertx.core.json.JsonObject;
import lombok.Getter;

public final class GeoIpCountry {

	@Getter private String isoCode;
	@Getter private int confidence;
	@Getter private int geonameId;
	@Getter private String name;

	private GeoIpCountry() {} // For Jackson

	public GeoIpCountry(JsonObject legacy) {
		this.isoCode = legacy.getString("iso_code", "");
		this.confidence = legacy.getInteger("confidence", -1);
		this.geonameId = legacy.getInteger("geoname_id", -1);
		this.name = legacy.getJsonObject("names", new JsonObject()).getString("en", "INVALID");
	}

}