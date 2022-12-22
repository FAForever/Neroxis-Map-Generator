package com.faforever.neroxis.ngraph.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.awt.event.MouseEvent;

@EqualsAndHashCode(callSuper = true)
@Value
public class ContinueEvent extends EventObject {
    Double x;
    Double y;
    Double dx;
    Double dy;
    MouseEvent event;
}
