/**
 * Copyright (c) 2007-2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.util;

import com.faforever.neroxis.ngraph.model.UndoableChange;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class UndoableEdit {
    /**
     * Holds the source of the undoable edit.
     */
    protected final Object source;
    /**
     * Holds the list of changes that make up this undoable edit.
     */
    protected final List<UndoableChange> changes = new ArrayList<>();
    /**
     * Specifies this undoable edit is significant. Default is true.
     */
    protected boolean significant;
    /**
     * Specifies the state of the undoable edit.
     */
    protected boolean undone, redone;

    /**
     * Constructs a new undoable edit for the given source.
     */
    public UndoableEdit(Object source) {
        this(source, true);
    }

    /**
     * Constructs a new undoable edit for the given source.
     */
    public UndoableEdit(Object source, boolean significant) {
        this.source = source;
        this.significant = significant;
    }

    /**
     * Hook to free resources after the edit has been removed from the command
     * history. This implementation is empty.
     */
    public void die() {
        // empty
    }

    /**
     * Returns true if this edit contains no changes.
     */
    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /**
     * Adds the specified change to this edit. The change is an object that is
     * expected to either have an undo and redo, or an execute function.
     */
    public void add(UndoableChange change) {
        changes.add(change);
    }

    public void undo() {
        if (!undone) {
            int count = changes.size();

            for (int i = count - 1; i >= 0; i--) {
                UndoableChange change = changes.get(i);
                change.execute();
            }

            undone = true;
            redone = false;
        }

        dispatch();
    }

    /**
     * Hook to notify any listeners of the changes after an undo or redo
     * has been carried out. This implementation is empty.
     */
    public void dispatch() {
        // empty
    }

    public void redo() {
        if (!redone) {
            for (UndoableChange change : changes) {
                change.execute();
            }

            undone = false;
            redone = true;
        }

        dispatch();
    }
}
