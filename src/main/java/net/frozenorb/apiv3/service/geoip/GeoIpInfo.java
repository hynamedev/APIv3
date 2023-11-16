package net.frozenorb.apiv3.service.geoip;

import com.google.common.collect.ImmutableList;

import java.util.LinkedList;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Getter;

public final class GeoIpInfo {

	@Getter private GeoIpContinent continent;
	@Getter private GeoIpCity city;
	@Getter private GeoIpPostal postal;
	@Getter private GeoIpTraits traits;
	@Getter private GeoIpLocation location;
	@Getter private List<GeoIpSubdivision> subdivisions;
	@Getter private GeoIpCountry country;
	@Getter private GeoIpRegisteredCountry registeredCountry;

	private GeoIpInfo() {} // For Jackson

	public GeoIpInfo(JsonObject legacy) {
		this.continent = new GeoIpContinent(legacy.getJsonObject("continent", new JsonObject()));
		this.city = new GeoIpCity(legacy.getJsonObject("city", new JsonObject()));
		this.postal = new GeoIpPostal(legacy.getJsonObject("postal", new JsonObject()));
		this.traits = new GeoIpTraits(legacy.getJsonObject("traits"));
		this.location = new GeoIpLocation(legacy.getJsonObject("location", new JsonObject()));
		this.country = new GeoIpCountry(legacy.getJsonObject("country", new JsonObject()));
		this.registeredCountry = new GeoIpRegisteredCountry(legacy.getJsonObject("registered_country", new JsonObject()));

		List<GeoIpSubdivision> subdivisions = new LinkedList<>();

		for (Object subdivision : legacy.getJsonArray("subdivisions", new JsonArray())) {
			subdivisions.add(new GeoIpSubdivision((JsonObject) subdivision));
		}

		this.subdivisions = ImmutableList.copyOf(subdivisions);
	}

}