package neroxis.map;

import lombok.Getter;
import lombok.SneakyThrows;
import neroxis.generator.VisualDebugger;
import neroxis.util.Util;
import neroxis.util.Vector2f;

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

import static neroxis.brushes.Brushes.loadBrush;

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
        Float[][] empty = new Float[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                empty[x][y] = 0f;
            }
        }
        return empty;
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

    public FloatMask init(BinaryMask other, float low, float high) {
        checkSize(other);
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

    public FloatMask clear() {
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                setValueAt(x, y, 0f);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addGaussianNoise(float scale) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                addValueAt(x, y, (float) random.nextGaussian() * scale);
            }
        }
        int numSymPoints = symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        boolean symmetric = numSymPoints % 2 == 0 && numSymPoints <= 4;
        if (symmetric) {
            applySymmetry(SymmetryType.SPAWN);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWhiteNoise(float scale) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                addValueAt(x, y, random.nextFloat() * scale);
            }
        }
        applySymmetry(SymmetryType.SPAWN);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addDistance(BinaryMask other, float scale) {
        checkSize(other);
        FloatMask distanceField = other.getDistanceField();
        add(distanceField.multiply(scale));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask add(FloatMask other) {
        checkSize(other);
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                addValueAt(x, y, other.getValueAt(x, y));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask add(BinaryMask other, float value) {
        checkSize(other);
        FloatMask otherFloat = new FloatMask(getSize(), null, symmetrySettings);
        otherFloat.init(other, 0, value);
        add(otherFloat);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask add(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                addValueAt(x, y, val);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWeighted(FloatMask other, float weight) {
        checkSize(other);
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                addValueAt(x, y, other.getValueAt(x, y) * weight);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addtoroidally(FloatMask other) {
        int size = getSize();
        int otherSize = other.getSize();
        for (int y = 0; y < otherSize; y++) {
            for (int x = 0; x < otherSize; x++) {
                addValueAt(x % size, y % size, other.getValueAt(x, y));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWeightedtoroidally(FloatMask other, float weight) {
        int size = getSize();
        int otherSize = other.getSize();
        for (int y = 0; y < otherSize; y++) {
            for (int x = 0; x < otherSize; x++) {
                addValueAt(x % size, y % size, other.getValueAt(x, y) * weight);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWithOffsettoroidally(FloatMask other, int offsetX, int offsetY, boolean centered) {
        int size = getSize();
        int otherSize = other.getSize();
        if(centered) {
            offsetX = offsetX - otherSize / 2;
            offsetY = offsetY - otherSize / 2;
        }
        for (int y = 0; y < otherSize; y++) {
            for (int x = 0; x < otherSize; x++) {
                addValueAt((x + offsetX + size) % size, (y + offsetY + size) % size, other.getValueAt(x, y));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWithOffsettoroidally(FloatMask other, Vector2f loc, boolean centered) {
        addWithOffsettoroidally(other, (int) loc.x, (int) loc.y, centered);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWithOffset(FloatMask other, Vector2f loc, boolean centered) {
        return addWithOffset(other, (int) loc.x, (int) loc.y, centered);
    }

    public FloatMask addWithOffset(FloatMask other, int offsetX, int offsetY, boolean center) {
        int size = StrictMath.min(getSize(), other.getSize());
        if (center) {
            offsetX -= size / 2;
            offsetY -= size / 2;
        }
        boolean symmetric = symmetrySettings.getSpawnSymmetry().isPerfectSymmetry();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int shiftX = x + offsetX - 1;
                int shiftY = y + offsetY - 1;
                if (getSize() != size) {
                    if (inBounds(shiftX, shiftY)) {
                        addValueAt(shiftX, shiftY, other.getValueAt(x, y));
                        if (symmetric) {
                            ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(shiftX, shiftY, SymmetryType.SPAWN);
                            for (SymmetryPoint symmetryPoint : symmetryPoints) {
                                addValueAt(symmetryPoint.getLocation(), other.getValueAt(x, y));
                            }
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

    public FloatMask subtract(FloatMask other) {
        checkSize(other);
        add(other.copy().multiply(-1));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtract(BinaryMask other, float value) {
        checkSize(other);
        FloatMask otherFloat = new FloatMask(getSize(), null, symmetrySettings);
        otherFloat.init(other, 0, -value);
        add(otherFloat);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtractWithOffset(FloatMask other, Vector2f loc, boolean center) {
        return addWithOffset(other.copy().multiply(-1f), loc, center);
    }

    public FloatMask subtractWithOffset(FloatMask other, int offsetX, int offsetY, boolean center) {
        return addWithOffset(other.copy().multiply(-1f), offsetX, offsetY, center);
    }

    public FloatMask multiply(FloatMask other) {
        checkSize(other);
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                multiplyValueAt(x, y, other.getValueAt(x, y));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask multiply(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                multiplyValueAt(x, y, val);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask multiplyWithOffset(FloatMask other, Vector2f loc, boolean centered) {
        return multiplyWithOffset(other, (int) loc.x, (int) loc.y, centered);
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
                        ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(shiftX, shiftY, SymmetryType.SPAWN);
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

    public FloatMask sqrt() {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                setValueAt(x, y, (float) StrictMath.sqrt(getValueAt(x, y)));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask max(FloatMask other) {
        checkSize(other);
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                setValueAt(x, y, StrictMath.max(getValueAt(x, y), other.getValueAt(x, y)));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask max(BinaryMask other, float val) {
        checkSize(other);
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (other.getValueAt(x, y)) {
                    setValueAt(x, y, StrictMath.min(getValueAt(x, y), val));
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask max(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                setValueAt(x, y, StrictMath.min(getValueAt(x, y), val));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask min(FloatMask other) {
        checkSize(other);
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                setValueAt(x, y, StrictMath.min(getValueAt(x, y), other.getValueAt(x, y)));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask min(BinaryMask other, float val) {
        checkSize(other);
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (other.getValueAt(x, y)) {
                    setValueAt(x, y, StrictMath.max(getValueAt(x, y), val));
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask min(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                setValueAt(x, y, StrictMath.max(getValueAt(x, y), val));
            }
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
                int xNeg = StrictMath.max(0, x - 1);
                int yNeg = StrictMath.max(0, y - 1);
                float xPosSlope = StrictMath.abs(getValueAt(x, y) - getValueAt(xPos, y));
                float yPosSlope = StrictMath.abs(getValueAt(x, y) - getValueAt(x, yPos));
                float xNegSlope = StrictMath.abs(getValueAt(x, y) - getValueAt(xNeg, y));
                float yNegSlope = StrictMath.abs(getValueAt(x, y) - getValueAt(x, yNeg));
                maskCopy[x][y] = Collections.max(Arrays.asList(xPosSlope, yPosSlope, xNegSlope, yNegSlope));
            }
        }
        mask = maskCopy;
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

    public FloatMask interpolate() {
        return smooth(1);
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
        checkSize(limiter);
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
        checkSize(limiter);
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

    public FloatMask setToZero(BinaryMask other) {
        checkSize(other);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (other.getValueAt(x, y)) {
                    setValueAt(x, y, 0f);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask zeroOutsideRange(float min, float max) {
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

    public FloatMask setValues(BinaryMask other, float val) {
        checkSize(other);
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (other.getValueAt(x, y)) {
                    setValueAt(x, y, val);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask replaceValues(BinaryMask other, FloatMask replacement) {
        if (other.getSize() != getSize() || replacement.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size: replacement is " + replacement.getSize() + ", other is " + other.getSize() + " and FloatMask is " + getSize());
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (other.getValueAt(x, y)) {
                    setValueAt(x, y, replacement.getValueAt(x, y));
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask zeroInRange(float min, float max) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (this.getValueAt(x, y) >= min && this.getValueAt(x, y) < max) {
                    setValueAt(x, y, 0f);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask convertToBinaryMask(float minValue, float maxValue) {
        BinaryMask newMask = new BinaryMask(this, minValue, maxValue, random.nextLong());
        VisualDebugger.visualizeMask(this);
        return newMask;
    }

    public FloatMask smoothWithinEdgeDistance(BinaryMask other, int edgeDistance) {
        checkSize(other);
        for (int x = 0; x < edgeDistance; x = x + 2) {
            replaceValues(other.getAreasWithinEdgeDistance(x + 1), copy().smooth(1));
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask reduceValuesOnIntersectingSmoothingZones(BinaryMask avoidMakingZonesHere, float floatMax) {
        checkSize(avoidMakingZonesHere);
        avoidMakingZonesHere = avoidMakingZonesHere.copy();
        FloatMask newMaskInZones = copy().smooth(34).subtract(copy()).subtract(avoidMakingZonesHere, 1f * floatMax);
        BinaryMask zones = newMaskInZones.copy().zeroInRange(0f * floatMax, 0.5f * floatMax).smooth(2).convertToBinaryMask(0.5f * floatMax, 1f * floatMax).inflate(34);
        BinaryMask newMaskInZonesBase = convertToBinaryMask(1f * floatMax, 1f * floatMax).deflate(3).minus(zones.copy().invert());
        newMaskInZones.init(newMaskInZonesBase, 0, 1).smooth(4).max(0.35f * floatMax).add(newMaskInZonesBase, 1f * floatMax).smooth(2).max(0.65f * floatMax).add(newMaskInZonesBase, 1f * floatMax).smooth(1).add(newMaskInZonesBase, 1f * floatMax).max(1f * floatMax);
        replaceValues(zones, newMaskInZones).smoothWithinEdgeDistance(zones, 30);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeAreasOutsideIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        FloatMask tempMask2 = copy().init(this.copy().convertToBinaryMask(minIntensity, maxIntensity).removeAreasOutsideRange(minSize, maxSize).invert(), 0f, 1f);
        this.subtract(tempMask2).min(0f);
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
            removeAreasInIntensityAndSize(minSize, maxSize, ((1f - (float) x / (float) levelOfPrecision) * floatMax), 1f * floatMax);
        }
        removeAreasInIntensityAndSize(minSize, maxSize, 0.0000001f, 1f * floatMax);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask getLocalMaximums(float minValue, float maxValue) {
        BinaryMask localMaxima = new BinaryMask(getSize(), random.nextLong(), symmetrySettings);
        for (int x = getMinXBound(SymmetryType.SPAWN); x < getMaxXBound(SymmetryType.SPAWN); x++) {
            for (int y = getMinYBound(x, SymmetryType.SPAWN); y < getMaxYBound(x, SymmetryType.SPAWN); y++) {
                float value = getValueAt(x, y);
                if (value >= minValue && value < maxValue && isLocalMax(x, y)) {
                    localMaxima.setValueAt(x, y, true);
                    ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(x, y, SymmetryType.SPAWN);
                    symmetryPoints.forEach(symmetryPoint -> localMaxima.setValueAt(symmetryPoint.getLocation(), true));
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

    public FloatMask getDistanceFieldForRange(float minValue, float maxValue) {
        convertToBinaryMask(minValue, maxValue).getDistanceField();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask useBrush(Vector2f location, String brushName, float intensity, int size) {
        FloatMask brush = loadBrush(brushName, random.nextLong());
        int numSymPoints = symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        boolean symmetric = numSymPoints % 2 == 0 && numSymPoints <= 4;
        brush.multiply(intensity / brush.getMax()).setSize(size);
        addWithOffset(brush, location, true);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask useBrushWithinArea(BinaryMask area, String brushName, int size, int numUses, float intensity) {
        checkSize(size);
        boolean symmetric = symmetrySettings.getSpawnSymmetry().isPerfectSymmetry();
        ArrayList<Vector2f> possibleLocations = new ArrayList<>(area.getAllCoordinatesEqualTo(true, 1));
        int length = possibleLocations.size();
        FloatMask brush = loadBrush(brushName, random.nextLong());
        brush.multiply(intensity / brush.getMax()).setSize(size);
        for (int i = 0; i < numUses; i++) {
            Vector2f location = possibleLocations.get(random.nextInt(length));
            addWithOffset(brush, location, true);
            if (!symmetric) {
                ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(location, SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> addWithOffset(brush, symmetryPoint.getLocation(), true));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask useBrushWithinAreatoroidally(BinaryMask area, String brushName, int size, int numUses, float intensity) {
        checkSize(size);
        boolean symmetric = symmetrySettings.getSpawnSymmetry().isPerfectSymmetry();
        ArrayList<Vector2f> possibleLocations = new ArrayList<>(area.getAllCoordinatesEqualTo(true, 1));
        int length = possibleLocations.size();
        FloatMask brush = loadBrush(brushName, random.nextLong());
        brush.multiply(intensity / brush.getMax()).setSize(size);
        for (int i = 0; i < numUses; i++) {
            Vector2f location = possibleLocations.get(random.nextInt(length));
            addWithOffsettoroidally(brush, location, true);
            if (!symmetric) {
                ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(location, SymmetryType.SPAWN);
                symmetryPoints.forEach(symmetryPoint -> addWithOffsettoroidally(brush, symmetryPoint.getLocation(), true));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask useBrushWithinAreaWithDensity(BinaryMask area, String brushName, int size, float density, float intensity) {
        int frequency = (int) (density * (float) area.getCount() / 26.21f / symmetrySettings.getSpawnSymmetry().getNumSymPoints());
        useBrushWithinArea(area, brushName, size, frequency, intensity);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask useBrushWithinAreaWithDensitytoroidally(BinaryMask area, String brushName, int size, float density, float intensity) {
        int frequency = (int) (density * (float) area.getCount() / 26.21f / symmetrySettings.getSpawnSymmetry().getNumSymPoints());
        useBrushWithinAreatoroidally(area, brushName, size, frequency, intensity);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public boolean areAnyEdgesGreaterThan(float value) {
        int size = getSize();
        int farEdge = size - 1;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y += farEdge) {
                if(getValueAt(x, y) > value || getValueAt(farEdge - x, farEdge - y) > value
                        || getValueAt(x, farEdge - y)  > value || getValueAt(farEdge - x, y) > value) {
                    return true;
                }
            }
        }
        return false;
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

    public void checkSize(int size) {
        if (size > getSize()) {
            throw new IllegalArgumentException("Intended mask size is larger than base mask size: FloatMask is " + getSize() + " and size is " + size);
        }
    }

    public void checkSize(Mask<?> other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size: other is " + other.getSize() + " and FloatMask is " + getSize());
        }
    }

    public String toHash() throws NoSuchAlgorithmException {
        ByteBuffer bytes = ByteBuffer.allocate(getSize() * getSize() * 4);
        for (int x = getMinXBound(SymmetryType.SPAWN); x < getMaxXBound(SymmetryType.SPAWN); x++) {
            for (int y = getMinYBound(x, SymmetryType.SPAWN); y < getMaxYBound(x, SymmetryType.SPAWN); y++) {
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

    public FloatMask startVisualDebugger() {
        return startVisualDebugger(toString(), Util.getStackTraceParentClass());
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
