package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.ui.GraphMethod;
import com.faforever.neroxis.util.vector.Vector3;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

@SuppressWarnings({"UnusedReturnValue", "unused"})
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

    public Vector3Mask(Vector3Mask other) {
        this(other, null);
    }

    public Vector3Mask(Vector3Mask other, String name) {
        super(other, name);
    }

    public Vector3Mask(NormalMask other) {
        this(other, null);
    }

    public Vector3Mask(NormalMask other, String name) {
        super(other.getSize(), other.getNextSeed(), other.getSymmetrySettings(), name, other.isParallel());
        enqueue(dependencies -> {
            NormalMask source = (NormalMask) dependencies.get(0);
            set(point -> source.get(point).copy());
        }, other);
    }

    public Vector3Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, null, false);
    }

    public Vector3Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, name, false);
    }

    public Vector3Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name, boolean parallel) {
        super(sourceImage, seed, symmetrySettings, scaleFactor, name, parallel);
    }

    @Override
    protected Vector3[][] getNullMask(int size) {
        return new Vector3[size][size];
    }

    @Override
    protected Vector3 createValue(float scaleFactor, float... components) {
        assertMatchingDimension(components.length);
        return new Vector3(components[0], components[1], components[2]).multiply(scaleFactor);
    }

    @GraphMethod
    public Vector3Mask cross(Vector3Mask other) {
        assertCompatibleMask(other);
        enqueue((dependencies) -> {
            Vector3Mask source = (Vector3Mask) dependencies.get(0);
            set(point -> get(point).cross(source.get(point)));
        }, other);
        return this;
    }

    @GraphMethod
    public Vector3Mask cross(Vector3 vector) {
        set(point -> get(point).cross(vector));
        return this;
    }

    @Override
    protected Vector3 getZeroValue() {
        return new Vector3(0f, 0f, 0f);
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        WritableRaster imageRaster = image.getRaster();
        Vector3 maxComponents = getMaxComponents();
        Vector3 minComponents = getMinComponents();
        Vector3 rangeComponents = maxComponents.copy().subtract(minComponents);
        loop(point -> imageRaster.setPixel(point.x, point.y, get(point).copy().subtract(minComponents).divide(rangeComponents).multiply(255f).toArray()));
        return image;
    }
}
