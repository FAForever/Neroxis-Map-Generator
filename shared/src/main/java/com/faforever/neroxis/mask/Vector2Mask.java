package com.faforever.neroxis.mask;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.annotations.GraphParameter;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.vector.Vector2;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public final class Vector2Mask extends VectorMask<Vector2, Vector2Mask> {
    public Vector2Mask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    /**
     * Create a new vector2 mask
     *
     * @param size             Size of the mask
     * @param seed             Random seed of the mask
     * @param symmetrySettings symmetrySettings to enforce on the mask
     * @param name             name of the mask
     * @param parallel         whether to parallelize mask operations
     */
    @GraphMethod
    @GraphParameter(name = "name", value = "identifier")
    @GraphParameter(name = "parallel", value = "true")
    @GraphParameter(name = "seed", value = "random.nextLong()")
    @GraphParameter(name = "symmetrySettings", value = "symmetrySettings")
    public Vector2Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public Vector2Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
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

    public Vector2Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor,
                       String name) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, name, false);
    }

    public Vector2Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor,
                       String name, boolean parallel) {
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

    @GraphMethod
    public Vector2Mask setComponents(FloatMask comp0, FloatMask comp1) {
        assertCompatibleComponents(comp0, comp1);
        return enqueue(dependencies -> {
            FloatMask source1 = (FloatMask) dependencies.get(0);
            FloatMask source2 = (FloatMask) dependencies.get(1);
            apply((x, y) -> {
                setComponentAt(x, y, source1.get(x, y), 0);
                setComponentAt(x, y, source2.get(x, y), 1);
            });
        }, comp0, comp1);
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        WritableRaster imageRaster = image.getRaster();
        Vector2 maxComponents = getMaxComponents();
        Vector2 minComponents = getMinComponents();
        Vector2 rangeComponents = maxComponents.subtract(minComponents);
        loop((x, y) -> {
            float[] maskArray = get(x, y).subtract(minComponents).divide(rangeComponents).multiply(255f).toArray();
            float[] pixelArray = Arrays.copyOf(maskArray, 3);
            imageRaster.setPixel(x, y, pixelArray);
        });
        return image;
    }

    @Override
    protected Vector2 getZeroValue() {
        return new Vector2(0f, 0f);
    }
}
