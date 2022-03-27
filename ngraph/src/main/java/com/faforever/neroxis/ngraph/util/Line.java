/**
 * Copyright (c) 2007-2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.util;

import java.awt.geom.Line2D;
import java.io.Serial;

/**
 * Implements a line with double precision coordinates.
 */

public class Line extends Point {
    @Serial
    private static final long serialVersionUID = -4730972599169158546L;
    /**
     * The end point of the line
     */
    protected Point endPoint;

    /**
     * Creates a new line
     */
    public Line(Point startPt, Point endPt) {
        this.setX(startPt.getX());
        this.setY(startPt.getY());
        this.endPoint = endPt;
    }

    /**
     * Creates a new line
     */
    public Line(double startPtX, double startPtY, Point endPt) {
        x = startPtX;
        y = startPtY;
        this.endPoint = endPt;
    }

    /**
     * Returns the end point of the line.
     *
     * @return Returns the end point of the line.
     */
    public Point getEndPoint() {
        return this.endPoint;
    }

    /**
     * Sets the end point of the rectangle.
     *
     * @param value The new end point of the line
     */
    public void setEndPoint(Point value) {
        this.endPoint = value;
    }

    /**
     * Sets the start and end points.
     */
    public void setPoints(Point startPt, Point endPt) {
        this.setX(startPt.getX());
        this.setY(startPt.getY());
        this.endPoint = endPt;
    }

    /**
     * Returns the square of the shortest distance from a point to this line.
     * The line is considered extrapolated infinitely in both directions for
     * the purposes of the calculation.
     *
     * @param pt the point whose distance is being measured
     * @return the square of the distance from the specified point to this line.
     */
    public double ptLineDistSq(Point pt) {
        return new Line2D.Double(getX(), getY(), endPoint.getX(), endPoint.getY()).ptLineDistSq(pt.getX(), pt.getY());
    }

    /**
     * Returns the square of the shortest distance from a point to this
     * line segment.
     *
     * @param pt the point whose distance is being measured
     * @return the square of the distance from the specified point to this segment.
     */
    public double ptSegDistSq(Point pt) {
        return new Line2D.Double(getX(), getY(), endPoint.getX(), endPoint.getY()).ptSegDistSq(pt.getX(), pt.getY());
    }

}
