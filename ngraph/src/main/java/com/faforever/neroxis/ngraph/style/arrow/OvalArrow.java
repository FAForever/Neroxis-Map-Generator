package com.faforever.neroxis.ngraph.style.arrow;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

public class OvalArrow implements Arrow {

    @Override
    public PointDouble paintArrow(Graphics2DCanvas canvas, CellState state, PointDouble pe, double nx, double ny,
                                  double size, boolean source) {
        double cx = pe.getX() - nx / 2;
        double cy = pe.getY() - ny / 2;
        double a = size / 2;
        Shape shape = new Ellipse2D.Double(cx - a, cy - a, size, size);
        if (source ? state.getStyle().getShape().isStartFill() : state.getStyle().getShape().isEndFill()) {
            canvas.fillShape(shape);
        }
        canvas.getGraphics().draw(shape);
        return new PointDouble(-nx / 2, -ny / 2);
    }
}
