package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.style.arrow.Arrow;
import com.faforever.neroxis.ngraph.style.arrow.BlockArrow;
import com.faforever.neroxis.ngraph.style.arrow.ClassicArrow;
import com.faforever.neroxis.ngraph.style.arrow.DiamondArrow;
import com.faforever.neroxis.ngraph.style.arrow.OpenArrow;
import com.faforever.neroxis.ngraph.style.arrow.OvalArrow;
import com.faforever.neroxis.ngraph.util.Constants;
import java.util.HashMap;
import java.util.Map;

public class ArrowRegistry {

    protected static Map<String, Arrow> arrows = new HashMap<>();

    static {
        registerArrow(Constants.ARROW_CLASSIC, new ClassicArrow());
        registerArrow(Constants.ARROW_BLOCK, new BlockArrow());
        registerArrow(Constants.ARROW_OPEN, new OpenArrow());
        registerArrow(Constants.ARROW_OVAL, new OvalArrow());
        registerArrow(Constants.ARROW_DIAMOND, new DiamondArrow());
    }

    public static Arrow getArrow(String name) {
        return arrows.get(name);
    }

    public static void registerArrow(String name, Arrow marker) {
        arrows.put(name, marker);
    }
}
