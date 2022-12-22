package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.model.ICell;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Value
public class SplitEdgeEvent extends EventObject {
    List<ICell> cells;
    double dx;
    double dy;
    ICell edge;
    ICell newEdge;
}
