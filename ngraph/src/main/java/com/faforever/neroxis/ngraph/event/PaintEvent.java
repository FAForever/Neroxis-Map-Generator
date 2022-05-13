package com.faforever.neroxis.ngraph.event;

import java.awt.Graphics;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class PaintEvent extends EventObject {
    Graphics graphics;
}
