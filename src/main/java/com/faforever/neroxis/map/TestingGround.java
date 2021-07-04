package com.faforever.neroxis.map;

import com.faforever.neroxis.map.mask.FloatMask;
import com.faforever.neroxis.util.Util;
import com.faforever.neroxis.util.Vector3;

public strictfp class TestingGround {

    public static void main(String[] args) throws Exception {
        Util.DEBUG = true;
        Util.VISUALIZE = true;

        FloatMask test = new FloatMask(512, 0L, new SymmetrySettings(Symmetry.NONE));

        test.addPerlinNoise(128, 25);

        Vector3 lightDirection = new Vector3(5, 1, 0).normalize();

        FloatMask shadows = new FloatMask(test.getShadowMask(lightDirection), 0, .5f, "shadows");
        shadows.blur(2);

        test.getNormalMask().dot(lightDirection).add(1f).divide(2f).subtract(shadows).clampMin(0f);
    }
}
