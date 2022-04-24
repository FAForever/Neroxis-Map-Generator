package com.faforever.neroxis.ngraph.style.edge;

import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.GraphView;
import java.util.List;

/**
 * Provides a horizontal elbow edge.
 */
public class SideToSideEdgeStyleFunction implements EdgeStyleFunction {

    @Override
    public void apply(CellState state, CellState source, CellState target, List<PointDouble> points,
                      List<PointDouble> result) {
        GraphView view = state.getView();
        PointDouble pt = ((points != null && points.size() > 0) ? points.get(0) : null);
        PointDouble p0 = state.getAbsolutePoint(0);
        PointDouble pe = state.getAbsolutePoint(state.getAbsolutePointCount() - 1);
        if (pt != null) {
            pt = view.transformControlPoint(state, pt);
        }
        if (p0 != null) {
            source = new CellState();
            source.setX(p0.getX());
            source.setY(p0.getY());
        }
        if (pe != null) {
            target = new CellState();
            target.setX(pe.getX());
            target.setY(pe.getY());
        }
        if (source != null && target != null) {
            double l = Math.max(source.getX(), target.getX());
            double r = Math.min(source.getX() + source.getWidth(), target.getX() + target.getWidth());
            double x = (pt != null) ? pt.getX() : r + (l - r) / 2;
            double y1 = view.getRoutingCenterY(source);
            double y2 = view.getRoutingCenterY(target);
            if (pt != null) {
                if (pt.getY() >= source.getY() && pt.getY() <= source.getY() + source.getHeight()) {
                    y1 = pt.getY();
                }
                if (pt.getY() >= target.getY() && pt.getY() <= target.getY() + target.getHeight()) {
                    y2 = pt.getY();
                }
            }
            if (!target.contains(x, y1) && !source.contains(x, y1)) {
                result.add(new PointDouble(x, y1));
            }
            if (!target.contains(x, y2) && !source.contains(x, y2)) {
                result.add(new PointDouble(x, y2));
            }
            if (result.size() == 1) {
                if (pt != null) {
                    if (!target.contains(x, pt.getY()) && !source.contains(x, pt.getY())) {
                        result.add(new PointDouble(x, pt.getY()));
                    }
                } else {
                    double t = Math.max(source.getY(), target.getY());
                    double b = Math.min(source.getY() + source.getHeight(), target.getY() + target.getHeight());
                    result.add(new PointDouble(x, t + (b - t) / 2));
                }
            }
        }
    }
}
