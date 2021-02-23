package neroxis.generator;

import neroxis.map.*;
import neroxis.util.Vector2f;

import java.util.ArrayList;
import java.util.Random;

public strictfp class SpawnGenerator {
    private final SCMap map;
    private final Random random;

    public SpawnGenerator(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public BinaryMask[] generateSpawns(float separation, SymmetrySettings symmetrySettings, float plateauDensity, int spawnSize) {
        map.getLargeExpansionAIMarkers().clear();
        map.getSpawns().clear();
        BinaryMask spawnMask = new BinaryMask(map.getSize() + 1, random.nextLong(), symmetrySettings).invert();
        BinaryMask spawnLandMask = new BinaryMask(map.getSize() + 1, random.nextLong(), spawnMask.getSymmetrySettings());
        BinaryMask spawnPlateauMask = new BinaryMask(map.getSize() + 1, random.nextLong(), spawnMask.getSymmetrySettings());
        int centerFill = StrictMath.min(map.getSize() * 3 / 8, 256);
        spawnMask.fillSides(map.getSize() / map.getSpawnCountInit() * 3 / 2, false).fillCenter(centerFill, false).fillEdge(map.getSize() / 16, false).limitToSymmetryRegion();
        Vector2f location = spawnMask.getRandomPosition();
        while (map.getSpawnCount() < map.getSpawnCountInit()) {
            if (location == null) {
                if (separation - 4 >= 10) {
                    return generateSpawns(separation - 8, symmetrySettings, plateauDensity, spawnSize);
                } else {
                    return null;
                }
            }
            location.add(.5f, .5f);
            spawnMask.fillCircle(location, separation, false);
            ArrayList<Vector2f> symmetryPoints = spawnMask.getSymmetryPoints(location, SymmetryType.SPAWN);
            symmetryPoints.forEach(Vector2f::roundToNearestHalfPoint);
            symmetryPoints.forEach(symmetryPoint -> spawnMask.fillCircle(symmetryPoint, centerFill, false));

            spawnLandMask.fillCircle(location, spawnSize, true);
            symmetryPoints.forEach(symmetryPoint -> spawnLandMask.fillCircle(symmetryPoint, spawnSize, true));

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
                symmetryPoints.forEach(symmetryPoint -> spawnPlateauMask.fillCircle(symmetryPoint, spawnSize, true));
            }

            addSpawn(location, symmetryPoints);
            BinaryMask nextSpawn = new BinaryMask(spawnMask.getSize(), random.nextLong(), spawnMask.getSymmetrySettings());
            nextSpawn.fillCircle(location, separation * 4, true).intersect(spawnMask);
            location = nextSpawn.getRandomPosition();
            if (location == null) {
                location = spawnMask.getRandomPosition();
            }
        }
        return new BinaryMask[]{spawnLandMask, spawnPlateauMask};
    }

    public void generateSpawns(BinaryMask spawnMask, float separation) {
        map.getLargeExpansionAIMarkers().clear();
        map.getSpawns().clear();
        BinaryMask spawnMaskCopy = spawnMask.copy();
        spawnMaskCopy.fillSides(map.getSize() / map.getSpawnCountInit() * 3 / 2, false).fillCenter(map.getSize() * 3 / 8, false).fillEdge(map.getSize() / 32, false).limitToSymmetryRegion();
        Vector2f location = spawnMaskCopy.getRandomPosition();
        while (map.getSpawnCount() < map.getSpawnCountInit()) {
            if (location == null) {
                if (separation - 4 >= 10) {
                    generateSpawns(spawnMask, separation - 8);
                    break;
                } else {
                    return;
                }
            }
            spawnMaskCopy.fillCircle(location, separation, false);
            ArrayList<Vector2f> symmetryPoints = spawnMaskCopy.getSymmetryPoints(location, SymmetryType.SPAWN);
            symmetryPoints.forEach(Vector2f::roundToNearestHalfPoint);
            symmetryPoints.forEach(symmetryPoint -> spawnMaskCopy.fillCircle(symmetryPoint, separation, false));

            if (spawnMaskCopy.getSymmetrySettings().getSpawnSymmetry() == Symmetry.POINT2) {
                symmetryPoints.forEach(symmetryPoint -> spawnMaskCopy.fillCircle(symmetryPoint, separation, false));
            }
            addSpawn(location, symmetryPoints);
            location = spawnMaskCopy.getRandomPosition();
        }
    }

    private void addSpawn(Vector2f location, ArrayList<Vector2f> symmetryPoints) {
        map.addSpawn(new Spawn(String.format("ARMY_%d", map.getSpawnCount() + 1), location, new Vector2f(0, 0), 0));
        Group initial = new Group("INITIAL", new ArrayList<>());
        Army army = new Army(String.format("ARMY_%d", map.getArmyCount() + 1), new ArrayList<>());
        army.addGroup(initial);
        map.addArmy(army);
        for (int i = 0; i < symmetryPoints.size(); ++i) {
            Vector2f symmetryPoint = symmetryPoints.get(i);
            map.addSpawn(new Spawn(String.format("ARMY_%d", map.getSpawnCount() + 1), symmetryPoint, new Vector2f(0, 0), i + 1));
            Group initialSym = new Group("INITIAL", new ArrayList<>());
            Army armySym = new Army(String.format("ARMY_%d", map.getArmyCount() + 1), new ArrayList<>());
            armySym.addGroup(initialSym);
            map.addArmy(armySym);
        }
    }
}
