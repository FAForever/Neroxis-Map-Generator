package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;

import java.awt.*;

public class ArrowShape extends BasicShape {
    public static final int SPACING = 10;
    public static final int WIDTH = 30;
    public static final int SIZE = 30;

    @Override
    public Shape createShape(Graphics2DCanvas canvas, CellState state) {
        double scale = canvas.getScale();
        PointDouble p0 = state.getAbsolutePoint(0);
        PointDouble pe = state.getAbsolutePoint(state.getAbsolutePointCount() - 1);
        // Geometry of arrow
        double spacing = Constants.ARROW_SPACING * scale;
        double width = Constants.ARROW_WIDTH * scale;
        double arrow = Constants.ARROW_SIZE * scale;
        double dx = pe.getX() - p0.getX();
        double dy = pe.getY() - p0.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        double length = dist - 2 * spacing - arrow;

        // Computes the norm and the inverse norm
        double nx = dx / dist;
        double ny = dy / dist;
        double basex = length * nx;
        double basey = length * ny;
        double floorx = width * ny / 3;
        double floory = -width * nx / 3;

        // Computes points
        double p0x = p0.getX() - floorx / 2 + spacing * nx;
        double p0y = p0.getY() - floory / 2 + spacing * ny;
        double p1x = p0x + floorx;
        double p1y = p0y + floory;
        double p2x = p1x + basex;
        double p2y = p1y + basey;
        double p3x = p2x + floorx;
        double p3y = p2y + floory;
        // p4 not required
        double p5x = p3x - 3 * floorx;
        double p5y = p3y - 3 * floory;

        Polygon poly = new Polygon();
        poly.addPoint((int) Math.round(p0x), (int) Math.round(p0y));
        poly.addPoint((int) Math.round(p1x), (int) Math.round(p1y));
        poly.addPoint((int) Math.round(p2x), (int) Math.round(p2y));
        poly.addPoint((int) Math.round(p3x), (int) Math.round(p3y));
        poly.addPoint((int) Math.round(pe.getX() - spacing * nx), (int) Math.round(pe.getY() - spacing * ny));
        poly.addPoint((int) Math.round(p5x), (int) Math.round(p5y));
        poly.addPoint((int) Math.round(p5x + floorx), (int) Math.round(p5y + floory));

        return poly;
    }
}
