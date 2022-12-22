package com.faforever.neroxis.mask;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.annotations.GraphParameter;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.vector.Vector3;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class Vector3Mask extends VectorMask<Vector3, Vector3Mask> {
    public Vector3Mask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    /**
     * Create a new vector3 mask
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
    public Vector3Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public Vector3Mask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
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
            set((x, y) -> source.get(x, y).copy());
        }, other);
    }

    public Vector3Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, null, false);
    }

    public Vector3Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor,
                       String name) {
        this(sourceImage, seed, symmetrySettings, scaleFactor, name, false);
    }

    public Vector3Mask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, float scaleFactor,
                       String name, boolean parallel) {
        super(sourceImage, seed, symmetrySettings, scaleFactor, name, parallel);
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

    @GraphMethod
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

    @GraphMethod
    public Vector3Mask cross(Vector3Mask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            Vector3Mask source = (Vector3Mask) dependencies.get(0);
            set((x, y) -> get(x, y).cross(source.get(x, y)));
        }, other);
    }

    @GraphMethod
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
        Vector3 rangeComponents = maxComponents.copy().subtract(minComponents);
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
