package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.BezierCurve;
import com.faforever.neroxis.util.Vector2;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static com.faforever.neroxis.brushes.Brushes.loadBrush;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public strictfp class BooleanMask extends PrimitiveMask<Boolean, BooleanMask> {
    private static final int BOOLEANS_PER_LONG = 64;
    private static final long SINGLE_BIT_VALUE = 1;
    private long[] mask;
    private int maskBooleanSize;

    public BooleanMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public BooleanMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public BooleanMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public BooleanMask(BooleanMask other) {
        this(other, (String) null);
    }

    public BooleanMask(BooleanMask other, String name) {
        super(other, name);
    }

    public <T extends ComparableMask<U, ?>, U extends Comparable<U>> BooleanMask(T other, U minValue) {
        this(other, minValue, null);
    }

    public <T extends ComparableMask<U, ?>, U extends Comparable<U>> BooleanMask(T other, U minValue, String name) {
        this(other.getSize(), other.getNextSeed(), other.getSymmetrySettings(), name, other.isParallel());
        enqueue(dependencies -> {
            T source = (T) dependencies.get(0);
            set(point -> source.valueAtGreaterThanEqualTo(point, minValue));
        }, other);
    }

    public <T extends ComparableMask<U, ?>, U extends Comparable<U>> BooleanMask(T other, U minValue, U maxValue, Long seed) {
        this(other, minValue, maxValue, seed, null);
    }

    public <T extends ComparableMask<U, ?>, U extends Comparable<U>> BooleanMask(T other, U minValue, U maxValue, Long seed, String name) {
        this(other.getSize(), seed, other.getSymmetrySettings(), name, other.isParallel());
        enqueue(dependencies -> {
            T source = (T) dependencies.get(0);
            set(point -> source.valueAtGreaterThanEqualTo(point, minValue) && source.valueAtLessThanEqualTo(point.x, point.y, maxValue));
        }, other);
    }

    private static int minimumArraySize(int size) {
        return (int) StrictMath.ceil((float) size * size / BOOLEANS_PER_LONG);
    }

    private static int arrayIndex(int bitIndex) {
        return (int) StrictMath.floor((float) bitIndex / BOOLEANS_PER_LONG);
    }

    private static int bitIndex(Point point, int size) {
        return bitIndex(point.x, point.y, size);
    }

    private static int bitIndex(int x, int y, int size) {
        return x * size + y;
    }

    @Override
    protected void initializeMask(int size) {
        mask = new long[minimumArraySize(size)];
        maskBooleanSize = size;
    }

    @Override
    public Boolean getAvg() {
        float size = getSize();
        return getCount() / size / size > .5f;
    }

    @Override
    public BooleanMask add(BooleanMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            for (int i = 0; i < mask.length; i++) {
                mask[i] |= source.mask[i];
            }
        }, other);
    }

    @Override
    protected void addValueAt(int x, int y, Boolean value) {
        set(x, y, value | get(x, y));
    }

    @Override
    public BooleanMask subtract(BooleanMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            for (int i = 0; i < mask.length; i++) {
                mask[i] &= ~source.mask[i];
            }
        }, other);
    }

    @Override
    protected void subtractValueAt(int x, int y, Boolean value) {
        set(x, y, !value & get(x, y));
    }

    @Override
    public BooleanMask multiply(BooleanMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            for (int i = 0; i < mask.length; i++) {
                mask[i] &= source.mask[i];
            }
        }, other);
    }

    @Override
    protected void multiplyValueAt(int x, int y, Boolean value) {
        set(x, y, value & get(x, y));
    }

    @Override
    public BooleanMask divide(BooleanMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            for (int i = 0; i < mask.length; i++) {
                mask[i] ^= source.mask[i];
            }
        }, other);
    }

    @Override
    protected void divideValueAt(int x, int y, Boolean value) {
        set(x, y, value ^ get(x, y));
    }

    @Override
    public Boolean getSum() {
        throw new UnsupportedOperationException("Sum not supported for BooleanMask");
    }

    @Override
    public Boolean getMin() {
        throw new UnsupportedOperationException("Min not supported for BooleanMask");
    }

    @Override
    public Boolean getMax() {
        throw new UnsupportedOperationException("Max not supported for BooleanMask");
    }

    @Override
    protected Boolean getZeroValue() {
        return false;
    }

    @Override
    public Boolean get(int x, int y) {
        return getBit(x, y, getSize(), mask);
    }

    @Override
    protected void set(int x, int y, Boolean value) {
        setBit(x, y, value, getSize(), mask);
    }

    private static boolean getBit(int x, int y, int size, long[] mask) {
        return getBit(bitIndex(x, y, size), mask);
    }

    private static boolean getBit(int bitIndex, long[] mask) {
        return (mask[arrayIndex(bitIndex)] & (SINGLE_BIT_VALUE << bitIndex)) != 0;
    }

    private static void setBit(Point point, boolean value, int size, long[] mask) {
        setBit(point.x, point.y, value, size, mask);
    }

    private static void setBit(int x, int y, boolean value, int size, long[] mask) {
        setBit(bitIndex(x, y, size), value, mask);
    }

    private static void setBit(int bitIndex, boolean value, long[] mask) {
        if (value) {
            mask[arrayIndex(bitIndex)] |= SINGLE_BIT_VALUE << bitIndex;
        } else {
            mask[arrayIndex(bitIndex)] &= ~(SINGLE_BIT_VALUE << bitIndex);
        }
    }

    @Override
    public int getImmediateSize() {
        return maskBooleanSize;
    }

    @Override
    protected BooleanMask setSizeInternal(int newSize) {
        return enqueue(() -> {
            int oldSize = getSize();
            if (oldSize == 1) {
                boolean value = get(0, 0);
                initializeMask(newSize);
                fill(value);
            } else if (oldSize != newSize) {
                long[] oldMask = mask;
                initializeMask(newSize);
                Map<Integer, Integer> coordinateMap = getSymmetricScalingCoordinateMap(oldSize, newSize);
                set(point -> getBit(coordinateMap.get(point.x), coordinateMap.get(point.y), oldSize, oldMask));
            }
        });
    }

    private long[] getMaskCopy() {
        int arraySize = mask.length;
        long[] maskCopy = new long[arraySize];
        System.arraycopy(mask, 0, maskCopy, 0, arraySize);
        return maskCopy;
    }

    @Override
    protected BooleanMask copyFrom(BooleanMask other) {
        return enqueue((dependencies) -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            fill(source.mask, source.maskBooleanSize);
        }, other);
    }

    protected BooleanMask fill(Boolean value) {
        int arrayLength = mask.length;
        mask[0] = value ? ~0 : 0;
        for (int i = 1; i < arrayLength; i += i) {
            System.arraycopy(mask, 0, mask, i, StrictMath.min((arrayLength - i), i));
        }
        return this;
    }

    protected BooleanMask fill(long[] arrayToFillFrom, int maskBooleanSize) {
        assertNotPipelined();
        int arraySize = arrayToFillFrom.length;
        mask = new long[arraySize];
        this.maskBooleanSize = maskBooleanSize;
        System.arraycopy(arrayToFillFrom, 0, mask, 0, arraySize);
        return this;
    }

    public boolean isEdge(Point point) {
        return isEdge(point.x, point.y);
    }

    public boolean isEdge(int x, int y) {
        boolean value = get(x, y);
        int size = getSize();
        return ((x > 0 && get(x - 1, y) != value)
                || (y > 0 && get(x, y - 1) != value)
                || (x < size - 1 && get(x + 1, y) != value)
                || (y < size - 1 && get(x, y + 1) != value));
    }

    public <T extends Comparable<T>, U extends ComparableMask<T, U>> BooleanMask init(ComparableMask<T, U> other, T minValue) {
        init(other.convertToBooleanMask(minValue));
        return this;
    }

    public <T extends Comparable<T>, U extends ComparableMask<T, U>> BooleanMask init(ComparableMask<T, U> other, T minValue, T maxValue) {
        init(other.convertToBooleanMask(minValue, maxValue));
        return this;
    }

    public BooleanMask randomize(float density) {
        return randomize(density, SymmetryType.TERRAIN);
    }

    public BooleanMask randomize(float density, SymmetryType symmetryType) {
        return enqueue(() -> setWithSymmetry(symmetryType, point -> random.nextFloat() < density));
    }

    public BooleanMask flipValues(float density) {
        enqueue(() -> setWithSymmetry(SymmetryType.SPAWN, point -> get(point) && random.nextFloat() < density));
        return this;
    }

    public BooleanMask randomWalk(int numWalkers, int numSteps) {
        enqueue(() -> {
            for (int i = 0; i < numWalkers; i++) {
                int maxXBound = getMaxXBound(SymmetryType.TERRAIN);
                int minXBound = getMinXBound(SymmetryType.TERRAIN);
                int x = random.nextInt(maxXBound - minXBound) + minXBound;
                int maxYBound = getMaxYBound(x, SymmetryType.TERRAIN);
                int minYBound = getMinYBound(x, SymmetryType.TERRAIN);
                int y = random.nextInt(maxYBound - minYBound + 1) + minYBound;
                for (int j = 0; j < numSteps; j++) {
                    if (inBounds(x, y)) {
                        applyAtSymmetryPoints(x, y, SymmetryType.TERRAIN, spoint -> set(spoint, true));
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
        });
        return this;
    }

    public BooleanMask guidedWalkWithBrush(Vector2 start, Vector2 target, String brushName, int size, int numberOfUses,
                                           float minValue, float maxValue, int maxStepSize, boolean wrapEdges) {
        enqueue(() -> {
            Vector2 location = new Vector2(start);
            BooleanMask brush = loadBrush(brushName, null)
                    .setSize(size).convertToBooleanMask(minValue, maxValue);
            for (int i = 0; i < numberOfUses; i++) {
                addWithOffset(brush, location, true, wrapEdges);
                int dx = (target.getX() > location.getX() ? 1 : -1) * random.nextInt(maxStepSize + 1);
                int dy = (target.getY() > location.getY() ? 1 : -1) * random.nextInt(maxStepSize + 1);
                location.add(dx, dy);
            }
        });
        return this;
    }

    public BooleanMask path(Vector2 start, Vector2 end, float maxStepSize, int numMiddlePoints, float midPointMaxDistance, float midPointMinDistance, float maxAngleError, SymmetryType symmetryType) {
        enqueue(() -> {
            int size = getSize();
            List<Vector2> checkPoints = new ArrayList<>();
            checkPoints.add(new Vector2(start));
            for (int i = 0; i < numMiddlePoints; i++) {
                Vector2 previousLoc = checkPoints.get(checkPoints.size() - 1);
                float angle = (float) ((random.nextFloat() - .5f) * 2 * StrictMath.PI / 2f) + previousLoc.angleTo(end);
                if (symmetrySettings.getTerrainSymmetry() == Symmetry.POINT4 && angle % (StrictMath.PI / 2) < StrictMath.PI / 8) {
                    angle += (random.nextBoolean() ? -1 : 1) * (random.nextFloat() * .5f + .5f) * 2f * StrictMath.PI / 4f;
                }
                float magnitude = random.nextFloat() * (midPointMaxDistance - midPointMinDistance) + midPointMinDistance;
                Vector2 nextLoc = new Vector2(previousLoc).addPolar(angle, magnitude);
                checkPoints.add(nextLoc);
            }
            checkPoints.add(new Vector2(end));
            checkPoints.forEach(point -> point.round().clampMin(0f).clampMax(size - 1));
            int numSteps = 0;
            for (int i = 0; i < checkPoints.size() - 1; i++) {
                Vector2 location = checkPoints.get(i);
                Vector2 nextLoc = checkPoints.get(i + 1);
                float oldAngle = location.angleTo(nextLoc) + (random.nextFloat() - .5f) * 2f * maxAngleError;
                while (location.getDistance(nextLoc) > maxStepSize && numSteps < size * size) {
                    List<Vector2> symmetryPoints = getSymmetryPoints(location, symmetryType);
                    if (inBounds(location) && symmetryPoints.stream().allMatch(this::inBounds)) {
                        applyAtSymmetryPoints((int) location.getX(), (int) location.getY(), SymmetryType.TERRAIN, spoint -> set(spoint, true));
                    }
                    float magnitude = StrictMath.max(1, random.nextFloat() * maxStepSize);
                    float angle = oldAngle * .5f + location.angleTo(nextLoc) * .5f + (random.nextFloat() - .5f) * 2f * maxAngleError;
                    location.addPolar(angle, magnitude).round();
                    oldAngle = angle;
                    numSteps++;
                }
                if (numSteps >= size * size) {
                    break;
                }
            }
        });
        return this;
    }

    public BooleanMask pathBezier(Vector2 start, Vector2 end, int minOrder, int maxOrder, int numMiddlePoints, float midPointMaxDistance, float midPointMinDistance) {
        enqueue(() -> {
            int size = getSize();
            List<Vector2> checkPoints = new ArrayList<>();
            checkPoints.add(new Vector2(start));
            for (int i = 0; i < numMiddlePoints; i++) {
                Vector2 previousLoc = checkPoints.get(checkPoints.size() - 1);
                float angle = (float) ((random.nextFloat() - .5f) * 2 * StrictMath.PI / 2f) + previousLoc.angleTo(end);
                float magnitude = random.nextFloat() * start.getDistance(end) / numMiddlePoints;
                Vector2 nextLoc = previousLoc.copy().addPolar(angle, magnitude);
                checkPoints.add(nextLoc);
            }
            checkPoints.add(end.copy());
            checkPoints.forEach(point -> point.round().clampMin(0f).clampMax(size - 1));
            for (int i = 0; i < checkPoints.size() - 1; i++) {
                Vector2 location = checkPoints.get(i);
                Vector2 nextLoc = checkPoints.get(i + 1);
                BezierCurve bezierCurve = new BezierCurve(random.nextInt(maxOrder - minOrder) + minOrder, random.nextLong());
                bezierCurve.transformTo(location, nextLoc);
                List<Vector2> points = new ArrayList<>();
                for (float j = 0; j <= 1; j += 1f / size) {
                    points.add(bezierCurve.getPoint(j));
                }
                fillCoordinates(points.stream().filter(this::inBounds).collect(Collectors.toList()), true);
            }
        });
        return this;
    }

    public BooleanMask connect(Vector2 start, Vector2 end, float maxStepSize, int numMiddlePoints, float midPointMaxDistance, float midPointMinDistance, float maxAngleError, SymmetryType symmetryType) {
        enqueue(() -> {
            path(start, end, maxStepSize, numMiddlePoints, midPointMaxDistance, midPointMinDistance, maxAngleError, symmetryType);
            if (symmetrySettings.getSymmetry(symmetryType).getNumSymPoints() > 1) {
                List<Vector2> symmetryPoints = getSymmetryPointsWithOutOfBounds(end, symmetryType);
                path(start, symmetryPoints.get(0), maxStepSize, numMiddlePoints, midPointMaxDistance, midPointMinDistance, maxAngleError, symmetryType);
            }
        });
        return this;
    }

    public BooleanMask progressiveWalk(int numWalkers, int numSteps) {
        enqueue(() -> {
            for (int i = 0; i < numWalkers; i++) {
                int x = random.nextInt(getMaxXBound(SymmetryType.TERRAIN) - getMinXBound(SymmetryType.TERRAIN)) + getMinXBound(SymmetryType.TERRAIN);
                int y = random.nextInt(getMaxYBound(x, SymmetryType.TERRAIN) - getMinYBound(x, SymmetryType.TERRAIN) + 1) + getMinYBound(x, SymmetryType.TERRAIN);
                List<Integer> directions = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
                int regressiveDir = random.nextInt(directions.size());
                directions.remove(regressiveDir);
                for (int j = 0; j < numSteps; j++) {
                    if (inBounds(x, y)) {
                        applyAtSymmetryPoints(x, y, SymmetryType.TERRAIN, spoint -> set(spoint, true));
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
        });
        return this;
    }

    public BooleanMask space(float minSpacing, float maxSpacing) {
        enqueue(() -> {
            List<Vector2> coordinates = getRandomCoordinates(minSpacing, maxSpacing);
            clear();
            fillCoordinates(coordinates, true);
        });
        return this;
    }

    public BooleanMask invert() {
        return enqueue(() -> {
            for (int i = 0; i < mask.length; i++) {
                mask[i] = ~mask[i];
            }
        });
    }

    public BooleanMask inflate(float radius) {
        enqueue(() -> {
            long[] maskCopy = getMaskCopy();
            apply(point -> {
                if (get(point) && isEdge(point)) {
                    markInRadius(radius, maskCopy, point, true);
                }
            });
            mask = maskCopy;
        });
        return this;
    }

    public BooleanMask deflate(float radius) {
        enqueue(() -> {
            long[] maskCopy = getMaskCopy();
            apply(point -> {
                if (!get(point) && isEdge(point)) {
                    markInRadius(radius, maskCopy, point, false);
                }
            });
            mask = maskCopy;
        });
        return this;
    }

    private void markInRadius(float radius, long[] maskCopy, Point point, boolean value) {
        int x = point.x;
        int y = point.y;
        float radius2 = (radius + 0.5f) * (radius + 0.5f);
        int searchRange = (int) StrictMath.ceil(radius);
        int minX = x - searchRange;
        int maxX = x + searchRange + 1;
        int minY = y - searchRange;
        int maxY = y + searchRange + 1;
        for (int x2 = minX; x2 < maxX; ++x2) {
            for (int y2 = minY; y2 < maxY; ++y2) {
                int bitIndex = bitIndex(x2, y2, getSize());
                if (inBounds(x2, y2) && getBit(bitIndex, maskCopy) != value && (x - x2) * (x - x2) + (y - y2) * (y - y2) <= radius2) {
                    setBit(bitIndex, value, maskCopy);
                }
            }
        }
    }

    public BooleanMask cutCorners() {
        enqueue(() -> {
            int size = getSize();
            long[] maskCopy = getMaskCopy();
            apply(point -> {
                int x = point.x;
                int y = point.y;
                int count = 0;
                if (x > 0 && !get(x - 1, y)) {
                    count++;
                }
                if (y > 0 && !get(x, y - 1)) {
                    count++;
                }
                if (x < size - 1 && !get(x + 1, y)) {
                    count++;
                }
                if (y < size - 1 && !get(x, y + 1)) {
                    count++;
                }

                if (count > 1) {
                    setBit(bitIndex(point, size), false, maskCopy);
                }
            });
            mask = maskCopy;
        });
        return this;
    }

    public BooleanMask acid(float strength, float size) {
        BooleanMask holes = new BooleanMask(this, getName() + "holes");
        holes.randomize(strength, SymmetryType.SPAWN).inflate(size);
        enqueue((dependencies) -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            subtract(source);
        }, holes);
        return this;
    }

    public BooleanMask dilute(float strength) {
        return dilute(strength, 1);
    }

    public BooleanMask dilute(float strength, int count) {
        SymmetryType symmetryType = SymmetryType.SPAWN;
        enqueue(() -> {
            int size = getSize();
            for (int i = 0; i < count; i++) {
                long[] maskCopy = getMaskCopy();
                applyWithSymmetry(symmetryType, point -> {
                    if (!get(point) && random.nextFloat() < strength && isEdge(point)) {
                        applyAtSymmetryPoints(point, symmetryType, spoint -> setBit(spoint, true, size, maskCopy));
                    }
                });
                mask = maskCopy;
            }
        });
        return this;
    }

    public BooleanMask erode(float strength) {
        return erode(strength, 1);
    }

    public BooleanMask erode(float strength, int count) {
        SymmetryType symmetryType = SymmetryType.SPAWN;
        enqueue(() -> {
            int size = getSize();
            for (int i = 0; i < count; i++) {
                long[] maskCopy = getMaskCopy();
                applyWithSymmetry(symmetryType, point -> {
                    if (get(point) && random.nextFloat() < strength && isEdge(point)) {
                        applyAtSymmetryPoints(point, symmetryType, spoint -> setBit(spoint, false, size, maskCopy));
                    }
                });
                mask = maskCopy;
            }
        });
        return this;
    }

    public BooleanMask outline() {
        enqueue(() -> {
            int size = getSize();
            long[] maskCopy = new long[minimumArraySize(size)];
            apply(point -> setBit(point, isEdge(point), size, maskCopy));
            mask = maskCopy;
        });
        return this;
    }

    public BooleanMask blur(int radius, float density) {
        enqueue(() -> {
            int[][] innerCount = getInnerCount();
            set(point -> calculateAreaAverage(radius, point, innerCount) >= density);
        });
        return this;
    }

    private <T extends ComparableMask<U, ?>, U extends Comparable<U>> BooleanMask addWithOffset(T other, U minValue, U maxValue, Vector2 location, boolean wrapEdges) {
        addWithOffset(other.convertToBooleanMask(minValue, maxValue), location, true, wrapEdges);
        return this;
    }

    public BooleanMask addBrush(Vector2 location, String brushName, float minValue, float maxValue, int size) {
        enqueue(() -> {
            FloatMask brush = loadBrush(brushName, null).setSize(size);
            addWithOffset(brush, minValue, maxValue, location, false);
        });
        return this;
    }

    public <T extends Comparable<T>, U extends ComparableMask<T, U>> BooleanMask initMaxima(ComparableMask<T, U> other, T minValue, T maxValue) {
        assertCompatibleMask(other);
        enqueue(dependencies -> {
            ComparableMask<T, U> source = (ComparableMask<T, U>) dependencies.get(0);
            setWithSymmetry(SymmetryType.SPAWN, point -> {
                T value = source.get(point);
                return value.compareTo(minValue) >= 0 && value.compareTo(maxValue) < 0 && source.isLocalMax(point);
            });
        }, other);
        return this;
    }

    public <T extends Comparable<T>, U extends ComparableMask<T, U>> BooleanMask init1DMaxima(ComparableMask<T, U> other, T minValue, T maxValue) {
        assertCompatibleMask(other);
        enqueue(dependencies -> {
            ComparableMask<T, U> source = (ComparableMask<T, U>) dependencies.get(0);
            setWithSymmetry(SymmetryType.SPAWN, point -> {
                T value = source.get(point);
                return value.compareTo(minValue) >= 0 && value.compareTo(maxValue) < 0 && source.isLocal1DMax(point);
            });
        }, other);
        return this;
    }

    public BooleanMask limitToSymmetryRegion() {
        return limitToSymmetryRegion(SymmetryType.TEAM);
    }

    public BooleanMask limitToSymmetryRegion(SymmetryType symmetryType) {
        enqueue(() -> {
            int minXBound = getMinXBound(symmetryType);
            int maxXBound = getMaxXBound(symmetryType);
            set(point -> {
                int x = point.x;
                int y = point.y;
                return get(x, y) && !(x < minXBound || x >= maxXBound || y < getMinYBound(x, symmetryType) || y >= getMaxYBound(x, symmetryType));
            });
        });
        return this;
    }

    public BooleanMask limitToCenteredCircle(float circleRadius) {
        enqueue(() -> {
            int size = getSize();
            BooleanMask symmetryLimit = new BooleanMask(size, null, symmetrySettings, getName() + "symmetryLimit");
            symmetryLimit.fillCircle(size / 2f, size / 2f, circleRadius, true);
            multiply(symmetryLimit);
        });
        return this;
    }

    public BooleanMask fillShape(Vector2 location) {
        return enqueue(() -> fillCoordinates(getShapeCoordinates(location), !get(location)));
    }

    public BooleanMask fillGaps(int minDist) {
        enqueue(() -> {
            BooleanMask filledGaps = getDistanceField().getLocalMaximums(1f, minDist / 2f);
            filledGaps.inflate(minDist / 2f);
            add(filledGaps);
        });
        return this;
    }

    public BooleanMask widenGaps(int minDist) {
        enqueue(() -> {
            BooleanMask filledGaps = getDistanceField().getLocalMaximums(1f, minDist / 2f);
            filledGaps.inflate(minDist / 2f);
            subtract(filledGaps);
        });
        return this;
    }

    public BooleanMask removeAreasSmallerThan(int minArea) {
        enqueue(() -> {
            int size = getSize();
            Set<Vector2> seen = new HashSet<>(size * size, 1f);
            applyWithSymmetry(SymmetryType.SPAWN, point -> {
                Vector2 location = new Vector2(point);
                if (!seen.contains(location)) {
                    boolean value = get(location);
                    Set<Vector2> coordinates = getShapeCoordinates(location, minArea);
                    seen.addAll(coordinates);
                    if (coordinates.size() < minArea) {
                        fillCoordinates(coordinates, !value);
                    }
                }
            });
        });
        return this;
    }

    public BooleanMask removeAreasBiggerThan(int maxArea) {
        return enqueue(() -> subtract(copy().removeAreasSmallerThan(maxArea)));
    }

    public BooleanMask removeAreasOutsideSizeRange(int minSize, int maxSize) {
        return enqueue(() -> removeAreasSmallerThan(minSize).removeAreasBiggerThan(maxSize));
    }

    public BooleanMask removeAreasInSizeRange(int minSize, int maxSize) {
        return enqueue(() -> subtract(copy().removeAreasOutsideSizeRange(minSize, maxSize)));
    }

    public LinkedHashSet<Vector2> getShapeCoordinates(Vector2 location) {
        int size = getSize();
        return getShapeCoordinates(location, size * size);
    }

    public LinkedHashSet<Vector2> getShapeCoordinates(Vector2 location, int maxSize) {
        assertNotPipelined();
        LinkedHashSet<Vector2> areaHash = new LinkedHashSet<>();
        LinkedHashSet<Vector2> edgeHash = new LinkedHashSet<>();
        List<Vector2> queue = new ArrayList<>();
        LinkedHashSet<Vector2> queueHash = new LinkedHashSet<>();
        List<int[]> edges = Arrays.asList(new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}, new int[]{1, 0});
        boolean value = get(location);
        queue.add(location);
        queueHash.add(location);
        while (queue.size() > 0) {
            Vector2 next = queue.remove(0);
            queueHash.remove(next);
            if (get(next) == value && !areaHash.contains(next)) {
                areaHash.add(next);
                edges.forEach((e) -> {
                    Vector2 newLocation = new Vector2(next.getX() + e[0], next.getY() + e[1]);
                    if (!queueHash.contains(newLocation) && !areaHash.contains(newLocation) && !edgeHash.contains(newLocation) && inBounds(newLocation)) {
                        queue.add(newLocation);
                        queueHash.add(newLocation);
                    }
                });
            } else if (get(next) != value) {
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
        int size = getSize();
        int[][] innerCount = new int[size][size];
        apply(point -> calculateInnerValue(innerCount, point, get(point) ? 1 : 0));
        return innerCount;
    }

    @Override
    protected Boolean transformAverage(float value) {
        return value > .5f;
    }

    public FloatMask getDistanceField() {
        int size = getSize();
        FloatMask distanceField = new FloatMask(this, (float) (size * size), 0f, getName() + "DistanceField");
        distanceField.parabolicMinimization();
        return distanceField;
    }

    public int getCount() {
        assertNotPipelined();
        int count = 0;
        int size = getSize();
        for (long l : mask) {
            count += Long.bitCount(l);
        }
        return count;
    }

    public List<Vector2> getAllCoordinates(int spacing) {
        int size = getSize();
        List<Vector2> coordinates = new ArrayList<>(size * size);
        for (int x = 0; x < size; x += spacing) {
            for (int y = 0; y < size; y += spacing) {
                Vector2 location = new Vector2(x, y);
                coordinates.add(location);
            }
        }
        return coordinates;
    }

    public List<Vector2> getAllCoordinatesEqualTo(boolean value, int spacing) {
        assertNotPipelined();
        int size = getSize();
        int numPossibleCoordinates;
        int numTrue = getCount();
        if (value) {
            numPossibleCoordinates = numTrue;
        } else {
            numPossibleCoordinates = size * size - numTrue;
        }
        List<Vector2> coordinates = new ArrayList<>(numPossibleCoordinates / spacing);
        for (int x = 0; x < size; x += spacing) {
            for (int y = 0; y < size; y += spacing) {
                if (get(x, y) == value) {
                    coordinates.add(new Vector2(x, y));
                }
            }
        }
        return coordinates;
    }

    public List<Vector2> getSpacedCoordinates(float radius, int spacing) {
        List<Vector2> coordinateList = getAllCoordinates(spacing);
        return spaceCoordinates(radius, coordinateList);
    }

    public List<Vector2> getSpacedCoordinatesEqualTo(boolean value, float radius, int spacing) {
        List<Vector2> coordinateList = getAllCoordinatesEqualTo(value, spacing);
        return spaceCoordinates(radius, coordinateList);
    }

    private List<Vector2> spaceCoordinates(float radius, List<Vector2> coordinateList) {
        List<Vector2> chosenCoordinates = new ArrayList<>();
        while (coordinateList.size() > 0) {
            Vector2 location = coordinateList.remove(0);
            chosenCoordinates.add(location);
            coordinateList.removeIf((loc) -> location.getDistance(loc) < radius);
        }
        return chosenCoordinates;
    }

    public List<Vector2> getRandomCoordinates(float spacing) {
        return getRandomCoordinates(spacing, SymmetryType.TEAM);
    }

    public List<Vector2> getRandomCoordinates(float spacing, SymmetryType symmetryType) {
        return getRandomCoordinates(spacing, spacing, symmetryType);
    }

    public List<Vector2> getRandomCoordinates(float minSpacing, float maxSpacing) {
        return getRandomCoordinates(minSpacing, maxSpacing, SymmetryType.TEAM);
    }

    public List<Vector2> getRandomCoordinates(float minSpacing, float maxSpacing, SymmetryType symmetryType) {
        assertNotPipelined();
        List<Vector2> coordinateList;
        if (symmetryType != null) {
            coordinateList = copy().limitToSymmetryRegion().getAllCoordinatesEqualTo(true, 1);
        } else {
            coordinateList = getAllCoordinatesEqualTo(true, 1);
        }
        LinkedList<Vector2> chosenCoordinates = new LinkedList<>();
        while (coordinateList.size() > 0) {
            Vector2 location = coordinateList.remove(random.nextInt(coordinateList.size()));
            float spacing = random.nextFloat() * (maxSpacing - minSpacing) + minSpacing;
            chosenCoordinates.addLast(location);
            coordinateList.removeIf((loc) -> location.getDistance(loc) < spacing);
            if (symmetryType != null) {
                List<Vector2> symmetryPoints = getSymmetryPoints(location, symmetryType);
                symmetryPoints.forEach(symPoint -> coordinateList.removeIf((loc) -> symPoint.getDistance(loc) < spacing));
            }
        }
        return chosenCoordinates;
    }

    public Vector2 getRandomPosition() {
        assertNotPipelined();
        List<Vector2> coordinates = new ArrayList<>(getAllCoordinatesEqualTo(true, 1));
        if (coordinates.size() == 0)
            return null;
        int cell = random.nextInt(coordinates.size());
        return coordinates.get(cell);
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        writeToImage(image);
        return image;
    }

    @Override
    public BufferedImage writeToImage(BufferedImage image) {
        assertSize(image.getHeight());
        int size = getSize();
        DataBuffer imageBuffer = image.getRaster().getDataBuffer();
        loop(point -> imageBuffer.setElem(point.x + point.y * size, get(point) ? 255 : 0));
        return image;
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        int size = getSize();
        ByteBuffer bytes = ByteBuffer.allocate(size * size);
        applyWithSymmetry(SymmetryType.SPAWN, point -> bytes.put(get(point) ? (byte) 1 : 0));
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }
}
