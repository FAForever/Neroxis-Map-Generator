package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.view.CellState;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class CellStateEvent extends EventObject {
    CellState cellState;
}
