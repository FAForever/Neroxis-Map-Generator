/**
 * Copyright (c) 2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.LightweightLabel;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * To set global CSS for all HTML labels, use the following code:
 *
 * <pre>
 * Graphics2DCanvas.putTextShape(Graphics2DCanvas.TEXT_SHAPE_HTML,
 *   new HtmlTextShape()
 *   {
 *     protected String createHtmlDocument(Map<String, Object> style, String text)
 *     {
 *       return Utils.createHtmlDocument(style, text, 1, 0,
 *           "<style type=\"text/css\">.selectRef { " +
 *           "font-size:9px;font-weight:normal; }</style>");
 *     }
 *   }
 * );
 * </pre>
 */
public class HtmlTextShape implements ITextShape {

    /**
     * Specifies if linefeeds should be replaced with breaks in HTML markup.
     * Default is true.
     */
    protected boolean replaceHtmlLinefeeds = true;

    /**
     * Returns replaceHtmlLinefeeds
     */
    public boolean isReplaceHtmlLinefeeds() {
        return replaceHtmlLinefeeds;
    }

    /**
     * Returns replaceHtmlLinefeeds
     */
    public void setReplaceHtmlLinefeeds(boolean value) {
        replaceHtmlLinefeeds = value;
    }

    /**
     *
     */
    protected String createHtmlDocument(Map<String, Object> style, String text, int w, int h) {
        String overflow = Utils.getString(style, Constants.STYLE_OVERFLOW, "");

        if (overflow.equals("fill")) {
            return Utils.createHtmlDocument(style, text, 1, w, null, "height:" + h + "pt;");
        } else if (overflow.equals("width")) {
            return Utils.createHtmlDocument(style, text, 1, w);
        } else {
            return Utils.createHtmlDocument(style, text);
        }
    }

    /**
     *
     */
    public void paintShape(Graphics2DCanvas canvas, String text, CellState state, Map<String, Object> style) {
        LightweightLabel textRenderer = LightweightLabel.getSharedInstance();
        CellRendererPane rendererPane = canvas.getRendererPane();
        Rectangle rect = state.getLabelBounds().getRectangle();
        Graphics2D g = canvas.getGraphics();

        if (textRenderer != null && rendererPane != null && (g.getClipBounds() == null || g.getClipBounds().intersects(rect))) {
            double scale = canvas.getScale();
            int x = rect.x;
            int y = rect.y;
            int w = rect.width;
            int h = rect.height;

            if (!Utils.isTrue(style, Constants.STYLE_HORIZONTAL, true)) {
                g.rotate(-Math.PI / 2, x + w / 2, y + h / 2);
                g.translate(w / 2 - h / 2, h / 2 - w / 2);

                int tmp = w;
                w = h;
                h = tmp;
            }

            // Replaces the linefeeds with BR tags
            if (isReplaceHtmlLinefeeds()) {
                text = text.replaceAll("\n", "<br>");
            }

            // Renders the scaled text
            textRenderer.setText(createHtmlDocument(style, text, (int) Math.round(w / state.getView().getScale()), (int) Math.round(h / state.getView().getScale())));
            textRenderer.setFont(Utils.getFont(style, canvas.getScale()));
            g.scale(scale, scale);
            rendererPane.paintComponent(g, textRenderer, rendererPane, (int) (x / scale) + Constants.LABEL_INSET, (int) (y / scale) + Constants.LABEL_INSET, (int) (w / scale), (int) (h / scale), true);
        }
    }

}
