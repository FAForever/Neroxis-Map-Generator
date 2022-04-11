package com.faforever.neroxis.ngraph.model;

/**
 * Defines the requirements for an undoable change.
 */
public interface UndoableChange {
    /**
     * Undoes or redoes the change depending on its undo state.
     */
    void execute();
}
