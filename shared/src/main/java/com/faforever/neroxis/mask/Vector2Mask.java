package com.faforever.neroxis.mask;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.annotations.GraphParameter;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.vector.Vector2;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public strictfp class Vector2Mask extends VectorMask<Vector2, Vector2Mask> {

    public Vector2Mask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public Vector2Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    @GraphMethod
    @GraphParameter(name = "name", nullable = true)
    @GraphParameter(name = "parallel", value = "true")
    @GraphParameter(name = "seed", value = "random.nextLong()")
    @GraphParameter(name = "symmetrySettings", value = "symmetrySettings")
    public Vector2Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public Vector2Mask(Vector2Mask other) {
        this(other, null);
    }

    public Vector2Mask(Vector2Mask other, String name) {
        super(other, name);
    }

    public Vector2Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, null, false);
    }

    public Vector2Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor, String name) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, name, false);
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
    protected Vector2[][] getNullMask(int size) {
        return new Vector2[size][size];
    }

    @Override
    protected Vector2 getZeroValue() {
        return new Vector2(0f, 0f);
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        WritableRaster imageRaster = image.getRaster();
        Vector2 maxComponents = getMaxComponents();
        Vector2 minComponents = getMinComponents();
        Vector2 rangeComponents = maxComponents.copy().subtract(minComponents);
        loop(point -> {
            float[] maskArray = get(point).copy().subtract(minComponents).divide(rangeComponents).multiply(255f).toArray();
            float[] pixelArray = Arrays.copyOf(maskArray, 3);
            imageRaster.setPixel(point.x, point.y, pixelArray);
        });
        return image;
    }
}
