package neroxis.map;

import lombok.Getter;
import lombok.SneakyThrows;
import neroxis.generator.VisualDebugger;
import neroxis.util.Util;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static neroxis.brushes.Brushes.loadBrush;

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
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask randomize(float density) {
        for (int x = getMinXBound(SymmetryType.TERRAIN); x < getMaxXBound(SymmetryType.TERRAIN); x++) {
            for (int y = getMinYBound(x, SymmetryType.TERRAIN); y < getMaxYBound(x, SymmetryType.TERRAIN); y++) {
                setValueAt(x, y, random.nextFloat() < density);
            }
        }
        applySymmetry(SymmetryType.TERRAIN);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask flipValues(float density) {
        for (int x = getMinXBound(SymmetryType.SPAWN); x < getMaxXBound(SymmetryType.SPAWN); x++) {
            for (int y = getMinYBound(x, SymmetryType.SPAWN); y < getMaxYBound(x, SymmetryType.SPAWN); y++) {
                if (getValueAt(x, y)) {
                    setValueAt(x, y, random.nextFloat() < density);
                }
            }
        }
        applySymmetry(SymmetryType.SPAWN);
        VisualDebugger.visualizeMask(this);
        return this;
    }


    public BinaryMask randomWalk(int numWalkers, int numSteps) {
        for (int i = 0; i < numWalkers; i++) {
            int x = random.nextInt(getMaxXBound(SymmetryType.TERRAIN) - getMinXBound(SymmetryType.TERRAIN)) + getMinXBound(SymmetryType.TERRAIN);
            int y = random.nextInt(getMaxYBound(x, SymmetryType.TERRAIN) - getMinYBound(x, SymmetryType.TERRAIN) + 1) + getMinYBound(x, SymmetryType.TERRAIN);
            for (int j = 0; j < numSteps; j++) {
                if (inBounds(x, y)) {
                    setValueAt(x, y, true);
                    getSymmetryPoints(x, y, SymmetryType.TERRAIN).forEach(symmetryPoint -> setValueAt(symmetryPoint.getLocation(), true));
                }
                switch (random.nextInt(4)) {
                    case 0:
                        x++;
                        break;
                    case 1:
                        x--;
                        break;
                    case 2:
                        y++;
                        break;
                    case 3:
                        y--;
                        break;
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask randomWalkWithBrush(Vector2f start, String brushName, int size, int numberOfUses,
                                          float minValue, float maxValue, int maxStepSize) {
        Vector2f location = new Vector2f(start);
        BinaryMask brush = ((FloatMask) loadBrush(brushName, random.nextLong())
                .setSize(size)).convertToBinaryMask(minValue, maxValue);
        for (int i = 0; i < numberOfUses; i++) {
            combineWithOffset(brush, location, true, false);
            int dx = (random.nextBoolean() ? 1 : -1) * random.nextInt(maxStepSize + 1);
            int dy = (random.nextBoolean() ? 1 : -1) * random.nextInt(maxStepSize + 1);
            location.add(dx, dy);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask guidedWalkWithBrush(Vector2f start, Vector2f target, String brushName, int size, int numberOfUses,
                                          float minValue, float maxValue, int maxStepSize, boolean wrapEdges) {
        Vector2f location = new Vector2f(start);
        BinaryMask brush = ((FloatMask) loadBrush(brushName, random.nextLong())
                .setSize(size)).convertToBinaryMask(minValue, maxValue);
        for (int i = 0; i < numberOfUses; i++) {
            combineWithOffset(brush, location, true, wrapEdges);
            int dx = (target.getX() > location.getX() ? 1 : -1) * random.nextInt(maxStepSize + 1);
            int dy = (target.getY() > location.getY() ? 1 : -1) * random.nextInt(maxStepSize + 1);
            location.add(dx, dy);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask path(Vector2f start, Vector2f end, float maxStepSize, int numMiddlePoints, SymmetryType symmetryType) {
        float distance = start.getDistance(end);
        List<Vector2f> checkPoints = new ArrayList<>();
        checkPoints.add(start);
        for (int i = 0; i < numMiddlePoints; i++) {
            Vector2f previousLoc = checkPoints.get(checkPoints.size() - 1);
            float angle = (float) (previousLoc.getAngle(end) + (random.nextFloat() - .5f) * 2f * StrictMath.PI / 2);
            float magnitude = (random.nextFloat() + .5f) * distance / numMiddlePoints;
            checkPoints.add(new Vector2f(previousLoc).addPolar(angle, magnitude));
        }
        checkPoints.add(end);
        int numSteps = 0;
        for (int i = 0; i < checkPoints.size() - 1; i++) {
            Vector2f location = checkPoints.get(i);
            Vector2f nextLoc = checkPoints.get(i + 1);
            float oldAngle = (float) (location.getAngle(nextLoc) + (random.nextFloat() - .5f) * 2f * (StrictMath.PI / 2));
            while (location.getDistance(nextLoc) > maxStepSize && numSteps < getSize() * getSize()) {
                ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(location, symmetryType);
                if (inBounds(location) && symmetryPoints.stream().allMatch(symmetryPoint -> inBounds(symmetryPoint.getLocation()))) {
                    setValueAt(location, true);
                    getSymmetryPoints(location, symmetryType).forEach(symmetryPoint -> setValueAt(symmetryPoint.getLocation(), true));
                }
                float magnitude = StrictMath.max(1, random.nextFloat() * maxStepSize);
                float angle = (float) (oldAngle * .5f + location.getAngle(nextLoc) * .5f + (random.nextFloat() - .5f) * 2f * (StrictMath.PI / 2));
                location.addPolar(angle, magnitude);
                oldAngle = angle;
                numSteps++;
            }
            if (numSteps == getSize() * getSize()) {
                break;
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask progressiveWalk(int numWalkers, int numSteps) {
        for (int i = 0; i < numWalkers; i++) {
            int x = random.nextInt(getMaxXBound(SymmetryType.TERRAIN) - getMinXBound(SymmetryType.TERRAIN)) + getMinXBound(SymmetryType.TERRAIN);
            int y = random.nextInt(getMaxYBound(x, SymmetryType.TERRAIN) - getMinYBound(x, SymmetryType.TERRAIN) + 1) + getMinYBound(x, SymmetryType.TERRAIN);
            List<Integer> directions = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
            int regressiveDir = random.nextInt(directions.size());
            directions.remove(regressiveDir);
            for (int j = 0; j < numSteps; j++) {
                if (inBounds(x, y)) {
                    setValueAt(x, y, true);
                    getSymmetryPoints(x, y, SymmetryType.TERRAIN).forEach(symmetryPoint -> setValueAt(symmetryPoint.getLocation(), true));
                }
                switch (directions.get(random.nextInt(directions.size()))) {
                    case 0:
                        x++;
                        break;
                    case 1:
                        x--;
                        break;
                    case 2:
                        y++;
                        break;
                    case 3:
                        y--;
                        break;
                }
            }
        }
        applySymmetry(SymmetryType.TERRAIN);
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

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (isEdge(x, y) && getValueAt(x, y)) {
                    markTrueInRadius(radius, maskCopy, x, y);
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

        applySymmetry(SymmetryType.SPAWN);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask deflate(float radius) {
        Boolean[][] maskCopy = getEmptyMask(getSize());

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (isEdge(x, y) && !getValueAt(x, y)) {
                    markTrueInRadius(radius, maskCopy, x, y);
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

        applySymmetry(SymmetryType.SPAWN);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    private void markTrueInRadius(float radius, Boolean[][] maskCopy, int x, int y) {
        float radius2 = (radius + 0.5f) * (radius + 0.5f);
        for (int x2 = (int) (x - radius); x2 < x + radius + 1; x2++) {
            for (int y2 = (int) (y - radius); y2 < y + radius + 1; y2++) {
                if (inBounds(x2, y2) && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2) {
                    maskCopy[x2][y2] = true;
                }
            }
        }
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
        Symmetry spawnSymmetry = symmetrySettings.getSpawnSymmetry();
        BinaryMask holes = new BinaryMask(getSize(), random.nextLong(), new SymmetrySettings(spawnSymmetry, spawnSymmetry, spawnSymmetry));
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
        BinaryMask maskCopy = new BinaryMask(this, null);
        for (int i = 0; i < count; i++) {
            for (int x = getMinXBound(symmetryType); x < getMaxXBound(symmetryType); x++) {
                for (int y = getMinYBound(x, symmetryType); y < getMaxYBound(x, symmetryType); y++) {
                    if (isEdge(x, y)) {
                        boolean value = random.nextFloat() < strength || getValueAt(x, y);
                        maskCopy.setValueAt(x, y, value);
                    }
                }
            }
            combine(maskCopy);
        }
        applySymmetry(symmetryType);
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
        BinaryMask maskCopy = new BinaryMask(this, null);
        for (int i = 0; i < count; i++) {
            for (int x = getMinXBound(symmetryType); x < getMaxXBound(symmetryType); x++) {
                for (int y = getMinYBound(x, symmetryType); y < getMaxYBound(x, symmetryType); y++) {
                    if (isEdge(x, y)) {
                        boolean value = random.nextFloat() > strength && getValueAt(x, y);
                        maskCopy.setValueAt(x, y, value);
                    }
                }
            }
            intersect(maskCopy);
        }
        applySymmetry(symmetryType);
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

    public BinaryMask interpolate() {
        return smooth(1, .35f);
    }

    public BinaryMask smooth(int radius) {
        return smooth(radius, .5f);
    }

    public BinaryMask smooth(int radius, float density) {
        int[][] innerCount = getInnerCount();

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                float areaDensity = calculateAreaAverage(radius, x, y, innerCount);
                setValueAt(x, y, areaDensity >= density);
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask replace(BinaryMask other) {
        checkMatchingSize(other);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                mask[x][y] = other.getValueAt(x, y);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask combine(BinaryMask other) {
        checkMatchingSize(other);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                mask[x][y] = getValueAt(x, y) || other.getValueAt(x, y);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask combine(FloatMask other, float minValue, float maxValue) {
        combine(other.convertToBinaryMask(minValue, maxValue));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask combineWithOffset(BinaryMask other, Vector2f loc, boolean centered, boolean wrapEdges) {
        return combineWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    public BinaryMask combineWithOffset(BinaryMask other, int offsetX, int offsetY, boolean center, boolean wrapEdges) {
        int size = StrictMath.min(getSize(), other.getSize());
        if (center) {
            offsetX -= size / 2;
            offsetY -= size / 2;
        }
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int shiftX;
                int shiftY;
                if (wrapEdges) {
                    shiftX = (x + offsetX + size) % size;
                    shiftY = (y + offsetY + size) % size;
                } else {
                    shiftX = x + offsetX - 1;
                    shiftY = y + offsetY - 1;
                }
                if (getSize() != size) {
                    if (inBounds(shiftX, shiftY) && other.getValueAt(x, y)) {
                        setValueAt(shiftX, shiftY, true);
                        ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(shiftX, shiftY, SymmetryType.SPAWN);
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

    public BinaryMask combineWithOffset(FloatMask other, float minValue, float maxValue, Vector2f location, boolean wrapEdges) {
        combineWithOffset(other.convertToBinaryMask(minValue, maxValue), location, true, wrapEdges);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask combineBrush(Vector2f location, String brushName, float minValue, float maxValue, int size) {
        FloatMask brush = (FloatMask) loadBrush(brushName, random.nextLong()).setSize(size);
        combineWithOffset(brush, minValue, maxValue, location, false);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask intersect(BinaryMask other) {
        checkMatchingSize(other);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                mask[x][y] = getValueAt(x, y) && other.getValueAt(x, y);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask minus(BinaryMask other) {
        checkMatchingSize(other);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                mask[x][y] = getValueAt(x, y) && !other.getValueAt(x, y);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask limitToSymmetryRegion() {
        return limitToSymmetryRegion(SymmetryType.TEAM);
    }

    public BinaryMask limitToSymmetryRegion(SymmetryType symmetryType) {
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (x < getMinXBound(symmetryType)
                        || x >= getMaxXBound(symmetryType)
                        || y < getMinYBound(x, symmetryType)
                        || y >= getMaxYBound(x, symmetryType)) {
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
            case Z:
                fillRect(0, 0, extent / 2, getSize(), value).fillRect(getSize() - extent / 2, 0, getSize() - extent / 2, getSize(), value);
                break;
            case X:
                fillRect(0, 0, getSize(), extent / 2, value).fillRect(0, getSize() - extent / 2, getSize(), extent / 2, value);
                break;
            case XZ:
                fillParallelogram(0, 0, getSize(), extent * 3 / 4, 0, -1, value).fillParallelogram(getSize() - extent * 3 / 4, getSize(), getSize(), extent * 3 / 4, 0, -1, value);
                break;
            case ZX:
                fillParallelogram(getSize() - extent * 3 / 4, 0, extent * 3 / 4, extent * 3 / 4, 1, 0, value).fillParallelogram(-extent * 3 / 4, getSize() - extent * 3 / 4, extent * 3 / 4, extent * 3 / 4, 1, 0, value);
                break;
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillCenter(int extent, boolean value) {
        return fillCenter(extent, value, symmetrySettings.getSpawnSymmetry());
    }

    public BinaryMask fillCenter(int extent, boolean value, Symmetry symmetry) {
        switch (symmetry) {
            case POINT2:
            case POINT3:
            case POINT4:
            case POINT5:
            case POINT6:
            case POINT7:
            case POINT8:
            case POINT9:
            case POINT10:
            case POINT11:
            case POINT12:
            case POINT13:
            case POINT14:
            case POINT15:
            case POINT16:
                fillCircle((float) getSize() / 2, (float) getSize() / 2, extent * 3 / 4f, value);
                break;
            case Z:
                fillRect(0, getSize() / 2 - extent / 2, getSize(), extent, value);
                break;
            case X:
                fillRect(getSize() / 2 - extent / 2, 0, extent, getSize(), value);
                break;
            case XZ:
                fillDiagonal(extent * 3 / 4, false, value);
                break;
            case ZX:
                fillDiagonal(extent * 3 / 4, true, value);
                break;
            case DIAG:
                if (symmetrySettings.getTeamSymmetry() == Symmetry.DIAG) {
                    fillCenter(extent / 2, value, Symmetry.XZ);
                    fillCenter(extent / 2, value, Symmetry.ZX);
                } else {
                    fillCenter(extent / 4, value, Symmetry.XZ);
                    fillCenter(extent / 4, value, Symmetry.ZX);
                    fillCenter(extent, value, symmetrySettings.getTeamSymmetry());
                }
                break;
            case QUAD:
                if (symmetrySettings.getTeamSymmetry() == Symmetry.QUAD) {
                    fillCenter(extent / 2, value, Symmetry.X);
                    fillCenter(extent / 2, value, Symmetry.Z);
                } else {
                    fillCenter(extent / 4, value, Symmetry.X);
                    fillCenter(extent / 4, value, Symmetry.Z);
                    fillCenter(extent, value, symmetrySettings.getTeamSymmetry());
                }
                break;
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillCircle(Vector3f v, float radius, boolean value) {
        return fillCircle(new Vector2f(v), radius, value);
    }

    public BinaryMask fillCircle(Vector2f v, float radius, boolean value) {
        return fillCircle(v.getX(), v.getY(), radius, value);
    }

    public BinaryMask fillCircle(float x, float y, float radius, boolean value) {
        return fillArc(x, y, 0, 360, radius, value);
    }

    public BinaryMask fillArc(float x, float y, float startAngle, float endAngle, float radius, boolean value) {
        int ex = StrictMath.round(StrictMath.min(getSize(), x + radius + 1));
        int ey = StrictMath.round(StrictMath.min(getSize(), y + radius + 1));
        float dx;
        float dy;
        float radius2 = radius * radius;
        for (int cx = StrictMath.round(StrictMath.max(0, x - radius)); cx < ex; cx++) {
            for (int cy = StrictMath.round(StrictMath.max(0, y - radius)); cy < ey; cy++) {
                dx = x - cx;
                dy = y - cy;
                float angle = (float) (StrictMath.atan2(dy, dx) / StrictMath.PI * 180 + 360) % 360;
                if (dx * dx + dy * dy <= radius2 && angle >= startAngle && angle <= endAngle) {
                    setValueAt(cx, cy, value);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillSquare(Vector2f v, int extent, boolean value) {
        return fillSquare((int) v.getX(), (int) v.getY(), extent, value);
    }

    public BinaryMask fillSquare(int x, int y, int extent, boolean value) {
        return fillRect(x, y, extent, extent, value);
    }

    public BinaryMask fillRect(Vector2f v, int width, int height, boolean value) {
        return fillRect((int) v.getX(), (int) v.getY(), width, height, value);
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
        return fillParallelogram((int) v.getX(), (int) v.getY(), width, height, xSlope, ySlope, value);
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
        fillCoordinates(getShapeCoordinates(location), !getValueAt(location));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillCoordinates(Collection<Vector2f> coordinates, boolean value) {
        coordinates.forEach(location -> setValueAt(location, value));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask fillGaps(int minDist) {
        BinaryMask filledGaps = getDistanceField().getLocalMaximums(1f, minDist / 2f);
        filledGaps.inflate(minDist / 2f);
        combine(filledGaps);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask widenGaps(int minDist) {
        BinaryMask filledGaps = getDistanceField().getLocalMaximums(1f, minDist / 2f);
        filledGaps.inflate(minDist / 2f);
        minus(filledGaps);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask removeAreasSmallerThan(int minArea) {
        Set<Vector2f> seen = new HashSet<>(getSize() * getSize() * 2);
        for (int x = getMinXBound(SymmetryType.SPAWN); x < getMaxXBound(SymmetryType.SPAWN); x++) {
            for (int y = getMinYBound(x, SymmetryType.SPAWN); y < getMaxYBound(x, SymmetryType.SPAWN); y++) {
                Vector2f location = new Vector2f(x, y);
                if (!seen.contains(location)) {
                    boolean value = getValueAt(location);
                    Set<Vector2f> coordinates = getShapeCoordinates(location, minArea);
                    seen.addAll(coordinates);
                    if (coordinates.size() < minArea) {
                        fillCoordinates(coordinates, !value);
                    }
                }
            }
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

    public BinaryMask removeAreasOutsideSizeRange(int minSize, int maxSize) {
        removeAreasSmallerThan(minSize);
        removeAreasBiggerThan(maxSize);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask removeAreasInSizeRange(int minSize, int maxSize) {
        minus(this.copy().removeAreasOutsideSizeRange(minSize, maxSize));
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
                    Vector2f newLocation = new Vector2f(next.getX() + e[0], next.getY() + e[1]);
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

    @Override
    public int[][] getInnerCount() {
        int[][] innerCount = new int[getSize()][getSize()];

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                int val = getValueAt(x, y) ? 1 : 0;
                calculateInnerValue(innerCount, x, y, val);
            }
        }
        return innerCount;
    }

    public FloatMask getDistanceField() {
        FloatMask distanceField = new FloatMask(getSize(), random.nextLong(), symmetrySettings);
        distanceField.init(this, getSize() * getSize(), 0f);
        addCalculatedParabolicDistance(distanceField, false);
        addCalculatedParabolicDistance(distanceField, true);
        distanceField.sqrt();
        return distanceField;
    }

    private void addCalculatedParabolicDistance(FloatMask distanceField, boolean useColumns) {
        for (int i = 0; i < getSize(); i++) {
            ArrayList<Vector2f> vertices = new ArrayList<>();
            ArrayList<Vector2f> intersections = new ArrayList<>();
            int index = 0;
            float value;
            if (!useColumns) {
                value = distanceField.getValueAt(i, 0);
            } else {
                value = distanceField.getValueAt(0, i);
            }
            vertices.add(new Vector2f(0, value));
            intersections.add(new Vector2f(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY));
            intersections.add(new Vector2f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
            for (int j = 1; j < getSize(); j++) {
                if (!useColumns) {
                    value = distanceField.getValueAt(i, j);
                } else {
                    value = distanceField.getValueAt(j, i);
                }
                Vector2f current = new Vector2f(j, value);
                Vector2f vertex = vertices.get(index);
                float xIntersect = ((current.getY() + current.getX() * current.getX()) - (vertex.getY() + vertex.getX() * vertex.getX())) / (2 * current.getX() - 2 * vertex.getX());
                while (xIntersect <= intersections.get(index).getX()) {
                    index -= 1;
                    vertex = vertices.get(index);
                    xIntersect = ((current.getY() + current.getX() * current.getX()) - (vertex.getY() + vertex.getX() * vertex.getX())) / (2 * current.getX() - 2 * vertex.getX());
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
                while (intersections.get(index + 1).getX() < j) {
                    index += 1;
                }
                Vector2f vertex = vertices.get(index);
                float dx = j - vertex.getX();
                float height = dx * dx + vertex.getY();
                if (!useColumns) {
                    distanceField.setValueAt(i, j, height);
                } else {
                    distanceField.setValueAt(j, i, height);
                }
            }
        }
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
        return spaceCoordinates(radius, coordinateList);
    }

    public LinkedList<Vector2f> getSpacedCoordinatesEqualTo(boolean value, float radius, int spacing) {
        LinkedList<Vector2f> coordinateList = getAllCoordinatesEqualTo(value, spacing);
        return spaceCoordinates(radius, coordinateList);
    }

    private LinkedList<Vector2f> spaceCoordinates(float radius, LinkedList<Vector2f> coordinateList) {
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
            ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(location, SymmetryType.SPAWN);
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
        ArrayList<SymmetryPoint> symLocationList = getSymmetryPoints(start, SymmetryType.SPAWN);
        Vector2f location = new Vector2f(start);
        Vector2f end = symLocationList.get(0).getLocation();
        int maskSize = getSize();
        while (location.getDistance(end) > distanceThreshold) {
            useBrushRandomly(usesBatchSize, maxDistanceBetweenBrushUse, brush, location, maskSize);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    private void useBrushRandomly(int batchSize, int maxDistanceBetweenBrushUse, BinaryMask brush, Vector2f location, int maskSize) {
        for (int i = 0; i < batchSize; i++) {
            int dx = (random.nextBoolean() ? 1 : -1) * random.nextInt(maxDistanceBetweenBrushUse + 1);
            int dy = (random.nextBoolean() ? 1 : -1) * random.nextInt(maxDistanceBetweenBrushUse + 1);
            location.add(dx, dy).clampMax(maskSize, maskSize).clampMin(0, 0);
            combineWithOffset(brush, location, true, false);
        }
    }

    public BinaryMask connectLocationToLocationFromList(Vector2f start, ArrayList<Vector2f> targetLocations, String brushName, int size, int batchSize,
                                                        float minValue, float maxValue, int maxDistanceBetweenBrushUse, int distanceThreshold) {
        BinaryMask brush = ((FloatMask) loadBrush(brushName, random.nextLong())
                .setSize(size)).convertToBinaryMask(minValue, maxValue);
        int maskSize = getSize();
        Vector2f location = new Vector2f(start);
        while (targetLocations.stream().noneMatch(target -> location.getDistance(target) < distanceThreshold)) {
            useBrushRandomly(batchSize, maxDistanceBetweenBrushUse, brush, location, maskSize);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask connectSpawnsWithRandomBrushUse(ArrayList<Spawn> spawns, int numberOfTeams, float probabilityToConnectTeamAtSpawn,
                                                      String brushName, int size, int batchSize, float minIntensity, float maxIntensity,
                                                      int maxDistanceBrushUse, int distanceThreshold) {
        ArrayList<Vector2f> targetSpawns = new ArrayList<>();
        for (int z = spawns.size() / numberOfTeams; z < spawns.size(); z++) {
            targetSpawns.add(new Vector2f(spawns.get(z).getPosition().getX(), spawns.get(z).getPosition().getZ()));
        }
        for (int z = 0; z < spawns.size() / numberOfTeams; z += numberOfTeams) {
            if (probabilityToConnectTeamAtSpawn > random.nextFloat()) {
                Vector2f spawn = new Vector2f(spawns.get(z).getPosition().getX(), spawns.get(z).getPosition().getZ());
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
                Vector2f spawn = new Vector2f(spawns.get(z).getPosition().getX(), spawns.get(z).getPosition().getZ());
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

    public void checkMatchingSize(Mask<?> other) {
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
