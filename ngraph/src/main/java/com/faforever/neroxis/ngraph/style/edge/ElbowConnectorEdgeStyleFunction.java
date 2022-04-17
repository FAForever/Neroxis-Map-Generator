package com.faforever.neroxis.ngraph.style.edge;

import com.faforever.neroxis.ngraph.style.Elbow;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import java.util.List;

/**
 * Uses either SideToSide or TopToBottom depending on the horizontal
 * flag in the cell style. SideToSide is used if horizontal is true or
 * unspecified.
 */
public class ElbowConnectorEdgeStyleFunction implements EdgeStyleFunction {
    private static final TopToBottomEdgeStyleFunction TOP_TO_BOTTOM_EDGE_STYLE = new TopToBottomEdgeStyleFunction();
    private static final SideToSideEdgeStyleFunction SIDE_TO_SIDE_EDGE_STYLE = new SideToSideEdgeStyleFunction();

    @Override
    public void apply(CellState state, CellState source, CellState target, List<PointDouble> points, List<PointDouble> result) {
        PointDouble pt = (points != null && points.size() > 0) ? points.get(0) : null;
        boolean vertical = false;
        boolean horizontal = false;
        if (source != null && target != null) {
            if (pt != null) {
                double left = Math.min(source.getX(), target.getX());
                double right = Math.max(source.getX() + source.getWidth(), target.getX() + target.getWidth());
                double top = Math.min(source.getY(), target.getY());
                double bottom = Math.max(source.getY() + source.getHeight(), target.getY() + target.getHeight());
                pt = state.getView().transformControlPoint(state, pt);
                vertical = pt.getY() < top || pt.getY() > bottom;
                horizontal = pt.getX() < left || pt.getX() > right;
            } else {
                double left = Math.max(source.getX(), target.getX());
                double right = Math.min(source.getX() + source.getWidth(), target.getX() + target.getWidth());
                vertical = left == right;
                if (!vertical) {
                    double top = Math.max(source.getY(), target.getY());
                    double bottom = Math.min(source.getY() + source.getHeight(), target.getY() + target.getHeight());
                    horizontal = top == bottom;
                }
            }
        }
        if (!horizontal && (vertical || state.getStyle().getEdge().getElbow() == Elbow.VERTICAL)) {
            TOP_TO_BOTTOM_EDGE_STYLE.apply(state, source, target, points, result);
        } else {
            SIDE_TO_SIDE_EDGE_STYLE.apply(state, source, target, points, result);
        }
    }
}
