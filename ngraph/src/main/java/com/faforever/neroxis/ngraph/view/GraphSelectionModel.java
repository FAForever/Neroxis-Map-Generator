/*
 * Copyright (c) 2001-2005, Gaudenz Alder
 *
 * All rights reserved.
 *
 * See LICENSE file for license details. If you are unable to locate
 * this file please contact info (at) jgraph (dot) com.
 */
package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.event.ChangeEvent;
import com.faforever.neroxis.ngraph.event.EventSource;
import com.faforever.neroxis.ngraph.event.UndoEvent;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.UndoableChange;
import com.faforever.neroxis.ngraph.util.UndoableEdit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Implements the selection model for a graph.
 * <p>
 * This class fires the following events:
 * <p>
 * Event.UNDO fires after the selection was changed in changeSelection. The
 * <code>edit</code> property contains the UndoableEdit which contains the
 * SelectionChange.
 * <p>
 * Event.CHANGE fires after the selection changes by executing an
 * SelectionChange. The <code>added</code> and <code>removed</code>
 * properties contain Collections of cells that have been added to or removed
 * from the selection, respectively.
 * <p>
 * NOTE: Due to a historic bug that cannot be changed at this point the
 * names of the properties are "reversed".
 * <p>
 * To add a change listener to the graph selection model:
 *
 * <code>
 * addListener(
 * Event.CHANGE, new IEventListener()
 * {
 * public void invoke(Object sender, EventObject evt)
 * {
 * GraphSelectionModel model = (SelectionModel) sender;
 * Collection added = (Collection) evt.getProperty("added");
 * Collection removed = (Collection) evt.getProperty("removed");
 * selectionChanged(model, added, removed);
 * }
 * });
 * </code>
 */
public class GraphSelectionModel extends EventSource {

    /**
     * Reference to the enclosing graph.
     */
    protected Graph graph;

    /**
     * Specifies if only one selected item at a time is allowed.
     * Default is false.
     */
    protected boolean singleSelection = false;

    /**
     * Holds the selection cells.
     */
    protected Set<ICell> cells = new LinkedHashSet<>();

    /**
     * Constructs a new selection model for the specified graph.
     *
     */
    public GraphSelectionModel(Graph graph) {
        this.graph = graph;
    }

    /**
     * @return the singleSelection
     */
    public boolean isSingleSelection() {
        return singleSelection;
    }

    /**
     * @param singleSelection the singleSelection to set
     */
    public void setSingleSelection(boolean singleSelection) {
        this.singleSelection = singleSelection;
    }

    /**
     * Returns true if the given cell is selected.
     *
     * @return Returns true if the given cell is selected.
     */
    public boolean isSelected(Object cell) {
        return cell != null && cells.contains(cell);
    }

    /**
     * Returns true if no cells are selected.
     */
    public boolean isEmpty() {
        return cells.isEmpty();
    }

    /**
     * Returns the number of selected cells.
     */
    public int size() {
        return cells.size();
    }

    /**
     * Clears the selection.
     */
    public void clear() {
        changeSelection(null, cells);
    }

    /**
     * Returns the first selected cell.
     */
    public ICell getCell() {
        return (cells.isEmpty()) ? null : cells.iterator().next();
    }

    /**
     * Clears the selection and adds the given cell to the selection.
     */
    public void setCell(ICell cell) {
        if (cell != null) {
            setCells(List.of(cell));
        } else {
            clear();
        }
    }

    /**
     * Returns the selection cells.
     */
    public Set<ICell> getCells() {
        return cells;
    }

    /**
     * Clears the selection and adds the given cells.
     */
    public void setCells(List<ICell> cells) {
        if (cells != null) {
            if (singleSelection) {
                cells = List.of(getFirstSelectableCell(cells));
            }

            List<ICell> tmp = new ArrayList<>(cells.size());

            for (ICell cell : cells) {
                if (graph.isCellSelectable(cell)) {
                    tmp.add(cell);
                }
            }

            changeSelection(tmp, this.cells);
        } else {
            clear();
        }
    }

    /**
     * Returns the first selectable cell in the given array of cells.
     *
     * @param cells Array of cells to return the first selectable cell for.
     * @return Returns the first cell that may be selected.
     */
    protected ICell getFirstSelectableCell(List<ICell> cells) {
        if (cells != null) {
            return cells.stream().filter(graph::isCellSelectable).findFirst().orElse(null);
        }

        return null;
    }

    /**
     * Adds the given cell to the selection.
     */
    public void addCell(ICell cell) {
        if (cell != null) {
            addCells(List.of(cell));
        }
    }

    public void addCells(List<ICell> cells) {
        if (cells != null) {
            Collection<ICell> remove = null;

            if (singleSelection) {
                remove = this.cells;
                cells = List.of(getFirstSelectableCell(cells));
            }

            List<ICell> tmp = new ArrayList<>(cells.size());

            for (ICell cell : cells) {
                if (!isSelected(cell) && graph.isCellSelectable(cell)) {
                    tmp.add(cell);
                }
            }

            changeSelection(tmp, remove);
        }
    }

    /**
     * Removes the given cell from the selection.
     */
    public void removeCell(ICell cell) {
        if (cell != null) {
            removeCells(List.of(cell));
        }
    }

    public void removeCells(List<ICell> cells) {
        if (cells != null) {
            List<ICell> tmp = new ArrayList<>(cells.size());

            for (ICell cell : cells) {
                if (isSelected(cell)) {
                    tmp.add(cell);
                }
            }

            changeSelection(null, tmp);
        }
    }

    protected void changeSelection(Collection<ICell> added, Collection<ICell> removed) {
        if ((added != null && !added.isEmpty()) || (removed != null && !removed.isEmpty())) {
            SelectionChange change = new SelectionChange(this, added, removed);
            change.execute();
            UndoableEdit edit = new UndoableEdit(this, false);
            edit.add(change);
            fireEvent(new UndoEvent(edit));
        }
    }

    protected void cellAdded(ICell cell) {
        if (cell != null) {
            cells.add(cell);
        }
    }

    protected void cellRemoved(ICell cell) {
        if (cell != null) {
            cells.remove(cell);
        }
    }

    public static class SelectionChange implements UndoableChange {
        protected GraphSelectionModel model;
        protected List<ICell> added, removed;

        public SelectionChange(GraphSelectionModel model, Collection<ICell> added, Collection<ICell> removed) {
            this.model = model;
            this.added = (added != null) ? new ArrayList<>(added) : null;
            this.removed = (removed != null) ? new ArrayList<>(removed) : null;
        }

        public void execute() {
            if (removed != null) {

                for (ICell o : removed) {
                    model.cellRemoved(o);
                }
            }

            if (added != null) {

                for (ICell iCell : added) {
                    model.cellAdded(iCell);
                }
            }

            model.fireEvent(new ChangeEvent(null, null, added, removed));
        }

    }

}
