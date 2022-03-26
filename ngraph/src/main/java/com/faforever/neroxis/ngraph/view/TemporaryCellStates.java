package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.util.Rectangle;

import java.util.HashMap;

public class TemporaryCellStates {
    protected GraphView view;

    protected HashMap<ICell, CellState> oldStates;

    protected Rectangle oldBounds;

    protected double oldScale;

    /**
     * Constructs a new temporary cell states instance.
     */
    public TemporaryCellStates(GraphView view) {
        this(view, 1, null);
    }

    /**
     * Constructs a new temporary cell states instance.
     */
    public TemporaryCellStates(GraphView view, double scale) {
        this(view, scale, null);
    }

    /**
     * Constructs a new temporary cell states instance.
     */
    public TemporaryCellStates(GraphView view, double scale, ICell[] cells) {
        this.view = view;

        // Stores the previous state
        oldBounds = view.getGraphBounds();
        oldStates = view.getStates();
        oldScale = view.getScale();

        // Creates space for the new states
        view.setStates(new HashMap<>());
        view.setScale(scale);

        if (cells != null) {
            Rectangle bbox = null;

            // Validates the vertices and edges without adding them to
            // the model so that the original cells are not modified
            for (ICell cell : cells) {
                Rectangle bounds = view.getBoundingBox(view.validateCellState(view.validateCell(cell)));

                if (bbox == null) {
                    bbox = bounds;
                } else {
                    bbox.add(bounds);
                }
            }

            if (bbox == null) {
                bbox = new Rectangle();
            }

            view.setGraphBounds(bbox);
        }
    }

    /**
     * Destroys the cell states and restores the state of the graph view.
     */
    public void destroy() {
        view.setScale(oldScale);
        view.setStates(oldStates);
        view.setGraphBounds(oldBounds);
    }

}
