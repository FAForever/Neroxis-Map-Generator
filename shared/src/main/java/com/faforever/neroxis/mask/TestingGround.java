package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.DebugUtil;

public strictfp class TestingGround {

    public static void main(String[] args) throws Exception {
        DebugUtil.DEBUG = true;

        FloatMask mask = new FloatMask(1024, 0L, new SymmetrySettings(Symmetry.NONE));
        BooleanMask booleanMask = new BooleanMask(1024, 0L, new SymmetrySettings(Symmetry.NONE)).invert();

        mask.useBrushWithinAreaWithDensity(booleanMask, "mountain1.png", 64, 1f, 1f, false);
    }
}
