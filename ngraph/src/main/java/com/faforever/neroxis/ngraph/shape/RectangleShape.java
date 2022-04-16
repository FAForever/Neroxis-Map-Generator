/**
 * Copyright (c) 2007-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.swing.util.SwingConstants;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.Rectangle;
import java.util.Map;

public class RectangleShape extends BasicShape {
    public static final double RECTANGLE_ROUNDING_FACTOR = 0.15;

    public void paintShape(Graphics2DCanvas canvas, CellState state) {
        Map<String, Object> style = state.getStyle();

        if (Utils.isTrue(style, Constants.STYLE_ROUNDED, false)) {
            Rectangle tmp = state.getRectangle();

            int x = tmp.x;
            int y = tmp.y;
            int w = tmp.width;
            int h = tmp.height;
            int radius = getArcSize(state, w, h);

            boolean shadow = hasShadow(canvas, state);
            int shadowOffsetX = (shadow) ? Constants.SHADOW_OFFSETX : 0;
            int shadowOffsetY = (shadow) ? Constants.SHADOW_OFFSETY : 0;

            if (canvas.getGraphics().hitClip(x, y, w + shadowOffsetX, h + shadowOffsetY)) {
                // Paints the optional shadow
                if (shadow) {
                    canvas.getGraphics().setColor(SwingConstants.SHADOW_COLOR);
                    canvas.getGraphics().fillRoundRect(x + Constants.SHADOW_OFFSETX, y + Constants.SHADOW_OFFSETY, w, h, radius, radius);
                }

                // Paints the background
                if (configureGraphics(canvas, state, true)) {
                    canvas.getGraphics().fillRoundRect(x, y, w, h, radius, radius);
                }

                // Paints the foreground
                if (configureGraphics(canvas, state, false)) {
                    canvas.getGraphics().drawRoundRect(x, y, w, h, radius, radius);
                }
            }
        } else {
            Rectangle rect = state.getRectangle();

            // Paints the background
            if (configureGraphics(canvas, state, true)) {
                canvas.fillShape(rect, hasShadow(canvas, state));
            }

            // Paints the foreground
            if (configureGraphics(canvas, state, false)) {
                canvas.getGraphics().drawRect(rect.x, rect.y, rect.width, rect.height);
            }
        }
    }

    /**
     * Helper method to configure the given wrapper canvas.
     */
    protected int getArcSize(CellState state, double w, double h) {
        double f = Utils.getDouble(state.getStyle(), Constants.STYLE_ARCSIZE, RECTANGLE_ROUNDING_FACTOR * 100) / 100;

        return (int) (Math.min(w, h) * f * 2);
    }

}
