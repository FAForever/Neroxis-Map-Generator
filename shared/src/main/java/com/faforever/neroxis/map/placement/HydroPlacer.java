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

public class HydroPlacer {
    protected final SCMap map;
    protected final Random random;
    protected final int hydroSpacing;
    protected BooleanMask allowedHydroMask;

    public HydroPlacer(SCMap map, long seed) {
        this.map = map;
        this.hydroSpacing = 64;
        random = new Random(seed);
    }

    public void placeHydros(int hydroCount, BooleanMask allowedHydroMask) {
        this.allowedHydroMask = allowedHydroMask;
        map.getHydros().clear();
        int numSymPoints = allowedHydroMask.getSymmetrySettings().spawnSymmetry().getNumSymPoints();

        if (!allowedHydroMask.getSymmetrySettings().spawnSymmetry().isPerfectSymmetry()) {
            allowedHydroMask.limitToCenteredCircle(allowedHydroMask.getSize() / 2f);
        }
        allowedHydroMask.fillCenter(64, false).limitToSymmetryRegion();

        map.getMexes()
           .stream()
           .filter(mex -> allowedHydroMask.inTeam(mex.getPosition(), false))
           .forEach(mex -> allowedHydroMask.fillCircle(mex.getPosition(), 10, false));

        placeBaseHydros(allowedHydroMask);

        map.getSpawns()
           .stream()
           .filter(spawn -> allowedHydroMask.inTeam(spawn.getPosition(), false))
           .forEach(spawn -> allowedHydroMask.fillCircle(spawn.getPosition(), 30f, false));

        int numHydrosLeft = (hydroCount - map.getHydroCount()) / numSymPoints;

        placeIndividualHydros(allowedHydroMask, numHydrosLeft, hydroSpacing);
    }

    private void placeBaseHydros(BooleanMask allowedHydroMask) {
        boolean spawnHydro = random.nextBoolean();
        if (spawnHydro) {
            for (int i = 0; i < map.getSpawnCount(); i += allowedHydroMask.getSymmetrySettings()
                                                                   .spawnSymmetry()
                                                                   .getNumSymPoints()) {
                Spawn spawn = map.getSpawn(i);
                BooleanMask baseHydro = new BooleanMask(allowedHydroMask.getSize(), random.nextLong(),
                                                        allowedHydroMask.getSymmetrySettings());
                baseHydro.fillCircle(spawn.getPosition(), 30f, true)
                         .fillCircle(spawn.getPosition(), 10f, false)
                         .multiply(allowedHydroMask);
                map.getSpawns()
                   .stream()
                   .filter(otherSpawn -> allowedHydroMask.inTeam(otherSpawn.getPosition(), false))
                   .forEach(otherSpawn -> baseHydro.fillCircle(otherSpawn.getPosition(), 16, false));
                map.getHydros()
                   .stream()
                   .filter(hydro -> allowedHydroMask.inTeam(hydro.getPosition(), false))
                   .forEach(hydro -> baseHydro.fillCircle(hydro.getPosition(), 16, false));
                placeIndividualHydros(baseHydro, 1, hydroSpacing);
            }
        }
    }

    protected void placeIndividualHydros(BooleanMask spawnHydroMask, int numHydros, int hydroSpacing) {
        if (numHydros > 0) {
            List<Vector2> hydroLocations = spawnHydroMask.getRandomCoordinates(hydroSpacing);
            hydroLocations.stream().limit(numHydros).map(Vector2::roundToNearestHalfPoint).forEach(location -> {
                int hydroId = map.getHydroCount() / spawnHydroMask.getSymmetrySettings().spawnSymmetry().getNumSymPoints();
                Marker hydro = new Marker(String.format("Hydro %d", hydroId),
                                          new Vector3(location));
                map.addHydro(hydro);
                allowedHydroMask.fillCircle(hydro.getPosition(), 5, false);
                List<Vector2> symmetryPoints = spawnHydroMask.getSymmetryPoints(hydro.getPosition(), SymmetryType.SPAWN)
                                                        .stream()
                                                        .map(Vector2::roundToNearestHalfPoint)
                                                        .toList();
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Vector2 symmetryPoint = symmetryPoints.get(i);
                    map.addHydro(new Marker(String.format("Hydro %d sym %d", hydroId, i),
                                            new Vector3(symmetryPoint)));
                }
            });
        }
    }
}
