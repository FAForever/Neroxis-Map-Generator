package generator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import map.BinaryMask;
import map.FloatMask;

public class VisualDebugger {

	public static boolean ENABLED = false;// disables mask concurrency
	public static int perPixelSize = 3;// scale factor for debug image
	
	private static boolean isRecording = false;
	private static BufferedImage currentImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	private static GraphicsConfiguration gc;
	private static JFrame frame;
	
	// PUBLIC
	
	static void startRecording() {
		isRecording = true;
		if (frame == null) {
			frame = new JFrame(gc);
			JPanel panel = new JPanel() {
				@Override
		        protected void paintComponent(Graphics g) {				
		            super.paintComponent(g);
		            Graphics2D g2d = (Graphics2D) g.create();
	                int x = (getWidth() - currentImage.getWidth()) / 2;
	                int y = (getHeight() - currentImage.getHeight()) / 2;
	                g2d.drawImage(currentImage, x, y, this);
	                g2d.dispose();
		        }
				@Override
				public Dimension getPreferredSize() {
					return new Dimension(currentImage.getWidth(), currentImage.getHeight());
				}
			};
			frame.add(panel);
			frame.setVisible(true);
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			setFrameSize();
		}
	}
	
	static void stopRecording() {
		isRecording = false;
	}
	
	public static void visualizeMask(BinaryMask mask) {
		if (!ENABLED || !isRecording) return;
		visualize((x, y) -> {
			return mask.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
		}, mask.getSize());
	}
	
	public static void visualizeMask(FloatMask mask) {
		if (!ENABLED || !isRecording) return;
		// TODO untested
		visualize((x, y) -> {
			float value = mask.get(x, y);
			// if |value| <  1, scale white to black
			// if |value| 1-10, scale green to red
			// if |value| > 10, just output blue
			if (value >= -1 && value <= 1) {
				float normalized = ((value + 1) / 2);// map to 0 to 1
				int singleColor = ((int) (normalized * 255)) & 0xFF;
				return 0xFF_00_00_00
						& (singleColor << 16)
						& (singleColor << 8)
						& (singleColor);
			}
			else if (value >= -10 && value <= 10) {
				float normalized = ((value + 10) / 20);// map to 0 to 1
				int green = ((int) (255 - (normalized * 255))) & 0xFF;
				int red = ((int) (normalized * 255)) & 0xFF;
				return 0xFF_00_00_00
						& (red << 16)
						& (green << 8);
			} else {
				return 0xFF_00_00_FF;
			}
		}, mask.getSize());
	}
	
	// PRIVATE
	
	@FunctionalInterface
	private interface ImageSource {
		/**
		 * @return Color (A)RGB, see {@link ColorModel#getRGBdefault()}, alpha will be ignored.
		 */
		public int get(int x, int y);
	}
	
	private static void visualize(ImageSource imageSource, int size) {
		int imageSize = size * perPixelSize;
		currentImage = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB);
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
		
		frame.repaint();
		setFrameSize();
	}
	
	private static void setFrameSize() {
		Insets insets = frame.getInsets();
		int insetsWidth = insets.left + insets.right + 10;
		int insetsHeight = insets.top + insets.bottom + 10;
		frame.setSize(insetsWidth + currentImage.getWidth(), insetsHeight + currentImage.getHeight());
	}
}
