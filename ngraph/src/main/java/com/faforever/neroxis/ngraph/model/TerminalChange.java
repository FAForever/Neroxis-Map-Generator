package com.faforever.neroxis.ngraph.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TerminalChange extends AtomicGraphModelChange {

    protected ICell cell, terminal, previous;
    protected boolean source;

    public TerminalChange(GraphModel model, ICell cell, ICell terminal, boolean source) {
        super(model);
        this.cell = cell;
        this.terminal = terminal;
        this.previous = this.terminal;
        this.source = source;
    }

    /**
     * Changes the root of the model.
     */
    @Override
    public void execute() {
        terminal = previous;
        previous = ((GraphModel) model).terminalForCellChanged(cell, previous, source);
    }
}
