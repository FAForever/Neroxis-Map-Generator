package neroxis.util;

import neroxis.map.BinaryMask;
import neroxis.map.FloatMask;
import neroxis.map.Mask;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.HashMap;
import java.util.Map;

public strictfp class VisualDebugger {

    public static boolean ENABLED = Util.DEBUG;

    /**
     * If false, color representation of float masks is scaled to include negative ranges.
     * If true, all negative values are colored as checkerboard, leaving more color space
     * for positive numbers.
     */
    public static boolean ignoreNegativeRange = false;

    private static Map<Integer, String[]> drawMasksWhitelist = null;

    public static void whitelistMask(Mask<?> binaryOrFloatMask, String name, String parentClass) {
        if (drawMasksWhitelist == null) {
            drawMasksWhitelist = new HashMap<>();
        }
        drawMasksWhitelist.put(binaryOrFloatMask.hashCode(), new String[]{name, parentClass});
        createGUI();
    }

    public synchronized static void createGUI() {
        if (!VisualDebuggerGui.isCreated()) {
            VisualDebuggerGui.createGui();
        }
    }

    public static void visualizeMask(Mask<?> mask) {
        if (mask instanceof FloatMask) {
            visualizeMask((FloatMask) mask);
        } else if (mask instanceof BinaryMask) {
            visualizeMask((BinaryMask) mask);
        }
    }

    public static void visualizeMask(BinaryMask mask) {
        if (dontRecord(mask)) {
            return;
        }
        visualize((x, y) -> mask.getValueAt(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), mask.getSize(), mask.hashCode());
    }

    public static void visualizeMask(FloatMask mask) {
        if (dontRecord(mask)) {
            return;
        }
        float max = mask.getMax();
        float min = mask.getMin();
        float range = max - min;
        visualize((x, y) -> {
            float normalizedValue = (mask.getValueAt(x, y) - min) / range;

            int r = (int) (255 * normalizedValue);
            int g = (int) (255 * normalizedValue);
            int b = (int) (255 * normalizedValue);

            return 0xFF_00_00_00 | (r << 16) | (g << 8) | b;
        }, mask.getSize(), mask.hashCode());
    }

    private static boolean dontRecord(Mask<?> mask) {
        if (!ENABLED) {
            return true;
        }
        return drawMasksWhitelist == null || !drawMasksWhitelist.containsKey(mask.hashCode());
    }

    /**
     * Normalize value range from given range to [0, 1].
     * If {@link #ignoreNegativeRange} is true, given value must be positive.
     * Given ranges must be positive. Math.abs(value) must be between rangeMin and rangeMax.
     */
    private static float normalize(float value, float rangeMin, float rangeMax) {
        float rangeDiff = rangeMax - rangeMin;
        float result;
        if (ignoreNegativeRange) {
            result = (value - rangeMin) / rangeDiff;
        } else {
            float negColorSpace = 0.3f;// how much of the color space is given to negative numbers
            if (value >= 0) {
                result = negColorSpace + ((value - rangeMin) * (1 - negColorSpace) / rangeDiff);
                result = (float) Math.pow(result, 1.4);
            } else {
                result = negColorSpace + ((value + rangeMin) * negColorSpace / rangeDiff);
            }
        }
        return result;
    }


    private static void visualize(ImageSource imageSource, int size, int maskHash) {
        String[] maskDetails = drawMasksWhitelist.get(maskHash);
        String maskName = maskDetails[0];
        String callingMethod = Util.getStackTraceMethodInPackage("neroxis.map", "execute");
        float perPixelSize = calculateAutoZoom(size);
        BufferedImage currentImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        // iterate source pixels
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int color = imageSource.get(x, y);
                currentImage.setRGB(x, y, color);
            }
        }
        BufferedImage scaledImage = new BufferedImage((int) (size * perPixelSize), (int) (size * perPixelSize), BufferedImage.TYPE_INT_RGB);
        AffineTransform at = new AffineTransform();
        at.scale(perPixelSize, perPixelSize);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        scaleOp.filter(currentImage, scaledImage);
        VisualDebuggerGui.update(maskName + " " + callingMethod, scaledImage, size);
    }

    private static float calculateAutoZoom(int imageSize) {
        return 512f / imageSize;
    }

    @FunctionalInterface
    private interface ImageSource {
        /**
         * @return Color (A)RGB, see {@link ColorModel#getRGBdefault()}, alpha will be ignored.
         */
        int get(int x, int y);
    }
}
