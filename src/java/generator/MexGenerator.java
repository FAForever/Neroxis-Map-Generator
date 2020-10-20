package generator;

import map.AIMarker;
import map.BinaryMask;
import map.SCMap;
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

    public void generateMexes(BinaryMask spawnable, BinaryMask spawnablePlateau, BinaryMask spawnableWater) {
        map.getMexes().clear();
        BinaryMask spawnableLand = new BinaryMask(spawnable, random.nextLong());
        spawnable.fillHalf(false);
        spawnableWater.fillHalf(false);
        int mexSpawnDistance = 32;
        BinaryMask spawnableNoSpawns = new BinaryMask(spawnable, random.nextLong());
        int spawnCount = map.getSpawnCount();
        int totalMexCount = map.getMexCountInit();
        int numBaseMexes = (random.nextInt(2) + 3) * 2;
        int numNearMexes = (random.nextInt(2) + 5 - numBaseMexes / 2) * 2;
        int iMex = 0;
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
                map.addMex(new Vector3f(location));
                map.addMex(new Vector3f(symLocation));
                baseMexes.fillCircle(location, 10, false);
                spawnable.fillCircle(location, 10, false);
                iMex += 2;
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
                map.addMex(new Vector3f(location));
                map.addMex(new Vector3f(symLocation));
                nearMexes.fillCircle(location, 10, false);
                spawnable.fillCircle(location, 10, false);
                iMex += 2;
            }
        }
        for (int i = 0; i < map.getSpawnCount(); i += 2) {
            spawnable.fillCircle(map.getSpawn(i + 1).getPosition(), 24, false);
            spawnableNoSpawns.fillCircle(map.getSpawn(i + 1).getPosition(), mexSpawnDistance, false);
        }

        int numMexesLeft;
        int actualExpMexCount;
        int baseMexCount = iMex;
        int nonBaseMexCount = totalMexCount - baseMexCount;

        for (int i = 0; i < baseMexCount; i++) {
            spawnable.fillCircle(map.getMex(i), mexSpacing, false);
        }

        if (nonBaseMexCount / 2 > 10) {
            int possibleExpMexCount = (random.nextInt(nonBaseMexCount / 2 / spawnCount) + nonBaseMexCount / 2 / spawnCount) * 2;
            actualExpMexCount = generateMexExpansions(spawnable, baseMexCount, possibleExpMexCount);
            numMexesLeft = nonBaseMexCount - actualExpMexCount;
        } else {
            numMexesLeft = nonBaseMexCount;
        }

        spawnableNoSpawns.intersect(spawnable);
        spawnablePlateau.intersect(spawnableNoSpawns);
        spawnableLand.intersect(spawnableNoSpawns).minus(spawnablePlateau);

        float plateauDensity = (float) spawnablePlateau.getCount() / spawnableNoSpawns.getCount();
        int plateauMexCount = (int) (plateauDensity * numMexesLeft / 2) * 2;

        List<Vector2f> plateauLocations = new ArrayList<>(spawnablePlateau.getRandomCoordinates(mexSpacing));
        for (int i = 0; i < plateauMexCount; i += 2) {
            if (plateauLocations.size() == 0) {
                break;
            }

            Vector2f mexLocation = plateauLocations.remove(random.nextInt(plateauLocations.size())).add(.5f, .5f);
            Vector2f mexSymLocation = spawnablePlateau.getSymmetryPoint(mexLocation);
            numMexesLeft -= 2;

            map.addMex(new Vector3f(mexLocation));
            map.addMex(new Vector3f(mexSymLocation));
        }

        int numLandMexes = numMexesLeft;
        List<Vector2f> landLocations = new ArrayList<>(spawnableLand.getRandomCoordinates(mexSpacing));
        for (int i = 0; i < numLandMexes; i += 2) {
            if (landLocations.size() == 0) {
                break;
            }

            Vector2f mexLocation = landLocations.remove(random.nextInt(landLocations.size())).add(.5f, .5f);
            Vector2f mexSymLocation = spawnableLand.getSymmetryPoint(mexLocation);
            numMexesLeft -= 2;

            map.addMex(new Vector3f(mexLocation));
            map.addMex(new Vector3f(mexSymLocation));
        }

        for (int i = 0; i < map.getMexCount(); i += 1) {
            spawnable.fillCircle(map.getMex(i), mexSpacing, false);
        }

        int numNearSpawnMexes = numMexesLeft;
        List<Vector2f> nearSpawnLocations = new ArrayList<>(spawnable.getRandomCoordinates(mexSpacing));
        for (int i = 0; i < numNearSpawnMexes; i += 2) {
            if (nearSpawnLocations.size() == 0) {
                break;
            }

            Vector2f mexLocation = nearSpawnLocations.remove(random.nextInt(nearSpawnLocations.size())).add(.5f, .5f);
            Vector2f mexSymLocation = spawnable.getSymmetryPoint(mexLocation);
            numMexesLeft -= 2;

            map.addMex(new Vector3f(mexLocation));
            map.addMex(new Vector3f(mexSymLocation));
        }

        List<Vector2f> waterLocations = new ArrayList<>(spawnableWater.getRandomCoordinates(mexSpacing));
        for (int i = 0; i < StrictMath.min(numMexesLeft, 20); i += 2) {
            if (waterLocations.size() == 0) {
                break;
            }

            Vector2f mexLocation = waterLocations.remove(random.nextInt(waterLocations.size())).add(.5f, .5f);
            Vector2f mexSymLocation = spawnableWater.getSymmetryPoint(mexLocation);

            map.addMex(new Vector3f(mexLocation));
            map.addMex(new Vector3f(mexSymLocation));
        }
    }

    public int generateMexExpansions(BinaryMask spawnable, int baseMexCount, int possibleExpMexCount) {
        Vector2f expLocation;
        Vector2f mexLocation;
        Vector2f mexSymLocation;
        int actualExpMexCount = possibleExpMexCount;
        int expMexCount;
        int expMexCountLeft = possibleExpMexCount;
        int iMex = baseMexCount;
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
                actualExpMexCount = possibleExpMexCount - expMexCountLeft;
                break;
            }

            expansion.clear();
            expansion.fillRect((int) expLocation.x - expSize, (int) expLocation.y - expSize, expSize * 2, expSize * 2, true);
            expansion.intersect(spawnable);

            expMexCount = StrictMath.min((random.nextInt(3) + 2) * 2, expMexCountLeft);
            if (expMexCount >= 6) {
                map.addLargeExpansionMarker(new AIMarker(map.getLargeExpansionMarkerCount(), expLocation, null));
                map.addLargeExpansionMarker(new AIMarker(map.getLargeExpansionMarkerCount(), expansionSpawnable.getSymmetryPoint(expLocation), null));
            } else {
                map.addExpansionMarker(new AIMarker(map.getExpansionMarkerCount(), expLocation, null));
                map.addExpansionMarker(new AIMarker(map.getExpansionMarkerCount(), expansionSpawnable.getSymmetryPoint(expLocation), null));
            }


            List<Vector2f> expMexLocations = new ArrayList<>(expansion.getRandomCoordinates(expMexSpacing));
            for (int i = iMex; i < iMex + expMexCount; i += 2) {
                if (expMexLocations.size() == 0) {
                    expMexCount -= i - iMex;
                    break;
                }
                mexLocation = expMexLocations.remove(random.nextInt(expMexLocations.size())).add(.5f, .5f);
                mexSymLocation = expansion.getSymmetryPoint(mexLocation);

                map.addMex(new Vector3f(mexLocation));
                map.addMex(new Vector3f(mexSymLocation));

                spawnable.fillCircle(mexLocation, mexSpacing, false);
                spawnable.fillCircle(mexSymLocation, mexSpacing, false);
            }

            iMex += expMexCount;
            expMexCountLeft -= expMexCount;
        }
        return actualExpMexCount;
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
            map.getMexes().set(i, placeOnHeightmap(map, map.getMex(i)));
        }
    }
}
