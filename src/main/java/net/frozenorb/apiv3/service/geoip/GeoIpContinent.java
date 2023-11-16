package net.frozenorb.apiv3.service.geoip;

import io.vertx.core.json.JsonObject;
import lombok.Getter;

public final class GeoIpContinent {

	@Getter private String code;
	@Getter private int geonameId;
	@Getter private String name;

	private GeoIpContinent() {} // For Jackson

	public GeoIpContinent(JsonObject legacy) {
		this.code = legacy.getString("code", "");
		this.geonameId = legacy.getInteger("geoname_id", -1);
		this.name = legacy.getJsonObject("names", new JsonObject()).getString("en", "INVALID");
	}

}