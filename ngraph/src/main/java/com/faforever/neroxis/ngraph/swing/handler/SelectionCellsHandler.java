/**
 * Copyright (c) 2008, Gaudenz Alder
 * <p>
 * Known issue: Drag image size depends on the initial position and may sometimes
 * not align with the grid when dragging. This is because the rounding of the width
 * and height at the initial position may be different than that at the current
 * position as the left and bottom side of the shape must align to the grid lines.
 */
package com.faforever.neroxis.ngraph.swing.handler;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.util.Event;
import com.faforever.neroxis.ngraph.util.EventObject;
import com.faforever.neroxis.ngraph.util.EventSource.IEventListener;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class SelectionCellsHandler implements MouseListener, MouseMotionListener {

    /**
     *
     */
    private static final long serialVersionUID = -882368002120921842L;

    /**
     * Defines the default value for maxHandlers. Default is 100.
     */
    public static int DEFAULT_MAX_HANDLERS = 100;

    /**
     * Reference to the enclosing graph component.
     */
    protected GraphComponent graphComponent;

    /**
     * Specifies if this handler is enabled.
     */
    protected boolean enabled = true;

    /**
     * Specifies if this handler is visible.
     */
    protected boolean visible = true;

    /**
     * Reference to the enclosing graph component.
     */
    protected Rectangle bounds = null;

    /**
     * Defines the maximum number of handlers to paint individually.
     * Default is DEFAULT_MAX_HANDLES.
     */
    protected int maxHandlers = DEFAULT_MAX_HANDLERS;

    /**
     * Maps from cells to handlers in the order of the selection cells.
     */
    protected transient LinkedHashMap<Object, CellHandler> handlers = new LinkedHashMap<Object, CellHandler>();

    /**
     *
     */
    protected transient IEventListener refreshHandler = new IEventListener() {
        public void invoke(Object source, EventObject evt) {
            if (isEnabled()) {
                refresh();
            }
        }
    };

    /**
     *
     */
    protected transient PropertyChangeListener labelMoveHandler = new PropertyChangeListener() {

        /*
         * (non-Javadoc)
         * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
         */
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("vertexLabelsMovable") || evt.getPropertyName().equals("edgeLabelsMovable")) {
                refresh();
            }
        }

    };

    /**
     * @param graphComponent
     */
    public SelectionCellsHandler(final GraphComponent graphComponent) {
        this.graphComponent = graphComponent;

        // Listens to all mouse events on the rendering control
        graphComponent.getGraphControl().addMouseListener(this);
        graphComponent.getGraphControl().addMouseMotionListener(this);

        // Installs the graph listeners and keeps them in sync
        addGraphListeners(graphComponent.getGraph());

        graphComponent.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("graph")) {
                    removeGraphListeners((Graph) evt.getOldValue());
                    addGraphListeners((Graph) evt.getNewValue());
                }
            }
        });

        // Installs the paint handler
        graphComponent.addListener(Event.PAINT, new IEventListener() {
            public void invoke(Object sender, EventObject evt) {
                Graphics g = (Graphics) evt.getProperty("g");
                paintHandles(g);
            }
        });
    }

    /**
     * Installs the listeners to update the handles after any changes.
     */
    protected void addGraphListeners(Graph graph) {
        // LATER: Install change listener for graph model, selection model, view
        if (graph != null) {
            graph.getSelectionModel().addListener(Event.CHANGE, refreshHandler);
            graph.getModel().addListener(Event.CHANGE, refreshHandler);
            graph.getView().addListener(Event.SCALE, refreshHandler);
            graph.getView().addListener(Event.TRANSLATE, refreshHandler);
            graph.getView().addListener(Event.SCALE_AND_TRANSLATE, refreshHandler);
            graph.getView().addListener(Event.DOWN, refreshHandler);
            graph.getView().addListener(Event.UP, refreshHandler);

            // Refreshes the handles if moveVertexLabels or moveEdgeLabels changes
            graph.addPropertyChangeListener(labelMoveHandler);
        }
    }

    /**
     * Removes all installed listeners.
     */
    protected void removeGraphListeners(Graph graph) {
        if (graph != null) {
            graph.getSelectionModel().removeListener(refreshHandler, Event.CHANGE);
            graph.getModel().removeListener(refreshHandler, Event.CHANGE);
            graph.getView().removeListener(refreshHandler, Event.SCALE);
            graph.getView().removeListener(refreshHandler, Event.TRANSLATE);
            graph.getView().removeListener(refreshHandler, Event.SCALE_AND_TRANSLATE);
            graph.getView().removeListener(refreshHandler, Event.DOWN);
            graph.getView().removeListener(refreshHandler, Event.UP);

            // Refreshes the handles if moveVertexLabels or moveEdgeLabels changes
            graph.removePropertyChangeListener(labelMoveHandler);
        }
    }

    /**
     *
     */
    public GraphComponent getGraphComponent() {
        return graphComponent;
    }

    /**
     *
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     *
     */
    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     *
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     *
     */
    public void setVisible(boolean value) {
        visible = value;
    }

    /**
     *
     */
    public int getMaxHandlers() {
        return maxHandlers;
    }

    /**
     *
     */
    public void setMaxHandlers(int value) {
        maxHandlers = value;
    }

    /**
     *
     */
    public CellHandler getHandler(Object cell) {
        return handlers.get(cell);
    }

    /**
     * Dispatches the mousepressed event to the subhandles. This is
     * called from the connection handler as subhandles have precedence
     * over the connection handler.
     */
    public void mousePressed(MouseEvent e) {
        if (graphComponent.isEnabled() && !graphComponent.isForceMarqueeEvent(e) && isEnabled()) {
            Iterator<CellHandler> it = handlers.values().iterator();

            while (it.hasNext() && !e.isConsumed()) {
                it.next().mousePressed(e);
            }
        }
    }

    /**
     *
     */
    public void mouseMoved(MouseEvent e) {
        if (graphComponent.isEnabled() && isEnabled()) {
            Iterator<CellHandler> it = handlers.values().iterator();

            while (it.hasNext() && !e.isConsumed()) {
                it.next().mouseMoved(e);
            }
        }
    }

    /**
     *
     */
    public void mouseDragged(MouseEvent e) {
        if (graphComponent.isEnabled() && isEnabled()) {
            Iterator<CellHandler> it = handlers.values().iterator();

            while (it.hasNext() && !e.isConsumed()) {
                it.next().mouseDragged(e);
            }
        }
    }

    /**
     *
     */
    public void mouseReleased(MouseEvent e) {
        if (graphComponent.isEnabled() && isEnabled()) {
            Iterator<CellHandler> it = handlers.values().iterator();

            while (it.hasNext() && !e.isConsumed()) {
                it.next().mouseReleased(e);
            }
        }

        reset();
    }

    /**
     * Redirects the tooltip handling of the JComponent to the graph
     * component, which in turn may use getHandleToolTipText in this class to
     * find a tooltip associated with a handle.
     */
    public String getToolTipText(MouseEvent e) {
        MouseEvent tmp = SwingUtilities.convertMouseEvent(e.getComponent(), e, graphComponent.getGraphControl());
        Iterator<CellHandler> it = handlers.values().iterator();
        String tip = null;

        while (it.hasNext() && tip == null) {
            tip = it.next().getToolTipText(tmp);
        }

        return tip;
    }

    /**
     *
     */
    public void reset() {
        Iterator<CellHandler> it = handlers.values().iterator();

        while (it.hasNext()) {
            it.next().reset();
        }
    }

    /**
     *
     */
    public void refresh() {
        Graph graph = graphComponent.getGraph();

        // Creates a new map for the handlers and tries to
        // to reuse existing handlers from the old map
        LinkedHashMap<Object, CellHandler> oldHandlers = handlers;
        handlers = new LinkedHashMap<Object, CellHandler>();

        // Creates handles for all selection cells
        ICell[] tmp = graph.getSelectionCells();
        boolean handlesVisible = tmp.length <= getMaxHandlers();
        Rectangle handleBounds = null;

        for (ICell o : tmp) {
            CellState state = graph.getView().getState(o);

            if (state != null && state.getCell() != graph.getView().getCurrentRoot()) {
                CellHandler handler = oldHandlers.remove(o);

                if (handler != null) {
                    handler.refresh(state);
                } else {
                    handler = graphComponent.createHandler(state);
                }

                if (handler != null) {
                    handler.setHandlesVisible(handlesVisible);
                    handlers.put(o, handler);
                    Rectangle bounds = handler.getBounds();
                    Stroke stroke = handler.getSelectionStroke();

                    if (stroke != null) {
                        bounds = stroke.createStrokedShape(bounds).getBounds();
                    }

                    if (handleBounds == null) {
                        handleBounds = bounds;
                    } else {
                        handleBounds.add(bounds);
                    }
                }
            }
        }

        for (CellHandler handler : oldHandlers.values()) {
            handler.destroy();
        }

        Rectangle dirty = bounds;

        if (handleBounds != null) {
            if (dirty != null) {
                dirty.add(handleBounds);
            } else {
                dirty = handleBounds;
            }
        }

        if (dirty != null) {
            graphComponent.getGraphControl().repaint(dirty);
        }

        // Stores current bounds for later use
        bounds = handleBounds;
    }

    /**
     *
     */
    public void paintHandles(Graphics g) {
        Iterator<CellHandler> it = handlers.values().iterator();

        while (it.hasNext()) {
            it.next().paint(g);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent arg0) {
        // empty
    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent arg0) {
        // empty
    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent arg0) {
        // empty
    }

}
