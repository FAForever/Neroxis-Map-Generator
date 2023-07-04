package com.faforever.neroxis.utilities;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.DebugUtil;

import java.util.Random;

public class TestingGround {
    public static void main(String[] args) throws Exception {
        DebugUtil.DEBUG = true;

        FloatMask floatMask = new FloatMask(1024, new Random().nextLong(), new SymmetrySettings(Symmetry.POINT2));
        floatMask.startVisualDebugger();

        floatMask.addPerlinNoise(256, 1).addPerlinNoise(64, .5f).addPerlinNoise(32, .25f);
        floatMask.copyAsBooleanMask(-.65f, .65f)
                 .startVisualDebugger()
                 .resample(128)
                 .dilute(.5f, 4)
                 .resample(1024)
                 .forceSymmetry()
                 .blur(16);
    }
}
