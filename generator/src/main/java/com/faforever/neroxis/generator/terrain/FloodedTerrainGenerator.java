package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;

import java.util.random.RandomGenerator;

public class FloodedTerrainGenerator extends BasicTerrainGenerator {

    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, random, generatorParameters, symmetrySettings);
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
        plateaus.deflate(plateauBrushSize / 4);
    }

    @Override
    protected void spawnTerrainSetup() {
        spawnPlateauMask.add(spawnLandMask);
        super.spawnTerrainSetup();
    }
}

