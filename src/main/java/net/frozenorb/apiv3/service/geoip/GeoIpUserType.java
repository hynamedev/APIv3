package net.frozenorb.apiv3.service.geoip;

import lombok.Getter;

public enum GeoIpUserType {

	BUSINESS(true),
	CAFE(true),
	CELLULAR(true),
	COLLEGE(true),
	CONTENT_DELIVERY_NETWORK(false),
	DIALUP(true),
	GOVERNMENT(true),
	HOSTING(false),
	LIBRARY(true),
	MILITARY(true),
	RESIDENTIAL(true),
	ROUTER(true),
	SCHOOL(true),
	SEARCH_ENGINE_SPIDER(false),
	TRAVELER(true),
	UNKNOWN(true);

	@Getter private final boolean allowed;

	GeoIpUserType(boolean allowed) {
		this.allowed = allowed;
	}

}