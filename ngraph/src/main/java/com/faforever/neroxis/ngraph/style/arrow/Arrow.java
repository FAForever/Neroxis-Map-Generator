package com.faforever.neroxis.ngraph.style.arrow;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;

public interface Arrow {

    PointDouble paintArrow(Graphics2DCanvas canvas, CellState state, PointDouble pe, double nx, double ny, double size,
                           boolean source);
}
