package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.event.MouseEvent;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class StartEvent extends EventObject {
    CellState state;
    MouseEvent event;
}
