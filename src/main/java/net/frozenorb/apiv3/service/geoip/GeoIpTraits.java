package net.frozenorb.apiv3.service.geoip;

import io.vertx.core.json.JsonObject;
import lombok.Getter;

public final class GeoIpTraits {

	@Getter private String isp;
	@Getter private String domain;
	@Getter private int asn;
	@Getter private String asnOrganization;
	@Getter private GeoIpUserType userType;
	@Getter private String organization;

	private GeoIpTraits() {} // For Jackson

	public GeoIpTraits(JsonObject legacy) {
		this.isp = legacy.getString("isp", "");
		this.domain = legacy.getString("domain", "");
		this.asn = legacy.getInteger("autonomous_system_number", -1);
		this.asnOrganization = legacy.getString("autonomous_system_organization", "");
		this.userType = legacy.containsKey("user_type") ? GeoIpUserType.valueOf(legacy.getString("user_type").toUpperCase()) : GeoIpUserType.UNKNOWN;
		this.organization = legacy.getString("organization", "");
	}

}