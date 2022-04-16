package com.faforever.neroxis.ngraph.style.arrow;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.Polygon;

public class ClassicArrow implements Arrow {
    @Override
    public PointDouble paintArrow(Graphics2DCanvas canvas, CellState state, String type, PointDouble pe, double nx, double ny, double size, boolean source) {
        Polygon poly = new Polygon();
        poly.addPoint((int) Math.round(pe.getX()), (int) Math.round(pe.getY()));
        poly.addPoint((int) Math.round(pe.getX() - nx - ny / 2), (int) Math.round(pe.getY() - ny + nx / 2));
        poly.addPoint((int) Math.round(pe.getX() - nx * 3 / 4), (int) Math.round(pe.getY() - ny * 3 / 4));
        poly.addPoint((int) Math.round(pe.getX() + ny / 2 - nx), (int) Math.round(pe.getY() - ny - nx / 2));
        if (Utils.isTrue(state.getStyle(), (source) ? Constants.STYLE_STARTFILL : Constants.STYLE_ENDFILL, true)) {
            canvas.fillShape(poly);
        }
        canvas.getGraphics().draw(poly);
        return new PointDouble(-nx, -ny);
    }
}
