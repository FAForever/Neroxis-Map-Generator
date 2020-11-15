package generator;

import map.BinaryMask;
import map.Hydro;
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
        map.getHydros().clear();
        int hydroSpacing = 64;

        spawnable.fillHalf(false);
        spawnable.fillCenter(64, false);

        for (int i = 0; i < map.getMexCount(); i++) {
            spawnable.fillCircle(map.getMex(i).getPosition(), 10, false);
        }

        boolean spawnHydro = random.nextBoolean();
        if (spawnHydro) {
            for (int i = 0; i < map.getSpawnCount(); i += 2) {
                BinaryMask baseHydro = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
                baseHydro.fillCircle(map.getSpawn(i + 1).getPosition(), spawnSize * 2f, true).fillCircle(map.getSpawn(i + 1).getPosition(), 10, false).intersect(spawnable);
                for (int j = 0; j < map.getSpawnCount(); j += 2) {
                    baseHydro.fillCircle(map.getSpawn(j).getPosition(), 16, false);
                }
                for (Hydro hydro : map.getHydros()) {
                    baseHydro.fillCircle(hydro.getPosition(), 16, false);
                }
                generateIndividualHydros(baseHydro, 2, hydroSpacing);
            }
        }

        for (int i = 0; i < map.getSpawnCount(); i += 2) {
            spawnable.fillCircle(map.getSpawn(i + 1).getPosition(), spawnSize, false);
        }

        int numHydrosLeft = map.getHydroCountInit() - map.getHydroCount();

        generateIndividualHydros(spawnable, numHydrosLeft, hydroSpacing);
    }

    public void generateIndividualHydros(BinaryMask spawnable, int numHydros, int hydroSpacing) {
        List<Vector2f> mexLocations = new ArrayList<>(spawnable.getRandomCoordinates(hydroSpacing));
        for (int i = 0; i < numHydros; i += 2) {
            if (mexLocations.size() == 0) {
                break;
            }

            Vector2f location = mexLocations.remove(random.nextInt(mexLocations.size())).add(.5f, .5f);
            Vector2f symLocation = spawnable.getSymmetryPoint(location);

            int hydroId = map.getHydroCount() + 1;
            map.addHydro(new Hydro(String.format("Hydro %d", hydroId), new Vector3f(location)));
            map.addHydro(new Hydro(String.format("sym Hydro %d", hydroId), new Vector3f(symLocation)));
        }
    }

    public void setMarkerHeights() {
        for (Hydro hydro : map.getHydros()) {
            hydro.setPosition(placeOnHeightmap(map, hydro.getPosition()));
        }
    }
}
