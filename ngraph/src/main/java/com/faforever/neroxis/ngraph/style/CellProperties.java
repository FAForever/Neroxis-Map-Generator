package com.faforever.neroxis.ngraph.style;

import lombok.Data;

@Data
public class CellProperties {
    private boolean orthogonal = true;
    private boolean glass;
    private boolean shadow;
    private boolean rounded;
    private boolean dashed;
    private boolean deletable = true;
    private boolean cloneable = true;
    private boolean resizable = true;
    private boolean movable = true;
    private boolean bendable = true;
    private boolean editable = true;
    private boolean foldable = true;
    private boolean autosize = true;
    /**
     * This value only applies to vertices. If the shape is swimlane
     * false indicates that the swimlane should be drawn vertically, true indicates to draw it horizontally. If the
     * shape is not a swimlane, this value affects only whether the label is drawn horizontally or vertically.
     */
    private boolean horizontal = true;
}
