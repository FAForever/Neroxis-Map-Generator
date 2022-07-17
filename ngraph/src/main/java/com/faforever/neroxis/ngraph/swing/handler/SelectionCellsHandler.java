/**
 * Copyright (c) 2008, Gaudenz Alder
 * <p>
 * Known issue: Drag image size depends on the initial position and may sometimes
 * not align with the grid when dragging. This is because the rounding of the width
 * and height at the initial position may be different than that at the current
 * position as the left and bottom side of the shape must align to the grid lines.
 */
package com.faforever.neroxis.ngraph.swing.handler;

import com.faforever.neroxis.ngraph.event.*;
import com.faforever.neroxis.ngraph.event.EventSource.IEventListener;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class SelectionCellsHandler implements MouseListener, MouseMotionListener {
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
    protected transient LinkedHashMap<Object, CellHandler> handlers = new LinkedHashMap<>();
    protected transient IEventListener<?> refreshHandler = (source, evt) -> {
        if (isEnabled()) {
            refresh();
        }
    };
    protected transient PropertyChangeListener labelMoveHandler = evt -> {
        if (evt.getPropertyName().equals("vertexLabelsMovable") || evt.getPropertyName().equals("edgeLabelsMovable")) {
            refresh();
        }
    };

    public SelectionCellsHandler(final GraphComponent graphComponent) {
        this.graphComponent = graphComponent;

        // Listens to all mouse events on the rendering control
        graphComponent.getGraphControl().addMouseListener(this);
        graphComponent.getGraphControl().addMouseMotionListener(this);
        // Installs the graph listeners and keeps them in sync
        addGraphListeners(graphComponent.getGraph());
        graphComponent.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals("graph")) {
                removeGraphListeners((Graph) evt.getOldValue());
                addGraphListeners((Graph) evt.getNewValue());
            }
        });
        // Installs the paint handler
        graphComponent.addListener(PaintEvent.class, (sender, evt) -> {
            paintHandles(evt.getGraphics());
        });
    }

    /**
     * Installs the listeners to update the handles after any changes.
     */
    protected void addGraphListeners(Graph graph) {
        // LATER: Install change listener for graph model, selection model, view
        if (graph != null) {
            graph.getSelectionModel().addListener(ChangeEvent.class, (IEventListener<ChangeEvent>) refreshHandler);
            graph.getModel().addListener(ChangeEvent.class, (IEventListener<ChangeEvent>) refreshHandler);
            graph.getView().addListener(ScaleEvent.class, (IEventListener<ScaleEvent>) refreshHandler);
            graph.getView().addListener(TranslateEvent.class, (IEventListener<TranslateEvent>) refreshHandler);
            graph.getView()
                    .addListener(ScaleAndTranslateEvent.class, (IEventListener<ScaleAndTranslateEvent>) refreshHandler);
            graph.getView().addListener(DownEvent.class, (IEventListener<DownEvent>) refreshHandler);
            graph.getView().addListener(UpEvent.class, (IEventListener<UpEvent>) refreshHandler);
            // Refreshes the handles if moveVertexLabels or moveEdgeLabels changes
            graph.addPropertyChangeListener(labelMoveHandler);
        }
    }

    /**
     * Removes all installed listeners.
     */
    protected void removeGraphListeners(Graph graph) {
        if (graph != null) {
            graph.getSelectionModel().removeListener(refreshHandler);
            graph.getModel().removeListener(refreshHandler);
            // Refreshes the handles if moveVertexLabels or moveEdgeLabels changes
            graph.removePropertyChangeListener(labelMoveHandler);
        }
    }

    public void paintHandles(Graphics g) {

        for (CellHandler cellHandler : handlers.values()) {
            cellHandler.paint(g);
        }
    }

    public GraphComponent getGraphComponent() {
        return graphComponent;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean value) {
        visible = value;
    }

    public CellHandler getHandler(Object cell) {
        return handlers.get(cell);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (graphComponent.isEnabled() && isEnabled()) {
            Iterator<CellHandler> it = handlers.values().iterator();

            while (it.hasNext() && !e.isConsumed()) {
                it.next().mouseDragged(e);
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (graphComponent.isEnabled() && isEnabled()) {
            Iterator<CellHandler> it = handlers.values().iterator();

            while (it.hasNext() && !e.isConsumed()) {
                it.next().mouseMoved(e);
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
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

    public void refresh() {
        Graph graph = graphComponent.getGraph();

        // Creates a new map for the handlers and tries to
        // to reuse existing handlers from the old map
        LinkedHashMap<Object, CellHandler> oldHandlers = handlers;
        handlers = new LinkedHashMap<>();

        // Creates handles for all selection cells
        List<ICell> tmp = graph.getSelectionCells();
        boolean handlesVisible = tmp.size() <= getMaxHandlers();
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

    public int getMaxHandlers() {
        return maxHandlers;
    }

    public void setMaxHandlers(int value) {
        maxHandlers = value;
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        // empty
    }

    /**
     * Dispatches the mousepressed event to the subhandles. This is
     * called from the connection handler as subhandles have precedence
     * over the connection handler.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (graphComponent.isEnabled() && !graphComponent.isForceMarqueeEvent(e) && isEnabled()) {
            Iterator<CellHandler> it = handlers.values().iterator();

            while (it.hasNext() && !e.isConsumed()) {
                it.next().mousePressed(e);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (graphComponent.isEnabled() && isEnabled()) {
            Iterator<CellHandler> it = handlers.values().iterator();

            while (it.hasNext() && !e.isConsumed()) {
                it.next().mouseReleased(e);
            }
        }

        reset();
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
        // empty
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
        // empty
    }

    public void reset() {
        for (CellHandler cellHandler : handlers.values()) {
            cellHandler.reset();
        }
    }
}
