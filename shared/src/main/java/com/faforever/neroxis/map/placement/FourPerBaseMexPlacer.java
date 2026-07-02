package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.SCMap;

public class FourPerBaseMexPlacer extends MexPlacer {

    public FourPerBaseMexPlacer(SCMap map, long seed) {
        super(map, seed);
        minMexesPerPlayer = 4;
        maxMexesPerPlayer = 4;
    }

}
