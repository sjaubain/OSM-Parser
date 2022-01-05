package heig.osmparser.controllers;

import heig.osmparser.drawing.Box;
import heig.osmparser.utils.maths.Maths;
import heig.osmparser.utils.parsers.SVGParser;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.util.ResourceBundle;

public class BoundsController implements Initializable {

    @FXML
    private AnchorPane mainPane;

    @FXML
    private Pane mapPane2;

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

        // Area selection events
        mapPane2.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if(e.getButton().equals(MouseButton.PRIMARY)) {
                if(box != null) box.removeRectangleFromPane();
                box = new Box(mapPane2, e.getX(), e.getY());
                if(mainController != null) {
                    upperLeft = mainController.getLatLonFromMousePos(e.getX(), e.getY(), worldBounds, mapPane2);
                    mainController.displayBounds(new double[]{upperLeft[1], upperLeft[0], bottomRight[1], bottomRight[0]});
                }
            }
        });

        mapPane2.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if(e.getButton().equals(MouseButton.PRIMARY)) {
                box.render(e.getX(), e.getY());
                if(mainController != null) {
                    bottomRight = mainController.getLatLonFromMousePos(e.getX(), e.getY(), worldBounds, mapPane2);
                    mainController.displayBounds(new double[]{upperLeft[1], upperLeft[0], bottomRight[1], bottomRight[0]});
                }
            }
        });

        // Create operators for zoom and drag on map
        AnimatedZoomOperator zoomOperator = new AnimatedZoomOperator(mapPane2, zoomFactor);
        AnimatedDragOperator dragOperator = new AnimatedDragOperator(mapPane2);
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}
