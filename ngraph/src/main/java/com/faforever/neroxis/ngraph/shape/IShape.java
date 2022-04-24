package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.view.CellState;

public interface IShape {

    void paintShape(Graphics2DCanvas canvas, CellState state);
}
