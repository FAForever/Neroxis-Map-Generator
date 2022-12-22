package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.canvas.GraphicsCanvas2D;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.CellState;

import java.awt.*;
import java.util.Objects;

public class SwimlaneShape extends BasicShape {
    /**
     * Returns the bounding box for the gradient box for this shape.
     */
    protected double getTitleSize(Graphics2DCanvas canvas, CellState state) {
        return Math.max(0, state.getStyle().getEdge().getStartSize() * canvas.getScale());
    }

    @Override
    public void paintShape(Graphics2DCanvas canvas, CellState state) {
        double start = getTitleSize(canvas, state);
        Color fill = state.getStyle().getSwimlane().getColor();
        boolean swimlaneLine = state.getStyle().getSwimlane().isLine();
        double r = 0;
        if (state.getStyle().getCellProperties().isHorizontal()) {
            start = Math.min(start, state.getHeight());
        } else {
            start = Math.min(start, state.getWidth());
        }
        canvas.getGraphics().translate(state.getX(), state.getY());
        if (!state.getStyle().getCellProperties().isRounded()) {
            paintSwimlane(canvas, state, start, fill, swimlaneLine);
        } else {
            r = getArcSize(state, start);
            paintRoundedSwimlane(canvas, state, start, r, fill, swimlaneLine);
        }
        Color sep = state.getStyle().getShape().getSeparatorColor();
        paintSeparator(canvas, state, start, sep);
    }

    @Override
    protected RectangleDouble getGradientBounds(Graphics2DCanvas canvas, CellState state) {
        double start = getTitleSize(canvas, state);
        if (state.getStyle().getCellProperties().isHorizontal()) {
            start = Math.min(start, state.getHeight());
            return new RectangleDouble(state.getX(), state.getY(), state.getWidth(), start);
        } else {
            start = Math.min(start, state.getWidth());
            return new RectangleDouble(state.getX(), state.getY(), start, state.getHeight());
        }
    }

    /**
     * Helper method to configure the given wrapper canvas.
     */
    protected double getArcSize(CellState state, double start) {
        double f = Objects.requireNonNullElse(state.getStyle().getShape().getArcSize() / 100,
                                              RectangleShape.RECTANGLE_ROUNDING_FACTOR);

        return start * f * 3;
    }

    /**
     * Helper method to configure the given wrapper canvas.
     */
    protected GraphicsCanvas2D configureCanvas(Graphics2DCanvas canvas, CellState state, GraphicsCanvas2D c) {
        c.setShadow(hasShadow(canvas, state));
        c.setStrokeColor(state.getStyle().getShape().getStrokeColor());
        c.setStrokeWidth(state.getStyle().getShape().getStrokeWidth());
        c.setDashed(state.getStyle().getCellProperties().isDashed());
        Color fill = state.getStyle().getShape().getFillColor();
        Color gradient = state.getStyle().getShape().getGradientColor();
        if (fill != null && gradient != null) {
            RectangleDouble b = getGradientBounds(canvas, state);
            c.setGradient(fill, gradient, b.getX(), b.getY(), b.getWidth(), b.getHeight(),
                          state.getStyle().getShape().getGradientDirection(), 1, 1);
        } else {
            c.setFillColor(fill);
        }
        return c;
    }

    protected void paintSwimlane(Graphics2DCanvas canvas, CellState state, double start, Color fill,
                                 boolean swimlaneLine) {
        GraphicsCanvas2D canvas2D = configureCanvas(canvas, state, new GraphicsCanvas2D(canvas.getGraphics()));
        double width = state.getWidth();
        double height = state.getHeight();
        if (fill != null) {
            canvas2D.save();
            canvas2D.setFillColor(fill);
            canvas2D.rect(0, 0, width, height);
            canvas2D.fillAndStroke();
            canvas2D.restore();
            canvas2D.setShadow(false);
        }

        canvas2D.begin();
        if (state.getStyle().getCellProperties().isHorizontal()) {
            canvas2D.moveTo(0, start);
            canvas2D.lineTo(0, 0);
            canvas2D.lineTo(width, 0);
            canvas2D.lineTo(width, start);
            if (swimlaneLine || start >= height) {
                canvas2D.close();
            }
            canvas2D.fillAndStroke();
            // Transparent content area
            if (start < height && Constants.NONE.equals(fill)) {
                canvas2D.begin();
                canvas2D.moveTo(0, start);
                canvas2D.lineTo(0, height);
                canvas2D.lineTo(width, height);
                canvas2D.lineTo(width, start);
                canvas2D.stroke();
            }
        } else {
            canvas2D.moveTo(start, 0);
            canvas2D.lineTo(0, 0);
            canvas2D.lineTo(0, height);
            canvas2D.lineTo(start, height);

            if (swimlaneLine || start >= width) {
                canvas2D.close();
            }

            canvas2D.fillAndStroke();

            // Transparent content area
            if (start < width && Constants.NONE.equals(fill)) {
                canvas2D.begin();
                canvas2D.moveTo(start, 0);
                canvas2D.lineTo(width, 0);
                canvas2D.lineTo(width, height);
                canvas2D.lineTo(start, height);
                canvas2D.stroke();
            }
        }
    }

    /**
     * Function: paintRoundedSwimlane
     * <p>
     * Paints the swimlane vertex shape.
     */
    protected void paintRoundedSwimlane(Graphics2DCanvas canvas, CellState state, double start, double r, Color fill,
                                        boolean swimlaneLine) {
        GraphicsCanvas2D c = configureCanvas(canvas, state, new GraphicsCanvas2D(canvas.getGraphics()));
        double w = state.getWidth();
        double h = state.getHeight();
        if (fill != null) {
            c.save();
            c.setFillColor(fill);
            c.roundrect(0, 0, w, h, r, r);
            c.fillAndStroke();
            c.restore();
            c.setShadow(false);
        }

        c.begin();
        if (state.getStyle().getCellProperties().isHorizontal()) {
            c.moveTo(w, start);
            c.lineTo(w, r);
            c.quadTo(w, 0, w - Math.min(w / 2, r), 0);
            c.lineTo(Math.min(w / 2, r), 0);
            c.quadTo(0, 0, 0, r);
            c.lineTo(0, start);
            if (swimlaneLine || start >= h) {
                c.close();
            }

            c.fillAndStroke();

            // Transparent content area
            if (start < h && Constants.NONE.equals(fill)) {
                c.begin();
                c.moveTo(0, start);
                c.lineTo(0, h - r);
                c.quadTo(0, h, Math.min(w / 2, r), h);
                c.lineTo(w - Math.min(w / 2, r), h);
                c.quadTo(w, h, w, h - r);
                c.lineTo(w, start);
                c.stroke();
            }
        } else {
            c.moveTo(start, 0);
            c.lineTo(r, 0);
            c.quadTo(0, 0, 0, Math.min(h / 2, r));
            c.lineTo(0, h - Math.min(h / 2, r));
            c.quadTo(0, h, r, h);
            c.lineTo(start, h);

            if (swimlaneLine || start >= w) {
                c.close();
            }

            c.fillAndStroke();

            // Transparent content area
            if (start < w && Constants.NONE.equals(fill)) {
                c.begin();
                c.moveTo(start, h);
                c.lineTo(w - r, h);
                c.quadTo(w, h, w, h - Math.min(h / 2, r));
                c.lineTo(w, Math.min(h / 2, r));
                c.quadTo(w, 0, w - r, 0);
                c.lineTo(start, 0);
                c.stroke();
            }
        }
    }

    /**
     * Function: paintSwimlane
     * <p>
     * Paints the swimlane vertex shape.
     */
    protected void paintSeparator(Graphics2DCanvas canvas, CellState state, double start, Color color) {
        GraphicsCanvas2D c = new GraphicsCanvas2D(canvas.getGraphics());
        double w = state.getWidth();
        double h = state.getHeight();
        if (color != null) {
            c.setStrokeColor(color);
            c.setDashed(true);
            c.begin();
            if (state.getStyle().getCellProperties().isHorizontal()) {
                c.moveTo(w, start);
                c.lineTo(w, h);
            } else {
                c.moveTo(start, 0);
                c.lineTo(w, 0);
            }

            c.stroke();
            c.setDashed(false);
        }
    }
}
