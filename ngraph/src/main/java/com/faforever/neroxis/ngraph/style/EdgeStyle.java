package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.style.arrow.Arrow;
import com.faforever.neroxis.ngraph.style.edge.EdgeStyleFunction;
import lombok.Data;

@Data
public class EdgeStyle {
    private EdgeStyleFunction edgeStyleFunction;
    private EdgeStyleFunction loopStyleFunction;
    private Arrow startArrow;
    private Arrow endArrow;
    /**
     * Defines if the perimeter should be used to find the exact entry point
     * along the perimeter of the source.
     */
    private boolean entryPerimeter = true;
    /**
     * Defines the horizontal relative coordinate connection point
     * of an edge with its target terminal.
     */
    private float entryX;
    /**
     * Defines the vertical relative coordinate connection point
     * of an edge with its target terminal.
     */
    private float entryY;
    /**
     * Defines if the perimeter should be used to find the exact entry point
     * along the perimeter of the target.
     */
    private boolean exitPerimeter = true;
    /**
     * Defines the horizontsl relative coordinate connection point
     * of an edge with its source terminal.
     */
    private float exitX;
    /**
     * Defines the vertical relative coordinate connection point
     * of an edge with its source terminal.
     */
    private float exitY;
    /**
     * Specifies the dashed pattern to apply to edges drawn with this style. This style allows the user
     * to specify a custom-defined dash pattern. This is done using a series
     * of numbers. Dash styles are defined in terms of the length of the dash
     * (the drawn part of the stroke) and the length of the space between the
     * dashes. The lengths are relative to the line width: a length of "1" is
     * equal to the line width.
     */
    private float[] dashPattern;
    /**
     * Defines the direction(s) that edges are allowed to connect to cells in.
     * Possible values are <code>DIRECTION_NORTH, DIRECTION_SOUTH,
     * DIRECTION_EAST</code> and <code>DIRECTION_WEST</code>.
     */
    private Direction portConstraint;
}
