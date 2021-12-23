package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.MathUtils;
import com.faforever.neroxis.util.vector.Vector;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.faforever.neroxis.brushes.Brushes.loadBrush;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public strictfp class FloatMask extends PrimitiveMask<Float, FloatMask> {
    private float[][] mask;

    public FloatMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public FloatMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public FloatMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public FloatMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings) {
        this(sourceImage, seed, symmetrySettings, 1f, null, false);
    }

    public FloatMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, null, false);
    }

    public FloatMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, name, false);
    }

    public FloatMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name, boolean parallel) {
        this(sourceImage.getHeight(), seed, symmetrySettings, name, parallel);
        DataBuffer imageBuffer = sourceImage.getRaster().getDataBuffer();
        int size = getSize();
        enqueue(() -> apply(point -> setPrimitive(point, imageBuffer.getElemFloat(point.x + point.y * size) * scaleFactor)));
    }

    public FloatMask(FloatMask other) {
        this(other, null);
    }

    public FloatMask(FloatMask other, String name) {
        super(other, name);
    }

    @Override
    protected void initializeMask(int size) {
        mask = new float[size][size];
    }

    public FloatMask(BooleanMask other, float low, float high) {
        this(other, low, high, null);
    }

    public FloatMask(BooleanMask other, float low, float high, String name) {
        this(other.getSize(), other.getNextSeed(), other.getSymmetrySettings(), name, other.isParallel());
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            apply(point -> setPrimitive(point, source.getPrimitive(point) ? high : low));
        }, other);
    }

    public <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other1, VectorMask<T, U> other2) {
        this(other1, other2, null);
    }

    public <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other1, VectorMask<T, U> other2, String name) {
        this(other1.getSize(), other1.getNextSeed(), other1.getSymmetrySettings(), name, other1.isParallel());
        assertCompatibleMask(other1);
        assertCompatibleMask(other2);
        enqueue((dependencies) -> {
            U source1 = (U) dependencies.get(0);
            U source2 = (U) dependencies.get(1);
            apply(point -> setPrimitive(point, source1.get(point).dot(source2.get(point))));
        }, other1, other2);
    }

    public <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other, T vector) {
        this(other, vector, null);
    }

    public <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other, T vector, String name) {
        this(other.getSize(), other.getNextSeed(), other.getSymmetrySettings(), name, other.isParallel());
        assertCompatibleMask(other);
        enqueue((dependencies) -> {
            U source = (U) dependencies.get(0);
            apply(point -> setPrimitive(point, source.get(point).dot(vector)));
        }, other);
    }

    public <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other, int index) {
        this(other, index, null);
    }

    public <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other, int index, String name) {
        this(other.getSize(), other.getNextSeed(), other.getSymmetrySettings(), name, other.isParallel());
        assertCompatibleMask(other);
        enqueue((dependencies) -> {
            U source = (U) dependencies.get(0);
            apply(point -> setPrimitive(point, source.get(point).get(index)));
        }, other);
    }

    @Override
    public Float getAvg() {
        int size = getSize();
        return getSum() / size / size;
    }

    @Override
    public Float getZeroValue() {
        return 0f;
    }

    @Override
    protected FloatMask fill(Float value) {
        int maskSize = mask.length;
        mask[0][0] = value;
        for (int i = 1; i < maskSize; i += i) {
            System.arraycopy(mask[0], 0, mask[0], i, StrictMath.min((maskSize - i), i));
        }
        for (int r = 1; r < maskSize; ++r) {
            System.arraycopy(mask[0], 0, mask[r], 0, maskSize);
        }
        return this;
    }

    protected FloatMask fill(float[][] maskToFillFrom) {
        assertNotPipelined();
        int maskSize = maskToFillFrom.length;
        mask = new float[maskSize][maskSize];
        for (int r = 0; r < maskSize; ++r) {
            System.arraycopy(maskToFillFrom[r], 0, mask[r], 0, maskSize);
        }
        return this;
    }

    @Override
    public Float get(int x, int y) {
        return getPrimitive(x, y);
    }

    @Override
    protected void set(int x, int y, Float value) {
        setPrimitive(x, y, value);
    }

    public float getPrimitive(Vector2 location) {
        return getPrimitive(StrictMath.round(location.getX()), StrictMath.round(location.getY()));
    }

    protected void setPrimitive(Vector2 location, float value) {
        setPrimitive(StrictMath.round(location.getX()), StrictMath.round(location.getY()), value);
    }

    public float getPrimitive(Point point) {
        return getPrimitive(point.x, point.y);
    }

    protected void setPrimitive(Point point, float value) {
        setPrimitive(point.x, point.y, value);
    }

    public float getPrimitive(int x, int y) {
        return mask[x][y];
    }

    protected void setPrimitive(int x, int y, float value) {
        mask[x][y] = value;
    }

    @Override
    public int getImmediateSize() {
        return mask.length;
    }

    @Override
    protected FloatMask setSizeInternal(int newSize) {
        return enqueue(() -> {
            int oldSize = getSize();
            if (oldSize == 1) {
                float value = getPrimitive(0, 0);
                initializeMask(newSize);
                fill(value);
            } else if (oldSize != newSize) {
                float[][] oldMask = mask;
                initializeMask(newSize);
                Map<Integer, Integer> coordinateMap = getSymmetricScalingCoordinateMap(oldSize, newSize);
                apply(point -> setPrimitive(point, oldMask[coordinateMap.get(point.x)][coordinateMap.get(point.y)]));
            }
        });
    }

    @Override
    protected FloatMask copyFrom(FloatMask other) {
        return enqueue((dependencies) -> fill(((FloatMask) dependencies.get(0)).mask), other);
    }

    @Override
    protected void addValueAt(int x, int y, Float value) {
        mask[x][y] += value;
    }

    @Override
    protected void subtractValueAt(int x, int y, Float value) {
        mask[x][y] -= value;
    }

    @Override
    protected void multiplyValueAt(int x, int y, Float value) {
        mask[x][y] *= value;
    }

    @Override
    protected void divideValueAt(int x, int y, Float value) {
        mask[x][y] /= value;
    }

    @Override
    public FloatMask add(FloatMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            apply(point -> mask[point.x][point.y] += source.mask[point.x][point.y]);
        }, other);
    }

    @Override
    public FloatMask subtract(FloatMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            apply(point -> mask[point.x][point.y] -= source.mask[point.x][point.y]);
        }, other);
    }

    @Override
    public FloatMask multiply(FloatMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            apply(point -> mask[point.x][point.y] *= source.mask[point.x][point.y]);
        }, other);
    }

    @Override
    public FloatMask divide(FloatMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            apply(point -> mask[point.x][point.y] /= source.mask[point.x][point.y]);
        }, other);
    }

    @Override
    public FloatMask blur(int radius) {
        return enqueue(() -> {
            int[][] innerCount = getInnerCount();
            apply(point -> setPrimitive(point, transformAverage(calculateAreaAverageAsInts(radius, point, innerCount))));
        });
    }

    @Override
    public FloatMask blur(int radius, BooleanMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask limiter = (BooleanMask) dependencies.get(0);
            int[][] innerCount = getInnerCount();
            apply(point -> {
                if (limiter.get(point)) {
                    setPrimitive(point, transformAverage(calculateAreaAverageAsInts(radius, point, innerCount)));
                }
            });
        }, other);
    }

    protected Vector3 getNormalAt(Point point, float scale) {
        return getNormalAt(point.x, point.y, scale);
    }

    protected Vector3 getNormalAt(int x, int y, float scale) {
        if (!inBounds(x, y)) {
            throw new IllegalArgumentException(String.format("Arguments not in bound x: %d y: %d", x, y));
        }
        float xNormal, yNormal;
        if (x == 0) {
            xNormal = (getPrimitive(x, y) - getPrimitive(x + 1, y)) * scale;
        } else if (x == (getSize() - 1)) {
            xNormal = (getPrimitive(x - 1, y) - getPrimitive(x, y)) * scale;
        } else {
            xNormal = (getPrimitive(x - 1, y) - getPrimitive(x + 1, y)) * scale / 2f;
        }
        if (y == 0) {
            yNormal = (getPrimitive(x, y) - getPrimitive(x, y + 1)) * scale;
        } else if (y == (getSize() - 1)) {
            yNormal = (getPrimitive(x, y - 1) - getPrimitive(x, y)) * scale;
        } else {
            yNormal = (getPrimitive(x, y - 1) - getPrimitive(x, y + 1)) * scale / 2f;
        }
        return new Vector3(xNormal, 1, yNormal).normalize();
    }

    @Override
    public Float getSum() {
        return (float) Arrays.stream(mask).flatMapToDouble(row -> IntStream.range(0, row.length).mapToDouble(i -> row[i])).sum();
    }

    @Override
    public Float getMin() {
        return (float) Arrays.stream(mask).flatMapToDouble(row -> IntStream.range(0, row.length).mapToDouble(i -> row[i])).min().orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    @Override
    public Float getMax() {
        return (float) Arrays.stream(mask).flatMapToDouble(row -> IntStream.range(0, row.length).mapToDouble(i -> row[i])).max().orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    public FloatMask addGaussianNoise(float scale) {
        return enqueue(() -> addWithSymmetry(SymmetryType.SPAWN, point -> (float) random.nextGaussian() * scale));
    }

    public FloatMask addWhiteNoise(float scale) {
        return enqueue(() -> addWithSymmetry(SymmetryType.SPAWN, point -> random.nextFloat() * scale));
    }

    public FloatMask addWhiteNoise(float minValue, float maxValue) {
        float range = maxValue - minValue;
        return enqueue(() -> addWithSymmetry(SymmetryType.SPAWN, point -> random.nextFloat() * range + minValue));
    }

    public FloatMask addPerlinNoise(int resolution, float scale) {
        int size = getSize();
        int gradientSize = size / resolution;
        float gradientScale = (float) size / gradientSize;
        Vector2Mask gradientVectors = new Vector2Mask(gradientSize + 1,
                random.nextLong(), new SymmetrySettings(Symmetry.NONE), getName() + "PerlinVectors", isParallel());
        gradientVectors.randomize(-1f, 1f).normalize();
        FloatMask noise = new FloatMask(size,
                null, symmetrySettings, getName() + "PerlinNoise", isParallel());
        noise.enqueue((dependencies) -> {
            Vector2Mask source = (Vector2Mask) dependencies.get(0);
            noise.apply(point -> {
                int x = point.x;
                int y = point.y;
                int xLow = (int) (x / gradientScale);
                float dXLow = x / gradientScale - xLow;
                int xHigh = xLow + 1;
                float dXHigh = x / gradientScale - xHigh;
                int yLow = (int) (y / gradientScale);
                float dYLow = y / gradientScale - yLow;
                int yHigh = yLow + 1;
                float dYHigh = y / gradientScale - yHigh;
                float topLeft = new Vector2(dXLow, dYLow).dot(source.get(xLow, yLow));
                float topRight = new Vector2(dXLow, dYHigh).dot(source.get(xLow, yHigh));
                float bottomLeft = new Vector2(dXHigh, dYLow).dot(source.get(xHigh, yLow));
                float bottomRight = new Vector2(dXHigh, dYHigh).dot(source.get(xHigh, yHigh));
                noise.setPrimitive(point, MathUtils.smootherStep(MathUtils.smootherStep(topLeft, bottomLeft, dXLow),
                        MathUtils.smootherStep(topRight, bottomRight, dXLow), dYLow));
            });
            float noiseMin = noise.getMin();
            float noiseMax = noise.getMax();
            float noiseRange = noiseMax - noiseMin;
            noise.apply(point -> noise.setPrimitive(point, (noise.getPrimitive(point) - noiseMin) / noiseRange * scale));
        }, gradientVectors);
        enqueue((dependencies) -> add((FloatMask) dependencies.get(0)), noise);
        return this;
    }

    public FloatMask addDistance(BooleanMask other, float scale) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            FloatMask distanceField = source.getDistanceField();
            add(distanceField.multiply(scale));
        }, other);
    }

    public FloatMask sqrt() {
        return enqueue(() -> apply(point -> setPrimitive(point, (float) StrictMath.sqrt(getPrimitive(point)))));
    }

    public FloatMask gradient() {
        return enqueue(() -> {
            int size = getSize();
            float[][] newMask = new float[size][size];
            apply(point -> {
                int x = point.x;
                int y = point.y;
                int xNeg = StrictMath.max(0, x - 1);
                int xPos = StrictMath.min(size - 1, x + 1);
                int yNeg = StrictMath.max(0, y - 1);
                int yPos = StrictMath.min(size - 1, y + 1);
                float xSlope = (getPrimitive(xPos, y) - getPrimitive(xNeg, y)) / (xPos - xNeg);
                float ySlope = (getPrimitive(x, yPos) - getPrimitive(x, yNeg)) / (yPos - yNeg);
                newMask[x][y] = (float) StrictMath.sqrt(xSlope * xSlope + ySlope * ySlope);
            });
            mask = newMask;
        });
    }

    public FloatMask supcomGradient() {
        return enqueue(() -> {
            int size = getSize();
            float[][] newMask = new float[size][size];
            apply(point -> {
                int x = point.x;
                int y = point.y;
                int xPos = StrictMath.min(size - 1, x + 1);
                int yPos = StrictMath.min(size - 1, y + 1);
                int xNeg = StrictMath.max(0, x - 1);
                int yNeg = StrictMath.max(0, y - 1);
                float xPosSlope = StrictMath.abs(getPrimitive(x, y) - getPrimitive(xPos, y));
                float yPosSlope = StrictMath.abs(getPrimitive(x, y) - getPrimitive(x, yPos));
                float xNegSlope = StrictMath.abs(getPrimitive(x, y) - getPrimitive(xNeg, y));
                float yNegSlope = StrictMath.abs(getPrimitive(x, y) - getPrimitive(x, yNeg));
                newMask[x][y] = Collections.max(Arrays.asList(xPosSlope, yPosSlope, xNegSlope, yNegSlope));
            });
            mask = newMask;
        });
    }

    public FloatMask waterErode(int numDrops, int maxIterations, float friction, float speed, float erosionRate,
                                float depositionRate, float maxOffset, float iterationScale) {
        return enqueue(() -> {
            int size = getSize();
            for (int i = 0; i < numDrops; ++i) {
                waterDrop(maxIterations, random.nextInt(size), random.nextInt(size), friction, speed, erosionRate, depositionRate, maxOffset, iterationScale);
            }
            applySymmetry(SymmetryType.SPAWN);
        });
    }

    private void waterDrop(int maxIterations, float x, float y, float friction, float gravity, float erosionRate,
                           float depositionRate, float maxOffset, float iterationScale) {
        float xOffset = (random.nextFloat() * 2 - 1) * maxOffset;
        float yOffset = (random.nextFloat() * 2 - 1) * maxOffset;
        float sediment = 0;
        float xPrev = x;
        float yPrev = y;
        float xVelocity = 0;
        float yVelocity = 0;

        for (int i = 0; i < maxIterations; ++i) {
            int sampleX = (int) (x + xOffset);
            int sampleY = (int) (y + yOffset);
            if (!inBounds(sampleX, sampleY) || !inBounds((int) xPrev, (int) yPrev)) {
                return;
            }
            Vector3 surfaceNormal = getNormalAt(sampleX, sampleY, 1f);

            // If the terrain is flat, stop simulating, the snowball cannot roll any further
            if (surfaceNormal.getY() >= 1 && StrictMath.sqrt(xVelocity * xVelocity + yVelocity * yVelocity) < 1) {
                break;
            }

            // Calculate the deposition and erosion rate
            float deposit = sediment * depositionRate * surfaceNormal.getY();
            float erosion = erosionRate * (1 - surfaceNormal.getY()) * StrictMath.min(1, i * iterationScale);

            float sedimentChange = deposit - erosion;

            // Change the sediment on the place this snowball came from
            addValueAt((int) xPrev, (int) yPrev, sedimentChange);
            sediment -= sedimentChange;

            xVelocity = (1 - friction) * xVelocity + surfaceNormal.getX() * gravity;
            yVelocity = (1 - friction) * yVelocity + surfaceNormal.getZ() * gravity;
            xPrev = x;
            yPrev = y;
            x += xVelocity;
            y += yVelocity;
        }
    }

    private float transformAverage(float value) {
        return value / 1000f;
    }

    public FloatMask removeAreasOutsideIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        return enqueue(() -> {
            FloatMask tempMask2 = copy().init(this.copy().convertToBooleanMask(minIntensity, maxIntensity).removeAreasOutsideSizeRange(minSize, maxSize).invert(), 0f, 1f);
            subtract(tempMask2).clampMin(0f);
        });
    }

    public FloatMask removeAreasInIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        return enqueue(() -> subtract(copy().removeAreasOutsideIntensityAndSize(minSize, maxSize, minIntensity, maxIntensity)));
    }

    public FloatMask removeAreasOfSpecifiedSizeWithLocalMaximums(int minSize, int maxSize, int levelOfPrecision, float floatMax) {
        return enqueue(() -> {
            for (int x = 0; x < levelOfPrecision; x++) {
                removeAreasInIntensityAndSize(minSize, maxSize, ((1f - (float) x / (float) levelOfPrecision) * floatMax), floatMax);
            }
            removeAreasInIntensityAndSize(minSize, maxSize, 0.0000001f, floatMax);
        });
    }

    @Override
    protected int[][] getInnerCount() {
        int size = getSize();
        int[][] innerCount = new int[size][size];
        apply(point -> calculateInnerValue(innerCount, point, StrictMath.round(getPrimitive(point) * 1000)));
        return innerCount;
    }

    public FloatMask getDistanceFieldForRange(float minValue, float maxValue) {
        return convertToBooleanMask(minValue, maxValue).getDistanceField();
    }

    public FloatMask useBrush(Vector2 location, String brushName, float intensity, int size, boolean wrapEdges) {
        return enqueue(() -> {
            FloatMask brush = loadBrush(brushName, null);
            brush.multiply(intensity / brush.getMax()).setSize(size);
            addWithOffset(brush, location, true, wrapEdges);
        });
    }

    public FloatMask useBrushWithinArea(BooleanMask other, String brushName, int size, int numUses, float intensity, boolean wrapEdges) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            assertSmallerSize(size);
            ArrayList<Vector2> possibleLocations = new ArrayList<>(source.getAllCoordinatesEqualTo(true, 1));
            int length = possibleLocations.size();
            FloatMask brush = loadBrush(brushName, null);
            brush.multiply(intensity / brush.getMax()).setSize(size);
            for (int i = 0; i < numUses; i++) {
                Vector2 location = possibleLocations.get(random.nextInt(length));
                addWithOffset(brush, location, true, wrapEdges);
            }
        }, other);
    }

    public FloatMask useBrushWithinAreaWithDensity(BooleanMask other, String brushName, int size, float density, float intensity, boolean wrapEdges) {
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            int frequency = (int) (density * (float) source.getCount() / 26.21f / symmetrySettings.getSpawnSymmetry().getNumSymPoints());
            useBrushWithinArea(source, brushName, size, frequency, intensity, wrapEdges);
        }, other);
        return this;
    }

    public NormalMask getNormalMask() {
        return getNormalMask(1f);
    }

    public NormalMask getNormalMask(float scale) {
        NormalMask normalMask = new NormalMask(this, getNextSeed(), scale, getName() + "Normals");
        normalMask.symmetrySettings = new SymmetrySettings(Symmetry.NONE);
        return normalMask;
    }

    public BooleanMask getShadowMask(Vector3 lightDirection) {
        float angle = (float) ((lightDirection.getAzimuth() - StrictMath.PI) % (StrictMath.PI * 2));
        float slope = (float) StrictMath.tan(lightDirection.getElevation());
        BooleanMask shadowMask = new BooleanMask(getSize(), getNextSeed(), new SymmetrySettings(Symmetry.NONE), getName() + "Shadow", isParallel());
        shadowMask.enqueue(dependencies -> shadowMask.apply(point -> {
            Vector2 location = new Vector2(point);
            if (shadowMask.getPrimitive(location)) {
                return;
            }
            float startHeight = getPrimitive(location);
            int dist = 1;
            location.addPolar(angle, 1);
            while (inBounds(location)) {
                if (startHeight - getPrimitive(location) > dist * slope) {
                    shadowMask.setPrimitive(location, true);
                } else {
                    break;
                }
                location.addPolar(angle, 1);
                ++dist;
            }
        }), this).inflate(1).deflate(1);
        return shadowMask;
    }

    public FloatMask parabolicMinimization() {
        return enqueue(() -> {
            addCalculatedParabolicDistance(false);
            addCalculatedParabolicDistance(true);
            sqrt();
        });
    }

    public void addCalculatedParabolicDistance(boolean useColumns) {
        assertNotPipelined();
        int size = getSize();
        for (int i = 0; i < size; i++) {
            List<Vector2> vertices = new ArrayList<>();
            List<Vector2> intersections = new ArrayList<>();
            int index = 0;
            float value;
            if (!useColumns) {
                value = getPrimitive(i, 0);
            } else {
                value = getPrimitive(0, i);
            }
            vertices.add(new Vector2(0, value));
            intersections.add(new Vector2(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY));
            intersections.add(new Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
            for (int j = 1; j < size; j++) {
                if (!useColumns) {
                    value = getPrimitive(i, j);
                } else {
                    value = getPrimitive(j, i);
                }
                Vector2 current = new Vector2(j, value);
                Vector2 vertex = vertices.get(index);
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
                    intersections.set(index, new Vector2(xIntersect, Float.POSITIVE_INFINITY));
                    intersections.set(index + 1, new Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
                } else {
                    intersections.set(index, new Vector2(xIntersect, Float.POSITIVE_INFINITY));
                    intersections.add(new Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
                }
            }
            index = 0;
            for (int j = 0; j < size; j++) {
                while (intersections.get(index + 1).getX() < j) {
                    index += 1;
                }
                Vector2 vertex = vertices.get(index);
                float dx = j - vertex.getX();
                float height = dx * dx + vertex.getY();
                if (!useColumns) {
                    setPrimitive(i, j, height);
                } else {
                    setPrimitive(j, i, height);
                }
            }
        }
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        float min = getMin();
        float max = getMax();
        float range = max - min;
        writeToImage(image, 255 / range, min);
        return image;
    }

    @Override
    public BufferedImage writeToImage(BufferedImage image) {
        return writeToImage(image, 1f, 0f);
    }

    public BufferedImage writeToImage(BufferedImage image, float scaleFactor) {
        return writeToImage(image, scaleFactor, 0f);
    }

    public BufferedImage writeToImage(BufferedImage image, float scaleFactor, float offsetFactor) {
        assertSize(image.getHeight());
        int size = getSize();
        DataBuffer imageBuffer = image.getRaster().getDataBuffer();
        loop(point -> imageBuffer.setElemFloat(point.x + point.y * size, (getPrimitive(point) - offsetFactor) * scaleFactor));
        return image;
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        int size = getSize();
        ByteBuffer bytes = ByteBuffer.allocate(size * size * 4);
        applyWithSymmetry(SymmetryType.SPAWN, point -> bytes.putFloat(getPrimitive(point)));
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }
}
