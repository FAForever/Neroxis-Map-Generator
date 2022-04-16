package com.faforever.neroxis.ngraph.style;

import java.awt.Color;
import lombok.Data;

@Data
public class LabelStyle {
    private Spacing spacing;
    private Color labelBorderColor;
    private Color labelBackgroundColor;
    private VerticalAlignment labelVerticalAlignment;
    private HorizontalAlignment labelHorizontalAlignment;
    private boolean visible;
}
