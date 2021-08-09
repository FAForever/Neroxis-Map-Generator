package com.faforever.neroxis.map.generator.placement;

import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.Vector2;

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

    public void placeSpawns(int spawnCount, float teammateSeparation, int teamSeparation, SymmetrySettings symmetrySettings) {
        map.getLargeExpansionAIMarkers().clear();
        map.getSpawns().clear();
        BooleanMask spawnMask = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings).invert();
        spawnMask.fillSides(map.getSize() / spawnCount * 3 / 2, false).fillCenter(teamSeparation, false).fillEdge(map.getSize() / 16, false).limitToSymmetryRegion();
        if (!spawnMask.getSymmetrySettings().getSpawnSymmetry().isPerfectSymmetry()) {
            spawnMask.limitToCenteredCircle(spawnMask.getSize() / 2f - map.getSize() / 16f);
        }
        Vector2 location = spawnMask.getRandomPosition();
        while (map.getSpawnCount() < spawnCount) {
            if (location == null) {
                placeSpawns(spawnCount, StrictMath.max(teammateSeparation - 4, 0), teamSeparation, symmetrySettings);
                return;
            }
            location.add(.5f, .5f);
            spawnMask.fillCircle(location, teammateSeparation, false);
            List<Vector2> symmetryPoints = spawnMask.getSymmetryPoints(location, SymmetryType.SPAWN);
            symmetryPoints.forEach(Vector2::roundToNearestHalfPoint);
            symmetryPoints.forEach(symmetryPoint -> spawnMask.fillCircle(symmetryPoint, teamSeparation, false));

            addSpawn(location, symmetryPoints);
            if (spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints() != 1) {
                BooleanMask nextSpawn = new BooleanMask(spawnMask.getSize(), random.nextLong(), spawnMask.getSymmetrySettings());
                nextSpawn.fillCircle(location, teammateSeparation * 2, true).multiply(spawnMask);
                location = nextSpawn.getRandomPosition();
                if (location == null) {
                    location = spawnMask.getRandomPosition();
                }
            } else {
                location = spawnMask.getRandomPosition();
            }
        }
    }

    public void placeSpawns(int spawnCount, BooleanMask spawnMask, float separation) {
        map.getLargeExpansionAIMarkers().clear();
        map.getSpawns().clear();
        BooleanMask spawnMaskCopy = spawnMask.copy();
        spawnMaskCopy.fillSides(map.getSize() / spawnCount * 3 / 2, false).fillCenter(map.getSize() * 3 / 8, false).fillEdge(map.getSize() / 32, false).limitToSymmetryRegion();
        Vector2 location = spawnMaskCopy.getRandomPosition();
        while (map.getSpawnCount() < spawnCount) {
            if (location == null) {
                if (separation - 4 >= 10) {
                    placeSpawns(spawnCount, spawnMask, separation - 8);
                    break;
                } else {
                    return;
                }
            }
            spawnMaskCopy.fillCircle(location, separation, false);
            List<Vector2> symmetryPoints = spawnMaskCopy.getSymmetryPoints(location, SymmetryType.SPAWN);
            symmetryPoints.forEach(Vector2::roundToNearestHalfPoint);
            symmetryPoints.forEach(symmetryPoint -> spawnMaskCopy.fillCircle(symmetryPoint, separation, false));

            if (spawnMaskCopy.getSymmetrySettings().getSpawnSymmetry() == Symmetry.POINT2) {
                symmetryPoints.forEach(symmetryPoint -> spawnMaskCopy.fillCircle(symmetryPoint, separation, false));
            }
            addSpawn(location, symmetryPoints);
            location = spawnMaskCopy.getRandomPosition();
        }
    }

    private void addSpawn(Vector2 location, List<Vector2> symmetryPoints) {
        map.addSpawn(new Spawn(String.format("ARMY_%d", map.getSpawnCount() + 1), location, new Vector2(0, 0), 0));
        Group initial = new Group("INITIAL", new ArrayList<>());
        Army army = new Army(String.format("ARMY_%d", map.getArmyCount() + 1), new ArrayList<>());
        army.addGroup(initial);
        map.addArmy(army);
        for (int i = 0; i < symmetryPoints.size(); ++i) {
            Vector2 symmetryPoint = symmetryPoints.get(i);
            map.addSpawn(new Spawn(String.format("ARMY_%d", map.getSpawnCount() + 1), symmetryPoint, new Vector2(0, 0), i + 1));
            Group initialSym = new Group("INITIAL", new ArrayList<>());
            Army armySym = new Army(String.format("ARMY_%d", map.getArmyCount() + 1), new ArrayList<>());
            armySym.addGroup(initialSym);
            map.addArmy(armySym);
        }
    }
}
