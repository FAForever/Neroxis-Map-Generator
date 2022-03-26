package com.faforever.neroxis.ngraph.swing.util;

import com.faforever.neroxis.ngraph.util.Rectangle;
import com.faforever.neroxis.ngraph.view.CellState;

public interface ICellOverlay {

    /**
     *
     */
    Rectangle getBounds(CellState state);

}
