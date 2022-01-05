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
            svgPath.setFill((Paint.valueOf("#b5f6ff")));
            svgPath.setStroke(Paint.valueOf("black"));
            svgPath.setStrokeWidth(0.2);

            String paths = parser.toString("./src/main/resources/heig/osmparser/world.svg");
            svgPath.setContent(paths);
            Group root = new Group(svgPath);

            // it was computed in order to cover the best possible
            // the osm map square Mercator projection
            root.setTranslateY(80);
            mapPane2.getChildren().add(root);

        } catch (Exception e) {
            e.printStackTrace();
        }

        bounds = new double[4]; upperLeft = new double[2]; bottomRight = new double[2];

        // Area selection events
        mapPane2.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if(e.getButton().equals(MouseButton.PRIMARY)) {
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

        mapPane2.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if(e.getButton().equals(MouseButton.PRIMARY)) {
                box.removeRectangleFromPane();
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
