package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.model.ICell;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class LabelChangedEvent extends EventObject {
    ICell cell;
    Object value;
    java.util.EventObject event;
}
