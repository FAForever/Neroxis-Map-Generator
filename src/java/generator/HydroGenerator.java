package generator;

import map.BinaryMask;
import map.SCMap;
import util.Vector2f;
import util.Vector3f;

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

        for (int i = 0; i < map.getMexes().length; i++) {
            if (map.getMexes()[i] != null) {
                spawnable.fillCircle(map.getMexes()[i].x, map.getMexes()[i].z, 10, false);
            }
        }

        boolean spawnHydro = random.nextBoolean();
        for (int i = 0; i < map.getSpawns().length; i += 2) {
            if (spawnHydro) {
                BinaryMask baseHydro = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetryHierarchy());
                baseHydro.fillCircle(map.getSpawns()[i + 1], spawnSize * 1.5f, true).fillCircle(map.getSpawns()[i + 1], 10, false).intersect(spawnable);
                for (int j = 0; j < map.getSpawns().length; j += 2) {
                    baseHydro.fillCircle(map.getSpawns()[j], 16, false);
                }
                for (int j = 0; j < iHydro; j += 2) {
                    baseHydro.fillCircle(map.getHydros()[j], 16, false);
                }
                Vector2f location = baseHydro.getRandomPosition();
                if (location == null) {
                    break;
                }
                map.getHydros()[iHydro] = new Vector3f(location);
                Vector2f symLocation = spawnable.getSymmetryPoint(map.getHydros()[iHydro]);
                map.getHydros()[iHydro + 1] = new Vector3f(symLocation);
                spawnable.fillCircle(map.getSpawns()[i + 1], 30, false);
                spawnable.fillCircle(location, hydroSpacing, false);
                iHydro += 2;
            }
        }

        for (int i = 0; i < map.getSpawns().length; i += 2) {
            spawnable.fillCircle(map.getSpawns()[i + 1], spawnSize, false);
        }

        int baseHydroCount = iHydro;

        for (int i = baseHydroCount; i < map.getHydros().length; i += 2) {
            Vector2f hydroLocation = spawnable.getRandomPosition();

            if (hydroLocation == null) {
                break;
            }

            Vector2f hydroSymLocation = spawnable.getSymmetryPoint(hydroLocation);

            map.getHydros()[i] = new Vector3f(hydroLocation);
            map.getHydros()[i + 1] = new Vector3f(hydroSymLocation);

            spawnable.fillCircle(hydroLocation, hydroSpacing, false);
            spawnable.fillCircle(hydroSymLocation, hydroSpacing, false);
        }
    }

    public void setMarkerHeights() {
        for (int i = 0; i < map.getHydros().length; i++) {
            if (map.getHydros()[i] != null) {
                map.getHydros()[i] = placeOnHeightmap(map, map.getHydros()[i]);
            }
        }
    }
}
