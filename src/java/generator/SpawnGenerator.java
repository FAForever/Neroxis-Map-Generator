package generator;

import map.*;
import util.Vector2f;

import java.util.ArrayList;
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
        BinaryMask spawnLandMask = new BinaryMask(map.getSize() + 1, random.nextLong(), spawnable.getSymmetrySettings());
        BinaryMask spawnPlateauMask = new BinaryMask(map.getSize() + 1, random.nextLong(), spawnable.getSymmetrySettings());
        if (map.getSpawnCountInit() == 2 && (symmetry == Symmetry.POINT || symmetry == Symmetry.DIAG || symmetry == Symmetry.QUAD)) {
            spawnable.getSymmetrySettings().setSpawnSymmetry(Symmetry.POINT);
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
            location.add(.5f, .5f);
            symLocation = spawnable.getSymmetryPoint(location);
            spawnable.fillCircle(location, separation, false);
            spawnable.fillCircle(symLocation, separation, false);

            if (spawnable.getSymmetrySettings().getSpawnSymmetry() == Symmetry.POINT) {
                spawnable.fillCircle(symLocation, map.getSize() * 4 / 8f, false);
            }

            spawnLandMask.fillCircle(location, spawnSize, true);
            spawnLandMask.fillCircle(symLocation, spawnSize, true);

            if (random.nextFloat() < plateauDensity) {
                boolean valid = true;
                for (int j = 0; j < i; j += 2) {
                    if (!spawnPlateauMask.get(map.getSpawn(j).getPosition()) && map.getSpawn(j).getPosition().getXZDistance(location) < spawnSize * 8) {
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
                    if (spawnPlateauMask.get(map.getSpawn(j).getPosition()) && map.getSpawn(j).getPosition().getXZDistance(location) < spawnSize * 8) {
                        valid = true;
                        break;
                    }
                }
                if (valid) {
                    spawnPlateauMask.fillCircle(location, spawnSize, true);
                    spawnPlateauMask.fillCircle(symLocation, spawnSize, true);
                }
            }
            map.addSpawn(new Spawn(String.format("ARMY_%d", i + 1), location, new Vector2f(0, 0)));
            map.addSpawn(new Spawn(String.format("ARMY_%d", i + 2), symLocation, new Vector2f(0, 0)));
            Group initial1 = new Group("INITIAL", new ArrayList<>());
            Army army1 = new Army(String.format("ARMY_%d", i + 1), new ArrayList<>());
            army1.addGroup(initial1);
            Group initial2 = new Group("INITIAL", new ArrayList<>());
            Army army2 = new Army(String.format("ARMY_%d", i + 1), new ArrayList<>());
            army2.addGroup(initial2);
            map.addArmy(army1);
            map.addArmy(army2);
            map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), location, null));
            map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), symLocation, null));
            location = spawnable.getRandomPosition();
        }
        return new BinaryMask[]{spawnLandMask, spawnPlateauMask};
    }

    public void generateSpawns(BinaryMask spawnable, float separation) {
        map.getLargeExpansionAIMarkers().clear();
        map.getSpawns().clear();
        BinaryMask spawnableCopy = spawnable.copy();
        spawnableCopy.fillHalf(false).fillSides(map.getSize() / map.getSpawnCountInit() * 3 / 2, false).fillCenter(map.getSize() * 3 / 8, false).trimEdge(map.getSize() / 32);
        Vector2f location = spawnableCopy.getRandomPosition();
        Vector2f symLocation;
        for (int i = 0; i < map.getSpawnCountInit(); i += 2) {
            if (location == null) {
                if (separation - 4 >= 10) {
                    generateSpawns(spawnable, separation - 8);
                    break;
                } else {
                    return;
                }
            }
            symLocation = spawnableCopy.getSymmetryPoint(location);
            spawnableCopy.fillCircle(location, separation, false);
            spawnableCopy.fillCircle(symLocation, separation, false);

            if (spawnableCopy.getSymmetrySettings().getSpawnSymmetry() == Symmetry.POINT) {
                spawnableCopy.fillCircle(symLocation, map.getSize() * 3 / 8f, false);
            }
            map.addSpawn(new Spawn(String.format("ARMY_%d", i + 1), location, new Vector2f(0, 0)));
            map.addSpawn(new Spawn(String.format("ARMY_%d", i + 2), symLocation, new Vector2f(0, 0)));
            map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), location, null));
            map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), symLocation, null));
            location = spawnableCopy.getRandomPosition();
        }
    }

    public void setMarkerHeights() {
        for (Spawn spawn : map.getSpawns()) {
            spawn.setPosition(placeOnHeightmap(map, spawn.getPosition()));
        }
    }
}
