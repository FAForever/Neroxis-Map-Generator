package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.util.ICellOverlay;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class AddOverlayEvent extends EventObject {
    ICell cell;
    ICellOverlay overlay;
}
