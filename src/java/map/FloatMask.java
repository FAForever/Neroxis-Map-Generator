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
import java.util.Random;

@Getter
public strictfp class FloatMask extends Mask {
    private final Random random;
    private float[][] mask;

    public FloatMask(int size, Long seed, SymmetryHierarchy symmetryHierarchy) {
        this.mask = new float[size][size];
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
        this.symmetryHierarchy = symmetryHierarchy;
        for (int y = 0; y < this.getSize(); y++) {
            for (int x = 0; x < this.getSize(); x++) {
                this.mask[x][y] = 0f;
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public FloatMask(FloatMask mask, Long seed) {
        this.mask = new float[mask.getSize()][mask.getSize()];
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
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

    public float getMin() {
        float val = Float.MAX_VALUE;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                val = StrictMath.min(val, get(x, y));
            }
        }
        return val;
    }

    public float getMax() {
        float val = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                val = StrictMath.max(val, get(x, y));
            }
        }
        return val;
    }

    public float getAvg() {
        float val = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                val += get(x, y);
            }
        }
        return val / getSize() / getSize();
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

    public void multiply(int x, int y, float value) {
        mask[x][y] *= value;
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
        if (random != null) {
            return new FloatMask(this, random.nextLong());
        } else {
            return new FloatMask(this, null);
        }
    }

    public FloatMask multiply(FloatMask other) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                multiply(x, y, other.get(x, y));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask multiply(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                multiply(x, y, val);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
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

    public FloatMask add(BinaryMask other, float value) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (other.get(x, y)) {
                    add(x, y, value);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask min(FloatMask other) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                set(x, y, StrictMath.min(get(x, y), other.get(x, y)));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask clampMin(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                set(x, y, StrictMath.max(get(x, y), val));
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

    public FloatMask clampMax(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                set(x, y, StrictMath.min(get(x, y), val));
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
        FloatMask mountainBase = new FloatMask(getSize(), null, symmetryHierarchy);
        add(mountainBase.init(other, 0, 2f));
        while (other.getCount() > 0) {
            add(other, random.nextFloat() * .75f);
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
            add(other, -underWaterSlope);
            other.erode(0.75f, symmetryHierarchy.getSpawnSymmetry());
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask smooth(int radius) {
        int[][] innerCount = new int[getSize()][getSize()];

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                float val = get(x, y);
                innerCount[x][y] = StrictMath.round(val * 1000);
                innerCount[x][y] += x > 0 ? innerCount[x - 1][y] : 0;
                innerCount[x][y] += y > 0 ? innerCount[x][y - 1] : 0;
                innerCount[x][y] -= x > 0 && y > 0 ? innerCount[x - 1][y - 1] : 0;
            }
        }

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                int xLeft = StrictMath.max(0, x - radius);
                int xRight = StrictMath.min(getSize() - 1, x + radius);
                int yUp = StrictMath.max(0, y - radius);
                int yDown = StrictMath.min(getSize() - 1, y + radius);
                int countA = xLeft > 0 && yUp > 0 ? innerCount[xLeft - 1][yUp - 1] : 0;
                int countB = yUp > 0 ? innerCount[xRight][yUp - 1] : 0;
                int countC = xLeft > 0 ? innerCount[xLeft - 1][yDown] : 0;
                int countD = innerCount[xRight][yDown];
                int count = countD + countA - countB - countC;
                int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
                set(x, y, count / 1000f / area);
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask smooth(int radius, BinaryMask limiter) {
        int[][] innerCount = new int[getSize()][getSize()];

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                float val = get(x, y);
                innerCount[x][y] = StrictMath.round(val * 1000);
                innerCount[x][y] += x > 0 ? innerCount[x - 1][y] : 0;
                innerCount[x][y] += y > 0 ? innerCount[x][y - 1] : 0;
                innerCount[x][y] -= x > 0 && y > 0 ? innerCount[x - 1][y - 1] : 0;
            }
        }

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (limiter.get(x, y)) {
                    int xLeft = StrictMath.max(0, x - radius);
                    int xRight = StrictMath.min(getSize() - 1, x + radius);
                    int yUp = StrictMath.max(0, y - radius);
                    int yDown = StrictMath.min(getSize() - 1, y + radius);
                    int countA = xLeft > 0 && yUp > 0 ? innerCount[xLeft - 1][yUp - 1] : 0;
                    int countB = yUp > 0 ? innerCount[xRight][yUp - 1] : 0;
                    int countC = xLeft > 0 ? innerCount[xLeft - 1][yDown] : 0;
                    int countD = innerCount[xRight][yDown];
                    int count = countD + countA - countB - countC;
                    int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
                    set(x, y, count / 1000f / area);
                }
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
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
