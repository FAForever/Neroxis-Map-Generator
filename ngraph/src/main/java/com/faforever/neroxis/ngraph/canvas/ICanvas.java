/**
 * Copyright (c) 2007-2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.canvas;

import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;

/**
 * Defines the requirements for a canvas that paints the vertices and edges of
 * a graph.
 */
public interface ICanvas {
    /**
     * Sets the translation for the following drawing requests.
     */
    void setTranslate(double x, double y);

    /**
     * Returns the current translation.
     *
     * @return Returns the current translation.
     */
    PointDouble getTranslate();

    /**
     * Returns the scale.
     */
    double getScale();

    /**
     * Sets the scale for the following drawing requests.
     */
    void setScale(double scale);

    /**
     * Draws the given cell.
     *
     * @param state State of the cell to be painted.
     * @return Object that represents the cell.
     */
    Object drawCell(CellState state);

    /**
     * Draws the given label.
     *
     * @param text  String that represents the label.
     * @param state State of the cell whose label is to be painted.
     * @return Object that represents the label.
     */
    Object drawLabel(String text, CellState state);
}
