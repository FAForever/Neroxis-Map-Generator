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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public strictfp class BinaryMask extends Mask {
    private final Random random;
    private boolean[][] mask;

    public BinaryMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this.mask = new boolean[size][size];
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
        this.symmetrySettings = symmetrySettings;
        VisualDebugger.visualizeMask(this);
    }

    public BinaryMask(int size, Long seed, Symmetry symmetry) {
        this.mask = new boolean[size][size];
        this.random = new Random(seed);
        Symmetry spawnSymmetry;
        Symmetry teamSymmetry;
        Symmetry[] teams;
        Symmetry[] spawns;
        switch (symmetry) {
            case POINT -> {
                spawnSymmetry = symmetry;
                teams = new Symmetry[]{Symmetry.X, Symmetry.Z, Symmetry.XZ, Symmetry.ZX};
                teamSymmetry = teams[random.nextInt(teams.length)];
            }
            case QUAD -> {
                spawnSymmetry = Symmetry.POINT;
                teams = new Symmetry[]{Symmetry.X, Symmetry.Z};
                teamSymmetry = teams[random.nextInt(teams.length)];
            }
            case DIAG -> {
                spawnSymmetry = Symmetry.POINT;
                teams = new Symmetry[]{Symmetry.XZ, Symmetry.ZX};
                teamSymmetry = teams[random.nextInt(teams.length)];
            }
            default -> {
                spawnSymmetry = symmetry;
                teamSymmetry = symmetry;
            }
        }
        this.symmetrySettings = new SymmetrySettings(symmetry, teamSymmetry);
        this.symmetrySettings.setSpawnSymmetry(spawnSymmetry);
        VisualDebugger.visualizeMask(this);
    }

    public BinaryMask(BinaryMask mask, Long seed) {
        this.mask = new boolean[mask.getSize()][mask.getSize()];
        this.symmetrySettings = mask.getSymmetrySettings();
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
        for (int x = 0; x < mask.getSize(); x++) {
            for (int y = 0; y < mask.getSize(); y++) {
                this.mask[x][y] = mask.get(x, y);
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public BinaryMask(FloatMask mask, float threshold, Long seed) {
        this.mask = new boolean[mask.getSize()][mask.getSize()];
        this.symmetrySettings = mask.getSymmetrySettings();
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
        for (int x = 0; x < mask.getSize(); x++) {
            for (int y = 0; y < mask.getSize(); y++) {
                set(x, y, mask.get(x, y) >= threshold);
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

    public void set(Vector2f location, boolean value) {
        set((int) location.x, (int) location.y, value);
    }

    public void set(Vector3f location, boolean value) {
        set((int) location.x, (int) location.z, value);
    }

    public void set(int x, int y, boolean value) {
        mask[x][y] = value;
    }

    public boolean isEdge(int x, int y) {
        boolean value = get(x, y);
        return ((x > 0 && get(x - 1, y) != value)
                || (y > 0 && get(x, y - 1) != value)
                || (x < getSize() - 1 && get(x + 1, y) != value)
                || (y < getSize() - 1 && get(x, y + 1) != value));
    }

    public BinaryMask copy() {
        if (random != null) {
            return new BinaryMask(this, random.nextLong());
        } else {
            return new BinaryMask(this, null);
        }
    }

    public BinaryMask clear() {
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                set(x, y, false);
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask randomize(float density) {
        for (int x = getMinXBound(); x < getMaxXBound(); x++) {
            for (int y = getMinYBound(x); y < getMaxYBound(x); y++) {
                set(x, y, random.nextFloat() < density);
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask flipValues(float density) {
        return flipValues(density, symmetrySettings.getSpawnSymmetry());
    }

    public BinaryMask flipValues(float density, Symmetry symmetry) {
        for (int x = getMinXBound(symmetry); x < getMaxXBound(symmetry); x++) {
            for (int y = getMinYBound(x, symmetry); y < getMaxYBound(x, symmetry); y++) {
                if (get(x, y)) {
                    set(x, y, random.nextFloat() < density);
                }
            }
        }
        applySymmetry(symmetry);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask randomWalk(int numWalkers, int numSteps) {
        for (int i = 0; i < numWalkers; i++) {
            int x = random.nextInt(getMaxXBound() - getMinXBound()) + getMinXBound();
            int y = random.nextInt(getMaxYBound(x) - getMinYBound(x) + 1) + getMinYBound(x);
            for (int j = 0; j < numSteps; j++) {
                if (inBounds(x, y)) {
                    set(x, y, true);
                }
                int dir = random.nextInt(4);
                switch (dir) {
                    case 0 -> x++;
                    case 1 -> x--;
                    case 2 -> y++;
                    case 3 -> y--;
                }
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask progressiveWalk(int numWalkers, int numSteps) {
        for (int i = 0; i < numWalkers; i++) {
            int x = random.nextInt(getMaxXBound() - getMinXBound()) + getMinXBound();
            int y = random.nextInt(getMaxYBound(x) - getMinYBound(x) + 1) + getMinYBound(x);
            List<Integer> directions = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
            int regressiveDir = random.nextInt(directions.size());
            directions.remove(regressiveDir);
            for (int j = 0; j < numSteps; j++) {
                if (inBounds(x, y)) {
                    set(x, y, true);
                }
                int dir = directions.get(random.nextInt(directions.size()));
                switch (dir) {
                    case 0 -> x++;
                    case 1 -> x--;
                    case 2 -> y++;
                    case 3 -> y--;
                }
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask invert() {
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                set(x, y, !get(x, y));
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
                largeMask[x][y] = get(smallX, smallY);
            }
        }
        mask = largeMask;
        applySymmetry(symmetrySettings.getSpawnSymmetry());
        VisualDebugger.visualizeMask(this);
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
                smallMask[x][y] = get(largeX, largeY);
            }
        }
        mask = smallMask;
        applySymmetry(symmetrySettings.getSpawnSymmetry());
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask inflate(float radius) {
        boolean[][] maskCopy = new boolean[getSize()][getSize()];

        float radius2 = (radius + 0.5f) * (radius + 0.5f);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (isEdge(x, y) && get(x, y)) {
                    for (int x2 = (int) (x - radius); x2 < x + radius + 1; x2++) {
                        for (int y2 = (int) (y - radius); y2 < y + radius + 1; y2++) {
                            if (inBounds(x2, y2) && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2) {
                                maskCopy[x2][y2] = true;
                            }
                        }
                    }
                }
            }
        }

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (maskCopy[x][y]) {
                    set(x, y, true);
                }
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask deflate(float radius) {
        boolean[][] maskCopy = new boolean[getSize()][getSize()];

        float radius2 = (radius + 0.5f) * (radius + 0.5f);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (isEdge(x, y) && !get(x, y)) {
                    for (int x2 = (int) (x - radius); x2 < x + radius + 1; x2++) {
                        for (int y2 = (int) (y - radius); y2 < y + radius + 1; y2++) {
                            if (inBounds(x2, y2) && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2) {
                                maskCopy[x2][y2] = true;
                            }
                        }
                    }
                }
            }
        }

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (maskCopy[x][y]) {
                    set(x, y, false);
                }
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask cutCorners() {
        int size = getSize();
        boolean[][] maskCopy = new boolean[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int count = 0;
                if (x > 0 && !get(x - 1, y))
                    count++;
                if (y > 0 && !get(x, y - 1))
                    count++;
                if (x < size - 1 && !get(x + 1, y))
                    count++;
                if (y < size - 1 && !get(x, y + 1))
                    count++;
                if (count > 1)
                    maskCopy[x][y] = false;
                else
                    maskCopy[x][y] = get(x, y);
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask acid(float strength, float size) {
        BinaryMask holes = new BinaryMask(getSize(), random.nextLong(), getSymmetrySettings());
        holes.randomize(strength).inflate(size);
        BinaryMask maskCopy = this.copy();
        maskCopy.minus(holes);
        mask = maskCopy.getMask();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask grow(float strength) {
        return grow(strength, symmetrySettings.getTerrainSymmetry());
    }

    public BinaryMask grow(float strength, Symmetry symmetry) {
        return grow(strength, symmetry, 1);
    }

    public BinaryMask grow(float strength, Symmetry symmetry, int count) {
        for (int i = 0; i < count; i++) {
            boolean[][] maskCopy = new boolean[getSize()][getSize()];
            for (int x = getMinXBound(symmetry); x < getMaxXBound(symmetry); x++) {
                for (int y = getMinYBound(x, symmetry); y < getMaxYBound(x, symmetry); y++) {
                    if (inBounds(x, y)) {
                        boolean value = isEdge(x, y) && random.nextFloat() < strength;
                        maskCopy[x][y] = get(x, y) || value;
                    }
                }
            }
            mask = maskCopy;
        }
        applySymmetry(symmetry);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask erode(float strength) {
        return erode(strength, symmetrySettings.getTerrainSymmetry());
    }

    public BinaryMask erode(float strength, Symmetry symmetry) {
        return erode(strength, symmetry, 1);
    }

    public BinaryMask erode(float strength, Symmetry symmetry, int count) {
        for (int i = 0; i < count; i++) {
            boolean[][] maskCopy = new boolean[getSize()][getSize()];
            for (int x = getMinXBound(symmetry); x < getMaxXBound(symmetry); x++) {
                for (int y = getMinYBound(x, symmetry); y < getMaxYBound(x, symmetry); y++) {
                    if (inBounds(x, y)) {
                        boolean value = isEdge(x, y) && random.nextFloat() < strength;
                        maskCopy[x][y] = mask[x][y] && !value;
                    }
                }
            }
            mask = maskCopy;
            applySymmetry(symmetry);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask outline() {
        boolean[][] maskCopy = new boolean[getSize()][getSize()];

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                maskCopy[x][y] = isEdge(x, y);
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask smooth(int radius) {
        return smooth(radius, .5f);
    }

    public BinaryMask smooth(int radius, float density) {
        int[][] innerCount = new int[getSize()][getSize()];

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                int val = get(x, y) ? 1 : 0;
                innerCount[x][y] = val;
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
                set(x, y, count >= area * density);
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask combine(BinaryMask other) {
        int size = StrictMath.max(getSize(), other.getSize());
        if (getSize() != size)
            enlarge(size);
        if (other.getSize() != size) {
            other = other.copy().enlarge(size);
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
            other = other.copy().enlarge(size);
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
            other = other.copy().enlarge(size);
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

    public BinaryMask fillSides(int extent, boolean value) {
        return fillSides(extent, value, symmetrySettings.getSpawnSymmetry());
    }

    public BinaryMask fillSides(int extent, boolean value, Symmetry symmetry) {
        switch (symmetry) {
            case Z -> fillRect(0, 0, extent / 2, getSize(), value).fillRect(getSize() - extent / 2, 0, getSize() - extent / 2, getSize(), value);
            case X -> fillRect(0, 0, getSize(), extent / 2, value).fillRect(0, getSize() - extent / 2, getSize(), extent / 2, value);
            case XZ -> fillParallelogram(0, 0, getSize(), extent * 3 / 4, 0, -1, value).fillParallelogram(getSize() - extent * 3 / 4, getSize(), getSize(), extent * 3 / 4, 0, -1, value);
            case ZX -> fillParallelogram(getSize() - extent * 3 / 4, 0, extent * 3 / 4, extent * 3 / 4, 1, 0, value).fillParallelogram(-extent * 3 / 4, getSize() - extent * 3 / 4, extent * 3 / 4, extent * 3 / 4, 1, 0, value);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillCenter(int extent, boolean value) {
        return fillCenter(extent, value, symmetrySettings.getSpawnSymmetry());
    }

    public BinaryMask fillCenter(int extent, boolean value, Symmetry symmetry) {
        switch (symmetry) {
            case POINT -> fillCircle((float) getSize() / 2, (float) getSize() / 2, extent * 3 / 4f, value);
            case Z -> fillRect(0, getSize() / 2 - extent / 2, getSize(), extent, value);
            case X -> fillRect(getSize() / 2 - extent / 2, 0, extent, getSize(), value);
            case XZ -> fillDiagonal(extent * 3 / 4, false, value);
            case ZX -> fillDiagonal(extent * 3 / 4, true, value);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillHalf(boolean value) {
        return fillHalf(value, symmetrySettings.getTeamSymmetry());
    }

    public BinaryMask fillHalf(boolean value, Symmetry symmetry) {
        for (int x = getMinXBound(symmetry); x < getMaxXBound(symmetry); x++) {
            for (int y = getMinYBound(x, symmetry); y < getMaxYBound(x, symmetry); y++) {
                set(x, y, value);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
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
                    set(cx, cy, value);
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
                    set(calcX, calcY, value);
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
                    set(x, y, value);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask trimEdge(int rimWidth) {
        for (int a = 0; a < rimWidth; a++) {
            for (int b = 0; b < getSize() - rimWidth; b++) {
                set(a, b, false);
                set(getSize() - 1 - a, getSize() - 1 - b, false);
                set(b, getSize() - 1 - a, false);
                set(getSize() - 1 - b, a, false);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillShape(Vector2f location) {
        LinkedHashSet<Vector2f> area = new LinkedHashSet<>();
        LinkedHashSet<Vector2f> edge = new LinkedHashSet<>();
        LinkedHashSet<Vector2f> queueHash = new LinkedHashSet<>();
        LinkedList<Vector2f> queue = new LinkedList<>();
        List<int[]> edges = Arrays.asList(new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}, new int[]{1, 0});
        boolean value = get(location);
        queue.add(location);
        queueHash.add(location);
        while (queue.size() > 0) {
            Vector2f next = queue.get(0);
            queue.remove(next);
            queueHash.remove(next);
            if (get(next) == value && !area.contains(next)) {
                set(next, !value);
                area.add(next);
                edges.forEach((e) -> {
                    Vector2f newLocation = new Vector2f(next.x + e[0], next.y + e[1]);
                    if (!queueHash.contains(newLocation) && !area.contains(newLocation) && !edge.contains(newLocation) && inBounds(newLocation)) {
                        queue.add(newLocation);
                        queueHash.add(newLocation);
                    }
                });
            } else if (mask[(int) next.x][(int) next.y] != value) {
                edge.add(next);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public LinkedHashSet<Vector2f> getShapeCoordinates(Vector2f location) {
        LinkedHashSet<Vector2f> areaHash = new LinkedHashSet<>();
        LinkedHashSet<Vector2f> edgeHash = new LinkedHashSet<>();
        LinkedList<Vector2f> queue = new LinkedList<>();
        LinkedHashSet<Vector2f> queueHash = new LinkedHashSet<>();
        List<int[]> edges = Arrays.asList(new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}, new int[]{1, 0});
        boolean value = get(location);
        queue.add(location);
        queueHash.add(location);
        while (queue.size() > 0) {
            Vector2f next = queue.remove();
            queueHash.remove(next);
            if (get(next) == value && !areaHash.contains(next)) {
                areaHash.add(next);
                edges.forEach((e) -> {
                    Vector2f newLocation = new Vector2f(next.x + e[0], next.y + e[1]);
                    if (!queueHash.contains(newLocation) && !areaHash.contains(newLocation) && !edgeHash.contains(newLocation) && inBounds(newLocation)) {
                        queue.add(newLocation);
                        queueHash.add(newLocation);
                    }
                });
            } else if (get(next) != value) {
                edgeHash.add(next);
            }
        }
        return areaHash;
    }

    public BinaryMask fillCoordinates(Collection<Vector2f> coordinates, boolean value) {
        coordinates.forEach(location -> set(location, value));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillGaps(int minDist) {
        BinaryMask maskCopy = copy().outline();
        BinaryMask filledGaps = new BinaryMask(getSize(), random.nextLong(), symmetrySettings);
        LinkedHashSet<Vector2f> locHash = maskCopy.getAllCoordinatesEqualTo(true, 1);
        LinkedList<Vector2f> locList = new LinkedList<>(locHash);
        LinkedHashSet<Vector2f> toFill = new LinkedHashSet<>();
        while (locList.size() > 0) {
            Vector2f location = locList.removeFirst();
            Set<Vector2f> connected = maskCopy.getShapeCoordinates(location);
            BinaryMask otherEdgesMask = new BinaryMask(getSize(), null, symmetrySettings);
            otherEdgesMask.fillCoordinates(connected, true).inflate(minDist).intersect(maskCopy);
            Set<Vector2f> otherEdges = otherEdgesMask.getAllCoordinatesEqualTo(true, 1);
            otherEdges.removeAll(connected);
            List<Vector2f> connectedList = new LinkedList<>(connected);
            List<Vector2f> otherEdgesList = new LinkedList<>(otherEdges);
            connectedList.forEach(loc -> {
                if (loc.x > getMinXBound(symmetrySettings.getSpawnSymmetry()) && loc.x < getMaxXBound(symmetrySettings.getSpawnSymmetry())
                        && loc.y > getMinYBound((int) loc.x, symmetrySettings.getSpawnSymmetry()) && loc.y < getMaxYBound((int) loc.x, symmetrySettings.getSpawnSymmetry())) {
                    AtomicReference<Float> smallestDist = new AtomicReference<>((float) getSize());
                    AtomicReference<Vector2f> closest = new AtomicReference<>();
                    otherEdgesList.forEach(otherLoc -> {
                        if (get(otherLoc)) {
                            float dist = loc.getDistance(otherLoc);
                            if (dist < smallestDist.get()) {
                                closest.set(otherLoc);
                                smallestDist.set(dist);
                            }
                        }
                    });
                    if (smallestDist.get() < minDist) {
                        toFill.addAll(loc.getLine(closest.get()));
                        otherEdgesList.remove(closest.get());
                    }
                }
            });
            locList.removeAll(connected);
            locHash.removeAll(connected);
        }
        filledGaps.fillCoordinates(toFill, true).smooth(16, .1f);
        filledGaps.erode(.5f, symmetrySettings.getSpawnSymmetry(), 8).grow(.5f, symmetrySettings.getSpawnSymmetry(), 8);
        filledGaps.smooth(4);
        combine(filledGaps);
        applySymmetry(symmetrySettings.getSpawnSymmetry());
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask widenGaps(int minDist) {
        BinaryMask maskCopy = copy().invert().outline();
        BinaryMask filledGaps = new BinaryMask(getSize(), random.nextLong(), symmetrySettings);
        LinkedHashSet<Vector2f> locHash = maskCopy.getAllCoordinatesEqualTo(true, 1);
        LinkedList<Vector2f> locList = new LinkedList<>(locHash);
        LinkedHashSet<Vector2f> toFill = new LinkedHashSet<>();
        while (locList.size() > 0) {
            Vector2f location = locList.removeFirst();
            Set<Vector2f> connected = maskCopy.getShapeCoordinates(location);
            BinaryMask otherEdgesMask = new BinaryMask(getSize(), null, symmetrySettings);
            otherEdgesMask.fillCoordinates(connected, true).inflate(minDist).intersect(maskCopy);
            Set<Vector2f> otherEdges = otherEdgesMask.getAllCoordinatesEqualTo(true, 1);
            otherEdges.removeAll(connected);
            List<Vector2f> connectedList = new LinkedList<>(connected);
            List<Vector2f> otherEdgesList = new LinkedList<>(otherEdges);
            connectedList.forEach(loc -> {
                if (loc.x > getMinXBound(symmetrySettings.getSpawnSymmetry()) && loc.x < getMaxXBound(symmetrySettings.getSpawnSymmetry())
                        && loc.y > getMinYBound((int) loc.x, symmetrySettings.getSpawnSymmetry()) && loc.y < getMaxYBound((int) loc.x, symmetrySettings.getSpawnSymmetry())) {
                    AtomicReference<Float> smallestDist = new AtomicReference<>((float) getSize());
                    AtomicReference<Vector2f> closest = new AtomicReference<>();
                    otherEdgesList.forEach(otherLoc -> {
                        if (get(otherLoc)) {
                            float dist = loc.getDistance(otherLoc);
                            if (dist < smallestDist.get()) {
                                closest.set(otherLoc);
                                smallestDist.set(dist);
                            }
                        }
                    });
                    if (smallestDist.get() < minDist) {
                        toFill.addAll(loc.getLine(closest.get()));
                        otherEdgesList.remove(closest.get());
                    }
                }
            });
            locList.removeAll(connected);
            locHash.removeAll(connected);
        }
        filledGaps.fillCoordinates(toFill, true).smooth(16, .1f);
        filledGaps.erode(.5f, symmetrySettings.getSpawnSymmetry(), 8).grow(.5f, symmetrySettings.getSpawnSymmetry(), 8);
        filledGaps.smooth(4);
        minus(filledGaps);
        applySymmetry(symmetrySettings.getSpawnSymmetry());
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask filterShapes(int minArea) {
        BinaryMask maskCopy = copy();
        LinkedHashSet<Vector2f> locHash = getAllCoordinatesEqualTo(true, 1);
        LinkedList<Vector2f> locList = new LinkedList<>(locHash);
        while (locList.size() > 0) {
            Vector2f location = locList.removeFirst();
            Set<Vector2f> coordinates = getShapeCoordinates(location);
            maskCopy.fillCoordinates(coordinates, false);
            if (coordinates.size() < minArea) {
                fillCoordinates(coordinates, false);
            }
            locList.removeAll(coordinates);
        }
        maskCopy = copy().invert();
        locHash = getAllCoordinatesEqualTo(false, 1);
        locList = new LinkedList<>(locHash);
        while (locList.size() > 0) {
            Vector2f location = locList.removeFirst();
            Set<Vector2f> coordinates = getShapeCoordinates(location);
            maskCopy.fillCoordinates(coordinates, false);
            if (coordinates.size() < minArea) {
                fillCoordinates(coordinates, true);
            }
            locList.removeAll(coordinates);
        }
        applySymmetry(symmetrySettings.getSpawnSymmetry());
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public int getCount() {
        int cellCount = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (get(x, y))
                    cellCount++;
            }
        }
        return cellCount;
    }

    public LinkedHashSet<Vector2f> getAllCoordinates(int spacing) {
        LinkedHashSet<Vector2f> coordinates = new LinkedHashSet<>();
        for (int x = 0; x < getSize(); x += spacing) {
            for (int y = 0; y < getSize(); y += spacing) {
                Vector2f location = new Vector2f(x, y);
                coordinates.add(location);
            }
        }
        return coordinates;
    }

    public LinkedHashSet<Vector2f> getAllCoordinatesEqualTo(boolean value, int spacing) {
        LinkedHashSet<Vector2f> coordinates = new LinkedHashSet<>();
        for (int x = 0; x < getSize(); x += spacing) {
            for (int y = 0; y < getSize(); y += spacing) {
                if (get(x, y) == value) {
                    Vector2f location = new Vector2f(x, y);
                    coordinates.add(location);
                }
            }
        }
        return coordinates;
    }

    public LinkedHashSet<Vector2f> getSpacedCoordinates(float radius, int spacing) {
        LinkedHashSet<Vector2f> coordinates = getAllCoordinates(spacing);
        LinkedList<Vector2f> coordinateList = new LinkedList<>(coordinates);
        LinkedHashSet<Vector2f> chosenCoordinates = new LinkedHashSet<>();
        while (coordinates.size() > 0) {
            Vector2f location = coordinateList.removeFirst();
            chosenCoordinates.add(location);
            coordinates.removeIf((loc) -> location.getDistance(loc) < radius);
            coordinateList = new LinkedList<>(coordinates);
        }
        return chosenCoordinates;
    }

    public LinkedHashSet<Vector2f> getSpacedCoordinatesEqualTo(boolean value, float radius, int spacing) {
        LinkedHashSet<Vector2f> coordinates = getAllCoordinatesEqualTo(value, spacing);
        LinkedList<Vector2f> coordinateList = new LinkedList<>(coordinates);
        LinkedHashSet<Vector2f> chosenCoordinates = new LinkedHashSet<>();
        while (coordinates.size() > 0) {
            Vector2f location = coordinateList.removeFirst();
            chosenCoordinates.add(location);
            coordinates.removeIf((loc) -> location.getDistance(loc) < radius);
            coordinateList = new LinkedList<>(coordinates);
        }
        return chosenCoordinates;
    }

    public LinkedHashSet<Vector2f> getRandomCoordinates(float minSpacing) {
        LinkedHashSet<Vector2f> coordinates = getAllCoordinatesEqualTo(true, 1);
        ArrayList<Vector2f> coordinateArray = new ArrayList<>(coordinates);
        LinkedHashSet<Vector2f> chosenCoordinates = new LinkedHashSet<>();
        while (coordinates.size() > 0) {
            Vector2f location = coordinateArray.get(random.nextInt(coordinateArray.size()));
            chosenCoordinates.add(location);
            coordinates.removeIf((loc) -> location.getDistance(loc) < minSpacing);
            coordinates.removeIf((loc) -> getSymmetryPoint(location).getDistance(loc) < minSpacing);
            coordinateArray = new ArrayList<>(coordinates);
        }
        return chosenCoordinates;
    }

    public Vector2f getRandomPosition() {
        LinkedHashSet<Vector2f> coordinates = getAllCoordinatesEqualTo(true, 1);
        if (coordinates.size() == 0)
            return null;
        int cell = random.nextInt(coordinates.size());
        return (Vector2f) coordinates.toArray()[cell];
    }

    public void applySymmetry() {
        applySymmetry(symmetrySettings.getTerrainSymmetry());
    }

    public void applySymmetry(Symmetry symmetry) {
        applySymmetry(symmetry, false);
    }

    public void applySymmetry(boolean reverse) {
        applySymmetry(symmetrySettings.getTerrainSymmetry(), reverse);
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

    // --------------------------------------------------

    @SneakyThrows
    public void writeToFile(Path path) {
        Files.deleteIfExists(path);
        Files.createFile(path);
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path.toFile())));

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                out.writeBoolean(get(x, y));
            }
        }

        out.close();
    }

    public String toHash() throws NoSuchAlgorithmException {
        ByteBuffer bytes = ByteBuffer.allocate(getSize() * getSize());
        for (int x = getMinXBound(symmetrySettings.getSpawnSymmetry()); x < getMaxXBound(symmetrySettings.getSpawnSymmetry()); x++) {
            for (int y = getMinYBound(x, symmetrySettings.getSpawnSymmetry()); y < getMaxYBound(x, symmetrySettings.getSpawnSymmetry()); y++) {
                byte b = get(x, y) ? (byte) 1 : 0;
                bytes.put(b);
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
