package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.SCMap;

import java.util.random.RandomGenerator;

public class FourPerBaseMexPlacer extends MexPlacer {

    public FourPerBaseMexPlacer(SCMap map, RandomGenerator.SplittableGenerator random) {
        super(map, random);
        minMexesPerPlayer = 4;
        maxMexesPerPlayer = 4;
    }

}
