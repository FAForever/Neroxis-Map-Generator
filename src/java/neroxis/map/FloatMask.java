package neroxis.map;

import lombok.Getter;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;
import neroxis.util.VisualDebugger;

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

import static neroxis.brushes.Brushes.loadBrush;

@Getter
public strictfp class FloatMask extends Mask<Float> {

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

    public FloatMask(FloatMask sourceMask, Long seed, String name) {
        super(seed, sourceMask.getSymmetrySettings(), name, sourceMask.isParallel());
        this.mask = getEmptyMask(sourceMask.getSize());
        this.plannedSize = sourceMask.getSize();
        setProcessing(sourceMask.isProcessing());
        execute(() -> {
            modify(sourceMask::getValueAt);
            VisualDebugger.visualizeMask(this);
        }, sourceMask);
    }

    public FloatMask(BinaryMask sourceMask, float low, float high, Long seed) {
        this(sourceMask, low, high, seed, null);
    }

    public FloatMask(BinaryMask sourceMask, float low, float high, Long seed, String name) {
        super(seed, sourceMask.getSymmetrySettings(), name, sourceMask.isParallel());
        this.mask = getEmptyMask(sourceMask.getSize());
        this.plannedSize = sourceMask.getSize();
        setProcessing(sourceMask.isProcessing());
        execute(() -> {
            modify((x, y) -> sourceMask.getValueAt(x, y) ? high : low);
            VisualDebugger.visualizeMask(this);
        }, sourceMask);
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
        plannedSize = other.getSize();
        execute(() -> {
            setSize(other.getSize());
            assertCompatibleMask(other);
            modify((x, y) -> other.getValueAt(x, y) ? high : low);
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    public FloatMask init(FloatMask other) {
        plannedSize = other.getSize();
        execute(() -> {
            setSize(other.getSize());
            assertCompatibleMask(other);
            modify(other::getValueAt);
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    @Override
    public FloatMask copy() {
        if (random != null) {
            return new FloatMask(this, random.nextLong(), getName() + "Copy");
        } else {
            return new FloatMask(this, null, getName() + "Copy");
        }
    }

    public FloatMask clear() {
        execute(() -> {
            modify((x, y) -> 0f);
            VisualDebugger.visualizeMask(this);
        });
        return this;
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

    public FloatMask addDistance(BinaryMask other, float scale) {
        execute(() -> {
            assertCompatibleMask(other);
            FloatMask distanceField = other.getDistanceField();
            add(distanceField.multiply(scale));
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    public FloatMask add(FloatMask other) {
        execute(() -> {
            assertCompatibleMask(other);
            add(other::getValueAt);
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    public FloatMask add(BinaryMask other, float value) {
        execute(() -> {
            assertCompatibleMask(other);
            add((x, y) -> other.getValueAt(x, y) ? value : 0);
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    public FloatMask add(float val) {
        execute(() -> {
            add((x, y) -> val);
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask addWeighted(FloatMask other, float weight) {
        execute(() -> {
            assertCompatibleMask(other);
            add((x, y) -> other.getValueAt(x, y) * weight);
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    private FloatMask addWithOffset(FloatMask other, Vector2f loc, boolean centered, boolean wrapEdges) {
        return addWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    private FloatMask addWithOffset(FloatMask other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
        execute(() -> {
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
        }, other);
        return this;
    }

    public FloatMask subtractAvg() {
        execute(() -> subtract(getAvg()));
        return this;
    }

    public FloatMask subtract(float val) {
        return add(-val);
    }

    public FloatMask subtract(FloatMask other) {
        execute(() -> {
            assertCompatibleMask(other);
            add((x, y) -> -other.getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    public FloatMask subtract(BinaryMask other, float value) {
        execute(() -> {
            assertCompatibleMask(other);
            add((x, y) -> other.getValueAt(x, y) ? -value : 0);
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    private FloatMask subtractWithOffset(FloatMask other, Vector2f loc, boolean center, boolean wrapEdges) {
        return addWithOffset(other.copy().multiply(-1f), loc, center, wrapEdges);
    }

    private FloatMask subtractWithOffset(FloatMask other, int offsetX, int offsetY, boolean center, boolean wrapEdges) {
        return addWithOffset(other.copy().multiply(-1f), offsetX, offsetY, center, wrapEdges);
    }

    public FloatMask multiply(FloatMask other) {
        execute(() -> {
            assertCompatibleMask(other);
            multiply(other::getValueAt);
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask multiply(float val) {
        execute(() -> {
            multiply((x, y) -> val);
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    private FloatMask multiplyWithOffset(FloatMask other, Vector2f loc, boolean centered, boolean wrapEdges) {
        return multiplyWithOffset(other, (int) loc.getX(), (int) loc.getY(), centered, wrapEdges);
    }

    private FloatMask multiplyWithOffset(FloatMask other, int xCoordinate, int yCoordinate, boolean center, boolean wrapEdges) {
        execute(() -> {
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

    public FloatMask max(FloatMask other) {
        execute(() -> {
            assertCompatibleMask(other);
            modify((x, y) -> StrictMath.max(getValueAt(x, y), other.getValueAt(x, y)));
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    public FloatMask clampMax(BinaryMask area, float val) {
        execute(() -> {
            assertCompatibleMask(area);
            modify((x, y) -> area.getValueAt(x, y) ? StrictMath.min(getValueAt(x, y), val) : getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
        }, area);
        return this;
    }

    public FloatMask clampMax(float val) {
        execute(() -> {
            modify((x, y) -> StrictMath.min(getValueAt(x, y), val));
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask min(FloatMask other) {
        execute(() -> {
            assertCompatibleMask(other);
            modify((x, y) -> StrictMath.min(getValueAt(x, y), other.getValueAt(x, y)));
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    public FloatMask clampMin(BinaryMask area, float val) {
        execute(() -> {
            assertCompatibleMask(area);
            modify((x, y) -> area.getValueAt(x, y) ? StrictMath.max(getValueAt(x, y), val) : getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
        }, area);
        return this;
    }

    public FloatMask clampMin(float val) {
        execute(() -> {
            modify((x, y) -> StrictMath.max(getValueAt(x, y), val));
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

    public FloatMask threshold(float val) {
        execute(() -> {
            modify((x, y) -> getValueAt(x, y) < val ? 0 : getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask interpolate() {
        return blur(1);
    }


    public FloatMask blur(int radius) {
        execute(() -> {
            int[][] innerCount = getInnerCount();
            modify((x, y) -> calculateAreaAverage(radius, x, y, innerCount) / 1000);
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask blur(int radius, BinaryMask limiter) {
        execute(() -> {
            assertCompatibleMask(limiter);
            int[][] innerCount = getInnerCount();
            modify((x, y) -> limiter.getValueAt(x, y) ? calculateAreaAverage(radius, x, y, innerCount) / 1000 : getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
        }, limiter);
        return this;
    }

    public FloatMask spike(int radius) {
        execute(() -> {
            int[][] innerCount = getInnerCount();
            modify((x, y) -> {
                float value = calculateAreaAverage(radius, x, y, innerCount) / 1000;
                return value * value;
            });
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask spike(int radius, BinaryMask limiter) {
        execute(() -> {
            assertCompatibleMask(limiter);
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
        }, limiter);
        return this;
    }

    public FloatMask zeroOutsideRange(float min, float max) {
        execute(() -> {
            modify((x, y) -> getValueAt(x, y) < min || getValueAt(x, y) > max ? 0 : getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public FloatMask setToValue(BinaryMask other, float val) {
        execute(() -> {
            assertCompatibleMask(other);
            modify((x, y) -> other.getValueAt(x, y) ? val : getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
        }, other);
        return this;
    }

    public FloatMask replaceValues(BinaryMask other, FloatMask replacement) {
        execute(() -> {
            assertCompatibleMask(other);
            assertCompatibleMask(replacement);
            modify((x, y) -> other.getValueAt(x, y) ? replacement.getValueAt(x, y) : getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
        }, other, replacement);
        return this;
    }

    public FloatMask zeroInRange(float min, float max) {
        execute(() -> {
            modify((x, y) -> getValueAt(x, y) >= min && getValueAt(x, y) < max ? 0 : getValueAt(x, y));
            VisualDebugger.visualizeMask(this);
        });
        return this;
    }

    public BinaryMask convertToBinaryMask(float minValue, float maxValue) {
        BinaryMask newMask = new BinaryMask(this, minValue, maxValue, random.nextLong(), getName() + "ToBinary");
        VisualDebugger.visualizeMask(this);
        return newMask;
    }

    public FloatMask removeAreasOutsideIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        execute(() -> {
            FloatMask tempMask2 = copy().init(this.copy().convertToBinaryMask(minIntensity, maxIntensity).removeAreasOutsideSizeRange(minSize, maxSize).invert(), 0f, 1f);
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

    public BinaryMask getLocalMaximums(float minValue, float maxValue) {
        BinaryMask localMaxima = new BinaryMask(getSize(), random.nextLong(), symmetrySettings, getName() + "localMaxima");
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
        BinaryMask localMaxima = new BinaryMask(getSize(), random.nextLong(), symmetrySettings, getName() + "local1DMaxima");
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
        int size = getSize();
        int[][] innerCount = new int[size][size];
        apply((x, y) -> calculateInnerValue(innerCount, x, y, StrictMath.round(getValueAt(x, y) * 1000)));
        return innerCount;
    }

    public FloatMask getDistanceFieldForRange(float minValue, float maxValue) {
        return convertToBinaryMask(minValue, maxValue).getDistanceField();
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

    public FloatMask useBrushWithinArea(BinaryMask area, String brushName, int size, int numUses, float intensity, boolean wrapEdges) {
        execute(() -> {
            assertSmallerSize(size);
            ArrayList<Vector2f> possibleLocations = new ArrayList<>(area.getAllCoordinatesEqualTo(true, 1));
            int length = possibleLocations.size();
            FloatMask brush = loadBrush(brushName, random.nextLong());
            brush.multiply(intensity / brush.getMax()).setSize(size);
            for (int i = 0; i < numUses; i++) {
                Vector2f location = possibleLocations.get(random.nextInt(length));
                addWithOffset(brush, location, true, wrapEdges);
            }
            VisualDebugger.visualizeMask(this);
        }, area);
        return this;
    }

    public FloatMask useBrushWithinAreaWithDensity(BinaryMask area, String brushName, int size, float density, float intensity, boolean wrapEdges) {
        execute(() -> {
            int frequency = (int) (density * (float) area.getCount() / 26.21f / symmetrySettings.getSpawnSymmetry().getNumSymPoints());
            useBrushWithinArea(area, brushName, size, frequency, intensity, wrapEdges);
            VisualDebugger.visualizeMask(this);
        }, area);
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

    public FloatMask mockClone() {
        return new FloatMask(this, 0L, MOCKED_NAME);
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
