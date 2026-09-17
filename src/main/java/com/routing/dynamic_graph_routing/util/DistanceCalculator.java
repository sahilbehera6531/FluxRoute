package com.routing.dynamic_graph_routing.util;

public final class DistanceCalculator {

    // Earth's mean radius in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;

    private DistanceCalculator() {
        // Private constructor prevents instantiation of utility class
    }

    /**
     * Calculates the great-circle distance between two GPS points using the Haversine formula.
     * Guaranteed admissible straight-line distance h(n) <= g*(n).
     *
     * @param lat1 Latitude of source node in decimal degrees
     * @param lon1 Longitude of source node in decimal degrees
     * @param lat2 Latitude of target node in decimal degrees
     * @param lon2 Longitude of target node in decimal degrees
     * @return Straight-line distance in kilometers
     */
    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double originLatRad = Math.toRadians(lat1);
        double targetLatRad = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(originLatRad) * Math.cos(targetLatRad)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_KM * c;
    }
}