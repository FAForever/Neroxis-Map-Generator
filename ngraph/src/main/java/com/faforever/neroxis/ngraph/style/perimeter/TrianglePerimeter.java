package com.faforever.neroxis.ngraph.style.perimeter;

import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;

/**
 * Describes a triangle perimeter.
 */
public class TrianglePerimeter implements Perimeter {

    @Override
    public PointDouble apply(RectangleDouble bounds, CellState vertex, PointDouble next, boolean orthogonal) {
        Object direction = (vertex != null) ? vertex.getStyle().getShape().getDirection() : Constants.DIRECTION_EAST;
        boolean vertical = direction.equals(Constants.DIRECTION_NORTH) || direction.equals(Constants.DIRECTION_SOUTH);
        double x = bounds.getX();
        double y = bounds.getY();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        double cx = x + w / 2;
        double cy = y + h / 2;
        PointDouble start = new PointDouble(x, y);
        PointDouble corner = new PointDouble(x + w, cy);
        PointDouble end = new PointDouble(x, y + h);
        if (direction.equals(Constants.DIRECTION_NORTH)) {
            start = end;
            corner = new PointDouble(cx, y);
            end = new PointDouble(x + w, y + h);
        } else if (direction.equals(Constants.DIRECTION_SOUTH)) {
            corner = new PointDouble(cx, y + h);
            end = new PointDouble(x + w, y);
        } else if (direction.equals(Constants.DIRECTION_WEST)) {
            start = new PointDouble(x + w, y);
            corner = new PointDouble(x, cy);
            end = new PointDouble(x + w, y + h);
        }
        // Compute angle
        double dx = next.getX() - cx;
        double dy = next.getY() - cy;
        double alpha = (vertical) ? Math.atan2(dx, dy) : Math.atan2(dy, dx);
        double t = (vertical) ? Math.atan2(w, h) : Math.atan2(h, w);
        boolean base = false;
        if (direction.equals(Constants.DIRECTION_NORTH) || direction.equals(Constants.DIRECTION_WEST)) {
            base = alpha > -t && alpha < t;
        } else {
            base = alpha < -Math.PI + t || alpha > Math.PI - t;
        }
        PointDouble result = null;
        if (base) {
            if (orthogonal && ((vertical && next.getX() >= start.getX() && next.getX() <= end.getX()) || (!vertical
                                                                                                          && next.getY()
                                                                                                             >= start.getY()
                                                                                                          && next.getY()
                                                                                                             <= end.getY()))) {
                if (vertical) {
                    result = new PointDouble(next.getX(), start.getY());
                } else {
                    result = new PointDouble(start.getX(), next.getY());
                }
            } else {
                if (direction.equals(Constants.DIRECTION_EAST)) {
                    result = new PointDouble(x, y + h / 2 - w * Math.tan(alpha) / 2);
                } else if (direction.equals(Constants.DIRECTION_NORTH)) {
                    result = new PointDouble(x + w / 2 + h * Math.tan(alpha) / 2, y + h);
                } else if (direction.equals(Constants.DIRECTION_SOUTH)) {
                    result = new PointDouble(x + w / 2 - h * Math.tan(alpha) / 2, y);
                } else if (direction.equals(Constants.DIRECTION_WEST)) {
                    result = new PointDouble(x + w, y + h / 2 + w * Math.tan(alpha) / 2);
                }
            }
        } else {
            if (orthogonal) {
                PointDouble pt = new PointDouble(cx, cy);
                if (next.getY() >= y && next.getY() <= y + h) {
                    pt.setX((vertical) ? cx : ((direction.equals(Constants.DIRECTION_WEST)) ? x + w : x));
                    pt.setY(next.getY());
                } else if (next.getX() >= x && next.getX() <= x + w) {
                    pt.setX(next.getX());
                    pt.setY((!vertical) ? cy : ((direction.equals(Constants.DIRECTION_NORTH)) ? y + h : y));
                }
                // Compute angle
                dx = next.getX() - pt.getX();
                dy = next.getY() - pt.getY();
                cx = pt.getX();
                cy = pt.getY();
            }
            if ((vertical && next.getX() <= x + w / 2) || (!vertical && next.getY() <= y + h / 2)) {
                result = Utils.intersection(next.getX(), next.getY(), cx, cy, start.getX(), start.getY(), corner.getX(),
                                            corner.getY());
            } else {
                result = Utils.intersection(next.getX(), next.getY(), cx, cy, corner.getX(), corner.getY(), end.getX(),
                                            end.getY());
            }
        }
        if (result == null) {
            result = new PointDouble(cx, cy);
        }
        return result;
    }
}
