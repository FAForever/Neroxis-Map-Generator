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

        generateBaseMexes(spawnable);

        for (Spawn spawn : map.getSpawns()) {
            spawnable.fillCircle(spawn.getPosition(), 24, false);
        }

        int nonBaseMexCount = map.getMexCountInit() - map.getMexCount();

        for (Mex mex : map.getMexes()) {
            spawnable.fillCircle(mex.getPosition(), mexSpacing, false);
        }

        if (nonBaseMexCount / 2 > 8) {
            int possibleExpMexCount = (random.nextInt(nonBaseMexCount / map.getSpawnCount() / 2) + nonBaseMexCount / map.getSpawnCount() / 2) * 2;
            generateMexExpansions(spawnable, possibleExpMexCount);
        }

        generateIndividualMexes(spawnable, map.getMexCountInit() - map.getMexCount());
        generateIndividualMexes(spawnableWater, StrictMath.min(map.getMexCountInit() - map.getMexCount(), 20));
    }

    public void generateBaseMexes(BinaryMask spawnable) {
        int numBaseMexes = (random.nextInt(2) + 3) * 2;
        int numNearMexes = (random.nextInt(2) + 5 - numBaseMexes / 2) * 2;
        for (int i = 0; i < map.getSpawnCount(); i += 2) {
            BinaryMask baseMexes = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
            baseMexes.fillCircle(map.getSpawn(i + 1).getPosition(), 15, true).fillCircle(map.getSpawn(i + 1).getPosition(), 5, false).intersect(spawnable);
            for (int j = 0; j < numBaseMexes; j += 2) {
                Vector2f location = baseMexes.getRandomPosition();
                if (location == null) {
                    break;
                }
                location.add(.5f, .5f);
                Vector2f symLocation = baseMexes.getSymmetryPoint(location);
                map.addMex(new Mex(String.format("Mex %d", map.getMexCount()), new Vector3f(location)));
                map.addMex(new Mex(String.format("Mex %d", map.getMexCount()), new Vector3f(symLocation)));
                baseMexes.fillCircle(location, 10, false);
                spawnable.fillCircle(location, 10, false);
            }
            BinaryMask nearMexes = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
            nearMexes.fillCircle(map.getSpawn(i + 1).getPosition(), spawnSize * 3, true).fillCircle(map.getSpawn(i + 1).getPosition(), spawnSize / 2f, false).intersect(spawnable);
            for (int j = 0; j < map.getSpawnCount(); j += 2) {
                nearMexes.fillCircle(map.getSpawn(j + 1).getPosition(), spawnSize, false);
            }
            for (int j = 0; j < numNearMexes; j += 2) {
                Vector2f location = nearMexes.getRandomPosition();
                if (location == null) {
                    break;
                }
                location.add(.5f, .5f);
                Vector2f symLocation = nearMexes.getSymmetryPoint(location);
                map.addMex(new Mex(String.format("Mex %d", map.getMexCount()), new Vector3f(location)));
                map.addMex(new Mex(String.format("Mex %d", map.getMexCount()), new Vector3f(symLocation)));
                nearMexes.fillCircle(location, 10, false);
                spawnable.fillCircle(location, 10, false);
            }
        }
    }

    public void generateMexExpansions(BinaryMask spawnable, int possibleExpMexCount) {
        Vector2f expLocation;
        Vector2f location;
        Vector2f symLocation;
        int expMexCount;
        int expMexCountLeft = possibleExpMexCount;
        int expMexSpacing = 10;
        int expSize = 10;
        int expSpacing = 96;

        BinaryMask expansionSpawnable = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());
        BinaryMask expansion = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetrySettings());

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

            expansion.clear();
            expansion.fillRect((int) expLocation.x - expSize, (int) expLocation.y - expSize, expSize * 2, expSize * 2, true);
            expansion.intersect(spawnable);

            expMexCount = StrictMath.min((random.nextInt(3) + 2) * 2, expMexCountLeft);
            if (expMexCount >= 6) {
                map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), expLocation, null));
                map.addLargeExpansionMarker(new AIMarker(String.format("Large Expansion Area %d", map.getLargeExpansionMarkerCount()), expansionSpawnable.getSymmetryPoint(expLocation), null));
            } else {
                map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d", map.getExpansionMarkerCount()), expLocation, null));
                map.addExpansionMarker(new AIMarker(String.format("Expansion Area %d", map.getExpansionMarkerCount()), expansionSpawnable.getSymmetryPoint(expLocation), null));
            }


            List<Vector2f> expMexLocations = new ArrayList<>(expansion.getRandomCoordinates(expMexSpacing));
            for (int i = 0; i < expMexCount; i += 2) {
                if (expMexLocations.size() == 0) {
                    expMexCount -= i;
                    break;
                }
                location = expMexLocations.remove(random.nextInt(expMexLocations.size())).add(.5f, .5f);
                symLocation = expansion.getSymmetryPoint(location);

                map.addMex(new Mex(String.format("Mex %d", map.getMexCount()), new Vector3f(location)));
                map.addMex(new Mex(String.format("Mex %d", map.getMexCount()), new Vector3f(symLocation)));

                spawnable.fillCircle(location, mexSpacing * 1.5f, false);
                spawnable.fillCircle(symLocation, mexSpacing * 1.5f, false);
            }
            expMexCountLeft -= expMexCount;
        }
    }

    public void generateIndividualMexes(BinaryMask spawnable, int numMexes) {
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

        for (int dx = 0; dx < size / 2; dx++) {
            for (int dy = 0; dy < size / 2; dy++) {
                if (spawnable.get(StrictMath.min((int) location.x + dx, map.getSize() - 1), StrictMath.min((int) location.y + dy, map.getSize() - 1))) {
                    count++;
                }
                if (spawnable.get(StrictMath.min((int) location.x + dx, map.getSize() - 1), StrictMath.max((int) location.y - dy, 0))) {
                    count++;
                }
                if (spawnable.get(StrictMath.max((int) location.x - dx, 0), StrictMath.min((int) location.y + dy, map.getSize() - 1))) {
                    count++;
                }
                if (spawnable.get(StrictMath.max((int) location.x - dx, 0), StrictMath.max((int) location.y - dy, 0))) {
                    count++;
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
