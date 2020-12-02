package map;

import brushes.Brushes;
import generator.VisualDebugger;
import lombok.Getter;
import lombok.SneakyThrows;
import util.Util;
import util.Vector2f;
import util.Vector3f;

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
import java.util.LinkedList;

import static brushes.Brushes.loadBrush;

@Getter
public strictfp class FloatMask extends Mask<Float> {

    public FloatMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        super(seed);
        this.mask = getEmptyMask(size);
        this.symmetrySettings = symmetrySettings;
        for (int y = 0; y < this.getSize(); y++) {
            for (int x = 0; x < this.getSize(); x++) {
                this.mask[x][y] = 0f;
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public FloatMask(BufferedImage image, Long seed, SymmetrySettings symmetrySettings) {
        super(seed);
        this.mask = getEmptyMask(image.getHeight());
        Raster imageData = image.getData();
        this.symmetrySettings = symmetrySettings;
        for (int y = 0; y < this.getSize(); y++) {
            for (int x = 0; x < this.getSize(); x++) {
                int[] vals = new int[1];
                imageData.getPixel(x, y, vals);
                this.mask[x][y] = vals[0] / 255f;
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public FloatMask(FloatMask floatMask, Long seed) {
        super(seed);
        this.mask = getEmptyMask(floatMask.getSize());
        this.symmetrySettings = floatMask.getSymmetrySettings();
        for (int y = 0; y < floatMask.getSize(); y++) {
            for (int x = 0; x < floatMask.getSize(); x++) {
                this.mask[x][y] = floatMask.getValueAt(x, y);
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public FloatMask(BinaryMask binaryMask, float low, float high, Long seed) {
        super(seed);
        this.mask = getEmptyMask(binaryMask.getSize());
        this.symmetrySettings = binaryMask.getSymmetrySettings();
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (binaryMask.getValueAt(x, y)) {
                    setValueAt(x, y, high);
                } else {
                    setValueAt(x, y, low);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    protected Float[][] getEmptyMask(int size) {
        Float[][] maskCopy = new Float[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                maskCopy[x][y] = 0f;
            }
        }
        return maskCopy;
    }

    @Override
    public int getSize() {
        return mask[0].length;
    }

    @Override
    public FloatMask setSize(int size) {
        super.setSize(size);
        return this;
    }

    @Override
    public Float getValueAt(Vector2f pos) {
        return mask[(int) pos.x][(int) pos.y];
    }

    public float getMin() {
        float val = Float.MAX_VALUE;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                val = StrictMath.min(val, getValueAt(x, y));
            }
        }
        return val;
    }

    public float getMax() {
        float val = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                val = StrictMath.max(val, getValueAt(x, y));
            }
        }
        return val;
    }

    public float getSum() {
        float val = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                val += getValueAt(x, y);
            }
        }
        return val;
    }

    public float getAvg() {
        return getSum() / getSize() / getSize();
    }

    public boolean isLocalMax(int x, int y) {
        float value = getValueAt(x, y);
        return ((x > 0 && getValueAt(x - 1, y) <= value)
                && (y > 0 && getValueAt(x, y - 1) <= value)
                && (x < getSize() - 1 && getValueAt(x + 1, y) <= value)
                && (y < getSize() - 1 && getValueAt(x, y + 1) <= value)
                && (getValueAt(x - 1, y - 1) <= value)
                && (getValueAt(x + 1, y - 1) <= value)
                && (getValueAt(x + 1, y + 1) <= value)
                && (getValueAt(x + 1, y + 1) <= value));
    }

    @Override
    public Float getValueAt(int x, int y) {
        return mask[x][y];
    }

    public boolean isLocal1DMax(int x, int y) {
        float value = getValueAt(x, y);
        return (((x > 0 && getValueAt(x - 1, y) <= value)
                && (x < getSize() - 1 && getValueAt(x + 1, y) <= value))
                || ((y > 0 && getValueAt(x, y - 1) <= value)
                && (y < getSize() - 1 && getValueAt(x, y + 1) <= value)));
    }

    public void setValueAt(Vector2f location, Float value) {
        setValueAt((int) location.x, (int) location.y, value);
    }

    public void setValueAt(Vector3f location, Float value) {
        setValueAt((int) location.x, (int) location.z, value);
    }

    public void addValueAt(Vector2f loc, float value) {
        addValueAt((int) loc.x, (int) loc.y, value);
    }

    public void addValueAt(int x, int y, float value) {
        mask[x][y] += value;
    }

    public void subtractValueAt(int x, int y, float value) {
        addValueAt(x, y, -value);
    }

    public void multiplyValueAt(Vector2f loc, float value) {
        multiplyValueAt((int) loc.x, (int) loc.y, value);
    }

    public void multiplyValueAt(int x, int y, float value) {
        mask[x][y] *= value;
    }

    public FloatMask init(BinaryMask other, float low, float high) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (other.getValueAt(x, y)) {
                    setValueAt(x, y, high);
                } else {
                    setValueAt(x, y, low);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask copy() {
        if (random != null) {
            return new FloatMask(this, random.nextLong());
        } else {
            return new FloatMask(this, null);
        }
    }

    public void setValueAt(int x, int y, Float value) {
        mask[x][y] = value;
    }

    public FloatMask multiply(FloatMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                multiplyValueAt(x, y, other.getValueAt(x, y));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask multiplyWithOffset(FloatMask other, Vector2f loc, boolean centered) {
        return multiplyWithOffset(other, (int) loc.x, (int) loc.y, centered);
    }

    public FloatMask clear() {
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                setValueAt(x, y, 0f);
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask multiplyAll(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                multiplyValueAt(x, y, val);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask convolve(FloatMask other) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                multiplyWithOffset(other, x, y, true);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask add(FloatMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                addValueAt(x, y, other.getValueAt(x, y));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addToAll(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                addValueAt(x, y, val);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtract(FloatMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        add(other.copy().multiplyAll(-1));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWithOffset(FloatMask other, Vector2f loc, boolean centered) {
        return addWithOffset(other, (int) loc.x, (int) loc.y, centered);
    }

    public FloatMask subtractWithOffset(FloatMask other, Vector2f loc, boolean center) {
        return addWithOffset(other.copy().multiplyAll(-1f), loc, center);
    }

    public FloatMask multiplyWithOffset(FloatMask other, int offsetX, int offsetY, boolean centered) {
        int size = StrictMath.min(getSize(), other.getSize());
        if (centered) {
            offsetX -= size / 2;
            offsetY -= size / 2;
        }
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int shiftX = x + offsetX - 1;
                int shiftY = y + offsetY - 1;
                if (getSize() != size) {
                    if (inBounds(shiftX, shiftY)) {
                        multiplyValueAt(shiftX, shiftY, other.getValueAt(x, y));
                        ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(shiftX, shiftY);
                        for (SymmetryPoint symmetryPoint : symmetryPoints) {
                            multiplyValueAt(symmetryPoint.getLocation(), other.getValueAt(x, y));
                        }
                    }
                } else {
                    if (other.inBounds(shiftX, shiftY)) {
                        multiplyValueAt(x, y, other.getValueAt(shiftX, shiftY));
                    }
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtractWithOffset(FloatMask other, int offsetX, int offsetY, boolean center) {
        return addWithOffset(other.copy().multiplyAll(-1f), offsetX, offsetY, center);
    }

    public FloatMask add(BinaryMask other, float value) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        FloatMask otherFloat = new FloatMask(getSize(), null, symmetrySettings);
        otherFloat.init(other, 0, value);
        add(otherFloat);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addGaussianNoise(float scale) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                addValueAt(x, y, (float) random.nextGaussian() * scale);
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWhiteNoise(float scale) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                addValueAt(x, y, random.nextFloat() * scale);
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtract(BinaryMask other, float value) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        FloatMask otherFloat = new FloatMask(getSize(), null, symmetrySettings);
        otherFloat.init(other, 0, -value);
        add(otherFloat);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask sqrt() {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                setValueAt(x, y, (float) StrictMath.sqrt(getValueAt(x, y)));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask min(FloatMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                setValueAt(x, y, StrictMath.min(getValueAt(x, y), other.getValueAt(x, y)));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask clampMin(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                setValueAt(x, y, StrictMath.max(getValueAt(x, y), val));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask clampMinInArea(float val, BinaryMask area) {
        if (area.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size: area is " + area.getSize() + " and FloatMask is " + getSize());
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (area.getValueAt(x, y)) {
                    setValueAt(x, y, StrictMath.max(getValueAt(x, y), val));
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask threshold(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (getValueAt(x, y) < val) {
                    setValueAt(x, y, 0f);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask max(FloatMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                setValueAt(x, y, StrictMath.max(getValueAt(x, y), other.getValueAt(x, y)));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask clampMax(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                setValueAt(x, y, StrictMath.min(getValueAt(x, y), val));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask clampMaxInArea(float val, BinaryMask area) {
        if (area.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size: area is " + area.getSize() + " and FloatMask is " + getSize());
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (area.getValueAt(x, y)) {
                    setValueAt(x, y, StrictMath.min(getValueAt(x, y), val));
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWithOffset(FloatMask other, int offsetX, int offsetY, boolean center) {
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
                    if (inBounds(shiftX, shiftY)) {
                        addValueAt(shiftX, shiftY, other.getValueAt(x, y));
                        ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(shiftX, shiftY);
                        for (SymmetryPoint symmetryPoint : symmetryPoints) {
                            addValueAt(symmetryPoint.getLocation(), other.getValueAt(x, y));
                        }
                    }
                } else {
                    if (other.inBounds(shiftX, shiftY)) {
                        addValueAt(x, y, other.getValueAt(shiftX, shiftY));
                    }
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask setToZeroForValue(BinaryMask other, boolean value) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (other.getValueAt(x, y) == value) {
                    setValueAt(x, y, 0f);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeValuesOutsideOfRange(float min, float max) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (this.getValueAt(x, y) < min || this.getValueAt(x, y) > max) {
                    setValueAt(x, y, 0f);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeValuesInRange(float min, float max) {
        subtract(this.copy().removeValuesOutsideOfRange(min, max));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask replaceValuesInRangeWith(BinaryMask range, FloatMask replacement) {
        if (range.getSize() != getSize() || replacement.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        setToZeroForValue(range, true).add(replacement.copy().setToZeroForValue(range, false));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask smoothWithinSpecifiedDistanceOfEdgesOf(BinaryMask other, int distance) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int x = 0; x < distance; x = x + 2) {
            replaceValuesInRangeWith(other.getAreasWithinSpecifiedDistanceOfEdges(x + 1), copy().smooth(1));
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask reduceValuesOnIntersectingSmoothingZones(BinaryMask avoidMakingZonesHere, float floatMax) {
        if (avoidMakingZonesHere.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        avoidMakingZonesHere = avoidMakingZonesHere.copy();
        FloatMask newMaskInZones = copy().smooth(34).subtract(copy()).subtract(avoidMakingZonesHere, 1f * floatMax);
        BinaryMask zones = newMaskInZones.copy().removeValuesInRange(0f * floatMax, 0.5f * floatMax).smooth(2).convertToBinaryMask(0.5f * floatMax, 1f * floatMax).inflate(34);
        BinaryMask newMaskInZonesBase = convertToBinaryMask(1f * floatMax, 1f * floatMax).deflate(3).minus(zones.copy().invert());
        newMaskInZones.init(newMaskInZonesBase, 0, 1).smooth(4).clampMax(0.35f * floatMax).add(newMaskInZonesBase, 1f * floatMax).smooth(2).clampMax(0.65f * floatMax).add(newMaskInZonesBase, 1f * floatMax).smooth(1).add(newMaskInZonesBase, 1f * floatMax).clampMax(1f * floatMax);
        replaceValuesInRangeWith(zones, newMaskInZones).smoothWithinSpecifiedDistanceOfEdgesOf(zones, 30);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask convertToBinaryMask(float minValueToConvert, float maxValueToConvert) {
        BinaryMask newMask = new BinaryMask(this.copy().removeValuesOutsideOfRange(minValueToConvert, maxValueToConvert), minValueToConvert, random.nextLong());
        VisualDebugger.visualizeMask(this);
        return newMask;
    }

    public FloatMask getDistanceFieldForRange(float minValue, float maxValue) {
        convertToBinaryMask(minValue, maxValue).getDistanceField();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeAreasOutsideOfSpecifiedIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        FloatMask tempMask2 = copy().init(this.copy().convertToBinaryMask(minIntensity, maxIntensity).removeAreasOutsideOfSpecifiedSize(minSize, maxSize).invert(), 0f, 1f);
        this.subtract(tempMask2).clampMin(0f);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeAreasOfSpecifiedIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        subtract(this.copy().removeAreasOutsideOfSpecifiedIntensityAndSize(minSize, maxSize, minIntensity, maxIntensity));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeAreasOfSpecifiedSizeWithLocalMaximums(int minSize, int maxSize, int levelOfPrecision, float floatMax) {
        for (int x = 0; x < levelOfPrecision; x++) {
            removeAreasOfSpecifiedIntensityAndSize(minSize, maxSize, ((1f - (float) x / (float) levelOfPrecision) * floatMax), 1f * floatMax);
        }
        removeAreasOfSpecifiedIntensityAndSize(minSize, maxSize, 0.0000001f, 1f * floatMax);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask getLocalMaximums(float minValue, float maxValue) {
        BinaryMask localMaxima = new BinaryMask(getSize(), random.nextLong(), symmetrySettings);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                float value = getValueAt(x, y);
                if (value > minValue && value < maxValue && isLocalMax(x, y)) {
                    localMaxima.setValueAt(x, y, true);
                }
            }
        }
        return localMaxima;
    }

    public BinaryMask getLocal1DMaximums(float minValue, float maxValue) {
        BinaryMask localMaxima = new BinaryMask(getSize(), random.nextLong(), symmetrySettings);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                float value = getValueAt(x, y);
                if (value > minValue && value < maxValue && isLocal1DMax(x, y)) {
                    localMaxima.setValueAt(x, y, true);
                }
            }
        }
        return localMaxima;
    }

    public FloatMask maskToHills(BinaryMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        FloatMask brush = loadBrush(Brushes.HILL_BRUSHES[random.nextInt(Brushes.HILL_BRUSHES.length)], symmetrySettings);
        FloatMask otherDistance = other.copy().invert().getDistanceField();
        BinaryMask distanceMaximums = otherDistance.getLocalMaximums(.1f, Float.POSITIVE_INFINITY);
        LinkedList<Vector2f> coordinates = new LinkedList<>(distanceMaximums.getRandomCoordinates(16));
        FloatMask heightMultiplier = otherDistance.copy().clampMax(10f).smooth(2);
        while (coordinates.size() > 0) {
            Vector2f loc = coordinates.removeFirst();
            FloatMask useBrush = (FloatMask) brush.copy().shrink((int) (otherDistance.getValueAt(loc) * 8));
            useBrush.multiplyWithOffset(heightMultiplier, loc, true);
            addWithOffset(useBrush, loc, true);
            coordinates.removeIf(cloc -> loc.getDistance(cloc) < otherDistance.getValueAt(loc) * 2);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask maskToOceanHeights(float underWaterSlope, BinaryMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        FloatMask otherDistance = other.getDistanceField();
        add(otherDistance.multiplyAll(-underWaterSlope));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public int[][] getInnerCount() {
        int[][] innerCount = new int[getSize()][getSize()];

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                int val = StrictMath.round(getValueAt(x, y) * 1000);
                innerCount[x][y] = val;
                innerCount[x][y] += x > 0 ? innerCount[x - 1][y] : 0;
                innerCount[x][y] += y > 0 ? innerCount[x][y - 1] : 0;
                innerCount[x][y] -= x > 0 && y > 0 ? innerCount[x - 1][y - 1] : 0;
            }
        }
        return innerCount;
    }

    public FloatMask smooth(int radius) {
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
                float count = (countD + countA - countB - countC) / 1000f;
                int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
                setValueAt(x, y, count / area);
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask smooth(int radius, BinaryMask limiter) {
        if (limiter.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        int[][] innerCount = getInnerCount();

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (limiter.getValueAt(x, y)) {
                    int xLeft = StrictMath.max(0, x - radius);
                    int xRight = StrictMath.min(getSize() - 1, x + radius);
                    int yUp = StrictMath.max(0, y - radius);
                    int yDown = StrictMath.min(getSize() - 1, y + radius);
                    int countA = xLeft > 0 && yUp > 0 ? innerCount[xLeft - 1][yUp - 1] : 0;
                    int countB = yUp > 0 ? innerCount[xRight][yUp - 1] : 0;
                    int countC = xLeft > 0 ? innerCount[xLeft - 1][yDown] : 0;
                    int countD = innerCount[xRight][yDown];
                    float count = (countD + countA - countB - countC) / 1000f;
                    int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
                    setValueAt(x, y, count / area);
                }
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask spike(int radius) {
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
                float count = (countD + countA - countB - countC) / 1000f;
                int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
                setValueAt(x, y, count / area * count / area);
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask spike(int radius, BinaryMask limiter) {
        if (limiter.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        int[][] innerCount = getInnerCount();

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (limiter.getValueAt(x, y)) {
                    int xLeft = StrictMath.max(0, x - radius);
                    int xRight = StrictMath.min(getSize() - 1, x + radius);
                    int yUp = StrictMath.max(0, y - radius);
                    int yDown = StrictMath.min(getSize() - 1, y + radius);
                    int countA = xLeft > 0 && yUp > 0 ? innerCount[xLeft - 1][yUp - 1] : 0;
                    int countB = yUp > 0 ? innerCount[xRight][yUp - 1] : 0;
                    int countC = xLeft > 0 ? innerCount[xLeft - 1][yDown] : 0;
                    int countD = innerCount[xRight][yDown];
                    float count = (countD + countA - countB - countC) / 1000f;
                    int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
                    setValueAt(x, y, count / area * count / area);
                }
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addFloatMaskCenteredAtLocationWithSize(FloatMask other, Vector2f location, int size) {
        if (size > getSize()) {
            throw new IllegalArgumentException("Added mask size is larger than base mask size");
        }
        FloatMask maskToBeAdded = other.copy().setSize(size);
        addWithOffset(maskToBeAdded, location, true);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask useBrush(Vector2f location, String brushName, float intensity, int size) {
        FloatMask brush = loadBrush(brushName, new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
        brush.multiplyAll(intensity / brush.getMax());
        addFloatMaskCenteredAtLocationWithSize(brush, location, size);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask useBrushRepeatedlyCenteredWithinArea(BinaryMask area, String brushName, int size, int frequency, float intensity) {
        if (size > getSize()) {
            throw new IllegalArgumentException("Added mask size is larger than base mask size");
        }
        ArrayList<Vector2f> possibleLocations = new ArrayList<>(area.getAllCoordinatesEqualTo(true, 1));
        int length = possibleLocations.size();
        FloatMask brush = loadBrush(brushName, new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
        brush.multiplyAll(intensity / brush.getMax()).setSize(size);
        for (int z = 0; z < frequency; z++) {
            addWithOffset(brush, possibleLocations.get(random.nextInt(length)), true);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask useBrushRepeatedlyCenteredWithinAreaToDensity(BinaryMask area, String brushName, int size, float density, float intensity) {
        int frequency = (int) (density * (float) area.getCount() / 2621f);
        useBrushRepeatedlyCenteredWithinArea(area, brushName, size, frequency, intensity);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask maskToMountains(BinaryMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        FloatMask brush = loadBrush(Brushes.MOUNTAIN_BRUSHES[random.nextInt(Brushes.MOUNTAIN_BRUSHES.length)], symmetrySettings);
        brush.multiplyAll(1 / brush.getMax());
        FloatMask otherDistance = other.copy().invert().getDistanceField();
        BinaryMask distanceMaximums = otherDistance.getLocalMaximums(.1f, Float.POSITIVE_INFINITY);
        LinkedList<Vector2f> coordinates = new LinkedList<>(distanceMaximums.getRandomCoordinates(16));
        FloatMask heightMultiplier = otherDistance.copy().clampMax(16f).smooth(2);
        while (coordinates.size() > 0) {
            Vector2f loc = coordinates.removeFirst();
            FloatMask useBrush = (FloatMask) brush.copy().shrink((int) (otherDistance.getValueAt(loc) * 8), Symmetry.NONE);
            useBrush.multiplyWithOffset(heightMultiplier, loc, true);
            addWithOffset(useBrush, loc, true);
            coordinates.removeIf(cloc -> loc.getDistance(cloc) < otherDistance.getValueAt(loc) * 2);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask gradient() {
        Float[][] maskCopy = getEmptyMask(getSize());
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                int xNeg = StrictMath.max(0, x - 1);
                int xPos = StrictMath.min(getSize() - 1, x + 1);
                int yNeg = StrictMath.max(0, y - 1);
                int yPos = StrictMath.min(getSize() - 1, y + 1);
                float xSlope = getValueAt(xPos, y) - getValueAt(xNeg, y);
                float ySlope = getValueAt(x, yPos) - getValueAt(x, yNeg);
                maskCopy[x][y] = (float) StrictMath.sqrt(xSlope * xSlope + ySlope * ySlope);
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask supcomGradient() {
        Float[][] maskCopy = getEmptyMask(getSize());
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                int xPos = StrictMath.min(getSize() - 1, x + 1);
                int yPos = StrictMath.min(getSize() - 1, y + 1);
                float xSlope = StrictMath.abs(getValueAt(x, y) - getValueAt(xPos, y));
                float ySlope = StrictMath.abs(getValueAt(x, y) - getValueAt(x, yPos));
                float diagSlope = StrictMath.abs(getValueAt(x, y) - getValueAt(xPos, yPos));
                maskCopy[x][y] = Collections.max(Arrays.asList(xSlope, ySlope, diagSlope));
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
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

    public String toHash() throws NoSuchAlgorithmException {
        ByteBuffer bytes = ByteBuffer.allocate(getSize() * getSize() * 4);
        for (int x = getMinXBound(symmetrySettings.getSpawnSymmetry()); x < getMaxXBound(symmetrySettings.getSpawnSymmetry()); x++) {
            for (int y = getMinYBound(x, symmetrySettings.getSpawnSymmetry()); y < getMaxYBound(x, symmetrySettings.getSpawnSymmetry()); y++) {
                bytes.putFloat(getValueAt(x, y));
            }
        }
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }


    public FloatMask startVisualDebugger(String maskName) {
        return startVisualDebugger(maskName, Util.getStackTraceParentClass());
    }

    public FloatMask startVisualDebugger(String maskName, String parentClass) {
        VisualDebugger.whitelistMask(this, maskName, parentClass);
        show();
        return this;
    }
}
