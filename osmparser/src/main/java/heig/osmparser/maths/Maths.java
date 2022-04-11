package heig.osmparser.maths;

import heig.osmparser.model.Node;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class Maths {

    // earth radius used in OSM
    public final static double R = 6378.137;

    // depending on which scale between poles we want, here we want values between -1 and 1
    public final static double SCALING_FACTOR = 1 / Math.PI;

    public final static double MAX_LAT = 85.0511;

    public final static double MAX_LON = 180;

    // default is ~2 with the max lat above
    public final static double WORLD_MAP_SCALE = mapProjection(MAX_LAT, 0)[1] - mapProjection(-MAX_LAT, 0)[1];

    public static double toRad(double deg) {
        return Math.PI / 180d * deg;
    }

    public static double toDeg(double rad) { return 180 * rad / Math.PI;}

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
     * specific to swiss system
     * East and North are in meters
     * source : https://www.swisstopo.admin.ch/fr/cartes-donnees-en-ligne/calculation-services/navref.html
     */
    public static double[] MN95ToLatLon(double E, double N) {
        double yp = (E - 2600000) / 1000000;
        double xp = (N - 1200000) / 1000000;
        double lambdap = 2.6779094 + 4.728982 * yp + 0.791484 * xp * yp + 0.1306 * yp * xp * xp  - 0.0436 * yp * yp * yp;
        double phip = 16.9023892 + 3.238272 * xp - 0.270978 * yp * yp - 0.002528 * xp * xp
                - 0.0447 * yp * yp * xp - 0.0140 * xp * xp * xp;
        return new double[]{phip * 100./36, lambdap * 100./36};
    }

    /*
     * transform (lat, lon) for Mercator projection
     * returns (x, y) both in range [-1, 1] for correct values for lat and lon
     * (between min an max lat)
     */
    public static double[] mapProjection(double lat, double lon) {
        return new double[]{toRad(lon) * SCALING_FACTOR,
                Math.log(Math.tan(Math.PI / 4 + toRad(lat) / 2)) * SCALING_FACTOR};
    }

    /*
     * transform (x, y) for Mercator projection
     * returns (lat, lon), x and y should to be in range [-1, 1]
     */
    public static double[] mapProjectionInv(double x, double y) {
        return new double[]{toDeg((Math.atan(Math.exp(y / SCALING_FACTOR)) - Math.PI / 4) * 2),
                toDeg(x / SCALING_FACTOR)};
    }

    /*
     * returns real distance in kilometers between two coordinates on the globe
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

    public static double round(double value, int nbDecimal) {
        if (nbDecimal < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(nbDecimal, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}