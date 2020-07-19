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
public strictfp class BinaryMask implements Mask {
    private final SymmetryHierarchy symmetryHierarchy;
    private final Random random;
    private boolean[][] mask;

    public BinaryMask(int size, long seed, SymmetryHierarchy symmetryHierarchy) {
        this.mask = new boolean[size][size];
        this.random = new Random(seed);
        this.symmetryHierarchy = symmetryHierarchy;
    }

    public BinaryMask(int size, long seed, Symmetry symmetry) {
        this.mask = new boolean[size][size];
        this.random = new Random(seed);
        Symmetry spawnSymmetry;
        Symmetry teamSymmetry;
        Symmetry[] teams;
        Symmetry[] spawns;
        switch (symmetry) {
            case POINT:
                spawnSymmetry = symmetry;
                teams = new Symmetry[]{Symmetry.X, Symmetry.Y};
                teamSymmetry = teams[random.nextInt(teams.length)];
                break;
            case QUAD:
                spawns = new Symmetry[]{Symmetry.X, Symmetry.Y, Symmetry.POINT};
                spawnSymmetry = spawns[random.nextInt(spawns.length)];
                if (spawnSymmetry == Symmetry.POINT) {
                    teams = new Symmetry[]{Symmetry.X, Symmetry.Y};
                    teamSymmetry = teams[random.nextInt(teams.length)];
                } else {
                    teamSymmetry = spawnSymmetry;
                }
                break;
            case DIAG:
                spawns = new Symmetry[]{Symmetry.XY, Symmetry.YX, Symmetry.POINT};
                spawnSymmetry = spawns[random.nextInt(spawns.length)];
                if (spawnSymmetry == Symmetry.POINT) {
                    teams = new Symmetry[]{Symmetry.XY, Symmetry.YX};
                    teamSymmetry = teams[random.nextInt(teams.length)];
                } else {
                    teamSymmetry = spawnSymmetry;
                }
                break;
            case X:
            case Y:
            case XY:
            case YX:
            default:
                spawnSymmetry = symmetry;
                teamSymmetry = symmetry;
                break;
        }
        this.symmetryHierarchy = new SymmetryHierarchy(symmetry, teamSymmetry);
        this.symmetryHierarchy.setSpawnSymmetry(spawnSymmetry);
    }

    public BinaryMask(BinaryMask mask, long seed) {
        this.mask = new boolean[mask.getSize()][mask.getSize()];
        this.symmetryHierarchy = mask.getSymmetryHierarchy();
        this.random = new Random(seed);
        for (int y = 0; y < mask.getSize(); y++) {
            for (int x = 0; x < mask.getSize(); x++) {
                this.mask[x][y] = mask.get(x, y);
            }
        }

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
        applySymmetry(symmetryHierarchy.getTeamSymmetry());
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
        applySymmetry(symmetryHierarchy.getTeamSymmetry());
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
        return acid(strength, symmetryHierarchy.getTerrainSymmetry());
    }

    public BinaryMask acid(float strength, Symmetry symmetry) {
        boolean[][] maskCopy = new boolean[getSize()][getSize()];

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                boolean value = (((x > 0 && !mask[x - 1][y]) || (y > 0 && !mask[x][y - 1])
                        || (x < getSize() - 1 && !mask[x + 1][y])
                        || (y < getSize() - 1 && !mask[x][y + 1]))
                        && random.nextFloat() < strength);
                maskCopy[x][y] = mask[x][y] && !value;
            }
        }
        mask = maskCopy;
        applySymmetry(symmetry);
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
        return fillCenter(extent, value, symmetryHierarchy.getSpawnSymmetry());
    }

    public BinaryMask fillCenter(int extent, boolean value, Symmetry symmetry) {
        switch (symmetry) {
            case POINT:
                return fillCircle((float) getSize() / 2, (float) getSize() / 2, extent, value);
            case Y:
                return fillRect(0, getSize() / 2 - extent, getSize(), extent * 2, value);
            case X:
                return fillRect(getSize() / 2 - extent, 0, extent * 2, getSize(), value);
            case XY:
                return fillDiagonal(extent, false, value);
            case YX:
                return fillDiagonal(extent, true, value);
            default:
                return null;
        }
    }

    public BinaryMask fillHalf(boolean value) {
        return fillHalf(value, symmetryHierarchy.getTeamSymmetry());
    }

    public BinaryMask fillHalf(boolean value, Symmetry symmetry) {
        switch (symmetry) {
            case Y:
                return fillRect(0, 0, getSize(), getSize() / 2, value);
            case X:
                return fillRect(0, 0, getSize() / 2, getSize(), value);
            case XY:
                return fillParallelogram(0, 0, getSize(), getSize(), 0, 1, value);
            case YX:
                return fillParallelogram(0, 0, getSize(), getSize(), 0, -1, value);
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
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int calcX = x + px + py * xSlope;
                int calcY = y + py + px * ySlope;
                if (calcX >= 0 && calcX < getSize() && calcY >= 0 && calcY < getSize()) {
                    mask[calcX][calcY] = value;
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillDiagonal(int extent, boolean inverted, boolean value) {
        int count = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int cx = -extent; cx < extent; cx++) {
                int x;
                if (inverted) {
                    x = getSize() - (cx + count);
                } else {
                    x = cx + count;
                }
                if (x >= 0 && x < getSize()) {
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

    public int getCount() {
        int cellCount = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (mask[x][y])
                    cellCount++;
            }
        }
        return cellCount;
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

    public Vector2f getSymmetryPoint(Vector2f v) {
        return getSymmetryPoint((int) v.x, (int) v.y);
    }

    public Vector2f getSymmetryPoint(Vector2f v, Symmetry symmetry) {
        return getSymmetryPoint((int) v.x, (int) v.y, symmetry);
    }

    public Vector2f getSymmetryPoint(int x, int y) {
        return getSymmetryPoint(x, y, symmetryHierarchy.getSpawnSymmetry());
    }

    public Vector2f getSymmetryPoint(int x, int y, Symmetry symmetry) {
        switch (symmetry) {
            case POINT:
                return new Vector2f(getSize() - x - 1, getSize() - y - 1);
            case X:
                return new Vector2f(getSize() - x - 1, y);
            case Y:
                return new Vector2f(x, getSize() - y - 1);
            case XY:
                return new Vector2f(y, x);
            case YX:
                return new Vector2f(getSize() - y - 1, getSize() - x - 1);
            default:
                return null;
        }
    }

    public int getMinXBound(Symmetry symmetry) {
        switch (symmetry) {
            default:
                return 0;
        }
    }

    public int getMaxXBound(Symmetry symmetry) {
        switch (symmetry) {
            case X:
            case QUAD:
            case DIAG:
                return getSize() / 2;
            default:
                return getSize();
        }
    }

    public int getMinYBound(int x, Symmetry symmetry) {
        switch (symmetry) {
            case DIAG:
                return x;
            default:
                return 0;
        }
    }

    public int getMaxYBound(int x, Symmetry symmetry) {
        switch (symmetry) {
            case X:
                return getSize();
            case XY:
                return x + 1;
            case YX:
            case DIAG:
                return getSize() - x;
            default:
                return getSize() / 2;
        }
    }

    public Vector2f[] getTerrainSymmetryPoints(int x, int y, Symmetry symmetry) {
        Vector2f[] symmetryPoints;
        Vector2f symPoint1;
        Vector2f symPoint2;
        Vector2f symPoint3;
        switch (symmetry) {
            case POINT:
            case Y:
            case X:
            case XY:
            case YX:
                symPoint1 = getSymmetryPoint(x, y);
                symmetryPoints = new Vector2f[]{symPoint1};
                break;
            case QUAD:
                symPoint1 = getSymmetryPoint(x, y, Symmetry.Y);
                symPoint2 = getSymmetryPoint(x, y, Symmetry.X);
                symPoint3 = getSymmetryPoint(symPoint1, Symmetry.X);
                symmetryPoints = new Vector2f[]{symPoint1, symPoint2, symPoint3};
                break;
            case DIAG:
                symPoint1 = getSymmetryPoint(x, y, Symmetry.YX);
                symPoint2 = getSymmetryPoint(x, y, Symmetry.XY);
                symPoint3 = getSymmetryPoint(symPoint1, Symmetry.XY);
                symmetryPoints = new Vector2f[]{symPoint1, symPoint2, symPoint3};
                break;
            default:
                symmetryPoints = null;
                break;
        }
        return symmetryPoints;
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
