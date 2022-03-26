package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;

import java.awt.*;

public class TriangleShape extends BasicShape {

    public Shape createShape(Graphics2DCanvas canvas, CellState state) {
        Rectangle temp = state.getRectangle();
        int x = temp.x;
        int y = temp.y;
        int w = temp.width;
        int h = temp.height;
        String direction = Utils.getString(state.getStyle(), Constants.STYLE_DIRECTION, Constants.DIRECTION_EAST);
        Polygon triangle = new Polygon();

        if (direction.equals(Constants.DIRECTION_NORTH)) {
            triangle.addPoint(x, y + h);
            triangle.addPoint(x + w / 2, y);
            triangle.addPoint(x + w, y + h);
        } else if (direction.equals(Constants.DIRECTION_SOUTH)) {
            triangle.addPoint(x, y);
            triangle.addPoint(x + w / 2, y + h);
            triangle.addPoint(x + w, y);
        } else if (direction.equals(Constants.DIRECTION_WEST)) {
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
