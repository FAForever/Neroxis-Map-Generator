package com.faforever.neroxis.ngraph.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.awt.*;

@EqualsAndHashCode(callSuper = true)
@Value
public class BeforePaintEvent extends EventObject {
    Graphics graphics;
}
