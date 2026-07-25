package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;
import java.util.random.RandomGenerator;

public class SmallIslandsTerrainGenerator extends PathedTerrainGenerator {

    private BooleanMask islands;

    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, random, generatorParameters, symmetrySettings);
        islands = new BooleanMask(map.getSize() / 4, random.split(), symmetrySettings, "islands");
        spawnSize = 64;
    }

    @Override
    protected void landSetup() {
        int mapSize = map.getSize();

        int maxMiddlePoints = 4;
        int numPaths = (int) (4 * landDensity + 4) / symmetrySettings.spawnSymmetry().getNumSymPoints();
        int bound = ((int) (mapSize / 16f * (random.nextFloat() * .25f + landDensity * .75f)) + mapSize / 16);
        int maxStepSize = mapSize / 128;


        land.setSize(mapSize + 1);
        List<Vector2> team0Spawns = map.getSpawns()
                                       .stream()
                                       .filter(spawn -> spawn.getTeamID() == 0)
                                       .map(Spawn::getPosition)
                                       .map(Vector2::new)
                                       .toList();
        MapMaskMethods.pathAroundLocations(team0Spawns, random.split(), land, maxStepSize, numPaths, maxMiddlePoints,
                                           bound,
                                           (float) StrictMath.PI / 2);
        land.inflate(maxStepSize).setSize(mapSize / 4);

        islands.randomWalk(
                (int) (landDensity * 6 / symmetrySettings.terrainSymmetry().getNumSymPoints()) + 8,
                mapSize / 8);

        land.add(islands);
        land.dilute(.5f, 8);

        land.setSize(mapSize + 1);
        land.blur(16);
    }
}


