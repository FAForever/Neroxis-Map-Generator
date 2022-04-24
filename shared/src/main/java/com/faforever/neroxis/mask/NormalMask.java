package com.faforever.neroxis.mask;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.annotations.GraphParameter;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.vector.Vector3;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public strictfp class NormalMask extends VectorMask<Vector3, NormalMask> {

    public NormalMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    @GraphMethod
    @GraphParameter(name = "name", value = "identifier")
    @GraphParameter(name = "parallel", value = "true")
    @GraphParameter(name = "seed", value = "random.nextLong()")
    @GraphParameter(name = "symmetrySettings", value = "symmetrySettings")
    public NormalMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public NormalMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public NormalMask(NormalMask other) {
        this(other, null);
    }

    @GraphMethod
    public NormalMask(NormalMask other, String name) {
        super(other, name);
    }

    public NormalMask(FloatMask other) {
        this(other, 1f, null);
    }

    protected NormalMask(FloatMask other, float scale, String name) {
        this(other.getSize(), other.getNextSeed(), other.getSymmetrySettings(), name, other.isParallel());
        assertCompatibleMask(other);
        enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            set(point -> source.calculateNormalAt(point, scale));
        }, other);
    }

    public NormalMask(FloatMask other, float scale) {
        this(other, scale, null);
    }

    public NormalMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings) {
        this(sourceImage, seed, symmetrySettings, null, false);
    }

    public NormalMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, String name,
                      boolean parallel) {
        this(sourceImage.getHeight(), seed, symmetrySettings, name, parallel);
        Raster imageRaster = sourceImage.getData();
        set(point -> {
            float[] components = imageRaster.getPixel(point.x, point.y, new float[4]);
            return createValue(1f, components[3], components[0], components[1]);
        });
    }

    @Override
    protected Vector3 createValue(float scaleFactor, float... components) {
        assertMatchingDimension(components.length);
        return new Vector3(components[0], components[1], components[2]).multiply(scaleFactor);
    }

    @Override
    public BufferedImage writeToImage(BufferedImage image) {
        assertSize(image.getHeight());
        WritableRaster imageRaster = image.getRaster();
        loop(point -> {
            Vector3 value = get(point);
            int xV = (byte) StrictMath.min(StrictMath.max((128 * value.getX() + 128), 0), 255);
            int yV = (byte) StrictMath.min(StrictMath.max((127 * value.getY() + 128), 0), 255);
            int zV = (byte) StrictMath.min(StrictMath.max((128 * value.getZ() + 128), 0), 255);
            imageRaster.setPixel(point.x, point.y, new int[]{xV, zV, yV});
        });
        return image;
    }

    @Override
    protected Vector3[][] getNullMask(int size) {
        return new Vector3[size][size];
    }

    @GraphMethod
    public NormalMask cross(NormalMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            Vector3Mask source = (Vector3Mask) dependencies.get(0);
            set(point -> get(point).cross(source.get(point)));
        }, other);
    }

    @GraphMethod
    public NormalMask cross(Vector3 vector) {
        Vector3 normalizedVector = vector.copy().normalize();
        return set(point -> get(point).cross(normalizedVector));
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        writeToImage(image);
        return image;
    }

    @Override
    protected Vector3 getZeroValue() {
        return new Vector3(0f, 1f, 0f);
    }
}
