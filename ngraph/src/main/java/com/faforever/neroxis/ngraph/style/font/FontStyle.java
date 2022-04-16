package com.faforever.neroxis.ngraph.style.font;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;

@Data
public class FontStyle {
    private final Set<FontModifier> fontModifiers = new HashSet<>();
    private int fontStyle;
    private String fontFamily;
    private Color fontColor;
}
