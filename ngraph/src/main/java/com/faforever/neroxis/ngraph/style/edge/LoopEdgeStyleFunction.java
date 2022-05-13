package com.faforever.neroxis.ngraph.style.edge;

import com.faforever.neroxis.ngraph.style.util.Direction;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;
import com.faforever.neroxis.ngraph.view.GraphView;
import java.util.List;

/**
 * Provides a self-reference, aka. loop.
 */
public class LoopEdgeStyleFunction implements EdgeStyleFunction {
    @Override
    public void apply(CellState state, CellState source, CellState target, List<PointDouble> points,
                      List<PointDouble> result) {
        if (source != null) {
            GraphView view = state.getView();
            Graph graph = view.getGraph();
            PointDouble pt = (points != null && points.size() > 0) ? points.get(0) : null;
            if (pt != null) {
                pt = view.transformControlPoint(state, pt);
                if (source.contains(pt.getX(), pt.getY())) {
                    pt = null;
                }
            }
            double x = 0;
            double dx = 0;
            double y = 0;
            double dy = 0;
            double seg = state.getStyle().getEdge().getSegmentSize() * view.getScale();
            Direction dir = state.getStyle().getShape().getDirection();
            if (dir == Direction.NORTH || dir == Direction.SOUTH) {
                x = view.getRoutingCenterX(source);
                dx = seg;
            } else {
                y = view.getRoutingCenterY(source);
                dy = seg;
            }
            if (pt == null || pt.getX() < source.getX() || pt.getX() > source.getX() + source.getWidth()) {
                if (pt != null) {
                    x = pt.getX();
                    dy = Math.max(Math.abs(y - pt.getY()), dy);
                } else {
                    switch (dir) {
                        case NORTH -> y = source.getY() - 2 * dx;
                        case SOUTH -> y = source.getY() + source.getHeight() + 2 * dx;
                        case EAST -> x = source.getX() - 2 * dy;
                        default -> x = source.getX() + source.getWidth() + 2 * dy;
                    }
                }
            } else {
                // pt != null
                x = view.getRoutingCenterX(source);
                dx = Math.max(Math.abs(x - pt.getX()), dy);
                y = pt.getY();
                dy = 0;
            }
            result.add(new PointDouble(x - dx, y - dy));
            result.add(new PointDouble(x + dx, y + dy));
        }
    }
}
