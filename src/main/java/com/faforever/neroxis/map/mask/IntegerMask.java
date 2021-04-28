package com.faforever.neroxis.map.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public strictfp class IntegerMask extends PrimitiveMask<Integer, IntegerMask> {

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public IntegerMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public IntegerMask(IntegerMask other, Long seed) {
        super(other, seed);
    }

    public IntegerMask(IntegerMask other, Long seed, String name) {
        super(other, seed, name);
    }

    public IntegerMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings) {
        this(sourceImage, seed, symmetrySettings, null, false);
    }

    public IntegerMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(sourceImage, seed, symmetrySettings, name, false);
    }

    public IntegerMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(sourceImage.getHeight(), seed, symmetrySettings, name, parallel);
        DataBuffer imageBuffer = sourceImage.getRaster().getDataBuffer();
        int size = getSize();
        enqueue(() -> set((x, y) -> imageBuffer.getElem(x + y * size)));
    }

    @Override
    protected Integer[][] getEmptyMask(int size) {
        Integer[][] empty = new Integer[size][size];
        maskFill(empty, getZeroValue());
        return empty;
    }

    @Override
    protected Integer transformAverage(float value) {
        return StrictMath.round(value);
    }

    @Override
    public IntegerMask copy() {
        if (random != null) {
            return new IntegerMask(this, random.nextLong());
        } else {
            return new IntegerMask(this, null);
        }
    }

    @Override
    public Integer getZeroValue() {
        return 0;
    }

    @Override
    public Integer getSum() {
        return Arrays.stream(mask).flatMap(Arrays::stream).reduce(Integer::sum).orElseThrow(() -> new IllegalStateException("Empty Mask"));
    }

    @Override
    public void addValueAt(int x, int y, Integer value) {
        mask[x][y] += value;
    }

    @Override
    public void subtractValueAt(int x, int y, Integer value) {
        mask[x][y] -= value;
    }

    @Override
    public void multiplyValueAt(int x, int y, Integer value) {
        mask[x][y] *= value;
    }

    @Override
    public void divideValueAt(int x, int y, Integer value) {
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
        apply((x, y) -> imageBuffer.setElem(x + y * size, (int) (get(x, y) * scaleFactor)));
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
