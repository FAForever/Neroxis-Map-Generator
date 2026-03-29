package com.faforever.neroxis.utilities;

import com.faforever.neroxis.util.vector.Vector2;

import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;

public class TestingGround {
    void main() {
        FloatMask outsideMask = new FloatMask(100, new SplittableRandom(0L),
                                              new SymmetrySettings(Symmetry.NONE)).startVisualDebugger();
        outsideMask.addGaussianNoise(100f).shiftToPositive();
        Set<Vector2> positions = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            positions.add(outsideMask.getRandomPosition());
        }
        outsideMask.set((x, y) -> positions.contains(new Vector2(x, y)) ? outsideMask.get(x, y) : 0);
    }
}
