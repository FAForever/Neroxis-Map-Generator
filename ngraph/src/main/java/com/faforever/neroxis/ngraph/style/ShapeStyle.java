package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.shape.IShape;
import java.awt.Color;
import lombok.Data;

@Data
public class ShapeStyle {
    private IShape shape;
    private Direction direction = Direction.EAST;
    private Direction gradientDirection = Direction.SOUTH;
    private Color gradientColor;
    private Color fillColor;
    private double rotation;
    private float opacity = 100;
    private float fillOpacity = 100;
    private float strokeOpacity = 100;
    private float strokeWidth = 1;
    private Double arcSize;
    private Color strokeColor;
    private Color separatorColor;
    private boolean startFill = true;
    private boolean endFill = true;
    private boolean flipVertical;
    private boolean flipHorizontal;

    public void copyFrom(ShapeStyle source) {
        shape = source.shape;
        direction = source.direction;
        gradientDirection = source.gradientDirection;
        gradientColor = source.gradientColor;
        fillColor = source.fillColor;
        rotation = source.rotation;
        opacity = source.opacity;
        fillOpacity = source.fillOpacity;
        strokeOpacity = source.strokeOpacity;
        strokeWidth = source.strokeWidth;
        strokeColor = source.strokeColor;
        separatorColor = source.separatorColor;
        startFill = source.startFill;
        endFill = source.endFill;
        flipVertical = source.flipVertical;
        flipHorizontal = source.flipHorizontal;
    }
}
