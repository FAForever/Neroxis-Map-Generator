/**
 * Copyright (c) 2007-2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.Color;
import java.awt.Rectangle;

/**
 * A rectangular shape that contains a single image. See ImageBundle for
 * creating a lookup table with images which can then be referenced by key.
 */
public class ImageShape extends RectangleShape {

    @Override
    public void paintShape(Graphics2DCanvas canvas, CellState state) {
        super.paintShape(canvas, state);
        boolean flipH = state.getStyle().getImage().isFlipHorizontal();
        boolean flipV = state.getStyle().getImage().isFlipVertical();
        canvas.drawImage(getImageBounds(canvas, state), getImageForStyle(canvas, state),
                         Graphics2DCanvas.PRESERVE_IMAGE_ASPECT, flipH, flipV);
    }

    public Rectangle getImageBounds(Graphics2DCanvas canvas, CellState state) {
        return state.getRectangle();
    }

    public String getImageForStyle(Graphics2DCanvas canvas, CellState state) {
        return canvas.getImageForStyle(state.getStyle());
    }

    @Override
    public boolean hasGradient(Graphics2DCanvas canvas, CellState state) {
        return false;
    }

    @Override
    public Color getFillColor(Graphics2DCanvas canvas, CellState state) {
        return state.getStyle().getImage().getBackgroundColor();
    }

    @Override
    public Color getStrokeColor(Graphics2DCanvas canvas, CellState state) {
        return state.getStyle().getImage().getBorderColor();
    }
}
