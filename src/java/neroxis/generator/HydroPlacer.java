package neroxis.generator;

import neroxis.map.*;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

import java.util.LinkedList;
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

    public void placeHydros(BinaryMask spawnMask) {
        map.getHydros().clear();
        int numSymPoints = spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();

        if (!spawnMask.getSymmetrySettings().getSpawnSymmetry().isPerfectSymmetry()) {
            spawnMask.limitToCenteredCircle(spawnMask.getSize() / 2f);
        }
        spawnMask.fillCenter(64, false).limitToSymmetryRegion();

        map.getMexes().forEach(mex -> spawnMask.fillCircle(mex.getPosition(), 10, false));

        placeBaseHydros(spawnMask);

        map.getSpawns().forEach(spawn -> spawnMask.fillCircle(spawn.getPosition(), 30f, false));

        int numHydrosLeft = (map.getHydroCountInit() - map.getHydroCount()) / numSymPoints;

        placeIndividualHydros(spawnMask, numHydrosLeft, hydroSpacing);
    }

    public void placeBaseHydros(BinaryMask spawnMask) {
        boolean spawnHydro = random.nextBoolean();
        if (spawnHydro) {
            for (int i = 0; i < map.getSpawnCount(); i += spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints()) {
                Spawn spawn = map.getSpawn(i);
                BinaryMask baseHydro = new BinaryMask(spawnMask.getSize(), random.nextLong(), spawnMask.getSymmetrySettings());
                baseHydro.fillCircle(spawn.getPosition(), 30f, true).fillCircle(spawn.getPosition(), 10f, false).intersect(spawnMask);
                map.getSpawns().forEach(otherSpawn -> baseHydro.fillCircle(otherSpawn.getPosition(), 16, false));
                map.getHydros().forEach(hydro -> baseHydro.fillCircle(hydro.getPosition(), 16, false));
                placeIndividualHydros(baseHydro, 1, hydroSpacing);
            }
        }
    }

    public void placeIndividualHydros(BinaryMask spawnMask, int numHydros, int hydroSpacing) {
        if (numHydros > 0) {
            LinkedList<Vector2f> hydroLocations = spawnMask.getRandomCoordinates(hydroSpacing);
            hydroLocations.stream().limit(numHydros).forEachOrdered(location -> {
                int hydroId = map.getHydroCount() / spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();
                Marker hydro = new Marker(String.format("Hydro %d", hydroId), new Vector3f(location.add(.5f, .5f)));
                map.addHydro(hydro);
                List<Vector2f> symmetryPoints = spawnMask.getSymmetryPoints(hydro.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(Vector2f::roundToNearestHalfPoint);
                symmetryPoints.forEach(symmetryPoint -> map.addHydro(new Marker(String.format("Hydro %d sym %d", hydroId, symmetryPoints.indexOf(symmetryPoint)), new Vector3f(symmetryPoint))));
            });
        }
    }

}
