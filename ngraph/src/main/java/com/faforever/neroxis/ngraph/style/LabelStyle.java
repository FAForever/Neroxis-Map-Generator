package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.shape.ITextShape;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;

@Data
public class LabelStyle {
    private WhiteSpace whiteSpace = WhiteSpace.NO_WRAP;
    private Overflow overflow = Overflow.VISIBLE;
    private ITextShape textShape;
    private int topSpacing;
    private int bottomSpacing;
    private int leftSpacing;
    private int rightSpacing;
    private Color borderColor;
    private Color backgroundColor;
    private VerticalAlignment verticalAlignment = VerticalAlignment.MIDDLE;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.CENTER;
    private VerticalAlignment verticalAlignmentPosition = VerticalAlignment.MIDDLE;
    private HorizontalAlignment horizontalAlignmentPosition = HorizontalAlignment.CENTER;
    private boolean visible = true;
    private float textOpacity = 100;
    private Set<FontModifier> fontModifiers = new HashSet<>();
    private int fontSize = 11;
    private String fontFamily = "Arial,Helvetica";
    private Color textColor = Color.BLACK;
}
