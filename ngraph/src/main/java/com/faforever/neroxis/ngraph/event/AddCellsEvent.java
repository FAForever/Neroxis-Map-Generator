package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.model.ICell;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Value
public class AddCellsEvent extends EventObject {
    List<ICell> cells;
    ICell parent;
    Integer index;
    ICell source;
    ICell target;
}
