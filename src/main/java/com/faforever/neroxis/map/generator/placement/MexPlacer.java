package com.faforever.neroxis.map.generator.placement;

import com.faforever.neroxis.map.*;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.Vector2;
import com.faforever.neroxis.util.Vector3;

import java.util.List;
import java.util.Random;

public strictfp class MexPlacer {
    private final SCMap map;
    private final Random random;

    public MexPlacer(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void placeMexes(int mexCount, BooleanMask spawnMask, BooleanMask spawnMaskWater) {
        map.getMexes().clear();
        int mexSpacing = (int) (map.getSize() / 8 * StrictMath.min(StrictMath.max(40f / (mexCount * map.getSpawnCount()), .25f), 2f));
        if (!spawnMask.getSymmetrySettings().getSpawnSymmetry().isPerfectSymmetry()) {
            spawnMask.limitToCenteredCircle(spawnMask.getSize() / 2f);
        }
        spawnMask.limitToSymmetryRegion();
        spawnMaskWater.limitToSymmetryRegion();
        int numSymPoints = spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();

        int previousMexCount;
        placeBaseMexes(spawnMask);
        int numMexesLeft = (mexCount - map.getMexCount()) / numSymPoints;
        map.getSpawns().stream().filter(spawn -> spawnMask.inTeam(spawn.getPosition(), false)).forEach(spawn -> spawnMask.fillCircle(spawn.getPosition(), 24, false));

        previousMexCount = map.getMexCount();
        if (numMexesLeft > 8 && numMexesLeft > map.getSpawnCount()) {
            int possibleExpMexCount = (random.nextInt(numMexesLeft / 2) + numMexesLeft / map.getSpawnCount());
            placeMexExpansions(spawnMask, possibleExpMexCount, mexSpacing);

            spacePlacedMexes(spawnMask, mexSpacing, previousMexCount);
            numMexesLeft = mexCount - map.getMexCount();
            previousMexCount = map.getMexCount();
        }

        int numPlayerMexes = (int) (numMexesLeft / map.getSpawnCount() / numSymPoints * .75f);
        for (int i = 0; i < map.getSpawnCount(); i += spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints()) {
            Spawn spawn = map.getSpawn(i);
            BooleanMask playerSpawnMask = new BooleanMask(spawnMask.getSize(), 0L, spawnMask.getSymmetrySettings());
            playerSpawnMask.fillCircle(spawn.getPosition(), map.getSize() / 2f, true).multiply(spawnMask).fillEdge(map.getSize() / 16, false);
            if (mexCount < 6) {
                placeIndividualMexes(playerSpawnMask, numPlayerMexes, mexSpacing * 2);
            } else {
                placeIndividualMexes(playerSpawnMask, numPlayerMexes, mexSpacing);
            }
            spacePlacedMexes(spawnMask, mexSpacing, previousMexCount);
            previousMexCount = map.getMexCount();
        }

        numMexesLeft = (mexCount - map.getMexCount()) / numSymPoints;
        map.getSpawns().stream().filter(spawn -> spawnMask.inTeam(spawn.getPosition(), false)).forEach(spawn -> spawnMask.fillCircle(spawn.getPosition(), 48, false));
        placeIndividualMexes(spawnMask, numMexesLeft, mexSpacing);
        spacePlacedMexes(spawnMask, mexSpacing, previousMexCount);

        numMexesLeft = (mexCount - map.getMexCount()) / numSymPoints;

        placeIndividualMexes(spawnMaskWater, StrictMath.min(numMexesLeft, 10), mexSpacing);
    }

    private void spacePlacedMexes(BooleanMask spawnMask, int mexSpacing, int previousMexCount) {
        map.getMexes().stream().skip(previousMexCount).filter(mex -> spawnMask.inTeam(mex.getPosition(), false))
                .forEach(mex -> spawnMask.fillCircle(mex.getPosition(), mexSpacing, false));
    }

    public void placeBaseMexes(BooleanMask spawnMask) {
        int numBaseMexes = (random.nextInt(3) + 3);
        for (int i = 0; i < map.getSpawnCount(); i += spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints()) {
            Spawn spawn = map.getSpawn(i);
            BooleanMask baseMexes = new BooleanMask(spawnMask.getSize(), random.nextLong(), spawnMask.getSymmetrySettings());
            baseMexes.fillCircle(spawn.getPosition(), 15, true).fillCircle(spawn.getPosition(), 5, false).multiply(spawnMask);
            placeIndividualMexes(baseMexes, numBaseMexes, 10);
        }
    }

    public void placeMexExpansions(BooleanMask spawnMask, int possibleExpMexCount, int mexSpacing) {
        Vector2 expLocation;
        int expMexCount;
        int expMexCountLeft = possibleExpMexCount;
        int expMexSpacing = 10;
        int expSize = 10;
        int expSpacing = (int) (map.getSize() / 4 * StrictMath.min(StrictMath.max(8f / possibleExpMexCount, .75f), 1.75f));

        BooleanMask expansionSpawnMask = new BooleanMask(spawnMask.getSize(), random.nextLong(), spawnMask.getSymmetrySettings());
        expansionSpawnMask.invert().fillCenter(96, false).fillEdge(32, false).multiply(spawnMask);

        map.getSpawns().stream().filter(spawn -> expansionSpawnMask.inTeam(spawn.getPosition(), false)).forEach(spawn -> expansionSpawnMask.fillCircle(spawn.getPosition(), map.getSize() / 6f, false));

        expMexCount = StrictMath.min((random.nextInt(2) + 3), expMexCountLeft);

        List<Vector2> expansionLocations = expansionSpawnMask.getRandomCoordinates(expSpacing);

        while (expMexCountLeft > expMexCount) {
            if (expansionLocations.size() == 0) {
                break;
            }

            expLocation = expansionLocations.remove(0);

            while (!isMexExpValid(expLocation, expSize, spawnMask)) {
                if (expansionLocations.size() == 0) {
                    expLocation = null;
                    break;
                }
                expLocation = expansionLocations.remove(0);
            }

            if (expLocation == null) {
                break;
            }

            BooleanMask expansion = new BooleanMask(spawnMask.getSize(), random.nextLong(), spawnMask.getSymmetrySettings());
            expansion.fillCircle(expLocation, expSize, true);
            expansion.multiply(spawnMask);

            int expID = map.getLargeExpansionMarkerCount() / spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();
            if (expMexCount >= 3) {
                map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", expID), expLocation, null));
                List<Vector2> symmetryPoints = expansionSpawnMask.getSymmetryPoints(expLocation, SymmetryType.SPAWN);
                symmetryPoints.forEach(Vector2::roundToNearestHalfPoint);
                symmetryPoints.forEach(symmetryPoint -> map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d sym %d", expID, symmetryPoints.indexOf(symmetryPoint)), symmetryPoint, null)));
            } else {
                map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d", expID), expLocation, null));
                List<Vector2> symmetryPoints = expansionSpawnMask.getSymmetryPoints(expLocation, SymmetryType.SPAWN);
                symmetryPoints.forEach(Vector2::roundToNearestHalfPoint);
                symmetryPoints.forEach(symmetryPoint -> map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d sym %d", expID, symmetryPoints.indexOf(symmetryPoint)), symmetryPoint, null)));
            }

            placeIndividualMexes(expansion, expMexCount, expMexSpacing);
            spawnMask.fillCircle(expLocation, mexSpacing * 3f * expMexCount / 4f, false);
            expMexCountLeft -= expMexCount;
        }
    }

    public void placeIndividualMexes(BooleanMask spawnMask, int numMexes, int mexSpacing) {
        if (numMexes > 0) {
            List<Vector2> mexLocations = spawnMask.getRandomCoordinates(mexSpacing);
            mexLocations.stream().limit(numMexes).forEachOrdered(location -> {
                int mexID = map.getMexCount() / spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();
                Marker mex = new Marker(String.format("Mex %d", mexID), new Vector3(location.roundToNearestHalfPoint()));
                map.addMex(mex);
                List<Vector2> symmetryPoints = spawnMask.getSymmetryPoints(mex.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(Vector2::roundToNearestHalfPoint);
                symmetryPoints.forEach(symmetryPoint -> map.addMex(new Marker(String.format("Mex %d sym %d", mexID, symmetryPoints.indexOf(symmetryPoint)), new Vector3(symmetryPoint))));
            });
        }
    }

    private boolean isMexExpValid(Vector2 location, float size, BooleanMask spawnMask) {
        float count = 0;

        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                Vector2 loc = new Vector2(location).add(dx - size / 2, dy - size / 2);
                if (spawnMask.inBounds(loc)) {
                    if (spawnMask.get(loc)) {
                        ++count;
                    }
                }
            }
        }
        return count / (size * size) > 0.5;
    }
}
