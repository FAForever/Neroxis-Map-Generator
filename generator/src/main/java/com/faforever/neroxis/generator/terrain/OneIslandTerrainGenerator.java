package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;
import java.util.random.RandomGenerator;

public class OneIslandTerrainGenerator extends PathedTerrainGenerator {

    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, random, generatorParameters, symmetrySettings);
        mountainBrushSize = 32;
        mountainBrushDensity = .1f;
        mountainBrushIntensity = 10;
    }

    @Override
    protected void teamConnectionsSetup() {
        float maxStepSize = map.getSize() / 128f;
        int minMiddlePoints = 0;
        int maxMiddlePoints = 2;
        int numTeamConnections = (int) ((rampDensity + plateauDensity + (1 - mountainDensity)) / 3 * 2 + 2);
        int numTeammateConnections = 1;
        connections.setSize(map.getSize() + 1);

        List<Vector2> team0Spawns = map.getSpawns()
                                       .stream()
                                       .filter(spawn -> spawn.getTeamID() == 0)
                                       .map(Spawn::getPosition)
                                       .map(Vector2::new)
                                       .toList();

        MapMaskMethods.connectLocationsThroughMiddle(team0Spawns, random.split(), connections, minMiddlePoints,
                                                     maxMiddlePoints,
                                                     numTeamConnections, maxStepSize);
        MapMaskMethods.connectLocations(team0Spawns, random.split(), connections, maxMiddlePoints,
                                        numTeammateConnections,
                                        maxStepSize);
    }

    @Override
    protected void landSetup() {
        int mapSize = map.getSize();
        int minMiddlePoints = 2;
        int maxMiddlePoints = 4;
        int numTeamConnections = (int) (4 * landDensity + 4) / symmetrySettings.spawnSymmetry()
                                                                               .getNumSymPoints();
        int numTeammateConnections = (int) (2 * landDensity + 2) / symmetrySettings.spawnSymmetry()
                                                                                   .getNumSymPoints();
        int numWalkers = (int) (8 * landDensity + 8) / symmetrySettings.spawnSymmetry().getNumSymPoints();
        int bound = (int) (mapSize / 64f * (16 * (random.nextFloat() * .25f + (1 - landDensity) * .75f)))
                    + mapSize / 8;
        float maxStepSize = mapSize / 128f;
        land.setSize(mapSize + 1);

        MapMaskMethods.pathInCenterBounds(random.split(), land, maxStepSize, numWalkers, maxMiddlePoints, bound,
                                          (float) (StrictMath.PI / 2));
        land.add(connections.copy()
                            .fillEdge((int) (mapSize / 8f * (1 - landDensity) + mapSize / 8f), false)
                            .inflate(mapSize / 64)
                            .blur(12, .125f));

        List<Vector2> team0Spawns = map.getSpawns()
                                       .stream()
                                       .filter(spawn -> spawn.getTeamID() == 0)
                                       .map(Spawn::getPosition)
                                       .map(Vector2::new)
                                       .toList();

        MapMaskMethods.connectLocationsAroundCenter(team0Spawns, random.split(), land, minMiddlePoints,
                                                    maxMiddlePoints,
                                                    numTeamConnections, maxStepSize, 32);
        MapMaskMethods.connectLocations(team0Spawns, random.split(), land, maxMiddlePoints, numTeammateConnections,
                                        maxStepSize);
        land.inflate(mapSize / 128).setSize(mapSize / 8);
        land.dilute(.5f, 8).erode(.5f, 6);
        if (mapSize > 512) {
            land.erode(.5f, 4);
        }
        land.setSize(mapSize + 1);
        land.blur(mapSize / 64, .75f);
    }
}


