package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.util.Point;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class ScaleAndTranslateEvent extends EventObject {
    Point translate;
    Point previousTranslate;
    double scale;
    double previousScale;
}
