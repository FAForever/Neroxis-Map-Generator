package com.faforever.neroxis.ngraph.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StyleChange extends AtomicGraphModelChange {
    protected ICell cell;
    protected String style, previous;

    public StyleChange(GraphModel model, ICell cell, String style) {
        super(model);
        this.cell = cell;
        this.style = style;
        this.previous = this.style;
    }

    /**
     * Changes the root of the model.
     */
    @Override
    public void execute() {
        style = previous;
        previous = ((GraphModel) model).styleForCellChanged(cell, style);
    }
}
