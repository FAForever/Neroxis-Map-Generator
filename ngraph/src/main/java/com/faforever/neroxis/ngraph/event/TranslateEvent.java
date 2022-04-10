package com.faforever.neroxis.ngraph.event;

import com.faforever.neroxis.ngraph.util.PointDouble;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class TranslateEvent extends EventObject {
    PointDouble translate;
    PointDouble previousTranslate;
}
