package com.faforever.neroxis.map;

import com.faforever.neroxis.map.mask.FloatMask;
import com.faforever.neroxis.util.Util;

import java.util.Random;

public strictfp class TestingGround {

    public static void main(String[] args) throws Exception {
        Util.DEBUG = true;
        Util.VISUALIZE = true;

        FloatMask test = new FloatMask(129, new Random().nextLong(), new SymmetrySettings(Symmetry.POINT3));

        float middle = (test.getSize() - 1) / 2f;

        test.addWhiteNoise(10).getNormalMask();

//        Vector2 start = new Vector2(64, 64);
//        Vector2 end = new Vector2(64 + 128 + 256, 64 + 128 + 256);
//
//        test.clear();
//        test.path(start, end, 1, 5, 128, 64, (float) (StrictMath.PI / 2), SymmetryType.SPAWN);
//        test.clear();
//        test.pathBezier(start, end, 2, 10, 5, 128, 64);
    }
}
