package heig.osmparser.controllers;

import heig.osmparser.drawing.Box;
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
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class BoundsController implements Initializable {

    @FXML
    private AnchorPane mainPane;

    @FXML
    private Pane mapPane2;

    @FXML
    private WebView webView;

    private MainController mainController;
    private double zoomFactor = 1.6;
    private int mapWidth = 2000;
    private int mapHeight = 857;
    private Box box;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //Creating a SVGPath object
        try {

            mapPane2.setPrefHeight(mapHeight);
            mapPane2.setPrefWidth(mapWidth);
            mapPane2.setTranslateX(-800);

            SVGParser parser = new SVGParser();
            SVGPath svgPath = new SVGPath();
            // styling
            svgPath.setFill((Paint.valueOf("transparent")));
            svgPath.setStroke(Paint.valueOf("black"));
            svgPath.setStrokeWidth(0.2);

            String path = parser.toString("./src/main/resources/heig/osmparser/word.svg");
            svgPath.setContent(path);
            Group root = new Group(svgPath);
            mapPane2.getChildren().add(root);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Area selection events
        mapPane2.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if(e.getButton().equals(MouseButton.PRIMARY)) {
                box = new Box(mapPane2, e.getX(), e.getY());
            }
        });

        mapPane2.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if(e.getButton().equals(MouseButton.PRIMARY)) {
                box.render(e.getX(), e.getY());
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
