package com.faforever.neroxis.ngraph.style.util;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.view.Graph;

/**
 * Defines the key for the overflow style. Possible values are "visible",
 * "hidden" and "fill". The default value is "visible". This value
 * specifies how overlapping vertex labels are handles. A value of
 * "visible" will show the complete label. A value of "hidden" will clip
 * the label so that it does not overlap the vertex bounds. A value of
 * "fill" will use the vertex bounds for the label.
 *
 * @see Graph#isLabelClipped(ICell)
 */
public enum Overflow {
    VISIBLE, HIDDEN, FILL, WIDTH
}
