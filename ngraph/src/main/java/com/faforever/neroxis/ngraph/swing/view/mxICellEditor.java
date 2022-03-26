/**
 * Copyright (c) 2008, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.swing.view;

import java.util.EventObject;

/**
 *
 */
public interface mxICellEditor {

    /**
     * Returns the cell that is currently being edited.
     */
    Object getEditingCell();

    /**
     * Starts editing the given cell.
     */
    void startEditing(Object cell, EventObject trigger);

    /**
     * Stops the current editing.
     */
    void stopEditing(boolean cancel);

}
