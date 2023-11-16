package net.frozenorb.apiv3.service.geoip;

import io.vertx.core.json.JsonObject;
import lombok.Getter;

public final class GeoIpLocation {

	@Getter private double latitude;
	@Getter private double longitude;
	@Getter private int accuracyRadius;
	@Getter private String timeZone;
	@Getter private int populationDensity;
	@Getter private int metroCode;
	@Getter private int averageIncome;

	private GeoIpLocation() {} // For Jackson

	public GeoIpLocation(JsonObject legacy) {
		this.latitude = legacy.getDouble("latitude", -1D);
		this.longitude = legacy.getDouble("longitude", -1D);
		this.accuracyRadius = legacy.getInteger("accuracy_radius", -1);
		this.timeZone = legacy.getString("time_zone", "");
		this.populationDensity = legacy.getInteger("population_density", -1);
		this.metroCode = legacy.getInteger("metro_code", -1);
		this.averageIncome = legacy.getInteger("average_income", -1);
	}

}