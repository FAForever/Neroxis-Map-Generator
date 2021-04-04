package neroxis.generator;

import neroxis.map.*;
import neroxis.util.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public strictfp class SpawnPlacer {
    private final SCMap map;
    private final Random random;

    public SpawnPlacer(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void placeSpawns(float teammateSeparation, int teamSeparation, SymmetrySettings symmetrySettings) {
        map.getLargeExpansionAIMarkers().clear();
        map.getSpawns().clear();
        BinaryMask spawnMask = new BinaryMask(map.getSize() + 1, random.nextLong(), symmetrySettings).invert();
        spawnMask.fillSides(map.getSize() / map.getSpawnCountInit() * 3 / 2, false).fillCenter(teamSeparation, false).fillEdge(map.getSize() / 16, false).limitToSymmetryRegion();
        if (!spawnMask.getSymmetrySettings().getSpawnSymmetry().isPerfectSymmetry()) {
            spawnMask.limitToCenteredCircle(spawnMask.getSize() / 2f);
        }
        Vector2f location = spawnMask.getRandomPosition();
        while (map.getSpawnCount() < map.getSpawnCountInit()) {
            if (location == null) {
                placeSpawns(StrictMath.max(teammateSeparation - 4, 0), teamSeparation, symmetrySettings);
                return;
            }
            location.add(.5f, .5f);
            spawnMask.fillCircle(location, teammateSeparation, false);
            List<Vector2f> symmetryPoints = spawnMask.getSymmetryPoints(location, SymmetryType.SPAWN);
            symmetryPoints.forEach(Vector2f::roundToNearestHalfPoint);
            symmetryPoints.forEach(symmetryPoint -> spawnMask.fillCircle(symmetryPoint, teamSeparation, false));

            addSpawn(location, symmetryPoints);
            if (spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints() != 1) {
                BinaryMask nextSpawn = new BinaryMask(spawnMask.getSize(), random.nextLong(), spawnMask.getSymmetrySettings());
                nextSpawn.fillCircle(location, teammateSeparation * 2, true).intersect(spawnMask);
                location = nextSpawn.getRandomPosition();
                if (location == null) {
                    location = spawnMask.getRandomPosition();
                }
            } else {
                location = spawnMask.getRandomPosition();
            }
        }
    }

    public void placeSpawns(BinaryMask spawnMask, float separation) {
        map.getLargeExpansionAIMarkers().clear();
        map.getSpawns().clear();
        BinaryMask spawnMaskCopy = spawnMask.copy();
        spawnMaskCopy.fillSides(map.getSize() / map.getSpawnCountInit() * 3 / 2, false).fillCenter(map.getSize() * 3 / 8, false).fillEdge(map.getSize() / 32, false).limitToSymmetryRegion();
        Vector2f location = spawnMaskCopy.getRandomPosition();
        while (map.getSpawnCount() < map.getSpawnCountInit()) {
            if (location == null) {
                if (separation - 4 >= 10) {
                    placeSpawns(spawnMask, separation - 8);
                    break;
                } else {
                    return;
                }
            }
            spawnMaskCopy.fillCircle(location, separation, false);
            List<Vector2f> symmetryPoints = spawnMaskCopy.getSymmetryPoints(location, SymmetryType.SPAWN);
            symmetryPoints.forEach(Vector2f::roundToNearestHalfPoint);
            symmetryPoints.forEach(symmetryPoint -> spawnMaskCopy.fillCircle(symmetryPoint, separation, false));

            if (spawnMaskCopy.getSymmetrySettings().getSpawnSymmetry() == Symmetry.POINT2) {
                symmetryPoints.forEach(symmetryPoint -> spawnMaskCopy.fillCircle(symmetryPoint, separation, false));
            }
            addSpawn(location, symmetryPoints);
            location = spawnMaskCopy.getRandomPosition();
        }
    }

    private void addSpawn(Vector2f location, List<Vector2f> symmetryPoints) {
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
