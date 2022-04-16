package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.shape.IShape;
import com.faforever.neroxis.ngraph.shape.ITextShape;
import com.faforever.neroxis.ngraph.style.arrow.Arrow;
import com.faforever.neroxis.ngraph.style.font.FontStyle;
import java.awt.Color;
import lombok.Data;

@Data
public class Style {
    private IShape shape;
    private ITextShape textShape;
    private Arrow marker;
    private FontStyle fontStyle;
    private Direction direction;
    private Direction gradientDirection;
    private Color gradientColor;
    private Color fillColor;
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
    private WhiteSpace whiteSpace;
    private double rotation;
    private float opacity;
    private float fillOpacity;
    private float strokeOpacity;
    private boolean orthogonal;
    private Overflow overflow;
    private Elbow elbow;
    private LabelStyle labelStyle;
    private EdgeStyle edgeStyle;
    private IndicatorStyle indicatorStyle;
    private StencilStyle stencilStyle;
    private ImageStyle imageStyle;
    private PerimeterStyle perimeterStyle;
    private SwimlaneStyle swimlaneStyle;
    private VerticalAlignment verticalAlignment;
    private HorizontalAlignment horizontalAlignment;
    private float strokeWidth;
    private Color strokeColor;
    private Color separatorColor;
    private boolean glass;
    private boolean noEdgeStyle;
    private boolean flipVertical;
    private boolean flipHorizontal;
    /**
     * For edges this determines whether or not joins
     * between edges segments are smoothed to a rounded finish. For vertices
     * that have the rectangle shape, this determines whether or not the
     * rectangle is rounded.
     */
    private boolean shadow;
    private boolean rounded;
    private boolean dashed;
    private boolean startFill;
    private boolean endFill;
    private boolean deletable = true;
    private boolean cloneable = true;
    private boolean resizable = true;
    private boolean movable = true;
    private boolean bendable = true;
    private boolean editable = true;
    private boolean foldable = true;
    private boolean autosize;
    /**
     * This value only applies to vertices. If the shape is swimlane
     * false indicates that the swimlane should be drawn vertically, true indicates to draw it horizontally. If the
     * shape is not a swimlane, this value affects only whether the label is drawn horizontally or vertically.
     */
    private boolean horizontal;
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
    /**
     * Represents the size of the start marker
     * or the size of the swimlane title region depending on the shape it is
     * used for.
     */
    private float startSize;
    /**
     * Represents the size of the end
     * marker in pixels.
     */
    private float endSize;
    /**
     * Represents the size of the horizontal
     * segment of the entity relation style. Default is ENTITY_SEGMENT.
     */
    private float segementSize = 30;
}
