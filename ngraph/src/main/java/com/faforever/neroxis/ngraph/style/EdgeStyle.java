package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.style.arrow.Arrow;
import com.faforever.neroxis.ngraph.style.edge.EdgeStyleFunction;
import java.util.Set;
import lombok.Data;

@Data
public class EdgeStyle {
    private boolean noEdgeStyle;
    private EdgeStyleFunction edgeStyleFunction;
    private EdgeStyleFunction loopStyleFunction;
    private Elbow elbow;
    private Arrow startArrow;
    private Arrow endArrow;
    private boolean dashed;
    /**
     * Represents the size of the horizontal
     * segment of the entity relation style. Default is ENTITY_SEGMENT.
     */
    private float segmentSize = 30;
    /**
     * Represents the size of the start marker
     * or the size of the swimlane title region depending on the shape it is
     * used for.
     */
    private float startSize = 40;
    /**
     * Represents the size of the end
     * marker in pixels.
     */
    private float endSize = 6;
    /**
     * Defines the cell that should be used for computing the
     * perimeter point of the source for an edge. This allows for graphically
     * connecting to a cell while keeping the actual terminal of the edge.
     */
    private ICell sourcePort;
    /**
     * Defines the cell that should be used for computing the
     * perimeter point of the target for an edge. This allows for graphically
     * connecting to a cell while keeping the actual terminal of the edge.
     */
    private ICell targetPort;
    /**
     * Defines if the perimeter should be used to find the exact entry point
     * along the perimeter of the source.
     */
    private boolean entryPerimeter = true;
    /**
     * Defines the horizontal relative coordinate connection point
     * of an edge with its target terminal.
     */
    private Float entryX;
    /**
     * Defines the vertical relative coordinate connection point
     * of an edge with its target terminal.
     */
    private Float entryY;
    /**
     * Defines if the perimeter should be used to find the exact entry point
     * along the perimeter of the target.
     */
    private boolean exitPerimeter = true;
    /**
     * Defines the horizontsl relative coordinate connection point
     * of an edge with its source terminal.
     */
    private Float exitX;
    /**
     * Defines the vertical relative coordinate connection point
     * of an edge with its source terminal.
     */
    private Float exitY;
    /**
     * Specifies the dashed pattern to apply to edges drawn with this style. This style allows the user
     * to specify a custom-defined dash pattern. This is done using a series
     * of numbers. Dash styles are defined in terms of the length of the dash
     * (the drawn part of the stroke) and the length of the space between the
     * dashes. The lengths are relative to the line width: a length of "1" is
     * equal to the line width.
     */
    private float[] dashPattern = new float[]{3f, 3f};
    /**
     * Defines the direction(s) that edges are allowed to connect to cells in.
     * Possible values are <code>DIRECTION_NORTH, DIRECTION_SOUTH,
     * DIRECTION_EAST</code> and <code>DIRECTION_WEST</code>.
     */
    private Set<Direction> portConstraints;
    /**
     * This is the relative horizaontal offset from the center used for connecting edges.
     * Possible values are between -0.5 and 0.5.
     */
    private float routingCenterX;
    /**
     * This is the relative vertical offset from the center used for connecting edges.
     * Possible values are between -0.5 and 0.5.
     */
    private float routingCenterY;
}
