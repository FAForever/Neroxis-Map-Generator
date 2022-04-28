package com.faforever.neroxis.mask;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.annotations.GraphParameter;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.vector.Vector2;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public strictfp class IntegerMask extends PrimitiveMask<Integer, IntegerMask> {

    private int[][] mask;

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    @GraphMethod
    @GraphParameter(name = "name", value = "identifier")
    @GraphParameter(name = "parallel", value = "true")
    @GraphParameter(name = "seed", value = "random.nextLong()")
    @GraphParameter(name = "symmetrySettings", value = "symmetrySettings")
    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    protected IntegerMask(IntegerMask other) {
        this(other, null);
    }

    protected IntegerMask(IntegerMask other, String name) {
        super(other, name);
    }

    protected IntegerMask(BooleanMask other, int low, int high) {
        this(other, low, high, null);
    }

    protected IntegerMask(BooleanMask other, int low, int high, String name) {
        this(other.getSize(), other.getNextSeed(), other.getSymmetrySettings(), name, other.isParallel());
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            apply(point -> setPrimitive(point, source.getPrimitive(point) ? high : low));
        }, other);
    }

    public IntegerMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(sourceImage, seed, symmetrySettings, name, false);
    }

    public IntegerMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, String name,
                       boolean parallel) {
        this(sourceImage.getHeight(), seed, symmetrySettings, name, parallel);
        DataBuffer imageBuffer = sourceImage.getRaster().getDataBuffer();
        int size = getSize();
        apply(point -> setPrimitive(point, imageBuffer.getElem(point.x + point.y * size)));
    }

    public IntegerMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings) {
        this(sourceImage, seed, symmetrySettings, null, false);
    }

    protected void setPrimitive(Point point, int value) {
        setPrimitive(point.x, point.y, value);
    }

    protected void setPrimitive(int x, int y, int value) {
        mask[x][y] = value;
    }

    public int getPrimitive(Vector2 location) {
        return getPrimitive(StrictMath.round(location.getX()), StrictMath.round(location.getY()));
    }

    public int getPrimitive(int x, int y) {
        return mask[x][y];
    }

    protected void setPrimitive(Vector2 location, int value) {
        setPrimitive(StrictMath.round(location.getX()), StrictMath.round(location.getY()), value);
    }

    @Override
    public Integer getMin() {
        return Arrays.stream(mask)
                     .flatMapToInt(Arrays::stream)
                     .min()
                     .orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    @Override
    public Integer getMax() {
        return Arrays.stream(mask)
                     .flatMapToInt(Arrays::stream)
                     .max()
                     .orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    @Override
    public IntegerMask blur(int radius) {
        int[][] innerCount = getInnerCount();
        return apply(
                point -> setPrimitive(point, transformAverage(calculateAreaAverageAsInts(radius, point, innerCount))));
    }

    @Override
    public IntegerMask blur(int radius, BooleanMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask limiter = (BooleanMask) dependencies.get(0);
            int[][] innerCount = getInnerCount();
            apply(point -> {
                if (limiter.get(point)) {
                    setPrimitive(point, transformAverage(calculateAreaAverageAsInts(radius, point, innerCount)));
                }
            });
        }, other);
    }

    @Override
    protected IntegerMask copyFrom(IntegerMask other) {
        return enqueue(dependencies -> fill(((IntegerMask) dependencies.get(0)).mask), other);
    }

    @Override
    protected void initializeMask(int size) {
        enqueue(() -> mask = new int[size][size]);
    }

    @Override
    public int getImmediateSize() {
        return mask.length;
    }

    @Override
    public BufferedImage writeToImage(BufferedImage image) {
        return writeToImage(image, 1f);
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        writeToImage(image, 255f / getMax());
        return image;
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        ByteBuffer bytes = ByteBuffer.allocate(getSize() * getSize() * 4);
        loopWithSymmetry(SymmetryType.SPAWN, point -> bytes.putInt(getPrimitive(point)));
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }

    @Override
    public Integer get(int x, int y) {
        return getPrimitive(x, y);
    }

    @Override
    protected void set(int x, int y, Integer value) {
        setPrimitive(x, y, value);
    }

    @Override
    protected IntegerMask fill(Integer value) {
        return enqueue(() -> {
            int maskSize = mask.length;
            mask[0][0] = value;
            for (int i = 1; i < maskSize; i += i) {
                System.arraycopy(mask[0], 0, mask[0], i, StrictMath.min((maskSize - i), i));
            }
            for (int r = 1; r < maskSize; ++r) {
                System.arraycopy(mask[0], 0, mask[r], 0, maskSize);
            }
        });
    }

    @Override
    public Integer getZeroValue() {
        return 0;
    }

    @Override
    protected IntegerMask setSizeInternal(int newSize) {
        return enqueue(() -> {
            int oldSize = getSize();
            if (oldSize == 1) {
                int value = getPrimitive(0, 0);
                initializeMask(newSize);
                fill(value);
            } else if (oldSize != newSize) {
                int[][] oldMask = mask;
                initializeMask(newSize);
                Map<Integer, Integer> coordinateMap = getSymmetricScalingCoordinateMap(oldSize, newSize);
                apply(point -> setPrimitive(point, oldMask[coordinateMap.get(point.x)][coordinateMap.get(point.y)]));
            }
        });
    }

    private int transformAverage(float value) {
        return StrictMath.round(value);
    }

    protected IntegerMask fill(int[][] maskToFillFrom) {
        assertNotPipelined();
        int maskSize = maskToFillFrom.length;
        mask = new int[maskSize][maskSize];
        for (int r = 0; r < maskSize; ++r) {
            System.arraycopy(maskToFillFrom[r], 0, mask[r], 0, maskSize);
        }
        return this;
    }

    protected IntegerMask add(ToIntFunction<Point> valueFunction) {
        return apply(point -> addPrimitiveAt(point, valueFunction.applyAsInt(point)));
    }

    protected IntegerMask subtract(ToIntFunction<Point> valueFunction) {
        return apply(point -> subtractPrimitiveAt(point, valueFunction.applyAsInt(point)));
    }

    protected IntegerMask multiply(ToIntFunction<Point> valueFunction) {
        return apply(point -> multiplyPrimitiveAt(point, valueFunction.applyAsInt(point)));
    }

    protected IntegerMask divide(ToIntFunction<Point> valueFunction) {
        return apply(point -> dividePrimitiveAt(point, valueFunction.applyAsInt(point)));
    }

    protected void addPrimitiveAt(Point point, float value) {
        addPrimitiveAt(point.x, point.y, value);
    }

    protected void addPrimitiveAt(int x, int y, float value) {
        mask[x][y] += value;
    }

    protected void subtractPrimitiveAt(Point point, float value) {
        subtractPrimitiveAt(point.x, point.y, value);
    }

    protected void subtractPrimitiveAt(int x, int y, float value) {
        mask[x][y] -= value;
    }

    protected void multiplyPrimitiveAt(Point point, float value) {
        multiplyPrimitiveAt(point.x, point.y, value);
    }

    protected void multiplyPrimitiveAt(int x, int y, float value) {
        mask[x][y] *= value;
    }

    protected void dividePrimitiveAt(Point point, float value) {
        dividePrimitiveAt(point.x, point.y, value);
    }

    public BufferedImage writeToImage(BufferedImage image, float scaleFactor) {
        assertSize(image.getHeight());
        int size = getSize();
        DataBuffer imageBuffer = image.getRaster().getDataBuffer();
        loop(point -> imageBuffer.setElem(point.x + point.y * size, (int) (getPrimitive(point) * scaleFactor)));
        return image;
    }

    protected void dividePrimitiveAt(int x, int y, float value) {
        mask[x][y] /= value;
    }

    @Override
    protected int[][] getInnerCount() {
        int[][] innerCount = new int[getSize()][getSize()];
        apply(point -> calculateInnerValue(innerCount, point, getPrimitive(point)));
        return innerCount;
    }

    @Override
    public Integer getSum() {
        return Arrays.stream(mask).flatMapToInt(Arrays::stream).sum();
    }

    @Override
    public IntegerMask add(IntegerMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.get(0);
            apply(point -> mask[point.x][point.y] += source.mask[point.x][point.y]);
        }, other);
    }

    @Override
    protected void addValueAt(int x, int y, Integer value) {
        mask[x][y] += value;
    }

    @Override
    public IntegerMask add(BooleanMask other, Integer value) {
        assertCompatibleMask(other);
        int val = value;
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            apply(point -> {
                if (source.getPrimitive(point)) {
                    addPrimitiveAt(point, val);
                }
            });
        }, other);
    }

    @Override
    public IntegerMask add(BooleanMask other, IntegerMask value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            IntegerMask val = (IntegerMask) dependencies.get(1);
            apply(point -> {
                if (source.getPrimitive(point)) {
                    addPrimitiveAt(point, val.getPrimitive(point));
                }
            });
        }, other, value);
    }

    @Override
    public IntegerMask addWithOffset(IntegerMask other, int xOffset, int yOffset, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.get(0);
            applyWithOffset(source, (ObjIntConsumer<Point>) this::addPrimitiveAt, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    @Override
    protected void subtractValueAt(int x, int y, Integer value) {
        mask[x][y] -= value;
    }

    @Override
    public Integer getAvg() {
        assertNotPipelined();
        int size = getSize();
        return getSum() / size / size;
    }

    @Override
    public IntegerMask subtract(IntegerMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.get(0);
            apply(point -> mask[point.x][point.y] -= source.mask[point.x][point.y]);
        }, other);
    }

    @Override
    public IntegerMask subtract(BooleanMask other, Integer value) {
        assertCompatibleMask(other);
        int val = value;
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            apply(point -> {
                if (source.getPrimitive(point)) {
                    subtractPrimitiveAt(point, val);
                }
            });
        }, other);
    }

    @Override
    public IntegerMask subtract(BooleanMask other, IntegerMask value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            IntegerMask val = (IntegerMask) dependencies.get(1);
            apply(point -> {
                if (source.getPrimitive(point)) {
                    subtractPrimitiveAt(point, val.getPrimitive(point));
                }
            });
        }, other, value);
    }

    @Override
    public IntegerMask subtractWithOffset(IntegerMask other, int xOffset, int yOffset, boolean center,
                                          boolean wrapEdges) {
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.get(0);
            applyWithOffset(source, (ObjIntConsumer<Point>) this::subtractPrimitiveAt, xOffset, yOffset, center,
                            wrapEdges);
        }, other);
    }

    @Override
    public IntegerMask multiply(IntegerMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.get(0);
            apply(point -> mask[point.x][point.y] *= source.mask[point.x][point.y]);
        }, other);
    }

    @Override
    protected void multiplyValueAt(int x, int y, Integer value) {
        mask[x][y] *= value;
    }

    @Override
    public IntegerMask multiply(BooleanMask other, Integer value) {
        assertCompatibleMask(other);
        int val = value;
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            apply(point -> {
                if (source.getPrimitive(point)) {
                    multiplyPrimitiveAt(point, val);
                }
            });
        }, other);
    }

    @Override
    public IntegerMask multiply(BooleanMask other, IntegerMask value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            IntegerMask val = (IntegerMask) dependencies.get(1);
            apply(point -> {
                if (source.getPrimitive(point)) {
                    multiplyPrimitiveAt(point, val.getPrimitive(point));
                }
            });
        }, other, value);
    }

    @Override
    public IntegerMask multiplyWithOffset(IntegerMask other, int xOffset, int yOffset, boolean center,
                                          boolean wrapEdges) {
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.get(0);
            applyWithOffset(source, (ObjIntConsumer<Point>) this::multiplyPrimitiveAt, xOffset, yOffset, center,
                            wrapEdges);
        }, other);
    }

    @Override
    public IntegerMask divide(IntegerMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.get(0);
            apply(point -> mask[point.x][point.y] /= source.mask[point.x][point.y]);
        }, other);
    }

    @Override
    protected void divideValueAt(int x, int y, Integer value) {
        mask[x][y] /= value;
    }

    @Override
    public IntegerMask divide(BooleanMask other, Integer value) {
        assertCompatibleMask(other);
        int val = value;
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            apply(point -> {
                if (source.getPrimitive(point)) {
                    dividePrimitiveAt(point, val);
                }
            });
        }, other);
    }

    @Override
    public IntegerMask divide(BooleanMask other, IntegerMask value) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            IntegerMask val = (IntegerMask) dependencies.get(1);
            apply(point -> {
                if (source.getPrimitive(point)) {
                    dividePrimitiveAt(point, val.getPrimitive(point));
                }
            });
        }, other, value);
    }

    @Override
    public IntegerMask divideWithOffset(IntegerMask other, int xOffset, int yOffset, boolean center,
                                        boolean wrapEdges) {
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.get(0);
            applyWithOffset(source, (ObjIntConsumer<Point>) this::dividePrimitiveAt, xOffset, yOffset, center,
                            wrapEdges);
        }, other);
    }

    public int getPrimitive(Point point) {
        return getPrimitive(point.x, point.y);
    }

    protected IntegerMask addPrimitiveWithSymmetry(SymmetryType symmetryType, ToIntFunction<Point> valueFunction) {
        return applyWithSymmetry(symmetryType, point -> {
            int value = valueFunction.applyAsInt(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> addPrimitiveAt(symPoint, value));
        });
    }

    protected IntegerMask subtractPrimitiveWithSymmetry(SymmetryType symmetryType, ToIntFunction<Point> valueFunction) {
        return applyWithSymmetry(symmetryType, point -> {
            int value = valueFunction.applyAsInt(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> subtractPrimitiveAt(symPoint, value));
        });
    }

    protected IntegerMask multiplyPrimitiveWithSymmetry(SymmetryType symmetryType, ToIntFunction<Point> valueFunction) {
        return applyWithSymmetry(symmetryType, point -> {
            int value = valueFunction.applyAsInt(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> multiplyPrimitiveAt(symPoint, value));
        });
    }

    protected IntegerMask dividePrimitiveWithSymmetry(SymmetryType symmetryType, ToIntFunction<Point> valueFunction) {
        return applyWithSymmetry(symmetryType, point -> {
            int value = valueFunction.applyAsInt(point);
            applyAtSymmetryPoints(point, symmetryType, symPoint -> dividePrimitiveAt(symPoint, value));
        });
    }

    protected IntegerMask applyWithOffset(IntegerMask other, ObjIntConsumer<Point> action, int xOffset, int yOffset,
                                          boolean center, boolean wrapEdges) {
        return enqueue(() -> {
            int size = getSize();
            int otherSize = other.getSize();
            int smallerSize = StrictMath.min(size, otherSize);
            int biggerSize = StrictMath.max(size, otherSize);
            if (smallerSize == otherSize) {
                if (symmetrySettings.getSpawnSymmetry().isPerfectSymmetry()) {
                    Map<Integer, Integer> coordinateXMap = getShiftedCoordinateMap(xOffset, center, wrapEdges,
                                                                                   otherSize, size);
                    Map<Integer, Integer> coordinateYMap = getShiftedCoordinateMap(yOffset, center, wrapEdges,
                                                                                   otherSize, size);
                    other.apply(point -> {
                        int shiftX = coordinateXMap.get(point.x);
                        int shiftY = coordinateYMap.get(point.y);
                        if (inBounds(shiftX, shiftY)) {
                            int value = other.getPrimitive(point);
                            applyAtSymmetryPoints(shiftX, shiftY, SymmetryType.SPAWN,
                                                  symPoint -> action.accept(symPoint, value));
                        }
                    });
                } else {
                    applyAtSymmetryPointsWithOutOfBounds(xOffset, yOffset, SymmetryType.SPAWN, symPoint -> {
                        Map<Integer, Integer> coordinateXMap = getShiftedCoordinateMap(symPoint.x, center, wrapEdges,
                                                                                       otherSize, size);
                        Map<Integer, Integer> coordinateYMap = getShiftedCoordinateMap(symPoint.y, center, wrapEdges,
                                                                                       otherSize, size);
                        other.apply(point -> {
                            int shiftX = coordinateXMap.get(point.x);
                            int shiftY = coordinateYMap.get(point.y);
                            if (inBounds(shiftX, shiftY)) {
                                action.accept(new Point(shiftX, shiftY), other.getPrimitive(point));
                            }
                        });
                    });
                }
            } else {
                Map<Integer, Integer> coordinateXMap = getShiftedCoordinateMap(xOffset, center, wrapEdges, size,
                                                                               otherSize);
                Map<Integer, Integer> coordinateYMap = getShiftedCoordinateMap(yOffset, center, wrapEdges, size,
                                                                               otherSize);
                apply(point -> {
                    int shiftX = coordinateXMap.get(point.x);
                    int shiftY = coordinateYMap.get(point.y);
                    if (other.inBounds(shiftX, shiftY)) {
                        action.accept(point, other.getPrimitive(shiftX, shiftY));
                    }
                });
            }
        });
    }
}
