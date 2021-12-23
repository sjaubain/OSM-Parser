package heig.osmparser.controllers;
import javafx.scene.Cursor;
import javafx.scene.layout.Pane;

public class AnimatedDragOperator {

    private Pane pane;
    private double capturedX, capturedY;

    public AnimatedDragOperator(Pane pane) {

        pane.setOnMousePressed(e -> {
            capturedX = e.getSceneX() - pane.getTranslateX();
            capturedY = e.getSceneY() - pane.getTranslateY();
            pane.getScene().setCursor(Cursor.CLOSED_HAND);
        });

        pane.setOnMouseReleased(e -> pane.getScene().setCursor(Cursor.HAND)
        );

        pane.setOnMouseDragged(e -> {
            pane.setTranslateX(e.getSceneX() - capturedX);
            pane.setTranslateY(e.getSceneY() - capturedY);
        });

        pane.setOnMouseEntered(e -> {
            if (!e.isPrimaryButtonDown()) {
                pane.getScene().setCursor(Cursor.DEFAULT);
            }
        });

        pane.setOnMouseExited(e -> {
            if (!e.isPrimaryButtonDown()) {
                pane.getScene().setCursor(Cursor.DEFAULT);
            }
        });
    }
}
