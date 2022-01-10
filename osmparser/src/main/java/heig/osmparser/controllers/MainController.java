package heig.osmparser.controllers;

import heig.osmparser.Main;
import heig.osmparser.Shell;
import heig.osmparser.configs.Config;
import heig.osmparser.model.Graph;
import heig.osmparser.model.Node;
import heig.osmparser.model.Way;
import heig.osmparser.utils.converters.CSVConverter;
import heig.osmparser.utils.logs.Log;
import heig.osmparser.utils.maths.Maths;
import heig.osmparser.utils.parsers.GraphParser;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
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
    @FXML
    private MenuItem mnitmExportCSV, mnitmEdit, mnitmHelp;

    private final double  SCREEN_WIDTH = 892, SCREEN_HEIGHT = 473;
    private Graph g;
    private Shell shell;
    private GraphParser parser;
    private enum ACTION_PERFORM {DIJKSTRA, AREA_SELECTION};
    private ACTION_PERFORM current_action = ACTION_PERFORM.AREA_SELECTION;
    private boolean firstNodeChoosen = true;
    private HashMap<Long, Circle> nodesCircles;
    private Group mapShapesGroup = new Group();
    private final double zoomFactor = 1.6;
    private Group shortestPathLines;
    private Background background;
    private boolean backgroundDisplayed = false;
    private Stage stageBoundsChooser = null;

    // todo : store in another place
    private final static String API_KEY =
            "sk.eyJ1Ijoic2ltb25qb2JpbiIsImEiOiJja3hyYzQzbW0" +
            "wZGZzMnBwYzZjZTY4YnNvIn0.on-zVsaICGHIiDOzrm8awQ";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // just to fire scroll events, otherwise it is hidden by other nodes...
        logsPane.toFront(); logsPane.setPickOnBounds(false);
        mapPane.toBack();

        nodesCircles = new HashMap<>();
        g = new Graph();
        shell = new Shell(this);
        parser = new GraphParser(this);

        actionAreaSelection.setOnAction(e -> {
            current_action = ACTION_PERFORM.AREA_SELECTION;
        });
        actionDijkstra.setOnAction(e -> {
            current_action = ACTION_PERFORM.DIJKSTRA;
        });

        mapPane.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
           if(event.getButton().equals(MouseButton.PRIMARY) && current_action.equals(ACTION_PERFORM.DIJKSTRA)) {
                double[] coords = getLatLonFromMousePos(event.getX(), event.getY(), g.getBounds(), mapPane);
                log("coords : " + coords[0] + ", " + coords[1], Log.LogLevels.INFO);
                if(firstNodeChoosen) {
                    Node from = g.getClosestNodeFromGPSCoords(coords[0], coords[1]);
                    firstNodeChoosen = false;
                    g.dijkstra(from.getId());
                } else {
                    Node to = g.getClosestNodeFromGPSCoords(coords[0], coords[1]);
                    firstNodeChoosen = true;
                    drawPath(g.getShortestPath(to));
                    log("time cost : " + 1 / 0.7 * g.getLambda().get(to.getId()) / 60d + " minutes", Log.LogLevels.INFO);
                }
           }
        });

        mnitmExportCSV.setOnAction(event -> {
            log("exporting CSV files to output/ directory", Log.LogLevels.INFO);
            CSVConverter.graphToCSV(g);
        });

        // Create operators for zoom and drag on map
        AnimatedZoomOperator zoomOperator = new AnimatedZoomOperator(mapPane, zoomFactor);
        AnimatedDragOperator dragOperator = new AnimatedDragOperator(mapPane);
    }

    public void chooseBounds() {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("bounds.fxml"));
        Scene scene = null;
        try {
            stageBoundsChooser = new Stage();
            scene = new Scene(fxmlLoader.load(), 400, 400);
            stageBoundsChooser.setTitle("Choose Bounds");
            stageBoundsChooser.setScene(scene);
            stageBoundsChooser.show();
            ((BoundsController)(fxmlLoader.getController())).setMainController(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] generateOsmosisCommands() {

        // looking for .pbf file
        try {
            File folder = new File("./input");
            File[] listOfFiles = folder.listFiles();
            String pbfFile = "./input/";
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    if (listOfFiles[i].getName().contains(".pbf")) {
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
            for (Object choice : choices) {
                RadioButton rb = ((RadioButton) choice);
                if (rb.isSelected()) {
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

            // if user didn't give args at all
            if(nbPlace == 0 && nbRoad == 0) {
                log("You did not provide any parameter to parse", Log.LogLevels.WARNING);
                return new String[]{"", ""};
            }
            commandWays += (nbRoad == 0 ? "" : roads) + " --tf reject-relations --un --wx ./input/ways.osm";
            commandCities += (nbPlace == 0 ? "" : places) + " --tf reject-ways --tf reject-relation --wx ./input/cities.osm";
            return new String[]{commandWays, commandCities};
        } catch(Exception e) {
            log("you seem not to have an input file with a .pbf file (put it on the root folder of the project)",
                    Log.LogLevels.WARNING);
            return new String[]{"", ""};
        }
    }

    void displayBounds(double[] bounds) {
        if(bounds.length == 4) {
            minlon.setText(String.valueOf(bounds[0]));
            maxlat.setText(String.valueOf(bounds[1]));
            maxlon.setText(String.valueOf(bounds[2]));
            minlat.setText(String.valueOf(bounds[3]));
        }
    }

    double[] getLatLonFromMousePos(double x, double y, double[] bounds, Pane pane) {

        double ratioX = x / pane.getPrefWidth();
        double ratioY = y / pane.getPrefHeight();

        double yTop = 1;
        double yBottom = -1;
        double xLeft = -1;
        double xRight = 1;

        double boundsMapScaleY = Maths.mapProjection(bounds[1], 0)[1] - Maths.mapProjection(bounds[3], 0)[1];
        double boundsMapOffY = Maths.mapProjection(Maths.MAX_LAT, 0)[1] - Maths.mapProjection(bounds[1], 0)[1];
        ratioY = (ratioY * boundsMapScaleY + boundsMapOffY) / Maths.WORLD_MAP_SCALE;

        double boundsMapScaleX = Maths.mapProjection(0, bounds[2])[0] - Maths.mapProjection(0 , bounds[0])[0];
        double boundsMapOffX = Maths.mapProjection(0, bounds[0])[0] - Maths.mapProjection(0, -Maths.MAX_LON)[0];
        ratioX = (ratioX * boundsMapScaleX + boundsMapOffX) / Maths.WORLD_MAP_SCALE;

        double interpolatedX = xLeft + ratioX * (xRight - xLeft);
        double interpolatedY = yTop + ratioY * (yBottom - yTop);

        double[] latLon = Maths.mapProjectionInv(interpolatedX, interpolatedY);

        return new double[]{Maths.round(latLon[0], 4),
                            Maths.round(latLon[1], 4)};
    }

    public void loadGraph() {
        try {
            log("starting to parse file, please wait...", Log.LogLevels.INFO);
            new Thread(() -> {
                try {
                    g = parser.toGraph("./input/ways.osm");
                    //parser.addCities(g, "./input/cities.osm");
                } catch (IOException | SAXException | ParserConfigurationException e) {
                    log(e.toString(), Log.LogLevels.ERROR);
                }
                Platform.runLater(() -> {
                    drawGraph();
                    displayBounds(g.getBounds());
                });
            }).start();
        } catch(Exception e) {
            log(e.toString(), Log.LogLevels.ERROR);
        }
    }

    public void importData() {
        log("starting filtering data with osmosis", Log.LogLevels.INFO);
        String[] commands = generateOsmosisCommands();
        for(String command : commands) {
            shell.exec(command);
        }
    }

    public void drawPath(List<Node> nodes) {

        //default metric for MN03 is centimeter
        double[] bounds = g.getBounds();
        double[] shape1 = Maths.mapProjection(bounds[1], bounds[0]); // upper left corner
        double[] shape2 = Maths.mapProjection(bounds[3], bounds[2]); // bottom right corner
        double[] mapShape = {shape2[0] - shape1[0], -1 * (shape2[1] - shape1[1])}; // times -1 because y axis is in reverse side (downside)

        if(shortestPathLines != null) mapPane.getChildren().remove(shortestPathLines);
        shortestPathLines = new Group();
        for(int i = 0; i < nodes.size() - 1; ++i) {

            Node n1 = nodes.get(i), n2 = nodes.get(i + 1);
            double[] nodeShape1 = Maths.mapProjection(n1.getLat(), n1.getLon());
            double startX = (nodeShape1[0] - shape1[0]) * mapPane.getPrefWidth() / mapShape[0];
            double startY = -1 * (nodeShape1[1] - shape1[1]) * mapPane.getPrefHeight() / mapShape[1];
            double[] nodeShape2 = Maths.mapProjection(n2.getLat(), n2.getLon());
            double endX = (nodeShape2[0] - shape1[0]) * mapPane.getPrefWidth() / mapShape[0];
            double endY = -1 * (nodeShape2[1] - shape1[1]) * mapPane.getPrefHeight() / mapShape[1];

            Line line = new Line(startX, startY, endX, endY);
            line.setStroke(Color.BLUE);
            line.setStrokeWidth(0.65);
            shortestPathLines.getChildren().add(line);
        }
        mapPane.getChildren().add(shortestPathLines);
    }

    private void resetBackground() {
        mapPane.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 0), CornerRadii.EMPTY, Insets.EMPTY)));
    }

    public void drawGraph() {

        // reset the map
        mapShapesGroup.getChildren().clear();
        mapPane.getChildren().clear(); resetBackground(); mapShapesGroup.setVisible(true);
        mapPane.setLayoutX(0); mapPane.setLayoutY(0);

        HashMap<Long, Node> cities = g.getCities();

        double[] bounds = g.getBounds();
        // upper left corner coordinates
        double[] shape1 = Maths.mapProjection(bounds[1], bounds[0]);
        // bottom right corner coordinates
        double[] shape2 = Maths.mapProjection(bounds[3], bounds[2]);

        // multiplied by -1 because y-axis is in reverse side (downside)
        double[] mapShape = {shape2[0] - shape1[0], -1 * (shape2[1] - shape1[1])};
        double ratioHoverW = mapShape[1] / mapShape[0];
        // auto scale map within screen (depending on zoom level, see : https://wiki.openstreetmap.org/wiki/Zoom_levels)
        double defaultWidth = 400; double defaultHeight = 400;
        if(ratioHoverW > 1) {
            mapPane.setTranslateX((SCREEN_WIDTH - defaultWidth / ratioHoverW) / 2);
            mapPane.setTranslateY((SCREEN_HEIGHT - defaultWidth) / 2);
            mapPane.setPrefHeight(defaultWidth); mapPane.setPrefWidth(defaultWidth / ratioHoverW);
            defaultWidth = defaultWidth / ratioHoverW;
        } else {
            mapPane.setTranslateX((SCREEN_WIDTH - defaultWidth) / 2);
            mapPane.setTranslateY((SCREEN_HEIGHT - defaultWidth * ratioHoverW) / 2);
            mapPane.setPrefHeight(defaultWidth * ratioHoverW); mapPane.setPrefWidth(defaultWidth);
            defaultHeight = defaultWidth * ratioHoverW;
        }
        double factorX = mapPane.getPrefWidth();
        double factorY = mapPane.getPrefHeight();

        if(defaultWidth > defaultHeight) {
            defaultWidth = 1280; defaultHeight = ratioHoverW * 1280;
        } else {
            defaultHeight = 1280; defaultWidth = 1280 / ratioHoverW;
        }

        String url = "https://api.mapbox.com/styles/v1/mapbox/streets-v11/static/"
                + "[" + bounds[0] + "," + bounds[3] + "," + bounds[2] + "," + bounds[1] + "]/"
                + (int) defaultWidth + "x" + (int) defaultHeight
                + "@2x?access_token=" + API_KEY;
        Image image = new Image(url);
        // create a background image
        BackgroundImage backgroundimage = new BackgroundImage(image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(1.0, 1.0, true, true, false, false));
        // create Background
        background = new Background(backgroundimage);

        // draw roads
        double[] nodeShape;
        for(Long i : g.getAdjList().keySet()) {
            Node n1 = g.getNodes().get(i);
            if(n1 != null) {
                nodeShape = Maths.mapProjection(n1.getLat(), n1.getLon());
                double startX = (nodeShape[0] - shape1[0]) * factorX / mapShape[0];
                double startY = -1 * (nodeShape[1] - shape1[1]) * factorY / mapShape[1];

                for (Way w : (g.getAdjList().get(i))) {
                    Node n2 = g.getNodes().get(w.getToId());
                    String roadType = w.getRoadType();

                    if (n2 != null) {
                        nodeShape = Maths.mapProjection(n2.getLat(), n2.getLon());
                        double endX = (nodeShape[0] - shape1[0]) * factorX / mapShape[0];
                        double endY = -1 * (nodeShape[1] - shape1[1]) * factorY / mapShape[1];

                        Line line = new Line(startX, startY, endX, endY);
                        line.setStroke(Config.roadTypeColor.get(roadType));
                        line.setStrokeWidth(Config.roadTypeStrokeWidth.get(roadType));
                        mapShapesGroup.getChildren().add(line);
                    }
                }
            }
        }
        mapPane.getChildren().add(mapShapesGroup);
    }

    public void showBackground() {
        if(mapPane.getPrefWidth() != 0) {
            if(!backgroundDisplayed) {
                backgroundDisplayed = true;
                mapPane.setBackground(background);
                mapShapesGroup.setVisible(false);
            } else {
                backgroundDisplayed = false;
                resetBackground();
                mapShapesGroup.setVisible(true);
            }
        }
    }

    public void log(String msg, Log.LogLevels logLevel) {

        TextField newText = new TextField(msg);
        newText.setText(msg); newText.setEditable(false);
        if(logLevel.equals(Log.LogLevels.INFO))
            newText.setStyle("-fx-text-fill: #1a1919");
        else if(logLevel.equals(Log.LogLevels.WARNING))
            newText.setStyle("-fx-text-fill: darkorange");
        else if(logLevel.equals(Log.LogLevels.ERROR))
            newText.setStyle("-fx-text-fill: red");
        else
            newText.setStyle("-fx-text-fill: darkgreen");

        logsListView.getItems().add(logsListView.getItems().size(), newText);
        logsListView.scrollTo(logsListView.getItems().size() - 1);
    }
}