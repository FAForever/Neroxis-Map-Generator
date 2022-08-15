package com.faforever.neroxis.ngraph.style.perimeter;

import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.CellState;

/**
 * Defines the requirements for a perimeter function.
 */
public interface Perimeter {
    /**
     * Implements a perimeter function.
     *
     * @param bounds     Rectangle that represents the absolute bounds of the
     *                   vertex.
     * @param vertex     Cell state that represents the vertex.
     * @param next       Point that represents the nearest neighbour point on the
     *                   given edge.
     * @param orthogonal Boolean that specifies if the orthogonal projection onto
     *                   the perimeter should be returned. If this is false then the intersection
     *                   of the perimeter and the line between the next and the center point is
     *                   returned.
     * @return Returns the perimeter point.
     */
    PointDouble apply(RectangleDouble bounds, CellState vertex, PointDouble next, boolean orthogonal);
}
