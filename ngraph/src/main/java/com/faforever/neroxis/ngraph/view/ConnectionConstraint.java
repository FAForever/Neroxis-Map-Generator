/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.util.Point;

/**
 * Defines an object that contains the constraints about how to connect one
 * side of an edge to its terminal.
 */
public class ConnectionConstraint {
    /**
     * Point that specifies the fixed location of the connection point.
     */
    protected Point point;

    /**
     * Boolean that specifies if the point should be projected onto the perimeter
     * of the terminal.
     */
    protected boolean perimeter;

    /**
     * Constructs an empty connection constraint.
     */
    public ConnectionConstraint() {
        this(null);
    }

    /**
     * Constructs a connection constraint for the given point.
     */
    public ConnectionConstraint(Point point) {
        this(point, true);
    }

    /**
     * Constructs a new connection constraint for the given point and boolean
     * arguments.
     *
     * @param point     Optional Point that specifies the fixed location of the point
     *                  in relative coordinates. Default is null.
     * @param perimeter Optional boolean that specifies if the fixed point should be
     *                  projected onto the perimeter of the terminal. Default is true.
     */
    public ConnectionConstraint(Point point, boolean perimeter) {
        setPoint(point);
        setPerimeter(perimeter);
    }

    /**
     * Returns the point.
     */
    public Point getPoint() {
        return point;
    }

    /**
     * Sets the point.
     */
    public void setPoint(Point value) {
        point = value;
    }

    /**
     * Returns perimeter.
     */
    public boolean isPerimeter() {
        return perimeter;
    }

    /**
     * Sets perimeter.
     */
    public void setPerimeter(boolean value) {
        perimeter = value;
    }

}
