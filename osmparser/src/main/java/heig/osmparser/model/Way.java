package heig.osmparser.model;

public class Way {
    private long fromId;
    private long toId;
    private double timeCost;
    private double lengthCost;
    private String roadType;

    public Way(long fromId, long toId, double timeCost, double lengthCost, String roadType) {
        this.fromId = fromId;
        this.toId = toId;
        this.timeCost = timeCost;
        this.lengthCost = lengthCost;
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
    public double getTimeCost() {
        return timeCost;
    }
    public double getLengthCost() {
        return lengthCost;
    }
}
