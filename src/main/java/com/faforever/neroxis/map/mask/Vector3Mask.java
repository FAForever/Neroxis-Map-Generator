package com.faforever.neroxis.map.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.Vector3;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public strictfp class Vector3Mask extends VectorMask<Vector3, Vector3Mask> {

    public Vector3Mask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public Vector3Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public Vector3Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public Vector3Mask(Vector3Mask other, Long seed) {
        this(other, seed, null);
    }

    public Vector3Mask(Vector3Mask other, Long seed, String name) {
        super(other, seed, name);
    }

    public Vector3Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor) {
        super(sourceImage, seed, symmetrySettings, scaleFactor, null, false);
    }

    public Vector3Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name) {
        super(sourceImage, seed, symmetrySettings, scaleFactor, name, false);
    }

    public Vector3Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name, boolean parallel) {
        super(sourceImage, seed, symmetrySettings, scaleFactor, name, parallel);
    }

    @Override
    protected Vector3 createValue(float scaleFactor, float... components) {
        assertMatchingDimension(components.length);
        return new Vector3(components[0], components[1], components[2]).multiply(scaleFactor);
    }

    public Vector3Mask cross(Vector3Mask other) {
        assertCompatibleMask(other);
        enqueue((dependencies) -> {
            Vector3Mask source = (Vector3Mask) dependencies.get(0);
            set((x, y) -> get(x, y).cross(source.get(x, y)));
        }, other);
        return this;
    }

    @Override
    protected Vector3[][] getNullMask(int size) {
        return new Vector3[size][size];
    }

    @Override
    protected Vector3 getZeroValue() {
        return new Vector3(0f, 0f, 0f);
    }

    @Override
    public Vector3Mask copy() {
        return new Vector3Mask(this, getNextSeed(), getName() + "Copy");
    }

    @Override
    public Vector3Mask mock() {
        return new Vector3Mask(this, null, getName() + Mask.MOCK_NAME);
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        WritableRaster imageRaster = image.getRaster();
        Vector3 maxComponents = getMaxComponents();
        Vector3 minComponents = getMinComponents();
        Vector3 rangeComponents = maxComponents.copy().subtract(minComponents);
        apply((x, y) -> {
            imageRaster.setPixel(x, y, get(x, y).copy().subtract(minComponents).divide(rangeComponents).multiply(255f).toArray());
        });
        return image;
    }
}
