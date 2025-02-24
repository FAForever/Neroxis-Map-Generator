package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.vector.Vector3;

public class RiversAndOceansTerrainGenerator extends RiversTerrainGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        plateauHeight = 8f;
        plateauBrushSize = 96;
        plateauBrushIntensity = 8f;
        plateauBrushDensity = 0.3f;
        plateauDensity = 0.8f;
        rampDensity = random.nextFloat() * 0.2f + 0.8f;

        watermap = true;
    }

    @Override
    protected void spawnMaskSetup() {
        map.getSpawns().forEach(spawn -> {
            Vector3 location = spawn.getPosition();
            spawnLandMask.fillCircle(location, 10, true);
        });
    }

}
