package com.faforever.neroxis.map.mask;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.Vector;
import com.faforever.neroxis.util.Vector2;
import com.faforever.neroxis.util.Vector3;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.faforever.neroxis.brushes.Brushes.loadBrush;

@SuppressWarnings("unchecked")
public strictfp class FloatMask extends PrimitiveMask<Float, FloatMask> {

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
        super(sourceImage.getHeight(), seed, symmetrySettings, name, parallel);
        DataBuffer imageBuffer = sourceImage.getRaster().getDataBuffer();
        int size = getSize();
        enqueue(() -> set((x, y) -> imageBuffer.getElemFloat(x + y * size) * scaleFactor));
    }

    public FloatMask(FloatMask other, Long seed) {
        super(other, seed);
    }

    public FloatMask(FloatMask other, Long seed, String name) {
        super(other, seed, name);
    }

    public FloatMask(BooleanMask other, float low, float high, Long seed) {
        this(other, low, high, seed, null);
    }

    public FloatMask(BooleanMask other, float low, float high, Long seed, String name) {
        super(other.getSize(), seed, other.getSymmetrySettings(), name, other.isParallel());
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            set((x, y) -> source.get(x, y) ? high : low);
        }, other);
    }

    public <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other1, VectorMask<T, U> other2, Long seed) {
        this(other1, other2, seed, null);
    }

    public <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other1, VectorMask<T, U> other2, Long seed, String name) {
        super(other1.getSize(), seed, other1.getSymmetrySettings(), name, other1.isParallel());
        assertCompatibleMask(other1);
        assertCompatibleMask(other2);
        enqueue((dependencies) -> {
            U source1 = (U) dependencies.get(0);
            U source2 = (U) dependencies.get(1);
            set((x, y) -> source1.get(x, y).dot(source2.get(x, y)));
        }, other1, other2);
    }

    public <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other, T vector, Long seed) {
        this(other, vector, seed, null);
    }

    public <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other, T vector, Long seed, String name) {
        super(other.getSize(), seed, other.getSymmetrySettings(), name, other.isParallel());
        assertCompatibleMask(other);
        enqueue((dependencies) -> {
            U source = (U) dependencies.get(0);
            set((x, y) -> source.get(x, y).dot(vector));
        }, other);
    }

    public <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other, int index, Long seed) {
        this(other, index, seed, null);
    }

    public <T extends Vector<T>, U extends VectorMask<T, U>> FloatMask(VectorMask<T, U> other, int index, Long seed, String name) {
        super(other.getSize(), seed, other.getSymmetrySettings(), name, other.isParallel());
        assertCompatibleMask(other);
        enqueue((dependencies) -> {
            U source = (U) dependencies.get(0);
            set((x, y) -> source.get(x, y).get(index));
        }, other);
    }

    @Override
    protected Float[][] getEmptyMask(int size) {
        Float[][] empty = new Float[size][size];
        maskFill(empty, getZeroValue());
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
    public FloatMask mock() {
        return new FloatMask(this, null, getName() + Mask.MOCK_NAME);
    }

    @Override
    public Float getZeroValue() {
        return 0f;
    }

    @Override
    public void addValueAt(int x, int y, Float value) {
        mask[x][y] += value;
    }

    @Override
    public void subtractValueAt(int x, int y, Float value) {
        mask[x][y] -= value;
    }

    @Override
    public void multiplyValueAt(int x, int y, Float value) {
        mask[x][y] *= value;
    }

    @Override
    public void divideValueAt(int x, int y, Float value) {
        mask[x][y] /= value;
    }

    public Vector3 getNormalAt(int x, int y) {
        return getNormalAt(x, y, 1f);
    }

    public Vector3 getNormalAt(int x, int y, float scale) {
        if (onBoundary(x, y) || !inBounds(x, y)) {
            return new Vector3(0, 1, 0);
        }
        return new Vector3(
                (get(x - 1, y) - get(x + 1, y)) * scale / 2f,
                1,
                (get(x, y - 1) - get(x, y + 1)) * scale / 2f
        ).normalize();
    }

    @Override
    public Float getSum() {
        return Arrays.stream(mask).flatMap(Arrays::stream).reduce(Float::sum).orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    public FloatMask addGaussianNoise(float scale) {
        enqueue(() -> {
            addWithSymmetry(SymmetryType.SPAWN, (x, y) -> (float) random.nextGaussian() * scale);
        });
        return this;
    }

    public FloatMask addWhiteNoise(float scale) {
        enqueue(() -> addWithSymmetry(SymmetryType.SPAWN, (x, y) -> random.nextFloat() * scale));
        return this;
    }

    public FloatMask addDistance(BooleanMask other, float scale) {
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            assertCompatibleMask(source);
            FloatMask distanceField = source.getDistanceField();
            add(distanceField.multiply(scale));
        }, other);
        return this;
    }

    public FloatMask sqrt() {
        enqueue(() -> set((x, y) -> (float) StrictMath.sqrt(get(x, y))));
        return this;
    }

    public FloatMask gradient() {
        enqueue(() -> {
            int size = getSize();
            Float[][] maskCopy = getEmptyMask(size);
            apply((x, y) -> {
                int xNeg = StrictMath.max(0, x - 1);
                int xPos = StrictMath.min(size - 1, x + 1);
                int yNeg = StrictMath.max(0, y - 1);
                int yPos = StrictMath.min(size - 1, y + 1);
                float xSlope = (get(xPos, y) - get(xNeg, y)) / (xPos - xNeg);
                float ySlope = (get(x, yPos) - get(x, yNeg)) / (yPos - yNeg);
                maskCopy[x][y] = (float) StrictMath.sqrt(xSlope * xSlope + ySlope * ySlope);
            });
            mask = maskCopy;
        });
        return this;
    }

    public FloatMask supcomGradient() {
        enqueue(() -> {
            int size = getSize();
            Float[][] maskCopy = getEmptyMask(size);
            apply((x, y) -> {
                int xPos = StrictMath.min(size - 1, x + 1);
                int yPos = StrictMath.min(size - 1, y + 1);
                int xNeg = StrictMath.max(0, x - 1);
                int yNeg = StrictMath.max(0, y - 1);
                float xPosSlope = StrictMath.abs(get(x, y) - get(xPos, y));
                float yPosSlope = StrictMath.abs(get(x, y) - get(x, yPos));
                float xNegSlope = StrictMath.abs(get(x, y) - get(xNeg, y));
                float yNegSlope = StrictMath.abs(get(x, y) - get(x, yNeg));
                maskCopy[x][y] = Collections.max(Arrays.asList(xPosSlope, yPosSlope, xNegSlope, yNegSlope));
            });
            mask = maskCopy;
        });
        return this;
    }

    public FloatMask waterErode(int numDrops, int maxIterations, float friction, float speed, float erosionRate,
                                float depositionRate, float maxOffset, float iterationScale) {
        enqueue(() -> {
            int size = getSize();
            Vector3Mask normalVectorMask = getNormalMask(10);
            for (int i = 0; i < numDrops; ++i) {
                waterDrop(normalVectorMask, maxIterations, random.nextInt(size), random.nextInt(size), friction, speed, erosionRate, depositionRate, maxOffset, iterationScale);
            }
            applySymmetry(SymmetryType.SPAWN);
        });
        return this;
    }

    public void waterDrop(Vector3Mask normalMask, int maxIterations, float x, float y, float friction, float speed, float erosionRate,
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
            Vector3 surfaceNormal = normalMask.get(sampleX, sampleY);
//            Vector3f surfaceNormal = getNormalAt((int) x, (int) y, 100);

            // If the terrain is flat, stop simulating, the snowball cannot roll any further
            if (surfaceNormal.getY() >= 1 && StrictMath.sqrt(xVelocity * xVelocity + yVelocity * yVelocity) < 1) {
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

    public Float transformAverage(float value) {
        return value / 1000f;
    }

    public FloatMask removeAreasOutsideIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        enqueue(() -> {
            FloatMask tempMask2 = copy().init(this.copy().convertToBooleanMask(minIntensity, maxIntensity).removeAreasOutsideSizeRange(minSize, maxSize).invert(), 0f, 1f);
            this.subtract(tempMask2).clampMin(0f);
        });
        return this;
    }

    public FloatMask removeAreasInIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        enqueue(() -> {
            subtract(this.copy().removeAreasOutsideIntensityAndSize(minSize, maxSize, minIntensity, maxIntensity));
        });
        return this;
    }

    public FloatMask removeAreasOfSpecifiedSizeWithLocalMaximums(int minSize, int maxSize, int levelOfPrecision, float floatMax) {
        enqueue(() -> {
            for (int x = 0; x < levelOfPrecision; x++) {
                removeAreasInIntensityAndSize(minSize, maxSize, ((1f - (float) x / (float) levelOfPrecision) * floatMax), floatMax);
            }
            removeAreasInIntensityAndSize(minSize, maxSize, 0.0000001f, floatMax);
        });
        return this;
    }

    @Override
    protected int[][] getInnerCount() {
        int size = getSize();
        int[][] innerCount = new int[size][size];
        apply((x, y) -> calculateInnerValue(innerCount, x, y, StrictMath.round(get(x, y) * 1000)));
        return innerCount;
    }

    public FloatMask getDistanceFieldForRange(float minValue, float maxValue) {
        return convertToBooleanMask(minValue, maxValue).getDistanceField();
    }

    public FloatMask useBrush(Vector2 location, String brushName, float intensity, int size, boolean wrapEdges) {
        enqueue(() -> {
            FloatMask brush = loadBrush(brushName, random.nextLong());
            brush.multiply(intensity / brush.getMax()).setSize(size);
            addWithOffset(brush, location, true, wrapEdges);
        });
        return this;
    }

    public FloatMask useBrushWithinArea(BooleanMask other, String brushName, int size, int numUses, float intensity, boolean wrapEdges) {
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            assertSmallerSize(size);
            ArrayList<Vector2> possibleLocations = new ArrayList<>(source.getAllCoordinatesEqualTo(true, 1));
            int length = possibleLocations.size();
            FloatMask brush = loadBrush(brushName, random.nextLong());
            brush.multiply(intensity / brush.getMax()).setSize(size);
            for (int i = 0; i < numUses; i++) {
                Vector2 location = possibleLocations.get(random.nextInt(length));
                addWithOffset(brush, location, true, wrapEdges);
            }
        }, other);
        return this;
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

    public FloatMask parabolicMinimization() {
        enqueue(() -> {
            addCalculatedParabolicDistance(false);
            addCalculatedParabolicDistance(true);
            sqrt();
        });
        return this;
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
                value = get(i, 0);
            } else {
                value = get(0, i);
            }
            vertices.add(new Vector2(0, value));
            intersections.add(new Vector2(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY));
            intersections.add(new Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
            for (int j = 1; j < size; j++) {
                if (!useColumns) {
                    value = get(i, j);
                } else {
                    value = get(j, i);
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
                    set(i, j, height);
                } else {
                    set(j, i, height);
                }
            }
        }
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        writeToImage(image, 255 / getMax());
        return image;
    }

    @Override
    public BufferedImage writeToImage(BufferedImage image) {
        return writeToImage(image, 1f);
    }

    public BufferedImage writeToImage(BufferedImage image, float scaleFactor) {
        assertSize(image.getHeight());
        int size = getSize();
        DataBuffer imageBuffer = image.getRaster().getDataBuffer();
        apply((x, y) -> imageBuffer.setElemFloat(x + y * size, get(x, y) * scaleFactor));
        return image;
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        int size = getSize();
        ByteBuffer bytes = ByteBuffer.allocate(size * size * 4);
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> bytes.putFloat(get(x, y)));
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }
}
