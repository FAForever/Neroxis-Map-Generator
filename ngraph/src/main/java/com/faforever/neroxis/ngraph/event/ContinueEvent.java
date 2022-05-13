package com.faforever.neroxis.ngraph.event;

import java.awt.event.MouseEvent;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class ContinueEvent extends EventObject {
    Double x;
    Double y;
    Double dx;
    Double dy;
    MouseEvent event;
}
