package generator;

import lombok.Value;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class VisualDebuggerGui {

    public static final boolean AUTO_SELECT_NEW_MASKS = false;
    private static JFrame frame;
    private static Container contentPane;
    private static JList<MaskListItem> list;
    private static JPanel canvasContainer;
    private static final DefaultListModel<MaskListItem> listModel = new DefaultListModel<>();
    private static final Map<String, ImagePanel> maskNameToCanvas = new HashMap<>();

    public static boolean isCreated() {
        return frame != null;
    }

    public static void createGui() {
        if (frame != null) {
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

    public static void update(String uniqueMaskName, BufferedImage image, int zoomFactor) {
        if (!uniqueMaskName.isEmpty()) {
            if (!uniqueMaskName.equals("lastChanged")) {
                int count = 0;
                for (int i = 0; i < listModel.getSize(); i++) {
                    if (listModel.get(i).maskName.matches(uniqueMaskName + ".*")) {
                        count++;
                    }
                }
                uniqueMaskName = uniqueMaskName + count;
            }
            boolean isNewMask = !maskNameToCanvas.containsKey(uniqueMaskName);
            if (isNewMask) {
                maskNameToCanvas.put(uniqueMaskName, new ImagePanel());
                listModel.addElement(new MaskListItem(uniqueMaskName));
            }
            ImagePanel canvas = maskNameToCanvas.get(uniqueMaskName);
            canvas.setViewModel(image, zoomFactor);
            if (isNewMask && (listModel.getSize() == 1 || AUTO_SELECT_NEW_MASKS)) {
                list.setSelectedIndex(listModel.getSize() - 1);
            } else {
                MaskListItem selected = list.getSelectedValue();
                if (selected != null && selected.maskName.equals(uniqueMaskName)) {
                    updateVisibleCanvas(uniqueMaskName, canvas);
                }
            }
        }
    }

    public static void remove(String uniqueMaskName) {
        listModel.removeElement(new MaskListItem(uniqueMaskName));
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
        frame.pack();
        frame.setTitle("Mask: " + maskName + ", Zoom: x" + canvas.getZoomFactor());
    }

    /**
     * Panel that shows the given image.
     * Call {@link JPanel#revalidate()} to resize panel when image size changes.
     * Call {@link JPanel#repaint()} to update when image content changes.
     */
    @SuppressWarnings("serial")
    public static class ImagePanel extends JPanel {
        private final int padding = 10;

        private BufferedImage image;
        private int zoomFactor;

        public void setViewModel(BufferedImage image, int zoomFactor) {
            this.image = image;
            this.zoomFactor = zoomFactor;
        }

        public int getZoomFactor() {
            return zoomFactor;
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
