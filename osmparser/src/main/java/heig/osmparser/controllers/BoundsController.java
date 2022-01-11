package heig.osmparser.controllers;

import heig.osmparser.drawing.Box;
import heig.osmparser.maths.Maths;
import heig.osmparser.parsers.SVGParser;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class BoundsController implements Initializable {

    @FXML
    private AnchorPane mainPane;

    @FXML
    private Pane mapPane2;

    @FXML
    private Button btnOk;

    private MainController mainController;
    private double zoomFactor = 1.6;
    private int mapWidth = 2000;
    private int mapHeight = 2000;
    private Box box;
    private double[] bounds;
    private double[] upperLeft, bottomRight;
    private static double OFF_Y = 80;

    // those bounds correspond to the map world.svg, which covers
    // all the world map projected with Mercator formula. We cannot
    // use 90° and -90° because it would mean that the y coordinate
    // would be infinity and -infinity respectively
    private static final double[] worldBounds = {-Maths.MAX_LON, Maths.MAX_LAT, Maths.MAX_LON, -Maths.MAX_LAT};

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {

            mapPane2.setPrefHeight(mapHeight);
            mapPane2.setPrefWidth(mapWidth);
            mapPane2.setTranslateX(-1 * (mapWidth - mainPane.getPrefWidth()) / 2d);
            mapPane2.setTranslateY(-1 * (mapHeight - mainPane.getPrefHeight()) / 2d);

            // add SVG image 'world.svg'
            SVGParser parser = new SVGParser();
            SVGPath svgPath = new SVGPath();
            // styling
            svgPath.setFill((Paint.valueOf("#e8e8e8")));
            svgPath.setStroke(Paint.valueOf("black"));
            svgPath.setStrokeWidth(0.25);

            // adding a cross in the middle of the map
            String crossLine1 = "M" + (mapWidth / 2 - 8) + " " +  (mapHeight / 2 - OFF_Y) + "L" + (mapWidth / 2 + 8) + " " +  (mapHeight / 2 - OFF_Y);
            String crossLine2 = "M" + mapWidth / 2 + " " +  (mapHeight / 2 - OFF_Y - 8) + "L" + mapWidth / 2 + " " +  (mapHeight / 2 + - OFF_Y + 8);
            String paths = parser.toString("./src/main/resources/heig/osmparser/world.svg");
            svgPath.setContent(paths + crossLine1 + crossLine2);
            Group root = new Group(svgPath);

            // it was computed in order to cover the best possible
            // the osm map square Mercator projection
            root.setTranslateY(OFF_Y);
            mapPane2.getChildren().add(root);

        } catch (Exception e) {
            e.printStackTrace();
        }

        bounds = new double[4]; upperLeft = new double[2]; bottomRight = new double[2];

        // Create operators for zoom and drag on map
        AnimatedZoomOperator zoomOperator = new AnimatedZoomOperator(mapPane2, zoomFactor);
        AnimatedDragOperator dragOperator = new AnimatedDragOperator(mapPane2);
    }

    public void closePage() {
        Stage stage = (Stage) btnOk.getScene().getWindow();
        stage.close();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        AnimatedBoxOperator boxOperator = new AnimatedBoxOperator(mainController, mapPane2, worldBounds);
    }
}
