package net.frozenorb.apiv3.util;

import net.frozenorb.apiv3.service.geoip.GeoIpLocation;

public final class GeoJsonPoint {

    private String type = "Point";
    private double[] coordinates;

    private GeoJsonPoint() {} // For Jackson

    public GeoJsonPoint(GeoIpLocation geoIpLocation) {
        this(geoIpLocation.getLongitude(), geoIpLocation.getLatitude());
    }

    public GeoJsonPoint(double longitude, double latitude) {
        this.coordinates = new double[] { longitude, latitude};
    }

    public double getLongitude() {
        return coordinates[0];
    }

    public double getLatitude() {
        return coordinates[1];
    }

}