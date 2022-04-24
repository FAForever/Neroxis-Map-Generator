package com.faforever.neroxis.ngraph.style.edge;

import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import java.util.List;

/**
 * Defines the requirements for an edge style function.
 */
public interface EdgeStyleFunction {

    /**
     * Implements an edge style function. At the time the function is called, the result
     * array contains a placeholder (null) for the first absolute point,
     * that is, the point where the edge and source terminal are connected.
     * The implementation of the style then adds all intermediate waypoints
     * except for the last point, that is, the connection point between the
     * edge and the target terminal. The first ant the last point in the
     * result array are then replaced with Points that take into account
     * the terminal's perimeter and next point on the edge.
     *
     * @param state  Cell state that represents the edge to be updated.
     * @param source Cell state that represents the source terminal.
     * @param target Cell state that represents the target terminal.
     * @param points List of relative control points.
     * @param result Array of points that represent the actual points of the
     *               edge.
     */
    void apply(CellState state, CellState source, CellState target, List<PointDouble> points, List<PointDouble> result);
}
