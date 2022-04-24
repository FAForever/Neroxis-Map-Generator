package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.model.ICell;
import java.awt.Point;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class MoveCellsEvent extends EventObject {

    List<ICell> cells;
    double dx;
    double dy;
    boolean clone;
    ICell target;
    Point location;
}
