package heig.osmparser.utils.maths;

import heig.osmparser.model.Node;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class Maths {

    // earth radius used in OSM
    public final static double R =  6378.137;

    public static double toRad(double deg) {
        return Math.PI / 180d * deg;
    }

    /*
     * specific to swiss system
     * returns distance in centimeters
     */
    public static int[] latsToMN03(double lat, double lon) {
        // According to MN03 specifications
        double latRef = 46.951082877;
        double lonRef = 7.438632495;

        double dX = 100000 * Math.cos(lat * Math.PI / 180) * R * Math.PI / 180 * (lon - lonRef);
        double dY = 100000 * (lat - latRef) * Math.PI / 180 * R;

        int E = 60000000 + (int) Math.round(dX);
        int N = 20000000 + (int) Math.round(dY);

        return new int[]{E, N};
    }

    /*
     * transform (lat, lon) for Mercator projection
     * returns (x, y)
     */
    public static double[] mapProjection(double lat, double lon) {
        return new double[]{toRad(lon) * R, Math.log(Math.tan(Math.PI / 4 + toRad(lat) / 2)) * R};
    }

    /*
     * returns distance in kilometers
     */
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = toRad(lat2 - lat1);
        double lonDistance = toRad(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /*
     * return distance between two nodes in meters
     */
    public static double distanceNodes(Node n1, Node n2) {
        return haversine(n1.getLat(), n1.getLon(), n2.getLat(), n2.getLon()) * 1000;
    }

    public static double distanceGPS(double lat1, double lon1, double lat2, double lon2) {
        return distanceNodes(new Node(0, lat1, lon1, 0), new Node(0, lat2, lon2, 0));
    }

    public static double round(double value, int nbDecimal) {
        if (nbDecimal < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(nbDecimal, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}