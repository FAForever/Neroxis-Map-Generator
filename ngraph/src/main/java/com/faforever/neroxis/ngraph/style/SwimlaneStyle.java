package com.faforever.neroxis.ngraph.style;

import java.awt.Color;
import lombok.Data;

@Data
public class SwimlaneStyle {
    /**
     * Specifies whether the line between the title region of a swimlane should be visible.
     */
    private boolean line = true;
    private Color color;
}
