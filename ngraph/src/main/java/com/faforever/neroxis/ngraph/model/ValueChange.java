package com.faforever.neroxis.ngraph.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ValueChange extends AtomicGraphModelChange {

    protected ICell cell;
    protected Object value;
    protected Object previous;

    public ValueChange(GraphModel model, ICell cell, Object value) {
        super(model);
        this.cell = cell;
        this.value = value;
        this.previous = this.value;
    }

    /**
     * Changes the root of the model.
     */
    @Override
    public void execute() {
        value = previous;
        previous = ((GraphModel) model).valueForCellChanged(cell, previous);
    }
}
