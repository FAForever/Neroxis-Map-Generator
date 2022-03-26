package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.canvas.GraphicsCanvas2D;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Rectangle;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;

public class SwimlaneShape extends BasicShape {

    /**
     * Returns the bounding box for the gradient box for this shape.
     */
    protected double getTitleSize(Graphics2DCanvas canvas, CellState state) {
        return Math.max(0, Utils.getFloat(state.getStyle(), Constants.STYLE_STARTSIZE, Constants.DEFAULT_STARTSIZE) * canvas.getScale());
    }

    protected Rectangle getGradientBounds(Graphics2DCanvas canvas, CellState state) {
        double start = getTitleSize(canvas, state);

        if (Utils.isTrue(state.getStyle(), Constants.STYLE_HORIZONTAL, true)) {
            start = Math.min(start, state.getHeight());

            return new Rectangle(state.getX(), state.getY(), state.getWidth(), start);
        } else {
            start = Math.min(start, state.getWidth());

            return new Rectangle(state.getX(), state.getY(), start, state.getHeight());
        }
    }

    public void paintShape(Graphics2DCanvas canvas, CellState state) {
        double start = getTitleSize(canvas, state);
        String fill = Utils.getString(state.getStyle(), Constants.STYLE_SWIMLANE_FILLCOLOR, Constants.NONE);
        boolean swimlaneLine = Utils.isTrue(state.getStyle(), Constants.STYLE_SWIMLANE_LINE, true);
        double r = 0;

        if (Utils.isTrue(state.getStyle(), Constants.STYLE_HORIZONTAL, true)) {
            start = Math.min(start, state.getHeight());
        } else {
            start = Math.min(start, state.getWidth());
        }

        canvas.getGraphics().translate(state.getX(), state.getY());

        if (!Utils.isTrue(state.getStyle(), Constants.STYLE_ROUNDED)) {
            paintSwimlane(canvas, state, start, fill, swimlaneLine);
        } else {
            r = getArcSize(state, start);
            paintRoundedSwimlane(canvas, state, start, r, fill, swimlaneLine);
        }

        String sep = Utils.getString(state.getStyle(), Constants.STYLE_SEPARATORCOLOR, Constants.NONE);
        paintSeparator(canvas, state, start, sep);
    }

    /**
     * Helper method to configure the given wrapper canvas.
     */
    protected double getArcSize(CellState state, double start) {
        double f = Utils.getDouble(state.getStyle(), Constants.STYLE_ARCSIZE, Constants.RECTANGLE_ROUNDING_FACTOR * 100) / 100;

        return start * f * 3;
    }

    /**
     * Helper method to configure the given wrapper canvas.
     */
    protected GraphicsCanvas2D configureCanvas(Graphics2DCanvas canvas, CellState state, GraphicsCanvas2D c) {
        c.setShadow(hasShadow(canvas, state));
        c.setStrokeColor(Utils.getString(state.getStyle(), Constants.STYLE_STROKECOLOR, Constants.NONE));
        c.setStrokeWidth(Utils.getInt(state.getStyle(), Constants.STYLE_STROKEWIDTH, 1));
        c.setDashed(Utils.isTrue(state.getStyle(), Constants.STYLE_DASHED, false));

        String fill = Utils.getString(state.getStyle(), Constants.STYLE_FILLCOLOR, Constants.NONE);
        String gradient = Utils.getString(state.getStyle(), Constants.STYLE_GRADIENTCOLOR, Constants.NONE);

        if (!Constants.NONE.equals(fill) && !Constants.NONE.equals(gradient)) {
            Rectangle b = getGradientBounds(canvas, state);
            c.setGradient(fill, gradient, b.getX(), b.getY(), b.getWidth(), b.getHeight(), Utils.getString(state.getStyle(), Constants.STYLE_GRADIENT_DIRECTION, Constants.DIRECTION_NORTH), 1, 1);
        } else {
            c.setFillColor(fill);
        }

        return c;
    }

    protected void paintSwimlane(Graphics2DCanvas canvas, CellState state, double start, String fill, boolean swimlaneLine) {
        GraphicsCanvas2D c = configureCanvas(canvas, state, new GraphicsCanvas2D(canvas.getGraphics()));
        double w = state.getWidth();
        double h = state.getHeight();

        if (!Constants.NONE.equals(fill)) {
            c.save();
            c.setFillColor(fill);
            c.rect(0, 0, w, h);
            c.fillAndStroke();
            c.restore();
            c.setShadow(false);
        }

        c.begin();

        if (Utils.isTrue(state.getStyle(), Constants.STYLE_HORIZONTAL, true)) {
            c.moveTo(0, start);
            c.lineTo(0, 0);
            c.lineTo(w, 0);
            c.lineTo(w, start);

            if (swimlaneLine || start >= h) {
                c.close();
            }

            c.fillAndStroke();

            // Transparent content area
            if (start < h && Constants.NONE.equals(fill)) {
                c.begin();
                c.moveTo(0, start);
                c.lineTo(0, h);
                c.lineTo(w, h);
                c.lineTo(w, start);
                c.stroke();
            }
        } else {
            c.moveTo(start, 0);
            c.lineTo(0, 0);
            c.lineTo(0, h);
            c.lineTo(start, h);

            if (swimlaneLine || start >= w) {
                c.close();
            }

            c.fillAndStroke();

            // Transparent content area
            if (start < w && Constants.NONE.equals(fill)) {
                c.begin();
                c.moveTo(start, 0);
                c.lineTo(w, 0);
                c.lineTo(w, h);
                c.lineTo(start, h);
                c.stroke();
            }
        }
    }

    /**
     * Function: paintRoundedSwimlane
     * <p>
     * Paints the swimlane vertex shape.
     */
    protected void paintRoundedSwimlane(Graphics2DCanvas canvas, CellState state, double start, double r, String fill, boolean swimlaneLine) {
        GraphicsCanvas2D c = configureCanvas(canvas, state, new GraphicsCanvas2D(canvas.getGraphics()));
        double w = state.getWidth();
        double h = state.getHeight();

        if (!Constants.NONE.equals(fill)) {
            c.save();
            c.setFillColor(fill);
            c.roundrect(0, 0, w, h, r, r);
            c.fillAndStroke();
            c.restore();
            c.setShadow(false);
        }

        c.begin();

        if (Utils.isTrue(state.getStyle(), Constants.STYLE_HORIZONTAL, true)) {
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
    protected void paintSeparator(Graphics2DCanvas canvas, CellState state, double start, String color) {
        GraphicsCanvas2D c = new GraphicsCanvas2D(canvas.getGraphics());
        double w = state.getWidth();
        double h = state.getHeight();

        if (!Constants.NONE.equals(color)) {
            c.setStrokeColor(color);
            c.setDashed(true);
            c.begin();

            if (Utils.isTrue(state.getStyle(), Constants.STYLE_HORIZONTAL, true)) {
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
