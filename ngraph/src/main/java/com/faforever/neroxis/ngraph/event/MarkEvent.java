package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.view.CellState;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Holds the name for the mark event, which fires after a cell has been
 * marked. First and only argument in the array is the cell state that has
 * been marked or null, if no state has been marked.
 * <p>
 * To add a mark listener to the cell marker:
 *
 * <code>
 * addListener(
 * Event.MARK, new EventListener()
 * {
 * public void invoke(Object source, Object[] args)
 * {
 * cellMarked((CellMarker) source, (CellState) args[0]);
 * }
 * });
 * </code>
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class MarkEvent extends EventObject {
    CellState state;
}
