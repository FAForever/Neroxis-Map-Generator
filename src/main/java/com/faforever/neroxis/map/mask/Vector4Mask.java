package com.faforever.neroxis.map.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.Vector4;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public strictfp class Vector4Mask extends VectorMask<Vector4, Vector4Mask> {

    public Vector4Mask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public Vector4Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public Vector4Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public Vector4Mask(Vector4Mask other, Long seed) {
        this(other, seed, null);
    }

    public Vector4Mask(Vector4Mask other, Long seed, String name) {
        super(other, seed, name);
    }

    public Vector4Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor) {
        super(sourceImage, seed, symmetrySettings, scaleFactor, null, false);
    }

    public Vector4Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name) {
        super(sourceImage, seed, symmetrySettings, scaleFactor, name, false);
    }

    public Vector4Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name, boolean parallel) {
        super(sourceImage, seed, symmetrySettings, scaleFactor, name, parallel);
    }

    @Override
    protected Vector4 createValue(float scaleFactor, float... components) {
        assertMatchingDimension(components.length);
        return new Vector4(components[0], components[1], components[2], components[3]).multiply(scaleFactor);
    }

    @Override
    protected Vector4[][] getInnerCount() {
        Vector4[][] innerCount = new Vector4[getSize()][getSize()];
        apply((x, y) -> calculateInnerValue(innerCount, x, y, get(x, y)));
        return innerCount;
    }

    @Override
    protected Vector4[][] getEmptyMask(int size) {
        Vector4[][] empty = new Vector4[size][size];
        maskFill(empty, getZeroValue());
        return empty;
    }

    @Override
    protected Vector4 getZeroValue() {
        return new Vector4(0f, 0f, 0f, 0f);
    }

    @Override
    public Vector4Mask copy() {
        if (random != null) {
            return new Vector4Mask(this, random.nextLong(), getName() + "Copy");
        } else {
            return new Vector4Mask(this, null, getName() + "Copy");
        }
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        int size = getSize();
        ByteBuffer bytes = ByteBuffer.allocate(size * size * 4 * 4);
        applyWithSymmetry(SymmetryType.SPAWN, (x, y) -> {
            Vector4 value = get(x, y);
            bytes.putFloat(value.getX());
            bytes.putFloat(value.getY());
            bytes.putFloat(value.getZ());
            bytes.putFloat(value.getW());
        });
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }
}
