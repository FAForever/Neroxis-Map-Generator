package map;

import brushes.Brushes;
import generator.VisualDebugger;
import lombok.Getter;
import lombok.SneakyThrows;
import util.Util;
import util.Vector2f;
import util.Vector3f;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static brushes.Brushes.loadBrush;

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

    public FloatMask(BufferedImage image, Long seed, SymmetryHierarchy symmetryHierarchy) {
        this.mask = new float[image.getWidth()][image.getHeight()];
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
        Raster imageData = image.getData();
        this.symmetryHierarchy = symmetryHierarchy;
        for (int y = 0; y < this.getSize(); y++) {
            for (int x = 0; x < this.getSize(); x++) {
                int[] vals = new int[1];
                imageData.getPixel(x, y, vals);
                this.mask[x][y] = vals[0] / 255f;
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

    public float get(Vector2f pos) {
        return mask[(int) pos.x][(int) pos.y];
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

    public float getSum() {
        float val = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                val += get(x, y);
            }
        }
        return val;
    }

    public float getAvg() {
        return getSum() / getSize() / getSize();
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
        other = other.copy();
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

    public FloatMask subtract(FloatMask other) {
        add(other.copy().multiply(-1));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask add(FloatMask other, Vector2f loc) {
        return add(other, (int) loc.x, (int) loc.y);
    }

    public FloatMask subtract(FloatMask other, Vector2f loc) {
        return add(other.copy().multiply(-1f), loc);
    }

    public FloatMask add(FloatMask other, int offsetX, int offsetY) {
        for (int y = 0; y < other.getSize(); y++) {
            for (int x = 0; x < other.getSize(); x++) {
                int shiftX = x - other.getSize() / 2 + offsetX;
                int shiftY = y - other.getSize() / 2 + offsetY;
                if (inBounds(shiftX, shiftY)) {
                    add(shiftX, shiftY, other.get(x, y));
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtract(FloatMask other, int offsetX, int offsetY) {
        return add(other.copy().multiply(-1f), offsetX, offsetY);
    }

    public FloatMask add(BinaryMask other, float value) {
        FloatMask otherFloat = new FloatMask(getSize(), null, symmetryHierarchy);
        otherFloat.init(other, 0, value);
        add(otherFloat);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtract(BinaryMask other, float value) {
        FloatMask otherFloat = new FloatMask(getSize(), null, symmetryHierarchy);
        otherFloat.init(other, 0, -value);
        add(otherFloat);
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

    public FloatMask shrink(int size) {
        float[][] smallMask = new float[size][size];
        int largeX;
        int largeY;
        for (int x = 0; x < size; x++) {
            largeX = (x * getSize()) / size + (getSize() / size / 2);
            if (largeX >= getSize())
                largeX = getSize() - 1;
            for (int y = 0; y < size; y++) {
                largeY = (y * getSize()) / size + (getSize() / size / 2);
                if (largeY >= getSize())
                    largeY = getSize() - 1;
                smallMask[x][y] = get(largeX, largeY);
            }
        }
        mask = smallMask;
        VisualDebugger.visualizeMask(this);
        applySymmetry(symmetryHierarchy.getTeamSymmetry());
        return this;
    }

    public FloatMask maskToHills(BinaryMask other) {
        other = other.copy();
        int size = getSize();
        if (other.getSize() < size)
            other = other.copy().enlarge(size);
        if (other.getSize() > size) {
            other = other.copy().shrink(size);
        }
        FloatMask brush = loadBrush(Brushes.HILL_BRUSHES[random.nextInt(Brushes.HILL_BRUSHES.length)], symmetryHierarchy);
        BinaryMask otherCopy = other.copy().fillHalf(false);
        LinkedHashSet<Vector2f> extents = other.copy().outline().fillHalf(false).getAllCoordinatesEqualTo(true, 1);
        LinkedList<Vector2f> coordinates = new LinkedList<>(otherCopy.getRandomCoordinates(4));
        while (coordinates.size() > 0) {
            Vector2f loc = coordinates.removeFirst();
            AtomicReference<Float> distance = new AtomicReference<>(Float.MAX_VALUE);
            extents.forEach(eloc -> distance.set(StrictMath.min(distance.get(), loc.getDistance(eloc))));
            if (distance.get() > 1) {
                FloatMask useBrush = brush.copy().shrink(distance.get().intValue() * 4).multiply(distance.get() / 2);
                add(useBrush, loc);
                add(useBrush, getSymmetryPoint(loc));
                coordinates.removeIf(cloc -> loc.getDistance(cloc) < distance.get());
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
        FloatMask brush = loadBrush(Brushes.MOUNTAIN_BRUSHES[random.nextInt(Brushes.MOUNTAIN_BRUSHES.length)], symmetryHierarchy);
        brush.multiply(1 / brush.getMax());
        BinaryMask otherCopy = other.copy().fillHalf(false);
        LinkedHashSet<Vector2f> extents = other.copy().outline().fillHalf(false).getAllCoordinatesEqualTo(true, 1);
        LinkedList<Vector2f> coordinates = new LinkedList<>(otherCopy.getRandomCoordinates(4));
        while (coordinates.size() > 0) {
            Vector2f loc = coordinates.removeFirst();
            AtomicReference<Float> distance = new AtomicReference<>(Float.MAX_VALUE);
            extents.forEach(eloc -> distance.set(StrictMath.min(distance.get(), loc.getDistance(eloc))));
            if (distance.get() > 1) {
                FloatMask useBrush = brush.copy().shrink(distance.get().intValue() * 4).multiply(distance.get());
                add(useBrush, loc);
                add(useBrush, getSymmetryPoint(loc));
                coordinates.removeIf(cloc -> loc.getDistance(cloc) < distance.get());
            }
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
        limiter = limiter.copy();
        int size = getSize();
        if (limiter.getSize() < size)
            limiter = limiter.copy().enlarge(size);
        if (limiter.getSize() > size) {
            limiter = limiter.copy().shrink(size);
        }
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
        applySymmetry(symmetryHierarchy.getTerrainSymmetry(), false);
    }

    public void applySymmetry(boolean reverse) {
        applySymmetry(symmetryHierarchy.getTerrainSymmetry(), reverse);
    }

    public void applySymmetry(Symmetry symmetry, boolean reverse) {
        switch (symmetry) {
            case QUAD, DIAG -> {
                for (int x = getMinXBound(symmetry); x < getMaxXBound(symmetry); x++) {
                    for (int y = getMinYBound(x, symmetry); y < getMaxYBound(x, symmetry); y++) {
                        Vector2f[] symPoints = getTerrainSymmetryPoints(x, y, symmetry);
                        for (Vector2f symPoint : symPoints) {
                            set(symPoint, get(x, y));
                        }
                    }
                }
            }
            default -> {
                for (int x = getMinXBound(symmetry); x < getMaxXBound(symmetry); x++) {
                    for (int y = getMinYBound(x, symmetry); y < getMaxYBound(x, symmetry); y++) {
                        Vector2f symPoint = getSymmetryPoint(x, y, symmetry);
                        if (reverse) {
                            set(x, y, get(symPoint));
                        } else {
                            set(symPoint, get(x, y));
                        }
                    }
                }
            }
        }
    }

    public void applySymmetry(float angle) {
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (inHalf(x, y, angle)) {
                    Vector2f symPoint = getSymmetryPoint(x, y, Symmetry.POINT);
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
