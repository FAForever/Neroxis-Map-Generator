package neroxis.generator;

import neroxis.evaluator.MapEvaluator;
import neroxis.map.BinaryMask;
import neroxis.map.FloatMask;
import neroxis.map.Mask;
import neroxis.populator.MapPopulator;
import neroxis.transformer.MapTransformer;
import neroxis.util.Util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class VisualDebugger {

    public static boolean ENABLED = MapGenerator.DEBUG || MapTransformer.DEBUG || MapEvaluator.DEBUG || MapPopulator.DEBUG || ImageGenerator.DEBUG;

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
        visualize((x, y) -> mask.getValueAt(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), mask.getSize(), mask.hashCode(), mask.getClass());
    }

    public static void visualizeMask(FloatMask mask) {
        if (dontRecord(mask)) {
            return;
        }
        ImageSource checkerBoard = (x, y) -> (x + y) % 2 == 0 ? 0xFF_66_66_66 : 0xDD_DD_DD_DD;
        visualize((x, y) -> {
            float value = mask.getValueAt(x, y);

            if (ignoreNegativeRange && value < 0) {
                return checkerBoard.get(x, y);
            }
            // For each range we interpolate from color white to the color given in the corresponding array.
            // Ranges must be positive and ordered smallest to biggest. Anything bigger than biggest range
            // becomes black. Anything smaller than smallest becomes checkerboard.
            int[] colors = new int[]{
                    0xFF_FF_00_00,
                    0xFF_00_FF_00,
                    0xFF_00_00_FF,
                    0xFF_FF_00_FF,
                    0xFF_00_FF_FF,
                    0xFF_00_00_00};
            int[] ranges = new int[]{25, 50, 75, 100, 125, 150};

            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                int rangeMaxAbs = ranges[i];
                if (value >= -rangeMaxAbs && value <= rangeMaxAbs) {
                    int rangeMinAbs = i > 0 ? ranges[i - 1] : 0;
                    float normalized = normalize(value, rangeMinAbs, rangeMaxAbs);
                    float inverted = Math.max(0, 1 - normalized);

                    int r = (color >>> 16) & 0xFF;
                    int g = (color >>> 8) & 0xFF;
                    int b = color & 0xFF;

                    r = r + (int) (inverted * (255 - r));
                    g = g + (int) (inverted * (255 - g));
                    b = b + (int) (inverted * (255 - b));

                    return 0xFF_00_00_00 | (r << 16) | (g << 8) | b;
                }
            }
            if (value < 0) {
                return checkerBoard.get(x, y);
            } else {
                return 0xFF_00_00_00;
            }
        }, mask.getSize(), mask.hashCode(), mask.getClass());
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


    private static void visualize(ImageSource imageSource, int size, int maskHash, Class<? extends Mask<?>> clazz) {
        String[] maskDetails = drawMasksWhitelist.get(maskHash);
        String maskName = maskDetails[0];
        String parentClass = maskDetails[1];
        LinkedHashSet<String> methods = Util.getStackTraceMethods(clazz);
        methods.addAll(Util.getStackTraceMethods("neroxis.map.Mask"));
        String function;
        if (clazz.getCanonicalName().equals(parentClass)) {
            function = methods.iterator().next();
        } else if (methods.size() == 1) {
            function = methods.iterator().next();
        } else if (methods.contains("fillParallelogram")) {
            function = methods.iterator().next();
        } else if (methods.contains("fillArc")) {
            function = methods.iterator().next();
        } else if (methods.contains("show")) {
            function = "show";
        } else {
            return;
        }
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
        VisualDebuggerGui.update(maskName + " " + function, scaledImage, size);
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
