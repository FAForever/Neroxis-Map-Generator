package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.style.HorizontalAlignment;
import com.faforever.neroxis.ngraph.style.Style;
import com.faforever.neroxis.ngraph.style.VerticalAlignment;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;

public class LabelShape extends ImageShape {

    /**
     * Draws the glass effect
     */
    public static void drawGlassEffect(Graphics2DCanvas canvas, CellState state) {
        double size = 0.4;
        canvas.getGraphics().setPaint(new GradientPaint((float) state.getX(), (float) state.getY(), new Color(1, 1, 1, 0.9f), (float) (state.getX()), (float) (state.getY() + state.getHeight() * size), new Color(1, 1, 1, 0.3f)));
        float sw = (float) (state.getStyle().getShape().getStrokeWidth() * canvas.getScale() / 2);

        GeneralPath path = new GeneralPath();
        path.moveTo((float) state.getX() - sw, (float) state.getY() - sw);
        path.lineTo((float) state.getX() - sw, (float) (state.getY() + state.getHeight() * size));
        path.quadTo((float) (state.getX() + state.getWidth() * 0.5), (float) (state.getY() + state.getHeight() * 0.7), (float) (state.getX() + state.getWidth() + sw), (float) (state.getY() + state.getHeight() * size));
        path.lineTo((float) (state.getX() + state.getWidth() + sw), (float) state.getY() - sw);
        path.closePath();
        canvas.getGraphics().fill(path);
    }

    public void paintShape(Graphics2DCanvas canvas, CellState state) {
        super.paintShape(canvas, state);
        if (state.getStyle().getCellProperties().isGlass()) {
            drawGlassEffect(canvas, state);
        }
    }

    public Rectangle getImageBounds(Graphics2DCanvas canvas, CellState state) {
        Style style = state.getStyle();
        double scale = canvas.getScale();
        HorizontalAlignment imgAlign = style.getImage().getHorizontalAlignment();
        VerticalAlignment imgValign = style.getImage().getVerticalAlignment();
        int imgWidth = (int) (style.getImage().getWidth() * scale);
        int imgHeight = (int) (style.getImage().getHeight() * scale);
        RectangleDouble imageBounds = new RectangleDouble(state);
        if (imgAlign == HorizontalAlignment.CENTER) {
            imageBounds.setX(imageBounds.getX() + (imageBounds.getWidth() - imgWidth) / 2);
        } else if (imgAlign == HorizontalAlignment.RIGHT) {
            imageBounds.setX(imageBounds.getX() + imageBounds.getWidth() - imgWidth - 2);
        } else {
            imageBounds.setX(imageBounds.getX() + 4);
        }
        if (imgValign == VerticalAlignment.TOP) {
            imageBounds.setY(imageBounds.getY());
        } else if (imgValign == VerticalAlignment.BOTTOM) {
            imageBounds.setY(imageBounds.getY() + imageBounds.getHeight() - imgHeight);
        } else {
            imageBounds.setY(imageBounds.getY() + (imageBounds.getHeight() - imgHeight) / 2);
        }
        imageBounds.setWidth(imgWidth);
        imageBounds.setHeight(imgHeight);
        return imageBounds.getRectangle();
    }

    public Color getFillColor(Graphics2DCanvas canvas, CellState state) {
        return state.getStyle().getShape().getFillColor();
    }

    public Color getStrokeColor(Graphics2DCanvas canvas, CellState state) {
        return state.getStyle().getShape().getStrokeColor();
    }

    public boolean hasGradient(Graphics2DCanvas canvas, CellState state) {
        return true;
    }

}
