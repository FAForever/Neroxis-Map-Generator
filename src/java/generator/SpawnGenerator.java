package generator;

import map.AIMarker;
import map.BinaryMask;
import map.SCMap;
import map.Symmetry;
import util.Vector2f;
import util.Vector3f;

import java.util.Random;

import static util.Placement.placeOnHeightmap;

public strictfp class SpawnGenerator {
    private final SCMap map;
    private final Random random;
    private final int spawnSize;

    public SpawnGenerator(SCMap map, long seed, int spawnSize) {
        this.map = map;
        this.spawnSize = spawnSize;
        random = new Random(seed);
    }
    
    public BinaryMask[] generateSpawns(float separation, Symmetry symmetry, float plateauDensity) {
        map.getLargeExpansionAIMarkers().clear();
        map.getSpawns().clear();
        BinaryMask spawnable = new BinaryMask(map.getSize() + 1, random.nextLong(), symmetry);
        BinaryMask spawnLandMask = new BinaryMask(map.getSize() + 1, random.nextLong(), spawnable.getSymmetryHierarchy());
        BinaryMask spawnPlateauMask = new BinaryMask(map.getSize() + 1, random.nextLong(), spawnable.getSymmetryHierarchy());
        if (map.getSpawnCountInit() == 2 && (symmetry == Symmetry.POINT || symmetry == Symmetry.DIAG || symmetry == Symmetry.QUAD)) {
            spawnable.getSymmetryHierarchy().setSpawnSymmetry(Symmetry.POINT);
        }
        spawnable.fillHalf(true).fillSides(map.getSize() / map.getSpawnCountInit() * 3 / 2, false).fillCenter(map.getSize() * 4 / 8, false).trimEdge(map.getSize() / 16);
        Vector2f location = spawnable.getRandomPosition();
        Vector2f symLocation;
        for (int i = 0; i < map.getSpawnCountInit(); i += 2) {
            if (location == null) {
                if (separation - 4 >= 10) {
                    return generateSpawns(separation - 8, symmetry, plateauDensity);
                } else {
                    return null;
                }
            }
            symLocation = spawnable.getSymmetryPoint(location);
            spawnable.fillCircle(location, separation, false);
            spawnable.fillCircle(symLocation, separation, false);

            if (spawnable.getSymmetryHierarchy().getSpawnSymmetry() == Symmetry.POINT) {
                spawnable.fillCircle(symLocation, map.getSize() * 4 / 8f, false);
            }

            spawnLandMask.fillCircle(location, spawnSize, true);
            spawnLandMask.fillCircle(symLocation, spawnSize, true);

            if (random.nextFloat() < plateauDensity) {
                boolean valid = true;
                for (int j = 0; j < i; j += 2) {
                    if (!spawnPlateauMask.get(map.getSpawn(j)) && map.getSpawn(j).getXZDistance(location) < spawnSize * 8) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    spawnPlateauMask.fillCircle(location, spawnSize, true);
                    spawnPlateauMask.fillCircle(symLocation, spawnSize, true);
                }
            } else {
                boolean valid = false;
                for (int j = 0; j < i; j += 2) {
                    if (spawnPlateauMask.get(map.getSpawn(j)) && map.getSpawn(j).getXZDistance(location) < spawnSize * 8) {
                        valid = true;
                        break;
                    }
                }
                if (valid) {
                    spawnPlateauMask.fillCircle(location, spawnSize, true);
                    spawnPlateauMask.fillCircle(symLocation, spawnSize, true);
                }
            }
            map.addSpawn(new Vector3f(location));
            map.addSpawn(new Vector3f(symLocation));
            map.addLargeExpansionMarker(new AIMarker(map.getLargeExpansionMarkerCount(), location, null));
            map.addLargeExpansionMarker(new AIMarker(map.getLargeExpansionMarkerCount(), symLocation, null));
            location = spawnable.getRandomPosition();
        }
        return new BinaryMask[]{spawnLandMask, spawnPlateauMask};
    }
    
    public void setMarkerHeights() {
        for (int i = 0; i <   map.getSpawnCount(); i++) {
            map.getSpawns().set(i, placeOnHeightmap(map, map.getSpawn(i)));
        }
    }
}
