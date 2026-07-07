package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.util.serial.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.util.random.RandomGenerator;

public class RiversAndOceansTerrainGenerator extends RiversTerrainGenerator {

    private FloatMask rivers;

    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, random, generatorParameters, symmetrySettings);
        rivers = new FloatMask(map.getSize(), random.split(), land.getSymmetrySettings(), "rivers");
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

        land.setSize(mapSize + 1);
    }
}
