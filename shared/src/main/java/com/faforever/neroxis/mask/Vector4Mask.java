package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.vector.Vector4;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class Vector4Mask extends VectorMask<Vector4, Vector4Mask> {
    public Vector4Mask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    /**
     * Create a new vector4 mask
     *
     * @param size             Size of the mask
     * @param seed             Random seed of the mask
     * @param symmetrySettings symmetrySettings to enforce on the mask
     * @param name             name of the mask
     * @param parallel         whether to parallelize mask operations
     */
    public Vector4Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public Vector4Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public Vector4Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, null, false);
    }

    public Vector4Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor,
                       String name) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, name, false);
    }

    public Vector4Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor,
                       String name, boolean parallel) {
        super(sourceImage, seed, symmetrySettings, scaleFactor, name, parallel);
    }

    protected Vector4Mask(Vector4Mask other, String name, boolean immutable) {
        super(other, name, immutable);
    }

    @Override
    protected Vector4 createValue(float scaleFactor, float... components) {
        assertMatchingDimension(components.length);
        return new Vector4(components[0], components[1], components[2], components[3]).multiply(scaleFactor);
    }

    @Override
    protected Vector4[][] getNullMask(int size) {
        return new Vector4[size][size];
    }

    public Vector4Mask setComponents(FloatMask comp0, FloatMask comp1, FloatMask comp2, FloatMask comp3) {
        assertCompatibleComponents(comp0, comp1, comp2, comp3);
        return enqueue(dependencies -> {
            FloatMask source1 = (FloatMask) dependencies.get(0);
            FloatMask source2 = (FloatMask) dependencies.get(1);
            FloatMask source3 = (FloatMask) dependencies.get(2);
            FloatMask source4 = (FloatMask) dependencies.get(3);
            apply((x, y) -> {
                setComponentAt(x, y, source1.get(x, y), 0);
                setComponentAt(x, y, source2.get(x, y), 1);
                setComponentAt(x, y, source3.get(x, y), 2);
                setComponentAt(x, y, source4.get(x, y), 3);
            });
        }, comp0, comp1, comp2, comp3);
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        WritableRaster imageRaster = image.getRaster();
        Vector4 maxComponents = getMaxComponents();
        Vector4 minComponents = getMinComponents();
        Vector4 rangeComponents = maxComponents.copy().subtract(minComponents);
        loop((x, y) -> imageRaster.setPixel(x, y, get(x, y).copy()
                                                           .subtract(minComponents)
                                                           .divide(rangeComponents)
                                                           .multiply(255f, 255f, 255f, 255f - 64f)
                                                           .add(0f, 0f, 0f, 64f)
                                                           .toArray()));
        return image;
    }

    @Override
    protected Vector4 getZeroValue() {
        return new Vector4(0f, 0f, 0f, 0f);
    }
}
