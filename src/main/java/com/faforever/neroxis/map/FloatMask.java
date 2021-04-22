package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Vector2f;
import com.faforever.neroxis.util.Vector3f;
import com.faforever.neroxis.util.VisualDebugger;
import lombok.Getter;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static com.faforever.neroxis.brushes.Brushes.loadBrush;

@Getter
public strictfp class FloatMask extends NumberMask<Float, FloatMask> {

    public FloatMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public FloatMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public FloatMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(seed, symmetrySettings, name, parallel);
        this.mask = getEmptyMask(size);
        this.plannedSize = size;
        execute(() -> VisualDebugger.visualizeMask(this));
    }

    public FloatMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(sourceImage, seed, symmetrySettings, name, false);
    }

    public FloatMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(seed, symmetrySettings, name, parallel);
        this.mask = getEmptyMask(sourceImage.getHeight());
        Raster imageData = sourceImage.getData();
        execute(() -> {
            modify((x, y) -> {
                int[] value = new int[1];
                imageData.getPixel(x, y, value);
                return value[0] / 255f;
            });
            VisualDebugger.visualizeMask(this);
        });
    }

    public FloatMask(FloatMask sourceMask, Long seed) {
        this(sourceMask, seed, null);
    }

    public FloatMask(FloatMask other, Long seed, String name) {
        super(seed, other.getSymmetrySettings(), name, other.isParallel());
        this.mask = getEmptyMask(other.getSize());
        this.plannedSize = other.getSize();
        setProcessing(other.isProcessing());
        execute(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            modify(source::getValueAt);
            VisualDebugger.visualizeMask(this);
        }, other);
    }

    public FloatMask(BooleanMask sourceMask, float low, float high, Long seed) {
        this(sourceMask, low, high, seed, null);
    }

    public FloatMask(BooleanMask other, float low, float high, Long seed, String name) {
        super(seed, other.getSymmetrySettings(), name, other.isParallel());
        this.mask = getEmptyMask(other.getSize());
        this.plannedSize = other.getSize();
        setProcessing(other.isProcessing());
        execute(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            modify((x, y) -> source.getValueAt(x, y) ? high : low);
            VisualDebugger.visualizeMask(this);
        }, other);
    }

    @Override
    protected Float[][] getEmptyMask(int size) {
        Float[][] empty = new Float[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                empty[x][y] = 0f;
            }
        }
        return empty;
    }

    @Override
    public Float getAvg() {
        int size = getSize();
        return getSum() / size / size;
    }

    @Override
    public FloatMask copy() {
        if (random != null) {
            return new FloatMask(this, random.nextLong(), getName() + "Copy");
        } else {
            return new FloatMask(this, null, getName() + "Copy");
        }
    }

    @Override
    public Float getDefaultValue() {
        return 0f;
    }

    @Override
    public Float add(Float val1, Float val2) {
        return val1 + val2;
    }

    @Override
    public Float subtract(Float val1, Float val2) {
        return val1 - val2;
    }

    @Override
    public Float multiply(Float val1, Float val2) {
        return val1 * val2;
    }

    @Override
    public Float divide(Float val1, Float val2) {
        return val1 / val2;
    }

    public Vector3f getNormalAt(int x, int y) {
        if (onBoundary(x, y) || !inBounds(x, y)) {
            return new Vector3f(0, 1, 0);
        }
        return new Vector3f(
                (getValueAt(x - 1, y) - getValueAt(x + 1, y)) / 2f,
                1,
                (getValueAt(x, y - 1) - getValueAt(x, y + 1)) / 2f
        ).normalize();
    }

    public FloatMask addGaussianNoise(float scale) {
        execute(() -> {
            addWithSymmetry(SymmetryType.SPAWN, (x, y) -> (float) random.nextGaussian() * scale);
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask addWhiteNoise(float scale) {
        execute(() -> {
            addWithSymmetry(SymmetryType.SPAWN, (x, y) -> random.nextFloat() * scale);
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask addDistance(BooleanMask other, float scale) {
        execute(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            assertCompatibleMask(source);
            FloatMask distanceField = source.getDistanceField();
            add(distanceField.multiply(scale));
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    public FloatMask sqrt() {
        execute(() -> {
            modify((x, y) -> (float) StrictMath.sqrt(getValueAt(x, y)));
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask gradient() {
        execute(() -> {
            int size = getSize();
            Float[][] maskCopy = getEmptyMask(size);
            apply((x, y) -> {
                int xNeg = StrictMath.max(0, x - 1);
                int xPos = StrictMath.min(size - 1, x + 1);
                int yNeg = StrictMath.max(0, y - 1);
                int yPos = StrictMath.min(size - 1, y + 1);
                float xSlope = getValueAt(xPos, y) - getValueAt(xNeg, y);
                float ySlope = getValueAt(x, yPos) - getValueAt(x, yNeg);
                maskCopy[x][y] = (float) StrictMath.sqrt(xSlope * xSlope + ySlope * ySlope);
            });
            mask = maskCopy;
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask supcomGradient() {
        execute(() -> {
            int size = getSize();
            Float[][] maskCopy = getEmptyMask(size);
            apply((x, y) -> {
                int xPos = StrictMath.min(size - 1, x + 1);
                int yPos = StrictMath.min(size - 1, y + 1);
                int xNeg = StrictMath.max(0, x - 1);
                int yNeg = StrictMath.max(0, y - 1);
                float xPosSlope = StrictMath.abs(getValueAt(x, y) - getValueAt(xPos, y));
                float yPosSlope = StrictMath.abs(getValueAt(x, y) - getValueAt(x, yPos));
                float xNegSlope = StrictMath.abs(getValueAt(x, y) - getValueAt(xNeg, y));
                float yNegSlope = StrictMath.abs(getValueAt(x, y) - getValueAt(x, yNeg));
                maskCopy[x][y] = Collections.max(Arrays.asList(xPosSlope, yPosSlope, xNegSlope, yNegSlope));
            });
            mask = maskCopy;
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask waterErode(int numDrops, int maxIterations, float friction, float speed, float erosionRate,
                                float depositionRate, float maxOffset, float iterationScale) {
        execute(() -> {
            int size = getSize();
            for (int i = 0; i < numDrops; ++i) {
                waterDrop(maxIterations, random.nextInt(size), random.nextInt(size), friction, speed, erosionRate, depositionRate, maxOffset, iterationScale);
            }
            applySymmetry(SymmetryType.SPAWN);
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public void waterDrop(int maxIterations, float x, float y, float friction, float speed, float erosionRate,
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
                break;
            }
            Vector3f surfaceNormal = getNormalAt(sampleX, sampleY);

            // If the terrain is flat, stop simulating, the snowball cannot roll any further
            if (surfaceNormal.getY() >= 1) {
                break;
            }

            // Calculate the deposition and erosion rate
            float deposit = sediment * depositionRate * surfaceNormal.getY();
            float erosion = erosionRate * (1 - surfaceNormal.getY()) * StrictMath.min(1, i * iterationScale);

            float sedimentChange = erosion - deposit;

            // Change the sediment on the place this snowball came from
            subtractValueAt((int) xPrev, (int) yPrev, sedimentChange);
            sediment += sedimentChange;

            xVelocity = (1 - friction) * xVelocity + surfaceNormal.getX() * speed;
            yVelocity = (1 - friction) * yVelocity + surfaceNormal.getZ() * speed;
            xPrev = x;
            yPrev = y;
            x += xVelocity;
            y += yVelocity;
        }
    }

    @Override
    public FloatMask blur(int radius) {
        execute(() -> {
            int[][] innerCount = getInnerCount();
            modify((x, y) -> calculateAreaAverage(radius, x, y, innerCount) / 1000);
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    @Override
    public FloatMask blur(int radius, BooleanMask other) {
        execute(dependencies -> {
            BooleanMask limiter = (BooleanMask) dependencies.get(0);
            assertCompatibleMask(limiter);
            int[][] innerCount = getInnerCount();
            modify((x, y) -> limiter.getValueAt(x, y) ? calculateAreaAverage(radius, x, y, innerCount) / 1000 : getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    public FloatMask removeAreasOutsideIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        execute(() -> {
            FloatMask tempMask2 = copy().init(this.copy().convertToBooleanMask(minIntensity, maxIntensity).removeAreasOutsideSizeRange(minSize, maxSize).invert(), 0f, 1f);
            this.subtract(tempMask2).clampMin(0f);
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask removeAreasInIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        execute(() -> {
            subtract(this.copy().removeAreasOutsideIntensityAndSize(minSize, maxSize, minIntensity, maxIntensity));
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask removeAreasOfSpecifiedSizeWithLocalMaximums(int minSize, int maxSize, int levelOfPrecision, float floatMax) {
        execute(() -> {
            for (int x = 0; x < levelOfPrecision; x++) {
                removeAreasInIntensityAndSize(minSize, maxSize, ((1f - (float) x / (float) levelOfPrecision) * floatMax), floatMax);
            }
            removeAreasInIntensityAndSize(minSize, maxSize, 0.0000001f, floatMax);
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    @Override
    protected int[][] getInnerCount() {
        int size = getSize();
        int[][] innerCount = new int[size][size];
        apply((x, y) -> calculateInnerValue(innerCount, x, y, StrictMath.round(getValueAt(x, y) * 1000)));
        return innerCount;
    }

    public FloatMask getDistanceFieldForRange(float minValue, float maxValue) {
        return convertToBooleanMask(minValue, maxValue).getDistanceField();
    }

    public FloatMask useBrush(Vector2f location, String brushName, float intensity, int size, boolean wrapEdges) {
        execute(() -> {
            FloatMask brush = loadBrush(brushName, random.nextLong());
            brush.multiply(intensity / brush.getMax()).setSize(size);
            addWithOffset(brush, location, true, wrapEdges);
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask useBrushWithinArea(BooleanMask other, String brushName, int size, int numUses, float intensity, boolean wrapEdges) {
        execute(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            assertSmallerSize(size);
            ArrayList<Vector2f> possibleLocations = new ArrayList<>(source.getAllCoordinatesEqualTo(true, 1));
            int length = possibleLocations.size();
            FloatMask brush = loadBrush(brushName, random.nextLong());
            brush.multiply(intensity / brush.getMax()).setSize(size);
            for (int i = 0; i < numUses; i++) {
                Vector2f location = possibleLocations.get(random.nextInt(length));
                addWithOffset(brush, location, true, wrapEdges);
            }
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    public FloatMask useBrushWithinAreaWithDensity(BooleanMask other, String brushName, int size, float density, float intensity, boolean wrapEdges) {
        execute(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            int frequency = (int) (density * (float) source.getCount() / 26.21f / symmetrySettings.getSpawnSymmetry().getNumSymPoints());
            useBrushWithinArea(source, brushName, size, frequency, intensity, wrapEdges);
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    public boolean areAnyEdgesGreaterThan(float value) {
        int size = getSize();
        int farEdge = size - 1;
        AtomicBoolean edgesGreater = new AtomicBoolean(false);
        apply((x, y) -> edgesGreater.set(getValueAt(x, y) > value || getValueAt(farEdge - x, farEdge - y) > value
                || getValueAt(x, farEdge - y) > value || getValueAt(farEdge - x, y) > value));
        return edgesGreater.get();
    }

    protected void add(BiFunction<Integer, Integer, Float> valueFunction) {
        int size = getSize();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                addValueAt(x, y, valueFunction.apply(x, y));
            }
        }
    }

    protected void addWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, Float> valueFunction) {
        int minX = getMinXBound(symmetryType);
        int maxX = getMaxXBound(symmetryType);
        for (int x = minX; x < maxX; x++) {
            int minY = getMinYBound(x, symmetryType);
            int maxY = getMaxYBound(x, symmetryType);
            for (int y = minY; y < maxY; y++) {
                Float value = valueFunction.apply(x, y);
                addValueAt(x, y, value);
                Vector2f location = new Vector2f(x, y);
                List<Vector2f> symmetryPoints = getSymmetryPoints(location, symmetryType);
                symmetryPoints.forEach(symmetryPoint -> addValueAt(symmetryPoint, value));
            }
        }
    }

    protected void multiply(BiFunction<Integer, Integer, Float> valueFunction) {
        int size = getSize();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                multiplyValueAt(x, y, valueFunction.apply(x, y));
            }
        }
    }

    protected void multiplyWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, Float> valueFunction) {
        int minX = getMinXBound(symmetryType);
        int maxX = getMaxXBound(symmetryType);
        for (int x = minX; x < maxX; x++) {
            int minY = getMinYBound(x, symmetryType);
            int maxY = getMaxYBound(x, symmetryType);
            for (int y = minY; y < maxY; y++) {
                Float value = valueFunction.apply(x, y);
                multiplyValueAt(x, y, value);
                Vector2f location = new Vector2f(x, y);
                List<Vector2f> symmetryPoints = getSymmetryPoints(location, symmetryType);
                symmetryPoints.forEach(symmetryPoint -> multiplyValueAt(symmetryPoint, value));
            }
        }
    }

    // -------------------------------------------
    @Override
    public String toHash() throws NoSuchAlgorithmException {
        int size = getSize();
        ByteBuffer bytes = ByteBuffer.allocate(size * size * 4);
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> bytes.putFloat(getValueAt(x, y)));
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }
}
