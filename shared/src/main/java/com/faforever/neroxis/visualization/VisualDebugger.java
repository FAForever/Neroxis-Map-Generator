package com.faforever.neroxis.visualization;

import com.faforever.neroxis.mask.Mask;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class VisualDebugger {
    private static DefaultListModel<MaskListItem> listModel;
    private static JFrame frame;
    private static JList<MaskListItem> list;
    private static EntryPanel canvas;

    public static void visualizeMask(Mask<?, ?> mask) {
        visualizeMask(mask, null, null);
    }

    public static void visualizeMask(Mask<?, ?> mask, String method) {
        visualizeMask(mask, method, null);
    }

    public static void visualizeMask(Mask<?, ?> mask, String method, String line) {
        visualizeMask(mask, method, line, null);
    }

    public static void visualizeMask(Mask<?, ?> mask, String method, String line, Integer index) {
        Mask<?, ?> copyOfmask = mask.immutableCopy();
        SwingUtilities.invokeLater(() -> {
                                       createGui();
                                       String name = copyOfmask.getVisualName();
                                       name = name == null ? copyOfmask.getName() : name;
                                       updateList(name + " " + method + " " + line, copyOfmask.immutableCopy(), index);
                                   }
        );
    }

    public static void createGui() {
        if (isCreated()) {
            return;
        }
        frame = new JFrame();
        frame.setLayout(new GridBagLayout());

        setupList();
        setupCanvas();

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public static boolean isCreated() {
        return frame != null;
    }

    private static void setupList() {
        listModel = new DefaultListModel<>();
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
        listScroller.setPreferredSize(new Dimension(550, 0));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.gridy = 0;
        constraints.weighty = 1;

        frame.add(listScroller, constraints);
    }

    private static void updateVisibleCanvas(MaskListItem maskListItem) {
        if (maskListItem == null) {
            canvas.setMask(null);
            frame.setTitle("");
            return;
        }

        String maskName = maskListItem.maskName();
        Mask<?, ?> mask = maskListItem.mask();
        canvas.setMask(mask);
        frame.setTitle(String.format("Mask: %s MaskSize: %d", maskName, mask.getSize()));
    }

    private static void setupCanvas() {
        canvas = new EntryPanel();
        canvas.setPreferredSize(new Dimension(650, 650));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.gridy = 0;
        constraints.weighty = 1;
        frame.add(canvas, constraints);
    }

    public synchronized static void updateList(String uniqueMaskName, Mask<?, ?> mask, Integer index) {
        if (!uniqueMaskName.isBlank()) {
            if (index == null) {
                int ind = listModel.getSize();
                for (int i = listModel.getSize() - 1; i >= 0; i--) {
                    if (listModel.get(i).maskName.split(" ")[0].equals(uniqueMaskName.split(" ")[0])) {
                        ind = i + 1;
                        break;
                    }
                }
                listModel.add(ind, new MaskListItem(uniqueMaskName, mask));
                if (list.getSelectedIndex() == -1) {
                    list.setSelectedIndex(ind);
                }
            } else {
                if (listModel.getSize() < index + 1) {
                    listModel.setSize(index + 1);
                }
                listModel.set(index, new MaskListItem(uniqueMaskName, mask));
                if (list.getSelectedIndex() == -1) {
                    list.setSelectedIndex(index);
                }
            }


            list.revalidate();
            list.repaint();
        }
    }

    public record MaskListItem(String maskName, Mask<?, ?> mask) {
        @Override
        public String toString() {
            return maskName;
        }
    }
}
