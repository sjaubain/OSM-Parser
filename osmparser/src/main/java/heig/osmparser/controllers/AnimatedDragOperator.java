package heig.osmparser.controllers;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public class AnimatedDragOperator {

    private Pane pane;
    private double capturedX, capturedY;

    public AnimatedDragOperator(Pane pane) {

        /*
        pane.setOnMousePressed(e -> {
            if(e.getButton().equals(MouseButton.SECONDARY)) {
                capturedX = e.getSceneX() - pane.getTranslateX();
                capturedY = e.getSceneY() - pane.getTranslateY();
                pane.getScene().setCursor(Cursor.CLOSED_HAND);
            }
        });*/

        pane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton().equals(MouseButton.SECONDARY)) {
                if (!e.isPrimaryButtonDown()) {
                    capturedX = e.getSceneX() - pane.getTranslateX();
                    capturedY = e.getSceneY() - pane.getTranslateY();
                    pane.getScene().setCursor(Cursor.CLOSED_HAND);
                }
            }
        });

        /*
        pane.setOnMouseReleased(e -> {
            if (e.getButton().equals(MouseButton.SECONDARY)) {
                pane.getScene().setCursor(Cursor.DEFAULT);
            }
        });
        */

        pane.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getButton().equals(MouseButton.SECONDARY)) {
                if (!e.isPrimaryButtonDown()) {
                    pane.getScene().setCursor(Cursor.DEFAULT);
                }
            }
        });

        pane.setOnMouseDragged(e -> {
            if (e.getButton().equals(MouseButton.SECONDARY)) {
                pane.setTranslateX(e.getSceneX() - capturedX);
                pane.setTranslateY(e.getSceneY() - capturedY);
            }
        });

        pane.setOnMouseEntered(e -> {
            if (e.getButton().equals(MouseButton.SECONDARY)) {
                if (!e.isPrimaryButtonDown()) {
                    pane.getScene().setCursor(Cursor.DEFAULT);
                }
            }
        });

        pane.setOnMouseExited(e -> {
            if (e.getButton().equals(MouseButton.SECONDARY)) {
                if (!e.isPrimaryButtonDown()) {
                    pane.getScene().setCursor(Cursor.DEFAULT);
                }
            }
        });
    }
}
