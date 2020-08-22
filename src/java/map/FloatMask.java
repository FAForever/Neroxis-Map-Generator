package map;

import generator.VisualDebugger;
import lombok.Getter;
import lombok.SneakyThrows;
import util.Vector2f;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public FloatMask init(BinaryMask other, float low, float high) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (other.get(x, y)) {
                    mask[x][y] = high;
                } else {
                    mask[x][y] = low;
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
                mask[x][y] += other.get(x, y);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask max(FloatMask other) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                mask[x][y] = StrictMath.max(mask[x][y], other.get(x, y));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask maskToMountains(BinaryMask other) {
        BinaryMask otherCopy = other.copy();
        FloatMask mountainBase = new FloatMask(getSize(), 0, symmetryHierarchy);
        add(mountainBase.init(otherCopy, 0, 2f));
        while (otherCopy.getCount() > 0) {
            FloatMask layer = new FloatMask(getSize(), 0, symmetryHierarchy);
            add(layer.init(otherCopy, 0, random.nextFloat() * .75f));
//            add(layer.init(other.copy().acid(.1f, 2f), 0, random.nextFloat() * .15f));
            otherCopy.erode(random.nextFloat(), symmetryHierarchy.getSpawnSymmetry());
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask erodeMountains(BinaryMask other) {
        BinaryMask otherCopy = other.copy().fillHalf(false);
        BinaryMask otherEdge = other.copy().inflate(10);
        float velocity = 1f;
        float inertia = .75f;
        float capacity = 5f;
        float deposition = .25f;
        float erosion = .25f;
        float gravity = 1f;
        float evaporation = .05f;
        for (int i = 0; i < 1; i++) {
            Vector2f pos = otherCopy.getRandomPosition();
            float x = 227;
            float y = 133;
            float dx = 0;
            float dy = 0;
            int radius = 1;
            float sediment = 0;
            float water = 1f;
            float[][] maskCopy = this.copy().mask;
            for (int j = 0; j < 100; j++){
                int roundX = StrictMath.round(x);
                int roundY = StrictMath.round(y);
                if (roundX < 2 || roundX > getSize() - 2 || roundY < 2 || roundY > getSize() - 2 || !otherEdge.get(roundX, roundY)) {
                    break;
                }
                float gx = get(roundX - 1, roundY) - get(roundX + 1, roundY);
                float gy = get(roundX, roundY - 1) - get(roundX, roundY + 1);
                float mag = (float) StrictMath.sqrt(gx * gx + gy * gy);
                float height = get(roundX, roundY);
                if (mag > 0) {
                    gx = gx / mag;
                    gy = gy / mag;
                    dx = dx * inertia + gx * (1 - inertia);
                    dy = dy * inertia + gy * (1 - inertia);
                }
                float xNew = x + dx;
                float yNew = y + dy;
                int roundXNew = StrictMath.round(xNew);
                int roundYNew = StrictMath.round(yNew);
                float heightNew = get(roundXNew, roundYNew);
                float heightDif = heightNew - height;
                if (heightDif < 0) {
                    float carry = -heightDif * velocity * water * capacity;
                    if (sediment < carry) {
                        float change = StrictMath.min((carry - sediment) * erosion, -heightDif);
                        maskCopy[roundX][roundY] -= change;
                        sediment += change;
                    } else {
                        float change = StrictMath.min((sediment - carry) * deposition, -heightDif);
                        maskCopy[roundX][roundY] += change;
                        sediment -= change;
                    }
                } else {
                    float change = StrictMath.min(sediment, heightDif);
                    sediment -= change;
                    maskCopy[roundX][roundY] += change;
                }
                velocity = (float) StrictMath.sqrt(StrictMath.max(velocity * velocity - heightDif * gravity, 0));
                if (velocity == 0) {
                    break;
                }
                water *= (1 - evaporation);
                x = xNew;
                y = yNew;
            }
            mask = maskCopy;
        }
//        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask maskToHeightmap(float underWaterSlope, int maxRepeat, BinaryMask other) {
        BinaryMask otherCopy = new BinaryMask(other, random.nextLong());
        otherCopy.invert();
        int count = 0;
        while (otherCopy.getCount() > 0 && count < maxRepeat) {
            count++;
            FloatMask layer = new FloatMask(getSize(), 0, symmetryHierarchy);
            add(layer.init(otherCopy, 0, -underWaterSlope));
            otherCopy.erode(0.5f, symmetryHierarchy.getSpawnSymmetry());
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
                        if (x2 > 0 && y2 > 0 && x2 < getSize() && y2 < getSize() && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2) {
                            count++;
                            avg += mask[x2][y2];
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
                            if (x2 > 0 && y2 > 0 && x2 < getSize() && y2 < getSize() && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2) {
                                count++;
                                avg += mask[x2][y2];
                            }
                        }
                    }
                    maskCopy[x][y] = avg / count;
                } else {
                    maskCopy[x][y] = mask[x][y];
                }
            }
        }
    }

    public FloatMask gradient() {
        float[][] maskCopy = new float[getSize()][getSize()];
        for (int x = getMinXBound(); x < getMaxXBound(); x++) {
            for (int y = getMinYBound(x); y < getMaxYBound(x); y++) {
                int xNeg = StrictMath.max(0,x - 1);
                int xPos = StrictMath.min(getSize() - 1, x + 1);
                int yNeg = StrictMath.max(0,y - 1);
                int yPos = StrictMath.min(getSize() - 1, y + 1);
                float xSlope = get(xPos, y) - get(xNeg, y);
                float ySlope = get(x, yPos) - get(x, yNeg);
                maskCopy[x][y] = (float) StrictMath.sqrt(xSlope * xSlope + ySlope * ySlope);
            }
        }
        mask = maskCopy;
        applySymmetry();
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
                    mask[(int) symPoint.x][(int) symPoint.y] = mask[x][y];
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
                out.writeFloat(mask[x][y]);
            }
        }

        out.close();
    }

    public void startVisualDebugger() {
        VisualDebugger.whitelistMask(this);
    }

    public void startVisualDebugger(String maskName) {
        VisualDebugger.whitelistMask(this, maskName);
    }
}
