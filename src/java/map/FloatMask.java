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

    public FloatMask(int size, long seed, Symmetry symmetry) {
        this.symmetry = symmetry;
        mask = new float[size][size];
        height = size;
        width = size;
        for (int y = 0; y < this.getSize(); y++) {
            for (int x = 0; x < this.getSize(); x++) {
                this.mask[x][y] = 0f;
            }
        }
        random = new Random(seed);
    }

    public FloatMask(FloatMask mask, long seed) {
        this.mask = new float[mask.getSize()][mask.getSize()];
        this.symmetry = mask.getSymmetry();
        for (int y = 0; y < mask.getSize(); y++) {
            for (int x = 0; x < mask.getSize(); x++) {
                this.mask[x][y] = mask.get(x, y);
            }
        }
        random = new Random(seed);
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

    public FloatMask maskToMoutains(float firstSlope, float slope, BinaryMask other) {
        BinaryMask otherCopy = new BinaryMask(other, random.nextLong());
        FloatMask mountainBase = new FloatMask(getSize(), 0, other.getSymmetry());
        add(mountainBase.init(otherCopy, 0, firstSlope));
        otherCopy.acid(0.5f);
        for (int i = 0; i < getSize(); i++) {
            FloatMask layer = new FloatMask(getSize(), 0, other.getSymmetry());
            add(layer.init(otherCopy, 0, slope));
            otherCopy.acid(0.5f);
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask maskToHeightmap(float slope, float underWaterSlope, int maxRepeat, BinaryMask other) {
        BinaryMask otherCopy = new BinaryMask(other, random.nextLong());
        for (int i = 0; i < getSize(); i++) {
            FloatMask layer = new FloatMask(getSize(), 0, other.getSymmetry());
            add(layer.init(otherCopy, 0, slope));
            otherCopy.acid(0.5f);
        }
        otherCopy = new BinaryMask(other, random.nextLong());
        otherCopy.invert();
        for (int i = 0; i < maxRepeat; i++) {
            FloatMask layer = new FloatMask(getSize(), 0, other.getSymmetry());
            add(layer.init(otherCopy, 0, -underWaterSlope));
            otherCopy.acid(0.5f);
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

    protected void applySymmetry() {
        switch (symmetry) {
            case POINT:
            case Y:
                for (int y = 0; y < height / 2; y++) {
                    for (int x = 0; x < width; x++) {
                        Vector2f symPoint = getSymmetryPoint(x, y);
                        mask[(int) symPoint.x][(int) symPoint.y] = mask[x][y];
                    }
                }
                break;
            case X:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width / 2; x++) {
                        Vector2f symPoint = getSymmetryPoint(x, y);
                        mask[(int) symPoint.x][(int) symPoint.y] = mask[x][y];
                    }
                }
                break;
            case XY:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x <= y; x++) {
                        Vector2f symPoint = getSymmetryPoint(x, y);
                        mask[(int) symPoint.x][(int) symPoint.y] = mask[x][y];
                    }
                }
                break;
            case YX:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width - y; x++) {
                        Vector2f symPoint = getSymmetryPoint(x, y);
                        mask[(int) symPoint.x][(int) symPoint.y] = mask[x][y];
                    }
                }
                break;
            default:
                break;
        }
    }

    // -------------------------------------------

    @SneakyThrows
    public void writeToFile(Path path) {
        Files.createFile(path);
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
