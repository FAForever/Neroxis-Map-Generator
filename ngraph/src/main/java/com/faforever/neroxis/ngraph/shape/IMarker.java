package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.util.Point;
import com.faforever.neroxis.ngraph.view.CellState;

public interface IMarker {

    Point paintMarker(Graphics2DCanvas canvas, CellState state, String type, Point pe, double nx, double ny, double size, boolean source);

}
