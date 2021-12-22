package com.faforever.neroxis.ui;

import com.faforever.neroxis.mask.Mask;
import lombok.Value;

import javax.swing.*;
import java.awt.*;

public strictfp class VisualDebugger {
    private static final DefaultListModel<MaskListItem> listModel = new DefaultListModel<>();
    private static JFrame frame;
    private static JList<MaskListItem> list;
    private static EntryCanvas canvas;

    public static void visualizeMask(Mask<?, ?> mask, String method) {
        visualizeMask(mask, method, null);
    }

    public static void visualizeMask(Mask<?, ?> mask, String method, String line) {
        createGui();
        String name = mask.getVisualName();
        name = name == null ? mask.getName() : name;
        updateList(name + " " + method + " " + line, mask.mock());
    }

    public static boolean isCreated() {
        return frame != null;
    }

    public static void createGui() {
        if (isCreated()) {
            return;
        }
        frame = new JFrame();
        frame.setLayout(new GridBagLayout());

        createList();
        createCanvas();

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private static void createCanvas() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.gridy = 0;
        constraints.weighty = 1;

        canvas = new EntryCanvas(new Dimension(650, 650));

        frame.add(canvas, constraints);
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
        constraints.weightx = 0;
        constraints.gridy = 0;
        constraints.weighty = 1;

        frame.add(listScroller, constraints);
    }

    public synchronized static void updateList(String uniqueMaskName, Mask<?, ?> mask) {
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
        canvas.setMask(mask);
        frame.setTitle(String.format("Mask: %s MaskSize: %d", maskName, mask.getSize()));
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
}
