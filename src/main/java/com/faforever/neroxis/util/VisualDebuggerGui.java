package com.faforever.neroxis.util;

import com.faforever.neroxis.map.BooleanMask;
import com.faforever.neroxis.map.Mask;
import com.faforever.neroxis.map.NumberMask;
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
import java.util.HashMap;
import java.util.Map;

public strictfp class VisualDebuggerGui {

    private static final int IMAGE_SIZE = 512;

    private static final DefaultListModel<MaskListItem> listModel = new DefaultListModel<>();
    private static final Map<String, ImagePanel> maskNameToCanvas = new HashMap<>();
    private static final CanvasMouseListener CANVAS_MOUSE_LISTENER = new CanvasMouseListener();
    private static JFrame frame;
    private static Container contentPane;
    private static JList<MaskListItem> list;
    private static JPanel canvasContainer;
    private static double userZoomScale = 1d;
    private static double xOffset = 0;
    private static double yOffset = 0;

    public static boolean isCreated() {
        return frame != null;
    }

    public static void createGui() {
        if (frame != null || !VisualDebugger.ENABLED) {
            return;
        }
        frame = new JFrame();
        contentPane = frame.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));

        createList();
        canvasContainer = new JPanel();
        contentPane.add(canvasContainer);

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private static void createList() {
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                MaskListItem selectedItem = list.getSelectedValue();
                onSelect(selectedItem.maskName);
            }
        });
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setMinimumSize(new Dimension(100, 0));
        contentPane.add(listScroller);
    }

    public synchronized static void update(String uniqueMaskName, BufferedImage image, Mask<?, ?> mask, String line) {
        if (!uniqueMaskName.isEmpty()) {
            int ind = listModel.getSize();
            for (int i = 0; i < listModel.getSize(); i++) {
                if (listModel.get(i).maskName.split(" ")[0].equals(uniqueMaskName.split(" ")[0])) {
                    ind = i + 1;
                }
            }
            uniqueMaskName = String.format("%s %s", uniqueMaskName, line);
            maskNameToCanvas.put(uniqueMaskName, new ImagePanel());
            listModel.insertElementAt(new MaskListItem(uniqueMaskName), ind);
            ImagePanel canvas = maskNameToCanvas.get(uniqueMaskName);
            canvas.setToolTipText("");
            canvas.addMouseListener(CANVAS_MOUSE_LISTENER);
            canvas.addMouseMotionListener(CANVAS_MOUSE_LISTENER);
            canvas.addMouseWheelListener(CANVAS_MOUSE_LISTENER);
            canvas.setViewModel(image, mask);
            if (list.getSelectedIndex() == -1) {
                list.setSelectedIndex(ind);
            }
        }
    }

    private static void onSelect(String uniqueMaskName) {
        ImagePanel selectedCanvas = maskNameToCanvas.get(uniqueMaskName);
        canvasContainer.removeAll();
        canvasContainer.add(selectedCanvas);
        updateVisibleCanvas(uniqueMaskName, selectedCanvas);
    }

    private static void updateVisibleCanvas(String maskName, ImagePanel canvas) {
        canvas.revalidate();
        canvas.repaint();
        contentPane.repaint();
        frame.pack();
        frame.setTitle("Mask: " + maskName + ", Size: " + canvas.getImage().getHeight());
    }

    private static void setUserZoomScale(double scale) {
        userZoomScale = StrictMath.max(scale, 1);
    }

    private static void setOffsets(double x, double y) {
        xOffset = StrictMath.max(IMAGE_SIZE * (1 - userZoomScale), StrictMath.min(0, x));
        yOffset = StrictMath.max(IMAGE_SIZE * (1 - userZoomScale), StrictMath.min(0, y));
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

        public void setViewModel(BufferedImage image, Mask<?, ?> mask) {
            this.image = image;
            this.mask = mask;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
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

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(IMAGE_SIZE, IMAGE_SIZE);
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            if (mask instanceof NumberMask<?, ?>) {
                return ((NumberMask<?, ?>) mask).getValueAt((int) ((e.getX() - xOffset) / userZoomScale / imageZoomScaleX), (int) ((e.getY() - yOffset) / userZoomScale / imageZoomScaleY)).toString();
            } else if (mask instanceof BooleanMask) {
                return ((BooleanMask) mask).getValueAt((int) ((e.getX() - xOffset) / userZoomScale / imageZoomScaleX), (int) ((e.getY() - yOffset) / userZoomScale / imageZoomScaleY)).toString();
            } else {
                return null;
            }
        }
    }

    @Value
    public static class MaskListItem {
        String maskName;

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
            double imageXOffset = (e.getX() - xOffset) / userZoomScale;
            double imageYOffset = (e.getY() - yOffset) / userZoomScale;
            setUserZoomScale(userZoomScale - e.getWheelRotation() / 1.25d);
            setOffsets(-(imageXOffset * userZoomScale - e.getX()), -(imageYOffset * userZoomScale - e.getY()));
            ((ImagePanel) e.getSource()).repaint();
            contentPane.repaint();
        }

        public void mousePressed(MouseEvent e) {
            x = e.getX();
            y = e.getY();
        }

        public void mouseDragged(MouseEvent e) {
            setOffsets(xOffset - (x - e.getX()), yOffset - (y - e.getY()));
            x = e.getX();
            y = e.getY();
            ((ImagePanel) e.getSource()).repaint();
            contentPane.repaint();
        }
    }
}
