package com.faforever.neroxis.ngraph.style.perimeter;

import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;

/**
 * Describes a rhombus (aka diamond) perimeter.
 */
public class RhombusPerimeter implements Perimeter {
    @Override
    public PointDouble apply(RectangleDouble bounds, CellState vertex, PointDouble next, boolean orthogonal) {
        double x = bounds.getX();
        double y = bounds.getY();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        double cx = x + w / 2;
        double cy = y + h / 2;
        double px = next.getX();
        double py = next.getY();
        // Special case for intersecting the diamond's corners
        if (cx == px) {
            if (cy > py) {
                return new PointDouble(cx, y); // top
            } else {
                return new PointDouble(cx, y + h); // bottom
            }
        } else if (cy == py) {
            if (cx > px) {
                return new PointDouble(x, cy); // left
            } else {
                return new PointDouble(x + w, cy); // right
            }
        }
        double tx = cx;
        double ty = cy;
        if (orthogonal) {
            if (px >= x && px <= x + w) {
                tx = px;
            } else if (py >= y && py <= y + h) {
                ty = py;
            }
        }
        // In which quadrant will the intersection be?
        // set the slope and offset of the border line accordingly
        if (px < cx) {
            if (py < cy) {
                return Utils.intersection(px, py, tx, ty, cx, y, x, cy);
            } else {
                return Utils.intersection(px, py, tx, ty, cx, y + h, x, cy);
            }
        } else if (py < cy) {
            return Utils.intersection(px, py, tx, ty, cx, y, x + w, cy);
        } else {
            return Utils.intersection(px, py, tx, ty, cx, y + h, x + w, cy);
        }
    }
}
