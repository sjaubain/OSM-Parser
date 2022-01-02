package heig.osmparser.utils.parsers;

import heig.osmparser.model.Graph;
import heig.osmparser.model.Way;
import heig.osmparser.utils.maths.Maths;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Parser {

    public final static Logger LOG = Logger.getLogger(Parser.class.getName());

    public void addCities(Graph g, String filename) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(filename));
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("node");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                Element element = (Element) node;
                long n = Long.parseLong(element.getAttribute("id"));
                double lat = Double.parseDouble(element.getAttribute("lat"));
                double lon = Double.parseDouble(element.getAttribute("lon"));
                int pop = 0;

                NodeList children = element.getElementsByTagName("tag");
                for (int j = 0; j < children.getLength(); ++j) {
                    Node child = children.item(j);
                    Element element1 = (Element) child;
                    if (element1.getAttribute("k").equals("population")) {
                        String val = element1.getAttribute("v");
                        if (isInteger(val)) {
                            pop = Integer.parseInt(val);
                            if (pop > g.getMaxPopulation()) {
                                g.setMaxPopulation(pop);
                            }
                        }
                    }
                }
                g.addCity(new heig.osmparser.model.Node(n, lat, lon, pop));
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private String getRoadType(Node way) {
        Element element = (Element) way;
        NodeList children = element.getElementsByTagName("tag");
        for (int i = 0; i < children.getLength(); ++i) {
            Node node = children.item(i);
            if (((Element) node).getAttribute("k").equals("highway")) {
                return ((Element) node).getAttribute("v");
            }
        }
        return null;
    }

    public Graph toGraph(String filename) throws IOException, SAXException, ParserConfigurationException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            Graph g = new Graph();

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(filename));
            doc.getDocumentElement().normalize();

            LOG.log(Level.INFO, "parsing bounds");

            NodeList nodes = doc.getElementsByTagName("bounds");
            if(nodes.getLength() != 0) {
                Element bounds = (Element) nodes.item(0);
                g.setBounds(new double[]{
                        Double.parseDouble(bounds.getAttribute("minlon")),
                        Double.parseDouble(bounds.getAttribute("maxlat")),
                        Double.parseDouble(bounds.getAttribute("maxlon")),
                        Double.parseDouble(bounds.getAttribute("minlat"))
                });
            }

            // retrieve all used nodes composing ways
            nodes = doc.getElementsByTagName("node");
            HashMap<Long, heig.osmparser.model.Node> usedNodes = new HashMap<>();
            int len = nodes.getLength();
            for (int i = 0; i < len; i++) {
                Node node = nodes.item(i);
                Element element = (Element) node;
                long n = Long.parseLong(element.getAttribute("id"));
                double lat = Double.parseDouble(element.getAttribute("lat"));
                double lon = Double.parseDouble(element.getAttribute("lon"));
                usedNodes.put(n, new heig.osmparser.model.Node(n, lat, lon, 0));
            }

            LOG.log(Level.INFO, "parsing ways");

            // connect all nodes with ways of type route
            HashMap<String, Boolean> registeredNodes = new HashMap<>();
            nodes = doc.getElementsByTagName("way");

            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);
                Element way = (Element) node;
                NodeList nodesInWay = way.getElementsByTagName("nd");

                double maxSpeed = getMaxSpeed(way);
                String roadType = getRoadType(node);

                // first node of the road
                long n1 = Long.parseLong(((Element) nodesInWay.item(0))
                        .getAttribute("ref"));
                // last node of the road
                long n2 = Long.parseLong(((Element) nodesInWay.item(nodesInWay.getLength() - 1))
                        .getAttribute("ref"));
                registeredNodes.put(String.valueOf(n1), true);
                registeredNodes.put(String.valueOf(n2), true);

                double cost = 0;
                for(int j = 0; j < nodesInWay.getLength() - 1; ++j) {
                    long cur = Long.parseLong(((Element) nodesInWay.item(j))
                            .getAttribute("ref"));
                    long next = Long.parseLong(((Element) nodesInWay.item(j + 1))
                            .getAttribute("ref"));

                    if(usedNodes.get(cur) != null && usedNodes.get(next) != null) {
                        cost = getCost(usedNodes, maxSpeed, cost, cur, next);
                    }
                }
                g.addEdge(n1, n2, cost, roadType == null ? "" : roadType);
                g.addEdge(n2, n1, cost, roadType == null ? "" : roadType);
            }
            LOG.log(Level.INFO, "parsing nodes");

            // retrieve all nodes, just keep those who are at the beginning and end of each way
            nodes = doc.getElementsByTagName("node");
            for (int i = 0; i < len; i++) {
                Node node = nodes.item(i);
                Element element = (Element) node;
                String id = element.getAttribute("id");

                if (registeredNodes.containsKey(id)) {
                    long n = Long.parseLong(element.getAttribute("id"));
                    double lat = Double.parseDouble(element.getAttribute("lat"));
                    double lon = Double.parseDouble(element.getAttribute("lon"));
                    g.addNode(new heig.osmparser.model.Node(n, lat, lon, 0));
                }
            }

            LOG.log(Level.INFO, "resolving unconnected ways");

            // first retrieve again all the ways
            nodes = doc.getElementsByTagName("way");
            for (int i = 0; i < nodes.getLength(); ++i) {

                Node node = nodes.item(i);
                Element way = (Element) node;
                NodeList nodesInWay = way.getElementsByTagName("nd");

                double maxSpeed = getMaxSpeed(way);
                String roadType = getRoadType(node);

                long firstNode = Long.parseLong(((Element) nodesInWay.item(0)).getAttribute("ref"));
                long startNode = firstNode, curNode = firstNode;
                long lastNode = Long.parseLong(((Element) nodesInWay.item(nodesInWay.getLength() - 1)).getAttribute("ref"));
                boolean foundIntermediateNodes = false;

                //TODO : allow cost depending on tag "one_pass"
                double cost = 0;
                for (int k = 1; k < nodesInWay.getLength(); ++k) {
                    Node child = nodesInWay.item(k);
                    Element kthNode = (Element) child;
                    long nextNode = Long.parseLong(kthNode.getAttribute("ref"));

                    if(usedNodes.get(curNode) != null && usedNodes.get(nextNode) != null) {
                        cost = getCost(usedNodes, maxSpeed, cost, curNode, nextNode);
                        curNode = nextNode;
                    }

                    if (g.getAdjList().containsKey(curNode)) {
                        if(curNode != lastNode) foundIntermediateNodes = true;
                        g.addEdge(startNode, curNode, cost, roadType == null ? "" : roadType);
                        g.addEdge(curNode, startNode, cost, roadType == null ? "" : roadType);
                        startNode = curNode; cost = 0.0;
                    }
                }

                if(foundIntermediateNodes) {
                    g.getAdjList().get(firstNode).removeIf(w -> w.getToId() == lastNode);
                    g.getAdjList().get(lastNode).removeIf(w -> w.getToId() == firstNode);
                }
            }

            LOG.log(Level.INFO, "resolving missing nodes");

            // remove from adjList all node ids that have not been found in the file
            // because we must know the coordinates of such nodes.
            HashMap<Long, List<Way>> curAdjList = g.getAdjList();
            HashMap<Long, List<Way>> newAdjList = new HashMap<>();

            // THIS STEP IS VERY IMPORTANT BECAUSE EACH NODE IN THE WAYS MUST BE KNOWN (LAT & LON)
            // reconstruct the new cleaned adjList with only the ids whose nodes
            // can be retrieved
            for (long id : curAdjList.keySet()) {
                if (g.getNodes().containsKey(id)) {
                    newAdjList.put(id, new LinkedList<>());
                    for (int i = 0; i < curAdjList.get(id).size(); ++i) {
                        Way w = curAdjList.get(id).get(i);
                        long id2 = w.getToId();
                        if (g.getNodes().containsKey(id2)) {
                            newAdjList.get(id).add(new Way(id, id2, w.getCost(), w.getRoadType()));
                        }
                    }
                }
            }
            g.setAdjList(newAdjList);
            return g;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw e;
        }
    }

    private double getMaxSpeed(Element way) {
        double maxSpeed = 13.8; // default speed
        NodeList tags = way.getElementsByTagName("tag");
        for (int k = 1; k < tags.getLength(); ++k) {
            Node child = tags.item(k);
            Element element2 = (Element) child;
            String key = element2.getAttribute("k");
            if(key.equals("maxspeed")) {
                String maxSpeedStr = element2.getAttribute("v");
                if(isInteger(maxSpeedStr)) {
                    maxSpeed = Integer.parseInt(element2.getAttribute("v")) / 3.6;
                }
            }
        }
        return maxSpeed;
    }
    private double getCost(HashMap<Long, heig.osmparser.model.Node> usedNodes,
                           double maxSpeed, double cost, long cur, long next) {
        double lat1 = usedNodes.get(cur).getLat();
        double lon1 = usedNodes.get(cur).getLon();
        double lat2 = usedNodes.get(next).getLat();
        double lon2 = usedNodes.get(next).getLon();
        cost += Maths.distanceNodes(new heig.osmparser.model.Node(0, lat1, lon1, 0),
                new heig.osmparser.model.Node(0, lat2, lon2, 0)) / maxSpeed;
        return cost;
    }

    private boolean isInteger(String str) {
        for(int i = 0; i < str.length(); ++i) {
            if(!Character.isDigit(str.charAt(i)))
                return false;
        }
        return true;
    }
}

