package com.faforever.neroxis.mask;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
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
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public abstract sealed class VectorMask<T extends Vector<T>, U extends VectorMask<T, U>> extends OperationsMask<T, U> permits NormalMask, Vector2Mask, Vector3Mask, Vector4Mask {
    protected T[][] mask;

    public VectorMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor,
                      String name, boolean parallel) {
        this(sourceImage.getHeight(), seed, symmetrySettings, name, parallel);
        int numImageComponents = sourceImage.getColorModel().getNumComponents();
        assertMatchingDimension(numImageComponents);
        Raster imageRaster = sourceImage.getData();
        set((x, y) -> {
            float[] components = imageRaster.getPixel(x, y, new float[numImageComponents]);
            return createValue(scaleFactor, components);
        });
    }

    public VectorMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public VectorMask(Long seed, String name, FloatMask... components) {
        this(components[0].getSize(), seed, components[0].getSymmetrySettings(), name, components[0].isParallel());
        int numComponents = components.length;
        assertMatchingDimension(numComponents);
        assertCompatibleComponents(components);
        enqueue(dependencies -> {
            List<FloatMask> sources = dependencies.stream().map(dep -> ((FloatMask) dep)).toList();
            apply((x, y) -> {
                T value = getZeroValue();
                for (int i = 0; i < numComponents; ++i) {
                    value.set(i, sources.get(i).get(x, y));
                }
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
            BooleanMask limiter = (BooleanMask) dependencies.get(0);
            T[][] innerCount = getInnerCount();
            set((x, y) -> limiter.get(x, y) ? calculateAreaAverage(radius, x, y, innerCount).round().divide(1000) : get(
                    x, y));
        }, other);
    }

    @Override
    protected U copyFrom(U other) {
        return enqueue(dependencies -> fill(((U) dependencies.get(0)).mask), other);
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
        loopWithSymmetry(SymmetryType.SPAWN, (x, y) -> {
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

    @Override
    public T get(int x, int y) {
        return mask[x][y].copy();
    }

    @Override
    protected void set(int x, int y, T value) {
        mask[x][y] = value.copy();
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
                setWithSymmetry(SymmetryType.SPAWN, (x, y) -> oldMask[coordinateMap.get(x)][coordinateMap.get(y)].copy());
            }
        });
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

    protected void setComponentAt(Vector2 loc, float value, int component) {
        setComponentAt((int) loc.getX(), (int) loc.getY(), value, component);
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
        addScalarAt((int) loc.getX(), (int) loc.getY(), value);
    }

    protected void addScalarAt(int x, int y, float value) {
        mask[x][y].add(value);
    }

    protected void subtractScalarAt(Vector2 loc, float value) {
        subtractScalarAt((int) loc.getX(), (int) loc.getY(), value);
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
            setComponent(
                    (x, y) -> limiter.get(x, y) ? calculateComponentAreaAverage(radius, x, y, innerCount) / 1000f : get(
                            x, y).get(component), component);
        }, other);
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
                     .reduce((first, second) -> first.copy().add(second))
                     .orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    @Override
    protected void addValueAt(int x, int y, T value) {
        mask[x][y].add(value);
    }

    @Override
    protected void subtractValueAt(int x, int y, T value) {
        mask[x][y].subtract(value);
    }

    @Override
    public T getAvg() {
        assertNotPipelined();
        int size = getSize();
        return getSum().divide(size);
    }

    @Override
    protected void multiplyValueAt(int x, int y, T value) {
        mask[x][y].multiply(value);
    }

    @Override
    protected void divideValueAt(int x, int y, T value) {
        mask[x][y].divide(value);
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
        return addScalar((x, y) -> value);
    }

    protected U setComponent(ToFloatBiIntFunction valueFunction, int component) {
        return apply((x, y) -> setComponentAt(x, y, valueFunction.apply(x, y), component));
    }

    @GraphMethod
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

    @GraphMethod
    public U multiplyScalar(float value) {
        return multiplyScalar((x, y) -> value);
    }

    protected U addComponent(ToFloatBiIntFunction valueFunction, int component) {
        return apply((x, y) -> addComponentAt(x, y, valueFunction.apply(x, y), component));
    }

    @GraphMethod
    public U divideScalar(float value) {
        return divideScalar((x, y) -> value);
    }

    protected U subtractComponent(ToFloatBiIntFunction valueFunction, int component) {
        return enqueue(() -> apply((x, y) -> subtractComponentAt(x, y, valueFunction.apply(x, y), component)));
    }

    @GraphMethod
    public U clampComponentMin(float floor) {
        return apply((x, y) -> get(x, y).clampMin(floor));
    }

    @GraphMethod
    public U clampComponentMax(float ceiling) {
        return apply((x, y) -> get(x, y).clampMax(ceiling));
    }

    @GraphMethod
    public U randomize(float scale) {
        return setWithSymmetry(SymmetryType.SPAWN, (x, y) -> getZeroValue().randomize(random, scale));
    }

    @GraphMethod
    public U randomize(float minValue, float maxValue) {
        return setWithSymmetry(SymmetryType.SPAWN, (x, y) -> getZeroValue().randomize(random, minValue, maxValue));
    }

    @GraphMethod
    public U normalize() {
        return enqueue(dependencies -> set((x, y) -> get(x, y).normalize()));
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

    @GraphMethod
    public U addComponent(float value, int component) {
        return addComponent((x, y) -> value, component);
    }

    @GraphMethod
    public U setComponent(FloatMask other, int component) {
        return enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            setComponent(source::get, component);
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

    @GraphMethod
    public U addComponent(BooleanMask other, float value, int component) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            addComponent((x, y) -> source.get(x, y) ? value : 0, component);
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

    @GraphMethod
    public U subtractComponent(BooleanMask other, float value, int component) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            subtractComponent((x, y) -> source.get(x, y) ? value : 0, component);
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
        return multiplyComponent((x, y) -> value, component);
    }

    protected U divideComponentWithSymmetry(SymmetryType symmetryType, ToFloatBiIntFunction valueFunction,
                                            int component) {
        return applyWithSymmetry(symmetryType, (x, y) -> {
            float value = valueFunction.apply(x, y);
            applyAtSymmetryPoints(x, y, symmetryType, (sx, sy) -> divideComponentAt(sx, sy, value, component));
        });
    }

    @GraphMethod
    public U multiplyComponent(BooleanMask other, float value, int component) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            multiplyComponent((x, y) -> source.get(x, y) ? value : 0, component);
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
        return divideComponent((x, y) -> value, component);
    }

    @GraphMethod
    public U divideComponent(BooleanMask other, float value, int component) {
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            divideComponent((x, y) -> source.get(x, y) ? value : 0, component);
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
        int dimension = getZeroValue().getDimension();
        String name = getName();
        FloatMask[] components = new FloatMask[dimension];
        for (int i = 0; i < dimension; ++i) {
            components[i] = new FloatMask(getSize(), getNextSeed(), symmetrySettings, name + "Component" + i,
                                          isParallel());
        }

        enqueue(dependencies -> {
            FloatMask[] sources = dependencies.subList(0, dimension).toArray(FloatMask[]::new);
            apply((x, y) -> {
                for (int i = 0; i < dimension; ++i) {
                    sources[i].setPrimitive(x, y, get(x, y).get(i));
                }
            });
        }, components);

        return components;
    }
}
