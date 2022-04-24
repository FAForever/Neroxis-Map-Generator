/**
 * Copyright (c) 2007-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.util;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.canvas.ICanvas;
import com.faforever.neroxis.ngraph.canvas.ImageCanvas;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.view.Graph;
import com.faforever.neroxis.ngraph.view.GraphView;
import com.faforever.neroxis.ngraph.view.TemporaryCellStates;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

public class CellRenderer {

    private CellRenderer() {
        // static class
    }

    public static BufferedImage createBufferedImage(Graph graph, List<ICell> cells, double scale, Color background,
                                                    boolean antiAlias, RectangleDouble clip) {
        return createBufferedImage(graph, cells, scale, background, antiAlias, clip, new Graphics2DCanvas());
    }

    public static BufferedImage createBufferedImage(Graph graph, List<ICell> cells, double scale,
                                                    final Color background, final boolean antiAlias,
                                                    RectangleDouble clip, final Graphics2DCanvas graphicsCanvas) {
        ImageCanvas canvas = (ImageCanvas) drawCells(graph, cells, scale, clip, new CanvasFactory() {
            @Override
            public ICanvas createCanvas(int width, int height) {
                return new ImageCanvas(graphicsCanvas, width, height, background, antiAlias);
            }
        });
        return (canvas != null) ? canvas.destroy() : null;
    }

    /**
     * Draws the given cells using a Graphics2D canvas and returns the buffered image
     * that represents the cells.
     *
     * @param graph Graph to be painted onto the canvas.
     * @return Returns the image that represents the canvas.
     */
    public static ICanvas drawCells(Graph graph, List<ICell> cells, double scale, RectangleDouble clip,
                                    CanvasFactory factory) {
        ICanvas canvas = null;
        if (cells == null) {
            cells = List.of(graph.getModel().getRoot());
        }
        // Gets the current state of the view
        GraphView view = graph.getView();
        // Keeps the existing translation as the cells might
        // be aligned to the grid in a different way in a graph
        // that has a translation other than zero
        boolean eventsEnabled = view.isEventsEnabled();

        // Disables firing of scale events so that there is no
        // repaint or update of the original graph
        view.setEventsEnabled(false);

        // Uses the view to create temporary cell states for each cell
        TemporaryCellStates temp = new TemporaryCellStates(view, scale, cells);

        try {
            if (clip == null) {
                clip = graph.getPaintBounds(cells);
            }

            if (clip != null && clip.getWidth() > 0 && clip.getHeight() > 0) {
                java.awt.Rectangle rect = clip.getRectangle();
                canvas = factory.createCanvas(rect.width + 1, rect.height + 1);

                if (canvas != null) {
                    double previousScale = canvas.getScale();
                    PointDouble previousTranslate = canvas.getTranslate();

                    try {
                        canvas.setTranslate(-rect.x, -rect.y);
                        canvas.setScale(view.getScale());

                        for (ICell cell : cells) {
                            graph.drawCell(canvas, cell);
                        }
                    } finally {
                        canvas.setScale(previousScale);
                        canvas.setTranslate(previousTranslate.getX(), previousTranslate.getY());
                    }
                }
            }
        } finally {
            temp.destroy();
            view.setEventsEnabled(eventsEnabled);
        }

        return canvas;
    }

    public static abstract class CanvasFactory {

        /**
         * Separates the creation of the canvas from its initialization, when the
         * size of the required graphics buffer / document / container is known.
         */
        public abstract ICanvas createCanvas(int width, int height);
    }
}
