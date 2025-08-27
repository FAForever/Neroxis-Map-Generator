package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

public class FloodedMultiLevelTerrainGenerator extends MultiLevelTerrainGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);

        oceanFloor = -26f;

        spawnHeight = landHeight+plateauHeight;

        waterHeight -= landNoiseMapFirstLevel - 2f;

        noiseScaleMaxToValue = 40;
        landNoiseMapFirstLevel = 10;
        landNoiseMapSecondLevel = 28;
        landNoiseMapThirdLevel = 38;
    }

    @Override
    protected void mountainSetup() {
        mountains.setSize(map.getSize() + 1);
    }

    @Override
    protected void landSetup() {
        super.landSetup();

        int mapSize = map.getSize();
        if (mapSize < 512) {
            BooleanMask connectionsMask = connections.copy().dilute(1, 18).setSize(land.getSize());
            land.add(connectionsMask);
            secondLevelLand.add(connectionsMask);
        }

        map.getSpawns().forEach(spawn -> {
            if (spawn.getTeamID() == 0) {
                Vector3 location = spawn.getPosition();
                String brush = SPAWN_MASK_BRUSHES[StrictMath.abs(random.nextInt()) % SPAWN_MASK_BRUSHES.length];
                land.addBrush(new Vector2(location.x(), location.z()), brush, 15f, 256f, 200);
                secondLevelLand.addBrush(new Vector2(location.x(), location.z()), brush, 15f, 256f, 200);
            }
        });
    }
}
