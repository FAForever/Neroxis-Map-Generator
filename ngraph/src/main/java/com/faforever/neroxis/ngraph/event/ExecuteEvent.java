package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.model.AtomicGraphModelChange;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Holds the name for the execute event. First and only argument in the
 * argument array is the AtomicGraphChange that has been executed on the
 * model. This event fires before the change event.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ExecuteEvent extends EventObject {
    AtomicGraphModelChange change;
}
