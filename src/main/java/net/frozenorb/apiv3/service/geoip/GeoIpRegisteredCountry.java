package net.frozenorb.apiv3.service.geoip;

import io.vertx.core.json.JsonObject;
import lombok.Getter;

public final class GeoIpRegisteredCountry {

	@Getter private String isoCode;
	@Getter private int geonameId;
	@Getter private String name;

	private GeoIpRegisteredCountry() {} // For Jackson

	public GeoIpRegisteredCountry(JsonObject legacy) {
		this.isoCode = legacy.getString("iso_code", "");
		this.geonameId = legacy.getInteger("geoname_id", -1);
		this.name = legacy.getJsonObject("names", new JsonObject()).getString("en", "INVALID");
	}

}