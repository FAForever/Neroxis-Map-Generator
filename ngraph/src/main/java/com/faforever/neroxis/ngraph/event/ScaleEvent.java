package com.faforever.neroxis.ngraph.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class ScaleEvent extends EventObject {

    double scale;
    double previousScale;
}
