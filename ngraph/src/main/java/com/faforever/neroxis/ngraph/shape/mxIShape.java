package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.mxGraphics2DCanvas;
import com.faforever.neroxis.ngraph.view.mxCellState;

public interface mxIShape {
    /**
     *
     */
    void paintShape(mxGraphics2DCanvas canvas, mxCellState state);

}
