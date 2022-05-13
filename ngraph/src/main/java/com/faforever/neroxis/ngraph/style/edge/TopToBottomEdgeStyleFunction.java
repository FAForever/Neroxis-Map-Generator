package com.faforever.neroxis.ngraph.style.edge;

import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.GraphView;
import java.util.List;

/**
 * Provides a vertical elbow edge.
 */
public class TopToBottomEdgeStyleFunction implements EdgeStyleFunction {
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
            double t = Math.max(source.getY(), target.getY());
            double b = Math.min(source.getY() + source.getHeight(), target.getY() + target.getHeight());
            double x = view.getRoutingCenterX(source);
            if (pt != null && pt.getX() >= source.getX() && pt.getX() <= source.getX() + source.getWidth()) {
                x = pt.getX();
            }
            double y = (pt != null) ? pt.getY() : b + (t - b) / 2;
            if (!target.contains(x, y) && !source.contains(x, y)) {
                result.add(new PointDouble(x, y));
            }
            if (pt != null && pt.getX() >= target.getX() && pt.getX() <= target.getX() + target.getWidth()) {
                x = pt.getX();
            } else {
                x = view.getRoutingCenterX(target);
            }
            if (!target.contains(x, y) && !source.contains(x, y)) {
                result.add(new PointDouble(x, y));
            }
            if (result.size() == 1) {
                if (pt != null) {
                    if (!target.contains(pt.getX(), y) && !source.contains(pt.getX(), y)) {
                        result.add(new PointDouble(pt.getX(), y));
                    }
                } else {
                    double l = Math.max(source.getX(), target.getX());
                    double r = Math.min(source.getX() + source.getWidth(), target.getX() + target.getWidth());
                    result.add(new PointDouble(l + (r - l) / 2, y));
                }
            }
        }
    }
}
