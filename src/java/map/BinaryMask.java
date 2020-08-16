package map;

import generator.VisualDebugger;
import lombok.Getter;
import lombok.SneakyThrows;
import util.Vector2f;
import util.Vector3f;

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

    public BinaryMask(int size, long seed, SymmetryHierarchy symmetryHierarchy) {
        this.mask = new boolean[size][size];
        this.random = new Random(seed);
        this.symmetryHierarchy = symmetryHierarchy;
        VisualDebugger.visualizeMask(this);
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
        VisualDebugger.visualizeMask(this);
    }

    public BinaryMask(BinaryMask mask, long seed) {
        this.mask = new boolean[mask.getSize()][mask.getSize()];
        this.symmetryHierarchy = mask.getSymmetryHierarchy();
        this.random = new Random(seed);
        for (int x = 0; x < mask.getSize(); x++) {
            for (int y = 0; y < mask.getSize(); y++) {
                this.mask[x][y] = mask.get(x, y);
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public int getSize() {
        return mask[0].length;
    }

    public boolean get(Vector2f location) {
        return get((int) location.x, (int) location.y);
    }

    public boolean get(Vector3f location) {
        return get((int) location.x, (int) location.z);
    }

    public boolean get(int x, int y) {
        return mask[x][y];
    }

    public BinaryMask copy() {
        return new BinaryMask(this, random.nextLong());
    }

    public BinaryMask randomize(float density) {
        for (int x = getMinXBound(symmetryHierarchy.getTerrainSymmetry()); x < getMaxXBound(symmetryHierarchy.getTerrainSymmetry()); x++) {
            for (int y = getMinYBound(x, symmetryHierarchy.getTerrainSymmetry()); y < getMaxYBound(x, symmetryHierarchy.getTerrainSymmetry()); y++) {
                mask[x][y] = random.nextFloat() < density;
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask invert() {
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
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
        for (int x = 0; x < size; x++) {
            smallX = StrictMath.min(x / (size / getSize()), getSize() - 1);
            for (int y = 0; y < size; y++) {
                smallY = StrictMath.min(y / (size / getSize()), getSize() - 1);
                largeMask[x][y] = mask[smallX][smallY];
            }
        }
        mask = largeMask;
        VisualDebugger.visualizeMask(this);
        applySymmetry(symmetryHierarchy.getTeamSymmetry());
        return this;
    }

    public BinaryMask shrink(int size) {
        boolean[][] smallMask = new boolean[size][size];
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
                smallMask[x][y] = mask[largeX][largeY];
            }
        }
        mask = smallMask;
        VisualDebugger.visualizeMask(this);
        applySymmetry(symmetryHierarchy.getTeamSymmetry());
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
        for (int x = 0; x < getSize(); x++) {
            for (int y = startY; y < endY; y++) {
                maskCopy[x][y] = !inverted;
                l:
                for (int x2 = (int) (x - radius); x2 < x + radius + 1; x2++) {
                    for (int y2 = (int) (y - radius); y2 < y + radius + 1; y2++) {
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
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
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

    public BinaryMask acid(float strength, float size) {
        BinaryMask holes = new BinaryMask(getSize(), random.nextLong(), getSymmetryHierarchy());
        holes.randomize(strength).inflate(size);
        BinaryMask maskCopy = this.copy();
        maskCopy.minus(holes);
        mask = maskCopy.getMask();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask erode(float strength) {
        return erode(strength, symmetryHierarchy.getTerrainSymmetry());
    }

    public BinaryMask erode(float strength, Symmetry symmetry) {
        return erode(strength, symmetry, 1);
    }

    public BinaryMask erode(float strength, Symmetry symmetry, int count) {
        boolean[][] maskCopy = new boolean[getSize()][getSize()];

        for (int i = 0; i < count; i++) {
            for (int x = getMinXBound(symmetry) - 1; x < getMaxXBound(symmetry) + 1; x++) {
                for (int y = getMinYBound(x, symmetry) - 1; y < getMaxYBound(x, symmetry) + 1; y++) {
                    if (x >= 0 && y >=0 && x < getSize() && y < getSize()) {
                        boolean value = (((x > 0 && !mask[x - 1][y]) || (y > 0 && !mask[x][y - 1])
                                || (x < getSize() - 1 && !mask[x + 1][y])
                                || (y < getSize() - 1 && !mask[x][y + 1]))
                                && random.nextFloat() < strength);
                        maskCopy[x][y] = mask[x][y] && !value;
                    }
                }
            }
            mask = maskCopy;
        }
        applySymmetry(symmetry);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask outline() {
        boolean[][] maskCopy = new boolean[getSize()][getSize()];

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
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
        return smooth(radius, .5f);
    }

    public BinaryMask smooth(float radius, float density) {
        boolean[][] maskCopy = new boolean[getSize()][getSize()];

        Thread[] threads = new Thread[4];
        threads[0] = new Thread(() -> smoothRegion(radius, density, maskCopy, 0, (getSize() / 4)));
        threads[1] = new Thread(() -> smoothRegion(radius, density, maskCopy, (getSize() / 4), (getSize() / 2)));
        threads[2] = new Thread(() -> smoothRegion(radius, density, maskCopy, (getSize() / 2), (getSize() / 4) * 3));
        threads[3] = new Thread(() -> smoothRegion(radius, density, maskCopy, (getSize() / 4) * 3, getSize()));

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

    private void smoothRegion(float radius, float density, boolean[][] maskCopy, int startY, int endY) {
        float radius2 = (radius + 0.5f) * (radius + 0.5f);
        for (int x = 0; x < getSize(); x++) {
            for (int y = startY; y < endY; y++) {
                int count = 0;
                int count2 = 0;
                for (int x2 = (int) (x - radius); x2 <= x + radius; x2++) {
                    for (int y2 = (int) (y - radius); y2 <= y + radius; y2++) {
                        if (x2 > 0 && y2 > 0 && x2 < getSize() && y2 < getSize() && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2) {
                            count++;
                            if (mask[x2][y2])
                                count2++;
                        }
                    }
                }
                if (count2 > count * density) {
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
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
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
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
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
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
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
                return fillCircle((float) getSize() / 2, (float) getSize() / 2, extent * 3 / 4f, value);
            case Y:
                return fillRect(0, getSize() / 2 - extent / 2, getSize(), extent, value);
            case X:
                return fillRect(getSize() / 2 - extent / 2, 0, extent, getSize(), value);
            case XY:
                return fillDiagonal(extent * 3 / 4, false, value);
            case YX:
                return fillDiagonal(extent * 3 / 4, true, value);
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

    public BinaryMask fillCircle(Vector3f v, float radius, boolean value) {
        return fillCircle(new Vector2f(v), radius, value);
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
        for (int cx = (int) StrictMath.max(0, x - radius); cx < ex; cx++) {
            for (int cy = (int) StrictMath.max(0, y - radius); cy < ey; cy++) {
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
        for (int px = 0; px < width; px++) {
            for (int py = 0; py < height; py++) {
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
        for (int cx = -extent; cx < extent; cx++) {
            for (int y = 0; y < getSize(); y++) {
                int x;
                if (inverted) {
                    x = getSize() - (cx + y);
                } else {
                    x = cx + y;
                }
                if (x >= 0 && x < getSize()) {
                    mask[x][y] = value;
                }
            }
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
        int cellCount = getCount();
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
