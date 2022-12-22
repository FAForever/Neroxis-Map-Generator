/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.model;

import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the geometry of a cell. For vertices, the geometry consists
 * of the x- and y-location, as well as the width and height. For edges,
 * the geometry either defines the source- and target-terminal, or it
 * defines the respective terminal points.
 * <p>
 * For edges, if the geometry is relative (default), then the x-coordinate
 * is used to describe the distance from the center of the edge from -1 to 1
 * with 0 being the center of the edge and the default value, and the
 * y-coordinate is used to describe the absolute, orthogonal distance in
 * pixels from that point. In addition, the offset is used as an absolute
 * offset vector from the resulting point.
 */
public class Geometry extends RectangleDouble {
    @Serial
    private static final long serialVersionUID = 2649828026610336589L;
    /**
     * Global switch to translate the points in translate. Default is true.
     */
    public static boolean TRANSLATE_CONTROL_POINTS = true;
    /**
     * Stores alternate values for x, y, width and height in a rectangle.
     * Default is null.
     */
    protected RectangleDouble alternateBounds;
    /**
     * Defines the source- and target-point of the edge. This is used if the
     * corresponding edge does not have a source vertex. Otherwise it is
     * ignored. Default is null.
     */
    protected PointDouble sourcePoint, targetPoint;
    /**
     * List of Points which specifies the control points along the edge.
     * These points are the intermediate points on the edge, for the endpoints
     * use targetPoint and sourcePoint or set the terminals of the edge to
     * a non-null value. Default is null.
     */
    protected List<PointDouble> points;
    /**
     * Holds the offset of the label for edges. This is the absolute vector
     * between the center of the edge and the top, left point of the label.
     * Default is null.
     */
    protected PointDouble offset;
    /**
     * Specifies if the coordinates in the geometry are to be interpreted as
     * relative coordinates. Default is false. This is used to mark a geometry
     * with an x- and y-coordinate that is used to describe an edge label
     * position, or a relative location with respect to a parent cell's
     * width and height.
     */
    protected boolean relative = false;

    /**
     * Constructs a new geometry at (0, 0) with the width and height set to 0.
     */
    public Geometry() {
        this(0, 0, 0, 0);
    }

    /**
     * Constructs a geometry using the given parameters.
     *
     * @param x      X-coordinate of the new geometry.
     * @param y      Y-coordinate of the new geometry.
     * @param width  Width of the new geometry.
     * @param height Height of the new geometry.
     */
    public Geometry(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    /**
     * Swaps the x, y, width and height with the values stored in
     * alternateBounds and puts the previous values into alternateBounds as
     * a rectangle. This operation is carried-out in-place, that is, using the
     * existing geometry instance. If this operation is called during a graph
     * model transactional change, then the geometry should be cloned before
     * calling this method and setting the geometry of the cell using
     * GraphModel.setGeometry.
     */
    public void swap() {
        if (alternateBounds != null) {
            RectangleDouble old = new RectangleDouble(getX(), getY(), getWidth(), getHeight());

            x = alternateBounds.getX();
            y = alternateBounds.getY();
            width = alternateBounds.getWidth();
            height = alternateBounds.getHeight();

            alternateBounds = old;
        }
    }

    /**
     * Returns the point representing the source or target point of this edge.
     * This is only used if the edge has no source or target vertex.
     *
     * @param isSource Boolean that specifies if the source or target point
     *                 should be returned.
     * @return Returns the source or target point.
     */
    public PointDouble getTerminalPoint(boolean isSource) {
        return (isSource) ? sourcePoint : targetPoint;
    }

    /**
     * Sets the sourcePoint or targetPoint to the given point and returns the
     * new point.
     *
     * @param point    Point to be used as the new source or target point.
     * @param isSource Boolean that specifies if the source or target point
     *                 should be set.
     * @return Returns the new point.
     */
    public PointDouble setTerminalPoint(PointDouble point, boolean isSource) {
        if (isSource) {
            sourcePoint = point;
        } else {
            targetPoint = point;
        }
        return point;
    }

    /**
     * Translates the geometry by the specified amount. That is, x and y of the
     * geometry, the sourcePoint, targetPoint and all elements of points are
     * translated by the given amount. X and y are only translated if the
     * geometry is not relative. If TRANSLATE_CONTROL_POINTS is false, then
     * are not modified by this function.
     *
     * @param dx Integer that specifies the x-coordinate of the translation.
     * @param dy Integer that specifies the y-coordinate of the translation.
     */
    public void translate(double dx, double dy) {
        // Translates the geometry
        if (!isRelative()) {
            x += dx;
            y += dy;
        }

        // Translates the source point
        if (sourcePoint != null) {
            sourcePoint.setX(sourcePoint.getX() + dx);
            sourcePoint.setY(sourcePoint.getY() + dy);
        }

        // Translates the target point
        if (targetPoint != null) {
            targetPoint.setX(targetPoint.getX() + dx);
            targetPoint.setY(targetPoint.getY() + dy);
        }

        // Translate the control points
        if (TRANSLATE_CONTROL_POINTS && points != null) {
            for (PointDouble pt : points) {
                pt.setX(pt.getX() + dx);
                pt.setY(pt.getY() + dy);
            }
        }
    }

    /**
     * Returns a clone of the cell.
     */
    @Override
    public Geometry clone() {
        Geometry clone = (Geometry) super.clone();

        clone.setX(getX());
        clone.setY(getY());
        clone.setWidth(getWidth());
        clone.setHeight(getHeight());
        clone.setRelative(isRelative());
        List<PointDouble> pts = getPoints();

        if (pts != null) {
            clone.points = new ArrayList<>(pts.size());
            for (PointDouble pt : pts) {
                clone.points.add((PointDouble) pt.clone());
            }
        }
        PointDouble tp = getTargetPoint();

        if (tp != null) {
            clone.setTargetPoint((PointDouble) tp.clone());
        }
        PointDouble sp = getSourcePoint();

        if (sp != null) {
            setSourcePoint((PointDouble) sp.clone());
        }
        PointDouble off = getOffset();

        if (off != null) {
            clone.setOffset((PointDouble) off.clone());
        }
        RectangleDouble alt = getAlternateBounds();

        if (alt != null) {
            setAlternateBounds(alt.clone());
        }

        return clone;
    }

    /**
     * Returns true of the geometry is relative.
     */
    public boolean isRelative() {
        return relative;
    }

    /**
     * Sets the relative state of the geometry.
     *
     * @param value Boolean value to be used as the new relative state.
     */
    public void setRelative(boolean value) {
        relative = value;
    }

    /**
     * Returns the list of control points.
     */
    public List<PointDouble> getPoints() {
        return points;
    }

    /**
     * Sets the list of control points to the given list.
     *
     * @param value List that contains the new control points.
     */
    public void setPoints(List<PointDouble> value) {
        points = value;
    }

    /**
     * Returns the target point.
     *
     * @return Returns the target point.
     */
    public PointDouble getTargetPoint() {
        return targetPoint;
    }

    /**
     * Sets the target point.
     *
     * @param targetPoint Target point to be used.
     */
    public void setTargetPoint(PointDouble targetPoint) {
        this.targetPoint = targetPoint;
    }

    /**
     * Returns the source point.
     *
     * @return Returns the source point.
     */
    public PointDouble getSourcePoint() {
        return sourcePoint;
    }

    /**
     * Sets the source point.
     *
     * @param sourcePoint Source point to be used.
     */
    public void setSourcePoint(PointDouble sourcePoint) {
        this.sourcePoint = sourcePoint;
    }

    /**
     * Returns the offset.
     */
    public PointDouble getOffset() {
        return offset;
    }

    /**
     * Sets the offset to the given point.
     *
     * @param offset Point to be used for the offset.
     */
    public void setOffset(PointDouble offset) {
        this.offset = offset;
    }

    /**
     * Returns the alternate bounds.
     */
    public RectangleDouble getAlternateBounds() {
        return alternateBounds;
    }

    /**
     * Sets the alternate bounds to the given rectangle.
     *
     * @param rect Rectangle to be used for the alternate bounds.
     */
    public void setAlternateBounds(RectangleDouble rect) {
        alternateBounds = rect;
    }
}
