package com.faforever.neroxis.ngraph.style.perimeter;

import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.CellState;

/**
 * Describes a rectangular perimeter for the given bounds.
 */
public class RectanglePerimeter implements Perimeter {

    @Override
    public PointDouble apply(RectangleDouble bounds, CellState vertex, PointDouble next, boolean orthogonal) {
        double cx = bounds.getCenterX();
        double cy = bounds.getCenterY();
        double dx = next.getX() - cx;
        double dy = next.getY() - cy;
        double alpha = Math.atan2(dy, dx);
        PointDouble p = new PointDouble();
        double pi = Math.PI;
        double pi2 = Math.PI / 2;
        double beta = pi2 - alpha;
        double t = Math.atan2(bounds.getHeight(), bounds.getWidth());
        if (alpha < -pi + t || alpha > pi - t) {
            // Left edge
            p.setX(bounds.getX());
            p.setY(cy - bounds.getWidth() * Math.tan(alpha) / 2);
        } else if (alpha < -t) {
            // Top Edge
            p.setY(bounds.getY());
            p.setX(cx - bounds.getHeight() * Math.tan(beta) / 2);
        } else if (alpha < t) {
            // Right Edge
            p.setX(bounds.getX() + bounds.getWidth());
            p.setY(cy + bounds.getWidth() * Math.tan(alpha) / 2);
        } else {
            // Bottom Edge
            p.setY(bounds.getY() + bounds.getHeight());
            p.setX(cx + bounds.getHeight() * Math.tan(beta) / 2);
        }
        if (orthogonal) {
            if (next.getX() >= bounds.getX() && next.getX() <= bounds.getX() + bounds.getWidth()) {
                p.setX(next.getX());
            } else if (next.getY() >= bounds.getY() && next.getY() <= bounds.getY() + bounds.getHeight()) {
                p.setY(next.getY());
            }
            if (next.getX() < bounds.getX()) {
                p.setX(bounds.getX());
            } else if (next.getX() > bounds.getX() + bounds.getWidth()) {
                p.setX(bounds.getX() + bounds.getWidth());
            }
            if (next.getY() < bounds.getY()) {
                p.setY(bounds.getY());
            } else if (next.getY() > bounds.getY() + bounds.getHeight()) {
                p.setY(bounds.getY() + bounds.getHeight());
            }
        }
        return p;
    }
}
