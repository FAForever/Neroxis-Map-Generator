package generator;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class VisualDebuggerGui {

	/**
	 * Panel that shows the given image.
	 * Call {@link JPanel#revalidate()} to resize panel when image size changes.
	 * Call {@link JPanel#repaint()} to update when image content changes.
	 */
	@SuppressWarnings("serial")
	public static class ImagePanel extends JPanel {
		private Supplier<BufferedImage> image;
		private int padding;
		
		public ImagePanel(Supplier<BufferedImage> image, int padding) {
			this.image = image;
			this.padding = padding;
		}
		@Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            BufferedImage currentImage = image.get();
            int x = (getWidth() - currentImage.getWidth()) / 2;
            int y = (getHeight() - currentImage.getHeight()) / 2;
            g2d.drawImage(currentImage, x, y, this);
            g2d.dispose();
        }
		@Override
		public Dimension getPreferredSize() {
			BufferedImage currentImage = image.get();
			return new Dimension(currentImage.getWidth() + padding, currentImage.getHeight() + padding);
		}
		@Override
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}
	}
	
	private static JFrame frame;
	private static Container contentPane;
	private static JPanel canvas;
	
	public static void createGui(JPanel canvas) {
		if (frame != null) {
			return;
		}
		VisualDebuggerGui.canvas = canvas;
		frame = new JFrame();
		contentPane = frame.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));
		contentPane.add(canvas);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	
	public static void update(String title) {
		canvas.revalidate();
		canvas.repaint();
		frame.pack();
		frame.setTitle(title);
	}
	
	public static void setTitle(String title) {
		frame.setTitle(title);
	}
}
