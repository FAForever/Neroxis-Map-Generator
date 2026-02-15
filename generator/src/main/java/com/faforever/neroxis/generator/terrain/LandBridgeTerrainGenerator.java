package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;

public class LandBridgeTerrainGenerator extends PathedTerrainGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mapSizes(768, 1024)
                                   .numTeams(2, 4)
                                   .build();
    }

    @Override
    protected void landSetup() {
        int mapSize = map.getSize();
        int maxStepSize = mapSize / 128;
        int numPaths = 32 / generatorParameters.spawnCount();

        List<Vector2> team0Spawns = map.getSpawns()
                                       .stream()
                                       .filter(spawn -> spawn.getTeamID() == 0)
                                       .map(Spawn::getPosition)
                                       .map(Vector2::new)
                                       .toList();

        land.setSize(mapSize + 1);
        MapMaskMethods.connectLocations(team0Spawns, random.nextLong(), land, 8, 2, maxStepSize);
        MapMaskMethods.connectLocationsThroughMiddle(team0Spawns, random.nextLong(), land, 0, 2, 1, maxStepSize);
        MapMaskMethods.pathAroundLocations(team0Spawns, random.nextLong(), land, maxStepSize, numPaths, 4, mapSize / 6,
                                           (float) (StrictMath.PI / 2f));
        land.inflate(maxStepSize);
        land.setSize(mapSize / 8);
        land.dilute(.5f, 8);
        land.setSize(mapSize + 1);
        land.blur(8);
    }

    @Override
    protected void plateausSetup() {
        int mapSize = map.getSize();
        int maxStepSize = mapSize / 128;
        int maxMiddlePoints = 2;
        int numPaths = (int) (16 * plateauDensity) / symmetrySettings.spawnSymmetry().getNumSymPoints();
        int bound = mapSize / 4;
        plateaus.setSize(mapSize + 1);

        MapMaskMethods.pathInEdgeBounds(random.nextLong(), plateaus, maxStepSize, numPaths, maxMiddlePoints, bound,
                                        (float) (StrictMath.PI / 2));
        plateaus.inflate(mapSize / 256).setSize(mapSize / 4);
        plateaus.dilute(.5f, 4).setSize(mapSize + 1);
        plateaus.blur(12);
    }

    @Override
    public float getSpawnSeparation() {
        return getGeneratorParameters().mapSize() / 8f;
    }
}

