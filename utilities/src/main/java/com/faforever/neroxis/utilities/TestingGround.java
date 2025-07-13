package com.faforever.neroxis.utilities;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;

import java.util.Random;

public class TestingGround {
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 100; i++) {
            int size = 512;
            FloatMask floatMask = new FloatMask(size, new Random().nextLong(), new SymmetrySettings(Symmetry.POINT2));

            floatMask.addPerlinNoise(size / 4, 1).addPerlinNoise(size / 8, .5f).addPerlinNoise(size / 16, .25f);
            floatMask.copyAsBooleanMask(-.65f, .65f)
                     .startVisualDebugger()
                     .resample(size / 8)
                     .dilute(.5f, 4)
                     .resample(size)
                     .blur(16);
        }
    }
}
