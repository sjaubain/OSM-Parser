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
import java.util.logging.Logger;

public class Parser {

    /**
     * FOR THE WAYS : IMPORTANT : --un allows to retrieve only nodes that belongs to ways !!!!
     * osmosis --read-pbf switzerland-latest.osm.pbf --bounding-box top=46.8773 left=6.4021 bottom=46.6723 right=6.7207 --tf accept-ways highway=primary,secondary --tf accept-nodes --tf reject-relations --un --wx ways.osm
     * FOR THE CITIES
     * osmosis --read-pbf switzerland-latest.osm.pbf --bounding-box top=46.8773 left=6.4021 bottom=46.6723 right=6.7207 --tf reject-ways --tf accept-nodes place=town,village,city --tf reject-relations --wx cities.osm
     *
     * GET NODE BY ID WITH OVERPASS API
     * https://overpass-api.de/api/interpreter?data=[out:json];%20(%20node(172282);%20);%20(._;%3E;);%20out;
     */
    // typical osmosis usage example
    // osmosis
    // --bounding-box top=46.7961 left=6.617521 bottom=46.7687 right=6.6550
    // --read-pbf switzerland-latest.osm.pbf
    // --tf accept-ways highway=primary
    // --tf accept-nodes place=city,town,village
    // --tf reject-relations
    // --wx cities.osm
    public final static Logger LOG = Logger.getLogger(Parser.class.getName());

    public void addCities(Graph g, String filename) {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(filename));
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("node");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    //TODO add all tags from the node as a list of Strings (can extract whatever we want furthermore !!!!!)
                    //TODO add additive information depending on context (city name, population,...)
                    long n = Long.parseLong(element.getAttribute("id"));
                    double lat = Double.parseDouble(element.getAttribute("lat"));
                    double lon = Double.parseDouble(element.getAttribute("lon"));
                    int pop = 0;

                    NodeList children = element.getElementsByTagName("tag");
                    for(int j = 0; j < children.getLength(); ++j) {
                        Node child = children.item(j);
                        if (child.getNodeType() == Node.ELEMENT_NODE) {
                            Element element1 = (Element) child;
                            if(element1.getAttribute("k").equals("population")) {
                                String val = element1.getAttribute("v");
                                boolean validPopFormat = !val.isEmpty();
                                for(int k = 0; k < val.length(); ++k) {
                                    if(!Character.isDigit(val.charAt(k))) {
                                        validPopFormat = false; break;
                                    }
                                }
                                if(validPopFormat) {
                                    pop = Integer.parseInt(val);
                                    if(pop > g.getMaxPopulation()) {
                                        g.setMaxPopulation(pop);
                                    }
                                }
                            }
                        }
                    }
                    g.addCity(new heig.osmparser.model.Node(n, lat, lon, pop));
                }
            }
        }  catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public Graph toGraph(String filename) {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            Graph g = new Graph();

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(filename));
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("bounds");
            Element bnds = (Element) nodes.item(0);
            g.setBounds(new double[]{
                    Double.parseDouble(bnds.getAttribute("minlon")),
                    Double.parseDouble(bnds.getAttribute("maxlat")),
                    Double.parseDouble(bnds.getAttribute("maxlon")),
                    Double.parseDouble(bnds.getAttribute("minlat"))
            });

            // connect all nodes with ways of type route
            HashMap<String, Boolean> existingNodes = new HashMap<>();
            nodes = doc.getElementsByTagName("way");
            //TODO manage exeptions
            for(int i = 0; i < nodes.getLength(); ++i) {
                //TODO filter elements that contain a certain tag by factorizing in a function
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    // the way element must contain the attribute ("k", "highway") to be an edge
                    NodeList children = element.getElementsByTagName("tag");
                    for(int j = 0; j < children.getLength(); ++j) {
                        Node child = children.item(j);
                        if (child.getNodeType() == Node.ELEMENT_NODE) {
                            Element element1 = (Element) child;
                            if(element1.getAttribute("k").equals("highway")) {
                                children = element.getElementsByTagName("nd");
                                // first node of the road
                                long n1 = Long.parseLong(((Element)children.item(0))
                                        .getAttribute("ref"));
                                // last node of the road
                                long n2 = Long.parseLong(((Element)children.item(children.getLength() - 1))
                                        .getAttribute("ref"));
                                existingNodes.put(String.valueOf(n1), true);
                                existingNodes.put(String.valueOf(n2), true);
/*
                                g.addNode(OverpassAPIClient.getNodeById(n1));
                                g.addNode(OverpassAPIClient.getNodeById(n2));
*/
                                g.addEdge(n1, n2, false);
                                g.addEdge(n2, n1, false);
                                break;
                            }
                        }
                    }
                }
            }

            // retrieve all nodes
            nodes = doc.getElementsByTagName("node");
            int len = nodes.getLength();
            for (int i = 0; i < len; i++) {

                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    //TODO add additive information depending on context (city name, population,...)
                    String id = element.getAttribute("id");
                    if(existingNodes.containsKey(id)) {
                        long n = Long.parseLong(element.getAttribute("id"));
                        double lat = Double.parseDouble(element.getAttribute("lat"));
                        double lon = Double.parseDouble(element.getAttribute("lon"));
                        g.addNode(new heig.osmparser.model.Node(n, lat, lon, 0));
                    }
                }
            }
            return g;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}

