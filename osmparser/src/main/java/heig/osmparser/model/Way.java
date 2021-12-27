package heig.osmparser.model;

public class Way {
    private long fromId;
    private long toId;
    private double cost;
    private String roadType;

    public Way(long fromId, long toId, double cost, String roadType) {
        this.fromId = fromId;
        this.toId = toId;
        this.cost = cost;
        this.roadType = roadType;
    }
    public long getFromId() {
        return fromId;
    }
    public long getToId() {
        return toId;
    }
    public String getRoadType() {
        return roadType;
    }
    public double getCost() {
        return cost;
    }
}
