package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.style.Direction;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;

public class TriangleShape extends BasicShape {

    public Shape createShape(Graphics2DCanvas canvas, CellState state) {
        Rectangle temp = state.getRectangle();
        int x = temp.x;
        int y = temp.y;
        int w = temp.width;
        int h = temp.height;
        Direction direction = state.getStyle().getShape().getDirection();
        Polygon triangle = new Polygon();
        if (direction == Direction.NORTH) {
            triangle.addPoint(x, y + h);
            triangle.addPoint(x + w / 2, y);
            triangle.addPoint(x + w, y + h);
        } else if (direction == Direction.SOUTH) {
            triangle.addPoint(x, y);
            triangle.addPoint(x + w / 2, y + h);
            triangle.addPoint(x + w, y);
        } else if (direction == Direction.WEST) {
            triangle.addPoint(x + w, y);
            triangle.addPoint(x, y + h / 2);
            triangle.addPoint(x + w, y + h);
        } else {
            triangle.addPoint(x, y);
            triangle.addPoint(x + w, y + h / 2);
            triangle.addPoint(x, y + h);
        }

        return triangle;
    }

}
