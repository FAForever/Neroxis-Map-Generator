package generator;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.HashSet;
import java.util.Set;

import map.BinaryMask;
import map.FloatMask;
import map.Mask;

public class VisualDebugger {

	public static boolean ENABLED = false;// when enabled, mask concurrency is disabled
	
	// If true, color representation of float masks is scaled to include negative ranges.
	// If false, all negative values are colored as checkerboard, leaving more color space
	// for positive numbers.
	public static boolean ignoreNegativeRange = false;
	
	private static boolean isRecordingAllMasks = false;
	private static Set<Integer> whitelistMasks = null;
	
	public static void whitelistMask(Mask binaryOrFloatMask) {
		if (whitelistMasks == null) {
			whitelistMasks = new HashSet<>();
		}
		whitelistMasks.add(binaryOrFloatMask.hashCode());
	}
	
	public static void startRecordAll() {
		isRecordingAllMasks = true;
	}
	
	public static void stopRecordAll() {
		isRecordingAllMasks = false;
	}
	
	public static void visualizeMask(BinaryMask mask) {
		if (!shouldRecord(mask)) {
			return;
		}
		visualize((x, y) -> {
			return mask.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
		}, mask.getSize(), "" + mask.hashCode());
	}
	
	public static void visualizeMask(FloatMask mask) {
		if (!shouldRecord(mask)) {
			return;
		}
		visualize((x, y) -> {
			float value = mask.get(x, y);
			// if |value| <  1, scale white to black
			// if |value| 1-10, scale green to red
			// if |value| 10-50, dark blue to blue
			// if |value| > 100, just output blue
			
			if (ignoreNegativeRange && value < 0) {
				// checkerboard
				return (x + y) % 2 == 0 ?
						0xFF_66_66_66 : 0xDD_DD_DD_DD ;
			}
			// For each range we interpolate from color white to the color given in the corresponding mask.
			// Ranges must be ordered smallest to biggest. Anything bigger than biggest range becomes black.
			// Anything smaller than smallest becomes checkerboard.
			int[] colors = new int[] {
					0xFF_FF_00_00,
					0xFF_00_FF_00,
					0xFF_00_00_FF,
					0xFF_FF_00_FF,
					0xFF_00_FF_FF,
					0xFF_00_00_00};
			int[] ranges = new int[] {1, 2, 5, 10, 20, 50};
			
			for (int i = 0; i < colors.length; i++) {
				int color = colors[i];
				int rangeMaxAbs = ranges[i];
				if (value >= -rangeMaxAbs && value <= rangeMaxAbs) {
					int rangeMinAbs = i > 0 ? ranges[i - 1] : 0;
					float normalized = normalize(value, rangeMinAbs, rangeMaxAbs);
					float inverted = Math.max(0, 1 - normalized);
					
					int r = (color >>> 16) & 0xFF;
					int g = (color >>>  8) & 0xFF;
					int b =  color         & 0xFF;
					
					r = r + (int) (inverted * (255 - r));
					g = g + (int) (inverted * (255 - g));
					b = b + (int) (inverted * (255 - b));
					
					return 0xFF_00_00_00 | (r << 16)| (g << 8) | b;
				}
			}
			if (value < 0) {
				// checkerboard / dither
				return (x + y) % 2 == 0 ?
						0xFF_66_66_66 : 0xDD_DD_DD_DD ;
			} else {
				return 0xFF_00_00_00;
			}
		}, mask.getSize(), "" + mask.hashCode());
	}
	
	private static boolean shouldRecord(Mask mask) {
		if (!ENABLED) {
			return false;
		}
		return (whitelistMasks == null && isRecordingAllMasks)
				|| (whitelistMasks != null && whitelistMasks.contains(mask.hashCode()));
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
	
	@FunctionalInterface
	private interface ImageSource {
		/**
		 * @return Color (A)RGB, see {@link ColorModel#getRGBdefault()}, alpha will be ignored.
		 */
		public int get(int x, int y);
	}
	
	private static void visualize(ImageSource imageSource, int size, String maskName) {
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
		if (!VisualDebuggerGui.isCreated()) {
			VisualDebuggerGui.createGui();
		}
		VisualDebuggerGui.update("lastChanged", currentImage, perPixelSize);
		VisualDebuggerGui.update(maskName, currentImage, perPixelSize);
	}
	
	private static int calculateAutoZoom(int imageSize) {
		int perPixelSize;
		if (imageSize <= 32) {
			perPixelSize = 5;
		} else if (imageSize <= 128) {
			perPixelSize = 3;
		} else if (imageSize <= 256) {
			perPixelSize = 2;
		} else {
			perPixelSize = 1;
		}
		return perPixelSize;
	}
}
