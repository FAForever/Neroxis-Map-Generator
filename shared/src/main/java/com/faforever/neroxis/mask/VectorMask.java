package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.functional.BiIntFloatIntConsumer;
import com.faforever.neroxis.util.functional.ToFloatBiIntFunction;
import com.faforever.neroxis.util.vector.Vector;
import com.faforever.neroxis.util.vector.Vector2;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public abstract sealed class VectorMask<T extends Vector<T>, U extends VectorMask<T, U>> extends
                                                                                         OperationsMask<T, U> permits
                                                                                                              NormalMask,
                                                                                                              Vector2Mask,
                                                                                                              Vector3Mask,
                                                                                                              Vector4Mask {
    protected T[][] mask;

    public VectorMask(BufferedImage sourceImage, RandomGenerator.SplittableGenerator random,
                      SymmetrySettings symmetrySettings, float scaleFactor,
                      String name) {
        this(sourceImage.getHeight(), random, symmetrySettings, name);
        int numImageComponents = sourceImage.getColorModel().getNumComponents();
        assertMatchingDimension(numImageComponents);
        Raster imageRaster = sourceImage.getData();
        set((x, y) -> {
            float[] components = imageRaster.getPixel(x, y, new float[numImageComponents]);
            return createValue(scaleFactor, components);
        });
    }

    public VectorMask(int size, RandomGenerator.SplittableGenerator random, SymmetrySettings symmetrySettings,
                      String name) {
        super(size, random, symmetrySettings, name);
    }

    public VectorMask(RandomGenerator.SplittableGenerator random, String name, FloatMask... components) {
        this(components[0].getSize(), random, components[0].getSymmetrySettings(), name);
        int numComponents = components.length;
        assertMatchingDimension(numComponents);
        assertCompatibleComponents(components);
        enqueue(dependencies -> {
            List<FloatMask> sources = dependencies.stream().map(dep -> ((FloatMask) dep)).toList();
            set((x, y) -> {
                T value = mask[x][y];
                for (int i = 0; i < numComponents; ++i) {
                    value = value.withComponent(i, sources.get(i).get(x, y));
                }
                return value;
            });
        }, components);
    }

    protected VectorMask(U other, String name) {
        super(other, name);
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

    protected void assertCompatibleComponents(Mask<?, ?>... components) {
        Arrays.stream(components).forEach(this::assertCompatibleMask);
    }

    @Override
    public U blur(int radius) {
        T[][] innerCount = getInnerCount();
        return set((x, y) -> calculateAreaAverage(radius, x, y, innerCount).round().divide(1000));
    }

    @Override
    public U blur(int radius, BooleanMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask limiter = (BooleanMask) dependencies.getFirst();
            T[][] innerCount = getInnerCount();
            set((x, y) -> limiter.get(x, y) ? calculateAreaAverage(radius, x, y, innerCount).round().divide(1000) : get(
                    x, y));
        }, other);
    }

    @Override
    protected U copyFrom(U other) {
        return enqueue(dependencies -> fill(((U) dependencies.getFirst()).mask), other);
    }

    @Override
    protected void initializeMask(int size) {
        enqueue(() -> {
            mask = getNullMask(size);
            fill(getZeroValue());
        });
    }

    @Override
    protected int getImmediateSize() {
        return mask.length;
    }

    @Override
    public BufferedImage writeToImage(BufferedImage image) {
        int numImageComponents = image.getColorModel().getNumComponents();
        assertMatchingDimension(numImageComponents);
        WritableRaster imageRaster = image.getRaster();
        loop((x, y) -> imageRaster.setPixel(x, y, get(x, y).toArray()));
        return image;
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        int size = getSize();
        int dimension = get(0, 0).getDimension();
        ByteBuffer bytes = ByteBuffer.allocate(size * size * 4 * dimension);
        loopInSymmetryRegion(SymmetryType.SPAWN, (x, y) -> {
            Vector<?> value = get(x, y);
            for (int i = 0; i < dimension; ++i) {
                bytes.putFloat(value.get(i));
            }
        });
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        return HexFormat.of().formatHex(data);
    }

    @Override
    public T get(int x, int y) {
        return mask[x][y];
    }

    @Override
    protected void set(int x, int y, T value) {
        mask[x][y] = value;
    }

    @Override
    protected U fill(T value) {
        return set((x, y) -> value);
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
                setWithSymmetry(SymmetryType.SPAWN, (x, y) -> oldMask[coordinateMap.get(x)][coordinateMap.get(y)]);
            }
        });
    }

    protected T[][] getInnerCount() {
        T[][] innerCount = getNullMask(getSize());
        apply((x, y) -> calculateInnerValue(innerCount, x, y, get(x, y)));
        return innerCount;
    }

    protected void calculateInnerValue(T[][] innerCount, int x, int y, T val) {
        innerCount[x][y] = val.multiply(1000).round();
        if (x > 0) {
            innerCount[x][y] = innerCount[x][y].add(innerCount[x - 1][y]);
        }
        if (y > 0) {
            innerCount[x][y] = innerCount[x][y].add(innerCount[x][y - 1]);
        }
        if (x > 0 && y > 0) {
            innerCount[x][y] = innerCount[x][y].subtract(innerCount[x - 1][y - 1]);
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
                     .reduce(Vector::max)
                     .orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    public T getMinComponents() {
        return Arrays.stream(mask)
                     .flatMap(Arrays::stream)
                     .reduce(Vector::min)
                     .orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    protected void setComponentAt(Vector2 loc, float value, int component) {
        setComponentAt((int) loc.x(), (int) loc.y(), value, component);
    }

    protected U addScalar(ToFloatBiIntFunction valueFunction) {
        return apply((x, y) -> addScalarAt(x, y, valueFunction.apply(x, y)));
    }

    protected U subtractScalar(ToFloatBiIntFunction valueFunction) {
        return apply((x, y) -> subtractScalarAt(x, y, valueFunction.apply(x, y)));
    }

    protected U multiplyScalar(ToFloatBiIntFunction valueFunction) {
        return enqueue(() -> apply((x, y) -> multiplyScalarAt(x, y, valueFunction.apply(x, y))));
    }

    protected U divideScalar(ToFloatBiIntFunction valueFunction) {
        return apply((x, y) -> divideScalarAt(x, y, valueFunction.apply(x, y)));
    }

    protected void addScalarAt(Vector2 loc, float value) {
        addScalarAt((int) loc.x(), (int) loc.y(), value);
    }

    protected void addScalarAt(int x, int y, float value) {
        mask[x][y] = mask[x][y].add(value);
    }

    protected void subtractScalarAt(Vector2 loc, float value) {
        subtractScalarAt((int) loc.x(), (int) loc.y(), value);
    }

    protected void subtractScalarAt(int x, int y, float value) {
        mask[x][y] = mask[x][y].subtract(value);
    }

    public U blurComponent(int radius, int component, BooleanMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask limiter = (BooleanMask) dependencies.getFirst();
            int[][] innerCount = getComponentInnerCount(component);
            setComponent(
                    (x, y) -> limiter.get(x, y) ? calculateComponentAreaAverage(radius, x, y, innerCount) / 1000f : get(
                            x, y).get(component), component);
        }, other);
    }

    protected void multiplyScalarAt(Vector2 loc, float value) {
        multiplyScalarAt((int) loc.x(), (int) loc.y(), value);
    }

    protected void multiplyScalarAt(int x, int y, float value) {
        mask[x][y] = mask[x][y].multiply(value);
    }

    protected void divideScalarAt(Vector2 loc, float value) {
        divideScalarAt((int) loc.x(), (int) loc.y(), value);
    }

    protected void divideScalarAt(int x, int y, float value) {
        mask[x][y] = mask[x][y].divide(value);
    }

    protected int[][] getComponentInnerCount(int component) {
        int[][] innerCount = new int[getSize()][getSize()];
        apply((x, y) -> calculateComponentInnerValue(innerCount, x, y,
                                                     StrictMath.round(get(x, y).get(component) * 1000)));
        return innerCount;
    }

    protected void calculateComponentInnerValue(int[][] innerCount, int x, int y, int val) {
        calculateScalarInnerValue(innerCount, x, y, val);
    }

    @Override
    public T getSum() {
        return Arrays.stream(mask)
                     .flatMap(Arrays::stream)
                     .reduce(Vector::add)
                     .orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    @Override
    protected void addValueAt(int x, int y, T value) {
        mask[x][y] = mask[x][y].add(value);
    }

    @Override
    protected void subtractValueAt(int x, int y, T value) {
        mask[x][y] = mask[x][y].subtract(value);
    }

    @Override
    public T getAvg() {
        assertNotPipelined();
        int size = getSize();
        return getSum().divide(size);
    }

    @Override
    protected void multiplyValueAt(int x, int y, T value) {
        mask[x][y] = mask[x][y].multiply(value);
    }

    @Override
    protected void divideValueAt(int x, int y, T value) {
        mask[x][y] = mask[x][y].divide(value);
    }

    protected void setComponentAt(int x, int y, float value, int component) {
        mask[x][y] = mask[x][y].withComponent(component, value);
    }

    protected void addComponentAt(Vector2 loc, float value, int component) {
        addComponentAt((int) loc.x(), (int) loc.y(), value, component);
    }

    protected void addComponentAt(int x, int y, float value, int component) {
        mask[x][y] = mask[x][y].add(value, component);
    }

    protected void subtractComponentAt(Vector2 loc, float value, int component) {
        subtractComponentAt((int) loc.x(), (int) loc.y(), value, component);
    }

    protected void subtractComponentAt(int x, int y, float value, int component) {
        mask[x][y] = mask[x][y].subtract(value, component);
    }

    protected void multiplyComponentAt(Vector2 loc, float value, int component) {
        multiplyComponentAt((int) loc.x(), (int) loc.y(), value, component);
    }

    protected void multiplyComponentAt(int x, int y, float value, int component) {
        mask[x][y] = mask[x][y].multiply(value, component);
    }

    protected void divideComponentAt(Vector2 loc, float value, int component) {
        divideComponentAt((int) loc.x(), (int) loc.y(), value, component);
    }

    protected void divideComponentAt(int x, int y, float value, int component) {
        mask[x][y] = mask[x][y].divide(value, component);
    }

    public U addScalar(float value) {
        return addScalar((x, y) -> value);
    }

    protected U setComponent(ToFloatBiIntFunction valueFunction, int component) {
        return apply((x, y) -> setComponentAt(x, y, valueFunction.apply(x, y), component));
    }

    public U subtractScalar(float value) {
        return subtractScalar((x, y) -> value);
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

    public U multiplyScalar(float value) {
        return multiplyScalar((x, y) -> value);
    }

    protected U addComponent(ToFloatBiIntFunction valueFunction, int component) {
        return apply((x, y) -> addComponentAt(x, y, valueFunction.apply(x, y), component));
    }

    public U divideScalar(float value) {
        return divideScalar((x, y) -> value);
    }

    protected U subtractComponent(ToFloatBiIntFunction valueFunction, int component) {
        return enqueue(() -> apply((x, y) -> subtractComponentAt(x, y, valueFunction.apply(x, y), component)));
    }

    public U clampComponentMin(float floor) {
        return set((x, y) -> get(x, y).clampMin(floor));
    }

    public U clampComponentMax(float ceiling) {
        return set((x, y) -> get(x, y).clampMax(ceiling));
    }

    public U randomize(float scale) {
        return setWithSymmetry(SymmetryType.SPAWN, (x, y) -> getZeroValue().randomize(random, scale));
    }

    public U randomize(float minValue, float maxValue) {
        return setWithSymmetry(SymmetryType.SPAWN, (x, y) -> getZeroValue().randomize(random, minValue, maxValue));
    }

    public U normalize() {
        return enqueue(dependencies -> set((x, y) -> get(x, y).normalize()));
    }

    public FloatMask copyAsDotProduct(U other) {
        return copyAsDotProduct(other, getName() + "dot" + other.getName());
    }

    public FloatMask copyAsDotProduct(U other, String name) {
        assertCompatibleMask(other);
        return new FloatMask(this, other, name);
    }

    public FloatMask copyAsDotProduct(T vector) {
        return copyAsDotProduct(vector, getName() + "Dot");
    }

    public FloatMask copyAsDotProduct(T vector, String name) {
        assertMatchingDimension(vector.getDimension());
        return new FloatMask(this, vector, name);
    }

    public U blurComponent(int radius, int component) {
        int[][] innerCount = getComponentInnerCount(component);
        return setComponent((x, y) -> calculateComponentAreaAverage(radius, x, y, innerCount) / 1000f, component);
    }

    public U multiplyComponent(ToFloatBiIntFunction valueFunction, int component) {
        return apply((x, y) -> multiplyComponentAt(x, y, valueFunction.apply(x, y), component));
    }

    public U divideComponent(ToFloatBiIntFunction valueFunction, int component) {
        return apply((x, y) -> divideComponentAt(x, y, valueFunction.apply(x, y), component));
    }

    public U addScalarWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> addScalarAt(sx, sy, value));
        });
    }

    public U subtractScalarWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> subtractScalarAt(sx, sy, value));
        });
    }

    public U multiplyScalarWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> multiplyScalarAt(sx, sy, value));
        });
    }

    public U divideScalarWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> divideScalarAt(sx, sy, value));
        });
    }

    public U addComponent(float value, int component) {
        return addComponent((x, y) -> value, component);
    }

    public U setComponent(FloatMask other, int component) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            setComponent(source::getPrimitive, component);
        }, other);
    }

    public U setComponentWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction, int component) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> setComponentAt(sx, sy, value, component));
        });
    }

    public U addComponentWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction, int component) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> addComponentAt(sx, sy, value, component));
        });
    }

    public U addComponent(BooleanMask other, float value, int component) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            addComponent((x, y) -> source.get(x, y) ? value : 0, component);
        }, other);
    }

    public U addComponent(FloatMask other, int component) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            addComponent(source::get, component);
        }, other);
    }

    public U subtractComponent(float value, int component) {
        return subtractComponent((x, y) -> value, component);
    }

    public U subtractComponentWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction,
                                           int component) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> subtractComponentAt(sx, sy, value, component));
        });
    }

    public U multiplyComponentWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction,
                                           int component) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> multiplyComponentAt(sx, sy, value, component));
        });
    }

    public U subtractComponent(BooleanMask other, float value, int component) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            subtractComponent((x, y) -> source.get(x, y) ? value : 0, component);
        }, other);
    }

    public U subtractComponent(FloatMask other, int component) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            subtractComponent(source::get, component);
        }, other);
    }

    public U multiplyComponent(float value, int component) {
        return multiplyComponent((x, y) -> value, component);
    }

    protected U divideComponentWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction,
                                            int component) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> divideComponentAt(sx, sy, value, component));
        });
    }

    public U multiplyComponent(BooleanMask other, float value, int component) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            multiplyComponent((x, y) -> source.get(x, y) ? value : 0, component);
        }, other);
    }

    public U multiplyComponent(FloatMask other, int component) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            multiplyComponent(source::get, component);
        }, other);
    }

    public U divideComponent(float value, int component) {
        return divideComponent((x, y) -> value, component);
    }

    public U divideComponent(BooleanMask other, float value, int component) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            divideComponent((x, y) -> source.get(x, y) ? value : 0, component);
        }, other);
    }

    public U divideComponent(FloatMask other, int component) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            divideComponent(source::get, component);
        }, other);
    }

    public FloatMask copyAsComponentMask(int component) {
        return copyAsComponentMask(component, getName() + "Component" + component);
    }

    public FloatMask copyAsComponentMask(int component, String name) {
        return new FloatMask(this, component, name);
    }

    public FloatMask[] splitComponentMasks() {
        int dimension = getZeroValue().getDimension();
        String name = getName();
        FloatMask[] components = new FloatMask[dimension];
        for (int i = 0; i < dimension; ++i) {
            components[i] = new FloatMask(getSize(), getNextRandomGenerator(), symmetrySettings,
                                          name + "Component" + i);
        }

        enqueue(dependencies -> {
            Mask<?, ?>[] sources = dependencies.subList(0, dimension).toArray(Mask[]::new);
            apply((x, y) -> {
                for (int i = 0; i < dimension; ++i) {
                    ((FloatMask) sources[i]).setPrimitive(x, y, get(x, y).get(i));
                }
            });
        }, components);

        return components;
    }

    public U setComponentWithOffset(FloatMask other, int component, int xOffset, int yOffset, boolean center,
                                    boolean wrapEdges) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            applyComponentWithOffset(source, this::setComponentAt, component, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    public U addComponentWithOffset(FloatMask other, int component, int xOffset, int yOffset, boolean center,
                                    boolean wrapEdges) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            applyComponentWithOffset(source, this::addComponentAt, component, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    public U subtractComponentWithOffset(FloatMask other, int component, int xOffset, int yOffset, boolean center,
                                         boolean wrapEdges) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            applyComponentWithOffset(source, this::subtractComponentAt, component, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    public U multiplyComponentWithOffset(FloatMask other, int component, int xOffset, int yOffset, boolean center,
                                         boolean wrapEdges) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            applyComponentWithOffset(source, this::multiplyComponentAt, component, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    public U divideComponentWithOffset(FloatMask other, int component, int xOffset, int yOffset, boolean center,
                                       boolean wrapEdges) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            applyComponentWithOffset(source, this::divideComponentAt, component, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    private U applyComponentWithOffset(FloatMask other, BiIntFloatIntConsumer action, int component, int xOffset,
                                       int yOffset, boolean center, boolean wrapEdges) {
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
                        int shiftX = coordinateXMap.get(x);
                        int shiftY = coordinateYMap.get(y);
                        if (inBounds(shiftX, shiftY, size)) {
                            float value = other.getPrimitive(x, y);
                            applyAtSymmetryPoints(shiftX, shiftY, SymmetryType.SPAWN,
                                                  (sx, sy) -> action.accept(sx, sy, value, component));
                        }
                    });
                } else {
                    applyAtSymmetryPointsWithOutOfBounds(xOffset, yOffset, SymmetryType.SPAWN, (sx, sy) -> {
                        Map<Integer, Integer> coordinateXMap = getShiftedCoordinateMap(sx, center, wrapEdges, otherSize,
                                                                                       size);
                        Map<Integer, Integer> coordinateYMap = getShiftedCoordinateMap(sy, center, wrapEdges, otherSize,
                                                                                       size);
                        other.apply((x, y) -> {
                            int shiftX = coordinateXMap.get(x);
                            int shiftY = coordinateYMap.get(y);
                            if (inBounds(shiftX, shiftY, size)) {
                                action.accept(shiftX, shiftY, other.getPrimitive(x, y), component);
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
                    int shiftX = coordinateXMap.get(x);
                    int shiftY = coordinateYMap.get(y);
                    if (inBounds(shiftX, shiftY, otherSize)) {
                        action.accept(x, y, other.getPrimitive(shiftX, shiftY), component);
                    }
                });
            }
        });
    }
}
