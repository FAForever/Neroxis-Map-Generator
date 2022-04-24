package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;

public class CloudShape extends BasicShape {

    @Override
    public Shape createShape(Graphics2DCanvas canvas, CellState state) {
        Rectangle temp = state.getRectangle();
        int x = temp.x;
        int y = temp.y;
        int w = temp.width;
        int h = temp.height;
        GeneralPath path = new GeneralPath();

        path.moveTo((float) (x + 0.25 * w), (float) (y + 0.25 * h));
        path.curveTo((float) (x + 0.05 * w), (float) (y + 0.25 * h), x, (float) (y + 0.5 * h), (float) (x + 0.16 * w),
                     (float) (y + 0.55 * h));
        path.curveTo(x, (float) (y + 0.66 * h), (float) (x + 0.18 * w), (float) (y + 0.9 * h), (float) (x + 0.31 * w),
                     (float) (y + 0.8 * h));
        path.curveTo((float) (x + 0.4 * w), (y + h), (float) (x + 0.7 * w), (y + h), (float) (x + 0.8 * w),
                     (float) (y + 0.8 * h));
        path.curveTo((x + w), (float) (y + 0.8 * h), (x + w), (float) (y + 0.6 * h), (float) (x + 0.875 * w),
                     (float) (y + 0.5 * h));
        path.curveTo((x + w), (float) (y + 0.3 * h), (float) (x + 0.8 * w), (float) (y + 0.1 * h),
                     (float) (x + 0.625 * w), (float) (y + 0.2 * h));
        path.curveTo((float) (x + 0.5 * w), (float) (y + 0.05 * h), (float) (x + 0.3 * w), (float) (y + 0.05 * h),
                     (float) (x + 0.25 * w), (float) (y + 0.25 * h));
        path.closePath();

        return path;
    }
}
