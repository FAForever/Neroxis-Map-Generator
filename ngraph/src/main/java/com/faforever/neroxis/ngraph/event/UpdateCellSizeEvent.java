package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.model.ICell;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class UpdateCellSizeEvent extends EventObject {

    ICell cell;
    boolean ignoreChildren;
}
