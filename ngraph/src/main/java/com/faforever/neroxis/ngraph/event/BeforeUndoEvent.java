package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.util.UndoableEdit;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Holds the name for the beforeUndo event. First and only argument in the
 * argument array is the current edit that is currently in progress in the
 * model. This event fires before notify is called on the currentEdit in
 * the model.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class BeforeUndoEvent extends EventObject {
    UndoableEdit edit;
}
