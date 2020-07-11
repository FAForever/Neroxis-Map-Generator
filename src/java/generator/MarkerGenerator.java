package generator;

import map.BinaryMask;
import map.SCMap;
import util.Vector2f;
import util.Vector3f;

import java.util.Random;

public strictfp class MarkerGenerator {
    private final SCMap map;
    private final Random random;
    private final int mexSpacing = 16;

    public MarkerGenerator(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    private Vector3f placeOnHeightmap(Vector2f v1) {
        return placeOnHeightmap(v1.x, v1.y);
    }

    private Vector3f placeOnHeightmap(float x, float z) {
        Vector3f v = new Vector3f(x, 0, z);
        v.x = x;
        v.z = z;
        v.y = map.getHeightmap().getRaster().getPixel((int) v.x, (int) v.z, new int[]{0})[0] * (SCMap.HEIGHTMAP_SCALE);
        return v;
    }

    public void generateSpawns(BinaryMask spawnable, float separation) {
        BinaryMask spawnableCopy = new BinaryMask(spawnable, random.nextLong());
        Vector2f location = spawnableCopy.getRandomPosition();
        Vector2f symLocation;
        for (int i = 0; i < map.getSpawns().length; i += 2) {
            symLocation = spawnableCopy.getSymmetryPoint(location);
            spawnableCopy.fillCircle(location, separation, false);
            spawnableCopy.fillCircle(symLocation, separation, false);
            map.getSpawns()[i] = placeOnHeightmap(location);
            map.getSpawns()[i + 1] = placeOnHeightmap(symLocation);
            location = spawnableCopy.getRandomPosition();
        }
    }

    public void generateMexes(BinaryMask spawnable) {
        int baseMexCount = map.getSpawns().length * 4;

        for (int i = 0; i < map.getSpawns().length; i++) {
            map.getMexes()[i * 4] = placeOnHeightmap(map.getSpawns()[i].x + 10, map.getSpawns()[i].z);
            map.getMexes()[i * 4 + 1] = placeOnHeightmap(map.getSpawns()[i].x - 10, map.getSpawns()[i].z);
            map.getMexes()[i * 4 + 2] = placeOnHeightmap(map.getSpawns()[i].x, map.getSpawns()[i].z + 10);
            map.getMexes()[i * 4 + 3] = placeOnHeightmap(map.getSpawns()[i].x, map.getSpawns()[i].z - 10);
            spawnable.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 30, false);
        }

        int possibleExpMexCount = random.nextInt((map.getMexes().length - baseMexCount) / 2) * 2;
        int actualExpMexCount = generateMexExpansions(spawnable, baseMexCount, possibleExpMexCount);

        for (int i = baseMexCount + actualExpMexCount; i < map.getMexes().length; i += 2) {
            Vector2f mexLocation = spawnable.getRandomPosition();

            if (mexLocation == null) {
                break;
            }

            Vector2f mexSymLocation = spawnable.getSymmetryPoint(mexLocation);

            map.getMexes()[i] = placeOnHeightmap(mexLocation);
            map.getMexes()[i + 1] = placeOnHeightmap(mexSymLocation);

            spawnable.fillCircle(mexLocation, mexSpacing, false);
            spawnable.fillCircle(mexSymLocation, mexSpacing, false);
        }
    }

    public int generateMexExpansions(BinaryMask spawnable, int baseMexCount, int possibleExpMexCount) {
        Vector2f expLocation;
        Vector2f expSymLocation;
        Vector2f mexLocation;
        Vector2f mexSymLocation;
        int actualExpMexCount = possibleExpMexCount;
        int expMexCount;
        int expMexCountLeft = possibleExpMexCount;
        int iMex = baseMexCount;
        int expMexSpacing = 10;
        int expSize = 10;
        int expSpacing = 64;

        BinaryMask spawnableCopy = new BinaryMask(spawnable, random.nextLong());
        BinaryMask expansion = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetry());
        spawnableCopy.fillCenter(64, false);

        for (int i = 0; i < map.getSpawns().length; i++) {
            spawnableCopy.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 96, false);
        }

        while (expMexCountLeft > 0) {
            expLocation = spawnableCopy.getRandomPosition();

            while (expLocation != null && !isMexExpValid(expLocation, expSize, .5f, spawnable)) {
                spawnableCopy.fillRect(expLocation, 1, 1, false);
                expLocation = spawnableCopy.getRandomPosition();
            }

            if (expLocation == null) {
                actualExpMexCount = possibleExpMexCount - expMexCountLeft;
                break;
            }

            expSymLocation = spawnableCopy.getSymmetryPoint(expLocation);
            spawnableCopy.fillCircle(expLocation, expSpacing, false);
            spawnableCopy.fillCircle(expSymLocation, expSpacing, false);

            expansion.fillRect((int) expLocation.x - expSize / 2, (int) expLocation.y - expSize / 2, expSize, expSize, true);
            expansion.intersect(spawnable);

            expMexCount = StrictMath.min((random.nextInt(3) + 2) * 2, expMexCountLeft);

            for (int i = iMex; i < iMex + expMexCount; i += 2) {
                mexLocation = expansion.getRandomPosition();
                if (mexLocation == null) {
                    expMexCount = StrictMath.max(i - iMex - 2, 0);
                    break;
                }
                mexSymLocation = expansion.getSymmetryPoint(mexLocation);

                map.getMexes()[i] = placeOnHeightmap(mexLocation);
                map.getMexes()[i + 1] = placeOnHeightmap(mexSymLocation);

                expansion.fillCircle(mexLocation, expMexSpacing, false);

                spawnableCopy.fillCircle(mexLocation, expSpacing, false);
                spawnableCopy.fillCircle(mexSymLocation, expSpacing, false);

                spawnable.fillCircle(mexLocation, mexSpacing * 2, false);
                spawnable.fillCircle(mexSymLocation, mexSpacing * 2, false);
            }

            iMex += expMexCount;
            expansion.fillCircle(expLocation, expSize + 1, false);
            expMexCountLeft -= expMexCount;
        }
        return actualExpMexCount;
    }

    public void generateHydros(BinaryMask spawnable) {
        int baseHydroCount = map.getSpawns().length;
        int hydroSpacing = 64;

        for (int i = 0; i < map.getSpawns().length; i++) {
            int dx = map.getSpawns()[i].x < (float) map.getSize() / 2 ? -4 : +4;
            int dz = map.getSpawns()[i].z < (float) map.getSize() / 2 ? -14 : +14;
            map.getHydros()[i] = placeOnHeightmap(map.getSpawns()[i].x + dx, map.getSpawns()[i].z + dz);
            spawnable.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 30, false);
        }

        for (int i = baseHydroCount; i < map.getHydros().length; i += 2) {
            Vector2f hydroLocation = spawnable.getRandomPosition();

            if (hydroLocation == null) {
                break;
            }

            Vector2f hydroSymLocation = spawnable.getSymmetryPoint(hydroLocation);

            map.getHydros()[i] = placeOnHeightmap(hydroLocation);
            map.getHydros()[i + 1] = placeOnHeightmap(hydroSymLocation);

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
}
