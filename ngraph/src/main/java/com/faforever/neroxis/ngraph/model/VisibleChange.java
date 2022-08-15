package com.faforever.neroxis.ngraph.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VisibleChange extends AtomicGraphModelChange {
    protected ICell cell;
    protected boolean visible, previous;

    public VisibleChange(GraphModel model, ICell cell, boolean visible) {
        super(model);
        this.cell = cell;
        this.visible = visible;
        this.previous = this.visible;
    }

    /**
     * Changes the root of the model.
     */
    @Override
    public void execute() {
        visible = previous;
        previous = ((GraphModel) model).visibleStateForCellChanged(cell, previous);
    }
}
