package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
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
        enqueue(() -> set((x, y) -> imageBuffer.getElem(x + y * size)));
    }

    @Override
    protected Integer transformAverage(float value) {
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
        return mask[x][y];
    }

    @Override
    protected void set(int x, int y, Integer value) {
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
                int value = get(0, 0);
                initializeMask(newSize);
                fill(value);
            } else if (oldSize != newSize) {
                int[][] oldMask = mask;
                initializeMask(newSize);
                Map<Integer, Integer> coordinateMap = getSymmetricScalingCoordinateMap(oldSize, newSize);
                set((x, y) -> oldMask[coordinateMap.get(x)][coordinateMap.get(y)]);
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
    public Integer getAvg() {
        int size = getSize();
        return getSum() / size / size;
    }

    @Override
    protected int[][] getInnerCount() {
        int[][] innerCount = new int[getSize()][getSize()];
        apply((x, y) -> calculateInnerValue(innerCount, x, y, get(x, y)));
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
        loop((x, y) -> imageBuffer.setElem(x + y * size, (int) (get(x, y) * scaleFactor)));
        return image;
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        ByteBuffer bytes = ByteBuffer.allocate(getSize() * getSize() * 4);
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> bytes.putInt(get(x, y)));
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }
}