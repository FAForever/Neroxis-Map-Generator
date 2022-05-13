package com.faforever.neroxis.ngraph.model;

/**
 * Defines the interface for an atomic change of the graph model.
 */
public abstract class AtomicGraphModelChange implements UndoableChange {
    /**
     * Holds the model where the change happened.
     */
    protected IGraphModel model;

    /**
     * Constructs an empty atomic graph model change.
     */
    public AtomicGraphModelChange() {
        this(null);
    }

    /**
     * Constructs an atomic graph model change for the given model.
     */
    public AtomicGraphModelChange(IGraphModel model) {
        this.model = model;
    }

    /**
     * Returns the model where the change happened.
     */
    public IGraphModel getModel() {
        return model;
    }

    /**
     * Sets the model where the change is to be carried out.
     */
    public void setModel(IGraphModel model) {
        this.model = model;
    }
}
