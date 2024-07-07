package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;
import java.util.Random;

public class SpawnPlacer {
    private final SCMap map;
    private final Random random;

    public SpawnPlacer(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void placeSpawns(int spawnCount, float teammateSeparation, int teamSeparation,
                            SymmetrySettings symmetrySettings) {
        map.getLargeExpansionAIMarkers().clear();
        map.getSpawns().clear();
        BooleanMask spawnMask = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings).invert();
        spawnMask.fillSides(map.getSize() / spawnCount * 3 / 2, false)
                 .fillCenter(teamSeparation, false, SymmetryType.TEAM)
                 .fillCenter(15, false, SymmetryType.SPAWN)
                 .fillEdge(map.getSize() / 16, false)
                 .limitToSymmetryRegion(SymmetryType.TEAM);
        if (!spawnMask.getSymmetrySettings().spawnSymmetry().isPerfectSymmetry()) {
            spawnMask.limitToCenteredCircle(spawnMask.getSize() / 2f - map.getSize() / 16f);
        }
        Vector2 location = spawnMask.getRandomPosition();
        while (map.getSpawnCount() < spawnCount) {
            if (location == null) {
                placeSpawns(spawnCount, StrictMath.max(teammateSeparation - 4, 0),
                            StrictMath.max(teamSeparation - 2, 0), symmetrySettings);
                return;
            }
            location.roundToNearestHalfPoint();
            spawnMask.fillCircle(location, teammateSeparation, false);
            List<Vector2> symmetryPoints = spawnMask.getSymmetryPoints(location, SymmetryType.SPAWN);
            symmetryPoints.forEach(Vector2::roundToNearestHalfPoint);
            symmetryPoints.forEach(symmetryPoint -> spawnMask.fillCircle(symmetryPoint, teamSeparation, false));

            addSpawn(location, symmetryPoints);
            if (spawnMask.getSymmetrySettings().spawnSymmetry().getNumSymPoints() != 1) {
                BooleanMask nextSpawn = new BooleanMask(spawnMask.getSize(), random.nextLong(),
                                                        spawnMask.getSymmetrySettings());
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

    public void placeSpawns(int spawnCount, BooleanMask spawnMask, float teammateSeparation, int teamSeparation) {
        map.getLargeExpansionAIMarkers().clear();
        map.getSpawns().clear();
        BooleanMask spawnMaskCopy = spawnMask.copy();
        spawnMaskCopy.fillSides(map.getSize() / spawnCount * 3 / 2, false)
                     .fillCenter(teamSeparation, false, SymmetryType.TEAM)
                     .fillCenter(15, false, SymmetryType.SPAWN)
                     .fillEdge(map.getSize() / 32, false)
                     .limitToSymmetryRegion(SymmetryType.TEAM);
        Vector2 location = spawnMaskCopy.getRandomPosition();
        while (map.getSpawnCount() < spawnCount) {
            if (location == null) {
                if (teammateSeparation - 4 >= 10) {
                    placeSpawns(spawnCount, spawnMask, StrictMath.max(teammateSeparation - 4, 0), teamSeparation);
                    break;
                } else {
                    return;
                }
            }
            spawnMaskCopy.fillCircle(location, teammateSeparation, false);
            List<Vector2> symmetryPoints = spawnMaskCopy.getSymmetryPoints(location, SymmetryType.SPAWN);
            symmetryPoints.forEach(Vector2::roundToNearestHalfPoint);
            symmetryPoints.forEach(symmetryPoint -> spawnMaskCopy.fillCircle(symmetryPoint, teammateSeparation, false));

            if (spawnMaskCopy.getSymmetrySettings().spawnSymmetry() == Symmetry.POINT2) {
                symmetryPoints.forEach(
                        symmetryPoint -> spawnMaskCopy.fillCircle(symmetryPoint, teammateSeparation, false));
            }
            addSpawn(location, symmetryPoints);
            location = spawnMaskCopy.getRandomPosition();
        }
    }

    private void addSpawn(Vector2 location, List<Vector2> symmetryPoints) {
        map.addSpawn(new Spawn(String.format("ARMY_%d", map.getSpawnCount() + 1), location, new Vector2(0, 0), 0));
        Group initial = new Group("INITIAL");
        Army army = new Army(String.format("ARMY_%d", map.getArmyCount() + 1));
        army.addGroup(initial);
        map.addArmy(army);
        for (int i = 0; i < symmetryPoints.size(); ++i) {
            Vector2 symmetryPoint = symmetryPoints.get(i);
            map.addSpawn(new Spawn(String.format("ARMY_%d", map.getSpawnCount() + 1), symmetryPoint, new Vector2(0, 0),
                                   i + 1));
            Group initialSym = new Group("INITIAL");
            Army armySym = new Army(String.format("ARMY_%d", map.getArmyCount() + 1));
            armySym.addGroup(initialSym);
            map.addArmy(armySym);
        }
    }
}
