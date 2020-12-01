package generator;

import map.BinaryMask;
import map.Hydro;
import map.SCMap;
import map.SymmetryPoint;
import util.Vector2f;
import util.Vector3f;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import static util.Placement.placeOnHeightmap;

public strictfp class HydroGenerator {
    private final SCMap map;
    private final Random random;
    private final int spawnSize;
    private final int hydroSpacing;

    public HydroGenerator(SCMap map, long seed, int spawnSize) {
        this.map = map;
        this.spawnSize = spawnSize;
        this.hydroSpacing = 64;
        random = new Random(seed);
    }

    public void generateHydros(BinaryMask spawnable) {
        map.getHydros().clear();
        int numSymPoints = spawnable.getSymmetryPoints(0, 0).size() + 1;

        spawnable.limitToSpawnRegion();
        spawnable.fillCenter(64, false);

        for (int i = 0; i < map.getMexCount(); i++) {
            spawnable.fillCircle(map.getMex(i).getPosition(), 10, false);
        }

        generateBaseHydros(spawnable);

        for (int i = 0; i < map.getSpawnCount(); i += numSymPoints) {
            spawnable.fillCircle(map.getSpawn(i).getPosition(), spawnSize, false);
        }

        int numHydrosLeft = (map.getHydroCountInit() - map.getHydroCount()) / numSymPoints;

        generateIndividualHydros(spawnable, numHydrosLeft, hydroSpacing);
    }

    public void generateBaseHydros(BinaryMask spawnable) {
        boolean spawnHydro = random.nextBoolean();
        int numSymPoints = spawnable.getSymmetryPoints(0, 0).size() + 1;
        if (spawnHydro) {
            for (int i = 0; i < map.getSpawnCount(); i += numSymPoints) {
                BinaryMask baseHydro = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
                baseHydro.fillCircle(map.getSpawn(i).getPosition(), spawnSize * 2f, true).fillCircle(map.getSpawn(i).getPosition(), 10, false).intersect(spawnable);
                for (int j = 0; j < map.getSpawnCount(); j += numSymPoints) {
                    baseHydro.fillCircle(map.getSpawn(j).getPosition(), 16, false);
                }
                for (Hydro hydro : map.getHydros()) {
                    baseHydro.fillCircle(hydro.getPosition(), 16, false);
                }
                generateIndividualHydros(baseHydro, 1, hydroSpacing);
            }
        }
    }

    public void generateIndividualHydros(BinaryMask spawnable, int numHydros, int hydroSpacing) {
        LinkedList<Vector2f> hydroLocations = spawnable.getRandomCoordinates(hydroSpacing);
        hydroLocations.stream().limit(numHydros).forEachOrdered(location -> {
            int hydroId = map.getHydroCount() / spawnable.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();
            Hydro hydro = new Hydro(String.format("Hydro %d", hydroId), new Vector3f(location.add(.5f, .5f)));
            map.addHydro(hydro);
            ArrayList<SymmetryPoint> symmetryPoints = spawnable.getSymmetryPoints(hydro.getPosition());
            symmetryPoints.forEach(symmetryPoint -> map.addHydro(new Hydro(String.format("sym %d Hydro %d", symmetryPoints.indexOf(symmetryPoint), hydroId), new Vector3f(symmetryPoint.getLocation()))));
        });
    }

    public void setMarkerHeights() {
        for (Hydro hydro : map.getHydros()) {
            hydro.setPosition(placeOnHeightmap(map, hydro.getPosition()));
        }
    }
}
