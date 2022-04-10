/**
 * Copyright (c) 2008-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.swing.handler;

import com.faforever.neroxis.ngraph.event.AfterPaintEvent;
import com.faforever.neroxis.ngraph.event.ContinueEvent;
import com.faforever.neroxis.ngraph.event.EventSource;
import com.faforever.neroxis.ngraph.event.StartEvent;
import com.faforever.neroxis.ngraph.event.StopEvent;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.swing.util.SwingConstants;
import com.faforever.neroxis.ngraph.swing.view.CellStatePreview;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Connection handler creates new connections between cells. This control is used to display the connector
 * icon, while the preview is used to draw the line.
 */
public class MovePreview extends EventSource {
    protected GraphComponent graphComponent;

    /**
     * Maximum number of cells to preview individually. Default is 200.
     */
    protected int threshold = 200;

    /**
     * Specifies if the placeholder rectangle should be used for all
     * previews. Default is false. This overrides all other preview
     * settings if true.
     */
    protected boolean placeholderPreview = false;

    /**
     * Specifies if the preview should use clones of the original shapes.
     * Default is true.
     */
    protected boolean clonePreview = true;

    /**
     * Specifies if connected, unselected edges should be included in the
     * preview. Default is true. This should not be used if cloning is
     * enabled.
     */
    protected boolean contextPreview = true;

    /**
     * Specifies if the selection cells handler should be hidden while the
     * preview is visible. Default is false.
     */
    protected boolean hideSelectionHandler = false;

    protected transient CellState startState;

    protected transient List<CellState> previewStates;

    protected transient List<ICell> movingCells;

    protected transient java.awt.Rectangle initialPlaceholder;

    protected transient java.awt.Rectangle placeholder;
    protected transient RectangleDouble lastDirty;

    protected transient CellStatePreview preview;

    /**
     * Constructs a new rubberband selection for the given graph component.
     *
     * @param graphComponent Component that contains the rubberband.
     */
    public MovePreview(GraphComponent graphComponent) {
        this.graphComponent = graphComponent;
        // Installs the paint handler
        graphComponent.addListener(AfterPaintEvent.class, (sender, evt) -> {
            paint(evt.getGraphics());
        });
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int value) {
        threshold = value;
    }

    public boolean isPlaceholderPreview() {
        return placeholderPreview;
    }

    public void setPlaceholderPreview(boolean value) {
        placeholderPreview = value;
    }

    public boolean isClonePreview() {
        return clonePreview;
    }

    public void setClonePreview(boolean value) {
        clonePreview = value;
    }

    public boolean isContextPreview() {
        return contextPreview;
    }

    public void setContextPreview(boolean value) {
        contextPreview = value;
    }

    public boolean isHideSelectionHandler() {
        return hideSelectionHandler;
    }

    public void setHideSelectionHandler(boolean value) {
        hideSelectionHandler = value;
    }

    public boolean isActive() {
        return startState != null;
    }

    /**
     * FIXME: Cells should be assigned outside of getPreviewStates
     */
    public List<ICell> getMovingCells() {
        return List.copyOf(movingCells);
    }

    public List<ICell> getCells(CellState initialState) {
        Graph graph = graphComponent.getGraph();

        return graph.getMovableCells(graph.getSelectionCells());
    }

    /**
     * Returns the states that are affected by the move operation.
     */
    protected List<CellState> getPreviewStates() {
        Graph graph = graphComponent.getGraph();
        List<CellState> result = new ArrayList<>();

        for (ICell cell : movingCells) {
            CellState cellState = graph.getView().getState(cell);

            if (cellState != null) {
                result.add(cellState);

                // Terminates early if too many cells
                if (result.size() >= threshold) {
                    return null;
                }

                if (isContextPreview()) {
                    List<ICell> edges = graph.getAllEdges(List.of(cell));

                    for (ICell edge : edges) {
                        if (!graph.isCellSelected(edge)) {
                            CellState edgeState = graph.getView().getState(edge);

                            if (edgeState != null) {
                                // Terminates early if too many cells
                                if (result.size() >= threshold) {
                                    return null;
                                }

                                result.add(edgeState);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    protected boolean isCellOpaque(Object cell) {
        return startState != null && startState.getCell() == cell;
    }

    /**
     * Sets the translation of the preview.
     */
    public void start(MouseEvent e, CellState state) {
        startState = state;
        movingCells = getCells(state);
        previewStates = (!placeholderPreview) ? getPreviewStates() : null;

        if (previewStates == null || previewStates.size() >= threshold) {
            placeholder = getPlaceholderBounds(startState).getRectangle();
            initialPlaceholder = new java.awt.Rectangle(placeholder);
            graphComponent.getGraphControl().repaint(placeholder);
        }
        fireEvent(new StartEvent(startState, e));
    }

    protected RectangleDouble getPlaceholderBounds(CellState startState) {
        Graph graph = graphComponent.getGraph();
        return graph.getView().getBounds(graph.getSelectionCells());
    }

    public CellStatePreview createCellStatePreview() {
        return new CellStatePreview(graphComponent, isClonePreview()) {
            protected float getOpacityForCell(Object cell) {
                if (isCellOpaque(cell)) {
                    return 1;
                }

                return super.getOpacityForCell(cell);
            }
        };
    }

    /**
     * Sets the translation of the preview.
     */
    public void update(MouseEvent e, double dx, double dy, boolean clone) {
        Graph graph = graphComponent.getGraph();

        if (placeholder != null) {
            java.awt.Rectangle tmp = new java.awt.Rectangle(placeholder);
            placeholder.x = initialPlaceholder.x + (int) dx;
            placeholder.y = initialPlaceholder.x + (int) dy;
            tmp.add(placeholder);
            graphComponent.getGraphControl().repaint(tmp);
        } else if (previewStates != null) {
            preview = createCellStatePreview();
            preview.setOpacity(graphComponent.getPreviewAlpha());

            // Combines the layout result with the move preview
            for (CellState previewState : previewStates) {
                preview.moveState(previewState, dx, dy, false, false);

                // FIXME: Move into show-handler?
                boolean visible = true;

                if ((dx != 0 || dy != 0) && clone && isContextPreview()) {
                    visible = false;
                    ICell tmp = previewState.getCell();

                    while (!visible && tmp != null) {
                        visible = graph.isCellSelected(tmp);
                        tmp = graph.getModel().getParent(tmp);
                    }
                }
            }
            RectangleDouble dirty = lastDirty;

            lastDirty = preview.show();

            if (dirty != null) {
                dirty.add(lastDirty);
            } else {
                dirty = lastDirty;
            }

            if (dirty != null) {
                repaint(dirty);
            }
        }

        if (isHideSelectionHandler()) {
            graphComponent.getSelectionCellsHandler().setVisible(false);
        }
        fireEvent(new ContinueEvent(null, null, dx, dy, e));
    }

    protected void repaint(RectangleDouble dirty) {
        if (dirty != null) {
            graphComponent.getGraphControl().repaint(dirty.getRectangle());
        } else {
            graphComponent.getGraphControl().repaint();
        }
    }

    protected void reset() {
        Graph graph = graphComponent.getGraph();

        if (placeholder != null) {
            java.awt.Rectangle tmp = placeholder;
            placeholder = null;
            graphComponent.getGraphControl().repaint(tmp);
        }

        if (isHideSelectionHandler()) {
            graphComponent.getSelectionCellsHandler().setVisible(true);
        }

        // Revalidates the screen
        // TODO: Should only revalidate moved cells
        if (!isClonePreview() && previewStates != null) {
            graph.getView().revalidate();
        }

        previewStates = null;
        movingCells = null;
        startState = null;
        preview = null;

        if (lastDirty != null) {
            graphComponent.getGraphControl().repaint(lastDirty.getRectangle());
            lastDirty = null;
        }
    }

    public List<ICell> stop(boolean commit, MouseEvent e, double dx, double dy, boolean clone, ICell target) {
        List<ICell> cells = movingCells;
        reset();

        Graph graph = graphComponent.getGraph();
        graph.getModel().beginUpdate();
        try {
            if (commit) {
                double s = graph.getView().getScale();
                cells = graph.moveCells(cells, dx / s, dy / s, clone, target, e.getPoint());
            }
            fireEvent(new StopEvent(null, commit, e));
        } finally {
            graph.getModel().endUpdate();
        }

        return cells;
    }

    public void paint(Graphics g) {
        if (placeholder != null) {
            SwingConstants.PREVIEW_BORDER.paintBorder(graphComponent, g, placeholder.x, placeholder.y, placeholder.width, placeholder.height);
        }

        if (preview != null) {
            preview.paint(g);
        }
    }

}
