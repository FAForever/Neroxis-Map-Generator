package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.ParameterConstraints;

public class MountainRangeTerrainGenerator extends PathedPlateauTerrainGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mapSizes(256, 768)
                                   .build();
    }


    @Override
    protected void afterInitialize() {
        super.afterInitialize();
        mountainBrushSize = map.getSize() / 16;
        mountainBrushDensity = 1.25f;
        mountainBrushIntensity = 3f;
    }

    @Override
    protected void mountainSetup() {
        int mapSize = map.getSize();
        mountains.setSize(mapSize / 2);

        mountains.progressiveWalk(
                (int) (mountainDensity * 16 / symmetrySettings.terrainSymmetry().getNumSymPoints()) + 8,
                mapSize / 4);
        mountains.inflate(2);

        mountains.setSize(mapSize + 1);
    }
}
