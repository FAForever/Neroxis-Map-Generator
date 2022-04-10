/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.util.Point;
import com.faforever.neroxis.ngraph.util.Rectangle;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the current state of a cell in a given graph view.
 */
@Getter
@Setter
public class CellState extends Rectangle {
    @Serial
    private static final long serialVersionUID = 7588335615324083354L;

    /**
     * Reference to the enclosing graph view.
     */
    protected GraphView view;

    /**
     * Reference to the cell that is represented by this state.
     */
    protected ICell cell;

    /**
     * Holds the current label value, including newlines which result from
     * word wrapping.
     */
    protected String label;

    /**
     * Contains an array of key, value pairs that represent the style of the
     * cell.
     */
    protected Map<String, Object> style;

    /**
     * Holds the origin for all child cells.
     */
    protected Point origin = new Point();

    /**
     * List of Points that represent the absolute points of an edge.
     */
    protected List<Point> absolutePoints;

    /**
     * Holds the absolute offset. For edges, this is the absolute coordinates
     * of the label position. For vertices, this is the offset of the label
     * relative to the top, left corner of the vertex.
     */
    protected Point absoluteOffset = new Point();

    /**
     * Caches the distance between the end points and the length of an edge.
     */
    protected double terminalDistance, length;

    /**
     * Array of numbers that represent the cached length of each segment of the
     * edge.
     */
    protected double[] segments;

    /**
     * Holds the rectangle which contains the label.
     */
    protected Rectangle labelBounds;

    /**
     * Holds the largest rectangle which contains all rendering for this cell.
     */
    protected Rectangle boundingBox;

    /**
     * Specifies if the state is invalid. Default is true.
     */
    protected boolean invalid = true;

    /**
     * Caches the visible source and target terminal states.
     */
    protected CellState visibleSourceState, visibleTargetState;

    /**
     * Constructs an empty cell state.
     */
    public CellState() {
        this(null, null, null);
    }

    /**
     * Constructs a new object that represents the current state of the given
     * cell in the specified view.
     *
     * @param view  Graph view that contains the state.
     * @param cell  Cell that this state represents.
     * @param style Array of key, value pairs that constitute the style.
     */
    public CellState(GraphView view, ICell cell, Map<String, Object> style) {
        setView(view);
        setCell(cell);
        setStyle(style);
    }

    /**
     * Returns the absolute point at the given index.
     *
     * @return the Point at the given index
     */
    public Point getAbsolutePoint(int index) {
        return absolutePoints.get(index);
    }

    /**
     * Returns the absolute point at the given index.
     *
     * @return the Point at the given index
     */
    public Point setAbsolutePoint(int index, Point point) {
        return absolutePoints.set(index, point);
    }

    /**
     * Returns the number of absolute points.
     *
     * @return the absolutePoints
     */
    public int getAbsolutePointCount() {
        return (absolutePoints != null) ? absolutePoints.size() : 0;
    }


    /**
     * Returns the rectangle that should be used as the perimeter of the cell.
     * This implementation adds the perimeter spacing to the rectangle
     * defined by this cell state.
     *
     * @return Returns the rectangle that defines the perimeter.
     */
    public Rectangle getPerimeterBounds() {
        return getPerimeterBounds(0);
    }

    /**
     * Returns the rectangle that should be used as the perimeter of the cell.
     *
     * @return Returns the rectangle that defines the perimeter.
     */
    public Rectangle getPerimeterBounds(double border) {
        Rectangle bounds = new Rectangle(getRectangle());

        if (border != 0) {
            bounds.grow(border);
        }

        return bounds;
    }

    /**
     * Sets the first or last point in the list of points depending on isSource.
     *
     * @param point    Point that represents the terminal point.
     * @param isSource Boolean that specifies if the first or last point should
     *                 be assigned.
     */
    public void setAbsoluteTerminalPoint(Point point, boolean isSource) {
        if (isSource) {
            if (absolutePoints == null) {
                absolutePoints = new ArrayList<>();
            }

            if (absolutePoints.size() == 0) {
                absolutePoints.add(point);
            } else {
                absolutePoints.set(0, point);
            }
        } else {
            if (absolutePoints == null) {
                absolutePoints = new ArrayList<>();
                absolutePoints.add(null);
                absolutePoints.add(point);
            } else if (absolutePoints.size() == 1) {
                absolutePoints.add(point);
            } else {
                absolutePoints.set(absolutePoints.size() - 1, point);
            }
        }
    }

    /**
     * Returns the visible source or target terminal cell.
     *
     * @param source Boolean that specifies if the source or target cell should be
     *               returned.
     */
    public ICell getVisibleTerminal(boolean source) {
        CellState tmp = getVisibleTerminalState(source);

        return (tmp != null) ? tmp.getCell() : null;
    }

    /**
     * Returns the visible source or target terminal state.
     *
     * @param source Boolean that specifies if the source or target state should be
     *               returned.
     */
    public CellState getVisibleTerminalState(boolean source) {
        return (source) ? visibleSourceState : visibleTargetState;
    }

    /**
     * Sets the visible source or target terminal state.
     *
     * @param terminalState Cell state that represents the terminal.
     * @param source        Boolean that specifies if the source or target state should be set.
     */
    public void setVisibleTerminalState(CellState terminalState, boolean source) {
        if (source) {
            visibleSourceState = terminalState;
        } else {
            visibleTargetState = terminalState;
        }
    }

    /**
     * Returns a clone of this state where all members are deeply cloned
     * except the view and cell references, which are copied with no
     * cloning to the new instance.
     */
    public CellState clone() {
        CellState clone = (CellState) super.clone();
        clone.setView(view);
        clone.setCell(cell);
        clone.setStyle(style);
        if (label != null) {
            clone.label = label;
        }
        if (absolutePoints != null) {
            clone.absolutePoints = new ArrayList<>();
            for (int i = 0; i < absolutePoints.size(); i++) {
                clone.absolutePoints.add((Point) absolutePoints.get(i).clone());
            }
        }

        if (origin != null) {
            clone.origin = (Point) origin.clone();
        }

        if (absoluteOffset != null) {
            clone.absoluteOffset = (Point) absoluteOffset.clone();
        }

        if (labelBounds != null) {
            clone.labelBounds = labelBounds.clone();
        }

        if (boundingBox != null) {
            clone.boundingBox = boundingBox.clone();
        }

        clone.terminalDistance = terminalDistance;
        clone.segments = segments;
        clone.length = length;
        clone.x = x;
        clone.y = y;
        clone.width = width;
        clone.height = height;

        return clone;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [cell=" + cell + ", label=" + label + ", x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
    }

}
