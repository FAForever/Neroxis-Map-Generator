package generator;

import map.BinaryMask;
import map.SCMap;
import map.Symmetry;
import util.Vector2f;
import util.Vector3f;

import java.util.Random;

import static util.Placement.placeOnHeightmap;

public strictfp class MarkerGenerator {
    private final SCMap map;
    private final Random random;
    private final int mexSpacing = 32;
    private final int spawnSize = 32;

    public MarkerGenerator(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }


    public BinaryMask[] generateSpawns(float separation, Symmetry symmetry, float plateauDensity) {
        BinaryMask spawnable = new BinaryMask(map.getSize() + 1, random.nextLong(), symmetry);
        BinaryMask spawnLandMask = new BinaryMask(map.getSize() + 1, random.nextLong(), spawnable.getSymmetryHierarchy());
        BinaryMask spawnPlateauMask = new BinaryMask(map.getSize() + 1, random.nextLong(), spawnable.getSymmetryHierarchy());

        if (map.getSpawns().length == 2 && (symmetry == Symmetry.POINT || symmetry == Symmetry.DIAG || symmetry == Symmetry.QUAD)) {
            spawnable.getSymmetryHierarchy().setSpawnSymmetry(Symmetry.POINT);
        }
        spawnable.fillHalf(true).fillCenter(map.getSize() * 7 / 16, false).trimEdge(map.getSize() / 16);
        Vector2f location = spawnable.getRandomPosition();
        Vector2f symLocation;
        for (int i = 0; i < map.getSpawns().length; i += 2) {
            if (location == null) {
                if (separation - 4 >= 20) {
                    return generateSpawns(separation - 8, symmetry, plateauDensity);
                } else {
                    return null;
                }
            }
            symLocation = spawnable.getSymmetryPoint(location);
            spawnable.fillCircle(location, separation, false);
            spawnable.fillCircle(symLocation, separation, false);

            spawnLandMask.fillCircle(location, spawnSize, true);
            spawnLandMask.fillCircle(symLocation, spawnSize, true);

            if (random.nextFloat() < plateauDensity) {
                boolean valid = true;
                for (int j = 0; j < i; j += 2) {
                    if (!spawnPlateauMask.get(map.getSpawns()[j]) && map.getSpawns()[j].getXZDistance(location) < spawnSize * 4) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    spawnPlateauMask.fillCircle(location, spawnSize, true);
                    spawnPlateauMask.fillCircle(symLocation, spawnSize, true);
                }
            } else {
                boolean valid = false;
                for (int j = 0; j < i; j += 2) {
                    if (spawnPlateauMask.get(map.getSpawns()[j]) && map.getSpawns()[j].getXZDistance(location) < spawnSize * 4) {
                        valid = true;
                        break;
                    }
                }
                if (valid) {
                    spawnPlateauMask.fillCircle(location, spawnSize, true);
                    spawnPlateauMask.fillCircle(symLocation, spawnSize, true);
                }
            }
            map.getSpawns()[i] = new Vector3f(location);
            map.getSpawns()[i + 1] = new Vector3f(symLocation);
            location = spawnable.getRandomPosition();
        }
        return new BinaryMask[] {spawnLandMask, spawnPlateauMask};
    }

    public void generateMexes(BinaryMask spawnable, BinaryMask spawnablePlateau, BinaryMask spawnableWater) {
        BinaryMask spawnableLand = new BinaryMask(spawnable, random.nextLong());
        spawnable.fillHalf(false);
        float spawnDensity = (float) spawnable.getCount() / map.getSize() / map.getSize() * 2;
        int mexSpawnDistance = (int) StrictMath.max(StrictMath.min(spawnDensity * map.getSize() / 4, spawnSize * 3), spawnSize * 2);
        BinaryMask spawnableNoSpawns = new BinaryMask(spawnable, random.nextLong());
        for (int i = 0; i < map.getSpawns().length; i += 2) {
            spawnable.fillCircle(map.getSpawns()[i + 1], 24, false);
            spawnableNoSpawns.fillCircle(map.getSpawns()[i + 1], mexSpawnDistance, false);
        }
        int spawnCount = map.getSpawns().length;
        int totalMexCount = map.getMexes().length;
        int spawnMexCount = 4 * spawnCount;
        int nonSpawnMexCount = totalMexCount - spawnMexCount;
        int iMex = 0;
        for (int i = 0; i < map.getSpawns().length; i += 2) {
            map.getMexes()[iMex] = new Vector3f(map.getSpawns()[i].x + 10, 0, map.getSpawns()[i].z);
            map.getMexes()[iMex + 1] = new Vector3f(map.getSpawns()[i].x - 10, 0, map.getSpawns()[i].z);
            map.getMexes()[iMex + 2] = new Vector3f(map.getSpawns()[i].x, 0, map.getSpawns()[i].z + 10);
            map.getMexes()[iMex + 3] = new Vector3f(map.getSpawns()[i].x, 0, map.getSpawns()[i].z - 10);
            map.getMexes()[iMex + 4] = new Vector3f(spawnable.getSymmetryPoint(map.getMexes()[iMex]));
            map.getMexes()[iMex + 5] = new Vector3f(spawnable.getSymmetryPoint(map.getMexes()[iMex + 1]));
            map.getMexes()[iMex + 6] = new Vector3f(spawnable.getSymmetryPoint(map.getMexes()[iMex + 2]));
            map.getMexes()[iMex + 7] = new Vector3f(spawnable.getSymmetryPoint(map.getMexes()[iMex + 3]));
            iMex += 8;
            BinaryMask nearMexes = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetryHierarchy());
            nearMexes.fillCircle(map.getSpawns()[i + 1], spawnSize * 3, true).fillCircle(map.getSpawns()[i + 1], spawnSize, false).intersect(spawnable);
            int numNearMexes = random.nextInt(nonSpawnMexCount / 24 + 1) * 2;
            for (int j = 0; j < numNearMexes; j += 2) {
                Vector2f location = nearMexes.getRandomPosition();
                if (location == null) {
                    break;
                }
                Vector2f symLocation = nearMexes.getSymmetryPoint(location);
                map.getMexes()[iMex] = new Vector3f(location);
                map.getMexes()[iMex + 1] = new Vector3f(symLocation);
                nearMexes.fillCircle(location, mexSpacing, false);
                spawnable.fillCircle(location, mexSpacing, false);
                nearMexes.fillCircle(symLocation, mexSpacing, false);
                spawnable.fillCircle(symLocation, mexSpacing, false);
                iMex += 2;
            }
        }
        int numMexesLeft;
        int actualExpMexCount;
        int baseMexCount = iMex;
        int nonBaseMexCount = totalMexCount - baseMexCount;

        if (nonBaseMexCount / 2 > 10) {
            int possibleExpMexCount = (random.nextInt(nonBaseMexCount / 2 / 2) + nonBaseMexCount / 2 / 2) * 2;
            actualExpMexCount = generateMexExpansions(spawnable, baseMexCount, possibleExpMexCount);
            numMexesLeft = nonBaseMexCount - actualExpMexCount;
        } else {
            actualExpMexCount = 0;
            numMexesLeft = nonBaseMexCount;
        }

        spawnableNoSpawns.intersect(spawnable);
        spawnablePlateau.intersect(spawnableNoSpawns);
        spawnableLand.intersect(spawnableNoSpawns);
        spawnableLand.minus(spawnablePlateau);

        float plateauDensity = (float) spawnablePlateau.getCount() / spawnableNoSpawns.getCount();
        int plateauMexCount = (int) (plateauDensity * numMexesLeft / 2) * 2;

        for (int i = 0; i < plateauMexCount; i += 2) {
            int ind = i + baseMexCount + actualExpMexCount;
            Vector2f mexLocation = spawnablePlateau.getRandomPosition();

            if (mexLocation == null) {
                break;
            }

            numMexesLeft -= 2;
            Vector2f mexSymLocation = spawnablePlateau.getSymmetryPoint(mexLocation);

            map.getMexes()[ind] = new Vector3f(mexLocation);
            map.getMexes()[ind + 1] = new Vector3f(mexSymLocation);

            spawnablePlateau.fillCircle(mexLocation, mexSpacing, false);
        }

        int numLandMexes = numMexesLeft;
        for (int i = 0; i < numLandMexes; i += 2) {
            int ind = i + map.getMexes().length - numLandMexes;
            Vector2f mexLocation = spawnableLand.getRandomPosition();

            if (mexLocation == null) {
                break;
            }
            numMexesLeft -= 2;

            Vector2f mexSymLocation = spawnableLand.getSymmetryPoint(mexLocation);

            map.getMexes()[ind] = new Vector3f(mexLocation);
            map.getMexes()[ind + 1] = new Vector3f(mexSymLocation);

            spawnableLand.fillCircle(mexLocation, mexSpacing, false);
        }
        spawnable.intersect(spawnableLand.combine(spawnablePlateau));

        int numNearSpawnMexes = numMexesLeft;
        for (int i = 0; i < numNearSpawnMexes; i += 2) {
            int ind = i + map.getMexes().length - numLandMexes;
            Vector2f mexLocation = spawnable.getRandomPosition();

            if (mexLocation == null) {
                break;
            }
            numMexesLeft -= 2;

            Vector2f mexSymLocation = spawnable.getSymmetryPoint(mexLocation);

            map.getMexes()[ind] = new Vector3f(mexLocation);
            map.getMexes()[ind + 1] = new Vector3f(mexSymLocation);

            spawnable.fillCircle(mexLocation, mexSpacing, false);
            spawnable.fillCircle(mexSymLocation, mexSpacing, false);
        }

        for (int i = 0; i < numMexesLeft; i += 2) {
            int ind = i + map.getMexes().length - numMexesLeft;
            Vector2f mexLocation = spawnableWater.getRandomPosition();

            if (mexLocation == null) {
                break;
            }

            Vector2f mexSymLocation = spawnableWater.getSymmetryPoint(mexLocation);

            map.getMexes()[ind] = new Vector3f(mexLocation);
            map.getMexes()[ind + 1] = new Vector3f(mexSymLocation);

            spawnableWater.fillCircle(mexLocation, mexSpacing, false);
            spawnableWater.fillCircle(mexSymLocation, mexSpacing, false);
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
        int expSpacing = 64;

        BinaryMask spawnableCopy = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetryHierarchy());
        BinaryMask expansion = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetryHierarchy());

        spawnableCopy.fillCircle(map.getSize() / 2f, map.getSize() / 2f, map.getSize() / 2f, true).fillCenter(64, false).intersect(spawnable);

        for (int i = 0; i < map.getSpawns().length; i++) {
            spawnableCopy.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, map.getSize() / 4f, false);
        }

        while (expMexCountLeft > 1) {
            expLocation = spawnableCopy.getRandomPosition();

            while (expLocation != null && !isMexExpValid(expLocation, expSize, .5f, spawnable)) {
                spawnableCopy.fillRect(expLocation, 1, 1, false);
                expLocation = spawnableCopy.getRandomPosition();
            }

            if (expLocation == null) {
                actualExpMexCount = possibleExpMexCount - expMexCountLeft;
                break;
            }

            expansion.fillRect((int) expLocation.x - expSize, (int) expLocation.y - expSize, expSize * 2, expSize * 2, true);
            expansion.intersect(spawnable);

            expMexCount = StrictMath.min((random.nextInt(3) + 2) * 2, expMexCountLeft);

            spawnableCopy.fillCircle(expLocation, expSpacing, false);
            spawnableCopy.fillCircle(spawnableCopy.getSymmetryPoint(expLocation), expSpacing, false);

            for (int i = iMex; i < iMex + expMexCount; i += 2) {
                mexLocation = expansion.getRandomPosition();
                if (mexLocation == null) {
                    expMexCount -= i - iMex;
                    break;
                }
                mexSymLocation = expansion.getSymmetryPoint(mexLocation);

                map.getMexes()[i] = new Vector3f(mexLocation);
                map.getMexes()[i + 1] = new Vector3f(mexSymLocation);

                expansion.fillCircle(mexLocation, expMexSpacing, false);
                expansion.fillCircle(mexSymLocation, expMexSpacing, false);

                spawnable.fillCircle(mexLocation, mexSpacing, false);
                spawnable.fillCircle(mexSymLocation, mexSpacing, false);
            }

            iMex += expMexCount;
            expansion.fillCircle(expLocation, expSize + 1, false);
            expMexCountLeft -= expMexCount;
        }
        return actualExpMexCount;
    }

    public void generateHydros(BinaryMask spawnable, BinaryMask land) {
        int baseHydroCount = map.getSpawns().length;
        int hydroSpacing = 64;

        for (int i = 0; i < map.getSpawns().length; i += 2) {
            BinaryMask baseHydro = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetryHierarchy());
            baseHydro.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 18, true);
            baseHydro.trimEdge(16);
            for (int j = 0; j < map.getSpawns().length; j += 2) {
                baseHydro.fillCircle(map.getSpawns()[j].x, map.getSpawns()[j].z, 16, false);
            }
            for (int j = 0; j < i; j += 2) {
                baseHydro.fillCircle(map.getHydros()[j].x, map.getHydros()[j].z, 8, false);
            }
            Vector2f location = baseHydro.getRandomPosition();
            if (location == null) {
                break;
            }
            map.getHydros()[i] = new Vector3f(location);
            Vector2f symLocation = spawnable.getSymmetryPoint((int) map.getHydros()[i].x, (int) map.getHydros()[i].z);
            map.getHydros()[i + 1] = new Vector3f(symLocation);
            spawnable.fillCircle(map.getSpawns()[i + 1].x, map.getSpawns()[i + 1].z, 30, false);
        }

        for (int i = baseHydroCount; i < map.getHydros().length; i += 2) {
            Vector2f hydroLocation = spawnable.getRandomPosition();

            if (hydroLocation == null) {
                break;
            }

            Vector2f hydroSymLocation = spawnable.getSymmetryPoint(hydroLocation);

            map.getHydros()[i] = new Vector3f(hydroLocation);
            map.getHydros()[i + 1] = new Vector3f(hydroSymLocation);

            spawnable.fillCircle(hydroLocation, hydroSpacing, false);
            spawnable.fillCircle(hydroSymLocation, hydroSpacing, false);
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
        for (int i = 0; i < map.getSpawns().length; i++) {
            map.getSpawns()[i] = placeOnHeightmap(map, map.getSpawns()[i]);
        }
        for (int i = 0; i < map.getMexes().length; i++) {
            if (map.getMexes()[i] != null) {
                map.getMexes()[i] = placeOnHeightmap(map, map.getMexes()[i]);
            }
        }
        for (int i = 0; i < map.getHydros().length; i++) {
            if (map.getHydros()[i] != null) {
                map.getHydros()[i] = placeOnHeightmap(map, map.getHydros()[i]);
            }
        }
    }
}
