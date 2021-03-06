package com.faforever.neroxis.util;

import com.faforever.neroxis.map.mask.Mask;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public strictfp class VisualDebuggerGui {
    private static final DefaultListModel<MaskListItem> listModel = new DefaultListModel<>();
    private static final CanvasMouseListener CANVAS_MOUSE_LISTENER = new CanvasMouseListener();
    private static JFrame frame;
    private static Container contentPane;
    private static JList<MaskListItem> list;
    private static JLabel label;
    private static JPanel canvasContainer;
    private static ImagePanel canvas;
    private static double userZoomScale = 1d;
    private static int mouseX = 0;
    private static int mouseY = 0;
    private static int centeredX = 0;
    private static int centeredY = 0;
    private static double imageXOffset = 0;
    private static double imageYOffset = 0;
    private static double xOffset = 0;
    private static double yOffset = 0;

    public static boolean isCreated() {
        return frame != null;
    }

    public static void createGui() {
        if (frame != null) {
            return;
        }
        frame = new JFrame();
        contentPane = frame.getContentPane();
        contentPane.setLayout(new GridBagLayout());

        createList();
        createLabel();
        createCanvasContainer();

        contentPane.revalidate();
        contentPane.repaint();

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private static void createCanvasContainer() {
        canvasContainer = new JPanel();
        canvasContainer.setMinimumSize(new Dimension(600, 600));
        canvasContainer.setPreferredSize(new Dimension(600, 600));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.weightx = 4;
        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.gridheight = 1;

        contentPane.add(canvasContainer, constraints);

        canvas = new ImagePanel();
        canvas.addMouseListener(CANVAS_MOUSE_LISTENER);
        canvas.addMouseMotionListener(CANVAS_MOUSE_LISTENER);
        canvas.addMouseWheelListener(CANVAS_MOUSE_LISTENER);
        canvasContainer.add(canvas);
    }

    private static void createList() {
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                MaskListItem selectedItem = list.getSelectedValue();
                updateVisibleCanvas(selectedItem);
            }
        });
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setMinimumSize(new Dimension(250, 0));
        listScroller.setPreferredSize(new Dimension(250, 0));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridy = 0;
        constraints.weighty = 1;

        contentPane.add(listScroller, constraints);
    }

    private static void createLabel() {
        label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridwidth = 2;
        constraints.gridy = 1;
        constraints.weighty = 0;

        contentPane.add(label, constraints);
    }

    public synchronized static void update(String uniqueMaskName, Mask<?, ?> mask) {
        if (!uniqueMaskName.isEmpty()) {
            int ind = listModel.getSize();
            for (int i = 0; i < listModel.getSize(); i++) {
                if (listModel.get(i).maskName.split(" ")[0].equals(uniqueMaskName.split(" ")[0])) {
                    ind = i + 1;
                }
            }

            listModel.insertElementAt(new MaskListItem(uniqueMaskName, mask), ind);
            if (list.getSelectedIndex() == -1) {
                list.setSelectedIndex(ind);
            }
            list.revalidate();
            list.repaint();
        }
    }

    private static void updateVisibleCanvas(MaskListItem maskListItem) {
        String maskName = maskListItem.getMaskName();
        Mask<?, ?> mask = maskListItem.getMask();
        Point locationOnScreen = MouseInfo.getPointerInfo().getLocation();
        Point locationOnComponent = new Point(locationOnScreen);
        SwingUtilities.convertPointFromScreen(locationOnComponent, canvas);
        canvas.setMask(mask);
        setLabel();
        contentPane.revalidate();
        contentPane.repaint();
        frame.setTitle("Mask: " + maskName + ", Size: " + canvas.getImage().getHeight());
    }

    private static void setLabel() {
        Mask<?, ?> mask = canvas.getMask();
        if (mask != null) {
            int maskX = (int) ((mouseX - xOffset) / userZoomScale / canvas.getImageZoomScaleX());
            int maskY = (int) ((mouseY - yOffset) / userZoomScale / canvas.getImageZoomScaleY());
            if (mask.inBounds(maskX, maskY)) {
                label.setText(String.format("X: %5d, Y: %5d \t Value: %s", maskX, maskY, mask.get(maskX, maskY).toString()));
            }
        }
    }

    private static void setUserZoomScale(double scale) {
        userZoomScale = StrictMath.max(scale, 1);
    }

    private static void setOffsets(double x, double y) {
        xOffset = StrictMath.max(canvasContainer.getWidth() * (1 - userZoomScale), StrictMath.min(0, x));
        yOffset = StrictMath.max(canvasContainer.getHeight() * (1 - userZoomScale), StrictMath.min(0, y));
    }

    /**
     * Panel that shows the given image.
     * Call {@link JPanel#revalidate()} to resize panel when image size changes.
     * Call {@link JPanel#repaint()} to update when image content changes.
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class ImagePanel extends JPanel {
        private BufferedImage image;
        private Mask<?, ?> mask;
        private double imageZoomScaleX;
        private double imageZoomScaleY;

        public ImagePanel() {
            image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
            paintCheckerboard();
        }

        public void setMask(Mask<?, ?> mask) {
            this.mask = mask;
            image = new BufferedImage(mask.getImmediateSize(), mask.getImmediateSize(), BufferedImage.TYPE_INT_RGB);
            paintCheckerboard();

            Graphics g = image.getGraphics();
            g.drawImage(mask.toImage(), 0, 0, this);

            imageZoomScaleX = (double) getWidth() / image.getWidth();
            imageZoomScaleY = (double) getHeight() / image.getHeight();
        }

        private void paintCheckerboard() {
            for (int x = 0; x < image.getWidth(); ++x) {
                for (int y = 0; y < image.getHeight(); ++y) {
                    if ((x / 4 + y / 4) % 2 == 0) {
                        image.setRGB(x, y, Color.GRAY.getRGB());
                    } else {
                        image.setRGB(x, y, Color.DARK_GRAY.getRGB());
                    }
                }
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(canvasContainer.getWidth(), canvasContainer.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();

            setOffsets(-(imageXOffset * userZoomScale - centeredX), -(imageYOffset * userZoomScale - centeredY));

            imageZoomScaleX = (double) getWidth() / image.getWidth();
            imageZoomScaleY = (double) getHeight() / image.getHeight();
            AffineTransform at = new AffineTransform();
            at.translate(xOffset, yOffset);
            at.scale(imageZoomScaleX * userZoomScale, imageZoomScaleY * userZoomScale);
            AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            BufferedImage newImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            op.filter(image, newImage);
            g2d.drawImage(newImage, 0, 0, this);
            g2d.dispose();
        }
    }

    @Value
    public static class MaskListItem {
        String maskName;
        Mask<?, ?> mask;

        @Override
        public String toString() {
            return maskName;
        }
    }

    private static class CanvasMouseListener extends MouseInputAdapter {
        private int x;
        private int y;

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            centeredX = e.getX();
            centeredY = e.getY();
            imageXOffset = (e.getX() - xOffset) / userZoomScale;
            imageYOffset = (e.getY() - yOffset) / userZoomScale;
            setUserZoomScale(userZoomScale - e.getWheelRotation() / 1.25d);
            ((ImagePanel) e.getSource()).repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            x = e.getX();
            y = e.getY();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            ImagePanel sourceImagePanel = (ImagePanel) e.getSource();
            BufferedImage sourceImage = sourceImagePanel.getImage();
            imageXOffset += (x - e.getX()) / (userZoomScale * sourceImagePanel.imageZoomScaleX);
            imageYOffset += (y - e.getY()) / (userZoomScale * sourceImagePanel.imageZoomScaleY);
            imageXOffset = StrictMath.min(sourceImage.getWidth(), StrictMath.max(0, imageXOffset));
            imageYOffset = StrictMath.min(sourceImage.getWidth(), StrictMath.max(0, imageYOffset));
            x = e.getX();
            y = e.getY();
            sourceImagePanel.repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
            setLabel();
        }
    }
}
