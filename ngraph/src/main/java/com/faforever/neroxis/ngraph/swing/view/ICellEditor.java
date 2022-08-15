/**
 * Copyright (c) 2008, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.swing.view;

import com.faforever.neroxis.ngraph.model.ICell;
import java.util.EventObject;

/**
 *
 */
public interface ICellEditor {
    /**
     * Returns the cell that is currently being edited.
     */
    Object getEditingCell();

    /**
     * Starts editing the given cell.
     */
    void startEditing(ICell cell, EventObject trigger);

    /**
     * Stops the current editing.
     */
    void stopEditing(boolean cancel);
}
