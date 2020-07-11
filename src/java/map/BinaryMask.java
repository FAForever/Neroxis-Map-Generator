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
public strictfp class BinaryMask extends Mask {
    private final Random random;
    private boolean[][] mask;

    public BinaryMask(int size, long seed, Symmetry symmetry) {
        this.symmetry = symmetry;
        mask = new boolean[size][size];
        height = size;
        width = size;
        for (int y = 0; y < this.getSize(); y++) {
            for (int x = 0; x < this.getSize(); x++) {
                this.mask[x][y] = false;
            }
        }
        random = new Random(seed);
    }

    public BinaryMask(BinaryMask mask, long seed) {
        this.mask = new boolean[mask.getSize()][mask.getSize()];
        this.symmetry = mask.getSymmetry();
        height = this.mask[0].length;
        width = this.mask.length;
        for (int y = 0; y < mask.getSize(); y++) {
            for (int x = 0; x < mask.getSize(); x++) {
                this.mask[x][y] = mask.get(x, y);
            }
        }
        random = new Random(seed);
    }

    public BinaryMask(boolean[][] mask, long seed, Symmetry symmetry) {
        this.mask = mask;
        this.symmetry = symmetry;
        height = this.mask[0].length;
        width = this.mask.length;
        random = new Random(seed);
    }

    public int getSize() {
        return mask[0].length;
    }

    public boolean get(int x, int y) {
        return mask[x][y];
    }

    public BinaryMask randomize(float density) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                mask[x][y] = random.nextFloat() < density;
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask invert() {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                mask[x][y] = !mask[x][y];
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask enlarge(int size) {
        boolean[][] largeMask = new boolean[size][size];
        int smallX;
        int smallY;
        for (int y = 0; y < size; y++) {
            smallY = StrictMath.min(y / (size / getSize()), getSize() - 1);
            for (int x = 0; x < size; x++) {
                smallX = StrictMath.min(x / (size / getSize()), getSize() - 1);
                largeMask[x][y] = mask[smallX][smallY];
            }
        }
        mask = largeMask;
        height = this.getSize();
        width = this.getSize();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask shrink(int size) {
        boolean[][] smallMask = new boolean[size][size];
        int largeX;
        int largeY;
        for (int y = 0; y < size; y++) {
            largeY = (y * getSize()) / size + (getSize() / size / 2);
            if (largeY >= getSize())
                largeY = getSize() - 1;
            for (int x = 0; x < size; x++) {
                largeX = (x * getSize()) / size + (getSize() / size / 2);
                if (largeX >= getSize())
                    largeX = getSize() - 1;
                smallMask[x][y] = mask[largeX][largeY];
            }
        }
        mask = smallMask;
        height = this.getSize();
        width = this.getSize();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask inflate(float radius) {
        return deflate(-radius);
    }

    public BinaryMask deflate(float radius) {
        boolean[][] maskCopy = new boolean[getSize()][getSize()];

        boolean inverted = radius <= 0;

        Thread[] threads = new Thread[4];
        threads[0] = new Thread(() -> deflateRegion(inverted, StrictMath.abs(radius), maskCopy, 0, (getSize() / 4)));
        threads[1] = new Thread(() -> deflateRegion(inverted, StrictMath.abs(radius), maskCopy, (getSize() / 4), (getSize() / 2)));
        threads[2] = new Thread(() -> deflateRegion(inverted, StrictMath.abs(radius), maskCopy, (getSize() / 2), (getSize() / 4) * 3));
        threads[3] = new Thread(() -> deflateRegion(inverted, StrictMath.abs(radius), maskCopy, (getSize() / 4) * 3, getSize()));
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

    private void deflateRegion(boolean inverted, float radius, boolean[][] maskCopy, int startY, int endY) {
        float radius2 = (radius + 0.5f) * (radius + 0.5f);
        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < getSize(); x++) {
                maskCopy[x][y] = !inverted;
                l:
                for (int y2 = (int) (y - radius); y2 < y + radius + 1; y2++) {
                    for (int x2 = (int) (x - radius); x2 < x + radius + 1; x2++) {
                        if (x2 >= 0 && y2 >= 0 && x2 < getSize() && y2 < getSize() && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2 && inverted ^ !mask[x2][y2]) {
                            maskCopy[x][y] = inverted;
                            break l;
                        }
                    }
                }
            }
        }
    }

    public BinaryMask cutCorners() {
        int size = mask[0].length;
        boolean[][] maskCopy = new boolean[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int count = 0;
                if (x > 0 && !mask[x - 1][y])
                    count++;
                if (y > 0 && !mask[x][y - 1])
                    count++;
                if (x < size - 1 && !mask[x + 1][y])
                    count++;
                if (y < size - 1 && !mask[x][y + 1])
                    count++;
                if (count > 1)
                    maskCopy[x][y] = false;
                else
                    maskCopy[x][y] = mask[x][y];
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask acid(float strength) {
        boolean[][] maskCopy = new boolean[getSize()][getSize()];

        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (((x > 0 && !mask[x - 1][y]) || (y > 0 && !mask[x][y - 1]) || (x < getSize() - 1 && !mask[x + 1][y]) || (y < getSize() - 1 && !mask[x][y + 1])) && random.nextFloat() < strength)
                    maskCopy[x][y] = false;
                else
                    maskCopy[x][y] = mask[x][y];
            }
        }
        mask = maskCopy;
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask outline() {
        boolean[][] maskCopy = new boolean[getSize()][getSize()];

        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                maskCopy[x][y] = ((x > 0 && !mask[x - 1][y])
                        || (y > 0 && !mask[x][y - 1])
                        || (x < getSize() - 1 && !mask[x + 1][y])
                        || (y < getSize() - 1 && !mask[x][y + 1]))
                        && ((x > 0 && mask[x - 1][y])
                        || (y > 0 && mask[x][y - 1])
                        || (x < getSize() - 1 && mask[x + 1][y])
                        || (y < getSize() - 1 && mask[x][y + 1]));
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask smooth(float radius) {
        boolean[][] maskCopy = new boolean[getSize()][getSize()];

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

    private void smoothRegion(float radius, boolean[][] maskCopy, int startY, int endY) {
        float radius2 = (radius + 0.5f) * (radius + 0.5f);
        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < getSize(); x++) {
                int count = 0;
                int count2 = 0;
                for (int y2 = (int) (y - radius); y2 <= y + radius; y2++) {
                    for (int x2 = (int) (x - radius); x2 <= x + radius; x2++) {
                        if (x2 > 0 && y2 > 0 && x2 < getSize() && y2 < getSize() && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2) {
                            count++;
                            if (mask[x2][y2])
                                count2++;
                        }
                    }
                }
                if (count2 > count / 2) {
                    maskCopy[x][y] = true;
                }
            }
        }
    }

    public BinaryMask combine(BinaryMask other) {
        int size = StrictMath.max(getSize(), other.getSize());
        if (getSize() != size)
            enlarge(size);
        if (other.getSize() != size) {
            other = new BinaryMask(other, 0);
            other.enlarge(size);
        }
        boolean[][] maskCopy = new boolean[getSize()][getSize()];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                maskCopy[x][y] = get(x, y) || other.get(x, y);
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask intersect(BinaryMask other) {
        int size = StrictMath.max(getSize(), other.getSize());
        if (getSize() != size)
            enlarge(size);
        if (other.getSize() != size)
            other.enlarge(size);
        boolean[][] maskCopy = new boolean[getSize()][getSize()];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                maskCopy[x][y] = get(x, y) && other.get(x, y);
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask minus(BinaryMask other) {
        int size = StrictMath.max(getSize(), other.getSize());
        if (getSize() != size)
            enlarge(size);
        if (other.getSize() != size)
            other.enlarge(size);
        boolean[][] maskCopy = new boolean[getSize()][getSize()];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                maskCopy[x][y] = get(x, y) && !other.get(x, y);
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillCenter(int extent, boolean value) {
        switch (symmetry) {
            case POINT:
                return fillCircle((float) width / 2, (float) height / 2, extent, value);
            case Y:
                return fillRect(0, height / 2 - extent, width, extent * 2, value);
            case X:
                return fillRect(width / 2 - extent, 0, extent * 2, height, value);
            case XY:
                return fillDiagonal(extent, false, value);
            case YX:
                return fillDiagonal(extent, true, value);
            default:
                return null;
        }
    }

    public BinaryMask fillCircle(Vector2f v, float radius, boolean value) {
        return fillCircle(v.x, v.y, radius, value);
    }

    public BinaryMask fillCircle(float x, float y, float radius, boolean value) {
        int ex = (int) StrictMath.min(getSize(), x + radius);
        int ey = (int) StrictMath.min(getSize(), y + radius);
        float dx;
        float dy;
        float radius2 = radius * radius;
        for (int cy = (int) StrictMath.max(0, y - radius); cy < ey; cy++) {
            for (int cx = (int) StrictMath.max(0, x - radius); cx < ex; cx++) {
                dx = x - cx;
                dy = y - cy;
                if (dx * dx + dy * dy <= radius2) {
                    mask[cx][cy] = value;
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillRect(Vector2f v, int width, int height, boolean value) {
        return fillRect((int) v.x, (int) v.y, width, height, value);
    }

    public BinaryMask fillRect(int x, int y, int width, int height, boolean value) {
        return fillParallelogram(x, y, width, height, 0, 0, value);
    }

    public BinaryMask fillParallelogram(Vector2f v, int width, int height, int xSlope, int ySlope, boolean value) {
        return fillParallelogram((int) v.x, (int) v.y, width, height, xSlope, ySlope, value);
    }

    public BinaryMask fillParallelogram(int x, int y, int width, int height, int xSlope, int ySlope, boolean value) {
        int countY = 0;
        for (int cy = y; cy < height + y; cy++) {
            int countX = 0;
            int calcY = cy + countX * ySlope;
            for (int cx = x; cx < width + x; cx++) {
                int calcX = cx + countY * xSlope;
                if (calcX >= 0 && calcX < this.width && calcY >= 0 && calcY < this.height) {
                    mask[calcX][calcY] = value;
                }
                countX++;
            }
            countY++;
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillDiagonal(int extent, boolean inverted, boolean value) {
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int cx = -extent; cx < extent; cx++) {
                int x;
                if (inverted) {
                    x = width - (cx + count);
                } else {
                    x = cx + count;
                }
                if (x >= 0 && x < width) {
                    mask[x][y] = value;
                }
            }
            count++;
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask trimEdge(int rimWidth) {
        for (int a = 0; a < rimWidth; a++) {
            for (int b = 0; b < getSize() - rimWidth; b++) {
                mask[a][b] = false;
                mask[getSize() - 1 - a][getSize() - 1 - b] = false;
                mask[b][getSize() - 1 - a] = false;
                mask[getSize() - 1 - b][a] = false;
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public Vector2f getRandomPosition() {
        int cellCount = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (mask[x][y])
                    cellCount++;
            }
        }
        if (cellCount == 0)
            return null;
        int cell = random.nextInt(cellCount) + 1;
        cellCount = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (mask[x][y])
                    cellCount++;
                if (cellCount == cell)
                    return new Vector2f(x, y);
            }
        }
        return null;
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
                    for (int x = 0; x < width; x++) {
                        Vector2f symPoint = getSymmetryPoint(x, y);
                        mask[(int) symPoint.x][(int) symPoint.y] = mask[x][y];
                    }
                }
                break;
            default:
                break;
        }
    }

    // --------------------------------------------------

    @SneakyThrows
    public void writeToFile(Path path) {
        Files.createFile(path);
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path.toFile())));

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                out.writeBoolean(mask[x][y]);
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
