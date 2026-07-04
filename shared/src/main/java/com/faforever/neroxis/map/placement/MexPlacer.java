package com.faforever.neroxis.map.placement;

import com.faforever.neroxis.map.AIMarker;
import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.util.List;
import java.util.random.RandomGenerator;

public class MexPlacer {
    private final SCMap map;
    private final RandomGenerator.SplittableGenerator random;

    protected int minMexesPerPlayer = 3;
    protected int maxMexesPerPlayer = 5;

    public MexPlacer(SCMap map, RandomGenerator.SplittableGenerator random) {
        this.map = map;
        this.random = random.split();
    }

    public void placeMexes(int mexCount, BooleanMask allowedMexMask, BooleanMask spawnMaskWater) {
        int mexSpacing = (int) (map.getSize() / 8f * StrictMath.min(
                StrictMath.max(40f / (mexCount * map.getSpawnCount()), .25f), 2f)) / 2;
        placeMexes(mexCount, allowedMexMask, spawnMaskWater, mexSpacing, 24, 48);
    }

    public void placeMexes(int mexCount, BooleanMask allowedMexMask, BooleanMask spawnMaskWater, int mexSpacing,
                           int spawnMexRadius, int remainingMexRadius) {
        map.getMexes().clear();

        if (!allowedMexMask.getSymmetrySettings().spawnSymmetry().isPerfectSymmetry()) {
            allowedMexMask.limitToCenteredCircle(allowedMexMask.getSize() / 2f);
        }
        allowedMexMask.limitToSymmetryRegion();
        spawnMaskWater.limitToSymmetryRegion();
        int numSymPoints = allowedMexMask.getSymmetrySettings().spawnSymmetry().getNumSymPoints();

        int previousMexCount;
        placeBaseMexes(allowedMexMask);
        int numMexesLeft = (mexCount - map.getMexCount()) / numSymPoints;
        map.getSpawns()
           .stream()
           .filter(spawn -> allowedMexMask.inTeam(spawn.getPosition(), false))
           .forEach(spawn -> allowedMexMask.fillCircle(spawn.getPosition(), spawnMexRadius, false));

        previousMexCount = map.getMexCount();
        if (numMexesLeft > 8 && numMexesLeft > map.getSpawnCount()) {
            int possibleExpMexCount = (random.nextInt(numMexesLeft / 2) + numMexesLeft / map.getSpawnCount());
            placeMexExpansions(allowedMexMask, possibleExpMexCount, mexSpacing);

            spacePlacedMexes(allowedMexMask, mexSpacing, previousMexCount);
            numMexesLeft = mexCount - map.getMexCount();
            previousMexCount = map.getMexCount();
        }

        int numPlayerMexes = (int) ((float) numMexesLeft / map.getSpawnCount() / numSymPoints * .5f);
        for (int i = 0; i < map.getSpawnCount(); i += allowedMexMask.getSymmetrySettings()
                                                                    .spawnSymmetry()
                                                                    .getNumSymPoints()) {
            Spawn spawn = map.getSpawn(i);
            BooleanMask playerSpawnMask = new BooleanMask(allowedMexMask.getSize(), random.split(),
                                                          allowedMexMask.getSymmetrySettings());
            playerSpawnMask.fillCircle(spawn.getPosition(), map.getSize() / 6f, true)
                           .multiply(allowedMexMask)
                           .fillEdge(map.getSize() / 16, false);
            map.getSpawns().forEach(otherSpawn -> {
                if (otherSpawn.getTeamID() == spawn.getTeamID() && !spawn.equals(otherSpawn)) {
                    playerSpawnMask.fillCircle(otherSpawn.getPosition(), map.getSize() / 8f, false);
                }
            });
            if (mexCount < 6) {
                placeIndividualMexes(playerSpawnMask, numPlayerMexes, mexSpacing * 2);
            } else {
                placeIndividualMexes(playerSpawnMask, numPlayerMexes, mexSpacing);
            }
            spacePlacedMexes(allowedMexMask, mexSpacing, previousMexCount);
            previousMexCount = map.getMexCount();
        }

        numMexesLeft = (mexCount - map.getMexCount()) / numSymPoints;
        map.getSpawns()
           .stream()
           .filter(spawn -> allowedMexMask.inTeam(spawn.getPosition(), false))
           .forEach(spawn -> allowedMexMask.fillCircle(spawn.getPosition(), remainingMexRadius, false));
        placeIndividualMexes(allowedMexMask, numMexesLeft, mexSpacing);
        spacePlacedMexes(allowedMexMask, mexSpacing, previousMexCount);

        numMexesLeft = (mexCount - map.getMexCount()) / numSymPoints;

        placeIndividualMexes(spawnMaskWater, StrictMath.min(numMexesLeft, 10), mexSpacing);
    }

    private void spacePlacedMexes(BooleanMask allowedMexMask, int mexSpacing, int previousMexCount) {
        map.getMexes()
           .stream()
           .skip(previousMexCount)
           .filter(mex -> allowedMexMask.inTeam(mex.getPosition(), false))
           .forEach(mex -> allowedMexMask.fillCircle(mex.getPosition(), mexSpacing, false));
    }

    private void placeBaseMexes(BooleanMask allowedMexMask) {
        int numBaseMexes = random.nextInt(minMexesPerPlayer, maxMexesPerPlayer + 1);
        int previousMexCount = 0;
        for (int i = 0; i < map.getSpawnCount(); i += allowedMexMask.getSymmetrySettings()
                                                                    .spawnSymmetry()
                                                                    .getNumSymPoints()) {
            Spawn spawn = map.getSpawn(i);
            BooleanMask baseMexes = new BooleanMask(allowedMexMask.getSize(), random.split(),
                                                    allowedMexMask.getSymmetrySettings());
            baseMexes.fillCircle(spawn.getPosition(), 15, true)
                     .fillCircle(spawn.getPosition(), 5, false)
                     .multiply(allowedMexMask);
            placeIndividualMexes(baseMexes, numBaseMexes, 10);
            spacePlacedMexes(allowedMexMask, 2, previousMexCount);
            previousMexCount += numBaseMexes;
        }
    }

    private void placeMexExpansions(BooleanMask allowedMexMask, int possibleExpMexCount, int mexSpacing) {
        Vector2 expLocation;
        int expMexCount;
        int expMexCountLeft = possibleExpMexCount;
        int expMexSpacing = 10;
        int expSize = 10;
        int expSpacing = (int) (map.getSize() / 4f * StrictMath.min(StrictMath.max(8f / possibleExpMexCount, .75f),
                                                                    1.75f));

        BooleanMask expansionSpawnMask = new BooleanMask(allowedMexMask.getSize(), random.split(),
                                                         allowedMexMask.getSymmetrySettings());
        expansionSpawnMask.invert().fillCenter(96, false).fillEdge(32, false).multiply(allowedMexMask);

        map.getSpawns()
           .stream()
           .filter(spawn -> expansionSpawnMask.inTeam(spawn.getPosition(), false))
           .forEach(spawn -> expansionSpawnMask.fillCircle(spawn.getPosition(), map.getSize() / 6f, false));

        expMexCount = StrictMath.min((random.nextInt(2) + 3), expMexCountLeft);

        List<Vector2> expansionLocations = expansionSpawnMask.getRandomCoordinates(expSpacing);

        while (expMexCountLeft > expMexCount) {
            if (expansionLocations.isEmpty()) {
                break;
            }

            expLocation = expansionLocations.removeFirst();

            while (!isMexExpValid(expLocation, expSize, allowedMexMask)) {
                if (expansionLocations.isEmpty()) {
                    expLocation = null;
                    break;
                }
                expLocation = expansionLocations.removeFirst();
            }

            if (expLocation == null) {
                break;
            }

            BooleanMask expansionMask = new BooleanMask(allowedMexMask.getSize(), random.split(),
                                                        allowedMexMask.getSymmetrySettings());
            expansionMask.fillCircle(expLocation, expSize, true);
            expansionMask.multiply(allowedMexMask);

            int expID = map.getLargeExpansionMarkerCount() / allowedMexMask.getSymmetrySettings()
                                                                           .spawnSymmetry()
                                                                           .getNumSymPoints();
            List<Vector2> symmetryPoints = expansionSpawnMask.getSymmetryPoints(expLocation, SymmetryType.SPAWN)
                                                             .stream()
                                                             .map(Vector2::roundToNearestHalfPoint)
                                                             .toList();
            if (expMexCount >= 3) {
                map.addLargeExpansionMarker(
                        new AIMarker(String.format("Large Expansion Area %d", expID), expLocation, null));
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    map.addLargeExpansionMarker(
                            new AIMarker(String.format("Large Expansion Area %d sym %d", expID, i),
                                         symmetryPoints.get(i),
                                         null));
                }
            } else {
                map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d", expID), expLocation, null));
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    map.addExpansionMarker(
                            new AIMarker(String.format("Expansion Area %d sym %d", expID, i), symmetryPoints.get(i),
                                         null));
                }
            }

            placeIndividualMexes(expansionMask, expMexCount, expMexSpacing);
            allowedMexMask.fillCircle(expLocation, mexSpacing * 3f * expMexCount / 4f, false);
            expMexCountLeft -= expMexCount;
        }
    }

    private void placeIndividualMexes(BooleanMask individualMexMask, int numMexes, int mexSpacing) {
        if (numMexes > 0) {
            List<Vector2> mexLocations = individualMexMask.getRandomCoordinates(mexSpacing);
            mexLocations.stream().limit(numMexes).map(Vector2::roundToNearestHalfPoint).forEach(location -> {
                int mexID = map.getMexCount() /
                            individualMexMask.getSymmetrySettings().spawnSymmetry().getNumSymPoints();
                Marker mex = new Marker(String.format("Mex %d", mexID),
                                        new Vector3(location));
                map.addMex(mex);
                List<Vector2> symmetryPoints = individualMexMask.getSymmetryPoints(mex.getPosition(),
                                                                                   SymmetryType.SPAWN)
                                                                .stream()
                                                                .map(Vector2::roundToNearestHalfPoint)
                                                                .toList();
                for (int i = 0; i < symmetryPoints.size(); i++) {
                    Vector2 symmetryPoint = symmetryPoints.get(i);
                    Marker marker = new Marker(String.format("Mex %d sym %d", mexID, i),
                                               new Vector3(symmetryPoint));
                    map.addMex(marker);
                }
            });
        }
    }

    private boolean isMexExpValid(Vector2 location, float size, BooleanMask allowedMexMask) {
        float count = 0;

        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                Vector2 loc = location.add(dx - size / 2, dy - size / 2);
                if (allowedMexMask.inBounds(loc)) {
                    if (allowedMexMask.get(loc)) {
                        ++count;
                    }
                }
            }
        }
        return count / (size * size) > 0.5;
    }
}
