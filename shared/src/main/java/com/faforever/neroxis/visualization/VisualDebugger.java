package com.faforever.neroxis.visualization;

import com.faforever.neroxis.mask.Mask;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisualDebugger {
    private static DefaultListModel<MaskListItem> listModel;
    private static JFrame frame;
    private static JList<MaskListItem> list;
    private static EntryPanel canvas;
    private static final Map<String, List<MaskListItem>> MASK_ITEMS_BY_NAME = new HashMap<>();
    private static JTextField filter;

    public static void visualizeMask(Mask<?, ?> mask) {
        visualizeMask(mask, null, null);
    }

    public static void visualizeMask(Mask<?, ?> mask, String method) {
        visualizeMask(mask, method, null);
    }

    public static void visualizeMask(Mask<?, ?> mask, String method, String line) {
        Mask<?, ?> copyOfmask = mask.immutableCopy();
        SwingUtilities.invokeLater(() -> {
                                       createGui();
                                       String name = copyOfmask.getVisualName();
                                       updateList(name + " " + method + " " + line, copyOfmask.immutableCopy());
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
                if (selectedItem == null) {
                    return;
                }
                updateVisibleCanvas(selectedItem);
            }
        });

        filter = new JTextField();
        filter.addActionListener(event -> refreshList());
        filter.setMinimumSize(new Dimension(350, 50));

        GridBagConstraints filterConstraints = new GridBagConstraints();
        filterConstraints.fill = GridBagConstraints.HORIZONTAL;
        filterConstraints.gridx = 0;
        filterConstraints.weightx = 0;
        filterConstraints.gridy = 0;
        filterConstraints.weighty = 0;

        frame.add(filter, filterConstraints);

        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setMinimumSize(new Dimension(350, 0));
        listScroller.setPreferredSize(new Dimension(350, 0));

        GridBagConstraints scrollerConstraints = new GridBagConstraints();
        scrollerConstraints.fill = GridBagConstraints.BOTH;
        scrollerConstraints.gridx = 0;
        scrollerConstraints.weightx = 0;
        scrollerConstraints.gridy = 1;
        scrollerConstraints.weighty = 1;

        frame.add(listScroller, scrollerConstraints);
    }

    private static void updateVisibleCanvas(MaskListItem maskListItem) {
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
        constraints.gridheight = 2;
        frame.add(canvas, constraints);
    }

    private static void updateList(String uniqueMaskName, Mask<?, ?> mask) {
        MASK_ITEMS_BY_NAME.computeIfAbsent(uniqueMaskName, ignored -> new ArrayList<>())
                          .add(new MaskListItem(uniqueMaskName, mask));
        refreshList();
    }

    private static void refreshList() {
        MaskListItem selectedValue = list.getSelectedValue();
        listModel.clear();
        String text = filter.getText();
        listModel.addAll(MASK_ITEMS_BY_NAME.entrySet()
                                           .stream()
                                           .filter(entry -> text.isBlank() || entry.getKey().contains(text))
                                           .sorted(Map.Entry.comparingByKey())
                                           .map(Map.Entry::getValue)
                                           .flatMap(
                                                   Collection::stream)
                                           .toList());
        list.revalidate();
        list.repaint();
        int selected = listModel.indexOf(selectedValue);
        if (selected != -1) {
            list.setSelectedIndex(selected);
        }
    }

    private record MaskListItem(String maskName, Mask<?, ?> mask) {
        @Override
        public String toString() {
            return maskName;
        }
    }
}
