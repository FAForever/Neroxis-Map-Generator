package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.util.UndoableEdit;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class RedoEvent extends EventObject {

    UndoableEdit edit;
}
