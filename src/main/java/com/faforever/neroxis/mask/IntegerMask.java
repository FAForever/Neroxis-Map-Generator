package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.vector.Vector2;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public strictfp class IntegerMask extends PrimitiveMask<Integer, IntegerMask> {
    private int[][] mask;

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public IntegerMask(IntegerMask other) {
        this(other, null);
    }

    public IntegerMask(IntegerMask other, String name) {
        super(other, name);
    }

    @Override
    protected void initializeMask(int size) {
        mask = new int[size][size];
    }

    public IntegerMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings) {
        this(sourceImage, seed, symmetrySettings, null, false);
    }

    public IntegerMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(sourceImage, seed, symmetrySettings, name, false);
    }

    public IntegerMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        this(sourceImage.getHeight(), seed, symmetrySettings, name, parallel);
        DataBuffer imageBuffer = sourceImage.getRaster().getDataBuffer();
        int size = getSize();
        enqueue(() -> apply(point -> setPrimitive(point, imageBuffer.getElem(point.x + point.y * size))));
    }

    private int transformAverage(float value) {
        return StrictMath.round(value);
    }

    @Override
    public Integer getZeroValue() {
        return 0;
    }

    @Override
    protected IntegerMask fill(Integer value) {
        int maskSize = mask.length;
        mask[0][0] = value;
        for (int i = 1; i < maskSize; i += i) {
            System.arraycopy(mask[0], 0, mask[0], i, StrictMath.min((maskSize - i), i));
        }
        for (int r = 1; r < maskSize; ++r) {
            System.arraycopy(mask[0], 0, mask[r], 0, maskSize);
        }
        return this;
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

    @Override
    public Integer get(int x, int y) {
        return getPrimitive(x, y);
    }

    @Override
    protected void set(int x, int y, Integer value) {
        setPrimitive(x, y, value);
    }

    public int getPrimitive(Vector2 location) {
        return getPrimitive(StrictMath.round(location.getX()), StrictMath.round(location.getY()));
    }

    protected void setPrimitive(Vector2 location, int value) {
        setPrimitive(StrictMath.round(location.getX()), StrictMath.round(location.getY()), value);
    }

    public int getPrimitive(Point point) {
        return getPrimitive(point.x, point.y);
    }

    protected void setPrimitive(Point point, int value) {
        setPrimitive(point.x, point.y, value);
    }

    public int getPrimitive(int x, int y) {
        return mask[x][y];
    }

    protected void setPrimitive(int x, int y, int value) {
        mask[x][y] = value;
    }

    @Override
    public int getImmediateSize() {
        return mask.length;
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

    @Override
    protected IntegerMask copyFrom(IntegerMask other) {
        return enqueue((dependencies) -> fill(((IntegerMask) dependencies.get(0)).mask), other);
    }

    @Override
    public Integer getSum() {
        return Arrays.stream(mask).flatMapToInt(Arrays::stream).sum();
    }

    @Override
    public Integer getMin() {
        return Arrays.stream(mask).flatMapToInt(Arrays::stream).min().orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    @Override
    public Integer getMax() {
        return Arrays.stream(mask).flatMapToInt(Arrays::stream).max().orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    @Override
    protected void addValueAt(int x, int y, Integer value) {
        mask[x][y] += value;
    }

    @Override
    protected void subtractValueAt(int x, int y, Integer value) {
        mask[x][y] -= value;
    }

    @Override
    protected void multiplyValueAt(int x, int y, Integer value) {
        mask[x][y] *= value;
    }

    @Override
    protected void divideValueAt(int x, int y, Integer value) {
        mask[x][y] /= value;
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
    public IntegerMask subtract(IntegerMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.get(0);
            apply(point -> mask[point.x][point.y] -= source.mask[point.x][point.y]);
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
    public IntegerMask divide(IntegerMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            IntegerMask source = (IntegerMask) dependencies.get(0);
            apply(point -> mask[point.x][point.y] /= source.mask[point.x][point.y]);
        }, other);
    }

    @Override
    public IntegerMask blur(int radius) {
        return enqueue(() -> {
            int[][] innerCount = getInnerCount();
            apply(point -> setPrimitive(point, transformAverage(calculateAreaAverageAsInts(radius, point, innerCount))));
        });
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
    public Integer getAvg() {
        int size = getSize();
        return getSum() / size / size;
    }

    @Override
    protected int[][] getInnerCount() {
        int[][] innerCount = new int[getSize()][getSize()];
        apply(point -> calculateInnerValue(innerCount, point, getPrimitive(point)));
        return innerCount;
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        writeToImage(image, 255f / getMax());
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
        loop(point -> imageBuffer.setElem(point.x + point.y * size, (int) (getPrimitive(point) * scaleFactor)));
        return image;
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        ByteBuffer bytes = ByteBuffer.allocate(getSize() * getSize() * 4);
        applyWithSymmetry(SymmetryType.SPAWN, point -> bytes.putInt(getPrimitive(point)));
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }
}
