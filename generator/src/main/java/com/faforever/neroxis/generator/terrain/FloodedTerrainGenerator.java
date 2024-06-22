package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.ParameterConstraints;

public class FloodedTerrainGenerator extends BasicTerrainGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mapSizes(384, 1024)
                                   .build();
    }

    @Override
    protected void afterInitialize() {
        super.afterInitialize();
        waterHeight -= plateauHeight - 1f;
    }

    @Override
    protected void plateausSetup() {
        float plateauDensityMax = .7f;
        float plateauDensityMin = .65f;
        float plateauDensityRange = plateauDensityMax - plateauDensityMin;
        float scaledPlateauDensity = plateauDensity * plateauDensityRange + plateauDensityMin;
        plateaus.setSize(map.getSize() / 16);

        plateaus.randomize(scaledPlateauDensity).blur(2, .75f).setSize(map.getSize() / 4);
        plateaus.dilute(.5f, map.getSize() / 256);
        plateaus.setSize(map.getSize() + 1);
        plateaus.blur(16, .25f);
        plateaus.deflate(plateauBrushSize / 4f);
    }

    @Override
    protected void spawnTerrainSetup() {
        spawnPlateauMask.add(spawnLandMask);
        super.spawnTerrainSetup();
    }
}

