/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.swing.util;

import com.faforever.neroxis.ngraph.event.AfterPaintEvent;
import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.swing.view.CellStatePreview;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides animation effects.
 */
public class Morphing extends Animation {
    /**
     * Reference to the enclosing graph instance.
     */
    protected GraphComponent graphComponent;
    /**
     * Specifies the maximum number of steps for the morphing. Default is
     * 6.
     */
    protected int steps;
    /**
     * Counts the current number of steps of the animation.
     */
    protected int step;
    /**
     * Ease-off for movement towards the given vector. Larger values are
     * slower and smoother. Default is 1.5.
     */
    protected double ease;
    /**
     * Maps from cells to origins.
     */
    protected Map<Object, PointDouble> origins = new HashMap<Object, PointDouble>();
    /**
     * Optional array of cells to limit the animation to.
     */
    protected List<ICell> cells;
    protected transient RectangleDouble dirty;
    protected transient CellStatePreview preview;

    /**
     * Constructs a new morphing instance for the given graph.
     */
    public Morphing(GraphComponent graphComponent) {
        this(graphComponent, 6, 1.5, DEFAULT_DELAY);
        // Installs the paint handler
        graphComponent.addListener(AfterPaintEvent.class, (sender, evt) -> {
            paint(evt.getGraphics());
        });
    }

    /**
     * Constructs a new morphing instance for the given graph.
     */
    public Morphing(GraphComponent graphComponent, int steps, double ease, int delay) {
        super(delay);
        this.graphComponent = graphComponent;
        this.steps = steps;
        this.ease = ease;
    }

    public void paint(Graphics g) {
        if (preview != null) {
            preview.paint(g);
        }
    }

    /**
     * Returns the number of steps for the animation.
     */
    public int getSteps() {
        return steps;
    }

    /**
     * Sets the number of steps for the animation.
     */
    public void setSteps(int value) {
        steps = value;
    }

    /**
     * Returns the easing for the movements.
     */
    public double getEase() {
        return ease;
    }

    /**
     * Sets the easing for the movements.
     */
    public void setEase(double value) {
        ease = value;
    }

    /**
     * Optional array of cells to be animated. If this is not specified
     * then all cells are checked and animated if they have been moved
     * in the current transaction.
     */
    public void setCells(List<ICell> value) {
        cells = value;
    }

    /**
     * Animation step.
     */
    @Override
    public void updateAnimation() {
        super.updateAnimation();
        preview = new CellStatePreview(graphComponent, false);

        if (cells != null) {
            // Animates the given cells individually without recursion
            for (ICell cell : cells) {
                animateCell(cell, preview, false);
            }
        } else {
            // Animates all changed cells by using recursion to find
            // the changed cells but not for the animation itself
            ICell root = graphComponent.getGraph().getModel().getRoot();
            animateCell(root, preview, true);
        }

        show(preview);

        if (preview.isEmpty() || step++ >= steps) {
            stopAnimation();
        }
    }

    @Override
    public void stopAnimation() {
        graphComponent.getGraph().getView().revalidate();
        super.stopAnimation();

        preview = null;

        if (dirty != null) {
            graphComponent.getGraphControl().repaint(dirty.getRectangle());
        }
    }

    /**
     * Shows the changes in the given CellStatePreview.
     */
    protected void show(CellStatePreview preview) {
        if (dirty != null) {
            graphComponent.getGraphControl().repaint(dirty.getRectangle());
        } else {
            graphComponent.getGraphControl().repaint();
        }

        dirty = preview.show();

        if (dirty != null) {
            graphComponent.getGraphControl().repaint(dirty.getRectangle());
        }
    }

    /**
     * Animates the given cell state using moveState.
     */
    protected void animateCell(ICell cell, CellStatePreview move, boolean recurse) {
        Graph graph = graphComponent.getGraph();
        CellState state = graph.getView().getState(cell);
        PointDouble delta = null;

        if (state != null) {
            // Moves the animated state from where it will be after the model
            // change by subtracting the given delta vector from that location
            delta = getDelta(state);

            if (graph.getModel().isVertex(cell) && (delta.getX() != 0 || delta.getY() != 0)) {
                PointDouble translate = graph.getView().getTranslate();
                double scale = graph.getView().getScale();

                // FIXME: Something wrong with the scale
                delta.setX(delta.getX() + translate.getX() * scale);
                delta.setY(delta.getY() + translate.getY() * scale);

                move.moveState(state, -delta.getX() / ease, -delta.getY() / ease);
            }
        }

        if (recurse && !stopRecursion(state, delta)) {
            int childCount = graph.getModel().getChildCount(cell);

            for (int i = 0; i < childCount; i++) {
                animateCell(graph.getModel().getChildAt(cell, i), move, recurse);
            }
        }
    }

    /**
     * Returns true if the animation should not recursively find more
     * deltas for children if the given parent state has been animated.
     */
    protected boolean stopRecursion(CellState state, PointDouble delta) {
        return delta != null && (delta.getX() != 0 || delta.getY() != 0);
    }

    /**
     * Returns the vector between the current rendered state and the future
     * location of the state after the display will be updated.
     */
    protected PointDouble getDelta(CellState state) {
        Graph graph = graphComponent.getGraph();
        PointDouble origin = getOriginForCell(state.getCell());
        PointDouble translate = graph.getView().getTranslate();
        double scale = graph.getView().getScale();
        PointDouble current = new PointDouble(state.getX() / scale - translate.getX(),
                                              state.getY() / scale - translate.getY());
        return new PointDouble((origin.getX() - current.getX()) * scale, (origin.getY() - current.getY()) * scale);
    }

    /**
     * Returns the top, left corner of the given cell.
     */
    protected PointDouble getOriginForCell(ICell cell) {
        PointDouble result = origins.get(cell);
        if (result == null) {
            Graph graph = graphComponent.getGraph();
            if (cell != null) {
                result = new PointDouble(getOriginForCell(graph.getModel().getParent(cell)));
                Geometry geo = graph.getCellGeometry(cell);
                // TODO: Handle offset, relative geometries etc
                if (geo != null) {
                    result.setX(result.getX() + geo.getX());
                    result.setY(result.getY() + geo.getY());
                }
            }

            if (result == null) {
                PointDouble t = graph.getView().getTranslate();
                result = new PointDouble(-t.getX(), -t.getY());
            }

            origins.put(cell, result);
        }

        return result;
    }
}
