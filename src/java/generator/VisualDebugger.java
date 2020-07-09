package generator;

import map.BinaryMask;
import map.FloatMask;
import map.Mask;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.HashMap;
import java.util.Map;

public class VisualDebugger {

    /**
     * When enabled, mask concurrency is disabled.
     */
    public static final boolean ENABLED = false;

    /**
     * If false, color representation of float masks is scaled to include negative ranges.
	 * If true, all negative values are colored as checkerboard, leaving more color space
	 * for positive numbers.
	 */
	public static boolean ignoreNegativeRange = false;

	private static boolean isDrawAllMasks = false;
	private static Map<Integer, String> drawMasksWhitelist = null;

	public static void whitelistMask(Mask binaryOrFloatMask) {
		whitelistMask(binaryOrFloatMask, "" + binaryOrFloatMask.hashCode());
	}

	public static void whitelistMask(Mask binaryOrFloatMask, String name) {
		if (drawMasksWhitelist == null) {
			drawMasksWhitelist = new HashMap<>();
		}
		drawMasksWhitelist.put(binaryOrFloatMask.hashCode(), name);
		createGUI();
	}

	public static void createGUI(){
		if (!VisualDebuggerGui.isCreated()) {
			VisualDebuggerGui.createGui();
		}
    }

    public static void startRecordAll() {
        isDrawAllMasks = true;
    }

    public static void stopRecordAll() {
        isDrawAllMasks = false;
    }

    public static void visualizeMask(BinaryMask mask) {
        if (!shouldRecord(mask)) {
            return;
        }
        visualize((x, y) -> mask.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), mask.getSize(), mask.hashCode());
    }

    public static void visualizeMask(FloatMask mask) {
        if (!shouldRecord(mask)) {
            return;
        }
        ImageSource checkerBoard = (x, y) -> (x + y) % 2 == 0 ? 0xFF_66_66_66 : 0xDD_DD_DD_DD;
        visualize((x, y) -> {
            float value = mask.get(x, y);

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
            int[] ranges = new int[]{1, 2, 5, 10, 20, 50};

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
        }, mask.getSize(), mask.hashCode());
    }

    private static boolean shouldRecord(Mask mask) {
        if (!ENABLED) {
            return false;
        }
        return (drawMasksWhitelist == null && isDrawAllMasks)
                || (drawMasksWhitelist != null && drawMasksWhitelist.containsKey(mask.hashCode()));
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
		int perPixelSize = calculateAutoZoom(size);
		int imageSize = size * perPixelSize;
		BufferedImage currentImage = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB);
		// iterate source pixels
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int color = imageSource.get(x, y);
				// scale source pixel to filled rectangle so its possible to see stuff
				for (int yInner = 0; yInner < perPixelSize; yInner++) {
					for (int xInner = 0; xInner < perPixelSize; xInner++) {
						int drawX = (x * perPixelSize) + xInner;
						int drawY = (y * perPixelSize) + yInner;
						currentImage.setRGB(drawX, drawY, color);
					}
				}
			}
        }
        String maskName = drawMasksWhitelist.getOrDefault(maskHash, String.valueOf(maskHash));
        String function = new Throwable().getStackTrace()[2].getMethodName();
        VisualDebuggerGui.update(maskName + " " + function, currentImage, perPixelSize);
    }

    private static int calculateAutoZoom(int imageSize) {
        return 513 / imageSize;
    }

    @FunctionalInterface
    private interface ImageSource {
        /**
         * @return Color (A)RGB, see {@link ColorModel#getRGBdefault()}, alpha will be ignored.
         */
        int get(int x, int y);
    }
}
