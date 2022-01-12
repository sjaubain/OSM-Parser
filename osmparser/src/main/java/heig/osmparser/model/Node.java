package heig.osmparser.model;

public class Node {
    private long id;
    private double lat; private double lon;
    long population;
    String name;
    public Node(long id, double lat, double lon) {
        this.id = id; this.lat = lat; this.lon = lon; this.population = -1; this.name = "-";
    }
    public Node(long id, double lat, double lon, long population) {
        this(id, lat, lon); this.population = population; this.name = "-";
    }
    public Node(long id, double lat, double lon, long population, String name) {
        this(id, lat, lon, population); this.name = name;
    }
    public Node() {
        this(0, 0, 0, 0);
    }

    public long getId() {
        return id;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public long getPopulation() {
        return population;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "Node [" + id + "] : lat " + lat + ", lon : " + lon;
    }
}
