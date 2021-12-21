package heig.osmparser.controllers;

import heig.osmparser.Shell;
import heig.osmparser.drawing.Box;
import heig.osmparser.model.Graph;
import heig.osmparser.model.Node;
import heig.osmparser.utils.logs.Log;
import heig.osmparser.utils.maths.Maths;
import heig.osmparser.utils.parsers.Parser;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private Pane logsPane;
    @FXML
    private Pane mapPane; // w : 890, h : 496
    @FXML
    private ScrollPane logsScrollPane;
    @FXML
    private ListView logsListView;

    private Graph g;
    private Box selection;
    private Task<Integer> task;
    private Shell shell;

    @Override
    public void initialize(URL url2, ResourceBundle resourceBundle) {

        // just to fire scroll events, otherwise it is hidden by other nodes...
        logsListView.toFront();

        shell = new Shell(this);

        task = new Task() {
            @Override
            protected Integer call() throws Exception {
                Parser parser = new Parser();
                g = parser.toGraph("./input/ways.osm");
                parser.addCities(g, "./input/cities.osm");
                //EPSConverter.graphToEPS(g, "./output/drawing.ps");
                return 0;
            }
        };

        task.setOnSucceeded(e -> {
            // we're on the JavaFX application thread here
            Integer result = task.getValue();
            log("parsing done. drawing graph", Log.LogLevels.INFO);
            drawGraph();
        });

        task.setOnFailed(e -> {
            log("parsing error.", Log.LogLevels.ERROR);
        });

        mapPane.setOnMousePressed(event -> {
            selection = new Box(mapPane, event.getSceneX(), event.getSceneY() - 25);
        });
        mapPane.setOnMouseDragged(event -> {
            if (event.getSceneY() < mapPane.getPrefHeight()) {
                selection.render(event.getSceneX(), event.getSceneY() - 25);
            }
        });
        mapPane.setOnMouseReleased(event -> {
            mapPane.getChildren().remove(selection.getRectangle());
        });
    }

    public void load() {
        /*
        if(!task.isRunning()) {
            log("starting reading data from file, please wait...", Log.LogLevels.INFO);
            new Thread(task).start(); // alternatively use ExecutorService
        }
        */
        shell.exec("osmosis --caca");
    }

    public void drawGraph() {

        HashMap<Long, Node> cities = g.getCities();

        //default metric for MN03 is centimeter
        double[] bounds = g.getBounds();
        int[] shape1 = Maths.latsToMN03(bounds[2], bounds[3]);
        int[] shape2 = Maths.latsToMN03(bounds[0], bounds[1]);
        int[] mapShape = {shape1[0] - shape2[0], shape1[1] - shape2[1]};

        // draw roads
        for(Long i : g.getAdjList().keySet()) {
            Node n1 = g.getNodes().get(i);

            int[] nodeShape1 = Maths.latsToMN03(n1.getLat(), n1.getLon());
            double startX = (nodeShape1[0] - shape2[0]) * mapPane.getPrefWidth() / (double) mapShape[0];
            double startY = mapPane.getPrefHeight() - (nodeShape1[1] - shape2[1]) * mapPane.getPrefHeight() / (double) mapShape[1];
            for (Long j : g.getAdjList().get(i)) {
                Node n2 = g.getNodes().get(j);

                int[] nodeShape2 = Maths.latsToMN03(n2.getLat(), n2.getLon());
                double endX = (nodeShape2[0] - shape2[0]) * mapPane.getPrefWidth() / (double) mapShape[0];
                double endY = mapPane.getPrefHeight() - (nodeShape2[1] - shape2[1]) * mapPane.getPrefHeight() / (double) mapShape[1];

                Line line = new Line(startX, startY, endX, endY);
                line.setStroke(Color.LIGHTYELLOW);
                line.setStrokeWidth(0.3);
                mapPane.getChildren().add(line);
            }
        }

        // draw cities
        int maxPop = g.getMaxPopulation();
        double maxRadius = Math.sqrt(300);

        int[] c1 = {255, 255, 0};
        int[] c2 = {255, 0, 0};

        for(Node n : cities.values()) {

            double radius = Math.sqrt(300 * n.getPopulation() / (double)maxPop);
            double scale =  (1 - (radius / maxRadius));

            Platform.runLater(new Runnable() {
                @Override public void run() {
                    // color gradient between yellow and red
                    Color c = Color.rgb(255, (int)((c1[1] - c2[1]) * scale),0);
                    Circle circle = new Circle(radius, c);
                    int[] nodeShape = Maths.latsToMN03(n.getLat(), n.getLon());

                    circle.setLayoutY(mapPane.getPrefHeight() - (nodeShape[1] - shape2[1]) * mapPane.getPrefHeight() / (double)mapShape[1]);
                    circle.setLayoutX((nodeShape[0] - shape2[0]) * mapPane.getPrefWidth() / (double)mapShape[0]);
                    mapPane.getChildren().add(circle);
                }
            });
        }
    }

    public void log(String msg, Log.LogLevels logLevel) {

        Text newText = new Text("> " + msg);
        if(logLevel.equals(Log.LogLevels.INFO))
            newText.setFill(Color.DARKGREEN);
        else
            newText.setFill(Color.RED);

        logsListView.getItems().add(logsListView.getItems().size(), newText);
        logsListView.scrollTo(logsListView.getItems().size() - 1);
    }
}