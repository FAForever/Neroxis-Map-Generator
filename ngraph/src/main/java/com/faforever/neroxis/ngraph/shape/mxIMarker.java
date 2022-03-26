package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.mxGraphics2DCanvas;
import com.faforever.neroxis.ngraph.util.mxPoint;
import com.faforever.neroxis.ngraph.view.mxCellState;

public interface mxIMarker {
    /**
     *
     */
    mxPoint paintMarker(mxGraphics2DCanvas canvas, mxCellState state, String type, mxPoint pe, double nx, double ny, double size, boolean source);

}
