package com.faforever.neroxis.ngraph.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.awt.*;

@EqualsAndHashCode(callSuper = true)
@Value
public class PaintEvent extends EventObject {
    Graphics graphics;
}
