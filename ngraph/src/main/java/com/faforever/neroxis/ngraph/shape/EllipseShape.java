package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.view.CellState;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class EllipseShape extends BasicShape {
    @Override
    public Shape createShape(Graphics2DCanvas canvas, CellState state) {
        Rectangle temp = state.getRectangle();

        return new Ellipse2D.Float(temp.x, temp.y, temp.width, temp.height);
    }
}
