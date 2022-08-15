/**
 * Copyright (c) 2007-2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.util;

import java.awt.geom.Point2D;
import java.io.Serial;

/**
 * Extends awt point with double precision coordinates.
 */
public class PointDouble extends Point2D.Double {
    @Serial
    private static final long serialVersionUID = 6554231393215892186L;

    /**
     * Constructs a new point at (0, 0).
     */
    public PointDouble() {
        this(0, 0);
    }

    /**
     * Constructs a new point at (x, y).
     *
     * @param x X-coordinate of the point to be created.
     * @param y Y-coordinate of the point to be created.
     */
    public PointDouble(double x, double y) {
        setX(x);
        setY(y);
    }

    /**
     * Constructs a new point at the location of the given point.
     *
     * @param point Point that specifies the location.
     */
    public PointDouble(Point2D point) {
        this(point.getX(), point.getY());
    }

    /**
     * Constructs a new point at the location of the given point.
     *
     * @param point Point that specifies the location.
     */
    public PointDouble(PointDouble point) {
        this(point.getX(), point.getY());
    }

    /**
     * Returns the x-coordinate of the point.
     *
     * @return Returns the x-coordinate.
     */
    @Override
    public double getX() {
        return x;
    }

    /**
     * Sets the x-coordinate of the point.
     *
     * @param value Double that specifies the new x-coordinate.
     */
    public void setX(double value) {
        x = value;
    }

    /**
     * Returns the x-coordinate of the point.
     *
     * @return Returns the x-coordinate.
     */
    @Override
    public double getY() {
        return y;
    }

    /**
     * Sets the y-coordinate of the point.
     *
     * @param value Double that specifies the new x-coordinate.
     */
    public void setY(double value) {
        y = value;
    }

    /**
     * Returns a <code>String</code> that represents the value
     * of this <code>Point</code>.
     *
     * @return a string representation of this <code>Point</code>.
     */
    @Override
    public String toString() {
        return String.format("Point[%f, %f]", x, y);
    }

    public void scale(double scale) {
        this.x *= scale;
        this.y *= scale;
    }

    public void move(PointDouble point) {
        move(point.x, point.y);
    }

    public void move(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void translate(PointDouble point) {
        translate(point.x, point.y);
    }

    public void translate(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    /**
     * Returns the coordinates as a new point.
     *
     * @return Returns a new point for the location.
     */
    public java.awt.Point toPoint() {
        return new java.awt.Point((int) Math.round(x), (int) Math.round(y));
    }
}
