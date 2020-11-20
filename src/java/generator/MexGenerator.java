package generator;

import map.*;
import util.Vector2f;
import util.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static util.Placement.placeOnHeightmap;

public strictfp class MexGenerator {
    private final SCMap map;
    private final Random random;
    private final int mexSpacing;
    private final int spawnSize;

    public MexGenerator(SCMap map, long seed, int spawnSize, int mexSpacing) {
        this.map = map;
        this.spawnSize = spawnSize;
        this.mexSpacing = mexSpacing;
        random = new Random(seed);
    }

    public void generateMexes(BinaryMask spawnable, BinaryMask spawnableWater) {
        map.getMexes().clear();
        spawnable.limitToSpawnRegion();
        spawnableWater.limitToSpawnRegion();
        int numSymPoints = spawnable.getSymmetryPoints(0, 0).size() + 1;

        int previousMexCount = map.getMexCount();
        generateBaseMexes(spawnable);
        int numMexesLeft = (map.getMexCountInit() - map.getMexCount()) / numSymPoints;
        for (Spawn spawn : map.getSpawns()) {
            spawnable.fillCircle(spawn.getPosition(), 24, false);
        }
        for (Mex mex : map.getMexes().subList(previousMexCount, map.getMexCount())) {
            spawnable.fillCircle(mex.getPosition(), mexSpacing, false);
        }

        previousMexCount = map.getMexCount();
        if (numMexesLeft > 8 && numMexesLeft > map.getSpawnCount() * numSymPoints) {
            int possibleExpMexCount = (random.nextInt(numMexesLeft / map.getSpawnCount() / numSymPoints) + numMexesLeft / map.getSpawnCount() / numSymPoints);
            generateMexExpansions(spawnable, possibleExpMexCount);

            for (Mex mex : map.getMexes().subList(previousMexCount, map.getMexCount())) {
                spawnable.fillCircle(mex.getPosition(), mexSpacing, false);
            }
            numMexesLeft = map.getMexCountInit() - map.getMexCount();
            previousMexCount = map.getMexCount();
        }

        int numPlayerMexes = StrictMath.min(numMexesLeft / map.getSpawnCount(), 5);
        for (int i = 0; i < map.getSpawnCount(); i += numSymPoints) {
            BinaryMask playerSpawnable = new BinaryMask(spawnable.getSize(), 0L, spawnable.getSymmetrySettings());
            playerSpawnable.fillCircle(map.getSpawn(i).getPosition(), map.getSize() / 2f, true).intersect(spawnable);
            generateIndividualMexes(playerSpawnable, numPlayerMexes);
            for (Mex mex : map.getMexes().subList(previousMexCount, map.getMexCount())) {
                spawnable.fillCircle(mex.getPosition(), mexSpacing, false);
            }
            previousMexCount = map.getMexCount();
        }

        numMexesLeft = (map.getMexCountInit() - map.getMexCount()) / numSymPoints;
        generateIndividualMexes(spawnable, numMexesLeft);
        for (Mex mex : map.getMexes().subList(previousMexCount, map.getMexCount())) {
            spawnable.fillCircle(mex.getPosition(), mexSpacing / 2f, false);
        }
        numMexesLeft = (map.getMexCountInit() - map.getMexCount()) / numSymPoints;

        generateIndividualMexes(spawnableWater, StrictMath.min(numMexesLeft, 20));
    }

    public void generateBaseMexes(BinaryMask spawnable) {
        int numBaseMexes = (random.nextInt(2) + 3);
        int numNearMexes = (random.nextInt(2) + 5 - numBaseMexes);
        int numSymPoints = spawnable.getSymmetryPoints(0, 0).size() + 1;
        for (int i = 0; i < map.getSpawnCount(); i += numSymPoints) {
            BinaryMask baseMexes = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
            baseMexes.fillCircle(map.getSpawn(i).getPosition(), 15, true).fillCircle(map.getSpawn(i).getPosition(), 5, false).intersect(spawnable);
            generateIndividualMexes(baseMexes, numBaseMexes, 10);

            BinaryMask nearMexes = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
            nearMexes.fillCircle(map.getSpawn(i).getPosition(), spawnSize * 3, true).fillCircle(map.getSpawn(i).getPosition(), spawnSize / 2f, false).intersect(spawnable);
            for (int j = 0; j < map.getSpawnCount(); j += numSymPoints) {
                nearMexes.fillCircle(map.getSpawn(j).getPosition(), spawnSize, false);
            }
            generateIndividualMexes(nearMexes, numNearMexes, 10);
        }
    }

    public void generateMexExpansions(BinaryMask spawnable, int possibleExpMexCount) {
        Vector2f expLocation;
        int expMexCount;
        int expMexCountLeft = possibleExpMexCount;
        int expMexSpacing = 10;
        int expSize = 10;
        int expSpacing = 96;

        BinaryMask expansionSpawnable = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());

        expansionSpawnable.fillCircle(map.getSize() / 2f, map.getSize() / 2f, map.getSize() / 2f, true).fillCenter(96, false).intersect(spawnable);

        for (int i = 0; i < map.getSpawnCount(); i++) {
            expansionSpawnable.fillCircle(map.getSpawn(i).getPosition(), map.getSize() / 4f, false);
        }

        List<Vector2f> expansionLocations = new ArrayList<>(expansionSpawnable.getRandomCoordinates(expSpacing));

        while (expMexCountLeft > 1) {
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

            expMexCount = StrictMath.min((random.nextInt(3) + 2), expMexCountLeft);
            if (expMexCount >= 3) {
                map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), expLocation, null));
                ArrayList<SymmetryPoint> symmetryPoints = expansionSpawnable.getSymmetryPoints(expLocation);
                symmetryPoints.forEach(symmetryPoint -> map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), symmetryPoint.getLocation(), null)));
            } else {
                map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d", map.getExpansionMarkerCount()), expLocation, null));
                ArrayList<SymmetryPoint> symmetryPoints = expansionSpawnable.getSymmetryPoints(expLocation);
                symmetryPoints.forEach(symmetryPoint -> map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d", map.getExpansionMarkerCount()), symmetryPoint.getLocation(), null)));
            }

            generateIndividualMexes(expansion, expMexCount, expMexSpacing);
            spawnable.fillCircle(expLocation, mexSpacing * 2, false);
            ArrayList<SymmetryPoint> symmetryPoints = spawnable.getSymmetryPoints(expLocation);
            symmetryPoints.forEach(symmetryPoint -> spawnable.fillCircle(symmetryPoint.getLocation(), mexSpacing * 2, false));
            expMexCountLeft -= expMexCount;
        }
    }

    public void generateIndividualMexes(BinaryMask spawnable, int numMexes) {
        generateIndividualMexes(spawnable, numMexes, mexSpacing);
    }

    public void generateIndividualMexes(BinaryMask spawnable, int numMexes, int mexSpacing) {
        List<Vector2f> mexLocations = new ArrayList<>(spawnable.getRandomCoordinates(mexSpacing));
        for (int i = 0; i < numMexes; i++) {
            if (mexLocations.size() == 0) {
                break;
            }
            int mexId = map.getMexCount() + 1;
            Vector2f location = mexLocations.remove(random.nextInt(mexLocations.size())).add(.5f, .5f);
            Mex mex = new Mex(String.format("Mex %d", mexId), new Vector3f(location));
            map.addMex(mex);
            ArrayList<SymmetryPoint> symmetryPoints = spawnable.getSymmetryPoints(mex.getPosition());
            symmetryPoints.forEach(symmetryPoint -> map.addMex(new Mex(String.format("sym %d Mex %d", symmetryPoints.indexOf(symmetryPoint), mexId), new Vector3f(symmetryPoint.getLocation()))));
        }
    }

    private boolean isMexExpValid(Vector2f location, float size, float density, BinaryMask spawnable) {
        boolean valid = true;
        float count = 0;

        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                Vector2f loc = new Vector2f(location).add(dx - size / 2, dy - size / 2);
                if (spawnable.inBounds(loc)) {
                    if (spawnable.get(loc)) {
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

    public void setMarkerHeights() {
        for (int i = 0; i < map.getMexCount(); i++) {
            map.getMex(i).setPosition(placeOnHeightmap(map, map.getMex(i).getPosition()));
        }
    }
}
