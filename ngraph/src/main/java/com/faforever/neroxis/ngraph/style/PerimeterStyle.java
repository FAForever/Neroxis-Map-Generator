package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.style.perimeter.Perimeter;
import lombok.Data;

@Data
public class PerimeterStyle {
    /**
     * This is the distance between the source
     * connection point of an edge and the perimeter of the source vertex in
     * pixels. This only applies to edges.
     */
    double sourceSpacing;
    /**
     * This is the distance between the target
     * connection of an edge and the perimeter of the target vertex in
     * pixels. This style only applies to edges.
     */
    double targetSpacing;
    /**
     * This is the distance between
     * the connection point and the perimeter in pixels. When used in a vertex
     * style, this applies to all incoming edges to floating ports (edges that
     * terminate on the perimeter of the vertex). When used in an edge style,
     * this spacing applies to the source and target separately, if they
     * terminate in floating ports (on the perimeter of the vertex).
     */
    double vertexSpacing;
    private Perimeter perimeter;
}
