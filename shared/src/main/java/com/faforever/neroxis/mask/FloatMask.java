package com.faforever.neroxis.mask;

import com.faforever.neroxis.brushes.Brushes;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.MathUtil;
import com.faforever.neroxis.util.functional.BiIntFloatConsumer;
import com.faforever.neroxis.util.functional.ToFloatBiIntFunction;
import com.faforever.neroxis.util.vector.Vector;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import org.jspecify.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import static com.faforever.neroxis.brushes.Brushes.loadBrush;
import static com.faforever.neroxis.util.ops.FloatArrayOpsHolder.OPS;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public final class FloatMask extends PrimitiveMask<Float, FloatMask> {
    /** Flat row-major storage indexed {@code x * immediateSize + y}. */
    private float[] mask;
    private int immediateSize;

    public FloatMask(int size, RandomGenerator.@Nullable SplittableGenerator random, SymmetrySettings symmetrySettings) {
        this(size, random, symmetrySettings, null);
    }

    /**
     * Create a new float mask
     *
     * @param size             Size of the mask
     * @param random           RandomGenerator of the mask
     * @param symmetrySettings symmetrySettings to enforce on the mask
     * @param name             name of the mask
     */
    public FloatMask(int size, RandomGenerator.@Nullable SplittableGenerator random, SymmetrySettings symmetrySettings,
                     @Nullable String name) {
        mask = new float[0];
        super(size, random, symmetrySettings, name);
    }

    public FloatMask(BufferedImage sourceImage, RandomGenerator.@Nullable SplittableGenerator random,
                     SymmetrySettings symmetrySettings) {
        this(sourceImage, random, symmetrySettings, 1f, null);
    }

    public FloatMask(BufferedImage sourceImage, RandomGenerator.@Nullable SplittableGenerator random,
                     SymmetrySettings symmetrySettings, float scaleFactor,
                     @Nullable String name) {
        this(sourceImage.getHeight(), random, symmetrySettings, name);
        DataBuffer imageBuffer = sourceImage.getRaster().getDataBuffer();
        int size = getSize();
        apply((x, y) -> setPrimitive(x, y, imageBuffer.getElemFloat(x + y * size) * scaleFactor));
    }

    public FloatMask(BufferedImage sourceImage, RandomGenerator.@Nullable SplittableGenerator random,
                     SymmetrySettings symmetrySettings, float scaleFactor) {
        this(sourceImage, random, symmetrySettings, scaleFactor, null);
    }

    FloatMask(FloatMask other) {
        this(other, null);
    }

    FloatMask(FloatMask other, @Nullable String name) {
        mask = new float[0];
        super(other, name);
    }

    FloatMask(BooleanMask other, float low, float high) {
        this(other, low, high, null);
    }

    FloatMask(BooleanMask other, float low, float high, @Nullable String name) {
        this(other.getSize(), other.getNextRandomGenerator(), other.getSymmetrySettings(), name);
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            apply((x, y) -> setPrimitive(x, y, source.getPrimitive(x, y) ? high : low));
        }, other);
    }

    private <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other1,
                                                                        VectorMask<T, U> other2) {
        this(other1, other2, null);
    }

    <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other1, VectorMask<T, U> other2,
                                                                @Nullable String name) {
        this(other1.getSize(), other1.getNextRandomGenerator(), other1.getSymmetrySettings(), name);
        assertCompatibleMask(other1);
        assertCompatibleMask(other2);
        enqueue(dependencies -> {
            U source1 = (U) dependencies.get(0);
            U source2 = (U) dependencies.get(1);
            apply((x, y) -> setPrimitive(x, y, source1.get(x, y).dot(source2.get(x, y))));
        }, other1, other2);
    }

    private <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other, T vector) {
        this(other, vector, null);
    }

    <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other, T vector,
                                                                @Nullable String name) {
        this(other.getSize(), other.getNextRandomGenerator(), other.getSymmetrySettings(), name);
        assertCompatibleMask(other);
        enqueue(dependencies -> {
            U source = (U) dependencies.getFirst();
            apply((x, y) -> setPrimitive(x, y, source.get(x, y).dot(vector)));
        }, other);
    }

    private <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other, int index) {
        this(other, index, null);
    }

    <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other, int index,
                                                                @Nullable String name) {
        this(other.getSize(), other.getNextRandomGenerator(), other.getSymmetrySettings(), name);
        assertCompatibleMask(other);
        enqueue(dependencies -> {
            U source = (U) dependencies.getFirst();
            apply((x, y) -> setPrimitive(x, y, source.get(x, y).get(index)));
        }, other);
    }

    void setPrimitive(int x, int y, float value) {
        mask[x * immediateSize + y] = value;
    }

    /**
     * Add perlin noise to the mask with the given resolution and noise scale
     *
     * @param resolution The effective resolution of the noise which is relative to the size of the mask. E.g. 1 is
     *                   the same size and 2 is half the size...
     * @param scale      Multiplicative factor to scale the noise by
     */
    public FloatMask addPerlinNoise(int resolution, float scale) {
        assert random != null;
        int size = getSize();
        int gradientSize = size / resolution;
        if (gradientSize <= 0) {
            System.err.println("FloatMask:addPerlinNoise(): resolution " +
                               resolution +
                               " can't be greater than mask size " +
                               size);
        }
        float gradientScale = (float) size / gradientSize;
        Vector2Mask gradientVectors = new Vector2Mask(gradientSize + 1, getNextRandomGenerator(),
                                                      new SymmetrySettings(Symmetry.NONE), getName() + "PerlinVectors");
        gradientVectors.randomize(-1f, 1f).normalize();
        FloatMask noise = new FloatMask(size, null, symmetrySettings, getName() + "PerlinNoise");
        noise.enqueue(dependencies -> {
            Vector2Mask source = (Vector2Mask) dependencies.getFirst();
            // Gradient vectors extracted to flat arrays so the per-pixel loop below allocates
            // nothing. The float expressions transliterate the previous Vector2.dot calls exactly
            // (including the leading 0f of dot's accumulator, which matters for signed zeros).
            int gradientCount = source.getSize();
            float[] gradientX = new float[gradientCount * gradientCount];
            float[] gradientY = new float[gradientCount * gradientCount];
            for (int x = 0; x < gradientCount; x++) {
                for (int y = 0; y < gradientCount; y++) {
                    Vector2 gradient = source.get(x, y);
                    gradientX[x * gradientCount + y] = gradient.x();
                    gradientY[x * gradientCount + y] = gradient.y();
                }
            }
            noise.setPrimitiveWithSymmetry(SymmetryType.SPAWN, (x, y) -> {
                int xLow = (int) (x / gradientScale);
                float dXLow = x / gradientScale - xLow;
                int xHigh = xLow + 1;
                float dXHigh = x / gradientScale - xHigh;
                int yLow = (int) (y / gradientScale);
                float dYLow = y / gradientScale - yLow;
                int yHigh = yLow + 1;
                float dYHigh = y / gradientScale - yHigh;
                int lowLow = xLow * gradientCount + yLow;
                int lowHigh = xLow * gradientCount + yHigh;
                int highLow = xHigh * gradientCount + yLow;
                int highHigh = xHigh * gradientCount + yHigh;
                float topLeft = 0f + dXLow * gradientX[lowLow] + dYLow * gradientY[lowLow];
                float topRight = 0f + dXLow * gradientX[lowHigh] + dYHigh * gradientY[lowHigh];
                float bottomLeft = 0f + dXHigh * gradientX[highLow] + dYLow * gradientY[highLow];
                float bottomRight = 0f + dXHigh * gradientX[highHigh] + dYHigh * gradientY[highHigh];
                return MathUtil.smootherStep(MathUtil.smootherStep(topLeft, bottomLeft, dXLow),
                                             MathUtil.smootherStep(topRight, bottomRight, dXLow), dYLow);
            });
            float noiseMin = noise.getMin();
            float noiseMax = noise.getMax();
            float noiseRange = noiseMax - noiseMin;
            float[] noiseMask = noise.mask;
            for (int i = 0; i < noiseMask.length; i++) {
                noiseMask[i] = (noiseMask[i] - noiseMin) / noiseRange * scale;
            }
        }, gradientVectors);
        return enqueue(dependencies -> add((FloatMask) dependencies.getFirst()), noise);
    }

    @Override
    public Float getMin() {
        checkNotPipelined();
        if (mask.length == 0) {
            throw new IllegalStateException("Empty Mask");
        }
        return OPS.min(mask, mask.length);
    }

    @Override
    public Float getMax() {
        checkNotPipelined();
        if (mask.length == 0) {
            throw new IllegalStateException("Empty Mask");
        }
        return OPS.max(mask, mask.length);
    }

    public float getPrimitive(int x, int y) {
        return mask[x * immediateSize + y];
    }

    private void setPrimitive(Vector2 location, float value) {
        setPrimitive(StrictMath.round(location.x()), StrictMath.round(location.y()), value);
    }

    Vector3 calculateNormalAt(int x, int y, float scale) {
        float xNormal, yNormal;
        xNormal = ((getPrimitive(x, y) - getPrimitive(x + 1, y)) +
                   (getPrimitive(x, y + 1) - getPrimitive(x + 1, y + 1))) * 0.5f * scale;
        yNormal = ((getPrimitive(x, y) - getPrimitive(x, y + 1)) +
                   (getPrimitive(x + 1, y) - getPrimitive(x + 1, y + 1))) * 0.5f * scale;
        return new Vector3(xNormal, 1, yNormal).normalize();
    }

    /**
     * Add gaussian noise to the mask with the given noise scale
     *
     * @param scale Multiplicative factor for the noise
     */
    public FloatMask addGaussianNoise(float scale) {
        assert random != null;
        return addPrimitiveWithSymmetry(SymmetryType.SPAWN, (x, y) -> (float) random.nextGaussian() * scale);
    }

    /**
     * Add white/uniform noise to the mask with the given noise scale
     *
     * @param scale Multiplicative factor for the noise
     */
    public FloatMask addWhiteNoise(float scale) {
        assert random != null;
        return addPrimitiveWithSymmetry(SymmetryType.SPAWN, (x, y) -> random.nextFloat() * scale);
    }

    /**
     * Add white/uniform noise to the mask between the given values
     *
     * @param minValue minimum value for the noise
     * @param maxValue maximum value for the noise
     */
    public FloatMask addWhiteNoise(float minValue, float maxValue) {
        assert random != null;
        float range = maxValue - minValue;
        return addPrimitiveWithSymmetry(SymmetryType.SPAWN, (x, y) -> random.nextFloat() * range + minValue);
    }

    public FloatMask waterErode(int numDrops, int maxIterations, float friction, float speed, float erosionRate,
                                float depositionRate, float maxOffset, float iterationScale) {
        assert random != null;
        int size = getSize();
        for (int i = 0; i < numDrops; ++i) {
            waterDrop(maxIterations, random.nextInt(size), random.nextInt(size), friction, speed, erosionRate,
                      depositionRate, maxOffset, iterationScale);
        }
        return forceSymmetry(SymmetryType.SPAWN);
    }

    private void waterDrop(int maxIterations, float x, float y, float friction, float gravity, float erosionRate,
                           float depositionRate, float maxOffset, float iterationScale) {
        assert random != null;
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
            Vector3 surfaceNormal = calculateNormalAt(sampleX, sampleY, 1f);

            // If the terrain is flat, stop simulating, the snowball cannot roll any further
            if (surfaceNormal.y() >= 1 && StrictMath.sqrt(xVelocity * xVelocity + yVelocity * yVelocity) < 1) {
                break;
            }

            // Calculate the deposition and erosion rate
            float deposit = sediment * depositionRate * surfaceNormal.y();
            float erosion = erosionRate * (1 - surfaceNormal.y()) * StrictMath.min(1, i * iterationScale);

            float sedimentChange = deposit - erosion;

            // Change the sediment on the place this snowball came from
            addValueAt((int) xPrev, (int) yPrev, sedimentChange);
            sediment -= sedimentChange;

            xVelocity = (1 - friction) * xVelocity + surfaceNormal.x() * gravity;
            yVelocity = (1 - friction) * yVelocity + surfaceNormal.z() * gravity;
            xPrev = x;
            yPrev = y;
            x += xVelocity;
            y += yVelocity;
        }
    }

    public FloatMask removeAreasOfSpecifiedSizeWithLocalMaximums(int minSize, int maxSize, int levelOfPrecision,
                                                                 float floatMax) {
        for (int x = 0; x < levelOfPrecision; x++) {
            removeAreasInIntensityAndSize(minSize, maxSize, ((1f - (float) x / (float) levelOfPrecision) * floatMax),
                                          floatMax);
        }
        removeAreasInIntensityAndSize(minSize, maxSize, 0.0000001f, floatMax);
        return this;
    }

    public FloatMask removeAreasInIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        return subtract(copy().removeAreasOutsideRangeAndSize(minSize, maxSize, minIntensity, maxIntensity));
    }

    public FloatMask removeAreasOutsideRangeAndSize(int minSize, int maxSize, float minValue, float maxValue) {
        FloatMask areasToRemove = copy().copyAsBooleanMask(minValue, maxValue)
                                        .removeAreasOutsideSizeRange(minSize, maxSize)
                                        .invert()
                                        .copyAsFloatMask(0f, 1f);
        return subtract(areasToRemove).clampMin(0f);
    }

    public FloatMask useBrush(Vector2 location, String brushName, float intensity, int size, boolean wrapEdges) {
        return enqueue(() -> {
            FloatMask brush = loadBrush(brushName);
            brush.multiply(intensity / brush.getMax()).setSize(size);
            addWithOffset(brush, location, true, wrapEdges);
        });
    }

    /**
     * Add the distances from the nearest true pixel in the given {@link BooleanMask} to this one.
     *
     * @param other boolean mask to compute distances from
     * @param scale multiplicative factor to apply before adding distances
     */
    public FloatMask addDistance(BooleanMask other, float scale) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            FloatMask distanceField = source.copyAsDistanceField();
            add(distanceField.multiply(scale));
        }, other);
    }

    /**
     * Convert each pixel to the 2D gradient/slope of the mask at that location
     */
    public FloatMask gradient() {
        return enqueue(() -> {
            int size = getSize();
            float[] newMask = new float[size * size];
            apply((x, y) -> {
                int xNeg = StrictMath.max(0, x - 1);
                int xPos = StrictMath.min(size - 1, x + 1);
                int yNeg = StrictMath.max(0, y - 1);
                int yPos = StrictMath.min(size - 1, y + 1);
                float xSlope = (getPrimitive(xPos, y) - getPrimitive(xNeg, y)) / (xPos - xNeg);
                float ySlope = (getPrimitive(x, yPos) - getPrimitive(x, yNeg)) / (yPos - yNeg);
                newMask[x * size + y] = (float) StrictMath.sqrt(xSlope * xSlope + ySlope * ySlope);
            });
            mask = newMask;
        });
    }

    /**
     * Convert each pixel to the 2D gradient/slope of the mask at that location using the gradient equation found in SCFA
     */
    public FloatMask supcomGradient() {
        return enqueue(() -> {
            int size = getSize();
            float[] newMask = new float[size * size];
            apply((x, y) -> {
                int xPos = StrictMath.min(size - 1, x + 1);
                int yPos = StrictMath.min(size - 1, y + 1);
                int xNeg = StrictMath.max(0, x - 1);
                int yNeg = StrictMath.max(0, y - 1);
                float xPosSlope = StrictMath.abs(getPrimitive(x, y) - getPrimitive(xPos, y));
                float yPosSlope = StrictMath.abs(getPrimitive(x, y) - getPrimitive(x, yPos));
                float xNegSlope = StrictMath.abs(getPrimitive(x, y) - getPrimitive(xNeg, y));
                float yNegSlope = StrictMath.abs(getPrimitive(x, y) - getPrimitive(x, yNeg));
                newMask[x * size + y] = Collections.max(List.of(xPosSlope, yPosSlope, xNegSlope, yNegSlope));
            });
            mask = newMask;
        });
    }

    public FloatMask useBrushWithinAreaWithDensity(BooleanMask other, String brushName, int size, float density,
                                                   float intensity, boolean wrapEdges) {
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            int frequency = (int) (density * (float) source.getCount() / 26.21f / symmetrySettings.spawnSymmetry()
                                                                                                  .getNumSymPoints());
            useBrushWithinArea(source, brushName, size, frequency, intensity, wrapEdges);
        }, other);
        return this;
    }

    public FloatMask useBrushWithinArea(BooleanMask other, String brushName, int size, int numUses, float intensity,
                                        boolean wrapEdges) {
        assert random != null;
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            assertSmallerSize(size);
            ArrayList<Vector2> possibleLocations = new ArrayList<>(source.getAllCoordinatesEqualTo(true, 1));
            int length = possibleLocations.size();
            FloatMask brush = loadBrush(brushName);
            brush.multiply(intensity / brush.getMax()).setSize(size);
            for (int i = 0; i < numUses; i++) {
                Vector2 location = possibleLocations.get(random.nextInt(length));
                addWithOffset(brush, location, true, wrapEdges);
            }
        }, other);
    }

    public FloatMask useBrushWithCliffMap(FloatMask other, int size) {
        assert random != null;
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            assertSmallerSize(size);
            applyWithSymmetry(SymmetryType.TERRAIN, (x, y) -> {
                float slope = source.get(x, y);
                if (slope >= 5) {
                    if (random.nextInt(100) <= 10) {
                        String brushName = Brushes.GENERATOR_BRUSHES.get(
                                random.nextInt(Brushes.GENERATOR_BRUSHES.size()));
                        FloatMask brush = loadBrush(brushName);
                        brush.setSize(size + ((int) ((slope + 1) * 4)));
                        brush.multiply(0.1f);
                        addWithOffset(brush, new Vector2(x, y), true, false);
                    }
                }
            });
        }, other);
    }

    public BooleanMask copyAsShadowMask(Vector3 lightDirection) {
        float angle = (float) ((lightDirection.getAzimuth() - StrictMath.PI) % (StrictMath.PI * 2));
        float slope = (float) StrictMath.tan(lightDirection.getElevation());
        BooleanMask shadowMask = new BooleanMask(getSize(), getNextRandomGenerator(),
                                                 new SymmetrySettings(Symmetry.NONE),
                                                 getName() + "Shadow");
        return shadowMask.enqueue(dependencies -> shadowMask.apply((x, y) -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            Vector2 location = new Vector2(x, y);
            if (shadowMask.getPrimitive(location)) {
                return;
            }
            float startHeight = source.getPrimitive(location);
            int dist = 1;
            location = location.addPolar(angle, 1);
            while (source.inBounds(location)) {
                if (startHeight - source.getPrimitive(location) > dist * slope) {
                    shadowMask.setPrimitive(location, true);
                } else {
                    break;
                }
                location = location.addPolar(angle, 1);
                ++dist;
            }
        }), this).inflate(1).deflate(1);
    }

    public float getPrimitive(Vector2 location) {
        return getPrimitive(StrictMath.round(location.x()), StrictMath.round(location.y()));
    }

    /**
     * Return a normal mask whose values are the normal vector of this mask at each pixel
     * (Note the returned normal mask will have a {@link Symmetry#NONE} symmetry
     *
     * @return a new normal mask
     */
    public NormalMask copyAsNormalMask() {
        return copyAsNormalMask(1f);
    }

    /**
     * Return a normal mask whose values are the normal vector of this mask at each pixel
     * (Note the returned normal mask will have a {@link Symmetry#NONE} symmetry
     *
     * @param scale multiplicative factor for each normal vector
     * @return a new normal mask
     */
    public NormalMask copyAsNormalMask(float scale) {
        return new NormalMask(this, scale, getName() + "Normals");
    }

    public BufferedImage writeToImage(BufferedImage image, float scaleFactor) {
        return writeToImage(image, scaleFactor, 0f);
    }

    public BufferedImage writeToImage(BufferedImage image, float scaleFactor, float offsetFactor) {
        assertSize(image.getHeight());
        int size = getSize();
        DataBuffer imageBuffer = image.getRaster().getDataBuffer();
        loop((x, y) -> imageBuffer.setElemFloat(x + y * size, (getPrimitive(x, y) - offsetFactor) * scaleFactor));
        return image;
    }

    public FloatMask parabolicMinimization() {
        return enqueue(() -> {
            addCalculatedParabolicDistance(false);
            addCalculatedParabolicDistance(true);
            sqrt();
        });
    }

    private void addCalculatedParabolicDistance(boolean useColumns) {
        checkNotPipelined();
        int size = getSize();
        // Felzenszwalb lower-envelope scan with flat scratch arrays reused across rows (the float
        // expressions match the previous Vector2-based implementation exactly; vertex x values are
        // small integers, so int-to-float promotion is lossless)
        float[] vertexX = new float[size];
        float[] vertexY = new float[size];
        float[] intersectionX = new float[size + 1];
        for (int i = 0; i < size; i++) {
            int index = 0;
            float value;
            if (!useColumns) {
                value = getPrimitive(i, 0);
            } else {
                value = getPrimitive(0, i);
            }
            vertexX[0] = 0;
            vertexY[0] = value;
            intersectionX[0] = Float.NEGATIVE_INFINITY;
            intersectionX[1] = Float.POSITIVE_INFINITY;
            for (int j = 1; j < size; j++) {
                if (!useColumns) {
                    value = getPrimitive(i, j);
                } else {
                    value = getPrimitive(j, i);
                }
                float currentX = j;
                float xIntersect = ((value + currentX * currentX) - (vertexY[index] + vertexX[index]
                                                                                      * vertexX[index]))
                                   / (2 * currentX - 2 * vertexX[index]);
                while (xIntersect <= intersectionX[index]) {
                    index -= 1;
                    xIntersect = ((value + currentX * currentX) - (vertexY[index] + vertexX[index]
                                                                                    * vertexX[index]))
                                 / (2 * currentX - 2 * vertexX[index]);
                }
                index += 1;
                vertexX[index] = currentX;
                vertexY[index] = value;
                intersectionX[index] = xIntersect;
                intersectionX[index + 1] = Float.POSITIVE_INFINITY;
            }
            index = 0;
            for (int j = 0; j < size; j++) {
                while (intersectionX[index + 1] < j) {
                    index += 1;
                }
                float dx = j - vertexX[index];
                float height = dx * dx + vertexY[index];
                if (!useColumns) {
                    setPrimitive(i, j, height);
                } else {
                    setPrimitive(j, i, height);
                }
            }
        }
    }

    /**
     * Take the square root at every pixel
     */
    public FloatMask sqrt() {
        return enqueue(() -> OPS.sqrt(mask, mask.length));
    }

    @Override
    public FloatMask blur(int radius) {
        return enqueue(() -> {
            int size = getSize();
            int stride = size + 1;
            int[] innerCount = getInnerCount();
            int diameter = 2 * radius + 1;
            // For y in [radius, size - 1 - radius] neither y-clamp engages, so the filter height —
            // and with it the divisor area — is constant along the row and the whole span can go
            // through the bulk SAT lookup. The remaining y values fall back to the per-pixel path.
            int yStart = StrictMath.min(radius, size);
            int yEndExclusive = StrictMath.max(size - radius, yStart);
            int interiorLength = yEndExclusive - yStart;
            for (int x = 0; x < size; x++) {
                int xLeft = StrictMath.max(0, x - radius);
                int xRight = StrictMath.min(size - 1, x + radius);
                for (int y = 0; y < yStart; y++) {
                    setPrimitive(x, y, transformAverage(calculateAreaAverageAsInts(radius, x, y, innerCount)));
                }
                for (int y = yEndExclusive; y < size; y++) {
                    setPrimitive(x, y, transformAverage(calculateAreaAverageAsInts(radius, x, y, innerCount)));
                }
                if (interiorLength > 0) {
                    float area = (xRight - xLeft + 1) * diameter;
                    int aOffset = xLeft * stride;
                    int bOffset = (xRight + 1) * stride;
                    OPS.satBoxBlurRow(innerCount, aOffset, bOffset, aOffset + diameter, bOffset + diameter,
                                      area, 1000f, mask, x * size + yStart, interiorLength);
                }
            }
        });
    }

    @Override
    public FloatMask blur(int radius, BooleanMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask limiter = (BooleanMask) dependencies.getFirst();
            int[] innerCount = getInnerCount();
            apply((x, y) -> {
                if (limiter.get(x, y)) {
                    setPrimitive(x, y, transformAverage(calculateAreaAverageAsInts(radius, x, y, innerCount)));
                }
            });
        }, other);
    }

    @Override
    protected FloatMask copyFrom(FloatMask other) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            fill(source.mask, source.immediateSize);
        }, other);
    }

    @Override
    protected void initializeMask(int size) {
        enqueue(() -> {
            mask = new float[size * size];
            immediateSize = size;
        });
    }

    @Override
    protected int getImmediateSize() {
        return immediateSize;
    }

    @Override
    public BufferedImage writeToImage(BufferedImage image) {
        return writeToImage(image, 1f, 0f);
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
    public String toHash() throws NoSuchAlgorithmException {
        int size = getSize();
        ByteBuffer bytes = ByteBuffer.allocate(size * size * 4);
        loopInSymmetryRegion(SymmetryType.SPAWN, (x, y) -> bytes.putFloat(getPrimitive(x, y)));
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        return HexFormat.of().formatHex(data);
    }

    @Override
    public Float get(int x, int y) {
        return getPrimitive(x, y);
    }

    @Override
    protected void set(int x, int y, Float value) {
        setPrimitive(x, y, value);
    }

    @Override
    protected FloatMask fill(Float value) {
        return enqueue(() -> Arrays.fill(mask, value));
    }

    @Override
    protected Float getZeroValue() {
        return 0f;
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
                float[] oldMask = mask;
                int oldImmediateSize = immediateSize;
                initializeMask(newSize);
                Map<Integer, Integer> coordinateMap = getSymmetricScalingCoordinateMap(oldSize, newSize);
                applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> {
                    @SuppressWarnings("NullAway")
                    int newX = coordinateMap.get(x);
                    @SuppressWarnings("NullAway")
                    int newY = coordinateMap.get(y);
                    float value = oldMask[newX * oldImmediateSize + newY];
                    applyAtSymmetryPoints(x, y, SymmetryType.SPAWN, (sx, sy) -> setPrimitive(sx, sy, value));
                });
            }
        });
    }

    public FloatMask scaleToNewMinAndMaxHeight(float newMin, float newMax) {
        return enqueue(() -> {
            float oldMin = getMin();
            float oldMax = getMax();
            float scale = (oldMin == oldMax) ? 1f : (newMax - newMin) / (oldMax - oldMin);
            OPS.subtractMultiplyAdd(mask, oldMin, scale, newMin, mask.length);
        });
    }

    public FloatMask scaleExponentially(float exp) {
        return enqueue(() -> apply((x, y) -> {
            float oldValue = getPrimitive(x, y);
            float newValue = (float) StrictMath.pow(oldValue, exp);
            setPrimitive(x, y, newValue);
        }));
    }

    public FloatMask shiftToPositive() {
        return enqueue(() -> {
            float min = getMin();
            if (min < 0) {
                subtract(min);
            }
        });
    }

    private FloatMask fill(float[] maskToFillFrom, int maskSize) {
        mask = Arrays.copyOf(maskToFillFrom, maskToFillFrom.length);
        immediateSize = maskSize;
        return this;
    }

    private float transformAverage(float value) {
        return value / 1000f;
    }

    @Override
    protected int[] getInnerCount() {
        int size = getSize();
        int stride = size + 1;
        int[] innerCount = new int[stride * stride];
        for (int x = 0; x < size; x++) {
            OPS.roundScaled(mask, x * size, innerCount, (x + 1) * stride + 1, 1000f, size);
        }
        prefixSum2DPadded(innerCount, size);
        return innerCount;
    }

    private FloatMask add(ToFloatBiIntFunction valueFunction) {
        return apply((x, y) -> addPrimitiveAt(x, y, valueFunction.apply(x, y)));
    }

    private FloatMask subtract(ToFloatBiIntFunction valueFunction) {
        return apply((x, y) -> subtractPrimitiveAt(x, y, valueFunction.apply(x, y)));
    }

    private FloatMask multiply(ToFloatBiIntFunction valueFunction) {
        return apply((x, y) -> multiplyPrimitiveAt(x, y, valueFunction.apply(x, y)));
    }

    private FloatMask divide(ToFloatBiIntFunction valueFunction) {
        return apply((x, y) -> dividePrimitiveAt(x, y, valueFunction.apply(x, y)));
    }

    private void multiplyPrimitiveAt(int x, int y, float value) {
        mask[x * immediateSize + y] *= value;
    }

    private void addPrimitiveAt(int x, int y, float value) {
        mask[x * immediateSize + y] += value;
    }

    private void subtractPrimitiveAt(int x, int y, float value) {
        mask[x * immediateSize + y] -= value;
    }

    private void dividePrimitiveAt(int x, int y, float value) {
        mask[x * immediateSize + y] /= value;
    }

    @Override
    public Float getSum() {
        // Deliberately kept as a sequential stream: DoubleStream.sum() uses compensated summation
        // in a fixed order, and float summation is non-associative — any reordering (including a
        // SIMD reduction) would change map content between machines. See FloatArrayOps.
        return (float) IntStream.range(0, mask.length)
                                .mapToDouble(i -> mask[i])
                                .sum();
    }

    public @Nullable Vector2 getRandomPosition() {
        assert random != null;
        checkNotPipelined();
        float min = getMin();
        if (min < 0) {
            throw new IllegalArgumentException("Cannot get random position from a mask with negative values");
        }
        int size = getSize();
        float total = getSum();
        if (total == 0) {
            return null;
        }
        float sum = random.nextFloat(total);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if ((sum -= getPrimitive(x, y)) <= 0) {
                    return new Vector2(x, y);
                }
            }
        }
        throw new IllegalArgumentException("Did not find a coordinate");
    }

    public FloatMask setWithOffset(FloatMask other, int xOffset, int yOffset, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            applyWithOffset(source, (BiIntFloatConsumer) this::setPrimitive, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    @Override
    public FloatMask add(FloatMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            OPS.add(mask, source.mask, mask.length);
        }, other);
    }

    @Override
    public FloatMask add(Float val) {
        float value = val;
        return enqueue(() -> OPS.add(mask, value, mask.length));
    }

    @Override
    protected void addValueAt(int x, int y, Float value) {
        mask[x * immediateSize + y] += value;
    }

    @Override
    public FloatMask add(BooleanMask other, Float value) {
        assertCompatibleMask(other);
        float val = value;
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    addPrimitiveAt(x, y, val);
                }
            });
        }, other);
    }

    @Override
    public FloatMask add(BooleanMask other, FloatMask values) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            FloatMask val = (FloatMask) dependencies.get(1);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    addPrimitiveAt(x, y, val.getPrimitive(x, y));
                }
            });
        }, other, values);
    }

    @Override
    public FloatMask addWithOffset(FloatMask other, int xOffset, int yOffset, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            applyWithOffset(source, (BiIntFloatConsumer) this::addPrimitiveAt, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    @Override
    protected void subtractValueAt(int x, int y, Float value) {
        mask[x * immediateSize + y] -= value;
    }

    @Override
    public Float getAvg() {
        checkNotPipelined();
        int size = getSize();
        return getSum() / size / size;
    }

    @Override
    public FloatMask subtract(FloatMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            OPS.subtract(mask, source.mask, mask.length);
        }, other);
    }

    @Override
    public FloatMask subtract(Float val) {
        float value = val;
        return enqueue(() -> OPS.subtract(mask, value, mask.length));
    }

    @Override
    public FloatMask subtract(BooleanMask other, Float value) {
        assertCompatibleMask(other);
        float val = value;
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    subtractPrimitiveAt(x, y, val);
                }
            });
        }, other);
    }

    @Override
    public FloatMask subtract(BooleanMask other, FloatMask value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            FloatMask val = (FloatMask) dependencies.get(1);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    subtractPrimitiveAt(x, y, val.getPrimitive(x, y));
                }
            });
        }, other, value);
    }

    @Override
    public FloatMask subtractWithOffset(FloatMask other, int xOffset, int yOffset, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            applyWithOffset(source, (BiIntFloatConsumer) this::subtractPrimitiveAt, xOffset, yOffset, center,
                            wrapEdges);
        }, other);
    }

    @Override
    public FloatMask multiply(FloatMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            OPS.multiply(mask, source.mask, mask.length);
        }, other);
    }

    @Override
    public FloatMask multiply(Float val) {
        float value = val;
        return enqueue(() -> OPS.multiply(mask, value, mask.length));
    }

    @Override
    protected void multiplyValueAt(int x, int y, Float value) {
        mask[x * immediateSize + y] *= value;
    }

    @Override
    public FloatMask multiply(BooleanMask other, Float value) {
        assertCompatibleMask(other);
        float val = value;
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    multiplyPrimitiveAt(x, y, val);
                }
            });
        }, other);
    }

    @Override
    public FloatMask multiply(BooleanMask other, FloatMask value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            FloatMask val = (FloatMask) dependencies.get(1);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    multiplyPrimitiveAt(x, y, val.getPrimitive(x, y));
                }
            });
        }, other, value);
    }

    @Override
    public FloatMask multiplyWithOffset(FloatMask other, int xOffset, int yOffset, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            applyWithOffset(source, (BiIntFloatConsumer) this::multiplyPrimitiveAt, xOffset, yOffset, center,
                            wrapEdges);
        }, other);
    }

    @Override
    public FloatMask divide(FloatMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            OPS.divide(mask, source.mask, mask.length);
        }, other);
    }

    @Override
    public FloatMask divide(Float val) {
        float value = val;
        return enqueue(() -> OPS.divide(mask, value, mask.length));
    }

    @Override
    protected void divideValueAt(int x, int y, Float value) {
        mask[x * immediateSize + y] /= value;
    }

    @Override
    public FloatMask divide(BooleanMask other, Float value) {
        assertCompatibleMask(other);
        float val = value;
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    dividePrimitiveAt(x, y, val);
                }
            });
        }, other);
    }

    @Override
    public FloatMask divide(BooleanMask other, FloatMask value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            FloatMask val = (FloatMask) dependencies.get(1);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    dividePrimitiveAt(x, y, val.getPrimitive(x, y));
                }
            });
        }, other, value);
    }

    @Override
    public FloatMask divideWithOffset(FloatMask other, int xOffset, int yOffset, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            applyWithOffset(source, (BiIntFloatConsumer) this::dividePrimitiveAt, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    @Override
    public FloatMask clampMin(Float val) {
        float value = val;
        return enqueue(() -> OPS.clampMin(mask, value, mask.length));
    }

    @Override
    public FloatMask clampMax(Float val) {
        float value = val;
        return enqueue(() -> OPS.clampMax(mask, value, mask.length));
    }

    public FloatMask setPrimitiveWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> setPrimitive(sx, sy, value));
        });
    }

    public FloatMask addPrimitiveWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> addPrimitiveAt(sx, sy, value));
        });
    }

    public FloatMask subtractPrimitiveWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> subtractPrimitiveAt(sx, sy, value));
        });
    }

    public FloatMask multiplyPrimitiveWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> multiplyPrimitiveAt(sx, sy, value));
        });
    }

    public FloatMask dividePrimitiveWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> dividePrimitiveAt(sx, sy, value));
        });
    }

    private FloatMask applyWithOffset(FloatMask other, BiIntFloatConsumer action, int xOffset, int yOffset,
                                      boolean center, boolean wrapEdges) {
        return enqueue(() -> {
            int size = getSize();
            int otherSize = other.getSize();
            int smallerSize = StrictMath.min(size, otherSize);
            int biggerSize = StrictMath.max(size, otherSize);
            if (smallerSize == otherSize) {
                if (symmetrySettings.spawnSymmetry().isPerfectSymmetry()) {
                    Map<Integer, Integer> coordinateXMap = getShiftedCoordinateMap(xOffset, center, wrapEdges,
                                                                                   otherSize, size);
                    Map<Integer, Integer> coordinateYMap = getShiftedCoordinateMap(yOffset, center, wrapEdges,
                                                                                   otherSize, size);
                    other.apply((x, y) -> {
                        @SuppressWarnings("NullAway")
                        int shiftX = coordinateXMap.get(x);
                        @SuppressWarnings("NullAway")
                        int shiftY = coordinateYMap.get(y);
                        if (inBounds(shiftX, shiftY, size)) {
                            float value = other.getPrimitive(x, y);
                            applyAtSymmetryPoints(shiftX, shiftY, SymmetryType.SPAWN,
                                                  (sx, sy) -> action.accept(sx, sy, value));
                        }
                    });
                } else {
                    applyAtSymmetryPointsWithOutOfBounds(xOffset, yOffset, SymmetryType.SPAWN, (sx, sy) -> {
                        Map<Integer, Integer> coordinateXMap = getShiftedCoordinateMap(sx, center, wrapEdges, otherSize,
                                                                                       size);
                        Map<Integer, Integer> coordinateYMap = getShiftedCoordinateMap(sy, center, wrapEdges, otherSize,
                                                                                       size);
                        other.apply((x, y) -> {
                            @SuppressWarnings("NullAway")
                            int shiftX = coordinateXMap.get(x);
                            @SuppressWarnings("NullAway")
                            int shiftY = coordinateYMap.get(y);
                            if (inBounds(shiftX, shiftY, size)) {
                                action.accept(shiftX, shiftY, other.getPrimitive(x, y));
                            }
                        });
                    });
                }
            } else {
                Map<Integer, Integer> coordinateXMap = getShiftedCoordinateMap(xOffset, center, wrapEdges, size,
                                                                               otherSize);
                Map<Integer, Integer> coordinateYMap = getShiftedCoordinateMap(yOffset, center, wrapEdges, size,
                                                                               otherSize);
                apply((x, y) -> {
                    @SuppressWarnings("NullAway")
                    int shiftX = coordinateXMap.get(x);
                    @SuppressWarnings("NullAway")
                    int shiftY = coordinateYMap.get(y);
                    if (inBounds(shiftX, shiftY, otherSize)) {
                        action.accept(x, y, other.getPrimitive(shiftX, shiftY));
                    }
                });
            }
        });
    }
}
