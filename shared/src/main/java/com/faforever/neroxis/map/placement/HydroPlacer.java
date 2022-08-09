package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.util.List;
import java.util.Random;

public strictfp class HydroPlacer {
    private final SCMap map;
    private final Random random;
    private final int hydroSpacing;

    public HydroPlacer(SCMap map, long seed) {
        this.map = map;
        this.hydroSpacing = 64;
        random = new Random(seed);
    }

    public void placeHydros(int hydroCount, BooleanMask spawnMask) {
        map.getHydros().clear();
        int numSymPoints = spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();

        if (!spawnMask.getSymmetrySettings().getSpawnSymmetry().isPerfectSymmetry()) {
            spawnMask.limitToCenteredCircle(spawnMask.getSize() / 2f);
        }
        spawnMask.fillCenter(64, false).limitToSymmetryRegion();

        map.getMexes().stream().filter(mex -> spawnMask.inTeam(mex.getPosition(), false))
                .forEach(mex -> spawnMask.fillCircle(mex.getPosition(), 10, false));

        placeBaseHydros(spawnMask);

        map.getSpawns().stream().filter(spawn -> spawnMask.inTeam(spawn.getPosition(), false))
                .forEach(spawn -> spawnMask.fillCircle(spawn.getPosition(), 30f, false));

        int numHydrosLeft = (hydroCount - map.getHydroCount()) / numSymPoints;

        placeIndividualHydros(spawnMask, numHydrosLeft, hydroSpacing);
    }

    private void placeBaseHydros(BooleanMask spawnMask) {
        boolean spawnHydro = random.nextBoolean();
        if (spawnHydro) {
            for (int i = 0; i < map.getSpawnCount(); i += spawnMask.getSymmetrySettings()
                                                                   .getSpawnSymmetry()
                                                                   .getNumSymPoints()) {
                Spawn spawn = map.getSpawn(i);
                BooleanMask baseHydro = new BooleanMask(spawnMask.getSize(), random.nextLong(), spawnMask.getSymmetrySettings());
                baseHydro.fillCircle(spawn.getPosition(), 30f, true)
                         .fillCircle(spawn.getPosition(), 10f, false)
                         .multiply(spawnMask);
                map.getSpawns().stream().filter(otherSpawn -> spawnMask.inTeam(otherSpawn.getPosition(), false))
                   .forEach(otherSpawn -> baseHydro.fillCircle(otherSpawn.getPosition(), 16, false));
                map.getHydros().stream().filter(hydro -> spawnMask.inTeam(hydro.getPosition(), false))
                   .forEach(hydro -> baseHydro.fillCircle(hydro.getPosition(), 16, false));
                placeIndividualHydros(baseHydro, 1, hydroSpacing);
            }
        }
    }

    private void placeIndividualHydros(BooleanMask spawnMask, int numHydros, int hydroSpacing) {
        if (numHydros > 0) {
            List<Vector2> hydroLocations = spawnMask.getRandomCoordinates(hydroSpacing);
            hydroLocations.stream().limit(numHydros).forEachOrdered(location -> {
                int hydroId = map.getHydroCount() / spawnMask.getSymmetrySettings()
                                                             .getSpawnSymmetry()
                                                             .getNumSymPoints();
                Marker hydro = new Marker(String.format("Hydro %d", hydroId), new Vector3(location.roundToNearestHalfPoint()));
                map.addHydro(hydro);
                List<Vector2> symmetryPoints = spawnMask.getSymmetryPoints(hydro.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(Vector2::roundToNearestHalfPoint);
                symmetryPoints.forEach(symmetryPoint -> map.addHydro(new Marker(String.format("Hydro %d sym %d", hydroId, symmetryPoints.indexOf(symmetryPoint)), new Vector3(symmetryPoint))));
            });
        }
    }

}
