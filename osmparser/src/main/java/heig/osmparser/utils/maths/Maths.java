package heig.osmparser.utils.maths;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class Maths {

    public static int[] latsToMN03(double lat, double lon) {
        // According to MN03 specifications
        double latRef = 46.951082877;
        double lonRef = 7.438632495;

        double dX = 100000 * Math.cos(lat * Math.PI / 180) * 6371 * Math.PI / 180 * (lon - lonRef);
        double dY = 100000 * (lat - latRef) * Math.PI / 180 * 6371;

        int E = 60000000 + (int) Math.round(dX);
        int N = 20000000 + (int) Math.round(dY);

        return new int[]{E, N};
    }

    public static double round(double value, int nbDecimal) {
        if (nbDecimal < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(nbDecimal, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}