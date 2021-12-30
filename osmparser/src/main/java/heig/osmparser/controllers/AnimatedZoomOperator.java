package heig.osmparser.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class AnimatedZoomOperator {

    private Timeline timeline;

    private Pane pane;

    private double zoomFactor;

    //TODO  set scale factor in final static value in controller and pass it through controller
    public AnimatedZoomOperator(Pane pane, double zoomFactor) {
        this.pane = pane;
        this.timeline = new Timeline(60);
        this.zoomFactor = zoomFactor;
        pane.setOnScroll(event -> {
            double factor = this.zoomFactor;
            if (event.getDeltaY() <= 0) {
                factor = 1 / factor;
            }
            zoom(pane, factor, event.getSceneX(), event.getSceneY());
        });
    }

    public void zoom(Pane node, double factor, double x, double y) {
        // determine scale
        double oldScale = node.getScaleX();
        double scale = oldScale * factor;
        double f = (scale / oldScale) - 1;

        // determine offset that we will have to move the node
        Bounds bounds = node.localToScene(node.getBoundsInLocal());
        double dx = (x - (bounds.getWidth() / 2 + bounds.getMinX()));
        double dy = (y - (bounds.getHeight() / 2 + bounds.getMinY()));

        // timeline that scales and moves the node
        timeline.getKeyFrames().clear();
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(200), new KeyValue(node.translateXProperty(), node.getTranslateX() - f * dx)),
                new KeyFrame(Duration.millis(200), new KeyValue(node.translateYProperty(), node.getTranslateY() - f * dy)),
                new KeyFrame(Duration.millis(200), new KeyValue(node.scaleXProperty(), scale)),
                new KeyFrame(Duration.millis(200), new KeyValue(node.scaleYProperty(), scale))
        );
        timeline.play();
        timeline.setOnFinished(e -> {
            //System.out.println(pane.getScaleX());
            //String url = "file:./input/tiles/tile" + (int) (pane.getPrefWidth() * pane.getScaleX()) + ".png";
            System.out.println((int) (pane.getScaleX() * pane.getPrefWidth()));
            //pane.setStyle("-fx-background-image: url(" + url + ");");
            // create a image
            String url = "file:./src/main/resources/heig/osmparser/tiles/tile" + (int) (pane.getPrefWidth() * pane.getScaleX()) + ".png";
            Image image = new Image(url);
            // create a background image
            BackgroundImage backgroundimage = new BackgroundImage(image,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.DEFAULT,
                    new BackgroundSize(1.0, 1.0, true, true, false, false));
            // create Background
            Background background = new Background(backgroundimage);
            // set background
            pane.setBackground(background);
        });

    }
}