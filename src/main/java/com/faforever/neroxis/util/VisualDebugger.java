package com.faforever.neroxis.util;

import com.faforever.neroxis.map.mask.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.HashMap;
import java.util.Map;

public strictfp class VisualDebugger {

    public static boolean ENABLED = Util.DEBUG;

    private static Map<Integer, String[]> drawMasksWhitelist = null;

    public static void whitelistMask(Mask<?, ?> mask, String name, String parentClass) {
        if (drawMasksWhitelist == null) {
            drawMasksWhitelist = new HashMap<>();
        }
        drawMasksWhitelist.put(mask.hashCode(), new String[]{name, parentClass});
        createGUI();
    }

    public synchronized static void createGUI() {
        if (!VisualDebuggerGui.isCreated()) {
            VisualDebuggerGui.createGui();
        }
    }

    public static void visualizeMask(Mask<?, ?> mask, String method) {
        visualizeMask(mask, method, null);
    }

    public static void visualizeMask(Mask<?, ?> mask, String method, String line) {
        if (dontRecord(mask)) {
            return;
        }
        if (mask instanceof FloatMask) {
            visualizeMask((FloatMask) mask, method, line);
        } else if (mask instanceof BooleanMask) {
            visualizeMask((BooleanMask) mask, method, line);
        } else if (mask instanceof IntegerMask) {
            visualizeMask((IntegerMask) mask, method, line);
        } else if (mask instanceof NormalMask) {
            visualizeMask((NormalMask) mask, method, line);
        } else if (mask instanceof Vector3Mask) {
            visualizeMask((Vector3Mask) mask, method, line);
        } else if (mask instanceof Vector4Mask) {
            visualizeMask((Vector4Mask) mask, method, line);
        }
    }

    private static void visualizeMask(BooleanMask mask, String method, String line) {
        visualize((x, y) -> mask.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB()
                , mask.hashCode(), mask, method, line);
    }

    private static void visualizeMask(FloatMask mask, String method, String line) {
        float max = mask.getMax();
        float min = mask.getMin();
        float range = max - min;
        visualize((x, y) -> {
            float normalizedValue = (mask.get(x, y) - min) / range;

            int r = (int) (255 * normalizedValue);
            int g = (int) (255 * normalizedValue);
            int b = (int) (255 * normalizedValue);

            return 0xFF_00_00_00 | (r << 16) | (g << 8) | b;
        }, mask.hashCode(), mask, method, line);
    }

    private static void visualizeMask(IntegerMask mask, String method, String line) {
        int max = mask.getMax();
        int min = mask.getMin();
        int range = max - min;
        visualize((x, y) -> {
            float normalizedValue = (mask.get(x, y) - min) / (float) range;

            int r = (int) (255 * normalizedValue);
            int g = (int) (255 * normalizedValue);
            int b = (int) (255 * normalizedValue);

            return 0xFF_00_00_00 | (r << 16) | (g << 8) | b;
        }, mask.hashCode(), mask, method, line);
    }

    private static void visualizeMask(Vector3Mask mask, String method, String line) {
        Vector3 max = mask.getMaxComponents();
        Vector3 min = mask.getMinComponents();
        Vector3 range = max.copy().subtract(min);
        visualize((x, y) -> {
            Vector3 normalizedValue = mask.get(x, y).copy().subtract(min).divide(range);

            int r = (int) StrictMath.min(StrictMath.max((255 * normalizedValue.getX()), 0), 255);
            int g = (int) StrictMath.min(StrictMath.max((255 * normalizedValue.getY()), 0), 255);
            int b = (int) StrictMath.min(StrictMath.max((255 * normalizedValue.getZ()), 0), 255);

            return 0xFF_00_00_00 | (r << 16) | (g << 8) | b;
        }, mask.hashCode(), mask, method, line);
    }

    private static void visualizeMask(NormalMask mask, String method, String line) {
        visualize((x, y) -> {
            Vector3 normalizedValue = mask.get(x, y);

            int r = (int) StrictMath.min(StrictMath.max((127 * normalizedValue.getX() + 128), 0), 255);
            int g = (int) StrictMath.min(StrictMath.max((127 * normalizedValue.getZ() + 128), 0), 255);
            int b = (int) StrictMath.min(StrictMath.max((127 * normalizedValue.getY() + 128), 0), 255);

            return 0xFF_00_00_00 | (r << 16) | (g << 8) | b;
        }, mask.hashCode(), mask, method, line);
    }

    private static void visualizeMask(Vector4Mask mask, String method, String line) {
        Vector4 max = mask.getMaxComponents();
        Vector4 min = mask.getMinComponents();
        Vector4 range = max.copy().subtract(min);
        visualize((x, y) -> {
            Vector4 normalizedValue = mask.get(x, y).copy().subtract(min).divide(range);

            int r = (int) StrictMath.min(StrictMath.max((255 * normalizedValue.getX()), 0), 255);
            int g = (int) StrictMath.min(StrictMath.max((255 * normalizedValue.getY()), 0), 255);
            int b = (int) StrictMath.min(StrictMath.max((255 * normalizedValue.getZ()), 0), 255);
            int a = (int) StrictMath.min(StrictMath.max((160 * (1 - normalizedValue.getW()) + 95), 0), 255);

            return (a << 24) | (r << 16) | (g << 8) | b;
        }, mask.hashCode(), mask, method, line);
    }

    private static boolean dontRecord(Mask<?, ?> mask) {
        if (!ENABLED) {
            return true;
        }
        return drawMasksWhitelist == null || !drawMasksWhitelist.containsKey(mask.hashCode());
    }

    private static void visualize(ImageSource imageSource, int maskHash, Mask<?, ?> mask, String method, String line) {
        String[] maskDetails = drawMasksWhitelist.get(maskHash);
        String maskName = maskDetails[0];
        int size = mask.getImmediateSize();
        Mask<?, ?> maskCopy = mask.copy();
        BufferedImage currentImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        // iterate source pixels
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int color = imageSource.get(x, y);
                currentImage.setRGB(x, y, color);
            }
        }
        VisualDebuggerGui.update(maskName + " " + method, currentImage, maskCopy, line);
    }

    @FunctionalInterface
    private interface ImageSource {
        /**
         * @return Color (A)RGB, see {@link ColorModel#getRGBdefault()}, alpha will be ignored.
         */
        int get(int x, int y);
    }
}
