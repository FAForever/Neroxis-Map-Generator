package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.vector.Vector3;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public final class NormalMask extends VectorMask<Vector3, NormalMask> {

    public NormalMask(int size, Long seed, String name) {
        super(size, seed, new SymmetrySettings(Symmetry.NONE), name);
    }

    public NormalMask(NormalMask other) {
        this(other, null);
    }

    public NormalMask(NormalMask other, String name) {
        super(other, name);
    }

    public NormalMask(FloatMask other) {
        this(other, 1f, null);
    }

    public NormalMask(FloatMask other, float scale, String name) {
        this(other.getSize() - 1, other.getNextSeed(), name);
        enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.getFirst();
            set((x, y) -> source.calculateNormalAt(x, y, scale));
        }, other);
    }

    public NormalMask(FloatMask other, float scale) {
        this(other, scale, null);
    }

    public NormalMask(BufferedImage sourceImage, Long seed) {
        this(sourceImage, seed, null);
    }

    public NormalMask(BufferedImage sourceImage, Long seed, String name) {
        this(sourceImage.getHeight(), seed, name);
        Raster imageRaster = sourceImage.getData();
        set((x, y) -> {
            float[] components = imageRaster.getPixel(x, y, new float[4]);
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
        loop((x, y) -> {
            Vector3 value = get(x, y);
            int xV = (byte) StrictMath.min(StrictMath.max((128 * value.x() + 128), 0), 255);
            int yV = (byte) StrictMath.min(StrictMath.max((127 * value.y() + 128), 0), 255);
            int zV = (byte) StrictMath.min(StrictMath.max((128 * value.z() + 128), 0), 255);
            imageRaster.setPixel(x, y, new int[]{xV, zV, yV});
        });
        return image;
    }

    @Override
    protected Vector3[][] getNullMask(int size) {
        return new Vector3[size][size];
    }

    public NormalMask cross(NormalMask other) {
        assertCompatibleMask(other);
        return enqueue(dependencies -> {
            Vector3Mask source = (Vector3Mask) dependencies.getFirst();
            set((x, y) -> get(x, y).cross(source.get(x, y)));
        }, other);
    }

    public NormalMask cross(Vector3 vector) {
        Vector3 normalizedVector = vector.normalize();
        return set((x, y) -> get(x, y).cross(normalizedVector));
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
