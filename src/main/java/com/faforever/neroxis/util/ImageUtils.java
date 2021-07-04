package com.faforever.neroxis.util;

import com.faforever.neroxis.jsquish.Squish;
import com.faforever.neroxis.map.mask.FloatMask;
import com.faforever.neroxis.map.mask.NormalMask;
import com.faforever.neroxis.map.mask.Vector4Mask;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import static com.faforever.neroxis.jsquish.Squish.compressImage;

public strictfp class ImageUtils {

    public static boolean compareImages(BufferedImage imgA, BufferedImage imgB) {
        if (imgA.getWidth() != imgB.getWidth() || imgA.getHeight() != imgB.getHeight()) {
            return false;
        }

        int width = imgA.getWidth();
        int height = imgA.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static BufferedImage readImage(String resource) throws IOException {
        InputStream inputStream = ImageUtils.class.getResourceAsStream(resource);
        return ImageIO.read(inputStream);
    }

    public static BufferedImage scaleImage(BufferedImage image, int width, int height) {
        width = StrictMath.max(width, 1);
        height = StrictMath.max(height, 1);
        BufferedImage imageScaled = new BufferedImage(width, height, image.getType());
        AffineTransform at = new AffineTransform();
        at.scale((double) width / image.getWidth(), (double) height / image.getHeight());
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        scaleOp.filter(image, imageScaled);
        return imageScaled;
    }

    public static BufferedImage insertImageIntoNewImageOfSize(BufferedImage image, int width, int height, Vector2 locToInsertTopLeft) {
        BufferedImage newImage = new BufferedImage(width, height, image.getType());
        WritableRaster newImageRaster = newImage.getRaster();
        Raster imageRaster = image.getData();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int newX = x + (int) locToInsertTopLeft.getX();
                int newY = y + (int) locToInsertTopLeft.getY();
                if (inImageBounds(newX, newY, newImage)) {
                    newImageRaster.setPixel(newX, newY, imageRaster.getPixel(x, y, new int[image.getColorModel().getNumComponents()]));
                }
            }
        }
        return newImage;
    }

    public static void writePNGFromMasks(FloatMask redMask, FloatMask greenMask, FloatMask blueMask, float scaleMultiplier, Path path) throws IOException {
        int size = redMask.getSize();
        if (size != greenMask.getSize() || size != blueMask.getSize()) {
            throw new IllegalArgumentException("Masks not the same size: redMask is " + redMask.getSize() +
                    ", greenMask is " + greenMask.getSize() + " and blueMask is " + blueMask.getSize());
        }
        int length = size * size * 3;
        int index = 0;
        final byte[] byteArray = new byte[length];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                byte redByte = (byte) ((Float) (redMask.get(x, y) * scaleMultiplier)).intValue();
                byte greenByte = (byte) ((Float) (greenMask.get(x, y) * scaleMultiplier)).intValue();
                byte blueByte = (byte) ((Float) (blueMask.get(x, y) * scaleMultiplier)).intValue();
                byteArray[index] = redByte;
                index += 1;
                byteArray[index] = greenByte;
                index += 1;
                byteArray[index] = blueByte;
                index += 1;
            }
        }
        DataBuffer buffer = new DataBufferByte(byteArray, byteArray.length);
        WritableRaster raster = Raster.createInterleavedRaster(buffer, size, size, 3 * size, 3, new int[]{0, 1, 2}, new Point(0, 0));
        ColorModel colorModel = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        BufferedImage image = new BufferedImage(colorModel, raster, true, null);
        ImageIO.write(image, "png", path.toFile());
        System.out.println("PNG created at " + path);
    }

    public static void writeNormalDDS(NormalMask imageMask, Path path) throws IOException {
        int size = imageMask.getSize();
        int length = size * size * 4;
        ByteBuffer imageBytes = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Vector3 value = imageMask.get(x, y);
                byte xV = (byte) StrictMath.min(StrictMath.max((128 * value.getX() + 128), 0), 255);
                byte yV = (byte) StrictMath.min(StrictMath.max((128 * (1 - value.getY()) + 127), 0), 255);
                byte zV = (byte) StrictMath.min(StrictMath.max((128 * value.getZ() + 128), 0), 255);
                imageBytes.put(yV);
                imageBytes.put(zV);
                imageBytes.put((byte) 0);
                imageBytes.put(xV);
            }
        }
        DDSHeader ddsHeader = new DDSHeader();
        ddsHeader.setWidth(imageMask.getSize());
        ddsHeader.setHeight(imageMask.getSize());
        ddsHeader.setFourCC("DXT5");
        byte[] compressedData = compressImage(imageBytes.array(), ddsHeader.getWidth(), ddsHeader.getHeight(), null, Squish.CompressionType.DXT5);
        Files.write(path, ddsHeader.toBytes(), StandardOpenOption.CREATE);
        Files.write(path, compressedData, StandardOpenOption.APPEND);
    }

    public static void writeNormalDDS(BufferedImage image, Path path) throws IOException {
        int size = image.getHeight();
        int length = size * size * 4;
        Raster imageRaster = image.getData();
        ByteBuffer imageBytes = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int[] values = imageRaster.getPixel(x, y, new int[4]);
                for (int val : values) {
                    imageBytes.put((byte) val);
                }
            }
        }
        DDSHeader ddsHeader = new DDSHeader();
        ddsHeader.setWidth(size);
        ddsHeader.setHeight(size);
        ddsHeader.setFourCC("DXT5");
        byte[] compressedData = compressImage(imageBytes.array(), ddsHeader.getWidth(), ddsHeader.getHeight(), null, Squish.CompressionType.DXT5);
        Files.write(path, ddsHeader.toBytes(), StandardOpenOption.CREATE);
        Files.write(path, compressedData, StandardOpenOption.APPEND);
    }

    public static byte[] compressNormal(NormalMask mask) {
        int size = mask.getSize();
        int length = size * size * 4;
        ByteBuffer imageByteBuffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Vector3 value = mask.get(x, y);
                int xV = (byte) StrictMath.min(StrictMath.max((128 * value.getX() + 128), 0), 255);
                int yV = (byte) StrictMath.min(StrictMath.max((255 * (1 - value.getY())), 0), 255);
                int zV = (byte) StrictMath.min(StrictMath.max((128 * value.getZ() + 128), 0), 255);
                imageByteBuffer.put((byte) yV);
                imageByteBuffer.put((byte) zV);
                imageByteBuffer.put((byte) 0);
                imageByteBuffer.put((byte) xV);
            }
        }
        return getCompressedBytes(size, imageByteBuffer);
    }

    public static byte[] compressVector4(Vector4Mask mask) {
        int size = mask.getSize();
        int length = size * size * 4;
        ByteBuffer imageByteBuffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Vector4 value = mask.get(x, y);
                int xV = (byte) StrictMath.min(StrictMath.max(value.getX(), 0), 255);
                int yV = (byte) StrictMath.min(StrictMath.max(value.getY(), 0), 255);
                int zV = (byte) StrictMath.min(StrictMath.max(value.getZ(), 0), 255);
                int wV = (byte) StrictMath.min(StrictMath.max(value.getW(), 0), 255);
                imageByteBuffer.put((byte) xV);
                imageByteBuffer.put((byte) yV);
                imageByteBuffer.put((byte) zV);
                imageByteBuffer.put((byte) wV);
            }
        }
        return getCompressedBytes(size, imageByteBuffer);
    }

    public static byte[] compressShadow(FloatMask mask, float opacityScale) {
        int size = mask.getSize();
        int length = size * size * 4;
        ByteBuffer imageByteBuffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float value = mask.get(x, y);
                int val = (byte) StrictMath.min(StrictMath.max((1 - value) * 32, 0), 255);
                int op = (byte) StrictMath.min(StrictMath.max(value * opacityScale * 255, 0), 255);
                imageByteBuffer.put((byte) val);
                imageByteBuffer.put((byte) val);
                imageByteBuffer.put((byte) val);
                imageByteBuffer.put((byte) op);
            }
        }
        return getCompressedBytes(size, imageByteBuffer);
    }

    private static byte[] getCompressedBytes(int size, ByteBuffer imageByteBuffer) {
        DDSHeader ddsHeader = new DDSHeader();
        ddsHeader.setWidth(size);
        ddsHeader.setHeight(size);
        ddsHeader.setFourCC("DXT5");
        ddsHeader.toBytes();
        byte[] headerBytes = ddsHeader.toBytes();
        byte[] imageBytes = compressImage(imageByteBuffer.array(), ddsHeader.getWidth(), ddsHeader.getHeight(), null, Squish.CompressionType.DXT5, Squish.CompressionMethod.RANGE_FIT);
        int headerLength = headerBytes.length;
        int imageLength = imageBytes.length;
        byte[] allBytes = Arrays.copyOf(headerBytes, headerLength + imageLength);
        System.arraycopy(imageBytes, 0, allBytes, headerLength, imageLength);
        return allBytes;
    }

    public static void writeAutoScaledPNGFromMasks(FloatMask redMask, FloatMask greenMask, FloatMask blueMask, Path path) throws IOException {
        float scaleMultiplier = 255 / StrictMath.max(StrictMath.max(redMask.getMax(), greenMask.getMax()), blueMask.getMax());
        writePNGFromMasks(redMask, greenMask, blueMask, scaleMultiplier, path);
    }

    public static void writePNGFromMask(FloatMask mask, float scaleMultiplier, Path path) throws IOException {
        writePNGFromMasks(mask, mask, mask, scaleMultiplier, path);
    }

    public static void writeAutoScaledPNGFromMask(FloatMask mask, Path path) throws IOException {
        float scaleMultiplier = 255 / mask.getMax();
        writePNGFromMasks(mask, mask, mask, scaleMultiplier, path);
    }

    public static boolean inImageBounds(Vector2 position, BufferedImage image) {
        return inImageBounds((int) position.getX(), (int) position.getY(), image);
    }

    public static boolean inImageBounds(int x, int y, BufferedImage image) {
        return x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight();
    }

}
