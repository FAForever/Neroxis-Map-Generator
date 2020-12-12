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

    public BinaryMask[] generateSpawns(float separation, SymmetrySettings symmetrySettings, float plateauDensity) {
        map.getLargeExpansionAIMarkers().clear();
        map.getSpawns().clear();
        BinaryMask spawnable = new BinaryMask(map.getSize() + 1, random.nextLong(), symmetrySettings).invert();
        BinaryMask spawnLandMask = new BinaryMask(map.getSize() + 1, random.nextLong(), spawnable.getSymmetrySettings());
        BinaryMask spawnPlateauMask = new BinaryMask(map.getSize() + 1, random.nextLong(), spawnable.getSymmetrySettings());
        int centerFill = StrictMath.min(map.getSize() / 2, 256);
        spawnable.limitToSymmetryRegion().fillSides(map.getSize() / map.getSpawnCountInit() * 3 / 2, false).fillCenter(centerFill, false).fillEdge(map.getSize() / 16, false);
        Vector2f location = spawnable.getRandomPosition();
        while (map.getSpawnCount() < map.getSpawnCountInit()) {
            if (location == null) {
                if (separation - 4 >= 10) {
                    return generateSpawns(separation - 8, symmetrySettings, plateauDensity);
                } else {
                    return null;
                }
            }
            location.add(.5f, .5f);
            spawnable.fillCircle(location, separation, false);
            ArrayList<SymmetryPoint> symmetryPoints = spawnable.getSymmetryPoints(location);
            symmetryPoints.forEach(symmetryPoint -> {
                if (spawnable.inTeam(symmetryPoint.getLocation(), false)) {
                    spawnable.fillCircle(symmetryPoint.getLocation(), separation, false);
                } else {
                    spawnable.fillCircle(symmetryPoint.getLocation(), map.getSize() / 2f, false);
                }
            });

            spawnLandMask.fillCircle(location, spawnSize, true);
            symmetryPoints.forEach(symmetryPoint -> spawnLandMask.fillCircle(symmetryPoint.getLocation(), spawnSize, true));

            boolean valid;
            if (random.nextFloat() < plateauDensity) {
                valid = true;
                for (Spawn spawn : map.getSpawns()) {
                    if (!spawnPlateauMask.getValueAt(spawn.getPosition()) && spawn.getPosition().getXZDistance(location) < spawnSize * 8) {
                        valid = false;
                        break;
                    }
                }
            } else {
                valid = false;
                for (Spawn spawn : map.getSpawns()) {
                    if (spawnPlateauMask.getValueAt(spawn.getPosition()) && spawn.getPosition().getXZDistance(location) < spawnSize * 8) {
                        valid = true;
                        break;
                    }
                }
            }
            if (valid) {
                spawnPlateauMask.fillCircle(location, spawnSize, true);
                symmetryPoints.forEach(symmetryPoint -> spawnPlateauMask.fillCircle(symmetryPoint.getLocation(), spawnSize, true));
            }

            map.addSpawn(new Spawn(String.format("ARMY_%d", map.getSpawnCount() + 1), location, new Vector2f(0, 0)));
            Group initial = new Group("INITIAL", new ArrayList<>());
            Army army = new Army(String.format("ARMY_%d", map.getArmyCount() + 1), new ArrayList<>());
            army.addGroup(initial);
            map.addArmy(army);
            symmetryPoints.forEach(symmetryPoint -> {
                map.addSpawn(new Spawn(String.format("ARMY_%d", map.getSpawnCount() + 1), symmetryPoint.getLocation(), new Vector2f(0, 0)));
                Group initialSym = new Group("INITIAL", new ArrayList<>());
                Army armySym = new Army(String.format("ARMY_%d", map.getArmyCount() + 1), new ArrayList<>());
                armySym.addGroup(initialSym);
                map.addArmy(armySym);
            });

            map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), location, null));
            symmetryPoints.forEach(symmetryPoint -> map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), symmetryPoint.getLocation(), null)));
            BinaryMask nextSpawn = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
            nextSpawn.fillCircle(location, separation * 4, true).intersect(spawnable);
            location = nextSpawn.getRandomPosition();
            if (location == null) {
                location = spawnable.getRandomPosition();
            }
        }
        return new BinaryMask[]{spawnLandMask, spawnPlateauMask};
    }

    public void generateSpawns(BinaryMask spawnable, float separation) {
        map.getLargeExpansionAIMarkers().clear();
        map.getSpawns().clear();
        BinaryMask spawnableCopy = spawnable.copy();
        spawnableCopy.limitToSymmetryRegion().fillSides(map.getSize() / map.getSpawnCountInit() * 3 / 2, false).fillCenter(map.getSize() * 3 / 8, false).fillEdge(map.getSize() / 32, false);
        Vector2f location = spawnableCopy.getRandomPosition();
        while (map.getSpawnCount() < map.getSpawnCountInit()) {
            if (location == null) {
                if (separation - 4 >= 10) {
                    generateSpawns(spawnable, separation - 8);
                    break;
                } else {
                    return;
                }
            }
            spawnableCopy.fillCircle(location, separation, false);
            ArrayList<SymmetryPoint> symmetryPoints = spawnableCopy.getSymmetryPoints(location);
            symmetryPoints.forEach(symmetryPoint -> spawnableCopy.fillCircle(symmetryPoint.getLocation(), separation, false));

            if (spawnableCopy.getSymmetrySettings().getSpawnSymmetry() == Symmetry.POINT2) {
                symmetryPoints.forEach(symmetryPoint -> spawnableCopy.fillCircle(symmetryPoint.getLocation(), separation, false));
            }
            map.addSpawn(new Spawn(String.format("ARMY_%d", map.getSpawnCount() + 1), location, new Vector2f(0, 0)));
            Group initial = new Group("INITIAL", new ArrayList<>());
            Army army = new Army(String.format("ARMY_%d", map.getArmyCount() + 1), new ArrayList<>());
            army.addGroup(initial);
            map.addArmy(army);
            symmetryPoints.forEach(symmetryPoint -> {
                map.addSpawn(new Spawn(String.format("ARMY_%d", map.getSpawnCount() + 1), symmetryPoint.getLocation(), new Vector2f(0, 0)));
                Group initialSym = new Group("INITIAL", new ArrayList<>());
                Army armySym = new Army(String.format("ARMY_%d", map.getArmyCount() + 1), new ArrayList<>());
                armySym.addGroup(initialSym);
                map.addArmy(armySym);
            });
            map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), location, null));
            symmetryPoints.forEach(symmetryPoint -> map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), symmetryPoint.getLocation(), null)));
            location = spawnableCopy.getRandomPosition();
        }
    }

    public void setMarkerHeights() {
        for (Spawn spawn : map.getSpawns()) {
            spawn.setPosition(placeOnHeightmap(map, spawn.getPosition()));
        }
    }
}
