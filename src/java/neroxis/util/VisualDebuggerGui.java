package neroxis.util;

import lombok.Value;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public strictfp class VisualDebuggerGui {

    private static final DefaultListModel<MaskListItem> listModel = new DefaultListModel<>();
    private static final Map<String, ImagePanel> maskNameToCanvas = new HashMap<>();
    private static JFrame frame;
    private static Container contentPane;
    private static JList<MaskListItem> list;
    private static JPanel canvasContainer;

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

    public synchronized static void update(String uniqueMaskName, BufferedImage image, int size) {
        if (!uniqueMaskName.isEmpty()) {
            int ind = listModel.getSize();
            int count = 0;
            for (int i = 0; i < listModel.getSize(); i++) {
                if (listModel.get(i).maskName.split(" ")[0].equals(uniqueMaskName.split(" ")[0])) {
                    ind = i + 1;
                    count += 1;
                }
            }
            uniqueMaskName = String.format("%s %d", uniqueMaskName, count);
            maskNameToCanvas.put(uniqueMaskName, new ImagePanel());
            listModel.insertElementAt(new MaskListItem(uniqueMaskName), ind);
            ImagePanel canvas = maskNameToCanvas.get(uniqueMaskName);
            canvas.setViewModel(image, size);
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
        frame.setTitle("Mask: " + maskName + ", Size: " + canvas.getImageSize());
    }

    /**
     * Panel that shows the given image.
     * Call {@link JPanel#revalidate()} to resize panel when image size changes.
     * Call {@link JPanel#repaint()} to update when image content changes.
     */
    public static class ImagePanel extends JPanel {

        private BufferedImage image;
        private int imageSize;

        public void setViewModel(BufferedImage image, int imageSize) {
            this.image = image;
            this.imageSize = imageSize;
        }

        public float getImageSize() {
            return imageSize;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            BufferedImage currentImage = image;
            int x = (getWidth() - currentImage.getWidth()) / 2;
            int y = (getHeight() - currentImage.getHeight()) / 2;
            g2d.drawImage(currentImage, x, y, this);
            g2d.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            BufferedImage currentImage = image;
            int padding = 10;
            return new Dimension(currentImage.getWidth() + padding, currentImage.getHeight() + padding);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }
    }

    @Value
    public static class MaskListItem {
        String maskName;

        @Override
        public String toString() {
            return "  " + maskName + "  ";
        }
    }
}
