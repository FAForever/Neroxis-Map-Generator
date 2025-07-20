package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.Pipeline;

public class MultiLevelOpenTerrainGenerator extends MultiLevelTerrainGenerator {

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);

        noiseSmallestDetail = 2;
        noiseOctaveMultiplier = 1.5f;
        noiseMapBlurAmount = 1;

        noiseScaleMaxToValue = 40;
        landNoiseMapFirstLevel = 0;
        landNoiseMapSecondLevel = 29;
        landNoiseMapThirdLevel = 35;

        spawnSize = 64;

        plateauHeight = 6f;
        plateauBrushIntensity = 16f;

        mountainDensity = random.nextFloat(0.2f);
    }

    @Override
    protected void mountainSetup() {
        super.mountainSetup();
        mountains.add(thirdLevelLand);
    }

    @Override
    protected void initRamps() {
    }

    @Override
    protected void blurRamps() {
    }

}
