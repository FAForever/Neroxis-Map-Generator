package com.faforever.neroxis.util;

import com.faforever.neroxis.map.BooleanMask;
import com.faforever.neroxis.map.FloatMask;
import com.faforever.neroxis.map.Mask;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.HashMap;
import java.util.Map;

public strictfp class VisualDebugger {

    public static boolean ENABLED = Util.DEBUG;

    private static Map<Integer, String[]> drawMasksWhitelist = null;

    public static void whitelistMask(Mask<?, ?> binaryOrFloatMask, String name, String parentClass) {
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
        }
    }

    private static void visualizeMask(BooleanMask mask, String method, String line) {
        visualize((x, y) -> mask.getValueAt(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB()
                , mask.hashCode(), mask, method, line);
    }

    private static void visualizeMask(FloatMask mask, String method, String line) {
        float max = mask.getMax();
        float min = mask.getMin();
        float range = max - min;
        visualize((x, y) -> {
            float normalizedValue = (mask.getValueAt(x, y) - min) / range;

            int r = (int) (255 * normalizedValue);
            int g = (int) (255 * normalizedValue);
            int b = (int) (255 * normalizedValue);

            return 0xFF_00_00_00 | (r << 16) | (g << 8) | b;
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
        BufferedImage currentImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        // iterate source pixels
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int color = imageSource.get(x, y);
                currentImage.setRGB(x, y, color);
            }
        }
        VisualDebuggerGui.update(maskName + " " + method, currentImage, mask.copy(), line);
    }

    @FunctionalInterface
    private interface ImageSource {
        /**
         * @return Color (A)RGB, see {@link ColorModel#getRGBdefault()}, alpha will be ignored.
         */
        int get(int x, int y);
    }
}
