package com.faforever.neroxis.ngraph.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChildChange extends AtomicGraphModelChange {

    protected ICell parent, previous, child;
    protected int index, previousIndex;

    public ChildChange(GraphModel model, ICell parent, ICell child) {
        this(model, parent, child, 0);
    }

    public ChildChange(GraphModel model, ICell parent, ICell child, int index) {
        super(model);
        this.parent = parent;
        previous = this.parent;
        this.child = child;
        this.index = index;
        previousIndex = index;
    }

    /**
     * Gets the source or target terminal field for the given
     * edge even if the edge is not stored as an incoming or
     * outgoing edge in the respective terminal.
     */
    protected ICell getTerminal(ICell edge, boolean source) {
        return model.getTerminal(edge, source);
    }

    /**
     * Sets the source or target terminal field for the given edge
     * without inserting an incoming or outgoing edge in the
     * respective terminal.
     */
    protected void setTerminal(ICell edge, ICell terminal, boolean source) {
        edge.setTerminal(terminal, source);
    }

    protected void connect(ICell cell, boolean isConnect) {
        ICell source = getTerminal(cell, true);
        ICell target = getTerminal(cell, false);
        if (source != null) {
            if (isConnect) {
                ((GraphModel) model).terminalForCellChanged(cell, source, true);
            } else {
                ((GraphModel) model).terminalForCellChanged(cell, null, true);
            }
        }
        if (target != null) {
            if (isConnect) {
                ((GraphModel) model).terminalForCellChanged(cell, target, false);
            } else {
                ((GraphModel) model).terminalForCellChanged(cell, null, false);
            }
        }
        // Stores the previous terminals in the edge
        setTerminal(cell, source, true);
        setTerminal(cell, target, false);
        int childCount = model.getChildCount(cell);
        for (int i = 0; i < childCount; i++) {
            connect(model.getChildAt(cell, i), isConnect);
        }
    }

    /**
     * Returns the index of the given child inside the given parent.
     */
    protected int getChildIndex(Object parent, Object child) {
        return (parent instanceof ICell && child instanceof ICell) ? ((ICell) parent).getIndex((ICell) child) : 0;
    }

    /**
     * Changes the root of the model.
     */
    @Override
    public void execute() {
        ICell tmp = model.getParent(child);
        int tmp2 = getChildIndex(tmp, child);
        if (previous == null) {
            connect(child, false);
        }
        tmp = ((GraphModel) model).parentForCellChanged(child, previous, previousIndex);
        if (previous != null) {
            connect(child, true);
        }
        parent = previous;
        previous = tmp;
        index = previousIndex;
        previousIndex = tmp2;
    }
}
