/**
 * Copyright (c) 2007-2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.canvas;

import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * An implementation of a canvas that uses Graphics2D for painting. To use an
 * image canvas for an existing graphics canvas and create an image the
 * following code is used:
 *
 * <code>BufferedImage image = CellRenderer.createBufferedImage(graph, cells, 1, Color.white, true, null, canvas);</code>
 */
public class ImageCanvas implements ICanvas {

    protected Graphics2DCanvas canvas;

    protected Graphics2D previousGraphics;

    protected BufferedImage image;

    public ImageCanvas(Graphics2DCanvas canvas, int width, int height, Color background, boolean antiAlias) {
        this(canvas, width, height, background, antiAlias, true);
    }

    public ImageCanvas(Graphics2DCanvas canvas, int width, int height, Color background, boolean antiAlias, boolean textAntiAlias) {
        this.canvas = canvas;
        previousGraphics = canvas.getGraphics();
        image = Utils.createBufferedImage(width, height, background);

        if (image != null) {
            Graphics2D g = image.createGraphics();
            Utils.setAntiAlias(g, antiAlias, textAntiAlias);
            canvas.setGraphics(g);
        }
    }

    public Graphics2DCanvas getGraphicsCanvas() {
        return canvas;
    }

    public BufferedImage getImage() {
        return image;
    }

    public Object drawCell(CellState state) {
        return canvas.drawCell(state);
    }

    public Object drawLabel(String label, CellState state, boolean html) {
        return canvas.drawLabel(label, state, html);
    }

    public double getScale() {
        return canvas.getScale();
    }

    public void setScale(double scale) {
        canvas.setScale(scale);
    }

    public PointDouble getTranslate() {
        return canvas.getTranslate();
    }

    public void setTranslate(double dx, double dy) {
        canvas.setTranslate(dx, dy);
    }

    public BufferedImage destroy() {
        BufferedImage tmp = image;

        if (canvas.getGraphics() != null) {
            canvas.getGraphics().dispose();
        }

        canvas.setGraphics(previousGraphics);

        previousGraphics = null;
        canvas = null;
        image = null;

        return tmp;
    }

}
