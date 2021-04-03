package neroxis.map;

import lombok.Getter;
import lombok.SneakyThrows;
import neroxis.util.Vector2f;
import neroxis.util.VisualDebugger;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static neroxis.brushes.Brushes.loadBrush;

@Getter
public strictfp class FloatMask extends Mask<Float> {

    public FloatMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        super(seed);
        this.mask = getEmptyMask(size);
        this.symmetrySettings = symmetrySettings;
        VisualDebugger.visualizeMask(this);
    }

    public FloatMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings) {
        super(seed);
        this.mask = getEmptyMask(sourceImage.getHeight());
        Raster imageData = sourceImage.getData();
        this.symmetrySettings = symmetrySettings;
        modify((x, y) -> {
            int[] value = new int[1];
            imageData.getPixel(x, y, value);
            return value[0] / 255f;
        });
        VisualDebugger.visualizeMask(this);
    }

    public FloatMask(FloatMask sourceMask, Long seed) {
        super(seed);
        this.mask = getEmptyMask(sourceMask.getSize());
        this.symmetrySettings = sourceMask.getSymmetrySettings();
        modify(sourceMask::getValueAt);
        VisualDebugger.visualizeMask(this);
    }

    public FloatMask(BinaryMask sourceMask, float low, float high, Long seed) {
        super(seed);
        this.mask = getEmptyMask(sourceMask.getSize());
        this.symmetrySettings = sourceMask.getSymmetrySettings();
        modify((x, y) -> sourceMask.getValueAt(x, y) ? high : low);
        VisualDebugger.visualizeMask(this);
    }

    protected Float[][] getEmptyMask(int size) {
        Float[][] empty = new Float[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                empty[x][y] = 0f;
            }
        }
        return empty;
    }

    public void addValueAt(Vector2f loc, float value) {
        addValueAt((int) loc.getX(), (int) loc.getY(), value);
    }

    public void addValueAt(int x, int y, float value) {
        mask[x][y] += value;
    }

    public void subtractValueAt(int x, int y, float value) {
        addValueAt(x, y, -value);
    }

    public void multiplyValueAt(Vector2f loc, float value) {
        multiplyValueAt((int) loc.getX(), (int) loc.getY(), value);
    }

    public void multiplyValueAt(int x, int y, float value) {
        mask[x][y] *= value;
    }

    public boolean isLocalMax(int x, int y) {
        float value = getValueAt(x, y);
        return ((x > 0 && getValueAt(x - 1, y) <= value)
                && (x < getSize() - 1 && getValueAt(x + 1, y) <= value)
                && (y > 0 && getValueAt(x, y - 1) <= value)
                && (y < getSize() - 1 && getValueAt(x, y + 1) <= value)
                && (getValueAt(x - 1, y - 1) <= value)
                && (getValueAt(x + 1, y - 1) <= value)
                && (getValueAt(x - 1, y + 1) <= value)
                && (getValueAt(x + 1, y + 1) <= value));
    }

    public boolean isLocal1DMax(int x, int y) {
        float value = getValueAt(x, y);
        return (((x > 0 && getValueAt(x - 1, y) <= value)
                && (x < getSize() - 1 && getValueAt(x + 1, y) <= value))
                || ((y > 0 && getValueAt(x, y - 1) <= value)
                && (y < getSize() - 1 && getValueAt(x, y + 1) <= value)));
    }

    public float getMin() {
        final float[] val = {Float.MAX_VALUE};
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> val[0] = StrictMath.min(val[0], getValueAt(x, y)));
        return val[0];
    }

    public float getMax() {
        final float[] val = {-Float.MAX_VALUE};
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> val[0] = StrictMath.max(val[0], getValueAt(x, y)));
        return val[0];
    }

    public float getSum() {
        final float[] val = {0};
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> val[0] += getValueAt(x, y));
        return val[0];
    }

    public float getAvg() {
        return getSum() / getSize() / getSize();
    }

    public FloatMask init(BinaryMask other, float low, float high) {
        setSize(other.getSize());
        checkCompatibleMask(other);
        modify((x, y) -> other.getValueAt(x, y) ? high : low);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask init(FloatMask other) {
        setSize(other.getSize());
        checkCompatibleMask(other);
        modify(other::getValueAt);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    @Override
    public FloatMask copy() {
        if (random != null) {
            return new FloatMask(this, random.nextLong());
        } else {
            return new FloatMask(this, null);
        }
    }

    public FloatMask clear() {
        modify((x, y) -> 0f);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addGaussianNoise(float scale) {
        addWithSymmetry(SymmetryType.SPAWN, (x, y) -> (float) random.nextGaussian() * scale);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWhiteNoise(float scale) {
        addWithSymmetry(SymmetryType.SPAWN, (x, y) -> random.nextFloat() * scale);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addDistance(BinaryMask other, float scale) {
        checkCompatibleMask(other);
        FloatMask distanceField = other.getDistanceField();
        add(distanceField.multiply(scale));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask add(FloatMask other) {
        checkCompatibleMask(other);
        add(other::getValueAt);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask add(BinaryMask other, float value) {
        checkCompatibleMask(other);
        add((x, y) -> other.getValueAt(x, y) ? value : 0);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask add(float val) {
        add((x, y) -> val);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWeighted(FloatMask other, float weight) {
        checkCompatibleMask(other);
        add((x, y) -> other.getValueAt(x, y) * weight);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWithOffset(FloatMask other, Vector2f loc, boolean centered, boolean wrapEdges) {
        return addWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    public FloatMask addWithOffset(FloatMask other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
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
            other.apply((x, y) -> {
                int shiftX = getShiftedValue(x, offsetX, size, wrapEdges);
                int shiftY = getShiftedValue(y, offsetY, size, wrapEdges);
                if (inBounds(shiftX, shiftY)) {
                    float value = other.getValueAt(x, y);
                    addValueAt(shiftX, shiftY, value);
                    List<Vector2f> symmetryPoints = getSymmetryPoints(shiftX, shiftY, SymmetryType.SPAWN);
                    for (Vector2f symmetryPoint : symmetryPoints) {
                        addValueAt(symmetryPoint, value);
                    }
                }
            });
        } else {
            apply((x, y) -> {
                int shiftX = getShiftedValue(x, offsetX, otherSize, wrapEdges);
                int shiftY = getShiftedValue(y, offsetY, otherSize, wrapEdges);
                if (other.inBounds(shiftX, shiftY)) {
                    addValueAt(x, y, other.getValueAt(shiftX, shiftY));
                }
            });
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtractAvg() {
        return subtract(getAvg());
    }

    public FloatMask subtract(float val) {
        return add(-val);
    }

    public FloatMask subtract(FloatMask other) {
        checkCompatibleMask(other);
        add((x, y) -> -other.getValueAt(x, y));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtract(BinaryMask other, float value) {
        checkCompatibleMask(other);
        add((x, y) -> other.getValueAt(x, y) ? -value : 0);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtractWithOffset(FloatMask other, Vector2f loc, boolean center, boolean wrapEdges) {
        return addWithOffset(other.copy().multiply(-1f), loc, center, wrapEdges);
    }

    public FloatMask subtractWithOffset(FloatMask other, int offsetX, int offsetY, boolean center, boolean wrapEdges) {
        return addWithOffset(other.copy().multiply(-1f), offsetX, offsetY, center, wrapEdges);
    }

    public FloatMask multiply(FloatMask other) {
        checkCompatibleMask(other);
        multiply(other::getValueAt);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask multiply(float val) {
        multiply((x, y) -> val);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask multiplyWithOffset(FloatMask other, Vector2f loc, boolean centered, boolean wrapEdges) {
        return multiplyWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    public FloatMask multiplyWithOffset(FloatMask other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
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
            other.apply((x, y) -> {
                int shiftX = getShiftedValue(x, offsetX, size, wrapEdges);
                int shiftY = getShiftedValue(y, offsetY, size, wrapEdges);
                if (inBounds(shiftX, shiftY)) {
                    float value = other.getValueAt(x, y);
                    multiplyValueAt(shiftX, shiftY, value);
                    List<Vector2f> symmetryPoints = getSymmetryPoints(shiftX, shiftY, SymmetryType.SPAWN);
                    for (Vector2f symmetryPoint : symmetryPoints) {
                        multiplyValueAt(symmetryPoint, value);
                    }
                }
            });
        } else {
            apply((x, y) -> {
                int shiftX = getShiftedValue(x, offsetX, otherSize, wrapEdges);
                int shiftY = getShiftedValue(y, offsetY, otherSize, wrapEdges);
                if (other.inBounds(shiftX, shiftY)) {
                    multiplyValueAt(x, y, other.getValueAt(shiftX, shiftY));
                }
            });
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask sqrt() {
        modify((x, y) -> (float) StrictMath.sqrt(getValueAt(x, y)));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask max(FloatMask other) {
        checkCompatibleMask(other);
        modify((x, y) -> StrictMath.max(getValueAt(x, y), other.getValueAt(x, y)));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask clampMax(BinaryMask area, float val) {
        checkCompatibleMask(area);
        modify((x, y) -> area.getValueAt(x, y) ? StrictMath.min(getValueAt(x, y), val) : getValueAt(x, y));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask clampMax(float val) {
        modify((x, y) -> StrictMath.min(getValueAt(x, y), val));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask min(FloatMask other) {
        checkCompatibleMask(other);
        modify((x, y) -> StrictMath.min(getValueAt(x, y), other.getValueAt(x, y)));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask clampMin(BinaryMask area, float val) {
        checkCompatibleMask(area);
        modify((x, y) -> area.getValueAt(x, y) ? StrictMath.max(getValueAt(x, y), val) : getValueAt(x, y));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask clampMin(float val) {
        modify((x, y) -> StrictMath.max(getValueAt(x, y), val));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask gradient() {
        Float[][] maskCopy = getEmptyMask(getSize());
        apply((x, y) -> {
            int xNeg = StrictMath.max(0, x - 1);
            int xPos = StrictMath.min(getSize() - 1, x + 1);
            int yNeg = StrictMath.max(0, y - 1);
            int yPos = StrictMath.min(getSize() - 1, y + 1);
            float xSlope = getValueAt(xPos, y) - getValueAt(xNeg, y);
            float ySlope = getValueAt(x, yPos) - getValueAt(x, yNeg);
            maskCopy[x][y] = (float) StrictMath.sqrt(xSlope * xSlope + ySlope * ySlope);
        });
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask supcomGradient() {
        Float[][] maskCopy = getEmptyMask(getSize());
        apply((x, y) -> {
            int xPos = StrictMath.min(getSize() - 1, x + 1);
            int yPos = StrictMath.min(getSize() - 1, y + 1);
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
        return this;
    }

    public FloatMask threshold(float val) {
        modify((x, y) -> getValueAt(x, y) < val ? 0 : getValueAt(x, y));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask fixNonPerfectSymmetry() {
        return smooth(1);
    }


    public FloatMask smooth(int radius) {
        int[][] innerCount = getInnerCount();
        modify((x, y) -> calculateAreaAverage(radius, x, y, innerCount) / 1000);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask smooth(int radius, BinaryMask limiter) {
        checkCompatibleMask(limiter);
        int[][] innerCount = getInnerCount();
        modify((x, y) -> limiter.getValueAt(x, y) ? calculateAreaAverage(radius, x, y, innerCount) / 1000 : getValueAt(x, y));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask spike(int radius) {
        int[][] innerCount = getInnerCount();
        modify((x, y) -> {
            float value = calculateAreaAverage(radius, x, y, innerCount) / 1000;
            return value * value;
        });
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask spike(int radius, BinaryMask limiter) {
        checkCompatibleMask(limiter);
        int[][] innerCount = getInnerCount();
        modify((x, y) -> {
            if (limiter.getValueAt(x, y)) {
                float value = calculateAreaAverage(radius, x, y, innerCount) / 1000;
                return value * value;
            } else {
                return getValueAt(x, y);
            }
        });
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask zeroOutsideRange(float min, float max) {
        modify((x, y) -> getValueAt(x, y) < min || getValueAt(x, y) > max ? 0 : getValueAt(x, y));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask setToValue(BinaryMask other, float val) {
        checkCompatibleMask(other);
        modify((x, y) -> other.getValueAt(x, y) ? val : getValueAt(x, y));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask replaceValues(BinaryMask other, FloatMask replacement) {
        checkCompatibleMask(other);
        checkCompatibleMask(replacement);
        modify((x, y) -> other.getValueAt(x, y) ? replacement.getValueAt(x, y) : getValueAt(x, y));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask zeroInRange(float min, float max) {
        modify((x, y) -> getValueAt(x, y) >= min && getValueAt(x, y) < max ? 0 : getValueAt(x, y));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask convertToBinaryMask(float minValue, float maxValue) {
        BinaryMask newMask = new BinaryMask(this, minValue, maxValue, random.nextLong());
        VisualDebugger.visualizeMask(this);
        return newMask;
    }

    public FloatMask removeAreasOutsideIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        FloatMask tempMask2 = copy().init(this.copy().convertToBinaryMask(minIntensity, maxIntensity).removeAreasOutsideSizeRange(minSize, maxSize).invert(), 0f, 1f);
        this.subtract(tempMask2).clampMin(0f);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeAreasInIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        subtract(this.copy().removeAreasOutsideIntensityAndSize(minSize, maxSize, minIntensity, maxIntensity));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeAreasOfSpecifiedSizeWithLocalMaximums(int minSize, int maxSize, int levelOfPrecision, float floatMax) {
        for (int x = 0; x < levelOfPrecision; x++) {
            removeAreasInIntensityAndSize(minSize, maxSize, ((1f - (float) x / (float) levelOfPrecision) * floatMax), floatMax);
        }
        removeAreasInIntensityAndSize(minSize, maxSize, 0.0000001f, floatMax);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask getLocalMaximums(float minValue, float maxValue) {
        BinaryMask localMaxima = new BinaryMask(getSize(), random.nextLong(), symmetrySettings);
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> {
            float value = getValueAt(x, y);
            if (value >= minValue && value < maxValue && isLocalMax(x, y)) {
                localMaxima.setValueAt(x, y, true);
                List<Vector2f> symmetryPoints = getSymmetryPoints(x, y, SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> localMaxima.setValueAt(symmetryPoint, true));
            }
        });
        return localMaxima;
    }

    public BinaryMask getLocal1DMaximums(float minValue, float maxValue) {
        BinaryMask localMaxima = new BinaryMask(getSize(), random.nextLong(), symmetrySettings);
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> {
            float value = getValueAt(x, y);
            if (value > minValue && value < maxValue && isLocal1DMax(x, y)) {
                localMaxima.setValueAt(x, y, true);
                List<Vector2f> symmetryPoints = getSymmetryPoints(x, y, SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> localMaxima.setValueAt(symmetryPoint, true));
            }
        });
        return localMaxima;
    }

    @Override
    protected int[][] getInnerCount() {
        int[][] innerCount = new int[getSize()][getSize()];
        apply((x, y) -> calculateInnerValue(innerCount, x, y, StrictMath.round(getValueAt(x, y) * 1000)));
        return innerCount;
    }

    public FloatMask getDistanceFieldForRange(float minValue, float maxValue) {
        convertToBinaryMask(minValue, maxValue).getDistanceField();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask useBrush(Vector2f location, String brushName, float intensity, int size, boolean wrapEdges) {
        FloatMask brush = loadBrush(brushName, random.nextLong());
        brush.multiply(intensity / brush.getMax()).setSize(size);
        addWithOffset(brush, location, true, wrapEdges);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask useBrushWithinArea(BinaryMask area, String brushName, int size, int numUses, float intensity, boolean wrapEdges) {
        checkSmallerSize(size);
        ArrayList<Vector2f> possibleLocations = new ArrayList<>(area.getAllCoordinatesEqualTo(true, 1));
        int length = possibleLocations.size();
        FloatMask brush = loadBrush(brushName, random.nextLong());
        brush.multiply(intensity / brush.getMax()).setSize(size);
        for (int i = 0; i < numUses; i++) {
            Vector2f location = possibleLocations.get(random.nextInt(length));
            addWithOffset(brush, location, true, wrapEdges);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask useBrushWithinAreaWithDensity(BinaryMask area, String brushName, int size, float density, float intensity, boolean wrapEdges) {
        int frequency = (int) (density * (float) area.getCount() / 26.21f / symmetrySettings.getSpawnSymmetry().getNumSymPoints());
        useBrushWithinArea(area, brushName, size, frequency, intensity, wrapEdges);
        VisualDebugger.visualizeMask(this);
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
        for (int x = getMinXBound(symmetryType); x < getMaxXBound(symmetryType); x++) {
            for (int y = getMinYBound(x, symmetryType); y < getMaxYBound(x, symmetryType); y++) {
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
        for (int x = getMinXBound(symmetryType); x < getMaxXBound(symmetryType); x++) {
            for (int y = getMinYBound(x, symmetryType); y < getMaxYBound(x, symmetryType); y++) {
                Float value = valueFunction.apply(x, y);
                multiplyValueAt(x, y, value);
                Vector2f location = new Vector2f(x, y);
                List<Vector2f> symmetryPoints = getSymmetryPoints(location, symmetryType);
                symmetryPoints.forEach(symmetryPoint -> multiplyValueAt(symmetryPoint, value));
            }
        }
    }

    // -------------------------------------------

    @SneakyThrows
    public void writeToFile(Path path) {
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path.toFile())));

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                out.writeFloat(getValueAt(x, y));
            }
        }

        out.close();
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        ByteBuffer bytes = ByteBuffer.allocate(getSize() * getSize() * 4);
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> bytes.putFloat(getValueAt(x, y)));
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }
}
