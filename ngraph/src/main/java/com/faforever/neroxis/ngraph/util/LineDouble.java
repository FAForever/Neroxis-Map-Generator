/**
 * Copyright (c) 2007-2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.util;

import java.awt.geom.Line2D;
import java.io.Serial;

/**
 * Implements a line with double precision coordinates.
 */
public class LineDouble extends Line2D.Double {
    @Serial
    private static final long serialVersionUID = -4730972599169158546L;

    /**
     * Creates a new line
     */
    public LineDouble(PointDouble startPt, PointDouble endPt) {
        super(startPt, endPt);
    }

    /**
     * Creates a new line
     */
    public LineDouble(double startPtX, double startPtY, double endPtX, double endPtY) {
        super(startPtX, startPtY, endPtX, endPtY);
    }

    /**
     * Returns the start point of the line.
     *
     * @return Returns the end point of the line.
     */
    @Override
    public PointDouble getP1() {
        return new PointDouble(x1, y1);
    }

    /**
     * Returns the end point of the line.
     *
     * @return Returns the end point of the line.
     */
    @Override
    public PointDouble getP2() {
        return new PointDouble(x2, y2);
    }

    /**
     * Sets the start and end points.
     */
    public void setPoints(PointDouble startPt, PointDouble endPt) {
        setLine(startPt, endPt);
    }
}
