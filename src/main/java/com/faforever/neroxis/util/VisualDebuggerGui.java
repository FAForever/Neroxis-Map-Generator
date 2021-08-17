package com.faforever.neroxis.util;

import com.faforever.neroxis.mask.Mask;
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
    private static ImagePanel canvas;
    private static final Vector2 mousePosition = new Vector2();
    private static final Vector2 fractionalImageOffset = new Vector2();
    private static final Vector2 imageZoomFactor = new Vector2();
    private static float userZoomLevel = 0;

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
        createCanvas();

        contentPane.revalidate();
        contentPane.repaint();

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private static void createCanvas() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.weightx = 10;
        constraints.gridy = 0;
        constraints.gridheight = 2;
        constraints.weighty = 1;

        canvas = new ImagePanel();
        canvas.setMinimumSize(new Dimension(750, 750));
        canvas.setPreferredSize(new Dimension(750, 750));
        canvas.addMouseListener(CANVAS_MOUSE_LISTENER);
        canvas.addMouseMotionListener(CANVAS_MOUSE_LISTENER);
        canvas.addMouseWheelListener(CANVAS_MOUSE_LISTENER);

        contentPane.add(canvas, constraints);
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
        listScroller.setMinimumSize(new Dimension(350, 0));
        listScroller.setPreferredSize(new Dimension(350, 0));

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
        frame.setTitle(String.format("Mask: %s MaskSize: %d", maskName, canvas.mask.getSize()));
    }

    private static void setLabel() {
        Mask<?, ?> mask = canvas.mask;
        if (mask != null) {
            Vector2 maskCoords = canvasCoordinatesToMaskCoordinates(mousePosition).floor();
            if (mask.inBounds(maskCoords)) {
                label.setText(String.format("X: %5.0f, Y: %5.0f Value: %s", maskCoords.getX(), maskCoords.getY(), mask.get(maskCoords).toString()));
            }
        }
    }

    private static Vector2 canvasCoordinatesToMaskCoordinates(Vector2 canvasCoords) {
        return canvasCoordinatesToFractionalMaskCoordinates(canvasCoords).multiply(canvas.mask.getSize());
    }

    private static Vector2 canvasCoordinatesToFractionalMaskCoordinates(Vector2 canvasCoords) {
        return canvasCoords.copy().divide(getFullScalingVector().multiply(canvas.mask.getSize())).subtract(fractionalImageOffset);
    }

    private static Vector2 maskCoordinatesToCanvasCoordinates(Vector2 maskCoords) {
        return maskCoords.copy().add(fractionalImageOffset.multiply(canvas.mask.getSize())).multiply(getFullScalingVector());
    }

    private static Vector2 getFullScalingVector() {
        return new Vector2().add(getUserScaleFactor()).multiply(imageZoomFactor);
    }

    private static float getUserScaleFactor() {
        return (float) StrictMath.pow(2, userZoomLevel);
    }

    /**
     * Panel that shows the given image.
     * Call {@link JPanel#revalidate()} to resize panel when image size changes.
     * Call {@link JPanel#repaint()} to update when image content changes.
     */
    private static class ImagePanel extends JPanel {
        private BufferedImage image;
        private Mask<?, ?> mask;

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
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (canvas.mask != null) {

                imageZoomFactor.setX((float) getWidth() / image.getWidth());
                imageZoomFactor.setY((float) getHeight() / image.getHeight());
                Vector2 fullScalingVector = getFullScalingVector();
                Vector2 imageOffset = fractionalImageOffset.copy().multiply(canvas.mask.getSize());

                AffineTransform at = new AffineTransform();
                at.scale(fullScalingVector.getX(), fullScalingVector.getY());
                at.translate(imageOffset.getX(), imageOffset.getY());
                AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                BufferedImage newImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                op.filter(image, newImage);

                Graphics2D g2d = (Graphics2D) g.create();
                g2d.drawImage(newImage, 0, 0, this);
                g2d.dispose();

                setLabel();
            }
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
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            mousePosition.set(e.getPoint());
            Vector2 oldMousePositionOnMask = canvasCoordinatesToFractionalMaskCoordinates(mousePosition);
            userZoomLevel = (float) StrictMath.min(StrictMath.max(userZoomLevel - e.getWheelRotation() * .25, 0), MathUtils.log2(canvas.mask.getSize()));
            Vector2 newMousePositionOnMask = canvasCoordinatesToFractionalMaskCoordinates(mousePosition);
            fractionalImageOffset.subtract(oldMousePositionOnMask).add(newMousePositionOnMask);
            boundOffset();
            ((ImagePanel) e.getSource()).repaint();
        }

        private void boundOffset() {
            fractionalImageOffset.min(0).max(1 / getUserScaleFactor() - 1);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Vector2 newMousePosition = new Vector2(e.getPoint());
            ImagePanel sourceImagePanel = (ImagePanel) e.getSource();
            fractionalImageOffset.subtract(mousePosition.copy().subtract(newMousePosition).divide(getFullScalingVector().multiply(canvas.mask.getSize())));
            boundOffset();
            mousePosition.set(newMousePosition);
            sourceImagePanel.repaint();
            setLabel();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mousePosition.set(e.getPoint());
            setLabel();
        }
    }
}
