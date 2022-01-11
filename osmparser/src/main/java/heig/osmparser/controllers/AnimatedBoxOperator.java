package heig.osmparser.controllers;

import heig.osmparser.drawing.Box;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public class AnimatedBoxOperator {

    private Pane pane;
    private Box box;
    private double[] upperLeft, bottomRight;
    private MainController controller;

    public AnimatedBoxOperator(MainController controller, Pane pane, double[] bounds) {
        this.controller = controller;
        this.pane = pane;
        this.upperLeft = new double[2];
        this.bottomRight = new double[2];

        // Area selection events
        pane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                if (box != null) box.removeRectangleFromPane();
                box = new Box(pane, e.getX(), e.getY());
                if (controller != null) {
                    upperLeft = controller.getLatLonFromMousePos(e.getX(), e.getY(), bounds, pane);
                    controller.displayBounds(new double[]{upperLeft[1], upperLeft[0], bottomRight[1], bottomRight[0]});
                }
            }
        });

        pane.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                box.render(e.getX(), e.getY());
                if (controller != null) {
                    bottomRight = controller.getLatLonFromMousePos(e.getX(), e.getY(), bounds, pane);
                    controller.displayBounds(new double[]{upperLeft[1], upperLeft[0], bottomRight[1], bottomRight[0]});
                }
            }
        });
    }

    public Box getBox() {
        return box;
    }
}
