package com.faforever.neroxis.ngraph.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RootChange extends AtomicGraphModelChange {

    /**
     * Holds the new and previous root cell.
     */
    protected ICell root, previous;

    public RootChange(GraphModel model, ICell root) {
        super(model);
        this.root = root;
        previous = root;
    }

    /**
     * Changes the root of the model.
     */
    @Override
    public void execute() {
        root = previous;
        previous = ((GraphModel) model).rootChanged(previous);
    }
}
