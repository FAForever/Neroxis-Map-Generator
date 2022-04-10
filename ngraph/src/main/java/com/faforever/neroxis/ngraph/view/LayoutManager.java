package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.event.BeforeUndoEvent;
import com.faforever.neroxis.ngraph.event.EventSource;
import com.faforever.neroxis.ngraph.event.LayoutCellsEvent;
import com.faforever.neroxis.ngraph.event.MoveCellsEvent;
import com.faforever.neroxis.ngraph.layout.IGraphLayout;
import com.faforever.neroxis.ngraph.model.GraphModel;
import com.faforever.neroxis.ngraph.model.GraphModel.ChildChange;
import com.faforever.neroxis.ngraph.model.GraphModel.GeometryChange;
import com.faforever.neroxis.ngraph.model.GraphModel.RootChange;
import com.faforever.neroxis.ngraph.model.GraphModel.TerminalChange;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.util.UndoableEdit;
import com.faforever.neroxis.ngraph.util.UndoableEdit.UndoableChange;
import com.faforever.neroxis.ngraph.util.Utils;
import java.awt.Point;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implements a layout manager that updates the layout for a given transaction.
 * The following example installs an automatic tree layout in a graph:
 *
 * <code>
 * new LayoutManager(graph) {
 * <p>
 * CompactTreeLayout layout = new CompactTreeLayout(graph);
 * <p>
 * public IGraphLayout getLayout(Object parent)
 * {
 * if (graph.getModel().getChildCount(parent) > 0) {
 * return layout;
 * }
 * return null;
 * }
 * };
 * </code>
 * <p>
 * This class fires the following event:
 * <p>
 * Event.LAYOUT_CELLS fires between begin- and endUpdate after all cells have
 * been layouted in layoutCells. The <code>cells</code> property contains all
 * cells that have been passed to layoutCells.
 */
public class LayoutManager extends EventSource {

    /**
     * Defines the type of the source or target terminal. The type is a string
     * passed to Cell.is to check if the rule applies to a cell.
     */
    protected Graph graph;
    /**
     * Optional string that specifies the value of the attribute to be passed
     * to Cell.is to check if the rule applies to a cell. Default is true.
     */
    protected boolean enabled = true;
    /**
     * Optional string that specifies the attributename to be passed to
     * Cell.is to check if the rule applies to a cell. Default is true.
     */
    protected boolean bubbling = true;
    protected IEventListener<BeforeUndoEvent> undoHandler = (source, evt) -> {
        if (isEnabled()) {
            beforeUndo(evt.getEdit());
        }
    };
    protected IEventListener<MoveCellsEvent> moveHandler = (source, evt) -> {
        if (isEnabled()) {
            cellsMoved(evt.getCells(), evt.getLocation());
        }
    };

    public LayoutManager(Graph graph) {
        setGraph(graph);
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param value the enabled to set
     */
    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * @return the bubbling
     */
    public boolean isBubbling() {
        return bubbling;
    }

    /**
     * @param value the bubbling to set
     */
    public void setBubbling(boolean value) {
        bubbling = value;
    }

    /**
     * @return the graph
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * @param value the graph to set
     */
    public void setGraph(Graph value) {
        if (graph != null) {
            IGraphModel model = graph.getModel();
            model.removeListener(undoHandler);
            graph.removeListener(moveHandler);
        }

        graph = value;

        if (graph != null) {
            IGraphModel model = graph.getModel();
            model.addListener(BeforeUndoEvent.class, undoHandler);
            graph.addListener(MoveCellsEvent.class, moveHandler);
        }
    }

    protected IGraphLayout getLayout(ICell parent) {
        return null;
    }

    protected void cellsMoved(List<ICell> cells, Point location) {
        if (cells != null && location != null) {
            IGraphModel model = getGraph().getModel();

            // Checks if a layout exists to take care of the moving
            for (ICell cell : cells) {
                IGraphLayout layout = getLayout(model.getParent(cell));

                if (layout != null) {
                    layout.moveCell(cell, location.x, location.y);
                }
            }
        }
    }

    protected void beforeUndo(UndoableEdit edit) {
        List<ICell> cells = List.copyOf(getCellsForChanges(edit.getChanges()));
        IGraphModel model = getGraph().getModel();

        if (isBubbling()) {
            List<ICell> tmp = GraphModel.getParents(model, cells);

            while (!tmp.isEmpty()) {
                cells.addAll(tmp);
                tmp = GraphModel.getParents(model, tmp);
            }
        }

        layoutCells(Utils.sortCells(cells, false));
    }

    protected Set<ICell> getCellsForChanges(List<UndoableChange> changes) {
        Set<ICell> result = new HashSet<>();

        for (UndoableChange change : changes) {
            if (change instanceof RootChange) {
                return new HashSet<>();
            } else {
                result.addAll(getCellsForChange(change));
            }
        }

        return result;
    }

    protected Collection<ICell> getCellsForChange(UndoableChange change) {
        IGraphModel model = getGraph().getModel();
        Set<ICell> result = new HashSet<>();

        if (change instanceof ChildChange) {
            ChildChange cc = (ChildChange) change;
            ICell parent = model.getParent(cc.getChild());

            if (cc.getChild() != null) {
                result.add(cc.getChild());
            }

            if (parent != null) {
                result.add(parent);
            }

            if (cc.getPrevious() != null) {
                result.add(cc.getPrevious());
            }
        } else if (change instanceof TerminalChange || change instanceof GeometryChange) {
            ICell cell = (change instanceof TerminalChange) ? ((TerminalChange) change).getCell() : ((GeometryChange) change).getCell();

            if (cell != null) {
                result.add(cell);
                ICell parent = model.getParent(cell);

                if (parent != null) {
                    result.add(parent);
                }
            }
        }

        return result;
    }

    protected void layoutCells(List<ICell> cells) {
        if (!cells.isEmpty()) {
            // Invokes the layouts while removing duplicates
            IGraphModel model = getGraph().getModel();

            model.beginUpdate();
            try {
                for (ICell cell : cells) {
                    if (cell != model.getRoot()) {
                        executeLayout(getLayout(cell), cell);
                    }
                }
                fireEvent(new LayoutCellsEvent(cells));
            } finally {
                model.endUpdate();
            }
        }
    }

    protected void executeLayout(IGraphLayout layout, ICell parent) {
        if (layout != null && parent != null) {
            layout.execute(parent);
        }
    }

    public void destroy() {
        setGraph(null);
    }

}
