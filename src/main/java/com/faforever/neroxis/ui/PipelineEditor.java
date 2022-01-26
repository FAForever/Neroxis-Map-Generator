package com.faforever.neroxis.ui;

import com.faforever.neroxis.ui.panel.PipelinePanel;
import com.faforever.neroxis.util.DebugUtil;

import javax.swing.*;
import java.awt.*;

public strictfp class PipelineEditor {
    private JFrame frame = new JFrame();
    private PipelinePanel pipelinePanel = new PipelinePanel();

    public static void main(String[] args) {
        DebugUtil.DEBUG = true;
        new PipelineEditor().createGui();
    }

    public void createGui() {
        frame = new JFrame();
        frame.setLayout(new GridBagLayout());

        createGraphCanvas();

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private void createGraphCanvas() {
        pipelinePanel = new PipelinePanel();

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridy = 0;
        constraints.weighty = 1;

        frame.add(pipelinePanel, constraints);
    }

}
