package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.view.CellState;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.awt.event.MouseEvent;

@EqualsAndHashCode(callSuper = true)
@Value
public class StartEvent extends EventObject {
    CellState state;
    MouseEvent event;
}
