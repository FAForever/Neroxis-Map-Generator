package com.faforever.neroxis.ngraph.style;

import java.awt.Color;
import lombok.Data;

@Data
public class ImageStyle {
    private String image;
    private int width;
    private int height;
    private Color backgroundColor;
    private Color borderColor;
    private boolean flipHorizontal;
    private boolean flipVertical;
    private VerticalAlignment verticalAlignment = VerticalAlignment.MIDDLE;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.CENTER;
}
