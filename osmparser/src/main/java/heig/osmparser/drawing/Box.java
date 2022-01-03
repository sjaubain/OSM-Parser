package heig.osmparser.drawing;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Box {

    private static int RECTANGLE_NB_POINTS = 4;

    private Pane drawingPane;
    private Rectangle rectangle;
    private double startX, startY;
    private Color color;

    public Box(Pane drawingPane, double startX, double startY) {
        this.drawingPane = drawingPane;
        this.startX = startX;
        this.startY = startY;

        this.rectangle = new Rectangle();
        rectangle.setFill(Color.TRANSPARENT);
        rectangle.setStyle("-fx-stroke-width: 0.2");
        color = Color.rgb(255,0,0, 1);
        rectangle.setStroke(color);
        this.drawingPane.getChildren().add(rectangle);
    }

    public Box(Pane drawingPane, double startX, double startY, double width, double height) {
        this(drawingPane, startX, startY);
        rectangle.setWidth(width);
        rectangle.setHeight(height);
        rectangle.setX(startX);
        rectangle.setY(startY);
    }

    public void render(double endX, double endY) {

        if(endX < startX) {
            rectangle.setX(endX);
            if(endY < startY)
                rectangle.setY(endY);
        } else if(endY < startY) {
            rectangle.setY(endY);
        } else {
            rectangle.setX(startX);
            rectangle.setY(startY);
        }

        rectangle.setWidth(Math.abs(endX - startX));
        rectangle.setHeight(Math.abs(endY - startY));
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public void removeRectangleFromPane() {
        this.drawingPane.getChildren().remove(this.rectangle);
    }
}