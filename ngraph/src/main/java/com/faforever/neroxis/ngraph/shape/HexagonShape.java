package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;

import java.awt.*;

public class HexagonShape extends BasicShape {

    public Shape createShape(Graphics2DCanvas canvas, CellState state) {
        Rectangle temp = state.getRectangle();
        int x = temp.x;
        int y = temp.y;
        int w = temp.width;
        int h = temp.height;
        String direction = Utils.getString(state.getStyle(), Constants.STYLE_DIRECTION, Constants.DIRECTION_EAST);
        Polygon hexagon = new Polygon();

        if (direction.equals(Constants.DIRECTION_NORTH) || direction.equals(Constants.DIRECTION_SOUTH)) {
            hexagon.addPoint(x + (int) (0.5 * w), y);
            hexagon.addPoint(x + w, y + (int) (0.25 * h));
            hexagon.addPoint(x + w, y + (int) (0.75 * h));
            hexagon.addPoint(x + (int) (0.5 * w), y + h);
            hexagon.addPoint(x, y + (int) (0.75 * h));
            hexagon.addPoint(x, y + (int) (0.25 * h));
        } else {
            hexagon.addPoint(x + (int) (0.25 * w), y);
            hexagon.addPoint(x + (int) (0.75 * w), y);
            hexagon.addPoint(x + w, y + (int) (0.5 * h));
            hexagon.addPoint(x + (int) (0.75 * w), y + h);
            hexagon.addPoint(x + (int) (0.25 * w), y + h);
            hexagon.addPoint(x, y + (int) (0.5 * h));
        }

        return hexagon;
    }

}
