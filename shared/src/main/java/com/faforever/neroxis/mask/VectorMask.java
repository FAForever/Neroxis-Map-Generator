package com.faforever.neroxis.mask;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.vector.Vector;
import com.faforever.neroxis.util.vector.Vector2;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public abstract strictfp class VectorMask<T extends Vector<T>, U extends VectorMask<T, U>> extends OperationsMask<T, U> {

    protected T[][] mask;

    public VectorMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor,
                      String name, boolean parallel) {
        this(sourceImage.getHeight(), seed, symmetrySettings, name, parallel);
        int numImageComponents = sourceImage.getColorModel().getNumComponents();
        assertMatchingDimension(numImageComponents);
        Raster imageRaster = sourceImage.getData();
        set(point -> {
            float[] components = imageRaster.getPixel(point.x, point.y, new float[numImageComponents]);
            return createValue(scaleFactor, components);
        });
    }

    public VectorMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    protected void assertMatchingDimension(int numImageComponents) {
        int dimension = getZeroValue().getDimension();
        if (numImageComponents != dimension) {
            throw new IllegalArgumentException(
                    String.format("Image does not have matching number of components: image %d this %d",
                                  numImageComponents, dimension));
        }
    }

    protected abstract T createValue(float scaleFactor, float... components);

    public VectorMask(Long seed, String name, FloatMask... components) {
        this(components[0].getSize(), seed, components[0].getSymmetrySettings(), name, components[0].isParallel());
        int numComponents = components.length;
        assertMatchingDimension(numComponents);
        assertCompatibleComponents(components);
        enqueue(dependencies -> {
            List<FloatMask> sources = dependencies.stream().map(dep -> ((FloatMask) dep)).collect(Collectors.toList());
            apply(point -> {
                T value = getZeroValue();
                for (int i = 0; i < numComponents; ++i) {
                    value.set(i, sources.get(i).get(point));
                }
            });
        }, components);
    }

    protected void assertCompatibleComponents(Mask<?, ?>... components) {
        Arrays.stream(components).forEach(this::assertCompatibleMask);
    }

    protected VectorMask(U other, String name) {
        super(other, name);
    }

    @Override
    protected U copyFrom(U other) {
        return enqueue(dependencies -> fill(((U) dependencies.get(0)).mask), other);
    }

    @Override
    protected void initializeMask(int size) {
        enqueue(() -> mask = getNullMask(size));
        fill(getZeroValue());
    }

    @Override
    public int getImmediateSize() {
        return mask.length;
    }

    @Override
    public BufferedImage writeToImage(BufferedImage image) {
        int numImageComponents = image.getColorModel().getNumComponents();
        assertMatchingDimension(numImageComponents);
        WritableRaster imageRaster = image.getRaster();
        loop(point -> imageRaster.setPixel(point.x, point.y, get(point).toArray()));
        return image;
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        int size = getSize();
        int dimension = get(0, 0).getDimension();
        ByteBuffer bytes = ByteBuffer.allocate(size * size * 4 * dimension);
        loopWithSymmetry(SymmetryType.SPAWN, point -> {
            Vector<?> value = get(point);
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

    @Override
    @GraphMethod
    public U blur(int radius) {
        T[][] innerCount = getInnerCount();
        return set(point -> calculateAreaAverage(radius, point, innerCount).round().divide(1000));
    }

    @Override
    @GraphMethod
    public U blur(int radius, BooleanMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask limiter = (BooleanMask) dependencies.get(0);
            T[][] innerCount = getInnerCount();
            set(point -> limiter.get(point) ? calculateAreaAverage(radius, point, innerCount).round()
                                                                                             .divide(1000) : get(
                    point));
        }, other);
    }

    @Override
    public T getAvg() {
        assertNotPipelined();
        int size = getSize();
        return getSum().divide(size);
    }

    @Override
    protected void set(int x, int y, T value) {
        mask[x][y] = value.copy();
    }

    protected T[][] getInnerCount() {
        T[][] innerCount = getNullMask(getSize());
        apply(point -> calculateInnerValue(innerCount, point, get(point)));
        return innerCount;
    }

    @Override
    protected U fill(T value) {
        return set(point -> value);
    }

    @Override
    public T get(int x, int y) {
        return mask[x][y].copy();
    }

    protected void calculateInnerValue(T[][] innerCount, Point point, T val) {
        calculateInnerValue(innerCount, point.x, point.y, val);
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

    protected T calculateAreaAverage(int radius, Point point, T[][] innerCount) {
        return calculateAreaAverage(radius, point.x, point.y, innerCount);
    }

    @Override
    protected void addValueAt(int x, int y, T value) {
        mask[x][y].add(value);
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

    @Override
    protected U setSizeInternal(int newSize) {
        return enqueue(() -> {
            int oldSize = getSize();
            if (oldSize == 1) {
                T value = get(0, 0);
                mask = getNullMask(newSize);
                fill(value);
            } else if (oldSize != newSize) {
                T[][] oldMask = mask;
                mask = getNullMask(newSize);
                Map<Integer, Integer> coordinateMap = getSymmetricScalingCoordinateMap(oldSize, newSize);
                set(point -> oldMask[coordinateMap.get(point.x)][coordinateMap.get(point.y)]);
            }
        });
    }

    protected U fill(T[][] maskToFillFrom) {
        int maskSize = maskToFillFrom.length;
        mask = getNullMask(maskSize);
        for (int x = 0; x < maskSize; x++) {
            for (int y = 0; y < maskSize; y++) {
                set(x, y, maskToFillFrom[x][y]);
            }
        }
        return (U) this;
    }

    protected abstract T[][] getNullMask(int size);

    public float getMaxMagnitude() {
        return Arrays.stream(mask)
                     .flatMap(Arrays::stream)
                     .map(Vector::getMagnitude)
                     .max(Comparator.comparing(magnitude -> magnitude))
                     .orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    public T getMaxComponents() {
        return Arrays.stream(mask)
                     .flatMap(Arrays::stream)
                     .reduce((first, second) -> first.copy().max(second))
                     .orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    public T getMinComponents() {
        return Arrays.stream(mask)
                     .flatMap(Arrays::stream)
                     .reduce((first, second) -> first.copy().min(second))
                     .orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    @Override
    protected void subtractValueAt(int x, int y, T value) {
        mask[x][y].subtract(value);
    }

    protected void setComponentAt(Vector2 loc, float value, int component) {
        setComponentAt((int) loc.getX(), (int) loc.getY(), value, component);
    }

    protected U addScalar(Function<Point, Float> valueFunction) {
        return apply(point -> addScalarAt(point, valueFunction.apply(point)));
    }

    protected void addScalarAt(Point point, float value) {
        addScalarAt(point.x, point.y, value);
    }

    protected U subtractScalar(Function<Point, Float> valueFunction) {
        return apply(point -> subtractScalarAt(point, valueFunction.apply(point)));
    }

    protected void subtractScalarAt(Point point, float value) {
        subtractScalarAt(point.x, point.y, value);
    }

    protected U multiplyScalar(Function<Point, Float> valueFunction) {
        return enqueue(() -> apply(point -> multiplyScalarAt(point, valueFunction.apply(point))));
    }

    protected void multiplyScalarAt(Point point, float value) {
        multiplyScalarAt(point.x, point.y, value);
    }

    @Override
    protected void multiplyValueAt(int x, int y, T value) {
        mask[x][y].multiply(value);
    }

    protected U divideScalar(Function<Point, Float> valueFunction) {
        return apply(point -> divideScalarAt(point, valueFunction.apply(point)));
    }

    protected void addScalarAt(Vector2 loc, float value) {
        addScalarAt((int) loc.getX(), (int) loc.getY(), value);
    }

    protected void addScalarAt(int x, int y, float value) {
        mask[x][y].add(value);
    }

    protected void subtractScalarAt(Vector2 loc, float value) {
        subtractScalarAt((int) loc.getX(), (int) loc.getY(), value);
    }

    protected void divideScalarAt(Point point, float value) {
        divideScalarAt(point.x, point.y, value);
    }

    protected void subtractScalarAt(int x, int y, float value) {
        mask[x][y].subtract(value);
    }

    @GraphMethod
    public U blurComponent(int radius, int component, BooleanMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask limiter = (BooleanMask) dependencies.get(0);
            int[][] innerCount = getComponentInnerCount(component);
            setComponent(point -> limiter.get(point) ? calculateComponentAreaAverage(radius, point, innerCount)
                                                       / 1000f : get(point).get(component), component);
        }, other);
    }

    @Override
    protected void divideValueAt(int x, int y, T value) {
        mask[x][y].divide(value);
    }

    protected void multiplyScalarAt(Vector2 loc, float value) {
        multiplyScalarAt((int) loc.getX(), (int) loc.getY(), value);
    }

    protected void multiplyScalarAt(int x, int y, float value) {
        mask[x][y].multiply(value);
    }

    protected void divideScalarAt(Vector2 loc, float value) {
        divideScalarAt((int) loc.getX(), (int) loc.getY(), value);
    }

    protected void divideScalarAt(int x, int y, float value) {
        mask[x][y].divide(value);
    }

    protected int[][] getComponentInnerCount(int component) {
        int[][] innerCount = new int[getSize()][getSize()];
        apply(point -> calculateComponentInnerValue(innerCount, point,
                                                    StrictMath.round(get(point).get(component) * 1000)));
        return innerCount;
    }

    protected void calculateComponentInnerValue(int[][] innerCount, Point point, int val) {
        calculateComponentInnerValue(innerCount, point.x, point.y, val);
    }

    protected void calculateComponentInnerValue(int[][] innerCount, int x, int y, int val) {
        calculateScalarInnerValue(innerCount, x, y, val);
    }

    @Override
    public T getSum() {
        return Arrays.stream(mask)
                     .flatMap(Arrays::stream)
                     .reduce((first, second) -> first.copy().add(second))
                     .orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    protected void setComponentAt(int x, int y, float value, int component) {
        mask[x][y].set(component, value);
    }

    protected void addComponentAt(Vector2 loc, float value, int component) {
        addComponentAt((int) loc.getX(), (int) loc.getY(), value, component);
    }

    protected void addComponentAt(int x, int y, float value, int component) {
        mask[x][y].add(value, component);
    }

    protected void subtractComponentAt(Vector2 loc, float value, int component) {
        subtractComponentAt((int) loc.getX(), (int) loc.getY(), value, component);
    }

    protected void subtractComponentAt(int x, int y, float value, int component) {
        mask[x][y].subtract(value, component);
    }

    protected void multiplyComponentAt(Vector2 loc, float value, int component) {
        multiplyComponentAt((int) loc.getX(), (int) loc.getY(), value, component);
    }

    protected void multiplyComponentAt(int x, int y, float value, int component) {
        mask[x][y].multiply(value, component);
    }

    protected void divideComponentAt(Vector2 loc, float value, int component) {
        divideComponentAt((int) loc.getX(), (int) loc.getY(), value, component);
    }

    protected void divideComponentAt(int x, int y, float value, int component) {
        mask[x][y].divide(value, component);
    }

    @GraphMethod
    public U addScalar(float value) {
        return addScalar(point -> value);
    }

    protected U setComponent(Function<Point, Float> valueFunction, int component) {
        return apply(point -> setComponentAt(point, valueFunction.apply(point), component));
    }

    protected void setComponentAt(Point point, float value, int component) {
        setComponentAt(point.x, point.y, value, component);
    }

    @GraphMethod
    public U subtractScalar(float value) {
        return subtractScalar(point -> value);
    }

    protected float calculateComponentAreaAverage(int radius, Point point, int[][] innerCount) {
        return calculateComponentAreaAverage(radius, point.x, point.y, innerCount);
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

    @GraphMethod
    public U multiplyScalar(float value) {
        return multiplyScalar(point -> value);
    }

    protected U addComponent(Function<Point, Float> valueFunction, int component) {
        return apply(point -> addComponentAt(point, valueFunction.apply(point), component));
    }

    protected void addComponentAt(Point point, float value, int component) {
        addComponentAt(point.x, point.y, value, component);
    }

    @GraphMethod
    public U divideScalar(float value) {
        return divideScalar(point -> value);
    }

    protected U subtractComponent(Function<Point, Float> valueFunction, int component) {
        return enqueue(() -> apply(point -> subtractComponentAt(point, valueFunction.apply(point), component)));
    }

    protected void subtractComponentAt(Point point, float value, int component) {
        subtractComponentAt(point.x, point.y, value, component);
    }

    @GraphMethod
    public U clampComponentMin(float floor) {
        return apply(point -> get(point).clampMin(floor));
    }

    @GraphMethod
    public U clampComponentMax(float ceiling) {
        return apply(point -> get(point).clampMax(ceiling));
    }

    @GraphMethod
    public U randomize(float scale) {
        return setWithSymmetry(SymmetryType.SPAWN, point -> getZeroValue().randomize(random, scale));
    }

    @GraphMethod
    public U randomize(float minValue, float maxValue) {
        return setWithSymmetry(SymmetryType.SPAWN, point -> getZeroValue().randomize(random, minValue, maxValue));
    }

    @GraphMethod
    public U normalize() {
        return enqueue(dependencies -> apply(point -> get(point).normalize()));
    }

    @GraphMethod(returnsSelf = false)
    public FloatMask copyAsDotProduct(U other) {
        return copyAsDotProduct(other, getName() + "dot" + other.getName());
    }

    public FloatMask copyAsDotProduct(U other, String name) {
        assertCompatibleMask(other);
        return new FloatMask(this, other, name);
    }

    @GraphMethod(returnsSelf = false)
    public FloatMask copyAsDotProduct(T vector) {
        return copyAsDotProduct(vector, getName() + "Dot");
    }

    public FloatMask copyAsDotProduct(T vector, String name) {
        assertMatchingDimension(vector.getDimension());
        return new FloatMask(this, vector, name);
    }

    @GraphMethod
    public U blurComponent(int radius, int component) {
        int[][] innerCount = getComponentInnerCount(component);
        return setComponent(point -> calculateComponentAreaAverage(radius, point, innerCount) / 1000f, component);
    }

    protected U multiplyComponent(Function<Point, Float> valueFunction, int component) {
        return apply(point -> multiplyComponentAt(point, valueFunction.apply(point), component));
    }

    protected void multiplyComponentAt(Point point, float value, int component) {
        multiplyComponentAt(point.x, point.y, value, component);
    }

    protected U divideComponent(Function<Point, Float> valueFunction, int component) {
        return apply(point -> divideComponentAt(point, valueFunction.apply(point), component));
    }

    protected void divideComponentAt(Point point, float value, int component) {
        divideComponentAt(point.x, point.y, value, component);
    }

    protected U addScalarWithSymmetry(SymmetryType symmetryType, Function<Point, Float> valueFunction) {
        return applyWithSymmetry(symmetryType, point -> {
            Float value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> addScalarAt(symPoint, value));
        });
    }

    protected U subtractScalarWithSymmetry(SymmetryType symmetryType, Function<Point, Float> valueFunction) {
        return applyWithSymmetry(symmetryType, point -> {
            Float value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> subtractScalarAt(symPoint, value));
        });
    }

    protected U multiplyScalarWithSymmetry(SymmetryType symmetryType, Function<Point, Float> valueFunction) {
        return applyWithSymmetry(symmetryType, point -> {
            Float value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> multiplyScalarAt(symPoint, value));
        });
    }

    protected U divideScalarWithSymmetry(SymmetryType symmetryType, Function<Point, Float> valueFunction) {
        return applyWithSymmetry(symmetryType, point -> {
            Float value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> divideScalarAt(symPoint, value));
        });
    }

    @GraphMethod
    public U addComponent(float value, int component) {
        return addComponent(point -> value, component);
    }

    protected U setComponentWithSymmetry(SymmetryType symmetryType, Function<Point, Float> valueFunction,
                                         int component) {
        return applyWithSymmetry(symmetryType, point -> {
            Float value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> setComponentAt(symPoint, value, component));
        });
    }

    protected U addComponentWithSymmetry(SymmetryType symmetryType, Function<Point, Float> valueFunction,
                                         int component) {
        return applyWithSymmetry(symmetryType, point -> {
            Float value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> addComponentAt(symPoint, value, component));
        });
    }

    @GraphMethod
    public U addComponent(BooleanMask other, float value, int component) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            addComponent(point -> source.get(point) ? value : 0, component);
        }, other);
    }

    @GraphMethod
    public U addComponent(FloatMask other, int component) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            addComponent(source::get, component);
        }, other);
    }

    @GraphMethod
    public U subtractComponent(float value, int component) {
        return subtractComponent(point -> value, component);
    }

    protected U subtractComponentWithSymmetry(SymmetryType symmetryType, Function<Point, Float> valueFunction,
                                              int component) {
        return applyWithSymmetry(symmetryType, point -> {
            Float value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> subtractComponentAt(symPoint, value, component));
        });
    }

    protected U multiplyComponentWithSymmetry(SymmetryType symmetryType, Function<Point, Float> valueFunction,
                                              int component) {
        return applyWithSymmetry(symmetryType, point -> {
            Float value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> multiplyComponentAt(symPoint, value, component));
        });
    }

    @GraphMethod
    public U subtractComponent(BooleanMask other, float value, int component) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            subtractComponent(point -> source.get(point) ? value : 0, component);
        }, other);
    }

    @GraphMethod
    public U subtractComponent(FloatMask other, int component) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            subtractComponent(source::get, component);
        }, other);
    }

    @GraphMethod
    public U multiplyComponent(float value, int component) {
        return multiplyComponent(point -> value, component);
    }

    protected U divideComponentWithSymmetry(SymmetryType symmetryType, Function<Point, Float> valueFunction,
                                            int component) {
        return applyWithSymmetry(symmetryType, point -> {
            Float value = valueFunction.apply(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> divideComponentAt(symPoint, value, component));
        });
    }

    @GraphMethod
    public U multiplyComponent(BooleanMask other, float value, int component) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            multiplyComponent(point -> source.get(point) ? value : 0, component);
        }, other);
    }

    @GraphMethod
    public U multiplyComponent(FloatMask other, int component) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            multiplyComponent(source::get, component);
        }, other);
    }

    @GraphMethod
    public U divideComponent(float value, int component) {
        return divideComponent(point -> value, component);
    }

    @GraphMethod
    public U divideComponent(BooleanMask other, float value, int component) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            divideComponent(point -> source.get(point) ? value : 0, component);
        }, other);
    }

    @GraphMethod
    public U divideComponent(FloatMask other, int component) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            divideComponent(source::get, component);
        }, other);
    }

    @GraphMethod(returnsSelf = false)
    public FloatMask copyAsComponentMask(int component) {
        return copyAsComponentMask(component, getName() + "Component" + component);
    }

    public FloatMask copyAsComponentMask(int component, String name) {
        return new FloatMask(this, component, name);
    }

    public FloatMask[] splitComponentMasks() {
        int dimesion = getZeroValue().getDimension();
        String name = getName();
        FloatMask[] components = new FloatMask[dimesion];
        for (int i = 0; i < dimesion; ++i) {
            components[i] = new FloatMask(this, i, name + "Component" + i);
        }
        return components;
    }






























}
