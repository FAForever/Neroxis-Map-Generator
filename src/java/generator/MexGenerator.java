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
        spawnable.fillHalf(false);
        spawnableWater.fillHalf(false);

        int previousMexCount = map.getMexCount();
        generateBaseMexes(spawnable);
        int numMexesLeft = map.getMexCountInit() - map.getMexCount();
        for (Spawn spawn : map.getSpawns()) {
            spawnable.fillCircle(spawn.getPosition(), 24, false);
        }
        for (Mex mex : map.getMexes().subList(previousMexCount, map.getMexCount())) {
            spawnable.fillCircle(mex.getPosition(), mexSpacing, false);
        }

        previousMexCount = map.getMexCount();
        if (numMexesLeft / 2 > 8) {
            int possibleExpMexCount = (random.nextInt(numMexesLeft / map.getSpawnCount() / 2) + numMexesLeft / map.getSpawnCount() / 2) * 2;
            generateMexExpansions(spawnable, possibleExpMexCount);

            for (Mex mex : map.getMexes().subList(previousMexCount, map.getMexCount())) {
                spawnable.fillCircle(mex.getPosition(), mexSpacing, false);
            }
            numMexesLeft = map.getMexCountInit() - map.getMexCount();
            previousMexCount = map.getMexCount();
        }

        int numPlayerMexes = numMexesLeft / map.getSpawnCount() * 2;
        for (int i = 0; i < map.getSpawnCount(); i += 2) {
            BinaryMask playerSpawnable = new BinaryMask(spawnable.getSize(), 0L, spawnable.getSymmetrySettings());
            playerSpawnable.fillCircle(map.getSpawn(i + 1).getPosition(), map.getSize() / 2f, true).intersect(spawnable);
            generateIndividualMexes(playerSpawnable, numPlayerMexes);
            for (Mex mex : map.getMexes().subList(previousMexCount, map.getMexCount())) {
                spawnable.fillCircle(mex.getPosition(), mexSpacing, false);
            }
            previousMexCount = map.getMexCount();
        }

        numMexesLeft = map.getMexCountInit() - map.getMexCount();
        generateIndividualMexes(spawnable, numMexesLeft);
        for (Mex mex : map.getMexes().subList(previousMexCount, map.getMexCount())) {
            spawnable.fillCircle(mex.getPosition(), mexSpacing / 2f, false);
        }
        numMexesLeft = map.getMexCountInit() - map.getMexCount();

        generateIndividualMexes(spawnableWater, StrictMath.min(numMexesLeft, 20));
    }

    public void generateBaseMexes(BinaryMask spawnable) {
        int numBaseMexes = (random.nextInt(2) + 3) * 2;
        int numNearMexes = (random.nextInt(2) + 5 - numBaseMexes / 2) * 2;
        for (int i = 0; i < map.getSpawnCount(); i += 2) {
            BinaryMask baseMexes = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
            baseMexes.fillCircle(map.getSpawn(i + 1).getPosition(), 15, true).fillCircle(map.getSpawn(i + 1).getPosition(), 5, false).intersect(spawnable);
            generateIndividualMexes(baseMexes, numBaseMexes, 10);

            BinaryMask nearMexes = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
            nearMexes.fillCircle(map.getSpawn(i + 1).getPosition(), spawnSize * 3, true).fillCircle(map.getSpawn(i + 1).getPosition(), spawnSize / 2f, false).intersect(spawnable);
            for (int j = 0; j < map.getSpawnCount(); j += 2) {
                nearMexes.fillCircle(map.getSpawn(j + 1).getPosition(), spawnSize, false);
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

            expMexCount = StrictMath.min((random.nextInt(3) + 2) * 2, expMexCountLeft);
            if (expMexCount >= 6) {
                map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), expLocation, null));
                map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), expansionSpawnable.getSymmetryPoint(expLocation), null));
            } else {
                map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d", map.getExpansionMarkerCount()), expLocation, null));
                map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d", map.getExpansionMarkerCount()), expansionSpawnable.getSymmetryPoint(expLocation), null));
            }

            generateIndividualMexes(expansion, expMexCount, expMexSpacing);
            spawnable.fillCircle(expLocation, mexSpacing * 2, false);
            spawnable.fillCircle(spawnable.getSymmetryPoint(expLocation), mexSpacing * 1.5f, false);
            expMexCountLeft -= expMexCount;
        }
    }

    public void generateIndividualMexes(BinaryMask spawnable, int numMexes) {
        generateIndividualMexes(spawnable, numMexes, mexSpacing);
    }

    public void generateIndividualMexes(BinaryMask spawnable, int numMexes, int mexSpacing) {
        List<Vector2f> mexLocations = new ArrayList<>(spawnable.getRandomCoordinates(mexSpacing));
        for (int i = 0; i < numMexes; i += 2) {
            if (mexLocations.size() == 0) {
                break;
            }

            Vector2f location = mexLocations.remove(random.nextInt(mexLocations.size())).add(.5f, .5f);
            Vector2f symLocation = spawnable.getSymmetryPoint(location);

            map.addMex(new Mex(String.format("Mex %d", map.getMexCount()), new Vector3f(location)));
            map.addMex(new Mex(String.format("Mex %d", map.getMexCount()), new Vector3f(symLocation)));
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
