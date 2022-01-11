package heig.osmparser.converters;

import heig.osmparser.model.Graph;
import heig.osmparser.model.Node;
import heig.osmparser.model.Way;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
public abstract class CSVConverter {

    public static final String DEFAULT_OUTPUT_FILENAME_NODES = "./output/nodes.csv";
    public static final String DEFAULT_OUTPUT_FILENAME_EDGES = "./output/edges.csv";

    public static void graphToCSV(Graph g) {
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
}
