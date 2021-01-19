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

    public void generateMexes(BinaryMask spawnable, BinaryMask spawnableWater) {
        map.getMexes().clear();
        spawnable.limitToSymmetryRegion();
        spawnableWater.limitToSymmetryRegion();
        int numSymPoints = spawnable.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();

        int previousMexCount;
        generateBaseMexes(spawnable);
        int numMexesLeft = (map.getMexCountInit() - map.getMexCount()) / numSymPoints;
        map.getSpawns().forEach(spawn -> spawnable.fillCircle(spawn.getPosition(), 24, false));

        previousMexCount = map.getMexCount();
        if (numMexesLeft > 8 && numMexesLeft > map.getSpawnCount()) {
            int possibleExpMexCount = (random.nextInt(numMexesLeft / 2) + numMexesLeft / 2);
            generateMexExpansions(spawnable, possibleExpMexCount);

            map.getMexes().stream().skip(previousMexCount)
                    .forEach(mex -> spawnable.fillCircle(mex.getPosition(), mexSpacing, false));
            numMexesLeft = map.getMexCountInit() - map.getMexCount();
            previousMexCount = map.getMexCount();
        }

        int numPlayerMexes = numMexesLeft / map.getSpawnCount() / numSymPoints;
        map.getSpawns().forEach(spawn -> {
        });
        for (Spawn spawn : map.getSpawns()) {
            BinaryMask playerSpawnable = new BinaryMask(spawnable.getSize(), 0L, spawnable.getSymmetrySettings());
            playerSpawnable.fillCircle(spawn.getPosition(), map.getSize(), true).intersect(spawnable);
            generateIndividualMexes(playerSpawnable, numPlayerMexes);
            map.getMexes().stream().skip(previousMexCount)
                    .forEach(mex -> spawnable.fillCircle(mex.getPosition(), mexSpacing, false));
            previousMexCount = map.getMexCount();
        }

        numMexesLeft = (map.getMexCountInit() - map.getMexCount()) / numSymPoints;
        generateIndividualMexes(spawnable, numMexesLeft);
        map.getMexes().stream().skip(previousMexCount)
                .forEach(mex -> spawnable.fillCircle(mex.getPosition(), mexSpacing, false));

        numMexesLeft = (map.getMexCountInit() - map.getMexCount()) / numSymPoints;

        generateIndividualMexes(spawnableWater, StrictMath.min(numMexesLeft, 10));
    }

    public void generateBaseMexes(BinaryMask spawnable) {
        int numBaseMexes = (random.nextInt(2) + 3);
        map.getSpawns().forEach(spawn -> {
            BinaryMask baseMexes = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
            baseMexes.fillCircle(spawn.getPosition(), 15, true).fillCircle(spawn.getPosition(), 5, false).intersect(spawnable);
            generateIndividualMexes(baseMexes, numBaseMexes, 10);
        });
    }

    public void generateMexExpansions(BinaryMask spawnable, int possibleExpMexCount) {
        Vector2f expLocation;
        int expMexCount;
        int expMexCountLeft = possibleExpMexCount;
        int expMexSpacing = 10;
        int expSize = 10;

        BinaryMask expansionSpawnable = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
        expansionSpawnable.fillCircle(map.getSize() / 2f, map.getSize() / 2f, map.getSize() * .45f, true)
                .fillCenter(96, false).fillEdge(32, false).intersect(spawnable);

        map.getSpawns().forEach(spawn -> expansionSpawnable.fillCircle(spawn.getPosition(), map.getSize() / 4f, false));

        expMexCount = StrictMath.min((random.nextInt(2) + 3), expMexCountLeft);
        int expSpacing = map.getSize() / 4;

        LinkedList<Vector2f> expansionLocations = expansionSpawnable.getRandomCoordinates(expSpacing);

        while (expMexCountLeft > expMexCount) {
            if (expansionLocations.size() == 0) {
                break;
            }

            expLocation = expansionLocations.remove(random.nextInt(expansionLocations.size()));

            while (!isMexExpValid(expLocation, expSize, .5f, spawnable)) {
                if (expansionLocations.size() == 0) {
                    expLocation = null;
                    break;
                }
                expLocation = expansionLocations.remove(random.nextInt(expansionLocations.size()));
            }

            if (expLocation == null) {
                break;
            }

            BinaryMask expansion = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
            expansion.fillCircle(expLocation, expSize, true);
            expansion.intersect(spawnable);

            if (expMexCount >= 3) {
                map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), expLocation, null));
                ArrayList<SymmetryPoint> symmetryPoints = expansionSpawnable.getSymmetryPoints(expLocation, SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> symmetryPoint.getLocation().roundToNearestHalfPoint());
                symmetryPoints.forEach(symmetryPoint -> map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), symmetryPoint.getLocation(), null)));
            } else {
                map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d", map.getExpansionMarkerCount()), expLocation, null));
                ArrayList<SymmetryPoint> symmetryPoints = expansionSpawnable.getSymmetryPoints(expLocation, SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> symmetryPoint.getLocation().roundToNearestHalfPoint());
                symmetryPoints.forEach(symmetryPoint -> map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d", map.getExpansionMarkerCount()), symmetryPoint.getLocation(), null)));
            }

            generateIndividualMexes(expansion, expMexCount, expMexSpacing);
            spawnable.fillCircle(expLocation, mexSpacing * 2f * expMexCount / 4f, false);
            ArrayList<SymmetryPoint> symmetryPoints = spawnable.getSymmetryPoints(expLocation, SymmetryType.SPAWN);
            symmetryPoints.forEach(symmetryPoint -> symmetryPoint.getLocation().roundToNearestHalfPoint());
            symmetryPoints.forEach(symmetryPoint -> spawnable.fillCircle(symmetryPoint.getLocation(), mexSpacing * 2f * expMexCount / 4f, false));
            expMexCountLeft -= expMexCount;
        }
    }

    public void generateIndividualMexes(BinaryMask spawnable, int numMexes) {
        generateIndividualMexes(spawnable, numMexes, mexSpacing);
    }

    public void generateIndividualMexes(BinaryMask spawnable, int numMexes, int mexSpacing) {
        if (numMexes > 0) {
            LinkedList<Vector2f> mexLocations = spawnable.getRandomCoordinates(mexSpacing);
            mexLocations.stream().limit(numMexes).forEachOrdered(location -> {
                int mexId = map.getMexCount() / spawnable.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();
                Marker mex = new Marker(String.format("Mex %d", mexId), new Vector3f(location.add(.5f, .5f)));
                map.addMex(mex);
                ArrayList<SymmetryPoint> symmetryPoints = spawnable.getSymmetryPoints(mex.getPosition(), SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> symmetryPoint.getLocation().roundToNearestHalfPoint());
                symmetryPoints.forEach(symmetryPoint -> map.addMex(new Marker(String.format("Mex %d sym %d", mexId, symmetryPoints.indexOf(symmetryPoint)), new Vector3f(symmetryPoint.getLocation()))));
            });
        }
    }

    private boolean isMexExpValid(Vector2f location, float size, float density, BinaryMask spawnable) {
        boolean valid = true;
        float count = 0;

        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                Vector2f loc = new Vector2f(location).add(dx - size / 2, dy - size / 2);
                if (spawnable.inBounds(loc)) {
                    if (spawnable.getValueAt(loc)) {
                        count++;
                    }
                }
            }
        }
        if (count / (size * size) < density) {
            valid = false;
        }
        return valid;
    }
}
