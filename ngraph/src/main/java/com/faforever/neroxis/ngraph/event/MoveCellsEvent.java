package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.model.ICell;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.awt.*;
import java.util.List;

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
