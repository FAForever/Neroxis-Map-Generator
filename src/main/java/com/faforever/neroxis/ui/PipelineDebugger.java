package com.faforever.neroxis.ui;

import com.faforever.neroxis.ui.components.EntryGraphPanel;
import com.faforever.neroxis.ui.components.EntryPanel;
import com.faforever.neroxis.util.Pipeline;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public strictfp class PipelineDebugger {
    private static JFrame frame;
    private static EntryPanel mainEntryPanel;
    private static List<Pipeline.Entry> pipeline;

    public static void setPipeline(List<Pipeline.Entry> pipeline) {
        PipelineDebugger.pipeline = pipeline;
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

        createGraphCanvas();
        createMaskCanvases();

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private static void createMaskCanvases() {
        createMainMaskCanvas();
    }

    private static void createMainMaskCanvas() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.gridy = 1;
        constraints.gridheight = 4;
        constraints.weighty = 1;

        mainEntryPanel = new EntryPanel(new Dimension(450, 450));

        frame.add(mainEntryPanel, constraints);
    }

    private static void createGraphCanvas() {
        EntryGraphPanel graphCanvas = new EntryGraphPanel(pipeline);
        graphCanvas.setMinimumSize(new Dimension(450, 450));
        graphCanvas.setPreferredSize(new Dimension(450, 450));
        graphCanvas.setEntryVertexSelectionAction(entry -> {
            mainEntryPanel.setMask(entry.getImmutableResult());
            frame.setTitle(String.format("Mask: %s MaskSize: %d", entry.getExecutingMask().getName(), entry.getImmutableResult().getSize()));
        });

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridy = 0;
        constraints.gridheight = 6;
        constraints.weighty = 1;

        frame.add(graphCanvas, constraints);
    }

}
