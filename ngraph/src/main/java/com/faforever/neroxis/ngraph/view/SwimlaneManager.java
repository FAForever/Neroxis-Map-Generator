/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.event.AddCellsEvent;
import com.faforever.neroxis.ngraph.event.CellsResizedEvent;
import com.faforever.neroxis.ngraph.event.EventSource;
import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.style.Style;
import com.faforever.neroxis.ngraph.util.RectangleDouble;

import java.util.List;

/**
 * Manager for swimlanes and nested swimlanes that sets the size of newly added
 * swimlanes to that of their siblings, and propagates changes to the size of a
 * swimlane to its siblings, if siblings is true, and its ancestors, if
 * bubbling is true.
 */
public class SwimlaneManager extends EventSource {
    /**
     * Defines the type of the source or target terminal. The type is a string
     * passed to Cell.is to check if the rule applies to a cell.
     */
    protected Graph graph;
    /**
     * Optional string that specifies the value of the attribute to be passed
     * to Cell.is to check if the rule applies to a cell.
     */
    protected boolean enabled;
    /**
     * Optional string that specifies the attributename to be passed to
     * Cell.is to check if the rule applies to a cell.
     */
    protected boolean horizontal;
    /**
     * Specifies if newly added cells should be resized to match the size of their
     * existing siblings. Default is true.
     */
    protected boolean addEnabled;
    /**
     * Specifies if resizing of swimlanes should be handled. Default is true.
     */
    protected boolean resizeEnabled;
    protected IEventListener<AddCellsEvent> addHandler = (source, evt) -> {
        if (isEnabled() && isAddEnabled()) {
            cellsAdded(evt.getCells());
        }
    };
    protected IEventListener<CellsResizedEvent> resizeHandler = (source, evt) -> {
        if (isEnabled() && isResizeEnabled()) {
            cellsResized(evt.getCells());
        }
    };

    public SwimlaneManager(Graph graph) {
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
    public boolean isHorizontal() {
        return horizontal;
    }

    /**
     * @param value the bubbling to set
     */
    public void setHorizontal(boolean value) {
        horizontal = value;
    }

    /**
     * @return the addEnabled
     */
    public boolean isAddEnabled() {
        return addEnabled;
    }

    /**
     * @param value the addEnabled to set
     */
    public void setAddEnabled(boolean value) {
        addEnabled = value;
    }

    /**
     * @return the resizeEnabled
     */
    public boolean isResizeEnabled() {
        return resizeEnabled;
    }

    /**
     * @param value the resizeEnabled to set
     */
    public void setResizeEnabled(boolean value) {
        resizeEnabled = value;
    }

    /**
     * @return the graph
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * @param graph the graph to set
     */
    public void setGraph(Graph graph) {
        if (this.graph != null) {
            this.graph.removeListener(addHandler);
            this.graph.removeListener(resizeHandler);
        }

        this.graph = graph;

        if (this.graph != null) {
            this.graph.addListener(AddCellsEvent.class, addHandler);
            this.graph.addListener(CellsResizedEvent.class, resizeHandler);
        }
    }

    /**
     * Returns true if the given swimlane should be ignored.
     */
    protected boolean isSwimlaneIgnored(ICell swimlane) {
        return !getGraph().isSwimlane(swimlane);
    }

    /**
     * Returns true if the given cell is horizontal. If the given cell is not a
     * swimlane, then the <horizontal> value is returned.
     */
    protected boolean isCellHorizontal(ICell cell) {
        if (graph.isSwimlane(cell)) {
            CellState state = graph.getView().getState(cell);
            Style style = (state != null) ? state.getStyle() : graph.getCellStyle(cell);
            return style.getCellProperties().isHorizontal();
        }

        return !isHorizontal();
    }

    /**
     * Called if any cells have been added. Calls swimlaneAdded for all swimlanes
     * where isSwimlaneIgnored returns false.
     */
    protected void cellsAdded(List<ICell> cells) {
        if (cells != null) {
            IGraphModel model = getGraph().getModel();

            model.beginUpdate();
            try {
                for (ICell cell : cells) {
                    if (!isSwimlaneIgnored(cell)) {
                        swimlaneAdded(cell);
                    }
                }
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Called for each swimlane which has been added. This finds a reference
     * sibling swimlane and applies its size to the newly added swimlane. If no
     * sibling can be found then the parent swimlane is resized so that the
     * new swimlane fits into the parent swimlane.
     */
    protected void swimlaneAdded(ICell swimlane) {
        IGraphModel model = getGraph().getModel();
        ICell parent = model.getParent(swimlane);
        int childCount = model.getChildCount(parent);
        Geometry geo = null;

        // Finds the first valid sibling swimlane as reference
        for (int i = 0; i < childCount; i++) {
            ICell child = model.getChildAt(parent, i);

            if (child != swimlane && !this.isSwimlaneIgnored(child)) {
                geo = model.getGeometry(child);

                if (geo != null) {
                    break;
                }
            }
        }

        // Applies the size of the refernece to the newly added swimlane
        if (geo != null) {
            boolean parentHorizontal = (parent != null) ? isCellHorizontal(parent) : horizontal;
            resizeSwimlane(swimlane, geo.getWidth(), geo.getHeight(), parentHorizontal);
        }
    }

    /**
     * Called if any cells have been resizes. Calls swimlaneResized for all
     * swimlanes where isSwimlaneIgnored returns false.
     */
    protected void cellsResized(List<ICell> cells) {
        if (cells != null) {
            IGraphModel model = this.getGraph().getModel();

            model.beginUpdate();
            try {
                // Finds the top-level swimlanes and adds offsets
                for (ICell cell : cells) {
                    if (!this.isSwimlaneIgnored(cell)) {
                        Geometry geo = model.getGeometry(cell);

                        if (geo != null) {
                            RectangleDouble size = new RectangleDouble(0, 0, geo.getWidth(), geo.getHeight());
                            ICell top = cell;
                            ICell current = top;

                            while (current != null) {
                                top = current;
                                current = model.getParent(current);
                                RectangleDouble tmp = (graph.isSwimlane(current)) ? graph.getStartSize(
                                        current) : new RectangleDouble();
                                size.setWidth(size.getWidth() + tmp.getWidth());
                                size.setHeight(size.getHeight() + tmp.getHeight());
                            }

                            boolean parentHorizontal = (current != null) ? isCellHorizontal(current) : horizontal;
                            resizeSwimlane(top, size.getWidth(), size.getHeight(), parentHorizontal);
                        }
                    }
                }
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Sets the width or height of the given swimlane to the given value depending
     * on <horizontal>. If <horizontal> is true, then the width is set, otherwise,
     * the height is set.
     */
    protected void resizeSwimlane(ICell swimlane, double w, double h, boolean parentHorizontal) {
        IGraphModel model = getGraph().getModel();

        model.beginUpdate();
        try {
            boolean horizontal = this.isCellHorizontal(swimlane);

            if (!this.isSwimlaneIgnored(swimlane)) {
                Geometry geo = model.getGeometry(swimlane);

                if (geo != null) {

                    if ((parentHorizontal && geo.getHeight() != h) || (!parentHorizontal && geo.getWidth() != w)) {
                        geo = geo.clone();

                        if (parentHorizontal) {
                            geo.setHeight(h);
                        } else {
                            geo.setWidth(w);
                        }

                        model.setGeometry(swimlane, geo);
                    }
                }
            }
            RectangleDouble tmp = (graph.isSwimlane(swimlane)) ? graph.getStartSize(swimlane) : new RectangleDouble();
            w -= tmp.getWidth();
            h -= tmp.getHeight();

            int childCount = model.getChildCount(swimlane);

            for (int i = 0; i < childCount; i++) {
                ICell child = model.getChildAt(swimlane, i);
                resizeSwimlane(child, w, h, horizontal);
            }
        } finally {
            model.endUpdate();
        }
    }

    public void destroy() {
        setGraph(null);
    }
}
