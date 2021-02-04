package neroxis.generator;

import neroxis.map.*;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public strictfp class MexGenerator {
    private final SCMap map;
    private final Random random;
    private final int mexSpacing;

    public MexGenerator(SCMap map, long seed, int mexSpacing) {
        this.map = map;
        this.mexSpacing = mexSpacing;
        random = new Random(seed);
    }

    public void generateMexes(BinaryMask spawnMask, BinaryMask spawnMaskWater) {
        map.getMexes().clear();
        spawnMask.limitToSymmetryRegion();
        spawnMaskWater.limitToSymmetryRegion();
        int numSymPoints = spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();

        int previousMexCount;
        generateBaseMexes(spawnMask);
        int numMexesLeft = (map.getMexCountInit() - map.getMexCount()) / numSymPoints;
        map.getSpawns().forEach(spawn -> spawnMask.fillCircle(spawn.getPosition(), 24, false));

        previousMexCount = map.getMexCount();
        if (numMexesLeft > 8 && numMexesLeft > map.getSpawnCount()) {
            int possibleExpMexCount = (random.nextInt(numMexesLeft / 2) + numMexesLeft / 2);
            generateMexExpansions(spawnMask, possibleExpMexCount);

            map.getMexes().stream().skip(previousMexCount)
                    .forEach(mex -> spawnMask.fillCircle(mex.getPosition(), mexSpacing, false));
            numMexesLeft = map.getMexCountInit() - map.getMexCount();
            previousMexCount = map.getMexCount();
        }

        int numPlayerMexes = numMexesLeft / map.getSpawnCount() / numSymPoints;
        for (Spawn spawn : map.getSpawns()) {
            BinaryMask playerSpawnMask = new BinaryMask(spawnMask.getSize(), 0L, spawnMask.getSymmetrySettings());
            playerSpawnMask.fillCircle(spawn.getPosition(), map.getSize(), true).intersect(spawnMask);
            generateIndividualMexes(playerSpawnMask, numPlayerMexes);
            map.getMexes().stream().skip(previousMexCount)
                    .forEach(mex -> spawnMask.fillCircle(mex.getPosition(), mexSpacing, false));
            previousMexCount = map.getMexCount();
        }

        numMexesLeft = (map.getMexCountInit() - map.getMexCount()) / numSymPoints;
        generateIndividualMexes(spawnMask, numMexesLeft);
        map.getMexes().stream().skip(previousMexCount)
                .forEach(mex -> spawnMask.fillCircle(mex.getPosition(), mexSpacing, false));

        numMexesLeft = (map.getMexCountInit() - map.getMexCount()) / numSymPoints;

        generateIndividualMexes(spawnMaskWater, StrictMath.min(numMexesLeft, 10));
    }

    public void generateBaseMexes(BinaryMask spawnMask) {
        int numBaseMexes = (random.nextInt(2) + 3);
        for (int i = 0; i < map.getSpawnCount(); i += spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints()) {
            Spawn spawn = map.getSpawn(i);
            BinaryMask baseMexes = new BinaryMask(spawnMask.getSize(), random.nextLong(), spawnMask.getSymmetrySettings());
            baseMexes.fillCircle(spawn.getPosition(), 15, true).fillCircle(spawn.getPosition(), 5, false).intersect(spawnMask);
            generateIndividualMexes(baseMexes, numBaseMexes, 10);
        }
    }

    public void generateMexExpansions(BinaryMask spawnMask, int possibleExpMexCount) {
        Vector2f expLocation;
        int expMexCount;
        int expMexCountLeft = possibleExpMexCount;
        int expMexSpacing = 10;
        int expSize = 10;

        BinaryMask expansionSpawnMask = new BinaryMask(spawnMask.getSize(), random.nextLong(), spawnMask.getSymmetrySettings());
        expansionSpawnMask.fillCircle(map.getSize() / 2f, map.getSize() / 2f, map.getSize() * .45f, true)
                .fillCenter(96, false).fillEdge(32, false).intersect(spawnMask);

        map.getSpawns().forEach(spawn -> expansionSpawnMask.fillCircle(spawn.getPosition(), map.getSize() / 3f, false));

        expMexCount = StrictMath.min((random.nextInt(2) + 3), expMexCountLeft);
        int expSpacing = map.getSize() / 4;

        LinkedList<Vector2f> expansionLocations = expansionSpawnMask.getRandomCoordinates(expSpacing);

        while (expMexCountLeft > expMexCount) {
            if (expansionLocations.size() == 0) {
                break;
            }

            expLocation = expansionLocations.remove(random.nextInt(expansionLocations.size()));

            while (!isMexExpValid(expLocation, expSize, spawnMask)) {
                if (expansionLocations.size() == 0) {
                    expLocation = null;
                    break;
                }
                expLocation = expansionLocations.remove(random.nextInt(expansionLocations.size()));
            }

            if (expLocation == null) {
                break;
            }

            BinaryMask expansion = new BinaryMask(spawnMask.getSize(), random.nextLong(), spawnMask.getSymmetrySettings());
            expansion.fillCircle(expLocation, expSize, true);
            expansion.intersect(spawnMask);

            if (expMexCount >= 3) {
                map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), expLocation, null));
                ArrayList<Vector2f> symmetryPoints = expansionSpawnMask.getSymmetryPoints(expLocation, SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> symmetryPoint.roundToNearestHalfPoint());
                symmetryPoints.forEach(symmetryPoint -> map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), symmetryPoint, null)));
            } else {
                map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d", map.getExpansionMarkerCount()), expLocation, null));
                ArrayList<Vector2f> symmetryPoints = expansionSpawnMask.getSymmetryPoints(expLocation, SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> symmetryPoint.roundToNearestHalfPoint());
                symmetryPoints.forEach(symmetryPoint -> map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d", map.getExpansionMarkerCount()), symmetryPoint, null)));
            }

            generateIndividualMexes(expansion, expMexCount, expMexSpacing);
            spawnMask.fillCircle(expLocation, mexSpacing * 2f * expMexCount / 4f, false);
            ArrayList<Vector2f> symmetryPoints = spawnMask.getSymmetryPoints(expLocation, SymmetryType.SPAWN);
            symmetryPoints.forEach(symmetryPoint -> symmetryPoint.roundToNearestHalfPoint());
            symmetryPoints.forEach(symmetryPoint -> spawnMask.fillCircle(symmetryPoint, mexSpacing * 2f * expMexCount / 4f, false));
            expMexCountLeft -= expMexCount;
        }
    }

    public void generateIndividualMexes(BinaryMask spawnMask, int numMexes) {
        generateIndividualMexes(spawnMask, numMexes, mexSpacing);
    }

    public void generateIndividualMexes(BinaryMask spawnMask, int numMexes, int mexSpacing) {
        if (numMexes > 0) {
            LinkedList<Vector2f> mexLocations = spawnMask.getRandomCoordinates(mexSpacing);
            mexLocations.stream().limit(numMexes).forEachOrdered(location -> {
                int mexId = map.getMexCount() / spawnMask.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();
                Marker mex = new Marker(String.format("Mex %d", mexId), new Vector3f(location.add(.5f, .5f)));
                map.addMex(mex);
                ArrayList<Vector2f> symmetryPoints = spawnMask.getSymmetryPoints(mex.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> symmetryPoint.roundToNearestHalfPoint());
                symmetryPoints.forEach(symmetryPoint -> map.addMex(new Marker(String.format("Mex %d sym %d", mexId, symmetryPoints.indexOf(symmetryPoint)), new Vector3f(symmetryPoint))));
            });
        }
    }

    private boolean isMexExpValid(Vector2f location, float size, BinaryMask spawnMask) {
        float count = 0;

        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                Vector2f loc = new Vector2f(location).add(dx - size / 2, dy - size / 2);
                if (spawnMask.inBounds(loc)) {
                    if (spawnMask.getValueAt(loc)) {
                        ++count;
                    }
                }
            }
        }
        return count / (size * size) > 0.5;
    }
}
