package com.faforever.neroxis.ngraph.swing.util;

import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.CellState;

public interface ICellOverlay {
    RectangleDouble getBounds(CellState state);
}
