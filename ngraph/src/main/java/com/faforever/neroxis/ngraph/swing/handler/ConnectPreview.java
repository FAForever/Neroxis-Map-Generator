/**
 * Copyright (c) 2008-2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.swing.handler;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.event.AfterPaintEvent;
import com.faforever.neroxis.ngraph.event.ContinueEvent;
import com.faforever.neroxis.ngraph.event.EventSource;
import com.faforever.neroxis.ngraph.event.StartEvent;
import com.faforever.neroxis.ngraph.event.StopEvent;
import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Connection handler creates new connections between cells. This control is used to display the connector
 * icon, while the preview is used to draw the line.
 */
public class ConnectPreview extends EventSource {
    protected GraphComponent graphComponent;
    protected CellState previewState;
    protected CellState sourceState;
    protected PointDouble startPoint;

    public ConnectPreview(GraphComponent graphComponent) {
        this.graphComponent = graphComponent;
        // Installs the paint handler
        graphComponent.addListener(AfterPaintEvent.class, (sender, evt) -> {
            Graphics g = evt.getGraphics();
            paint(g);
        });
    }

    public void paint(Graphics g) {
        if (previewState != null) {
            Graphics2DCanvas canvas = graphComponent.getCanvas();

            if (graphComponent.isAntiAlias()) {
                Utils.setAntiAlias((Graphics2D) g, true, false);
            }

            float alpha = graphComponent.getPreviewAlpha();

            if (alpha < 1) {
                ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            }
            Graphics2D previousGraphics = canvas.getGraphics();
            PointDouble previousTranslate = canvas.getTranslate();
            double previousScale = canvas.getScale();

            try {
                canvas.setScale(graphComponent.getGraph().getView().getScale());
                canvas.setTranslate(0, 0);
                canvas.setGraphics((Graphics2D) g);

                paintPreview(canvas);
            } finally {
                canvas.setScale(previousScale);
                canvas.setTranslate(previousTranslate.getX(), previousTranslate.getY());
                canvas.setGraphics(previousGraphics);
            }
        }
    }

    /**
     * Draws the preview using the graphics canvas.
     */
    protected void paintPreview(Graphics2DCanvas canvas) {
        graphComponent.getGraphControl().drawCell(graphComponent.getCanvas(), previewState.getCell());
    }

    public boolean isActive() {
        return sourceState != null;
    }

    public CellState getSourceState() {
        return sourceState;
    }

    public CellState getPreviewState() {
        return previewState;
    }

    public PointDouble getStartPoint() {
        return startPoint;
    }

    /**
     * Updates the style of the edge preview from the incoming edge
     */
    public void start(MouseEvent event, CellState startState, String style) {
        Graph graph = graphComponent.getGraph();
        sourceState = startState;
        startPoint = transformScreenPoint(startState.getCenterX(), startState.getCenterY());
        ICell cell = createCell(startState, style);
        graph.getView().validateCell(cell);
        previewState = graph.getView().getState(cell);
        fireEvent(new StartEvent(previewState, event));
    }

    protected PointDouble transformScreenPoint(double x, double y) {
        Graph graph = graphComponent.getGraph();
        PointDouble tr = graph.getView().getTranslate();
        double scale = graph.getView().getScale();
        return new PointDouble(graph.snap(x / scale - tr.getX()), graph.snap(y / scale - tr.getY()));
    }

    /**
     * Creates a new instance of Shape for previewing the edge.
     */
    protected ICell createCell(CellState startState, String style) {
        Graph graph = graphComponent.getGraph();
        ICell cell = graph.createEdge(null, null, "", (startState != null) ? startState.getCell() : null, null, style);
        startState.getCell().insertEdge(cell, true);

        return cell;
    }

    public void update(MouseEvent event, CellState targetState, double x, double y) {
        Graph graph = graphComponent.getGraph();
        ICell cell = previewState.getCell();
        RectangleDouble dirty = graphComponent.getGraph().getPaintBounds(java.util.List.of(previewState.getCell()));
        if (cell.getTarget() != null) {
            cell.getTarget().removeEdge(cell, false);
        }
        if (targetState != null) {
            targetState.getCell().insertEdge(cell, false);
        }

        Geometry geo = graph.getCellGeometry(previewState.getCell());

        geo.setTerminalPoint(startPoint, true);
        geo.setTerminalPoint(transformScreenPoint(x, y), false);
        revalidate(previewState);
        fireEvent(new ContinueEvent(x, y, null, null, event));

        // Repaints the dirty region
        // TODO: Cache the new dirty region for next repaint
        java.awt.Rectangle tmp = getDirtyRect(dirty);

        if (tmp != null) {
            graphComponent.getGraphControl().repaint(tmp);
        } else {
            graphComponent.getGraphControl().repaint();
        }
    }

    public void revalidate(CellState state) {
        state.getView().invalidate(state.getCell());
        state.getView().validateCellState(state.getCell());
    }

    protected java.awt.Rectangle getDirtyRect(RectangleDouble dirty) {
        if (previewState != null) {
            RectangleDouble tmp = graphComponent.getGraph().getPaintBounds(List.of(previewState.getCell()));
            if (dirty != null) {
                dirty.add(tmp);
            } else {
                dirty = tmp;
            }
            if (dirty != null) {
                // TODO: Take arrow size into account
                dirty.grow(2);

                return dirty.getRectangle();
            }
        }

        return null;
    }

    public Object stop(boolean commit) {
        return stop(commit, null);
    }

    public ICell stop(boolean commit, MouseEvent e) {
        ICell result = (sourceState != null) ? sourceState.getCell() : null;

        if (previewState != null) {
            Graph graph = graphComponent.getGraph();

            graph.getModel().beginUpdate();
            try {
                ICell cell = previewState.getCell();
                ICell source = cell.getSource();
                ICell target = cell.getTarget();
                if (source != null) {
                    source.removeEdge(cell, true);
                }
                if (target != null) {
                    target.removeEdge(cell, false);
                }
                if (commit) {
                    result = graph.addCell(cell, null, null, source, target);
                }
                fireEvent(new StopEvent((commit) ? result : null, commit, e));
                // Clears the state before the model commits
                if (previewState != null) {
                    java.awt.Rectangle dirty = getDirtyRect();
                    graph.getView().clear(cell, false, true);
                    previewState = null;

                    if (!commit && dirty != null) {
                        graphComponent.getGraphControl().repaint(dirty);
                    }
                }
            } finally {
                graph.getModel().endUpdate();
            }
        }

        sourceState = null;
        startPoint = null;

        return result;
    }

    protected java.awt.Rectangle getDirtyRect() {
        return getDirtyRect(null);
    }
}
