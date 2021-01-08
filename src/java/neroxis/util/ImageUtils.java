package neroxis.util;

import neroxis.map.FloatMask;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class ImageUtils {

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
        imageScaled = scaleOp.filter(image, imageScaled);

        return imageScaled;
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
                byte redByte = (byte) ((Float) (redMask.getValueAt(x, y) * scaleMultiplier)).intValue();
                byte greenByte = (byte) ((Float) (greenMask.getValueAt(x, y) * scaleMultiplier)).intValue();
                byte blueByte = (byte) ((Float) (blueMask.getValueAt(x, y) * scaleMultiplier)).intValue();
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
        RenderedImage image = (RenderedImage) new BufferedImage(colorModel, raster, true, null);
        ImageIO.write(image, "png", path.toFile());
        System.out.println("PNG created at " + path.toString());
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

}
