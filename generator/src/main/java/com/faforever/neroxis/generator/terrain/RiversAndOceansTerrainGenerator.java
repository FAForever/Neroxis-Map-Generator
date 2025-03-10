package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.vector.Vector2;
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
        plateauDensity = random.nextFloat() * 0.03f + 0.8f;
    }

    @Override
    protected void landSetup() {
        int mapSize = map.getSize();

        land.setSize(mapSize);

        int riversScale = mapSize / 64;
        FloatMask rivers = new FloatMask(mapSize, getRandom().nextLong(), land.getSymmetrySettings(), "rivers", true);
        rivers.addPerlinNoise(StrictMath.min(96 + riversScale, mapSize), 1);
        riverMask = rivers.copyAsBooleanMask(0.2f, 0.8f);

        riverMask.invert();
        riverMask.blur(10);

        if (mapSize < 512) {
            riverMask.add(connections.copy().dilute(1, 10).setSize(riverMask.getSize()));
        }

        riverMask.erode(0.3f, 10);

        String[] SPAWN_MASK_BRUSHES = {
                "mountain4.png",
                "mountain7.png",
                "mountain8.png",
                "mountain9.png"
        };
        map.getSpawns().forEach(spawn -> {
            if (spawn.getTeamID() == 0) {
                Vector3 location = spawn.getPosition();
                String brush = SPAWN_MASK_BRUSHES[StrictMath.abs(random.nextInt()) % SPAWN_MASK_BRUSHES.length];
                riverMask.addBrush(new Vector2(location.x(), location.z()), brush, 1f, 256f, 150);
            }
        });

        land.add(riverMask);

        land.setSize(mapSize+1);
    }
}
