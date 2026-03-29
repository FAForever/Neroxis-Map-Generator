package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.vector.Vector3;
import org.jspecify.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.random.RandomGenerator;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public final class Vector3Mask extends VectorMask<Vector3, Vector3Mask> {
    public Vector3Mask(int size, RandomGenerator.@Nullable SplittableGenerator random, SymmetrySettings symmetrySettings) {
        this(size, random, symmetrySettings, null);
    }

    /**
     * Create a new vector3 mask
     *
     * @param size             Size of the mask
     * @param random           RandomGenerator of the mask
     * @param symmetrySettings symmetrySettings to enforce on the mask
     * @param name             name of the mask
     */
    public Vector3Mask(int size, RandomGenerator.@Nullable SplittableGenerator random, SymmetrySettings symmetrySettings,
                       @Nullable String name) {
        super(size, random, symmetrySettings, name);
    }

    public Vector3Mask(Vector3Mask other) {
        this(other, null);
    }

    public Vector3Mask(Vector3Mask other, @Nullable String name) {
        super(other, name);
    }

    public Vector3Mask(NormalMask other) {
        this(other, null);
    }

    public Vector3Mask(NormalMask other, @Nullable String name) {
        super(other.getSize(), other.getNextRandomGenerator(), other.getSymmetrySettings(), name);
        enqueue(dependencies -> {
            NormalMask source = (NormalMask) dependencies.getFirst();
            set(source::get);
        }, other);
    }

    public Vector3Mask(BufferedImage sourceImage, RandomGenerator.@Nullable SplittableGenerator random,
                       SymmetrySettings symmetrySettings, float scaleFactor) {
        this(sourceImage, random, symmetrySettings, scaleFactor, null);
    }

    public Vector3Mask(BufferedImage sourceImage, RandomGenerator.@Nullable SplittableGenerator random,
                       SymmetrySettings symmetrySettings, float scaleFactor,
                       @Nullable String name) {
        super(sourceImage, random, symmetrySettings, scaleFactor, name);
    }

    @Override
    protected Vector3 createValue(float scaleFactor, float... components) {
        assertMatchingDimension(components.length);
        return new Vector3(components[0], components[1], components[2]).multiply(scaleFactor);
    }

    @Override
    protected Vector3[][] getNullMask(int size) {
        return new Vector3[size][size];
    }

    public Vector3Mask setComponents(FloatMask comp0, FloatMask comp1, FloatMask comp2) {
        assertCompatibleComponents(comp0, comp1, comp2);
        return enqueue(dependencies -> {
            FloatMask source1 = (FloatMask) dependencies.get(0);
            FloatMask source2 = (FloatMask) dependencies.get(1);
            FloatMask source3 = (FloatMask) dependencies.get(2);
            apply((x, y) -> {
                setComponentAt(x, y, source1.get(x, y), 0);
                setComponentAt(x, y, source2.get(x, y), 1);
                setComponentAt(x, y, source3.get(x, y), 2);
            });
        }, comp0, comp1, comp2);
    }

    public Vector3Mask cross(Vector3Mask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            Vector3Mask source = (Vector3Mask) dependencies.getFirst();
            set((x, y) -> get(x, y).cross(source.get(x, y)));
        }, other);
    }

    public Vector3Mask cross(Vector3 vector) {
        return set((x, y) -> get(x, y).cross(vector));
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        WritableRaster imageRaster = image.getRaster();
        Vector3 maxComponents = getMaxComponents();
        Vector3 minComponents = getMinComponents();
        Vector3 rangeComponents = maxComponents.subtract(minComponents);
        loop((x, y) -> imageRaster.setPixel(x, y, get(x, y).subtract(minComponents)
                                                           .divide(rangeComponents)
                                                           .multiply(255f)
                                                           .toArray()));
        return image;
    }

    @Override
    protected Vector3 getZeroValue() {
        return new Vector3(0f, 0f, 0f);
    }
}
