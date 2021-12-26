package heig.osmparser.utils.parsers;

import heig.osmparser.model.Graph;
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

            LOG.log(Level.INFO, "parsing ways");

            // connect all nodes with ways of type route
            HashMap<String, Boolean> registeredNodes = new HashMap<>();
            nodes = doc.getElementsByTagName("way");

            for (int i = 0; i < nodes.getLength(); ++i) {

                Node node = nodes.item(i);
                Element element = (Element) node;
                NodeList children = element.getElementsByTagName("nd");

                // first node of the road
                long n1 = Long.parseLong(((Element) children.item(0))
                        .getAttribute("ref"));
                // last node of the road
                long n2 = Long.parseLong(((Element) children.item(children.getLength() - 1))
                        .getAttribute("ref"));
                registeredNodes.put(String.valueOf(n1), true);
                registeredNodes.put(String.valueOf(n2), true);

                g.addEdge(n1, n2);
                g.addEdge(n2, n1);
            }

            LOG.log(Level.INFO, "parsing nodes");

            // retrieve all nodes
            nodes = doc.getElementsByTagName("node");
            int len = nodes.getLength();
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
                Element element = (Element) node;
                NodeList children = element.getElementsByTagName("nd");

                long firstNode = Long.parseLong(((Element) children.item(0)).getAttribute("ref"));

                for (int k = 1; k < children.getLength(); ++k) {
                    Node child = children.item(k);
                    Element element2 = (Element) child;
                    long curNode = Long.parseLong(element2.getAttribute("ref"));
                    if (g.getAdjList().containsKey(curNode)) {
                        //System.out.println(firstNode + " -> " + curNode);
                        g.addEdge(firstNode, curNode);
                        g.addEdge(curNode, firstNode);
                        firstNode = curNode;
                    }
                }
            }

            LOG.log(Level.INFO, "resolving missing nodes");

            // remove from adjList all node ids that have not been found in the file
            // because we must know the coordinates of such nodes.
            HashMap<Long, List<Long>> curAdjList = g.getAdjList();
            HashMap<Long, List<Long>> newAdjList = new HashMap<>();

            // reconstruct the new cleaned adjList with only the ids whose nodes
            // can be retrieved
            for (long id : curAdjList.keySet()) {
                if (g.getNodes().containsKey(id)) {
                    newAdjList.put(id, new LinkedList<>());
                    for (long id2 : curAdjList.get(id)) {
                        if (g.getNodes().containsKey(id2)) {
                            newAdjList.get(id).add(id2);
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

