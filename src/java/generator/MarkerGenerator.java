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

    private void placeOnHeightmap(float x, float z, Vector3f v) {
        v.x = x;
        v.z = z;
        v.y = map.getHeightmap().getRaster().getPixel((int) v.x, (int) v.z, new int[]{0})[0] * (SCMap.HEIGHTMAP_SCALE);
    }

    private Vector3f placeOnHeightmap(float x, float z) {
        Vector3f v = new Vector3f(x, 0, z);
        v.y = map.getHeightmap().getRaster().getPixel((int) v.x, (int) v.z, new int[]{0})[0] * (SCMap.HEIGHTMAP_SCALE);
        return v;
    }

    public void generateSpawns(BinaryMask spawnable, float separation) {
        BinaryMask spawnableCopy = new BinaryMask(spawnable, random.nextLong());
        Vector2f location = spawnableCopy.getRandomPosition();
        for (int i = 0; i < map.getSpawns().length; i += 2) {
            spawnableCopy.fillCircle(location.x, location.y, separation, false);
            spawnableCopy.fillCircle(map.getSize() - location.x, map.getSize() - location.y, separation, false);
            map.getSpawns()[i] = placeOnHeightmap(location.x, location.y);
            map.getSpawns()[i + 1] = placeOnHeightmap(map.getSize() - location.x, map.getSize() - location.y);
            location = spawnableCopy.getRandomPosition();
        }
    }

    public void generateMexes(BinaryMask spawnable) {
        int baseMexCount = map.getSpawns().length * 4;

        for (int i = 0; i < map.getSpawns().length; i++) {
            map.getMexes()[i * 4] = new Vector3f(0, 0, 0);
            map.getMexes()[i * 4 + 1] = new Vector3f(0, 0, 0);
            map.getMexes()[i * 4 + 2] = new Vector3f(0, 0, 0);
            map.getMexes()[i * 4 + 3] = new Vector3f(0, 0, 0);
            placeOnHeightmap(map.getSpawns()[i].x + 10, map.getSpawns()[i].z, map.getMexes()[i * 4]);
            placeOnHeightmap(map.getSpawns()[i].x - 10, map.getSpawns()[i].z, map.getMexes()[i * 4 + 1]);
            placeOnHeightmap(map.getSpawns()[i].x, map.getSpawns()[i].z + 10, map.getMexes()[i * 4 + 2]);
            placeOnHeightmap(map.getSpawns()[i].x, map.getSpawns()[i].z - 10, map.getMexes()[i * 4 + 3]);
            spawnable.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 30, false);
        }

        int possibleExpMexCount = random.nextInt((map.getMexes().length - baseMexCount) / 2) * 2;
        int actualExpMexCount = generateMexExpansions(spawnable, baseMexCount, possibleExpMexCount);

        for (int i = baseMexCount + actualExpMexCount; i < map.getMexes().length; i += 2) {
            Vector2f mexLocation = spawnable.getRandomPosition();

            if (mexLocation == null) {
                break;
            }

            map.getMexes()[i] = new Vector3f(0, 0, 0);
            map.getMexes()[i + 1] = new Vector3f(0, 0, 0);

            placeOnHeightmap(mexLocation.x, mexLocation.y, map.getMexes()[i]);
            placeOnHeightmap(map.getSize() - mexLocation.x, map.getSize() - mexLocation.y, map.getMexes()[i + 1]);

            spawnable.fillCircle(mexLocation.x, mexLocation.y, mexSpacing, false);
            spawnable.fillCircle(map.getSize() - mexLocation.x, map.getSize() - mexLocation.y, mexSpacing, false);
        }
    }

    public int generateMexExpansions(BinaryMask spawnable, int baseMexCount, int possibleExpMexCount) {
        Vector2f expLocation;
        Vector2f mexLocation;
        int actualExpMexCount = possibleExpMexCount;
        int expMexCount;
        int expMexCountLeft = possibleExpMexCount;
        int iMex = baseMexCount;
        int expMexSpacing = 10;
        int expSize = 10;
        int expSpacing = 64;

        BinaryMask spawnableCopy = new BinaryMask(spawnable, random.nextLong());
        BinaryMask expansion = new BinaryMask(spawnable.getSize(), random.nextLong());

        for (int i = 0; i < map.getSpawns().length; i++) {
            spawnableCopy.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 96, false);
        }

        while (expMexCountLeft > 0) {
            expLocation = spawnableCopy.getRandomPosition();

            if (expLocation == null) {
                actualExpMexCount = possibleExpMexCount - expMexCountLeft;
                break;
            }

            while (!isMexExpValid(expLocation, expSize, .5f, spawnable)) {
                spawnableCopy.fillCircle(expLocation.x, expLocation.y, 1, false);
                spawnableCopy.fillCircle(map.getSize() - expLocation.x, map.getSize() - expLocation.y, 1, false);

                expLocation = spawnableCopy.getRandomPosition();

                if (expLocation == null) {
                    actualExpMexCount = possibleExpMexCount - expMexCountLeft;
                    break;
                }
            }

            if (expLocation == null) {
                actualExpMexCount = possibleExpMexCount - expMexCountLeft;
                break;
            }

            spawnableCopy.fillCircle(expLocation.x, expLocation.y, expSpacing, false);
            spawnableCopy.fillCircle(map.getSize() - expLocation.x, map.getSize() - expLocation.y, expSpacing, false);

            expansion.fillCircle(expLocation.x, expLocation.y, 1, true);
            expansion.fillCircle(map.getSize() - expLocation.x, map.getSize() - expLocation.y, 1, true);
            expansion.inflate(expSize).intersect(spawnable);

            expMexCount = StrictMath.min((random.nextInt(3) + 2) * 2, expMexCountLeft);

            for (int i = iMex; i < iMex + expMexCount; i += 2) {
                mexLocation = expansion.getRandomPosition();
                if (mexLocation == null) {
                    expMexCount = i - iMex - 2;
                    break;
                }

                map.getMexes()[i] = new Vector3f(0, 0, 0);
                map.getMexes()[i + 1] = new Vector3f(0, 0, 0);

                placeOnHeightmap(mexLocation.x, mexLocation.y, map.getMexes()[i]);
                placeOnHeightmap(map.getSize() - mexLocation.x, map.getSize() - mexLocation.y, map.getMexes()[i + 1]);

                expansion.fillCircle(mexLocation.x, mexLocation.y, expMexSpacing, false);
                expansion.fillCircle(map.getSize() - mexLocation.x, map.getSize() - mexLocation.y, expMexSpacing, false);

                spawnableCopy.fillCircle(mexLocation.x, mexLocation.y, expSpacing, false);
                spawnableCopy.fillCircle(map.getSize() - mexLocation.x, map.getSize() - mexLocation.y, expSpacing, false);

                spawnable.fillCircle(mexLocation.x, mexLocation.y, mexSpacing * 2, false);
                spawnable.fillCircle(map.getSize() - mexLocation.x, map.getSize() - mexLocation.y, mexSpacing * 2, false);
            }

            iMex += expMexCount;
            expansion.fillCircle(expLocation.x, expLocation.y, expSize + 1, false);
            expansion.fillCircle(map.getSize() - expLocation.x, map.getSize() - expLocation.y, expSize + 1, false);
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
            map.getHydros()[i] = new Vector3f(0, 0, 0);
            placeOnHeightmap(map.getSpawns()[i].x + dx, map.getSpawns()[i].z + dz, map.getHydros()[i]);
            spawnable.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 30, false);
        }

        for (int i = baseHydroCount; i < map.getHydros().length; i += 2) {
            Vector2f hydroLocation = spawnable.getRandomPosition();

            if (hydroLocation == null) {
                break;
            }

            map.getHydros()[i] = new Vector3f(0, 0, 0);
            map.getHydros()[i + 1] = new Vector3f(0, 0, 0);

            placeOnHeightmap(hydroLocation.x, hydroLocation.y, map.getHydros()[i]);
            placeOnHeightmap(map.getSize() - hydroLocation.x, map.getSize() - hydroLocation.y, map.getHydros()[i + 1]);

            spawnable.fillCircle(hydroLocation.x, hydroLocation.y, hydroSpacing, false);
            spawnable.fillCircle(map.getSize() - hydroLocation.x, map.getSize() - hydroLocation.y, hydroSpacing, false);
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
