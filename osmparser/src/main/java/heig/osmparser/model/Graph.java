package heig.osmparser.model;

import heig.osmparser.controllers.MainController;
import heig.osmparser.utils.logs.Log;
import heig.osmparser.utils.maths.Maths;
import javafx.fxml.Initializable;
import javafx.util.Pair;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Graph {

    final static Logger LOG = Logger.getLogger(Graph.class.getName());

    private final int MAX_NODES = 10;

    private HashMap<Long, Node> nodes;

    private HashMap<Long, List<Long>> adjList;

    private HashMap<Long, Node> cities;

    private double[] bounds;

    private int maxPopulation;

    private HashMap<Long, Long> pred;

    private HashMap<Long, Double> lambda;

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

    public double distance(Node n1, Node n2) {
        int[] shape1 = Maths.latsToMN03(n1.getLat(), n1.getLon());
        int[] shape2 = Maths.latsToMN03(n2.getLat(), n2.getLon());
        double dX = Math.abs((double)(shape2[0] - shape1[0])), dY = Math.abs((double)(shape2[1] - shape1[1]));
        return dX * dX + dY * dY;
    }

    public boolean containsNode(long id) {
        return nodes.containsKey(id);
    }

    public boolean adjListContainsNode(long id) {
        return adjList.containsKey(id);
    }

    public void addEdge(Long n1Id, Long n2Id) {

        if (!adjList.containsKey(n1Id)) {
            List list = new LinkedList<>(); list.add(n2Id);
            adjList.put(n1Id, list);
        } else {
            adjList.get(n1Id).add(n2Id);
        }
    }

    public String toString() {
        String ret = "";
        if(nodes.size() > MAX_NODES) {
            LOG.log(Level.INFO, "there are too many nodes to display the graph (>" + MAX_NODES + ")");
        } else {
            for(Long i : adjList.keySet()) {
                ret += "successors of Node " + i + " : ";
                for(Long j : adjList.get(i))
                    ret += j + " ";
                ret += System.lineSeparator();
            }
        }
        return ret;
    }

    public void dijkstra(long src) {
        pred = new HashMap<>();
        lambda = new HashMap<>();
        for(long node : adjList.keySet()) {
            if(node == src)
                lambda.put(node, 0.0);
            else
                lambda.put(node, Double.MAX_VALUE);
            pred.put(node, (long)-1);
        }
        // custom min heap
        PriorityQueue<Pair<Double, Long>> Q = new PriorityQueue<>(Comparator.comparing(Pair::getKey));
        Q.add(new Pair<>(0.0, src));
        while(!Q.isEmpty()) {
            Pair<Double, Long> p = Q.poll();
            long i_id = p.getValue();
            Node i = nodes.get(i_id);
            //System.out.println("threating node : " + i_id);
            if(i != null) {
                for (long j_id : adjList.get(i_id)) {
                    //System.out.println("    with neighbor : " + j_id);
                    Node j = nodes.get(j_id);
                    if(j != null) {
                        double c_ij = distance(i, j);
                        //System.out.println(c_ij);
                        if(lambda.get(j_id) > lambda.get(i_id) + c_ij) {
                            lambda.replace(j_id, lambda.get(i_id) + c_ij);
                            pred.replace(j_id, i_id);
                            Q.add(new Pair<>(lambda.get(j_id), j_id));
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

    public HashMap<Long, List<Long>> getAdjList() {
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

    public HashMap<Long, Double> getLambda() {
        return lambda;
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

    public void setAdjList(HashMap<Long, List<Long>> adjList) {
        this.adjList = adjList;
    }
}
