/**
 * Copyright (c) 2008, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.swing.handler;

import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.Resources;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.GraphView;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author Administrator
 */
public class ElbowEdgeHandler extends EdgeHandler {
    /**
     *
     */
    public ElbowEdgeHandler(GraphComponent graphComponent, CellState state) {
        super(graphComponent, state);
    }

    /**
     * Returns true if the given index is the index of the last handle.
     */
    @Override
    public boolean isLabel(int index) {
        return index == 3;
    }

    /**
     * Hook for subclassers to return tooltip texts for certain points on the
     * handle.
     */
    @Override
    public String getToolTipText(MouseEvent e) {
        int index = getIndexAt(e.getX(), e.getY());

        if (index == 1) {
            return Resources.get("doubleClickOrientation");
        }

        return null;
    }

    @Override
    protected Rectangle[] createHandles() {
        p = createPoints(state);
        Rectangle[] h = new Rectangle[4];
        PointDouble p0 = state.getAbsolutePoint(0);
        PointDouble pe = state.getAbsolutePoint(state.getAbsolutePointCount() - 1);
        h[0] = createHandle(p0.toPoint());
        h[2] = createHandle(pe.toPoint());
        // Creates the middle green edge handle
        Geometry geometry = graphComponent.getGraph().getModel().getGeometry(state.getCell());
        List<PointDouble> points = geometry.getPoints();
        java.awt.Point pt = null;
        if (points == null || points.isEmpty()) {
            pt = new java.awt.Point((int) (Math.round(p0.getX()) + Math.round((pe.getX() - p0.getX()) / 2)),
                                    (int) (Math.round(p0.getY()) + Math.round((pe.getY() - p0.getY()) / 2)));
        } else {
            GraphView view = graphComponent.getGraph().getView();
            pt = view.transformControlPoint(state, points.get(0)).toPoint();
        }

        // Create the green middle handle
        h[1] = createHandle(pt);

        // Creates the yellow label handle
        h[3] = createHandle(state.getAbsoluteOffset().toPoint(), Constants.LABEL_HANDLE_SIZE);

        // Makes handle slightly bigger if the yellow label handle
        // exists and intersects this green handle
        if (isHandleVisible(3) && h[1].intersects(h[3])) {
            h[1] = createHandle(pt, Constants.HANDLE_SIZE + 3);
        }

        return h;
    }

    @Override
    protected boolean isFlipEvent(MouseEvent e) {
        return e.getClickCount() == 2 && index == 1;
    }
}
