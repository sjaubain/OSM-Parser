package heig.osmparser.configs;

import javafx.scene.paint.Color;

import java.util.HashMap;

public abstract class Config {
    public static HashMap<String, Double> roadTypeStrokeWidth = new HashMap<>() {{
        put("", 0.4);
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

    public static HashMap<String, Color> roadTypeColor = new HashMap<>() {{
        put("", Color.RED);
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
}
