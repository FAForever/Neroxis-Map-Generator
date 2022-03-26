package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Point;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;

public class LineShape extends BasicShape {

    public void paintShape(Graphics2DCanvas canvas, CellState state) {
        if (configureGraphics(canvas, state, false)) {
            boolean rounded = Utils.isTrue(state.getStyle(), Constants.STYLE_ROUNDED, false) && canvas.getScale() > Constants.MIN_SCALE_FOR_ROUNDED_LINES;

            canvas.paintPolyline(createPoints(canvas, state), rounded);
        }
    }

    /**
     *
     */
    public Point[] createPoints(Graphics2DCanvas canvas, CellState state) {
        String direction = Utils.getString(state.getStyle(), Constants.STYLE_DIRECTION, Constants.DIRECTION_EAST);

        Point p0, pe;

        if (direction.equals(Constants.DIRECTION_EAST) || direction.equals(Constants.DIRECTION_WEST)) {
            double mid = state.getCenterY();
            p0 = new Point(state.getX(), mid);
            pe = new Point(state.getX() + state.getWidth(), mid);
        } else {
            double mid = state.getCenterX();
            p0 = new Point(mid, state.getY());
            pe = new Point(mid, state.getY() + state.getHeight());
        }

        Point[] points = new Point[2];
        points[0] = p0;
        points[1] = pe;

        return points;
    }

}
