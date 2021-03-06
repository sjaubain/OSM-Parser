package heig.osmparser.configs;

import javafx.scene.paint.Color;

import java.util.HashMap;

public abstract class Config {
    public static HashMap<String, Double> ROAD_TYPE_STROKE_WIDTH = new HashMap<String, Double>() {{
        put("", 0.07);
        put("motorway", 0.6);
        put("motorway_link", 0.6);
        put("primary", 0.3);
        put("primary_link", 0.3);
        put("secondary", 0.2);
        put("secondary_link", 0.2);
        put("trunk", 0.2);
        put("trunk_link", 0.2);
        put("unclassified", 0.2);
        put("tertiary", 0.1);
        put("tertiary_link", 0.1);
        put("residential", 0.08);
    }};

    public static HashMap<String, Color> ROAD_TYPE_COLOR = new HashMap<String, Color>() {{
        put("", Color.GREEN);
        put("motorway", Color.ORANGE);
        put("motorway_link", Color.ORANGE);
        put("primary", Color.DARKORANGE);
        put("primary_link", Color.DARKORANGE);
        put("secondary", Color.YELLOW);
        put("secondary_link", Color.YELLOW);
        put("trunk", Color.YELLOWGREEN);
        put("trunk_link", Color.YELLOWGREEN);
        put("unclassified", Color.WHITE);
        put("tertiary", Color.LIGHTYELLOW);
        put("tertiary_link", Color.LIGHTYELLOW);
        put("residential", Color.LIGHTCYAN);
    }};

    public static Double getRoadTypeStrokeWidth(String roadType) {
        Double sw = ROAD_TYPE_STROKE_WIDTH.get(roadType);
        if(sw == null) return ROAD_TYPE_STROKE_WIDTH.get("");
        return sw;
    }

    public static Color getRoadTypeColor(String roadType) {
        Color c = ROAD_TYPE_COLOR.get(roadType);
        if(c == null) return ROAD_TYPE_COLOR.get("");
        return c;
    }

    // because algorithm take into account the max speed limit, which is not the real speed
    public static double SPEED_SMOOTH_FACTOR = 1 / 0.7;
}
