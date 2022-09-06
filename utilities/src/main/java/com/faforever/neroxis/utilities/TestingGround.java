package com.faforever.neroxis.utilities;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;

public strictfp class TestingGround {
    public static void main(String[] args) throws Exception {
        DebugUtil.DEBUG = true;
        DebugUtil.VISUALIZE = true;

        BooleanMask mask = new BooleanMask(8, 0L, new SymmetrySettings(Symmetry.POINT2));

        mask.randomize(.74f).resample(11);
    }
}
