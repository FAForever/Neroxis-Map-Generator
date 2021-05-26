package com.faforever.neroxis.map.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.Vector;
import com.faforever.neroxis.util.Vector2;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public abstract strictfp class VectorMask<T extends Vector<T>, U extends VectorMask<T, U>> extends OperationsMask<T, U> {

    public VectorMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public VectorMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public VectorMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public VectorMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, null, false);
    }

    public VectorMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, name, false);
    }

    public VectorMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name, boolean parallel) {
        super(sourceImage.getHeight(), seed, symmetrySettings, name, parallel);
        int numImageComponents = sourceImage.getColorModel().getNumComponents();
        assertMatchingDimension(numImageComponents);
        Raster imageRaster = sourceImage.getData();

        enqueue(() ->
                set((x, y) -> {
                    float[] components = imageRaster.getPixel(x, y, new float[numImageComponents]);
                    return createValue(scaleFactor, components);
                })
        );
    }

    public VectorMask(Long seed, String name, FloatMask... components) {
        super(components[0].getSize(), seed, components[0].getSymmetrySettings(), name, components[0].isParallel());
        int numComponents = components.length;
        assertMatchingDimension(numComponents);
        assertCompatibleComponents(components);
        enqueue(dependencies -> {
            List<FloatMask> sources = dependencies.stream().map(dep -> ((FloatMask) dep)).collect(Collectors.toList());
            apply((x, y) -> {
                T value = getZeroValue();
                for (int i = 0; i < numComponents; ++i) {
                    value.set(i, sources.get(i).get(x, y));
                }
            });
        }, components);
    }

    public VectorMask(U other, Long seed) {
        super(other, seed);
    }

    public VectorMask(U other, Long seed, String name) {
        super(other, seed, name);
    }

    @Override
    public T getSum() {
        return Arrays.stream(mask).flatMap(Arrays::stream).reduce((first, second) -> first.copy().add(second)).orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    @Override
    public T getAvg() {
        int size = getSize();
        return getSum().divide(size);
    }

    public float getMaxMagnitude() {
        return Arrays.stream(mask).flatMap(Arrays::stream).map(Vector::getMagnitude).max(Comparator.comparing(magnitude -> magnitude)).orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    public T getMaxComponents() {
        return Arrays.stream(mask).flatMap(Arrays::stream).reduce((first, second) -> first.copy().max(second)).orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    public T getMinComponents() {
        return Arrays.stream(mask).flatMap(Arrays::stream).reduce((first, second) -> first.copy().min(second)).orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    protected abstract T createValue(float scaleFactor, float... components);

    @Override
    public void addValueAt(int x, int y, T value) {
        get(x, y).add(value);
    }

    @Override
    public void subtractValueAt(int x, int y, T value) {
        get(x, y).subtract(value);
    }

    @Override
    public void multiplyValueAt(int x, int y, T value) {
        get(x, y).multiply(value);
    }

    @Override
    public void divideValueAt(int x, int y, T value) {
        get(x, y).divide(value);
    }

    public void addScalarAt(Vector2 loc, float value) {
        addScalarAt((int) loc.getX(), (int) loc.getY(), value);
    }

    public void addScalarAt(int x, int y, float value) {
        get(x, y).add(value);
    }

    public void subtractScalarAt(Vector2 loc, float value) {
        subtractScalarAt((int) loc.getX(), (int) loc.getY(), value);
    }

    public void subtractScalarAt(int x, int y, float value) {
        get(x, y).subtract(value);
    }

    public void multiplyScalarAt(Vector2 loc, float value) {
        multiplyScalarAt((int) loc.getX(), (int) loc.getY(), value);
    }

    public void multiplyScalarAt(int x, int y, float value) {
        get(x, y).multiply(value);
    }

    public void divideScalarAt(Vector2 loc, float value) {
        divideScalarAt((int) loc.getX(), (int) loc.getY(), value);
    }

    public void divideScalarAt(int x, int y, float value) {
        get(x, y).divide(value);
    }

    public void setComponentAt(Vector2 loc, float value, int component) {
        setComponentAt((int) loc.getX(), (int) loc.getY(), value, component);
    }

    public void setComponentAt(int x, int y, float value, int component) {
        get(x, y).set(component, value);
    }

    public void addComponentAt(Vector2 loc, float value, int component) {
        addComponentAt((int) loc.getX(), (int) loc.getY(), value, component);
    }

    public void addComponentAt(int x, int y, float value, int component) {
        get(x, y).add(value, component);
    }

    public void subtractComponentAt(Vector2 loc, float value, int component) {
        subtractComponentAt((int) loc.getX(), (int) loc.getY(), value, component);
    }

    public void subtractComponentAt(int x, int y, float value, int component) {
        get(x, y).subtract(value, component);
    }

    public void multiplyComponentAt(Vector2 loc, float value, int component) {
        multiplyComponentAt((int) loc.getX(), (int) loc.getY(), value, component);
    }

    public void multiplyComponentAt(int x, int y, float value, int component) {
        get(x, y).multiply(value, component);
    }

    public void divideComponentAt(Vector2 loc, float value, int component) {
        divideComponentAt((int) loc.getX(), (int) loc.getY(), value, component);
    }

    public void divideComponentAt(int x, int y, float value, int component) {
        get(x, y).divide(value, component);
    }

    public U addScalar(float value) {
        enqueue(() -> addScalar((x, y) -> value));
        return (U) this;
    }

    public U subtractScalar(float value) {
        enqueue(() -> subtractScalar((x, y) -> value));
        return (U) this;
    }

    public U multiplyScalar(float value) {
        enqueue(() -> multiplyScalar((x, y) -> value));
        return (U) this;
    }

    public U divideScalar(float value) {
        enqueue(() -> divideScalar((x, y) -> value));
        return (U) this;
    }

    public U clampComponentMin(float floor) {
        enqueue(() -> apply((x, y) -> get(x, y).clampMin(floor)));
        return (U) this;
    }

    public U clampComponentMax(float ceiling) {
        enqueue(() -> apply((x, y) -> get(x, y).clampMax(ceiling)));
        return (U) this;
    }

    public U randomize(float scale) {
        T empty = getZeroValue();
        enqueue(() -> setWithSymmetry(SymmetryType.SPAWN, (x, y) -> empty.copy().randomize(random, scale)));
        return (U) this;
    }

    public U randomize(float minValue, float maxValue) {
        T empty = getZeroValue();
        enqueue(() -> setWithSymmetry(SymmetryType.SPAWN, (x, y) -> empty.copy().randomize(random, minValue, maxValue)));
        return (U) this;
    }

    public U normalize() {
        enqueue((dependencies) -> apply((x, y) -> get(x, y).normalize()));
        return (U) this;
    }

    public FloatMask dot(U other) {
        assertCompatibleMask(other);
        return new FloatMask(this, other, getNextSeed(), getName() + "dot" + other.getName());
    }

    public FloatMask dot(T vector) {
        assertMatchingDimension(vector.getDimension());
        return new FloatMask(this, vector, getNextSeed(), getName() + "dot");
    }

    public U blur(int radius) {
        enqueue(() -> {
            T[][] innerCount = getInnerCount();
            set((x, y) -> calculateAreaAverage(radius, x, y, innerCount).round().divide(1000));
        });
        return (U) this;
    }

    public U blur(int radius, BooleanMask other) {
        enqueue(dependencies -> {
            BooleanMask limiter = (BooleanMask) dependencies.get(0);
            assertCompatibleMask(limiter);
            T[][] innerCount = getInnerCount();
            set((x, y) -> limiter.get(x, y) ? calculateAreaAverage(radius, x, y, innerCount).round().divide(1000) : get(x, y));
        }, other);
        return (U) this;
    }

    public U blurComponent(int radius, int component) {
        enqueue(() -> {
            int[][] innerCount = getComponentInnerCount(component);
            setComponent((x, y) -> calculateComponentAreaAverage(radius, x, y, innerCount) / 1000f, component);
        });
        return (U) this;
    }

    public U blurComponent(int radius, int component, BooleanMask other) {
        enqueue(dependencies -> {
            BooleanMask limiter = (BooleanMask) dependencies.get(0);
            assertCompatibleMask(limiter);
            int[][] innerCount = getComponentInnerCount(component);
            setComponent((x, y) -> limiter.get(x, y) ? calculateComponentAreaAverage(radius, x, y, innerCount) / 1000f : get(x, y).get(component), component);
        }, other);
        return (U) this;
    }

    public U addComponent(float value, int component) {
        enqueue(() -> addComponent((x, y) -> value, component));
        return (U) this;
    }

    public U addComponent(FloatMask other, int component) {
        enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            addComponent(source::get, component);
        }, other);
        return (U) this;
    }

    public U subtractComponent(float value, int component) {
        enqueue(() -> subtractComponent((x, y) -> value, component));
        return (U) this;
    }

    public U subtractComponent(FloatMask other, int component) {
        enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            subtractComponent(source::get, component);
        }, other);
        return (U) this;
    }

    public U multiplyComponent(float value, int component) {
        enqueue(() -> multiplyComponent((x, y) -> value, component));
        return (U) this;
    }

    public U multiplyComponent(FloatMask other, int component) {
        enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            multiplyComponent(source::get, component);
        }, other);
        return (U) this;
    }

    public U divideComponent(float value, int component) {
        enqueue(() -> divideComponent((x, y) -> value, component));
        return (U) this;
    }

    public U divideComponent(FloatMask other, int component) {
        enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            divideComponent(source::get, component);
        }, other);
        return (U) this;
    }

    public FloatMask getComponentMask(int component) {
        return new FloatMask(this, component, getNextSeed(), getName() + "Component" + component);
    }

    public FloatMask[] splitComponentMasks() {
        int dimesion = getZeroValue().getDimension();
        String name = getName();
        FloatMask[] components = new FloatMask[dimesion];
        for (int i = 0; i < dimesion; ++i) {
            components[i] = new FloatMask(this, i, getNextSeed(), name + "Component" + i);
        }
        return components;
    }

    protected int[][] getComponentInnerCount(int component) {
        int[][] innerCount = new int[getSize()][getSize()];
        apply((x, y) -> calculateComponentInnerValue(innerCount, x, y, StrictMath.round(get(x, y).get(component) * 1000)));
        return innerCount;
    }

    protected void calculateComponentInnerValue(int[][] innerCount, int x, int y, int val) {
        calculateScalarInnerValue(innerCount, x, y, val);
    }

    protected float calculateComponentAreaAverage(int radius, int x, int y, int[][] innerCount) {
        int size = getSize();
        int xLeft = StrictMath.max(0, x - radius);
        int xRight = StrictMath.min(size - 1, x + radius);
        int yUp = StrictMath.max(0, y - radius);
        int yDown = StrictMath.min(size - 1, y + radius);
        int countA = xLeft > 0 && yUp > 0 ? innerCount[xLeft - 1][yUp - 1] : 0;
        int countB = yUp > 0 ? innerCount[xRight][yUp - 1] : 0;
        int countC = xLeft > 0 ? innerCount[xLeft - 1][yDown] : 0;
        int countD = innerCount[xRight][yDown];
        int count = countD + countA - countB - countC;
        int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
        return (float) count / area;
    }

    protected T[][] getInnerCount() {
        T[][] innerCount = getNullMask(getSize());
        apply((x, y) -> calculateInnerValue(innerCount, x, y, get(x, y)));
        return innerCount;
    }

    protected void calculateInnerValue(T[][] innerCount, int x, int y, T val) {
        innerCount[x][y] = val.copy().multiply(1000).round();
        if (x > 0) {
            innerCount[x][y].add(innerCount[x - 1][y]);
        }
        if (y > 0) {
            innerCount[x][y].add(innerCount[x][y - 1]);
        }
        if (x > 0 && y > 0) {
            innerCount[x][y].subtract(innerCount[x - 1][y - 1]);
        }
    }

    protected T calculateAreaAverage(int radius, int x, int y, T[][] innerCount) {
        T result = getZeroValue();
        int xLeft = StrictMath.max(0, x - radius);
        int size = getSize();
        int xRight = StrictMath.min(size - 1, x + radius);
        int yUp = StrictMath.max(0, y - radius);
        int yDown = StrictMath.min(size - 1, y + radius);
        T countA = xLeft > 0 && yUp > 0 ? innerCount[xLeft - 1][yUp - 1] : getZeroValue();
        T countB = yUp > 0 ? innerCount[xRight][yUp - 1] : getZeroValue();
        T countC = xLeft > 0 ? innerCount[xLeft - 1][yDown] : getZeroValue();
        T countD = innerCount[xRight][yDown];
        int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
        return result.add(countD).add(countA).subtract(countB).subtract(countC).divide(area);
    }

    public void addScalar(BiFunction<Integer, Integer, Float> valueFunction) {
        apply((x, y) -> addScalarAt(x, y, valueFunction.apply(x, y)));
    }

    public void addScalarWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, Float> valueFunction) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            Float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> addScalarAt(sx, sy, value));
        });
    }

    public void subtractScalar(BiFunction<Integer, Integer, Float> valueFunction) {
        apply((x, y) -> subtractScalarAt(x, y, valueFunction.apply(x, y)));
    }

    public void subtractScalarWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, Float> valueFunction) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            Float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> subtractScalarAt(sx, sy, value));
        });
    }

    public void multiplyScalar(BiFunction<Integer, Integer, Float> valueFunction) {
        apply((x, y) -> multiplyScalarAt(x, y, valueFunction.apply(x, y)));
    }

    public void multiplyScalarWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, Float> valueFunction) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            Float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> multiplyScalarAt(sx, sy, value));
        });
    }

    public void divideScalar(BiFunction<Integer, Integer, Float> valueFunction) {
        apply((x, y) -> divideScalarAt(x, y, valueFunction.apply(x, y)));
    }

    public void divideScalarWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, Float> valueFunction) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            Float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> divideScalarAt(sx, sy, value));
        });
    }

    protected void setComponent(BiFunction<Integer, Integer, Float> valueFunction, int component) {
        apply((x, y) -> setComponentAt(x, y, valueFunction.apply(x, y), component));
    }

    protected void setComponentWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, Float> valueFunction, int component) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            Float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> setComponentAt(sx, sy, value, component));
        });
    }

    protected void addComponent(BiFunction<Integer, Integer, Float> valueFunction, int component) {
        apply((x, y) -> addComponentAt(x, y, valueFunction.apply(x, y), component));
    }

    protected void addComponentWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, Float> valueFunction, int component) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            Float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> addComponentAt(sx, sy, value, component));
        });
    }

    protected void subtractComponent(BiFunction<Integer, Integer, Float> valueFunction, int component) {
        apply((x, y) -> subtractComponentAt(x, y, valueFunction.apply(x, y), component));
    }

    protected void subtractComponentWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, Float> valueFunction, int component) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            Float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> subtractComponentAt(sx, sy, value, component));
        });
    }

    protected void multiplyComponent(BiFunction<Integer, Integer, Float> valueFunction, int component) {
        apply((x, y) -> multiplyComponentAt(x, y, valueFunction.apply(x, y), component));
    }

    protected void multiplyComponentWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, Float> valueFunction, int component) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            Float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> multiplyComponentAt(sx, sy, value, component));
        });
    }

    protected void divideComponent(BiFunction<Integer, Integer, Float> valueFunction, int component) {
        apply((x, y) -> divideComponentAt(x, y, valueFunction.apply(x, y), component));
    }

    protected void divideComponentWithSymmetry(SymmetryType symmetryType, BiFunction<Integer, Integer, Float> valueFunction, int component) {
        applyWithSymmetry(symmetryType, (x, y) -> {
            Float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> divideComponentAt(sx, sy, value, component));
        });
    }

    protected void maskFill(T value) {
        maskFill(mask, value);
    }

    protected void maskFill(T[][] maskToFill, T value) {
        int maskSize = maskToFill.length;
        for (int x = 0; x < maskSize; ++x) {
            for (int y = 0; y < maskSize; ++y) {
                maskToFill[x][y] = value.copy();
            }
        }
    }

    @Override
    protected void maskFill(T[][] maskToFill) {
        int maskSize = mask.length;
        assertSize(maskSize);
        for (int x = 0; x < maskSize; ++x) {
            for (int y = 0; y < maskSize; ++y) {
                maskToFill[x][y] = get(x, y).copy();
            }
        }
    }

    @Override
    public BufferedImage writeToImage(BufferedImage image) {
        int numImageComponents = image.getColorModel().getNumComponents();
        assertMatchingDimension(numImageComponents);
        WritableRaster imageRaster = image.getRaster();
        apply((x, y) -> imageRaster.setPixel(x, y, get(x, y).toArray()));
        return image;
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        int size = getSize();
        int dimension = get(0, 0).getDimension();
        ByteBuffer bytes = ByteBuffer.allocate(size * size * 4 * dimension);
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> {
            Vector<?> value = get(x, y);
            for (int i = 0; i < dimension; ++i) {
                bytes.putFloat(value.get(i));
            }
        });
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }

    protected void assertMatchingDimension(int numImageComponents) {
        int dimension = getZeroValue().getDimension();
        if (numImageComponents != dimension) {
            throw new IllegalArgumentException(String.format("Image does not have matching number of components: image %d this %d", numImageComponents, dimension));
        }
    }

    protected void assertCompatibleComponents(Mask<?, ?>... components) {
        Arrays.stream(components).forEach(this::assertCompatibleMask);
    }

}
