package heig.osmparser.controllers;

import heig.osmparser.Shell;
import heig.osmparser.model.Graph;
import heig.osmparser.model.Node;
import heig.osmparser.utils.logs.Log;
import heig.osmparser.utils.maths.Maths;
import heig.osmparser.utils.parsers.Parser;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Pair;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private AnchorPane leftPane;
    @FXML
    private Pane logsPane;
    @FXML
    private Pane mapPane; // w : 890, h : 496
    @FXML
    private ListView logsListView;
    @FXML
    private TextField minlat, minlon, maxlat, maxlon;
    @FXML
    private RadioButton actionAreaSelection, actionDijkstra;
    @FXML
    private VBox importChoices;

    private final double  SCREEN_WIDTH = 892, SCREEN_HEIGHT = 473;
    private Graph g;
    private Box selection;
    private Task<Integer> task;
    private Shell shell;
    private enum ACTION_PERFORM {DIJKSTRA, AREA_SELECTION};
    private ACTION_PERFORM current_action = ACTION_PERFORM.AREA_SELECTION;
    private boolean firstNodeChoosen = true;
    private HashMap<Long, Circle> nodesCircles;
    private Node selectedNode;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // just to fire scroll events, otherwise it is hidden by other nodes...
        logsPane.toFront(); logsPane.setPickOnBounds(false);
        actionAreaSelection.setSelected(true);
        mapPane.toBack();

        nodesCircles = new HashMap<>();
        g = new Graph();

        shell = new Shell(this);

        task = new Task() {
            @Override
            protected Integer call() throws Exception {
            try {
                Parser parser = new Parser();
                g = parser.toGraph("./input/ways.osm");
                //parser.addCities(g, "./input/cities.osm");
                //EPSConverter.graphToEPS(g, "./output/drawing.ps");
                return 0;
            } catch(Exception e) {
                log(e.getStackTrace().toString(), Log.LogLevels.ERROR);
                return -1;
            }
            }
        };

        task.setOnSucceeded(e -> {
            // we're on the JavaFX application thread here
            Integer result = task.getValue();
            log("parsing done. drawing graph", Log.LogLevels.INFO);
            drawGraph();
            displayGraphBounds();
        });

        task.setOnFailed(e -> {
            log("parsing error. " + e.toString(), Log.LogLevels.ERROR);
        });

        actionAreaSelection.setOnAction(e -> {
            current_action = ACTION_PERFORM.AREA_SELECTION;
        });
        actionDijkstra.setOnAction(e -> {
            current_action = ACTION_PERFORM.DIJKSTRA;
        });

        /*
        mapPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            //TODO this event is not triggered because it is shadowed by the scroll and drag events
            if(current_action.equals(ACTION_PERFORM.AREA_SELECTION)) {
                if(event.getButton().equals(MouseButton.PRIMARY)) {
                    selection = new Box(mapPane, event.getSceneX(), event.getSceneY() - 25);
                    double[] latLon = getLatLonFromMousePos(event.getSceneX(), event.getSceneY());
                    maxlat.setText(String.valueOf(latLon[0]));
                    minlon.setText(String.valueOf(latLon[1]));
                }
            } else if(current_action.equals(ACTION_PERFORM.DIJKSTRA)) {
                if(firstNodeChoosen) {
                    System.out.println("dijkstra");
                    if(selectedNode != null) {
                        from = selectedNode;
                        firstNodeChoosen = false;
                        g.dijkstra(from.getId());
                    }
                } else {
                    if(selectedNode != null) {
                        to = selectedNode;
                        firstNodeChoosen = true;
                        //TODO wait that dijkstra is done
                        System.out.println(g.getShortestPath(to).size());
                        selectedNode = null;
                        drawPath(g.getShortestPath(to));
                    }
                }
            }
        });
        mapPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if(current_action.equals(ACTION_PERFORM.AREA_SELECTION)) {
                if(event.getButton().equals(MouseButton.PRIMARY)) {
                    selection.render(event.getSceneX(), event.getSceneY() - 25);
                    double[] latLon = getLatLonFromMousePos(event.getSceneX(), event.getSceneY());
                    minlat.setText(String.valueOf(latLon[0]));
                    maxlon.setText(String.valueOf(latLon[1]));
                }
            }
        });
        mapPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                if (current_action.equals(ACTION_PERFORM.AREA_SELECTION)) {
                    mapPane.getChildren().remove(selection.getRectangle());
                }
            }
        });*/

        // Create operators for zoom and drag on map
        AnimatedZoomOperator zoomOperator = new AnimatedZoomOperator(mapPane);
        AnimatedDragOperator dragOperator = new AnimatedDragOperator(mapPane);
    }

    public String[] generateOsmosisCommands() {

        // looking for .pbf file
        File folder = new File("./input");
        File[] listOfFiles = folder.listFiles();
        String pbfFile = "./input/";
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                if(listOfFiles[i].getName().contains(".pbf")) {
                    pbfFile += listOfFiles[i].getName();
                }
            }
        }

        String boundingBox = " --bounding-box" +
                " top=" + maxlat.getText() +
                " left=" + minlon.getText() +
                " bottom=" + minlat.getText() +
                " right=" + maxlon.getText();
        String commandWays = "osmosis --read-pbf " + pbfFile
                // assuming user has not given bounds yet
                + (Double.parseDouble(minlon.getText()) == 0 ? "" : boundingBox);
        String commandCities = "osmosis --read-pbf " + pbfFile
                + (Double.parseDouble(minlon.getText()) == 0 ? "" : boundingBox);
        int nbPlace = 0, nbRoad = 0;
        String places = " --tf accept-nodes place=", roads = " --tf accept-ways highway=";
        ObservableList choices = importChoices.getChildren();
        for(Object choice : choices) {
            RadioButton rb = ((RadioButton) choice);
            if(rb.isSelected()) {
                // we use 'I' as a delimiter in the fx id because special chars are not allowed
                String choiceKey = rb.getId(), choiceValue = choiceKey.substring(choiceKey.indexOf("I") + 1);
                switch (choiceKey.substring(0, choiceKey.indexOf("I"))) {
                    case "place":
                        places += (nbPlace > 0 ? "," : "") + choiceValue;
                        nbPlace++;
                        break;
                    case "highway":
                        roads += (nbRoad > 0 ? "," : "") + choiceValue;
                        nbRoad++;
                        break;
                    default:
                        break;
                }
            }
        }
        commandWays += roads + " --tf reject-relations --un --wx ./input/ways.osm";
        commandCities += places + " --tf reject-ways --tf reject-relation --wx ./input/cities.osm";
        return new String[]{commandWays, commandCities};
    }

    void displayGraphBounds() {
        double[] bounds = g.getBounds();
        minlon.setText(String.valueOf(bounds[0]));
        maxlat.setText(String.valueOf(bounds[1]));
        maxlon.setText(String.valueOf(bounds[2]));
        minlat.setText(String.valueOf(bounds[3]));
    }

    double[] getLatLonFromMousePos(double x, double y) {
        double[] latLon = new double[2];
        if(!minlat.getText().isEmpty()) { // meaning other bounds are not empty too
            // scales
            // TODO : factorize this for getting bounds
            double[] bounds = g.getBounds();
            double ratioX = x / mapPane.getPrefWidth();
            double ratioY = y / mapPane.getPrefHeight();
            double fromLat = bounds[1];
            double toLat = bounds[3];
            double fromLon = bounds[0];
            double toLon = bounds[2];
            latLon = new double[]{
                    Maths.round(fromLat + ratioY * (toLat - fromLat), 4),
                    Maths.round(fromLon + ratioX * (toLon - fromLon), 4)
            };
        }
        return latLon;
    }

    public void loadGraph() {
        if(!task.isRunning()) {
            new Thread(task).start();
        }
    }

    public void importData() {
        log("starting filtering data with osmosis", Log.LogLevels.INFO);
        String[] commands = generateOsmosisCommands();
        for(String command : commands) {
            shell.exec(command);
        }
    }

    private Group shortestPathLines;

    public void drawPath(List<Node> nodes) {

        //default metric for MN03 is centimeter
        double[] bounds = g.getBounds();
        int[] shape1 = Maths.latsToMN03(bounds[1], bounds[0]); // upper left corner
        int[] shape2 = Maths.latsToMN03(bounds[3], bounds[2]); // bottom right corner
        int[] mapShape = {shape2[0] - shape1[0], -1 * (shape2[1] - shape1[1])}; // times -1 because y axis is in reverse side (downside)

        if(shortestPathLines != null) mapPane.getChildren().remove(shortestPathLines);
        shortestPathLines = new Group();
        for(int i = 0; i < nodes.size() - 1; ++i) {

            Node n1 = nodes.get(i), n2 = nodes.get(i + 1);
            int[] nodeShape1 = Maths.latsToMN03(n1.getLat(), n1.getLon());
            double startX = (nodeShape1[0] - shape1[0]) * mapPane.getPrefWidth() / (double) mapShape[0];
            double startY = -1 * (nodeShape1[1] - shape1[1]) * mapPane.getPrefHeight() / (double) mapShape[1];
            int[] nodeShape2 = Maths.latsToMN03(n2.getLat(), n2.getLon());
            double endX = (nodeShape2[0] - shape1[0]) * mapPane.getPrefWidth() / (double) mapShape[0];
            double endY = -1 * (nodeShape2[1] - shape1[1]) * mapPane.getPrefHeight() / (double) mapShape[1];

            Line line = new Line(startX, startY, endX, endY);
            line.setStroke(Color.BLUE);
            line.setStrokeWidth(0.25);
            shortestPathLines.getChildren().add(line);
        }
        mapPane.getChildren().add(shortestPathLines);
    }

    public void drawGraph() {

        HashMap<Long, Node> cities = g.getCities();

        //default metric for MN03 is centimeter
        double[] bounds = g.getBounds();
        int[] shape1 = Maths.latsToMN03(bounds[1], bounds[0]); // upper left corner coordinates
        int[] shape2 = Maths.latsToMN03(bounds[3], bounds[2]); // bottom right corner coordinates
        // multiplied by -1 because y-axis is in reverse side (downside)
        int[] mapShape = {shape2[0] - shape1[0], -1 * (shape2[1] - shape1[1])};
        double ratioHoverW = mapShape[1] / (double) mapShape[0];
        if(ratioHoverW > 1) {
            mapPane.setTranslateX((SCREEN_WIDTH - 400 / ratioHoverW) / 2);
            mapPane.setTranslateY((SCREEN_HEIGHT - 400) / 2);
            mapPane.setPrefHeight(400); mapPane.setPrefWidth(400 / ratioHoverW);
        } else {
            mapPane.setTranslateX((SCREEN_WIDTH - 400) / 2);
            mapPane.setTranslateY((SCREEN_HEIGHT - 400 * ratioHoverW) / 2);
            mapPane.setPrefHeight(400 * ratioHoverW); mapPane.setPrefWidth(400);
        }
        double factorX = mapPane.getPrefWidth();
        double factorY = mapPane.getPrefHeight();

        // draw roads
        int[] nodeShape;
        for(Long i : g.getAdjList().keySet()) {
            Node n1 = g.getNodes().get(i);
            if(n1 != null) {
                nodeShape = Maths.latsToMN03(n1.getLat(), n1.getLon());
                double startX = (nodeShape[0] - shape1[0]) * factorX / (double) mapShape[0];
                double startY = -1 * (nodeShape[1] - shape1[1]) * factorY / (double) mapShape[1];

                for (Pair<Long, Double> p : (g.getAdjList().get(i))) {
                    Node n2 = g.getNodes().get(p.getKey());

                    if (n2 != null) {
                        nodeShape = Maths.latsToMN03(n2.getLat(), n2.getLon());
                        double endX = (nodeShape[0] - shape1[0]) * factorX / (double) mapShape[0];
                        double endY = -1 * (nodeShape[1] - shape1[1]) * factorY / (double) mapShape[1];

                        Line line = new Line(startX, startY, endX, endY);
                        line.setStroke(Color.LIGHTYELLOW);
                        line.setStrokeWidth(0.1);
                        mapPane.getChildren().add(line);
                    }
                }

                //TODO : make a function addNodeCircle
                Circle circle = new Circle(0.1, Color.WHITE);
                circle.setLayoutX((nodeShape[0] - shape1[0]) * factorX / (double) mapShape[0]);
                // times -1 because y axis is in reverse side (downside)
                circle.setLayoutY(-1 * (nodeShape[1] - shape1[1]) * factorY / (double) mapShape[1]);
                circle.setOnMouseClicked(e -> {
                    for(Circle c : nodesCircles.values()) {
                        c.setFill(Color.WHITE);
                    }
                    circle.setFill(Color.RED);
                    circle.toFront();
                    if(current_action.equals(ACTION_PERFORM.DIJKSTRA)) {
                        selectedNode = n1;
                        if(firstNodeChoosen) {
                            System.out.println("dijkstra");
                            firstNodeChoosen = false;
                            g.dijkstra(selectedNode.getId());
                        } else {
                            firstNodeChoosen = true;
                            //TODO wait that dijkstra is done
                            System.out.println(g.getShortestPath(selectedNode).size());
                            drawPath(g.getShortestPath(selectedNode));
                        }
                    }
                });

                circle.setOnMouseEntered(e -> {
                    circle.setRadius(circle.getRadius() * 3);
                });

                circle.setOnMouseExited(e -> {
                    circle.setRadius(circle.getRadius() / 3);
                });

                nodesCircles.put(n1.getId(), circle);
                mapPane.getChildren().add(circle);
            }
        }
        /*
        // draw cities
        int maxPop = g.getMaxPopulation();
        double maxRadius = Math.sqrt(300);

        int[] c1 = {255, 255, 0}; // yellow
        int[] c2 = {255, 0, 0}; // red

        for(Node n : cities.values()) {

            double radius = Math.sqrt(300 * n.getPopulation() / (double)maxPop);
            double scale =  (1 - (radius / maxRadius));

            Platform.runLater(() -> {
                // color gradient between yellow and red
                Color c = Color.rgb(255, (int)((c1[1] - c2[1]) * scale),0);
                Circle circle = new Circle(radius, c);
                int[] nodeShape = Maths.latsToMN03(n.getLat(), n.getLon());

                circle.setLayoutX((nodeShape[0] - shape1[0]) * factorX / (double)mapShape[0]);
                // times -1 because y axis is in reverse side (downside)
                circle.setLayoutY(-1 * (nodeShape[1] - shape1[1]) * factorY / (double)mapShape[1]);
                mapPane.getChildren().add(circle);
            });
        }
        */
    }

    public void log(String msg, Log.LogLevels logLevel) {

        TextField newText = new TextField(msg);
        newText.setText(msg); newText.setEditable(false);
        if(logLevel.equals(Log.LogLevels.INFO))
            newText.setStyle("-fx-text-fill: green");
        else
            newText.setStyle("-fx-text-fill: red");

        logsListView.getItems().add(logsListView.getItems().size(), newText);
        logsListView.scrollTo(logsListView.getItems().size() - 1);
    }
}