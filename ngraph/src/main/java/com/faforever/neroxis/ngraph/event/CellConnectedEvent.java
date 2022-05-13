package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.model.ICell;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class CellConnectedEvent extends EventObject {
    ICell edge;
    ICell terminal;
    ICell previous;
    boolean source;
}
