package heig.osmparser.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class AnimatedZoomOperator {

    private Timeline timeline;

    private Pane pane;

    public AnimatedZoomOperator(Pane pane) {
        this.pane = pane;
        this.timeline = new Timeline(60);

        pane.setOnScroll(event -> {
            double zoomFactor = 1.4;
            if (event.getDeltaY() <= 0) {
                zoomFactor = 1 / zoomFactor;
            }
            zoom(pane, zoomFactor, event.getSceneX(), event.getSceneY());
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
    }
}