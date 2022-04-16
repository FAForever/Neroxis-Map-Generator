package com.faforever.neroxis.ngraph.style.arrow;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.geom.Line2D;

public class OpenArrow implements Arrow {
    @Override
    public PointDouble paintArrow(Graphics2DCanvas canvas, CellState state, String type, PointDouble pe, double nx, double ny, double size, boolean source) {
        canvas.getGraphics().draw(new Line2D.Float((int) Math.round(pe.getX() - nx - ny / 2), (int) Math.round(pe.getY() - ny + nx / 2), (int) Math.round(pe.getX() - nx / 6), (int) Math.round(pe.getY() - ny / 6)));
        canvas.getGraphics().draw(new Line2D.Float((int) Math.round(pe.getX() - nx / 6), (int) Math.round(pe.getY() - ny / 6), (int) Math.round(pe.getX() + ny / 2 - nx), (int) Math.round(pe.getY() - ny - nx / 2)));
        return new PointDouble(-nx / 2, -ny / 2);
    }
}
