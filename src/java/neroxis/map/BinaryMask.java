package neroxis.map;

import lombok.Getter;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;
import neroxis.util.VisualDebugger;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static neroxis.brushes.Brushes.loadBrush;

@Getter
public strictfp class BinaryMask extends Mask<Boolean> {

    public BinaryMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public BinaryMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public BinaryMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(seed, symmetrySettings, name, parallel);
        this.mask = getEmptyMask(size);
        this.plannedSize = size;
        VisualDebugger.visualizeMask(this);
    }

    public BinaryMask(BinaryMask sourceMask, Long seed) {
        this(sourceMask, seed, null);
    }

    public BinaryMask(BinaryMask sourceMask, Long seed, String name) {
        super(seed, sourceMask.getSymmetrySettings(), name, sourceMask.isParallel());
        this.mask = getEmptyMask(sourceMask.getSize());
        this.plannedSize = sourceMask.getSize();
        setProcessing(sourceMask.isProcessing());
        execute(() -> {
            modify(sourceMask::getValueAt);
            VisualDebugger.visualizeMask(this);
            return this;
        }, sourceMask);
    }

    public BinaryMask(FloatMask sourceMask, float minValue, Long seed) {
        this(sourceMask, minValue, seed, null);
    }

    public BinaryMask(FloatMask sourceMask, float minValue, Long seed, String name) {
        super(seed, sourceMask.getSymmetrySettings(), name, sourceMask.isParallel());
        this.mask = getEmptyMask(sourceMask.getSize());
        this.plannedSize = sourceMask.getSize();
        setProcessing(sourceMask.isProcessing());
        execute(() -> {
            modify((x, y) -> sourceMask.getValueAt(x, y) >= minValue);
            VisualDebugger.visualizeMask(this);
            return this;
        }, sourceMask);
    }

    public BinaryMask(FloatMask sourceMask, float minValue, float maxValue, Long seed, String name) {
        super(seed, sourceMask.getSymmetrySettings(), name, sourceMask.isParallel());
        this.mask = getEmptyMask(sourceMask.getSize());
        this.plannedSize = sourceMask.getSize();
        setProcessing(sourceMask.isProcessing());
        execute(() -> {
            modify((x, y) -> sourceMask.getValueAt(x, y) >= minValue && sourceMask.getValueAt(x, y) < maxValue);
            VisualDebugger.visualizeMask(this);
            return this;
        }, sourceMask);
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

    @Override
    public BinaryMask copy() {
        if (random != null) {
            return new BinaryMask(this, random.nextLong(), getName() + "Copy");
        } else {
            return new BinaryMask(this, null, getName() + "Copy");
        }
    }

    public BinaryMask clear() {
        return execute(() -> {
            apply((x, y) -> setValueAt(x, y, false));
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask init(BinaryMask other) {
        plannedSize = other.getSize();
        return execute(() -> {
            setSize(other.getSize());
            assertCompatibleMask(other);
            combine(other);
            VisualDebugger.visualizeMask(this);
            return this;
        }, other);
    }

    public BinaryMask init(FloatMask other, float threshold) {
        plannedSize = other.getSize();
        return execute(() -> {
            setSize(other.getSize());
            assertCompatibleMask(other);
            combine(other, threshold);
            VisualDebugger.visualizeMask(this);
            return this;
        }, other);
    }

    public BinaryMask randomize(float density) {
        return randomize(density, SymmetryType.TERRAIN);
    }

    public BinaryMask randomize(float density, SymmetryType symmetryType) {
        return execute(() -> {
            modifyWithSymmetry(symmetryType, (x, y) -> random.nextFloat() < density);
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask flipValues(float density) {
        return execute(() -> {
            modifyWithSymmetry(SymmetryType.SPAWN, (x, y) -> getValueAt(x, y) && random.nextFloat() < density);
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }


    public BinaryMask randomWalk(int numWalkers, int numSteps) {
        return execute(() -> {
            for (int i = 0; i < numWalkers; i++) {
                int maxXBound = getMaxXBound(SymmetryType.TERRAIN);
                int minXBound = getMinXBound(SymmetryType.TERRAIN);
                int x = random.nextInt(maxXBound - minXBound) + minXBound;
                int maxYBound = getMaxYBound(x, SymmetryType.TERRAIN);
                int minYBound = getMinYBound(x, SymmetryType.TERRAIN);
                int y = random.nextInt(maxYBound - minYBound + 1) + minYBound;
                for (int j = 0; j < numSteps; j++) {
                    if (inBounds(x, y)) {
                        setValueAt(x, y, true);
                        getSymmetryPoints(x, y, SymmetryType.TERRAIN).forEach(symmetryPoint -> setValueAt(symmetryPoint, true));
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
        });
    }

    public BinaryMask randomWalkWithBrush(Vector2f start, String brushName, int size, int numberOfUses,
                                          float minValue, float maxValue, int maxStepSize) {
        return execute(() -> {
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
        });
    }

    public BinaryMask guidedWalkWithBrush(Vector2f start, Vector2f target, String brushName, int size, int numberOfUses,
                                          float minValue, float maxValue, int maxStepSize, boolean wrapEdges) {
        return execute(() -> {
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
        });
    }

    public BinaryMask path(Vector2f start, Vector2f end, float maxStepSize, int numMiddlePoints, float midPointMaxDistance, float midPointMinDistance, float maxAngleError, SymmetryType symmetryType) {
        return execute(() -> {
            List<Vector2f> checkPoints = new ArrayList<>();
            checkPoints.add(new Vector2f(start));
            for (int i = 0; i < numMiddlePoints; i++) {
                Vector2f previousLoc = checkPoints.get(checkPoints.size() - 1);
                float angle = (float) ((random.nextFloat() - .5f) * 2 * StrictMath.PI / 2f) + previousLoc.getAngle(end);
                if (symmetrySettings.getTerrainSymmetry() == Symmetry.POINT4 && angle % (StrictMath.PI / 2) < StrictMath.PI / 8) {
                    angle += (random.nextBoolean() ? -1 : 1) * (random.nextFloat() * .5f + .5f) * 2f * StrictMath.PI / 4f;
                }
                float magnitude = random.nextFloat() * (midPointMaxDistance - midPointMinDistance) + midPointMinDistance;
                Vector2f nextLoc = new Vector2f(previousLoc).addPolar(angle, magnitude);
                checkPoints.add(nextLoc);
            }
            checkPoints.add(new Vector2f(end));
            checkPoints.forEach(point -> point.round().clampMin(0, 0).clampMax(getSize() - 1, getSize() - 1));
            int size = getSize();
            int numSteps = 0;
            for (int i = 0; i < checkPoints.size() - 1; i++) {
                Vector2f location = checkPoints.get(i);
                Vector2f nextLoc = checkPoints.get(i + 1);
                float oldAngle = location.getAngle(nextLoc) + (random.nextFloat() - .5f) * 2f * maxAngleError;
                while (location.getDistance(nextLoc) > maxStepSize && numSteps < size * size) {
                    List<Vector2f> symmetryPoints = getSymmetryPoints(location, symmetryType);
                    if (inBounds(location) && symmetryPoints.stream().allMatch(this::inBounds)) {
                        setValueAt(location, true);
                        symmetryPoints.forEach(symmetryPoint -> setValueAt(symmetryPoint, true));
                    }
                    float magnitude = StrictMath.max(1, random.nextFloat() * maxStepSize);
                    float angle = oldAngle * .5f + location.getAngle(nextLoc) * .5f + (random.nextFloat() - .5f) * 2f * maxAngleError;
                    location.addPolar(angle, magnitude).round();
                    oldAngle = angle;
                    numSteps++;
                }
                if (numSteps >= size * size) {
                    break;
                }
            }
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask connect(Vector2f start, Vector2f end, float maxStepSize, int numMiddlePoints, float midPointMaxDistance, float midPointMinDistance, float maxAngleError, SymmetryType symmetryType) {
        return execute(() -> {
            path(start, end, maxStepSize, numMiddlePoints, midPointMaxDistance, midPointMinDistance, maxAngleError, symmetryType);
            if (symmetrySettings.getSymmetry(symmetryType).getNumSymPoints() > 1) {
                List<Vector2f> symmetryPoints = getSymmetryPointsWithOutOfBounds(end, symmetryType);
                path(start, symmetryPoints.get(0), maxStepSize, numMiddlePoints, midPointMaxDistance, midPointMinDistance, maxAngleError, symmetryType);
            }
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask progressiveWalk(int numWalkers, int numSteps) {
        return execute(() -> {
            for (int i = 0; i < numWalkers; i++) {
                int x = random.nextInt(getMaxXBound(SymmetryType.TERRAIN) - getMinXBound(SymmetryType.TERRAIN)) + getMinXBound(SymmetryType.TERRAIN);
                int y = random.nextInt(getMaxYBound(x, SymmetryType.TERRAIN) - getMinYBound(x, SymmetryType.TERRAIN) + 1) + getMinYBound(x, SymmetryType.TERRAIN);
                List<Integer> directions = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
                int regressiveDir = random.nextInt(directions.size());
                directions.remove(regressiveDir);
                for (int j = 0; j < numSteps; j++) {
                    if (inBounds(x, y)) {
                        setValueAt(x, y, true);
                        getSymmetryPoints(x, y, SymmetryType.TERRAIN).forEach(symmetryPoint -> setValueAt(symmetryPoint, true));
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
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask space(float minSpacing, float maxSpacing) {
        return execute(() -> {
            List<Vector2f> coordinates = getRandomCoordinates(minSpacing, maxSpacing);
            List<Vector2f> symmetricCoordinates = new ArrayList<>();
            coordinates.forEach(coordinate -> symmetricCoordinates.addAll(getSymmetryPoints(coordinate, SymmetryType.SPAWN)));
            coordinates.addAll(symmetricCoordinates);
            clear();
            fillCoordinates(coordinates, true);
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask invert() {
        return execute(() -> {
            modify((x, y) -> !getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask inflate(float radius) {
        return execute(() -> {
            Boolean[][] maskCopy = getEmptyMask(getSize());
            apply((x, y) -> {
                if (getValueAt(x, y) && isEdge(x, y)) {
                    markInRadius(radius, maskCopy, x, y, true);
                }
            });
            modify((x, y) -> maskCopy[x][y] || getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask deflate(float radius) {
        return execute(() -> {
            Boolean[][] maskCopy = getEmptyMask(getSize());
            apply((x, y) -> {
                if (!getValueAt(x, y) && isEdge(x, y)) {
                    markInRadius(radius, maskCopy, x, y, true);
                }
            });
            modify((x, y) -> !maskCopy[x][y] && getValueAt(x, y));

            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    private void markInRadius(float radius, Boolean[][] maskCopy, int x, int y, boolean value) {
        float radius2 = (radius + 0.5f) * (radius + 0.5f);
        int minX = StrictMath.round(x - radius);
        int maxX = StrictMath.round(x + radius + 1);
        int minY = StrictMath.round(y - radius);
        int maxY = StrictMath.round(y + radius + 1);
        for (int x2 = minX; x2 < maxX; x2++) {
            for (int y2 = minY; y2 < maxY; y2++) {
                if (inBounds(x2, y2) && maskCopy[x2][y2] != value && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2) {
                    maskCopy[x2][y2] = value;
                }
            }
        }
    }

    public BinaryMask cutCorners() {
        return execute(() -> {
            int size = getSize();
            Boolean[][] maskCopy = getEmptyMask(size);
            apply((x, y) -> {
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
            });
            mask = maskCopy;
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask acid(float strength, float size) {
        return execute(() -> {
            BinaryMask holes = new BinaryMask(getSize(), random.nextLong(), symmetrySettings, getName() + "holes");
            holes.randomize(strength, SymmetryType.SPAWN).inflate(size);
            minus(holes);
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask dilute(float strength) {
        return dilute(strength, SymmetryType.TERRAIN);
    }

    public BinaryMask dilute(float strength, SymmetryType symmetryType) {
        return dilute(strength, symmetryType, 1);
    }

    public BinaryMask dilute(float strength, SymmetryType symmetryType, int count) {
        return execute(() -> {
            for (int i = 0; i < count; i++) {
                Boolean[][] newMask = getEmptyMask(getSize());
                applyWithSymmetry(symmetryType, (x, y) -> {
                    boolean value = getValueAt(x, y) || (isEdge(x, y) && random.nextFloat() < strength);
                    newMask[x][y] = value;
                    List<Vector2f> symPoints = getSymmetryPoints(x, y, symmetryType);
                    symPoints.forEach(symmetryPoint -> newMask[(int) symmetryPoint.getX()][(int) symmetryPoint.getY()] = value);
                });
                mask = newMask;
            }
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask erode(float strength) {
        return erode(strength, SymmetryType.TERRAIN);
    }

    public BinaryMask erode(float strength, SymmetryType symmetryType) {
        return erode(strength, symmetryType, 1);
    }

    public BinaryMask erode(float strength, SymmetryType symmetryType, int count) {
        return execute(() -> {
            for (int i = 0; i < count; i++) {
                Boolean[][] newMask = getEmptyMask(getSize());
                applyWithSymmetry(symmetryType, (x, y) -> {
                    boolean value = getValueAt(x, y) && (!isEdge(x, y) || random.nextFloat() > strength);
                    newMask[x][y] = value;
                    List<Vector2f> symPoints = getSymmetryPoints(x, y, symmetryType);
                    symPoints.forEach(symmetryPoint -> newMask[(int) symmetryPoint.getX()][(int) symmetryPoint.getY()] = value);
                });
                mask = newMask;
            }
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask outline() {
        return execute(() -> {
            Boolean[][] maskCopy = getEmptyMask(getSize());
            apply((x, y) -> maskCopy[x][y] = isEdge(x, y));
            mask = maskCopy;
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask interpolate() {
        return blur(1, .35f);
    }

    public BinaryMask blur(int radius) {
        return blur(radius, .5f);
    }

    public BinaryMask blur(int radius, float density) {
        return execute(() -> {
            int[][] innerCount = getInnerCount();
            modify((x, y) -> calculateAreaAverage(radius, x, y, innerCount) >= density);
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask replace(BinaryMask other) {
        return execute(() -> {
            assertCompatibleMask(other);
            modify(other::getValueAt);
            VisualDebugger.visualizeMask(this);
            return this;
        }, other);
    }

    public BinaryMask combine(BinaryMask other) {
        return execute(() -> {
            assertCompatibleMask(other);
            modify((x, y) -> getValueAt(x, y) || other.getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
            return this;
        }, other);
    }

    public BinaryMask combine(FloatMask other, float minValue) {
        return execute(() -> {
            modify((x, y) -> other.getValueAt(x, y) >= minValue);
            VisualDebugger.visualizeMask(this);
            return this;
        }, other);
    }

    public BinaryMask combine(FloatMask other, float minValue, float maxValue) {
        return execute(() -> {
            modify((x, y) -> other.getValueAt(x, y) >= minValue && other.getValueAt(x, y) <= maxValue);
            VisualDebugger.visualizeMask(this);
            return this;
        }, other);
    }

    private BinaryMask combineWithOffset(BinaryMask other, Vector2f loc, boolean centered, boolean wrapEdges) {
        return combineWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    private BinaryMask combineWithOffset(BinaryMask other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
        return execute(() -> {
            int size = getSize();
            int otherSize = other.getSize();
            int smallerSize = StrictMath.min(size, otherSize);
            int offsetX;
            int offsetY;
            if (center) {
                offsetX = xCoordinate - smallerSize / 2;
                offsetY = yCoordinate - smallerSize / 2;
            } else {
                offsetX = xCoordinate;
                offsetY = yCoordinate;
            }
            if (size >= otherSize) {
                apply((x, y) -> {
                    int shiftX = getShiftedValue(x, offsetX, size, wrapEdges);
                    int shiftY = getShiftedValue(y, offsetY, size, wrapEdges);
                    if (inBounds(shiftX, shiftY) && other.getValueAt(x, y)) {
                        setValueAt(shiftX, shiftY, true);
                        List<Vector2f> symmetryPoints = getSymmetryPoints(shiftX, shiftY, SymmetryType.SPAWN);
                        for (Vector2f symmetryPoint : symmetryPoints) {
                            setValueAt(symmetryPoint, true);
                        }
                    }
                });
            } else {
                other.apply((x, y) -> {
                    int shiftX = getShiftedValue(x, offsetX, otherSize, wrapEdges);
                    int shiftY = getShiftedValue(y, offsetY, otherSize, wrapEdges);
                    if (other.inBounds(shiftX, shiftY) && other.getValueAt(shiftX, shiftY)) {
                        setValueAt(x, y, true);
                    }
                });
            }
            return this;
        }, other);
    }

    private BinaryMask combineWithOffset(FloatMask other, float minValue, float maxValue, Vector2f location, boolean wrapEdges) {
        return execute(() -> {
            combineWithOffset(other.convertToBinaryMask(minValue, maxValue), location, true, wrapEdges);
            return this;
        }, other);
    }

    public BinaryMask combineBrush(Vector2f location, String brushName, float minValue, float maxValue, int size) {
        return execute(() -> {
            FloatMask brush = (FloatMask) loadBrush(brushName, random.nextLong()).setSize(size);
            combineWithOffset(brush, minValue, maxValue, location, false);
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask intersect(BinaryMask other) {
        return execute(() -> {
            assertCompatibleMask(other);
            modify((x, y) -> getValueAt(x, y) && other.getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
            return this;
        }, other);
    }

    public BinaryMask minus(BinaryMask other) {
        return execute(() -> {
            assertCompatibleMask(other);
            modify((x, y) -> getValueAt(x, y) && !other.getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
            return this;
        }, other);
    }

    public BinaryMask limitToSymmetryRegion() {
        return limitToSymmetryRegion(SymmetryType.TEAM);
    }

    public BinaryMask limitToSymmetryRegion(SymmetryType symmetryType) {
        return execute(() -> {
            int minXBound = getMinXBound(symmetryType);
            int maxXBound = getMaxXBound(symmetryType);
            modify((x, y) -> getValueAt(x, y) && !(x < minXBound || x >= maxXBound || y < getMinYBound(x, symmetryType) || y >= getMaxYBound(x, symmetryType)));
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask limitToCenteredCircle(float circleRadius) {
        return execute(() -> {
            BinaryMask symmetryLimit = new BinaryMask(getSize(), random.nextLong(), symmetrySettings, getName() + "symmetryLimit");
            symmetryLimit.fillCircle(getSize() / 2f, getSize() / 2f, circleRadius, true);
            intersect(symmetryLimit);
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask fillSides(int extent, boolean value) {
        return fillSides(extent, value, SymmetryType.TEAM);
    }

    public BinaryMask fillSides(int extent, boolean value, SymmetryType symmetryType) {
        return execute(() -> {
            switch (symmetrySettings.getSymmetry(symmetryType)) {
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
            applySymmetry(symmetryType);
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask fillCenter(int extent, boolean value) {
        return fillCenter(extent, value, SymmetryType.SPAWN);
    }

    public BinaryMask fillCenter(int extent, boolean value, SymmetryType symmetryType) {
        return execute(() -> {
            switch (symmetrySettings.getSymmetry(symmetryType)) {
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
                        fillDiagonal(extent * 3 / 8, false, value);
                        fillDiagonal(extent * 3 / 8, true, value);
                    } else {
                        fillDiagonal(extent * 3 / 16, false, value);
                        fillDiagonal(extent * 3 / 16, true, value);
                        fillCenter(extent, value, SymmetryType.TEAM);
                    }
                    break;
                case QUAD:
                    if (symmetrySettings.getTeamSymmetry() == Symmetry.QUAD) {
                        fillRect(getSize() / 2 - extent / 4, 0, extent / 2, getSize(), value);
                        fillRect(0, getSize() / 2 - extent / 4, getSize(), extent / 2, value);
                    } else {
                        fillRect(getSize() / 2 - extent / 8, 0, extent / 4, getSize(), value);
                        fillRect(0, getSize() / 2 - extent / 8, getSize(), extent / 4, value);
                        fillCenter(extent, value, SymmetryType.TEAM);
                    }
                    break;
            }
            applySymmetry(SymmetryType.SPAWN);
            VisualDebugger.visualizeMask(this);
            return this;
        });
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
        return execute(() -> {
            float dx;
            float dy;
            float radius2 = (radius + .5f) * (radius + .5f);
            float radiansToDegreeFactor = (float) (180 / StrictMath.PI);
            for (int cx = StrictMath.round(x - radius); cx < StrictMath.round(x + radius + 1); cx++) {
                for (int cy = StrictMath.round(y - radius); cy < StrictMath.round(y + radius + 1); cy++) {
                    dx = x - cx;
                    dy = y - cy;
                    float angle = (float) (StrictMath.atan2(dy, dx) / radiansToDegreeFactor + 360) % 360;
                    if (inBounds(cx, cy) && dx * dx + dy * dy <= radius2 && angle >= startAngle && angle <= endAngle) {
                        setValueAt(cx, cy, value);
                    }
                }
            }
            VisualDebugger.visualizeMask(this);
            return this;
        });
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
        return execute(() -> {
            int smallX = StrictMath.min(x1, x2);
            int bigX = StrictMath.max(x1, x2);
            int smallZ = StrictMath.min(z1, z2);
            int bigZ = StrictMath.max(z1, z2);
            return fillRect(smallX, smallZ, bigX - smallX, bigZ - smallZ, value);
        });
    }

    public BinaryMask fillParallelogram(Vector2f v, int width, int height, int xSlope, int ySlope, boolean value) {
        return fillParallelogram((int) v.getX(), (int) v.getY(), width, height, xSlope, ySlope, value);
    }

    public BinaryMask fillParallelogram(int x, int y, int width, int height, int xSlope, int ySlope, boolean value) {
        return execute(() -> {
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
        });
    }

    public BinaryMask fillDiagonal(int extent, boolean inverted, boolean value) {
        return execute(() -> {
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
        });
    }

    public BinaryMask fillEdge(int rimWidth, boolean value) {
        return execute(() -> {
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
        });
    }

    public BinaryMask fillShape(Vector2f location) {
        return execute(() -> {
            fillCoordinates(getShapeCoordinates(location), !getValueAt(location));
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask fillCoordinates(Collection<Vector2f> coordinates, boolean value) {
        return execute(() -> {
            coordinates.forEach(location -> setValueAt(location, value));
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask fillGaps(int minDist) {
        return execute(() -> {
            BinaryMask filledGaps = getDistanceField().getLocalMaximums(1f, minDist / 2f);
            filledGaps.inflate(minDist / 2f);
            combine(filledGaps);
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask widenGaps(int minDist) {
        return execute(() -> {
            BinaryMask filledGaps = getDistanceField().getLocalMaximums(1f, minDist / 2f);
            filledGaps.inflate(minDist / 2f);
            minus(filledGaps);
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask removeAreasSmallerThan(int minArea) {
        return execute(() -> {
            Set<Vector2f> seen = new HashSet<>(getSize() * getSize() * 2);
            applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> {
                Vector2f location = new Vector2f(x, y);
                if (!seen.contains(location)) {
                    boolean value = getValueAt(location);
                    Set<Vector2f> coordinates = getShapeCoordinates(location, minArea);
                    seen.addAll(coordinates);
                    if (coordinates.size() < minArea) {
                        fillCoordinates(coordinates, !value);
                    }
                }
            });
            applySymmetry(SymmetryType.SPAWN);
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask removeAreasBiggerThan(int maxArea) {
        return execute(() -> {
            minus(copy().removeAreasSmallerThan(maxArea));
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask removeAreasOutsideSizeRange(int minSize, int maxSize) {
        return execute(() -> {
            removeAreasSmallerThan(minSize);
            removeAreasBiggerThan(maxSize);
            VisualDebugger.visualizeMask(this);
            return this;
        });
    }

    public BinaryMask removeAreasInSizeRange(int minSize, int maxSize) {
        return execute(() -> {
            minus(this.copy().removeAreasOutsideSizeRange(minSize, maxSize));
            VisualDebugger.visualizeMask(this);
            return this;
        });
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

    @Override
    protected int[][] getInnerCount() {
        int[][] innerCount = new int[getSize()][getSize()];
        apply((x, y) -> calculateInnerValue(innerCount, x, y, getValueAt(x, y) ? 1 : 0));
        return innerCount;
    }

    public FloatMask getDistanceField() {
        FloatMask distanceField = new FloatMask(getSize(), random.nextLong(), symmetrySettings, getName() + "distanceField");
        distanceField.init(this, getSize() * getSize(), 0f);
        addCalculatedParabolicDistance(distanceField, false);
        addCalculatedParabolicDistance(distanceField, true);
        distanceField.sqrt();
        return distanceField;
    }

    private void addCalculatedParabolicDistance(FloatMask distanceField, boolean useColumns) {
        for (int i = 0; i < getSize(); i++) {
            List<Vector2f> vertices = new ArrayList<>();
            List<Vector2f> intersections = new ArrayList<>();
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
        AtomicInteger cellCount = new AtomicInteger();
        apply((x, y) -> {
            if (getValueAt(x, y)) {
                cellCount.incrementAndGet();
            }
        });
        return cellCount.get();
    }

    public LinkedList<Vector2f> getAllCoordinates(int spacing) {
        LinkedList<Vector2f> coordinates = new LinkedList<>();
        for (int x = 0; x < getSize(); x += spacing) {
            for (int y = 0; y < getSize(); y += spacing) {
                Vector2f location = new Vector2f(x, y);
                coordinates.addLast(location);
            }
        }
        return coordinates;
    }

    public LinkedList<Vector2f> getAllCoordinatesEqualTo(boolean value, int spacing) {
        LinkedList<Vector2f> coordinates = new LinkedList<>();
        for (int x = 0; x < getSize(); x += spacing) {
            for (int y = 0; y < getSize(); y += spacing) {
                if (getValueAt(x, y) == value) {
                    Vector2f location = new Vector2f(x, y);
                    coordinates.addLast(location);
                }
            }
        }
        return coordinates;
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
        LinkedList<Vector2f> chosenCoordinates = new LinkedList<>();
        while (coordinateList.size() > 0) {
            Vector2f location = coordinateList.removeFirst();
            chosenCoordinates.addLast(location);
            coordinateList.removeIf((loc) -> location.getDistance(loc) < radius);
        }
        return chosenCoordinates;
    }

    public LinkedList<Vector2f> getRandomCoordinates(float spacing) {
        return getRandomCoordinates(spacing, spacing);
    }

    public LinkedList<Vector2f> getRandomCoordinates(float minSpacing, float maxSpacing) {
        LinkedList<Vector2f> coordinateList = getAllCoordinatesEqualTo(true, 1);
        LinkedList<Vector2f> chosenCoordinates = new LinkedList<>();
        while (coordinateList.size() > 0) {
            Vector2f location = coordinateList.remove(random.nextInt(coordinateList.size()));
            float spacing = random.nextFloat() * (maxSpacing - minSpacing) + minSpacing;
            chosenCoordinates.addLast(location);
            coordinateList.removeIf((loc) -> location.getDistance(loc) < spacing);
            List<Vector2f> symmetryPoints = getSymmetryPoints(location, SymmetryType.SPAWN);
            symmetryPoints.forEach(symmetryPoint -> coordinateList.removeIf((loc) -> symmetryPoint.getDistance(loc) < spacing));
        }
        return chosenCoordinates;
    }

    public Vector2f getRandomPosition() {
        List<Vector2f> coordinates = new ArrayList<>(getAllCoordinatesEqualTo(true, 1));
        if (coordinates.size() == 0)
            return null;
        int cell = random.nextInt(coordinates.size());
        return coordinates.get(cell);
    }

    // --------------------------------------------------
    @Override
    public Mask<Boolean> mockClone() {
        return new BinaryMask(this, 0L, MOCKED_NAME);
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        ByteBuffer bytes = ByteBuffer.allocate(getSize() * getSize());
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> bytes.put(getValueAt(x, y) ? (byte) 1 : 0));
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
}
