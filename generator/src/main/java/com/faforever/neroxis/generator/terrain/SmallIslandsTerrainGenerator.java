package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;

public class SmallIslandsTerrainGenerator extends PathedTerrainGenerator {
    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mapSizes(768, 1024)
                                   .build();
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        spawnSize = 64;
    }

    @Override
    protected void landSetup() {
        int mapSize = map.getSize();

        int maxMiddlePoints = 4;
        int numPaths = (int) (4 * landDensity + 4) / symmetrySettings.spawnSymmetry().getNumSymPoints();
        int bound = ((int) (mapSize / 16f * (random.nextFloat() * .25f + landDensity * .75f)) + mapSize / 16);
        float maxStepSize = mapSize / 128f;

        BooleanMask islands = new BooleanMask(mapSize / 4, random.nextLong(), symmetrySettings, "islands", true);

        land.setSize(mapSize + 1);

        List<Vector2> team0SpawnLocations = map.getSpawns()
                                               .stream()
                                               .filter(spawn -> spawn.getTeamID() == 0)
                                               .map(Spawn::getPosition)
                                               .map(Vector2::new)
                                               .toList();

        MapMaskMethods.pathAroundSpawns(team0SpawnLocations, random.nextLong(), land, maxStepSize, numPaths,
                                        maxMiddlePoints, bound,
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


