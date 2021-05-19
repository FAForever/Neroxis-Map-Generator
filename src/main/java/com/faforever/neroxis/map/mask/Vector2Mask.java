package com.faforever.neroxis.map.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.Vector2;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

public strictfp class Vector2Mask extends VectorMask<Vector2, Vector2Mask> {

    public Vector2Mask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public Vector2Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public Vector2Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public Vector2Mask(Vector2Mask other, Long seed) {
        this(other, seed, null);
    }

    public Vector2Mask(Vector2Mask other, Long seed, String name) {
        super(other, seed, name);
    }

    public Vector2Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor) {
        super(sourceImage, seed, symmetrySettings, scaleFactor, null, false);
    }

    public Vector2Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name) {
        super(sourceImage, seed, symmetrySettings, scaleFactor, name, false);
    }

    public Vector2Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name, boolean parallel) {
        super(sourceImage, seed, symmetrySettings, scaleFactor, name, parallel);
    }

    @Override
    protected Vector2 createValue(float scaleFactor, float... components) {
        assertMatchingDimension(components.length);
        return new Vector2(components[0], components[1]).multiply(scaleFactor);
    }

    @Override
    protected Vector2[][] getInnerCount() {
        Vector2[][] innerCount = new Vector2[getSize()][getSize()];
        apply((x, y) -> calculateInnerValue(innerCount, x, y, get(x, y)));
        return innerCount;
    }

    @Override
    protected Vector2[][] getNullMask(int size) {
        return new Vector2[size][size];
    }

    @Override
    protected Vector2 getZeroValue() {
        return new Vector2(0f, 0f);
    }

    @Override
    public Vector2Mask copy() {
        return new Vector2Mask(this, getNextSeed(), getName() + "Copy");
    }

    @Override
    public Vector2Mask mock() {
        return new Vector2Mask(this, null, getName() + Mask.MOCK_NAME);
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        WritableRaster imageRaster = image.getRaster();
        Vector2 maxComponents = getMaxComponents();
        Vector2 minComponents = getMinComponents();
        Vector2 rangeComponents = maxComponents.copy().subtract(minComponents);
        apply((x, y) -> {
            float[] maskArray = get(x, y).copy().subtract(minComponents).divide(rangeComponents).multiply(255f).toArray();
            float[] pixelArray = Arrays.copyOf(maskArray, 3);
            imageRaster.setPixel(x, y, pixelArray);
        });
        return image;
    }
}
