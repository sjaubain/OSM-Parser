package heig.osmparser.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import heig.osmparser.model.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public abstract class OverpassAPIClient {

    private static HttpURLConnection conn;

    public static Node getNodeById(long id) {
        BufferedReader reader;
        String line;
        StringBuilder responseContent = new StringBuilder();
        try {
            URL url = new URL("https://overpass-api.de/api/interpreter?data=[out:json];%20node("
                    + String.valueOf(id) + ");%20out%20geom;");

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int status = conn.getResponseCode();

            if (status >= 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            }

            JsonParser jsonParser = new JsonParser();
            JsonObject jo = (JsonObject) jsonParser.parse(responseContent.toString());

            double lat = jo.get("elements").getAsJsonArray().get(0).getAsJsonObject().get("lat").getAsDouble();
            double lon = jo.get("elements").getAsJsonArray().get(0).getAsJsonObject().get("lon").getAsDouble();

            return new Node(id, lat, lon, 0);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            conn.disconnect();
        }
        return null;
    }

    public static List<Node> getNodesById(List<Long> ids) {

        List<Node> ret = new LinkedList<>();

        BufferedReader reader;
        String line;
        StringBuilder responseContent = new StringBuilder();
        String query = "https://overpass-api.de/api/interpreter?data=[out:json];%20(";
        for(int i = 0; i < ids.size(); ++i) {
            query += "node(" + ids.get(i) + ");";
        }
        query += ");%20out%20geom;";

        System.out.println(query);
        try {
            URL url = new URL(query);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int status = conn.getResponseCode();

            if (status >= 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            }

            JsonParser jsonParser = new JsonParser();
            JsonObject jo = (JsonObject) jsonParser.parse(responseContent.toString());

            for (int i = 0; i < jo.get("elements").getAsJsonArray().size(); ++i) {
                double lat = jo.get("elements").getAsJsonArray().get(i).getAsJsonObject().get("lat").getAsDouble();
                double lon = jo.get("elements").getAsJsonArray().get(i).getAsJsonObject().get("lon").getAsDouble();
                ret.add(new Node(ids.get(i), lat, lon, 0, "-"));
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            conn.disconnect();
        }
        return ret;
    }
}
