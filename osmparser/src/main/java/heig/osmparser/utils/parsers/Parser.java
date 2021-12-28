package heig.osmparser.utils.parsers;

import heig.osmparser.model.Graph;
import heig.osmparser.model.Way;
import heig.osmparser.net.OverpassAPIClient;
import heig.osmparser.utils.maths.Maths;
import javafx.util.Pair;
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
                        boolean validPopFormat = !val.isEmpty();
                        for (int k = 0; k < val.length(); ++k) {
                            if (!Character.isDigit(val.charAt(k))) {
                                validPopFormat = false;
                                break;
                            }
                        }
                        if (validPopFormat) {
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
        if(children != null) {
            for (int i = 0; i < children.getLength(); ++i) {
                Node node = children.item(i);
                if (((Element) node).getAttribute("k").equals("highway")) {
                    return ((Element) node).getAttribute("v");
                }
            }
        }
        return null;
    }

    public Graph toGraph(String filename) {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            Graph g = new Graph();

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(filename));
            doc.getDocumentElement().normalize();

            LOG.log(Level.INFO, "parsing bounds");

            NodeList nodes = doc.getElementsByTagName("bounds");
            Element bnds = (Element) nodes.item(0);
            g.setBounds(new double[]{
                    Double.parseDouble(bnds.getAttribute("minlon")),
                    Double.parseDouble(bnds.getAttribute("maxlat")),
                    Double.parseDouble(bnds.getAttribute("maxlon")),
                    Double.parseDouble(bnds.getAttribute("minlat"))
            });

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
                Element element = (Element) node;
                NodeList children = element.getElementsByTagName("nd");
                String roadType = getRoadType(node);

                // first node of the road
                long n1 = Long.parseLong(((Element) children.item(0))
                        .getAttribute("ref"));
                // last node of the road
                long n2 = Long.parseLong(((Element) children.item(children.getLength() - 1))
                        .getAttribute("ref"));
                registeredNodes.put(String.valueOf(n1), true);
                registeredNodes.put(String.valueOf(n2), true);

                //TODO : compute time cost depending on tag "max_speed"
                double cost = 0;
                for(int j = 0; j < children.getLength() - 1; ++j) {
                    long cur = Long.parseLong(((Element) children.item(j))
                            .getAttribute("ref"));
                    long next = Long.parseLong(((Element) children.item(j + 1))
                            .getAttribute("ref"));

                    if(usedNodes.get(cur) != null && usedNodes.get(next) != null) {
                        double lat1 = usedNodes.get(cur).getLat();
                        double lon1 = usedNodes.get(cur).getLon();
                        double lat2 = usedNodes.get(next).getLat();
                        double lon2 = usedNodes.get(next).getLon();
                        cost += Maths.distanceNodes(new heig.osmparser.model.Node(0, lat1, lon1, 0),
                                new heig.osmparser.model.Node(0, lat2, lon2, 0));
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

            // TODO : a lot of factorization and exception handling
            // first retrieve again all the ways
            nodes = doc.getElementsByTagName("way");
            for (int i = 0; i < nodes.getLength(); ++i) {

                Node node = nodes.item(i);
                Element element = (Element) node;
                NodeList children = element.getElementsByTagName("nd");
                String roadType = getRoadType(node);

                long firstNode = Long.parseLong(((Element) children.item(0)).getAttribute("ref"));
                //TODO : compute time cost depending on tag "max_speed"
                double cost = 0;

                for (int k = 1; k < children.getLength(); ++k) {
                    Node child = children.item(k);
                    Element element2 = (Element) child;
                    long curNode = Long.parseLong(element2.getAttribute("ref"));

                    if(usedNodes.get(firstNode) != null && usedNodes.get(curNode) != null) {
                        double lat1 = usedNodes.get(firstNode).getLat();
                        double lon1 = usedNodes.get(firstNode).getLon();
                        double lat2 = usedNodes.get(curNode).getLat();
                        double lon2 = usedNodes.get(curNode).getLon();
                        cost += Maths.distanceNodes(new heig.osmparser.model.Node(0, lat1, lon1, 0),
                                new heig.osmparser.model.Node(0, lat2, lon2, 0));
                    }

                    if (g.getAdjList().containsKey(curNode)) {
                        g.addEdge(firstNode, curNode, cost, roadType == null ? "" : roadType);
                        g.addEdge(curNode, firstNode, cost, roadType == null ? "" : roadType);
                        firstNode = curNode; cost = 0.0;
                    }
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
            e.printStackTrace();
        }

        return null;
    }
}

