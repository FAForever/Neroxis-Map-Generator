/**
 * Copyright (c) 2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.style.Style;
import com.faforever.neroxis.ngraph.style.util.HorizontalAlignment;
import com.faforever.neroxis.ngraph.style.util.VerticalAlignment;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;

import java.awt.*;

public class DefaultTextShape implements ITextShape {
    @Override
    public void paintShape(Graphics2DCanvas canvas, String text, CellState state, Style style) {
        Rectangle rect = state.getLabelBounds().getRectangle();
        Graphics2D g = canvas.getGraphics();
        if (g.getClipBounds() == null || g.getClipBounds().intersects(rect)) {
            boolean horizontal = style.getCellProperties().isHorizontal();
            double scale = canvas.getScale();
            int x = rect.x;
            int y = rect.y;
            int w = rect.width;
            int h = rect.height;

            if (!horizontal) {
                g.rotate(-Math.PI / 2, x + w / 2, y + h / 2);
                g.translate(w / 2 - h / 2, h / 2 - w / 2);
            }
            Color fontColor = style.getLabel().getTextColor();
            g.setColor(fontColor);

            // Shifts the y-coordinate down by the ascent plus a workaround
            // for the line not starting at the exact vertical location
            Font scaledFont = Utils.getFont(style, scale);
            g.setFont(scaledFont);
            int fontSize = style.getLabel().getFontSize();
            FontMetrics fm = g.getFontMetrics();
            int scaledFontSize = scaledFont.getSize();
            double fontScaleFactor = ((double) scaledFontSize) / ((double) fontSize);
            // This factor is the amount by which the font is smaller/
            // larger than we expect for the given scale. 1 means it's
            // correct, 0.8 means the font is 0.8 the size we expected
            // when scaled, etc.
            double fontScaleRatio = fontScaleFactor / scale;
            // The y position has to be moved by (1 - ratio) * height / 2
            y += 2 * fm.getMaxAscent() - fm.getHeight() + Constants.LABEL_INSET * scale;
            VerticalAlignment verticalAlignment = style.getLabel().getVerticalAlignment();
            double vertAlignProportion = 0.5;
            if (verticalAlignment == VerticalAlignment.TOP) {
                vertAlignProportion = 0;
            } else if (verticalAlignment == VerticalAlignment.BOTTOM) {
                vertAlignProportion = 1.0;
            }
            y += (1.0 - fontScaleRatio) * h * vertAlignProportion;
            // Gets the alignment settings
            HorizontalAlignment horizontalAlignment = style.getLabel().getHorizontalAlignment();
            if (horizontalAlignment == HorizontalAlignment.LEFT) {
                x += Constants.LABEL_INSET * scale;
            } else if (horizontalAlignment == HorizontalAlignment.RIGHT) {
                x -= Constants.LABEL_INSET * scale;
            }
            // Draws the text line by line
            String[] lines = text.split("\n");
            for (String line : lines) {
                int dx = 0;
                if (horizontalAlignment == HorizontalAlignment.CENTER) {
                    int sw = fm.stringWidth(line);
                    if (horizontal) {
                        dx = (w - sw) / 2;
                    } else {
                        dx = (h - sw) / 2;
                    }
                } else if (horizontalAlignment == HorizontalAlignment.RIGHT) {
                    int sw = fm.stringWidth(line);
                    dx = ((horizontal) ? w : h) - sw;
                }
                g.drawString(line, x + dx, y);
                postProcessLine(text, line, fm, canvas, x + dx, y);
                y += fm.getHeight() + Constants.LINESPACING;
            }
        }
    }

    /**
     * Hook to add functionality after a line has been drawn
     *
     * @param text   the entire label text
     * @param line   the line at the specified location
     * @param fm     the text font metrics
     * @param canvas the canvas object currently being painted to
     * @param x      the x co-ord of the baseline of the text line
     * @param y      the y co-ord of the baseline of the text line
     */
    protected void postProcessLine(String text, String line, FontMetrics fm, Graphics2DCanvas canvas, int x, int y) {}
}
