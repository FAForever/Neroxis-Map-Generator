/**
 * Copyright (c) 2007-2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.util;

import java.awt.geom.Line2D;
import java.io.Serial;

/**
 * Implements a line with double precision coordinates.
 */
public class Line extends Line2D.Double {
    @Serial
    private static final long serialVersionUID = -4730972599169158546L;

    /**
     * Creates a new line
     */
    public Line(Point startPt, Point endPt) {
        super(startPt, endPt);
    }

    /**
     * Creates a new line
     */
    public Line(double startPtX, double startPtY, double endPtX, double endPtY) {
        super(startPtX, startPtY, endPtX, endPtY);
    }

    /**
     * Returns the start point of the line.
     *
     * @return Returns the end point of the line.
     */
    public Point getP1() {
        return new Point(x1, y1);
    }

    /**
     * Returns the end point of the line.
     *
     * @return Returns the end point of the line.
     */
    public Point getP2() {
        return new Point(x2, y2);
    }

    /**
     * Sets the start and end points.
     */
    public void setPoints(Point startPt, Point endPt) {
        setLine(startPt, endPt);
    }
}
