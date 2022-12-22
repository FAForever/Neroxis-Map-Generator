package com.faforever.neroxis.ngraph.style.edge;

import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.GraphView;

import java.util.List;

/**
 * Implements an orthogonal edge style. Use <EdgeSegmentHandler>
 * as an interactive handler for this style.
 */
public class SegmentConnectorEdgeStyleFunction implements EdgeStyleFunction {
    @Override
    public void apply(CellState state, CellState source, CellState target, List<PointDouble> points,
                      List<PointDouble> result) {
        // Creates array of all way- and terminalpoints
        List<PointDouble> pts = state.getAbsolutePoints();
        boolean horizontal = true;
        PointDouble hint;
        // Adds the first point
        PointDouble pt = pts.get(0);
        GraphView view = state.getView();
        if (pt == null && source != null) {
            pt = new PointDouble(view.getRoutingCenterX(source), view.getRoutingCenterY(source));
        } else if (pt != null) {
            pt = (PointDouble) pt.clone();
        }
        int lastInx = pts.size() - 1;
        // Adds the waypoints
        if (points != null && points.size() > 0) {
            hint = view.transformControlPoint(state, points.get(0));
            CellState currentTerm = source;
            PointDouble currentPt = pts.get(0);
            boolean hozChan;
            boolean vertChan;
            PointDouble currentHint = hint;
            int pointsLen = points.size();
            for (int i = 0; i < 2; i++) {
                boolean fixedVertAlign = currentPt != null && currentPt.getX() == currentHint.getX();
                boolean fixedHozAlign = currentPt != null && currentPt.getY() == currentHint.getY();
                boolean inHozChan = currentTerm != null && (currentHint.getY() >= currentTerm.getY()
                                                            && currentHint.getY()
                                                               <= currentTerm.getY() + currentTerm.getHeight());
                boolean inVertChan = currentTerm != null && (currentHint.getX() >= currentTerm.getX()
                                                             && currentHint.getX()
                                                                <= currentTerm.getX() + currentTerm.getWidth());
                hozChan = fixedHozAlign || (currentPt == null && inHozChan);
                vertChan = fixedVertAlign || (currentPt == null && inVertChan);
                if (currentPt != null && (!fixedHozAlign && !fixedVertAlign) && (inHozChan || inVertChan)) {
                    horizontal = !inHozChan;
                    break;
                }
                if (vertChan || hozChan) {
                    horizontal = hozChan;
                    if (i == 1) {
                        // Work back from target end
                        horizontal = points.size() % 2 == 0 ? hozChan : vertChan;
                    }
                    break;
                }
                currentTerm = target;
                currentPt = pts.get(lastInx);
                currentHint = view.transformControlPoint(state, points.get(pointsLen - 1));
            }
            if (horizontal && ((pts.get(0) != null && pts.get(0).getY() != hint.getY()) || (pts.get(0) == null
                                                                                            && source != null
                                                                                            && (hint.getY()
                                                                                                < source.getY()
                                                                                                || hint.getY()
                                                                                                   > source.getY()
                                                                                                     + source.getHeight())))) {
                result.add(new PointDouble(pt.getX(), hint.getY()));
            } else if (!horizontal && ((pts.get(0) != null && pts.get(0).getX() != hint.getX()) || (pts.get(0) == null
                                                                                                    && source != null
                                                                                                    && (hint.getX()
                                                                                                        < source.getX()
                                                                                                        || hint.getX()
                                                                                                           > source.getX()
                                                                                                             + source.getWidth())))) {
                result.add(new PointDouble(hint.getX(), pt.getY()));
            }
            if (horizontal) {
                pt.setY(hint.getY());
            } else {
                pt.setX(hint.getX());
            }
            for (PointDouble pointDouble : points) {
                horizontal = !horizontal;
                hint = view.transformControlPoint(state, pointDouble);
                //				Log.show();
                //				Log.debug('hint', i, hint.x, hint.y);
                if (horizontal) {
                    pt.setY(hint.getY());
                } else {
                    pt.setX(hint.getX());
                }
                result.add((PointDouble) pt.clone());
            }
        } else {
            hint = pt;
            // FIXME: First click in connect preview toggles orientation
            horizontal = true;
        }
        // Adds the last point
        pt = pts.get(lastInx);
        if (pt == null && target != null) {
            pt = new PointDouble(view.getRoutingCenterX(target), view.getRoutingCenterY(target));
        }
        if (horizontal && ((pts.get(lastInx) != null && pts.get(lastInx).getY() != hint.getY()) || (pts.get(lastInx)
                                                                                                    == null
                                                                                                    && target != null
                                                                                                    && (hint.getY()
                                                                                                        < target.getY()
                                                                                                        || hint.getY()
                                                                                                           > target.getY()
                                                                                                             + target.getHeight())))) {
            result.add(new PointDouble(pt.getX(), hint.getY()));
        } else if (!horizontal && ((pts.get(lastInx) != null && pts.get(lastInx).getX() != hint.getX()) || (pts.get(
                lastInx) == null && target != null && (hint.getX() < target.getX()
                                                       || hint.getX() > target.getX() + target.getWidth())))) {
            result.add(new PointDouble(hint.getX(), pt.getY()));
        }
        // Removes bends inside the source terminal for floating ports
        if (pts.get(0) == null && source != null) {
            while (result.size() > 1 && source.contains(result.get(1).getX(), result.get(1).getY())) {
                result.remove(1);
            }
        }
        // Removes bends inside the target terminal
        if (pts.get(lastInx) == null && target != null) {
            while (result.size() > 1 && target.contains(result.get(result.size() - 1).getX(),
                                                        result.get(result.size() - 1).getY())) {
                result.remove(result.size() - 1);
            }
        }
    }
}
