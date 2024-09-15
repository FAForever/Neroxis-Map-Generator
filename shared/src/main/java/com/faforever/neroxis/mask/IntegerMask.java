package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.functional.ToIntBiIntFunction;
import com.faforever.neroxis.util.functional.TriIntConsumer;
import com.faforever.neroxis.util.vector.Vector2;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class IntegerMask extends PrimitiveMask<Integer, IntegerMask> {
    private int[][] mask;

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    /**
     * Create a new integer mask
     *
     * @param size             Size of the mask
     * @param seed             Random seed of the mask
     * @param symmetrySettings symmetrySettings to enforce on the mask
     * @param name             name of the mask
     * @param parallel         whether to parallelize mask operations
     */
    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    IntegerMask(BooleanMask other, int low, int high) {
        this(other, low, high, null);
    }

    public IntegerMask(BooleanMask other, int low, int high, String name) {
        this(other.getSize(), other.getNextSeed(), other.getSymmetrySettings(), name, other.isParallel());
        enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            apply((x, y) -> setPrimitive(x, y, source.getPrimitive(x, y) ? high : low));
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
        apply((x, y) -> setPrimitive(x, y, imageBuffer.getElem(x + y * size)));
    }

    public IntegerMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings) {
        this(sourceImage, seed, symmetrySettings, null, false);
    }

    protected IntegerMask(IntegerMask other, String name, boolean immutable) {
        super(other, name, immutable);
    }

    private void setPrimitive(int x, int y, int value) {
        mask[x][y] = value;
    }

    @Override
    protected void copyValue(int sourceX, int sourceY, int destX, int destY) {
        setPrimitive(destX, destY, getPrimitive(sourceX, sourceY));
    }

    public int getPrimitive(Vector2 location) {
        return getPrimitive(StrictMath.round(location.getX()), StrictMath.round(location.getY()));
    }

    public int getPrimitive(int x, int y) {
        return mask[x][y];
    }

    private void setPrimitive(Vector2 location, int value) {
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
                (x, y) -> setPrimitive(x, y, transformAverage(calculateAreaAverageAsInts(radius, x, y, innerCount))));
    }

    @Override
    public IntegerMask blur(int radius, BooleanMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask limiter = (BooleanMask) dependencies.getFirst();
            int[][] innerCount = getInnerCount();
            apply((x, y) -> {
                if (limiter.get(x, y)) {
                    setPrimitive(x, y, transformAverage(calculateAreaAverageAsInts(radius, x, y, innerCount)));
                }
            });
        }, other);
    }

    @Override
    protected IntegerMask copyFrom(IntegerMask other) {
        return enqueue(dependencies -> fill(((IntegerMask) dependencies.getFirst()).mask), other);
    }

    @Override
    protected void initializeMask(int size) {
        enqueue(() -> mask = new int[size][size]);
    }

    @Override
    protected int getImmediateSize() {
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
        loopInSymmetryRegion(SymmetryType.SPAWN, (x, y) -> bytes.putInt(getPrimitive(x, y)));
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
    protected Integer getZeroValue() {
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
                apply((x, y) -> {
                    int value = oldMask[coordinateMap.get(x)][coordinateMap.get(y)];
                    setPrimitive(x, y, value);
                });
            }
        });
    }

    private int transformAverage(float value) {
        return StrictMath.round(value);
    }

    private IntegerMask fill(int[][] maskToFillFrom) {
        assertNotPipelined();
        int maskSize = maskToFillFrom.length;
        mask = new int[maskSize][maskSize];
        for (int r = 0; r < maskSize; ++r) {
            System.arraycopy(maskToFillFrom[r], 0, mask[r], 0, maskSize);
        }
        return this;
    }

    private IntegerMask add(ToIntBiIntFunction valueFunction) {
        return apply((x, y) -> addPrimitiveAt(x, y, valueFunction.apply(x, y)));
    }

    private IntegerMask subtract(ToIntBiIntFunction valueFunction) {
        return apply((x, y) -> subtractPrimitiveAt(x, y, valueFunction.apply(x, y)));
    }

    private IntegerMask multiply(ToIntBiIntFunction valueFunction) {
        return apply((x, y) -> multiplyPrimitiveAt(x, y, valueFunction.apply(x, y)));
    }

    private IntegerMask divide(ToIntBiIntFunction valueFunction) {
        return apply((x, y) -> dividePrimitiveAt(x, y, valueFunction.apply(x, y)));
    }

    private void addPrimitiveAt(int x, int y, int value) {
        mask[x][y] += value;
    }

    private void subtractPrimitiveAt(int x, int y, int value) {
        mask[x][y] -= value;
    }

    private void multiplyPrimitiveAt(int x, int y, int value) {
        mask[x][y] *= value;
    }

    private void dividePrimitiveAt(int x, int y, int value) {
        mask[x][y] /= value;
    }

    public BufferedImage writeToImage(BufferedImage image, float scaleFactor) {
        assertSize(image.getHeight());
        int size = getSize();
        DataBuffer imageBuffer = image.getRaster().getDataBuffer();
        loop((x, y) -> imageBuffer.setElem(x + y * size, (int) (getPrimitive(x, y) * scaleFactor)));
        return image;
    }

    @Override
    protected int[][] getInnerCount() {
        int[][] innerCount = new int[getSize()][getSize()];
        apply((x, y) -> calculateInnerValue(innerCount, x, y, getPrimitive(x, y)));
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
            IntegerMask source = (IntegerMask) dependencies.getFirst();
            apply((x, y) -> mask[x][y] += source.mask[x][y]);
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
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    addPrimitiveAt(x, y, val);
                }
            });
        }, other);
    }

    @Override
    public IntegerMask add(BooleanMask other, IntegerMask values) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            IntegerMask val = (IntegerMask) dependencies.get(1);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    addPrimitiveAt(x, y, val.getPrimitive(x, y));
                }
            });
        }, other, values);
    }

    @Override
    public IntegerMask addWithOffset(IntegerMask other, int xOffset, int yOffset, boolean center, boolean wrapEdges) {
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.getFirst();
            applyWithOffset(source, (TriIntConsumer) this::addPrimitiveAt, xOffset, yOffset, center, wrapEdges);
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
            IntegerMask source = (IntegerMask) dependencies.getFirst();
            apply((x, y) -> mask[x][y] -= source.mask[x][y]);
        }, other);
    }

    @Override
    public IntegerMask subtract(BooleanMask other, Integer values) {
        assertCompatibleMask(other);
        int val = values;
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
    public IntegerMask subtract(BooleanMask other, IntegerMask values) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            BooleanMask source = (BooleanMask) dependencies.get(0);
            IntegerMask val = (IntegerMask) dependencies.get(1);
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    subtractPrimitiveAt(x, y, val.getPrimitive(x, y));
                }
            });
        }, other, values);
    }

    @Override
    public IntegerMask subtractWithOffset(IntegerMask other, int xOffset, int yOffset, boolean center,
                                          boolean wrapEdges) {
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.getFirst();
            applyWithOffset(source, (TriIntConsumer) this::subtractPrimitiveAt, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    @Override
    public IntegerMask multiply(IntegerMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.getFirst();
            apply((x, y) -> mask[x][y] *= source.mask[x][y]);
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
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    multiplyPrimitiveAt(x, y, val);
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
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    multiplyPrimitiveAt(x, y, val.getPrimitive(x, y));
                }
            });
        }, other, value);
    }

    @Override
    public IntegerMask multiplyWithOffset(IntegerMask other, int xOffset, int yOffset, boolean center,
                                          boolean wrapEdges) {
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.getFirst();
            applyWithOffset(source, (TriIntConsumer) this::multiplyPrimitiveAt, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    @Override
    public IntegerMask divide(IntegerMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.getFirst();
            apply((x, y) -> mask[x][y] /= source.mask[x][y]);
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
            BooleanMask source = (BooleanMask) dependencies.getFirst();
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    dividePrimitiveAt(x, y, val);
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
            apply((x, y) -> {
                if (source.getPrimitive(x, y)) {
                    dividePrimitiveAt(x, y, val.getPrimitive(x, y));
                }
            });
        }, other, value);
    }

    @Override
    public IntegerMask divideWithOffset(IntegerMask other, int xOffset, int yOffset, boolean center,
                                        boolean wrapEdges) {
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.getFirst();
            applyWithOffset(source, (TriIntConsumer) this::dividePrimitiveAt, xOffset, yOffset, center, wrapEdges);
        }, other);
    }

    public IntegerMask addPrimitiveWithSymmetry(SymmetryType symmetryType, ToIntBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> addPrimitiveAt(x, y, valueFunction.apply(x, y)));
    }

    public IntegerMask subtractPrimitiveWithSymmetry(SymmetryType symmetryType, ToIntBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> subtractPrimitiveAt(x, y, valueFunction.apply(x, y)));
    }

    public IntegerMask multiplyPrimitiveWithSymmetry(SymmetryType symmetryType, ToIntBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> multiplyPrimitiveAt(x, y, valueFunction.apply(x, y)));
    }

    public IntegerMask dividePrimitiveWithSymmetry(SymmetryType symmetryType, ToIntBiIntFunction valueFunction) {
        return applyWithSymmetry(symmetryType, (x, y) -> dividePrimitiveAt(x, y, valueFunction.apply(x, y)));
    }

    public IntegerMask applyWithOffset(IntegerMask other, TriIntConsumer action, int xOffset, int yOffset,
                                       boolean center, boolean wrapEdges) {
        return enqueue(() -> {
            int size = getSize();
            int otherSize = other.getSize();
            int smallerSize = StrictMath.min(size, otherSize);
            int biggerSize = StrictMath.max(size, otherSize);
            if (smallerSize == otherSize) {
                Map<Integer, Integer> coordinateXMap = getShiftedCoordinateMap(xOffset, center, wrapEdges,
                                                                               otherSize, size);
                Map<Integer, Integer> coordinateYMap = getShiftedCoordinateMap(yOffset, center, wrapEdges,
                                                                               otherSize, size);
                other.apply((x, y) -> {
                    int shiftX = coordinateXMap.get(x);
                    int shiftY = coordinateYMap.get(y);
                    if (inBounds(shiftX, shiftY, size)) {
                        action.accept(shiftX, shiftY, other.getPrimitive(x, y));
                    }
                });
            } else {
                Map<Integer, Integer> coordinateXMap = getShiftedCoordinateMap(xOffset, center, wrapEdges, size,
                                                                               otherSize);
                Map<Integer, Integer> coordinateYMap = getShiftedCoordinateMap(yOffset, center, wrapEdges, size,
                                                                               otherSize);
                apply((x, y) -> {
                    int shiftX = coordinateXMap.get(x);
                    int shiftY = coordinateYMap.get(y);
                    if (inBounds(shiftX, shiftY, otherSize)) {
                        action.accept(x, y, other.getPrimitive(shiftX, shiftY));
                    }
                });
            }
            applySymmetry(SymmetryType.SPAWN);
        });
    }
}
