package com.faforever.neroxis.utilities;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public strictfp class TestingGround {
    public static void main(String[] args) throws Exception {
        DebugUtil.DEBUG = true;

        BooleanMask mask0 = new BooleanMask(64, 0L, new SymmetrySettings(Symmetry.NONE), "1", true);
        BooleanMask mask1 = new BooleanMask(64, 0L, new SymmetrySettings(Symmetry.NONE), "1", true);

        mask0.randomize(.5f);
        mask1.randomize(.5f);

        mask1.add(mask0);

        Pipeline.start();
        Pipeline.join();
        Pipeline.shutdown();
    }
}
