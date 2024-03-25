package com.faforever.neroxis.util;

import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.NormalMask;
import com.faforever.neroxis.mask.Vector4Mask;
import com.faforever.neroxis.util.dds.DDSHeader;
import com.faforever.neroxis.util.jsquish.Squish;
import com.faforever.neroxis.util.serial.biome.LightingSettings;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import static com.faforever.neroxis.util.jsquish.Squish.compressImage;

public class ImageUtil {
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
        InputStream inputStream = ImageUtil.class.getResourceAsStream(resource);
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

    public static BufferedImage insertImageIntoNewImageOfSize(BufferedImage image, int width, int height,
                                                              Vector2 locToInsertTopLeft) {
        BufferedImage newImage = new BufferedImage(width, height, image.getType());
        WritableRaster newImageRaster = newImage.getRaster();
        Raster imageRaster = image.getData();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int newX = x + (int) locToInsertTopLeft.getX();
                int newY = y + (int) locToInsertTopLeft.getY();
                if (inImageBounds(newX, newY, newImage)) {
                    newImageRaster.setPixel(newX, newY, imageRaster.getPixel(x, y, new int[image.getColorModel()
                                                                                                .getNumComponents()]));
                }
            }
        }
        return newImage;
    }

    public static boolean inImageBounds(int x, int y, BufferedImage image) {
        return x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight();
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
        byte[] compressedData = compressImage(imageBytes.array(), ddsHeader.getWidth(), ddsHeader.getHeight(), null,
                                              Squish.CompressionType.DXT5);
        Files.write(path, ddsHeader.toBytes(), StandardOpenOption.CREATE);
        Files.write(path, compressedData, StandardOpenOption.APPEND);
    }

    public static void writeCompressedDDS(BufferedImage image, Path path) throws IOException {
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
        byte[] compressedData = compressImage(imageBytes.array(), ddsHeader.getWidth(), ddsHeader.getHeight(), null,
                                              Squish.CompressionType.DXT5);
        // If we don't do this we get weird results when the file already exists
        Files.deleteIfExists(path);
        Files.write(path, ddsHeader.toBytes(), StandardOpenOption.CREATE);
        Files.write(path, compressedData, StandardOpenOption.APPEND);
    }

    public static void writeRawDDS(BufferedImage image, Path path) throws IOException {
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
        ddsHeader.setRGBBitCount(32);
        ddsHeader.setRBitMask(0x000000FF);
        ddsHeader.setGBitMask(0x0000FF00);
        ddsHeader.setBBitMask(0x00FF0000);
        ddsHeader.setABitMask(0xFF000000);

        // If we don't do this we get weird results when the file already exists
        Files.deleteIfExists(path);
        Files.write(path, ddsHeader.toBytes(), StandardOpenOption.CREATE);
        Files.write(path, imageBytes.array(), StandardOpenOption.APPEND);
    }

    public static BufferedImage getMapwideTexture(NormalMask normalMask, FloatMask waterDepth, FloatMask shadowMask) {
        if (shadowMask.getSize() != normalMask.getSize()) {
            throw new IllegalArgumentException("Mask sizes do not match: shadow size %d, normal size %d"
                                                       .formatted(shadowMask.getSize(), normalMask.getSize()));
        }
        waterDepth.resample(shadowMask.getSize());
        int size = shadowMask.getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        WritableRaster imageRaster = image.getRaster();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Vector3 normalValue = normalMask.get(x, y);
                int xV = (byte) StrictMath.min(StrictMath.max(128 * normalValue.getX() + 127, 0), 255);
                int yV = (byte) StrictMath.min(StrictMath.max(128 * normalValue.getZ() + 127, 0), 255);
                int zV = (byte) StrictMath.min(StrictMath.max(waterDepth.get(x, y) * 255, 0), 255);
                int wV = (byte) StrictMath.min(StrictMath.max(shadowMask.get(x, y) * 255, 0), 255);
                imageRaster.setPixel(x, y, new int[]{xV, yV, zV, wV});
            }
        }
        return image;
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
        return getCompressedDDSImageBytes(size, imageByteBuffer);
    }

    public static byte[] compressShadow(FloatMask mask, LightingSettings lightingSettings) {
        int size = mask.getSize();
        int length = size * size * 4;
        Vector3 shadowFillColor = lightingSettings.shadowFillColor()
                                                  .copy()
                                                  .add(lightingSettings.sunAmbience())
                                                  .divide(4);
        float opacityScale = lightingSettings.lightingMultiplier() / 4;
        ByteBuffer imageByteBuffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float value = mask.get(x, y);
                int r = (byte) StrictMath.min(StrictMath.max(shadowFillColor.getX() * 128, 0), 255);
                int g = (byte) StrictMath.min(StrictMath.max(shadowFillColor.getY() * 128, 0), 255);
                int b = (byte) StrictMath.min(StrictMath.max(shadowFillColor.getZ() * 128, 0), 255);
                int a = (byte) StrictMath.min(StrictMath.max((1 - value) * opacityScale * 255, 0), 255);
                imageByteBuffer.put((byte) r);
                imageByteBuffer.put((byte) g);
                imageByteBuffer.put((byte) b);
                imageByteBuffer.put((byte) a);
            }
        }
        return getCompressedDDSImageBytes(size, imageByteBuffer);
    }

    public static BufferedImage normalToARGB(NormalMask mask) {
        int size = mask.getSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        WritableRaster imageRaster = image.getRaster();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Vector3 value = mask.get(x, y);
                int xV = (byte) StrictMath.min(StrictMath.max((128 * value.getX() + 128), 0), 255);
                int yV = (byte) StrictMath.min(StrictMath.max((255 * (1 - value.getY())), 0), 255);
                int zV = (byte) StrictMath.min(StrictMath.max((128 * value.getZ() + 128), 0), 255);
                imageRaster.setPixel(x, y, new int[]{yV, zV, 0, xV});
            }
        }
        return image;
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
        return getCompressedDDSImageBytes(size, imageByteBuffer);
    }

    private static byte[] getCompressedDDSImageBytes(int size, ByteBuffer imageByteBuffer) {
        DDSHeader ddsHeader = new DDSHeader();
        ddsHeader.setWidth(size);
        ddsHeader.setHeight(size);
        ddsHeader.setFourCC("DXT5");
        ddsHeader.toBytes();
        byte[] headerBytes = ddsHeader.toBytes();
        byte[] imageBytes = compressImage(imageByteBuffer.array(), ddsHeader.getWidth(), ddsHeader.getHeight(), null,
                                          Squish.CompressionType.DXT5, Squish.CompressionMethod.RANGE_FIT);
        int headerLength = headerBytes.length;
        int imageLength = imageBytes.length;
        byte[] allBytes = Arrays.copyOf(headerBytes, headerLength + imageLength);
        System.arraycopy(imageBytes, 0, allBytes, headerLength, imageLength);
        return allBytes;
    }

    private static byte[] getRawDDSImageBytes(int size, ByteBuffer imageByteBuffer) {
        DDSHeader ddsHeader = new DDSHeader();
        ddsHeader.setWidth(size);
        ddsHeader.setHeight(size);
        ddsHeader.setRGBBitCount(32);
        ddsHeader.setRBitMask(0x000000FF);
        ddsHeader.setGBitMask(0x0000FF00);
        ddsHeader.setBBitMask(0x00FF0000);
        ddsHeader.setABitMask(0xFF000000);
        ddsHeader.toBytes();
        byte[] headerBytes = ddsHeader.toBytes();
        byte[] imageBytes = imageByteBuffer.array();
        int headerLength = headerBytes.length;
        int imageLength = size * size * 4;
        byte[] allBytes = Arrays.copyOf(headerBytes, headerLength + imageLength);
        System.arraycopy(imageBytes, 0, allBytes, headerLength, imageLength);
        return allBytes;
    }

    public static void writeAutoScaledPNGFromMasks(FloatMask redMask, FloatMask greenMask, FloatMask blueMask,
                                                   Path path) throws IOException {
        float scaleMultiplier = 255 / StrictMath.max(StrictMath.max(redMask.getMax(), greenMask.getMax()),
                                                     blueMask.getMax());
        writePNGFromMasks(redMask, greenMask, blueMask, scaleMultiplier, path);
    }

    public static void writePNGFromMasks(FloatMask redMask, FloatMask greenMask, FloatMask blueMask,
                                         float scaleMultiplier, Path path) throws IOException {
        int size = redMask.getSize();
        if (size != greenMask.getSize() || size != blueMask.getSize()) {
            throw new IllegalArgumentException("Masks not the same size: redMask is "
                                               + redMask.getSize()
                                               + ", greenMask is "
                                               + greenMask.getSize()
                                               + " and blueMask is "
                                               + blueMask.getSize());
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
        WritableRaster raster = Raster.createInterleavedRaster(buffer, size, size, 3 * size, 3, new int[]{0, 1, 2},
                                                               new Point(0, 0));
        ColorModel colorModel = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true,
                                                        Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        BufferedImage image = new BufferedImage(colorModel, raster, true, null);
        ImageIO.write(image, "png", path.toFile());
        System.out.println("PNG created at " + path);
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
}
