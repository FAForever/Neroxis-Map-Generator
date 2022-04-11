package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.UndoableChange;
import com.faforever.neroxis.ngraph.util.UndoableEdit;
import java.util.Collection;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Holds the name for the change event. First and only argument in the
 * argument array is the list of AtomicGraphChanges that have been
 * executed on the model.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ChangeEvent extends EventObject {
    UndoableEdit edit;
    List<UndoableChange> changes;
    Collection<ICell> added;
    Collection<ICell> removed;
}
