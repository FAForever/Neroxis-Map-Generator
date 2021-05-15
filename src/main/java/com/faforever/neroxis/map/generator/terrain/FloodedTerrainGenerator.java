package com.faforever.neroxis.map.generator.terrain;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.generator.ParameterConstraints;

public strictfp class FloodedTerrainGenerator extends BasicTerrainGenerator {

    public FloodedTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .plateauDensity(0, .1f)
                .landDensity(0, .5f)
                .mapSizes(512, 1024)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        this.mapParameters.getBiome().getWaterSettings().setElevation(waterHeight + plateauHeight - 1f);
    }

    @Override
    protected void spawnTerrainSetup() {
        spawnPlateauMask.add(spawnLandMask);
        super.spawnTerrainSetup();
    }
}

