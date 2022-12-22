package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Value
public class ResizeCellsEvent extends EventObject {
    List<ICell> cells;
    RectangleDouble[] bounds;
}
