package com.faforever.neroxis.utilities;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;

public class TestingGround {
    void main() {
        FloatMask outsideMask = new FloatMask(4096, 0L, new SymmetrySettings(Symmetry.NONE));
//        outsideMask.startVisualDebugger();
        outsideMask.add(1f);
        outsideMask.addPrimitive(1f);
        outsideMask.vectorizedAdd(1f);
    }
}
