package generator;

import map.BinaryMask;
import map.SCMap;
import util.Vector2f;
import util.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static util.Placement.placeOnHeightmap;

public strictfp class HydroGenerator {
    private final SCMap map;
    private final Random random;
    private final int spawnSize;

    public HydroGenerator(SCMap map, long seed, int spawnSize) {
        this.map = map;
        this.spawnSize = spawnSize;
        random = new Random(seed);
    }

    public void generateHydros(BinaryMask spawnable) {
        int hydroSpacing = 64;
        int iHydro = 0;

        spawnable.fillHalf(false);
        spawnable.fillCenter(64, false);

        for (int i = 0; i < map.getMexCount(); i++) {
            spawnable.fillCircle(map.getMex(i), 10, false);
        }

        boolean spawnHydro = random.nextBoolean();
        if (spawnHydro) {
            for (int i = 0; i < map.getSpawnCount(); i += 2) {
                BinaryMask baseHydro = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetryHierarchy());
                baseHydro.fillCircle(map.getSpawn(i + 1), spawnSize * 1.5f, true).fillCircle(map.getSpawn(i + 1), 10, false).intersect(spawnable);
                for (int j = 0; j < map.getSpawnCount(); j += 2) {
                    baseHydro.fillCircle(map.getSpawn(j), 16, false);
                }
                for (int j = 0; j < iHydro; j += 2) {
                    baseHydro.fillCircle(map.getHydro(j), 16, false);
                }
                Vector2f location = baseHydro.getRandomPosition();
                if (location == null) {
                    continue;
                }
                Vector2f symLocation = spawnable.getSymmetryPoint(location);
                map.addHydro(new Vector3f(location));
                map.addHydro(new Vector3f(symLocation));
                spawnable.fillCircle(map.getSpawn(i + 1), 30, false);
                spawnable.fillCircle(location, hydroSpacing, false);
                iHydro += 2;
            }
        }

        for (int i = 0; i < map.getSpawnCount(); i += 2) {
            spawnable.fillCircle(map.getSpawn(i + 1), spawnSize, false);
        }

        int baseHydroCount = iHydro;

        List<Vector2f> hydroLocations = new ArrayList<>(spawnable.getRandomCoordinates(hydroSpacing));
        for (int i = baseHydroCount; i < map.getHydroCountInit(); i += 2) {
            if (hydroLocations.size() == 0) {
                break;
            }

            Vector2f location = hydroLocations.remove(random.nextInt(hydroLocations.size()));
            Vector2f symLocation = spawnable.getSymmetryPoint(location);

            map.addHydro(new Vector3f(location));
            map.addHydro(new Vector3f(symLocation));
        }
    }

    public void setMarkerHeights() {
        for (int i = 0; i < map.getHydroCount(); i++) {
            map.getHydros().set(i, placeOnHeightmap(map, map.getHydro(i)));
        }
    }
}
