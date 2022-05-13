package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.util.PointDouble;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class ScaleAndTranslateEvent extends EventObject {
    PointDouble translate;
    PointDouble previousTranslate;
    double scale;
    double previousScale;
}
