package com.faforever.neroxis.map.generator.terrain;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.generator.ParameterConstraints;

public strictfp class LittleMountainTerrainGenerator extends PathedPlateauTerrainGenerator {

    public LittleMountainTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.5f, 1f)
                .mountainDensity(.25f, 1)
                .plateauDensity(0, .5f)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        mountainBrushSize = 24;
        mountainBrushDensity = .35f;
        mountainBrushIntensity = 8;
    }

    @Override
    protected void mountainSetup() {
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float normalizedMountainDensity = parameterConstraints.getMountainDensityRange().normalize(mapParameters.getMountainDensity());
        mountains.setSize(mapSize / 4);

        mountains.randomWalk((int) (normalizedMountainDensity * 250 / symmetrySettings.getTerrainSymmetry().getNumSymPoints() + 100), mapSize / 128);

        mountains.setSize(mapSize + 1);
    }

}
