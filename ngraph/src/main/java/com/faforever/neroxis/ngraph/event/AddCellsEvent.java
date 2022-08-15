package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.model.ICell;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class AddCellsEvent extends EventObject {
    List<ICell> cells;
    ICell parent;
    Integer index;
    ICell source;
    ICell target;
}
