package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.shape.IShape;
import java.awt.Color;
import lombok.Data;

@Data
public class IndicatorStyle {
    private String image;
    private Color color;
    private Color gradientColor;
    private int spacing;
    private int width;
    private int height;
    private IShape shape;
}
