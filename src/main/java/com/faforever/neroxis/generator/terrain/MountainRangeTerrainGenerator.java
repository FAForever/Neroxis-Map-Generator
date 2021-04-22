package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;

public strictfp class MountainRangeTerrainGenerator extends PathedPlateauTerrainGenerator {

    public MountainRangeTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.75f, 1f)
                .mountainDensity(.5f, 1)
                .mexDensity(.375f, 1)
                .mapSizes(256, 512)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        mountainBrushSize = map.getSize() / 16;
        mountainBrushDensity = 1.25f;
        mountainBrushIntensity = 2f;
    }

    @Override
    protected void mountainSetup() {
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float normalizedMountainDensity = parameterConstraints.getMountainDensityRange().normalize(mapParameters.getMountainDensity());
        mountains.setSize(mapSize / 2);

        mountains.progressiveWalk((int) (normalizedMountainDensity * 4 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()) + 8, mapSize / 4);
        mountains.inflate(2);

        mountains.setSize(mapSize + 1);
    }

}
