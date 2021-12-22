package com.faforever.neroxis.ui;

import com.faforever.neroxis.util.Pipeline;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public strictfp class PipelineDebugger {
    private static JFrame frame;
    private static EntryCanvas mainEntryCanvas;
    private static List<EntryCanvas> dependencyEntryCanvases;
    private static EntryGraphCanvas graphCanvas;
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
        createDependencyMaskCanvases();
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

        mainEntryCanvas = new EntryCanvas(new Dimension(450, 450));

        frame.add(mainEntryCanvas, constraints);
    }

    private static void createDependencyMaskCanvases() {
        dependencyEntryCanvases = new ArrayList<>();
        GridBagConstraints constraints1 = new GridBagConstraints();
        constraints1.fill = GridBagConstraints.BOTH;
        constraints1.gridx = 1;
        constraints1.weightx = 1;
        constraints1.gridy = 0;
        constraints1.weighty = 1;

        EntryCanvas entryCanvas1 = new EntryCanvas(new Dimension(250, 250));

        GridBagConstraints constraints2 = new GridBagConstraints();
        constraints2.fill = GridBagConstraints.BOTH;
        constraints2.gridx = 2;
        constraints2.weightx = 1;
        constraints2.gridy = 0;
        constraints2.weighty = 1;

        EntryCanvas entryCanvas2 = new EntryCanvas(new Dimension(250, 250));

        frame.add(entryCanvas1, constraints1);
        frame.add(entryCanvas2, constraints2);

        dependencyEntryCanvases.add(entryCanvas1);
        dependencyEntryCanvases.add(entryCanvas2);
    }

    private static void createGraphCanvas() {
        graphCanvas = new EntryGraphCanvas(pipeline);
        graphCanvas.setMinimumSize(new Dimension(350, 350));
        graphCanvas.setPreferredSize(new Dimension(350, 350));
        graphCanvas.setEntryVertexSelectionAction(entry -> {
            mainEntryCanvas.setEntry(entry);
            int dependencyCount = 0;
            List<Pipeline.Entry> dependencies = new ArrayList<>(entry.getDependencies());
            dependencies.stream().filter(dependency -> dependency.getExecutingMask().equals(entry.getExecutingMask())).findFirst()
                    .ifPresent(dependency -> {
                        dependencies.remove(dependency);
                        dependencies.add(0, dependency);
                    });
            for (Pipeline.Entry dependency : dependencies) {
                if (dependencyCount >= dependencyEntryCanvases.size()) {
                    break;
                }
                EntryCanvas dependencyCanvas = dependencyEntryCanvases.get(dependencyCount);
                dependencyCanvas.setEntry(dependency);
                dependencyCanvas.setVisible(true);
                dependencyCount++;
            }
            for (int i = entry.getDependencies().size(); i < dependencyEntryCanvases.size(); i++) {
                dependencyEntryCanvases.get(i).setVisible(false);
            }
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
