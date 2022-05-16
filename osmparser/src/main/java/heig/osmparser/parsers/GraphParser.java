package heig.osmparser.parsers;

import heig.osmparser.controllers.MainController;
import heig.osmparser.controllers.MainControllerHandler;
import heig.osmparser.logs.Log;
import heig.osmparser.maths.Maths;
import heig.osmparser.model.Graph;
import heig.osmparser.model.Way;
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
import java.util.logging.Logger;

/**
 * When we extract the ways using --un flag with osmosis from a .pbf or .osm file,
 * we may get ways with start or end node that has not been retrieved with --un
 * (i.e. nodes that overflow the bounding box). In this case, we cannot know the GPS
 * coordinates of such a node. To avoid this problem, we just get rid of the unknown
 * nodes. All these steps could be avoid because we use "completeWays=yes" as
 * --bounding-box option when filtering. Anyway, it is more safe to let the code so
 *
 *  1) we first store all used nodes (composing the way).
 *  2) then we construct the adjacency list while parsing the ways. If we found
 *     unknown nodes either starting or ending node, the cost will be 0 and the
 *     edge will still be registered. In order to add the nodes and their coordinates
 *     to the graph during the next step, we keep track of them looking at their ids
 *     and storing them in a temporary data structure 'registeredNodes'.
 *  3) Parsing used nodes : if a node has been registered during ways parsing, we
 *     add it to the graph.
 *  4) Resolving missing ways : find node k between i -> j and connect i to k, k to j
 *     and remove i -> j (k must be a registered node, i.e. either start or end
 *  5) Important step. We then remove from adjacency list the edges that contain
 *     nodes that have not been retrieved (step 1).
 */
public class GraphParser extends MainControllerHandler {

    public final static Logger LOG = Logger.getLogger(GraphParser.class.getName());

    private MainController controller;

    public GraphParser(MainController controller) {
        super(controller);
    }

    public GraphParser() {
        super();
    }

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
                String cityName = "-";

                // look for "population" and "name" attributes
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

                    if (element1.getAttribute("k").equals("name"))
                        cityName = element1.getAttribute("v");
                }
                g.addCity(new heig.osmparser.model.Node(n, lat, lon, pop, cityName));
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public Graph toGraph(String filename) throws IOException, SAXException, ParserConfigurationException {

        // todo : try to factorize code by combining steps 2 and 5
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            Graph g = new Graph();

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(filename));
            doc.getDocumentElement().normalize();

            sendMessageToController("parsing bounds", Log.LogLevels.INFO);

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

            // retrieve all used nodes composing ways, may use a lot of memory for too big maps
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

            sendMessageToController("parsing ways", Log.LogLevels.INFO);

            // retrieve all nodes, just keep those who are at the beginning and end of each way
            HashMap<String, Boolean> registeredNodes = new HashMap<>();
            nodes = doc.getElementsByTagName("way");

            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);
                Element way = (Element) node;
                if(isHighWay(way)) {
                    NodeList nodesInWay = way.getElementsByTagName("nd");

                    double maxSpeed = getMaxSpeed(way);
                    String roadType = getRoadType(way);
                    boolean bothSides = bothSideTraversable(way);

                    // first node of the road
                    long n1 = Long.parseLong(((Element) nodesInWay.item(0))
                            .getAttribute("ref"));
                    // last node of the road
                    long n2 = Long.parseLong(((Element) nodesInWay.item(nodesInWay.getLength() - 1))
                            .getAttribute("ref"));
                    registeredNodes.put(String.valueOf(n1), true);
                    registeredNodes.put(String.valueOf(n2), true);

                    double timeCost = 0;
                    for (int j = 0; j < nodesInWay.getLength() - 1; ++j) {
                        long cur = Long.parseLong(((Element) nodesInWay.item(j))
                                .getAttribute("ref"));
                        long next = Long.parseLong(((Element) nodesInWay.item(j + 1))
                                .getAttribute("ref"));

                        if (usedNodes.get(cur) != null && usedNodes.get(next) != null) {
                            timeCost = getTimeCost(usedNodes, maxSpeed, timeCost, cur, next);
                        }
                    }
                    g.addEdge(new Way(n1, n2, timeCost, timeCost * maxSpeed, roadType == null ? "" : roadType));
                    g.addEdge(new Way(n2, n1, bothSides ? timeCost : Double.MAX_VALUE,
                                              bothSides ? timeCost * maxSpeed : Double.MAX_VALUE,
                                              roadType == null ? "" : roadType));
                }
            }

            sendMessageToController("parsing nodes", Log.LogLevels.INFO);

            // add to graph all nodes at the beginning and end of each way
            // (those who have been retrieved during adjList construction)
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

            sendMessageToController("resolving unconnected ways", Log.LogLevels.INFO);

            // first retrieve again all the ways
            nodes = doc.getElementsByTagName("way");

            for (int i = 0; i < nodes.getLength(); ++i) {

                Node node = nodes.item(i);
                Element way = (Element) node;
                if(isHighWay(way)) {
                    NodeList nodesInWay = way.getElementsByTagName("nd");

                    double maxSpeed = getMaxSpeed(way);
                    String roadType = getRoadType(way);
                    boolean bothSides = bothSideTraversable(way);

                    long firstNode = Long.parseLong(((Element) nodesInWay.item(0)).getAttribute("ref"));
                    long startNode = firstNode, curNode = firstNode;
                    long lastNode = Long.parseLong(((Element) nodesInWay.item(nodesInWay.getLength() - 1)).getAttribute("ref"));
                    boolean foundIntermediateNodes = false;

                    double timeCost = 0;
                    for (int k = 1; k < nodesInWay.getLength(); ++k) {
                        Node child = nodesInWay.item(k);
                        Element kthNode = (Element) child;
                        long nextNode = Long.parseLong(kthNode.getAttribute("ref"));

                        if (usedNodes.get(curNode) != null && usedNodes.get(nextNode) != null) {
                            timeCost = getTimeCost(usedNodes, maxSpeed, timeCost, curNode, nextNode);
                            curNode = nextNode;
                        }

                        /**
                         * search for intermediate nodes that are part of another road
                         * because such a node could have already been registered as part
                         * of the way we are currently looking at, we may add edges that already
                         * exist. This problem will be resolved further, while building the
                         * new adj list (the one without paths that overflow bounding box)
                         */
                        if (g.getAdjList().containsKey(curNode)) {
                            if (curNode != lastNode) foundIntermediateNodes = true;
                            g.addEdge(new Way(startNode, curNode, timeCost, timeCost * maxSpeed,
                                    roadType == null ? "" : roadType));
                            g.addEdge(new Way(curNode, startNode,
                                    bothSides ? timeCost : Double.MAX_VALUE,
                                    bothSides ? timeCost * maxSpeed : Double.MAX_VALUE,
                                    roadType == null ? "" : roadType));
                            startNode = curNode;
                            timeCost = 0.0;
                        }
                    }

                    if (foundIntermediateNodes) {
                        g.getAdjList().get(firstNode).removeIf(w -> w.getToId() == lastNode);
                        g.getAdjList().get(lastNode).removeIf(w -> w.getToId() == firstNode);
                    }
                }
            }


            sendMessageToController("resolving missing nodes", Log.LogLevels.INFO);

            HashMap<Long, List<Way>> curAdjList = g.getAdjList();
            HashMap<Long, List<Way>> newAdjList = new HashMap<>();

            // THIS STEP IS VERY IMPORTANT BECAUSE EACH NODE IN THE WAYS MUST BE KNOWN (LAT & LON)
            // reconstruct the new cleaned adjList with only the ids whose nodes
            // can be retrieved
            int nbEdges = 0;
            for (long id : curAdjList.keySet()) {
                if (g.getNodes().containsKey(id)) {
                    newAdjList.put(id, new LinkedList<>());
                    for (int i = 0; i < curAdjList.get(id).size(); ++i) {
                        Way w = curAdjList.get(id).get(i);
                        long id2 = w.getToId();
                        if (g.getNodes().containsKey(id2)) {
                            // if the edge does not still exist TODO: know why edges are added multiple times
                            boolean edgeStillExists = false;
                            for(Way w2 : newAdjList.get(id)) {
                                // TODO: add a static method in Way class to compare ways
                                if(w2.getToId() == id2) edgeStillExists = true;
                            }
                            if(!edgeStillExists) {
                                nbEdges++;
                                newAdjList.get(id).add(w);
                            }
                        }
                    }
                }
            }
            g.setAdjList(newAdjList);
            int finalNbEdges = nbEdges;

            /* for debug purpose
            for (long id : newAdjList.keySet()) {
                if(newAdjList.get(id).size() > 2) {
                    System.out.println("[" + id + "," + g.getNodes().get(id).getLat() + "," + g.getNodes().get(id).getLon() + "],");
                }
            }
            */
            sendMessageToController("parsing done. " + g.getAdjList().size() + " nodes, " + finalNbEdges + " edges.",
                    Log.LogLevels.INFO);

            return g;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw e;
        }
    }

    private boolean isHighWay(Element way) {
        NodeList tags = way.getElementsByTagName("tag");
        for (int i = 0; i < tags.getLength(); ++i) {
            Node node = tags.item(i);
            if (((Element) node).getAttribute("k").equals("highway"))
                return true;
        }
        return false;
    }

    private String getRoadType(Element way) {
        NodeList tags = way.getElementsByTagName("tag");
        for (int i = 0; i < tags.getLength(); ++i) {
            Node node = tags.item(i);
            if (((Element) node).getAttribute("k").equals("highway")) {
                return ((Element) node).getAttribute("v");
            }
        }
        return "";
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

    private boolean bothSideTraversable(Element way) {
        NodeList tags = way.getElementsByTagName("tag");
        for (int k = 1; k < tags.getLength(); ++k) {
            Node child = tags.item(k);
            Element element2 = (Element) child;
            String key = element2.getAttribute("k");
            if(key.equals("oneway")) {
                String onewayStr = element2.getAttribute("v");
                return onewayStr.equals("no");
            }
        }
        return true;
    }

    private double getTimeCost(HashMap<Long, heig.osmparser.model.Node> usedNodes,
                           double maxSpeed, double timeCost, long cur, long next) {
        double lat1 = usedNodes.get(cur).getLat();
        double lon1 = usedNodes.get(cur).getLon();
        double lat2 = usedNodes.get(next).getLat();
        double lon2 = usedNodes.get(next).getLon();
        timeCost += Maths.distanceNodes(new heig.osmparser.model.Node(0, lat1, lon1, 0),
                new heig.osmparser.model.Node(0, lat2, lon2, 0)) / maxSpeed;
        return timeCost;
    }

    private boolean isInteger(String str) {
        for(int i = 0; i < str.length(); ++i) {
            if(!Character.isDigit(str.charAt(i)))
                return false;
        }
        return true;
    }
}