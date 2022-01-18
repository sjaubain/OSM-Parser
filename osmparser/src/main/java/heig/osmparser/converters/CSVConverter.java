package heig.osmparser.converters;

import heig.osmparser.maths.Maths;
import heig.osmparser.model.Graph;
import heig.osmparser.model.Node;
import heig.osmparser.model.Way;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class CSVConverter {

    public static final String DEFAULT_OUTPUT_FILENAME_NODES = "./output/nodes.csv";
    public static final String DEFAULT_OUTPUT_FILENAME_EDGES = "./output/edges.csv";

    /**
     * export the full graph as a file containing all
     * nodes ids and another file containing the adjList
     * @param g
     */
    public static void rawGraphToCSV(Graph g) {
        try {
            BufferedWriter nodesWriter = new BufferedWriter(new FileWriter(DEFAULT_OUTPUT_FILENAME_NODES));
            BufferedWriter edgesWriter = new BufferedWriter(new FileWriter(DEFAULT_OUTPUT_FILENAME_EDGES));

            nodesWriter.write("node_id,lat,lon\n");
            edgesWriter.write("from_node_id,to_node_id\n");
            for(long i : g.getAdjList().keySet()) {
                Node n1 = g.getNodes().get(i);
                nodesWriter.write(n1.getId() + "," + n1.getLat() + "," + n1.getLon() + "\n");
                for (Way w: g.getAdjList().get(i)) {
                    long n2Id = w.getToId();
                    edgesWriter.write(n1.getId() + "," + n2Id + "\n");
                }
            }
            nodesWriter.close(); edgesWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * compute SP for each couple of cities and
     * export the graph considering only the cities as nodes
     * @param g
     */
    public static void flattenGraphToCSV(Graph g) {
        try {
            BufferedWriter nodesWriter = new BufferedWriter(new FileWriter(DEFAULT_OUTPUT_FILENAME_NODES));
            BufferedWriter edgesWriter = new BufferedWriter(new FileWriter(DEFAULT_OUTPUT_FILENAME_EDGES));

            nodesWriter.write("node_id,place_name,lat,lon\n");
            edgesWriter.write("from_node_id,to_node_id,time_cost\n");
            List<Long> citiesIds = new ArrayList<>(g.getCities().keySet());
            for(int i = 0; i < citiesIds.size(); ++i) {
                Node city1 = g.getCities().get(citiesIds.get(i));
                Node n1 = g.getClosestNodeFromGPSCoords(city1.getLat(), city1.getLon());
                g.dijkstra(n1.getId());
                nodesWriter.write(city1.getId() + "," + city1.getName() + "," + city1.getLat() + "," + city1.getLon() + "\n");
                for (int j = 0; j < citiesIds.size(); ++j) {
                    Node city2 = g.getCities().get(citiesIds.get(j));
                    Node n2 = g.getClosestNodeFromGPSCoords(city2.getLat(), city2.getLon());
                    double c_ij = g.getLambda().get(n2.getId());
                    edgesWriter.write(city1.getId() + "," + city2.getId() + "," + Maths.round(c_ij, 4) + "\n");
                }
            }
            nodesWriter.close(); edgesWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
