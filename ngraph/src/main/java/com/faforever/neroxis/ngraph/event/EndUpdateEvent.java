package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.util.UndoableEdit;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Holds the name for the endUpdate event. This event has no arguments and fires
 * after the updateLevel has been changed in the model. First argument is the
 * currentEdit.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class EndUpdateEvent extends EventObject {

    UndoableEdit edit;
}
