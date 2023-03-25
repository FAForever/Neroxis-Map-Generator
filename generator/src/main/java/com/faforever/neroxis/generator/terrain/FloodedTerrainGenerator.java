package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;

public class FloodedTerrainGenerator extends BasicTerrainGenerator {
    public FloodedTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                                                   .plateauDensity(0, .25f)
                                                   .landDensity(0, .5f)
                                                   .mapSizes(384, 1024)
                                                   .build();
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        this.generatorParameters.biome().waterSettings().setElevation(waterHeight + plateauHeight - 1f);
    }

    @Override
    protected void plateausSetup() {
        float plateauDensityMax = .7f;
        float plateauDensityMin = .65f;
        float plateauDensityRange = plateauDensityMax - plateauDensityMin;
        float normalizedPlateauDensity = parameterConstraints.getPlateauDensityRange()
                                                             .normalize(generatorParameters.plateauDensity());
        float scaledPlateauDensity = normalizedPlateauDensity * plateauDensityRange + plateauDensityMin;
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

