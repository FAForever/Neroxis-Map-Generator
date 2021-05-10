package com.faforever.neroxis.map.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.Vector3;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public strictfp class NormalMask extends Vector3Mask {

    public NormalMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public NormalMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public NormalMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public NormalMask(NormalMask other, Long seed) {
        this(other, seed, null);
    }

    public NormalMask(NormalMask other, Long seed, String name) {
        super(other, seed, name);
    }

    public NormalMask(FloatMask other, Long seed) {
        this(other, seed, 1f, null);
    }

    public NormalMask(FloatMask other, Long seed, float scale) {
        this(other, seed, scale, null);
    }

    public NormalMask(FloatMask other, Long seed, float scale, String name) {
        super(other.getSize(), seed, other.getSymmetrySettings(), name, other.isParallel());
        assertCompatibleMask(other);
        enqueue(dependencies -> {
            FloatMask source = (FloatMask) dependencies.get(0);
            set((x, y) -> source.getNormalAt(x, y, scale));
        }, other);
    }

    public NormalMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings) {
        this(sourceImage, seed, symmetrySettings, null, false);
    }

    public NormalMask(BufferedImage sourceImage, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(sourceImage.getHeight(), seed, symmetrySettings, name, parallel);
        Raster imageRaster = sourceImage.getData();
        enqueue(() ->
                set((x, y) -> {
                    float[] components = imageRaster.getPixel(x, y, new float[4]);
                    return createValue(1f, components[3], components[0], components[1]);
                })
        );
    }

    @Override
    protected Vector3 getZeroValue() {
        return new Vector3(0f, 1f, 0f);
    }

    @Override
    public NormalMask copy() {
        if (random != null) {
            return new NormalMask(this, random.nextLong(), getName() + "Copy");
        } else {
            return new NormalMask(this, null, getName() + "Copy");
        }
    }

    @Override
    public NormalMask mock() {
        return new NormalMask(this, null, getName() + Mask.MOCK_NAME);
    }

    @Override
    public NormalMask getFinalMask() {
        Pipeline.await(this);
        return copy();
    }

    @Override
    public BufferedImage toImage() {
        int size = getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        writeToImage(image);
        return image;
    }

    @Override
    public BufferedImage writeToImage(BufferedImage image) {
        assertSize(image.getHeight());
        WritableRaster imageRaster = image.getRaster();
        apply((x, y) -> {
            Vector3 value = get(x, y);
            int xV = (byte) StrictMath.min(StrictMath.max((128 * value.getX() + 128), 0), 255);
            int yV = (byte) StrictMath.min(StrictMath.max((128 * (1 - value.getY()) + 127), 0), 255);
            int zV = (byte) StrictMath.min(StrictMath.max((128 * value.getZ() + 128), 0), 255);
            imageRaster.setPixel(x, y, new int[]{yV, zV, 0, xV});
        });
        return image;
    }
}