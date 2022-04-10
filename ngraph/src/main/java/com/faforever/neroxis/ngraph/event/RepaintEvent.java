package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.util.Rectangle;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class RepaintEvent extends EventObject {
    Rectangle region;
}
