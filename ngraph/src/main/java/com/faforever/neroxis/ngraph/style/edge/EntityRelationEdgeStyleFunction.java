package com.faforever.neroxis.ngraph.style.edge;

import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.GraphView;
import java.util.List;

/**
 * Provides an entity relation style for edges (as used in database
 * schema diagrams).
 */
public class EntityRelationEdgeStyleFunction implements EdgeStyleFunction {
    public static final double ENTITY_SEGMENT = 30;

    @Override
    public void apply(CellState state, CellState source, CellState target, List<PointDouble> points, List<PointDouble> result) {
        GraphView view = state.getView();
        IGraphModel model = view.getGraph().getModel();
        double segment = Utils.getDouble(state.getStyle(), Constants.STYLE_SEGMENT, ENTITY_SEGMENT) * state.getView().getScale();
        PointDouble p0 = state.getAbsolutePoint(0);
        PointDouble pe = state.getAbsolutePoint(state.getAbsolutePointCount() - 1);
        boolean isSourceLeft = false;
        if (p0 != null) {
            source = new CellState();
            source.setX(p0.getX());
            source.setY(p0.getY());
        } else if (source != null) {
            int constraint = Utils.getPortConstraints(source, state, true, Constants.DIRECTION_MASK_NONE);
            if (constraint != Constants.DIRECTION_MASK_NONE) {
                isSourceLeft = constraint == Constants.DIRECTION_MASK_WEST;
            } else {
                Geometry sourceGeometry = model.getGeometry(source.getCell());
                if (sourceGeometry.isRelative()) {
                    isSourceLeft = sourceGeometry.getX() <= 0.5;
                } else if (target != null) {
                    isSourceLeft = target.getX() + target.getWidth() < source.getX();
                }
            }
        }
        boolean isTargetLeft = true;
        if (pe != null) {
            target = new CellState();
            target.setX(pe.getX());
            target.setY(pe.getY());
        } else if (target != null) {
            int constraint = Utils.getPortConstraints(target, state, false, Constants.DIRECTION_MASK_NONE);
            if (constraint != Constants.DIRECTION_MASK_NONE) {
                isTargetLeft = constraint == Constants.DIRECTION_MASK_WEST;
            } else {
                Geometry targetGeometry = model.getGeometry(target.getCell());
                if (targetGeometry.isRelative()) {
                    isTargetLeft = targetGeometry.getX() <= 0.5;
                } else if (source != null) {
                    isTargetLeft = source.getX() + source.getWidth() < target.getX();
                }
            }
        }
        if (source != null && target != null) {
            double x0 = (isSourceLeft) ? source.getX() : source.getX() + source.getWidth();
            double y0 = view.getRoutingCenterY(source);
            double xe = (isTargetLeft) ? target.getX() : target.getX() + target.getWidth();
            double ye = view.getRoutingCenterY(target);
            double seg = segment;
            double dx = (isSourceLeft) ? -seg : seg;
            PointDouble dep = new PointDouble(x0 + dx, y0);
            result.add(dep);
            dx = (isTargetLeft) ? -seg : seg;
            PointDouble arr = new PointDouble(xe + dx, ye);
            // Adds intermediate points if both go out on same side
            if (isSourceLeft == isTargetLeft) {
                double x = (isSourceLeft) ? Math.min(x0, xe) - segment : Math.max(x0, xe) + segment;
                result.add(new PointDouble(x, y0));
                result.add(new PointDouble(x, ye));
            } else if ((dep.getX() < arr.getX()) == isSourceLeft) {
                double midY = y0 + (ye - y0) / 2;
                result.add(new PointDouble(dep.getX(), midY));
                result.add(new PointDouble(arr.getX(), midY));
            }
            result.add(arr);
        }
    }
}
