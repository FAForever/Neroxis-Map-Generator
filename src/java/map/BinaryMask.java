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

import static brushes.Brushes.loadBrush;

@Getter
public strictfp class BinaryMask extends Mask<Boolean> {
    public BinaryMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        super(seed);
        this.mask = getEmptyMask(size);
        this.symmetrySettings = symmetrySettings;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                this.mask[x][y] = false;
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public BinaryMask(BinaryMask binaryMask, Long seed) {
        super(seed);
        this.mask = getEmptyMask(binaryMask.getSize());
        this.symmetrySettings = binaryMask.getSymmetrySettings();
        for (int x = 0; x < binaryMask.getSize(); x++) {
            for (int y = 0; y < binaryMask.getSize(); y++) {
                this.mask[x][y] = binaryMask.getValueAt(x, y);
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public BinaryMask(FloatMask floatMask, float minValue, Long seed) {
        super(seed);
        this.mask = getEmptyMask(floatMask.getSize());
        this.symmetrySettings = floatMask.getSymmetrySettings();
        for (int x = 0; x < floatMask.getSize(); x++) {
            for (int y = 0; y < floatMask.getSize(); y++) {
                setValueAt(x, y, floatMask.getValueAt(x, y) >= minValue);
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public BinaryMask(FloatMask floatMask, float minValue, float maxValue, Long seed) {
        super(seed);
        this.mask = getEmptyMask(floatMask.getSize());
        this.symmetrySettings = floatMask.getSymmetrySettings();
        for (int x = 0; x < floatMask.getSize(); x++) {
            for (int y = 0; y < floatMask.getSize(); y++) {
                setValueAt(x, y, floatMask.getValueAt(x, y) >= minValue && floatMask.getValueAt(x, y) < maxValue);
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    @Override
    protected Boolean[][] getEmptyMask(int size) {
        Boolean[][] empty = new Boolean[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                empty[x][y] = false;
            }
        }
        return empty;
    }

    public boolean isEdge(int x, int y) {
        boolean value = getValueAt(x, y);
        return ((x > 0 && getValueAt(x - 1, y) != value)
                || (y > 0 && getValueAt(x, y - 1) != value)
                || (x < getSize() - 1 && getValueAt(x + 1, y) != value)
                || (y < getSize() - 1 && getValueAt(x, y + 1) != value));
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
                setValueAt(x, y, false);
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask randomize(float density) {
        for (int x = getMinXBound(); x < getMaxXBound(); x++) {
            for (int y = getMinYBound(x); y < getMaxYBound(x); y++) {
                setValueAt(x, y, random.nextFloat() < density);
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask flipValues(float density) {
        return flipValues(density, SymmetryType.SPAWN);
    }

    public BinaryMask flipValues(float density, SymmetryType symmetryType) {
        for (int x = getMinXBound(symmetryType); x < getMaxXBound(symmetryType); x++) {
            for (int y = getMinYBound(x, symmetryType); y < getMaxYBound(x, symmetryType); y++) {
                if (getValueAt(x, y)) {
                    setValueAt(x, y, random.nextFloat() < density);
                }
            }
        }
        applySymmetry(symmetryType);
        VisualDebugger.visualizeMask(this);
        return this;
    }


    public BinaryMask randomWalk(int numWalkers, int numSteps) {
        for (int i = 0; i < numWalkers; i++) {
            int x = random.nextInt(getMaxXBound() - getMinXBound()) + getMinXBound();
            int y = random.nextInt(getMaxYBound(x) - getMinYBound(x) + 1) + getMinYBound(x);
            for (int j = 0; j < numSteps; j++) {
                if (inBounds(x, y)) {
                    setValueAt(x, y, true);
                    getSymmetryPoints(x, y, SymmetryType.TERRAIN).forEach(symmetryPoint -> setValueAt(symmetryPoint.getLocation(), true));
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

    public BinaryMask randomWalkWithBrush(Vector2f start, String brushName, int size, int numberOfUses,
                                          float minValue, float maxValue, int maxStepSize) {
        Vector2f location = new Vector2f(start);
        BinaryMask brush = ((FloatMask) loadBrush(brushName, random.nextLong())
                .setSize(size)).convertToBinaryMask(minValue, maxValue);
        for (int i = 0; i < numberOfUses; i++) {
            combineWithOffset(brush, location, true);
            int dx = (random.nextBoolean() ? 1 : -1) * random.nextInt(maxStepSize + 1);
            int dy = (random.nextBoolean() ? 1 : -1) * random.nextInt(maxStepSize + 1);
            location.add(dx, dy);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask path(Vector2f start, Vector2f end, float maxStepSize, float maxAngleError, float inertia,
                           float distanceThreshold, int maxNumSteps) {
        Vector2f location = new Vector2f(start);
        setValueAt(location, true);
        int numSteps = 0;
        float oldAngle = location.getAngle(end) + (random.nextFloat() - .5f) * 2f * (maxAngleError);
        while (location.getDistance(end) > distanceThreshold && numSteps < maxNumSteps) {
            float magnitude = StrictMath.max(1, random.nextFloat() * maxStepSize);
            float angle = oldAngle * inertia + location.getAngle(end) * (1 - inertia) + (random.nextFloat() - .5f) * 2f * (maxAngleError);
            location.add((float) (magnitude * StrictMath.cos(angle)), (float) (magnitude * StrictMath.sin(angle)));
            ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(location, SymmetryType.TERRAIN);
            if (inBounds(location) && symmetryPoints.stream().allMatch(symmetryPoint -> inBounds(symmetryPoint.getLocation()))) {
                setValueAt(location, true);
                getSymmetryPoints(location, SymmetryType.TERRAIN).forEach(symmetryPoint -> setValueAt(symmetryPoint.getLocation(), true));
            }
            oldAngle = angle;
            numSteps++;
        }
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
                    setValueAt(x, y, true);
                    getSymmetryPoints(x, y, SymmetryType.TERRAIN).forEach(symmetryPoint -> setValueAt(symmetryPoint.getLocation(), true));
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
                setValueAt(x, y, !getValueAt(x, y));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask inflate(float radius) {
        Boolean[][] maskCopy = getEmptyMask(getSize());

        float radius2 = (radius + 0.5f) * (radius + 0.5f);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (isEdge(x, y) && getValueAt(x, y)) {
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
                    setValueAt(x, y, true);
                }
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask deflate(float radius) {
        Boolean[][] maskCopy = getEmptyMask(getSize());

        float radius2 = (radius + 0.5f) * (radius + 0.5f);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (isEdge(x, y) && !getValueAt(x, y)) {
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
                    setValueAt(x, y, false);
                }
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask cutCorners() {
        int size = getSize();
        Boolean[][] maskCopy = getEmptyMask(getSize());
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int count = 0;
                if (x > 0 && !getValueAt(x - 1, y))
                    count++;
                if (y > 0 && !getValueAt(x, y - 1))
                    count++;
                if (x < size - 1 && !getValueAt(x + 1, y))
                    count++;
                if (y < size - 1 && !getValueAt(x, y + 1))
                    count++;
                if (count > 1)
                    maskCopy[x][y] = false;
                else
                    maskCopy[x][y] = getValueAt(x, y);
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
        return grow(strength, SymmetryType.TERRAIN);
    }

    public BinaryMask grow(float strength, SymmetryType symmetryType) {
        return grow(strength, symmetryType, 1);
    }

    public BinaryMask grow(float strength, SymmetryType symmetryType, int count) {
        for (int i = 0; i < count; i++) {
            Boolean[][] maskCopy = getEmptyMask(getSize());
            for (int x = getMinXBound(symmetryType); x < getMaxXBound(symmetryType); x++) {
                for (int y = getMinYBound(x, symmetryType); y < getMaxYBound(x, symmetryType); y++) {
                    if (isEdge(x, y)) {
                        boolean value = random.nextFloat() < strength;
                        maskCopy[x][y] = getValueAt(x, y) || value;
                    } else if (inBounds(x, y)) {
                        maskCopy[x][y] = getValueAt(x, y);
                    }
                }
            }
            mask = maskCopy;
            applySymmetry(symmetryType);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask erode(float strength) {
        return erode(strength, SymmetryType.TERRAIN);
    }

    public BinaryMask erode(float strength, SymmetryType symmetryType) {
        return erode(strength, symmetryType, 1);
    }

    public BinaryMask erode(float strength, SymmetryType symmetryType, int count) {
        for (int i = 0; i < count; i++) {
            Boolean[][] maskCopy = getEmptyMask(getSize());
            for (int x = getMinXBound(symmetryType); x < getMaxXBound(symmetryType); x++) {
                for (int y = getMinYBound(x, symmetryType); y < getMaxYBound(x, symmetryType); y++) {
                    if (inBounds(x, y)) {
                        boolean value = isEdge(x, y) && random.nextFloat() < strength;
                        maskCopy[x][y] = mask[x][y] && !value;
                    }
                }
            }
            mask = maskCopy;
            applySymmetry(symmetryType);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask outline() {
        Boolean[][] maskCopy = getEmptyMask(getSize());

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
        int[][] innerCount = getInnerCount();

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
                setValueAt(x, y, count >= area * density);
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask combine(BinaryMask other) {
        checkSize(other);
        Boolean[][] maskCopy = getEmptyMask(getSize());
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                maskCopy[x][y] = getValueAt(x, y) || other.getValueAt(x, y);
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask combine(FloatMask other, float minValue, float maxValue) {
        combine(other.convertToBinaryMask(minValue, maxValue));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask combineWithOffset(BinaryMask other, Vector2f loc, boolean centered) {
        return combineWithOffset(other, (int) loc.x, (int) loc.y, centered);
    }

    public BinaryMask combineWithOffset(BinaryMask other, int offsetX, int offsetY, boolean center) {
        int size = StrictMath.min(getSize(), other.getSize());
        if (center) {
            offsetX -= size / 2;
            offsetY -= size / 2;
        }
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int shiftX = x + offsetX - 1;
                int shiftY = y + offsetY - 1;
                if (getSize() != size) {
                    if (inBounds(shiftX, shiftY) && other.getValueAt(x, y)) {
                        setValueAt(shiftX, shiftY, true);
                        ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(shiftX, shiftY);
                        for (SymmetryPoint symmetryPoint : symmetryPoints) {
                            setValueAt(symmetryPoint.getLocation(), true);
                        }
                    }
                } else {
                    if (other.inBounds(shiftX, shiftY) && other.getValueAt(shiftX, shiftY)) {
                        setValueAt(x, y, true);
                    }
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask combineWithOffset(FloatMask other, float minValue, float maxValue, Vector2f location) {
        combineWithOffset(other.convertToBinaryMask(minValue, maxValue), location, true);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask combineBrush(Vector2f location, String brushName, float minValue, float maxValue, int size) {
        FloatMask brush = (FloatMask) loadBrush(brushName, random.nextLong()).setSize(size);
        combineWithOffset(brush, minValue, maxValue, location);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask intersect(BinaryMask other) {
        checkSize(other);
        Boolean[][] maskCopy = getEmptyMask(getSize());
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                maskCopy[x][y] = getValueAt(x, y) && other.getValueAt(x, y);
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask minus(BinaryMask other) {
        checkSize(other);
        Boolean[][] maskCopy = getEmptyMask(getSize());
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                maskCopy[x][y] = getValueAt(x, y) && !other.getValueAt(x, y);
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask limitToSymmetryRegion() {
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (x < getMinXBound(SymmetryType.SPAWN)
                        || x >= getMaxXBound(SymmetryType.SPAWN)
                        || y < getMinYBound(x, SymmetryType.SPAWN)
                        || y >= getMaxYBound(x, SymmetryType.SPAWN)) {
                    setValueAt(x, y, false);
                }
            }
        }
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
            case POINT2 -> fillCircle((float) getSize() / 2, (float) getSize() / 2, extent * 3 / 4f, value);
            case POINT4 -> {
                fillCircle((float) getSize() / 2, (float) getSize() / 2, extent * 3 / 4f, value);
                if (symmetrySettings.getTeamSymmetry() == Symmetry.Z || symmetrySettings.getTeamSymmetry() == Symmetry.X) {
                    fillCenter(extent / 2, value, Symmetry.X);
                    fillCenter(extent / 2, value, Symmetry.Z);
                } else if (symmetrySettings.getTeamSymmetry() == Symmetry.ZX || symmetrySettings.getTeamSymmetry() == Symmetry.XZ) {
                    fillCenter(extent / 2, value, Symmetry.XZ);
                    fillCenter(extent / 2, value, Symmetry.ZX);
                }
            }
            case Z -> fillRect(0, getSize() / 2 - extent / 2, getSize(), extent, value);
            case X -> fillRect(getSize() / 2 - extent / 2, 0, extent, getSize(), value);
            case XZ -> fillDiagonal(extent * 3 / 4, false, value);
            case ZX -> fillDiagonal(extent * 3 / 4, true, value);
            case DIAG -> {
                if (symmetrySettings.getTeamSymmetry() == Symmetry.DIAG) {
                    fillCenter(extent / 2, value, Symmetry.XZ);
                    fillCenter(extent / 2, value, Symmetry.ZX);
                } else {
                    fillCenter(extent / 4, value, Symmetry.XZ);
                    fillCenter(extent / 4, value, Symmetry.ZX);
                    fillCenter(extent, value, symmetrySettings.getTeamSymmetry());
                }
            }
            case QUAD -> {
                if (symmetrySettings.getTeamSymmetry() == Symmetry.QUAD) {
                    fillCenter(extent / 2, value, Symmetry.X);
                    fillCenter(extent / 2, value, Symmetry.Z);
                } else {
                    fillCenter(extent / 4, value, Symmetry.X);
                    fillCenter(extent / 4, value, Symmetry.Z);
                    fillCenter(extent, value, symmetrySettings.getTeamSymmetry());
                }
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
        int ex = (int) StrictMath.min(getSize(), x + radius + 1);
        int ey = (int) StrictMath.min(getSize(), y + radius + 1);
        float dx;
        float dy;
        float radius2 = radius * radius;
        for (int cx = (int) StrictMath.max(0, x - radius); cx < ex; cx++) {
            for (int cy = (int) StrictMath.max(0, y - radius); cy < ey; cy++) {
                dx = x - cx;
                dy = y - cy;
                if (dx * dx + dy * dy <= radius2) {
                    setValueAt(cx, cy, value);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillSquare(Vector2f v, int extent, boolean value) {
        return fillSquare((int) v.x, (int) v.y, extent, value);
    }

    public BinaryMask fillSquare(int x, int y, int extent, boolean value) {
        return fillRect(x, y, extent, extent, value);
    }

    public BinaryMask fillRect(Vector2f v, int width, int height, boolean value) {
        return fillRect((int) v.x, (int) v.y, width, height, value);
    }

    public BinaryMask fillRect(int x, int y, int width, int height, boolean value) {
        return fillParallelogram(x, y, width, height, 0, 0, value);
    }

    public BinaryMask fillRectFromPoints(int x1, int x2, int z1, int z2, boolean value) {
        int smallX = StrictMath.min(x1, x2);
        int bigX = StrictMath.max(x1, x2);
        int smallZ = StrictMath.min(z1, z2);
        int bigZ = StrictMath.max(z1, z2);
        return fillRect(smallX, smallZ, bigX - smallX, bigZ - smallZ, value);
    }

    public BinaryMask fillParallelogram(Vector2f v, int width, int height, int xSlope, int ySlope, boolean value) {
        return fillParallelogram((int) v.x, (int) v.y, width, height, xSlope, ySlope, value);
    }

    public BinaryMask fillParallelogram(int x, int y, int width, int height, int xSlope, int ySlope, boolean value) {
        for (int px = 0; px < width; px++) {
            for (int py = 0; py < height; py++) {
                int calcX = x + px + py * xSlope;
                int calcY = y + py + px * ySlope;
                if (inBounds(calcX, calcY)) {
                    setValueAt(calcX, calcY, value);
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
                    setValueAt(x, y, value);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillEdge(int rimWidth, boolean value) {
        for (int a = 0; a < rimWidth; a++) {
            for (int b = 0; b < getSize() - rimWidth; b++) {
                setValueAt(a, b, value);
                setValueAt(getSize() - 1 - a, getSize() - 1 - b, value);
                setValueAt(b, getSize() - 1 - a, value);
                setValueAt(getSize() - 1 - b, a, value);
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
        boolean value = getValueAt(location);
        queue.add(location);
        queueHash.add(location);
        while (queue.size() > 0) {
            Vector2f next = queue.get(0);
            queue.remove(next);
            queueHash.remove(next);
            if (getValueAt(next) == value && !area.contains(next)) {
                setValueAt(next, !value);
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

    public BinaryMask fillCoordinates(Collection<Vector2f> coordinates, boolean value) {
        coordinates.forEach(location -> setValueAt(location, value));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillGaps(int minDist) {
        FloatMask distanceField = getDistanceField();
        BinaryMask filledGaps = new BinaryMask(getSize(), random.nextLong(), symmetrySettings);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                float distance = distanceField.getValueAt(x, y);
                if (distance < minDist / 2f && distance > 0f && distanceField.isLocalMax(x, y)) {
                    filledGaps.setValueAt(x, y, true);
                }
            }
        }
        filledGaps.inflate(minDist / 2f).smooth(4, .75f);
        combine(filledGaps);
        applySymmetry(SymmetryType.SPAWN);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask widenGaps(int minDist) {
        FloatMask distanceField = copy().invert().getDistanceField();
        BinaryMask filledGaps = new BinaryMask(getSize(), random.nextLong(), symmetrySettings);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                float distance = distanceField.getValueAt(x, y);
                if (distance < minDist / 2f && distance > 0f && distanceField.isLocalMax(x, y)) {
                    filledGaps.setValueAt(x, y, true);
                }
            }
        }
        filledGaps.inflate(minDist / 2f).smooth(4, .75f);
        minus(filledGaps);
        applySymmetry(SymmetryType.SPAWN);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask removeAreasSmallerThan(int minArea) {
        LinkedHashSet<Vector2f> locHash = new LinkedHashSet<>();
        FloatMask distanceField = getDistanceField();
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                float distance = distanceField.getValueAt(x, y);
                if (distance < StrictMath.sqrt(minArea) && distance > 0f && distanceField.isLocalMax(x, y)) {
                    locHash.add(new Vector2f(x, y));
                }
            }
        }
        LinkedList<Vector2f> locList = new LinkedList<>(locHash);
        while (locHash.size() > 0) {
            Vector2f location = locList.removeFirst();
            Set<Vector2f> coordinates = getShapeCoordinates(location, minArea);
            if (coordinates.size() < minArea) {
                fillCoordinates(coordinates, true);
            }
            locHash.removeAll(coordinates);
            locList = new LinkedList<>(locHash);
        }
        locHash = new LinkedHashSet<>();
        distanceField = copy().invert().getDistanceField();
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                float distance = distanceField.getValueAt(x, y);
                if (distance < StrictMath.sqrt(minArea) && distance > 0f && distanceField.isLocalMax(x, y)) {
                    locHash.add(new Vector2f(x, y));
                }
            }
        }
        locList = new LinkedList<>(locHash);
        while (locHash.size() > 0) {
            Vector2f location = locList.removeFirst();
            Set<Vector2f> coordinates = getShapeCoordinates(location, minArea);
            if (coordinates.size() < minArea) {
                fillCoordinates(coordinates, false);
            }
            locHash.removeAll(coordinates);
            locList = new LinkedList<>(locHash);
        }
        applySymmetry(SymmetryType.SPAWN);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask removeAreasBiggerThan(int maxArea) {
        minus(copy().removeAreasSmallerThan(maxArea));
        applySymmetry(SymmetryType.SPAWN);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask removeAreasOutside(int minSize, int maxSize) {
        removeAreasSmallerThan(minSize);
        removeAreasBiggerThan(maxSize);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask removeAreasBetween(int minSize, int maxSize) {
        minus(this.copy().removeAreasOutside(minSize, maxSize));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public LinkedHashSet<Vector2f> getShapeCoordinates(Vector2f location) {
        return getShapeCoordinates(location, getSize() * getSize());
    }

    public LinkedHashSet<Vector2f> getShapeCoordinates(Vector2f location, int maxSize) {
        LinkedHashSet<Vector2f> areaHash = new LinkedHashSet<>();
        LinkedHashSet<Vector2f> edgeHash = new LinkedHashSet<>();
        LinkedList<Vector2f> queue = new LinkedList<>();
        LinkedHashSet<Vector2f> queueHash = new LinkedHashSet<>();
        List<int[]> edges = Arrays.asList(new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}, new int[]{1, 0});
        boolean value = getValueAt(location);
        queue.add(location);
        queueHash.add(location);
        while (queue.size() > 0) {
            Vector2f next = queue.remove();
            queueHash.remove(next);
            if (getValueAt(next) == value && !areaHash.contains(next)) {
                areaHash.add(next);
                edges.forEach((e) -> {
                    Vector2f newLocation = new Vector2f(next.x + e[0], next.y + e[1]);
                    if (!queueHash.contains(newLocation) && !areaHash.contains(newLocation) && !edgeHash.contains(newLocation) && inBounds(newLocation)) {
                        queue.add(newLocation);
                        queueHash.add(newLocation);
                    }
                });
            } else if (getValueAt(next) != value) {
                edgeHash.add(next);
            }
            if (areaHash.size() > maxSize) {
                break;
            }
        }
        return areaHash;
    }

    public BinaryMask getAreasWithinEdgeDistance(int edgeDistance) {
        return copy().inflate(edgeDistance).minus(copy().deflate(edgeDistance));
    }

    public int[][] getInnerCount() {
        int[][] innerCount = new int[getSize()][getSize()];

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                int val = getValueAt(x, y) ? 1 : 0;
                innerCount[x][y] = val;
                innerCount[x][y] += x > 0 ? innerCount[x - 1][y] : 0;
                innerCount[x][y] += y > 0 ? innerCount[x][y - 1] : 0;
                innerCount[x][y] -= x > 0 && y > 0 ? innerCount[x - 1][y - 1] : 0;
            }
        }
        return innerCount;
    }

    public FloatMask getDistanceField() {
        FloatMask distanceField = new FloatMask(getSize(), random.nextLong(), symmetrySettings);
        distanceField.init(this, getSize() * getSize(), 0f);
        for (int i = 0; i < getSize(); i++) {
            ArrayList<Vector2f> vertices = new ArrayList<>();
            ArrayList<Vector2f> intersections = new ArrayList<>();
            int index = 0;
            vertices.add(new Vector2f(0, distanceField.getValueAt(i, 0)));
            intersections.add(new Vector2f(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY));
            intersections.add(new Vector2f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
            for (int j = 1; j < getSize(); j++) {
                Vector2f current = new Vector2f(j, distanceField.getValueAt(i, j));
                Vector2f vertex = vertices.get(index);
                float xIntersect = ((current.y + current.x * current.x) - (vertex.y + vertex.x * vertex.x)) / (2 * current.x - 2 * vertex.x);
                while (xIntersect <= intersections.get(index).x) {
                    index -= 1;
                    vertex = vertices.get(index);
                    xIntersect = ((current.y + current.x * current.x) - (vertex.y + vertex.x * vertex.x)) / (2 * current.x - 2 * vertex.x);
                }
                index += 1;
                if (index < vertices.size()) {
                    vertices.set(index, current);
                } else {
                    vertices.add(current);
                }
                if (index < intersections.size() - 1) {
                    intersections.set(index, new Vector2f(xIntersect, Float.POSITIVE_INFINITY));
                    intersections.set(index + 1, new Vector2f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
                } else {
                    intersections.set(index, new Vector2f(xIntersect, Float.POSITIVE_INFINITY));
                    intersections.add(new Vector2f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
                }
            }
            index = 0;
            for (int j = 0; j < getSize(); j++) {
                while (intersections.get(index + 1).x < j) {
                    index += 1;
                }
                Vector2f vertex = vertices.get(index);
                float dx = j - vertex.x;
                distanceField.setValueAt(i, j, dx * dx + vertex.y);
            }
        }
        for (int i = 0; i < getSize(); i++) {
            ArrayList<Vector2f> vertices = new ArrayList<>();
            ArrayList<Vector2f> intersections = new ArrayList<>();
            int index = 0;
            vertices.add(new Vector2f(0, distanceField.getValueAt(0, i)));
            intersections.add(new Vector2f(Float.NEGATIVE_INFINITY, Float.MAX_VALUE));
            intersections.add(new Vector2f(Float.POSITIVE_INFINITY, Float.MAX_VALUE));
            for (int j = 1; j < getSize(); j++) {
                Vector2f current = new Vector2f(j, distanceField.getValueAt(j, i));
                Vector2f vertex = vertices.get(index);
                float xIntersect = ((current.y + current.x * current.x) - (vertex.y + vertex.x * vertex.x)) / (2 * current.x - 2 * vertex.x);
                while (xIntersect <= intersections.get(index).x) {
                    index -= 1;
                    vertex = vertices.get(index);
                    xIntersect = ((current.y + current.x * current.x) - (vertex.y + vertex.x * vertex.x)) / (2 * current.x - 2 * vertex.x);
                }
                index += 1;
                if (index < vertices.size()) {
                    vertices.set(index, current);
                } else {
                    vertices.add(current);
                }
                if (index < intersections.size() - 1) {
                    intersections.set(index, new Vector2f(xIntersect, Float.POSITIVE_INFINITY));
                    intersections.set(index + 1, new Vector2f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
                } else {
                    intersections.set(index, new Vector2f(xIntersect, Float.POSITIVE_INFINITY));
                    intersections.add(new Vector2f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
                }
            }
            index = 0;
            for (int j = 0; j < getSize(); j++) {
                while (intersections.get(index + 1).x < j) {
                    index += 1;
                }
                Vector2f vertex = vertices.get(index);
                float dx = j - vertex.x;
                distanceField.setValueAt(j, i, dx * dx + vertex.y);
            }
        }
        distanceField.sqrt();
        return distanceField;
    }

    public int getCount() {
        int cellCount = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (getValueAt(x, y))
                    cellCount++;
            }
        }
        return cellCount;
    }

    public LinkedList<Vector2f> getAllCoordinates(int spacing) {
        LinkedHashSet<Vector2f> coordinates = new LinkedHashSet<>();
        for (int x = 0; x < getSize(); x += spacing) {
            for (int y = 0; y < getSize(); y += spacing) {
                Vector2f location = new Vector2f(x, y);
                coordinates.add(location);
            }
        }
        return new LinkedList<>(coordinates);
    }

    public LinkedList<Vector2f> getAllCoordinatesEqualTo(boolean value, int spacing) {
        LinkedHashSet<Vector2f> coordinates = new LinkedHashSet<>();
        for (int x = 0; x < getSize(); x += spacing) {
            for (int y = 0; y < getSize(); y += spacing) {
                if (getValueAt(x, y) == value) {
                    Vector2f location = new Vector2f(x, y);
                    coordinates.add(location);
                }
            }
        }
        return new LinkedList<>(coordinates);
    }

    public LinkedList<Vector2f> getSpacedCoordinates(float radius, int spacing) {
        LinkedList<Vector2f> coordinateList = getAllCoordinates(spacing);
        LinkedHashSet<Vector2f> chosenCoordinates = new LinkedHashSet<>();
        while (coordinateList.size() > 0) {
            Vector2f location = coordinateList.removeFirst();
            chosenCoordinates.add(location);
            coordinateList.removeIf((loc) -> location.getDistance(loc) < radius);
        }
        return new LinkedList<>(chosenCoordinates);
    }

    public LinkedList<Vector2f> getSpacedCoordinatesEqualTo(boolean value, float radius, int spacing) {
        LinkedList<Vector2f> coordinateList = getAllCoordinatesEqualTo(value, spacing);
        LinkedHashSet<Vector2f> chosenCoordinates = new LinkedHashSet<>();
        while (coordinateList.size() > 0) {
            Vector2f location = coordinateList.removeFirst();
            chosenCoordinates.add(location);
            coordinateList.removeIf((loc) -> location.getDistance(loc) < radius);
        }
        return new LinkedList<>(chosenCoordinates);
    }

    public LinkedList<Vector2f> getRandomCoordinates(float spacing) {
        return getRandomCoordinates(spacing, spacing);
    }

    public LinkedList<Vector2f> getRandomCoordinates(float minSpacing, float maxSpacing) {
        LinkedList<Vector2f> coordinateList = getAllCoordinatesEqualTo(true, 1);
        LinkedHashSet<Vector2f> chosenCoordinates = new LinkedHashSet<>();
        while (coordinateList.size() > 0) {
            Vector2f location = coordinateList.remove(random.nextInt(coordinateList.size()));
            float spacing = random.nextFloat() * (maxSpacing - minSpacing) + minSpacing;
            chosenCoordinates.add(location);
            coordinateList.removeIf((loc) -> location.getDistance(loc) < spacing);
            ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(location);
            symmetryPoints.forEach(symmetryPoint -> coordinateList.removeIf((loc) -> symmetryPoint.getLocation().getDistance(loc) < minSpacing));
        }
        return new LinkedList<>(chosenCoordinates);
    }

    public Vector2f getRandomPosition() {
        ArrayList<Vector2f> coordinates = new ArrayList<>(getAllCoordinatesEqualTo(true, 1));
        if (coordinates.size() == 0)
            return null;
        int cell = random.nextInt(coordinates.size());
        return coordinates.get(cell);
    }

    public BinaryMask connectLocationToNearItsSymLocation(Vector2f start, String brushName, int size, int usesBatchSize,
                                                          float minValue, float maxValue, int maxDistanceBetweenBrushUse, int distanceThreshold) {
        BinaryMask brush = ((FloatMask) loadBrush(brushName, random.nextLong())
                .setSize(size)).convertToBinaryMask(minValue, maxValue);
        ArrayList<SymmetryPoint> symLocationList = getSymmetryPoints(start);
        Vector2f location = new Vector2f(start);
        Vector2f end = symLocationList.get(0).getLocation();
        int maskSize = getSize();
        while (location.getDistance(end) > distanceThreshold) {
            for (int i = 0; i < usesBatchSize; i++) {
                int dx = (random.nextBoolean() ? 1 : -1) * random.nextInt(maxDistanceBetweenBrushUse + 1);
                int dy = (random.nextBoolean() ? 1 : -1) * random.nextInt(maxDistanceBetweenBrushUse + 1);
                location.add(dx, dy).clampMax(maskSize, maskSize).clampMin(0, 0);
                combineWithOffset(brush, location, true);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask connectLocationToLocationFromList(Vector2f start, ArrayList<Vector2f> targetLocations, String brushName, int size, int batchSize,
                                                        float minValue, float maxValue, int maxDistanceBetweenBrushUse, int distanceThreshold) {
        BinaryMask brush = ((FloatMask) loadBrush(brushName, random.nextLong())
                .setSize(size)).convertToBinaryMask(minValue, maxValue);
        int maskSize = getSize();
        Vector2f location = new Vector2f(start);
        while (targetLocations.stream().noneMatch(target -> location.getDistance(target) < distanceThreshold)) {
            for (int i = 0; i < batchSize; i++) {
                int dx = (random.nextBoolean() ? 1 : -1) * random.nextInt(maxDistanceBetweenBrushUse + 1);
                int dy = (random.nextBoolean() ? 1 : -1) * random.nextInt(maxDistanceBetweenBrushUse + 1);
                location.add(dx, dy).clampMax(maskSize, maskSize).clampMin(0, 0);
                combineWithOffset(brush, location, true);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask connectSpawnsWithRandomBrushUse(ArrayList<Spawn> spawns, int numberOfTeams, float probabilityToConnectTeamAtSpawn,
                                                      String brushName, int size, int batchSize, float minIntensity, float maxIntensity,
                                                      int maxDistanceBrushUse, int distanceThreshold) {
        ArrayList<Vector2f> targetSpawns = new ArrayList<>();
        for (int z = spawns.size() / numberOfTeams; z < spawns.size(); z++) {
            targetSpawns.add(new Vector2f(spawns.get(z).getPosition().x, spawns.get(z).getPosition().z));
        }
        for (int z = 0; z < spawns.size() / numberOfTeams; z += numberOfTeams) {
            if (probabilityToConnectTeamAtSpawn > random.nextFloat()) {
                Vector2f spawn = new Vector2f(spawns.get(z).getPosition().x, spawns.get(z).getPosition().z);
                connectLocationToLocationFromList(spawn, targetSpawns, brushName, size, batchSize, minIntensity, maxIntensity, maxDistanceBrushUse, distanceThreshold);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask connectSymSpawnWithRandomBrushUse(ArrayList<Spawn> spawns, int numberOfTeams, float probabilityToConnectSpawn,
                                                        String brushName, int size, int batchSize, float minIntensity, float maxIntensity,
                                                        int maxDistanceBetweenBrushstrokes, int distanceThreshold) {
        for (int z = 0; z < spawns.size() / numberOfTeams; z += numberOfTeams) {
            if (probabilityToConnectSpawn > random.nextFloat()) {
                Vector2f spawn = new Vector2f(spawns.get(z).getPosition().x, spawns.get(z).getPosition().z);
                connectLocationToNearItsSymLocation(spawn, brushName, size, batchSize, minIntensity, maxIntensity, maxDistanceBetweenBrushstrokes, distanceThreshold);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask connectToCenterWithBrush(Vector2f location, String brushName, int size, int usesBatchSize,
                                               float minIntensity, float maxIntensity, int maxDistanceBetweenBrushstroke) {
        int halfSize = getSize() / 2;
        while (!getValueAt(halfSize, halfSize)) {
            randomWalkWithBrush(location, brushName, size, usesBatchSize, minIntensity, maxIntensity, maxDistanceBetweenBrushstroke);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    // --------------------------------------------------

    @SneakyThrows
    public void writeToFile(Path path) {
        Files.deleteIfExists(path);
        Files.createFile(path);
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path.toFile())));

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                out.writeBoolean(getValueAt(x, y));
            }
        }

        out.close();
    }

    public void checkSize(Mask<?> other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size: other is " + other.getSize() + " and BinaryMask is " + getSize());
        }
    }

    public String toHash() throws NoSuchAlgorithmException {
        ByteBuffer bytes = ByteBuffer.allocate(getSize() * getSize());
        for (int x = getMinXBound(SymmetryType.SPAWN); x < getMaxXBound(SymmetryType.SPAWN); x++) {
            for (int y = getMinYBound(x, SymmetryType.SPAWN); y < getMaxYBound(x, SymmetryType.SPAWN); y++) {
                byte b = getValueAt(x, y) ? (byte) 1 : 0;
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

    public BinaryMask startVisualDebugger() {
        return startVisualDebugger(toString(), Util.getStackTraceParentClass());
    }

    public BinaryMask startVisualDebugger(String maskName) {
        return startVisualDebugger(maskName, Util.getStackTraceParentClass());
    }

    public BinaryMask startVisualDebugger(String maskName, String parentClass) {
        VisualDebugger.whitelistMask(this, maskName, parentClass);
        show();
        return this;
    }
}
