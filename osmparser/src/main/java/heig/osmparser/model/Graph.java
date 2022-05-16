package heig.osmparser.model;

import heig.osmparser.maths.Maths;
import javafx.util.Pair;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Graph {

    final static Logger LOG = Logger.getLogger(Graph.class.getName());

    private final int MAX_NODES = 100;

    private HashMap<Long, Node> nodes;

    private HashMap<Long, List<Way>> adjList;

    private HashMap<Long, Node> cities;

    private double[] bounds;

    private int maxPopulation;

    private HashMap<Long, Long> pred;

    private HashMap<Long, Double> lambdaTime;

    private HashMap<Long, Double> lambdaLength;

    public Graph() {
        nodes = new HashMap<>();
        cities = new HashMap<>();
        adjList = new HashMap<>();
        bounds = new double[4];
    }

    public void addNode(Node n) {
        if(!nodes.containsKey(n)) {
            nodes.put(n.getId(), n);
        }
    }

    public void addCity(Node n) {
        if(!cities.containsKey(n)) {
            cities.put(n.getId(), n);
        }
    }

    public boolean containsNode(long id) {
        return nodes.containsKey(id);
    }

    public void addEdge(Way way) {

        if (!adjList.containsKey(way.getFromId())) {
            List list = new LinkedList<>(); list.add(way);
            adjList.put(way.getFromId(), list);
        } else {
            adjList.get(way.getFromId()).add(way);
        }
    }

    public Node getClosestNodeFromGPSCoords(double lat, double lon) {
        Node minNode = new Node();
        double minDist = Double.MAX_VALUE;
        for(Node n : nodes.values()) {
            double curDist = Maths.haversine(+n.getLat(), n.getLon(), lat, lon);
            if(curDist < minDist) {
                minDist = curDist;
                minNode = n;
            }
        }
        return minNode;
    }

    public String toString() {
        String ret = "";
        if(nodes.size() > MAX_NODES) {
            LOG.log(Level.INFO, "there are too many nodes to display the graph (>" + MAX_NODES + ")");
        } else {
            for(Long i : adjList.keySet()) {
                ret += "successors of Node " + i + " : ";
                for(Way w : adjList.get(i))
                    ret += w.getToId() + " ";
                ret += System.lineSeparator();
            }
        }
        return ret;
    }

    public void dijkstra(long src) {
        pred = new HashMap<>();
        lambdaTime = new HashMap<>();
        lambdaLength = new HashMap<>();
        for(long node : adjList.keySet()) {
            if(node == src) {
                lambdaTime.put(node, 0.0);
                lambdaLength.put(node, 0.0);
            } else {
                lambdaTime.put(node, Double.MAX_VALUE);
                lambdaLength.put(node, Double.MAX_VALUE);
            }
            pred.put(node, (long)-1);
        }
        // custom min heap
        PriorityQueue<Pair<Double, Long>> Q = new PriorityQueue<>(Comparator.comparing(Pair::getKey));
        Q.add(new Pair<>(0.0, src));
        while(!Q.isEmpty()) {
            Pair<Double, Long> p = Q.poll();
            long i_id = p.getValue();
            Node i = nodes.get(i_id);
            if(i != null) {
                for (Way weightedEdge : adjList.get(i_id)) {
                    long j_id = weightedEdge.getToId();
                    Node j = nodes.get(j_id);
                    if(j != null) {
                        double c_ij = weightedEdge.getTimeCost();
                        double c_ij_p = weightedEdge.getLengthCost();
                        if(lambdaTime.get(j_id) > lambdaTime.get(i_id) + c_ij) {
                            lambdaTime.replace(j_id, lambdaTime.get(i_id) + c_ij);
                            lambdaLength.replace(j_id, lambdaLength.get(i_id) + c_ij_p);
                            pred.replace(j_id, i_id);
                            Q.add(new Pair<>(lambdaTime.get(j_id), j_id));
                        }
                    }
                }
            }
        }
    }

    public List<Node> getShortestPath(Node dest) {
        List<Node> ret = new LinkedList<>();
        while(pred.get(dest.getId()) != (long)-1) {
            ret.add(dest); dest = nodes.get(pred.get(dest.getId()));
        }
        ret.add(dest);
        return ret;
    }

    public HashMap<Long, List<Way>> getAdjList() {
        return adjList;
    }

    public HashMap<Long, Node> getNodes() {
        return nodes;
    }

    public HashMap<Long, Node> getCities() {
        return cities;
    }

    public int size() { return adjList.size(); }

    public double[] getBounds() {
        return bounds;
    }

    public int getMaxPopulation() {
        return maxPopulation;
    }

    public HashMap<Long, Double> getLambdaTime() {
        return lambdaTime;
    }

    public HashMap<Long, Double> getLambdaLength() {
        return lambdaLength;
    }

    public HashMap<Long, Long> getPred() {
        return pred;
    }

    public void setBounds(double[] bounds) {
        this.bounds = bounds;
    }

    public void setMaxPopulation(int maxPopulation) {
        this.maxPopulation = maxPopulation;
    }

    public void setAdjList(HashMap<Long, List<Way>> adjList) {
        this.adjList = adjList;
    }
}
