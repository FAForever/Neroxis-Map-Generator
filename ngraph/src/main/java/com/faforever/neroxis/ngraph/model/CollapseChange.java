package com.faforever.neroxis.ngraph.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CollapseChange extends AtomicGraphModelChange {
    protected ICell cell;
    protected boolean collapsed, previous;

    public CollapseChange(GraphModel model, ICell cell, boolean collapsed) {
        super(model);
        this.cell = cell;
        this.collapsed = collapsed;
        this.previous = this.collapsed;
    }

    /**
     * Changes the root of the model.
     */
    @Override
    public void execute() {
        collapsed = previous;
        previous = ((GraphModel) model).collapsedStateForCellChanged(cell, previous);
    }
}
