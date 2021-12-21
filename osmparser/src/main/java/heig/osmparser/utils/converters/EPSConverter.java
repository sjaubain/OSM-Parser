package heig.osmparser.utils.converters;

import heig.osmparser.model.Graph;
import heig.osmparser.model.Node;
import heig.osmparser.utils.maths.Maths;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public abstract class EPSConverter {

    public static final String DEFAULT_OUTPUT_FILENAME = "./output/cities.ps";

    public static void graphToEPS(Graph g, String outputFilename) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename));
            writer.write("%!PS-Adobe-2.0 EPSF-2.0\n" +
                    "%%BoundingBox: 0 0 600 600\n" +
                    "/l {lineto} def\n" +
                    "/n {newpath} def\n" +
                    "/s {stroke} def\n" +
                    "/m {moveto} def\n" +
                    "/dessin_route [[150 1 1 18][150 1 1 18][150 1 1 18][150 1 1 18][4000 1 .8 .5][150 1 1 18][150 1 1 18][150 1 1 18][150 1 1 18][150 1 1 18][150 1 1 18][150 1 1 18]] def\n" +
                    "/c {dessin_route exch get aload pop setrgbcolor setlinewidth} def\n" +
                    "1 setlinecap 1 setlinejoin\n" +
                    "6.66667e-05 6.66667e-05 scale\n" +
                    "-49300000 -13600000 translate\n");
            for(long i : g.getAdjList().keySet()) {
                Node n1 = g.getNodes().get(i);
                if(g.containsNode(i)) {
                    int[] coords1 = Maths.latsToMN03(n1.getLat(), n1.getLon());
                    String coordsStr1 = String.valueOf(coords1[0]) + " " + String.valueOf(coords1[1]);
                    for (long j : g.getAdjList().get(i)) {
                        String line = "4 c\nn " + coordsStr1;
                        Node n2 = g.getNodes().get(j);
                        if (g.containsNode(j)) {
                            int[] coords2 = Maths.latsToMN03(n2.getLat(), n2.getLon());
                            String coordsStr2 = String.valueOf(coords2[0]) + " " + String.valueOf(coords2[1]);
                            line += " m " + coordsStr2 + " l s\n";
                            writer.write(line);
                        }
                    }
                }
            }
            writer.write("showpage");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
