package map;

import generator.VisualDebugger;
import lombok.Getter;
import lombok.SneakyThrows;
import util.Util;
import util.Vector2f;
import util.Vector3f;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

@Getter
public strictfp class FloatMask extends Mask {
    private final Random random;
    private float[][] mask;

    public FloatMask(int size, long seed, SymmetryHierarchy symmetryHierarchy) {
        this.mask = new float[size][size];
        this.random = new Random(seed);
        this.symmetryHierarchy = symmetryHierarchy;
        for (int y = 0; y < this.getSize(); y++) {
            for (int x = 0; x < this.getSize(); x++) {
                this.mask[x][y] = 0f;
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public FloatMask(FloatMask mask, long seed) {
        this.mask = new float[mask.getSize()][mask.getSize()];
        this.random = new Random(seed);
        this.symmetryHierarchy = mask.getSymmetryHierarchy();
        for (int y = 0; y < mask.getSize(); y++) {
            for (int x = 0; x < mask.getSize(); x++) {
                this.mask[x][y] = mask.get(x, y);
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public int getSize() {
        return mask[0].length;
    }

    public float get(int x, int y) {
        return mask[x][y];
    }


    public void set(Vector2f location, float value) {
        set((int) location.x, (int) location.y, value);
    }

    public void set(Vector3f location, float value) {
        set((int) location.x, (int) location.z, value);
    }

    public void set(int x, int y, float value) {
        mask[x][y] = value;
    }

    public void add(int x, int y, float value) {
        mask[x][y] += value;
    }

    public FloatMask init(BinaryMask other, float low, float high) {
        int size = getSize();
        if (other.getSize() < size)
            other = other.copy().enlarge(size);
        if (other.getSize() > size) {
            other = other.copy().shrink(size);
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (other.get(x, y)) {
                    set(x, y, high);
                } else {
                    set(x, y, low);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask copy() {
        return new FloatMask(this, random.nextLong());
    }

    public FloatMask add(FloatMask other) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                add(x, y, other.get(x, y));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask max(FloatMask other) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                set(x, y, StrictMath.max(get(x, y), other.get(x, y)));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask maskToMountains(BinaryMask other) {
        other = other.copy();
        int size = getSize();
        if (other.getSize() < size)
            other = other.copy().enlarge(size);
        if (other.getSize() > size) {
            other = other.copy().shrink(size);
        }
        FloatMask mountainBase = new FloatMask(getSize(), 0, symmetryHierarchy);
        add(mountainBase.init(other, 0, 2f));
        while (other.getCount() > 0) {
            FloatMask layer = new FloatMask(getSize(), 0, symmetryHierarchy);
            add(layer.init(other, 0, random.nextFloat() * .75f));
            other.erode(random.nextFloat(), symmetryHierarchy.getSpawnSymmetry());
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask maskToHeightmap(float underWaterSlope, int maxRepeat, BinaryMask other) {
        other = other.copy().invert();
        int size = getSize();
        if (other.getSize() < size)
            other = other.copy().enlarge(size);
        if (other.getSize() > size) {
            other = other.copy().shrink(size);
        }
        int count = 0;
        while (other.getCount() > 0 && count < maxRepeat) {
            count++;
            FloatMask layer = new FloatMask(getSize(), 0, symmetryHierarchy);
            add(layer.init(other, 0, -underWaterSlope));
            other.erode(0.75f, symmetryHierarchy.getSpawnSymmetry());
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask smooth(float radius) {

        final float[][] maskCopy = new float[getSize()][getSize()];

        Thread[] threads = new Thread[4];
        threads[0] = new Thread(() -> smoothRegion(radius, maskCopy, 0, (getSize() / 4)));
        threads[1] = new Thread(() -> smoothRegion(radius, maskCopy, (getSize() / 4), (getSize() / 2)));
        threads[2] = new Thread(() -> smoothRegion(radius, maskCopy, (getSize() / 2), (getSize() / 4) * 3));
        threads[3] = new Thread(() -> smoothRegion(radius, maskCopy, (getSize() / 4) * 3, getSize()));

        Arrays.stream(threads).forEach(Thread::start);
        for (Thread f : threads) {
            try {
                f.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    private void smoothRegion(float radius, float[][] maskCopy, int startY, int endY) {
        float radius2 = (radius + 0.5f) * (radius + 0.5f);
        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < getSize(); x++) {
                int count = 0;
                float avg = 0;
                for (int y2 = (int) (y - radius); y2 <= y + radius; y2++) {
                    for (int x2 = (int) (x - radius); x2 <= x + radius; x2++) {
                        if (inBounds(x2, y2) && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2) {
                            count++;
                            avg += get(x2, y2);
                        }
                    }
                }
                maskCopy[x][y] = avg / count;
            }
        }
    }

    public FloatMask smooth(float radius, BinaryMask limiter) {

        final float[][] maskCopy = new float[getSize()][getSize()];

        Thread[] threads = new Thread[4];
        threads[0] = new Thread(() -> smoothRegion(radius, limiter, maskCopy, 0, (getSize() / 4)));
        threads[1] = new Thread(() -> smoothRegion(radius, limiter, maskCopy, (getSize() / 4), (getSize() / 2)));
        threads[2] = new Thread(() -> smoothRegion(radius, limiter, maskCopy, (getSize() / 2), (getSize() / 4) * 3));
        threads[3] = new Thread(() -> smoothRegion(radius, limiter, maskCopy, (getSize() / 4) * 3, getSize()));

        Arrays.stream(threads).forEach(Thread::start);
        for (Thread f : threads) {
            try {
                f.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    private void smoothRegion(float radius, BinaryMask limiter, float[][] maskCopy, int startY, int endY) {
        float radius2 = (radius + 0.5f) * (radius + 0.5f);
        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < getSize(); x++) {
                if (limiter.get(x, y)) {
                    int count = 0;
                    float avg = 0;
                    for (int y2 = (int) (y - radius); y2 <= y + radius; y2++) {
                        for (int x2 = (int) (x - radius); x2 <= x + radius; x2++) {
                            if (inBounds(x2, y2) && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2) {
                                count++;
                                avg += get(x2, y2);
                            }
                        }
                    }
                    maskCopy[x][y] = avg / count;
                } else {
                    maskCopy[x][y] = get(x, y);
                }
            }
        }
    }

    public FloatMask gradient() {
        float[][] maskCopy = new float[getSize()][getSize()];
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                int xNeg = StrictMath.max(0, x - 1);
                int xPos = StrictMath.min(getSize() - 1, x + 1);
                int yNeg = StrictMath.max(0, y - 1);
                int yPos = StrictMath.min(getSize() - 1, y + 1);
                float xSlope = get(xPos, y) - get(xNeg, y);
                float ySlope = get(x, yPos) - get(x, yNeg);
                maskCopy[x][y] = (float) StrictMath.sqrt(xSlope * xSlope + ySlope * ySlope);
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public void applySymmetry() {
        applySymmetry(symmetryHierarchy.getTerrainSymmetry());
    }

    public void applySymmetry(Symmetry symmetry) {
        for (int x = getMinXBound(symmetry); x < getMaxXBound(symmetry); x++) {
            for (int y = getMinYBound(x, symmetry); y < getMaxYBound(x, symmetry); y++) {
                Vector2f[] symPoints = getTerrainSymmetryPoints(x, y, symmetry);
                for (Vector2f symPoint : symPoints) {
                    set(symPoint, get(x, y));
                }
            }
        }
    }
    // -------------------------------------------

    @SneakyThrows
    public void writeToFile(Path path) {
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path.toFile())));

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                out.writeFloat(get(x, y));
            }
        }

        out.close();
    }

    public String toHash() throws NoSuchAlgorithmException {
        ByteBuffer bytes = ByteBuffer.allocate(getSize() * getSize() * 4);
        for (int x = getMinXBound(symmetryHierarchy.getSpawnSymmetry()); x < getMaxXBound(symmetryHierarchy.getSpawnSymmetry()); x++) {
            for (int y = getMinYBound(x, symmetryHierarchy.getSpawnSymmetry()); y < getMaxYBound(x, symmetryHierarchy.getSpawnSymmetry()); y++) {
                bytes.putFloat(get(x, y));
            }
        }
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }

    public void show() {
        VisualDebugger.visualizeMask(this);
    }

    public void startVisualDebugger(String maskName) {
        startVisualDebugger(maskName, Util.getStackTraceParentClass());
    }

    public void startVisualDebugger(String maskName, String parentClass) {
        VisualDebugger.whitelistMask(this, maskName, parentClass);
        show();
    }
}
