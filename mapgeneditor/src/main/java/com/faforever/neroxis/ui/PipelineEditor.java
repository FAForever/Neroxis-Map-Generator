package com.faforever.neroxis.ui;

import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.debugger.EntryPanel;
import com.faforever.neroxis.generator.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.ui.components.CloseableTabComponent;
import com.faforever.neroxis.ui.components.GraphPane;
import com.faforever.neroxis.ui.components.MaskGraphVertexEditPanel;
import com.faforever.neroxis.ui.components.PipelineGraph;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import picocli.CommandLine;

@CommandLine.Command(name = "Editor", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class, description = "Tool for creating generator pipelines")
public strictfp class PipelineEditor implements Callable<Integer> {
    private final JFrame frame = new JFrame();
    private final MaskGraphVertexEditPanel vertexEditPanel = new MaskGraphVertexEditPanel();
    private final EntryPanel entryPanel = new EntryPanel();
    private final JFileChooser fileChooser = new JFileChooser();
    private final JComboBox<Integer> mapSizeComboBox = new JComboBox<>();
    private final JComboBox<Integer> spawnCountComboBox = new JComboBox<>();
    private final JComboBox<Integer> numTeamsComboBox = new JComboBox<>();
    private final JComboBox<Symmetry> terrainSymmetryComboBox = new JComboBox<>();
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private PipelineGraph savedGraph;
    @CommandLine.Mixin
    private DebugMixin debugMixin;
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new PipelineEditor());
        commandLine.setAbbreviatedOptionsAllowed(true);
        commandLine.execute(args);
    }

    @Override
    public Integer call() {
        frame.setLayout(new GridBagLayout());
        setupGraphTabPane();
        setupVertexEditPanel();
        setupEntryPanel();
        setupMapOptions();
        setupButtons();
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        return 0;
    }

    private void setupGraphTabPane() {
        addNewGraphTab();
        tabbedPane.setPreferredSize(new Dimension(800, 800));
        tabbedPane.addChangeListener(e -> {
            int plusTabIndex = tabbedPane.indexOfTab("+");
            if (tabbedPane.getSelectedIndex() == plusTabIndex && plusTabIndex != -1) {
                tabbedPane.removeTabAt(plusTabIndex);
                addNewGraphTab();
            } else {
                GraphPane graphPane = (GraphPane) tabbedPane.getSelectedComponent();
                vertexEditPanel.setGraphPane(graphPane);
            }
        });
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.weightx = 4;
        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.gridheight = 5;
        frame.add(tabbedPane, constraints);
    }

    private void addNewGraphTab() {
        GraphPane graphPane = new GraphPane();
        graphPane.getGraphComponent().addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                boolean controlDown = e.isControlDown();
                GraphPane graphPane = (GraphPane) tabbedPane.getSelectedComponent();
                if (controlDown && graphPane != null) {
                    if (e.getKeyCode() == KeyEvent.VK_C) {
                        savedGraph = graphPane.getGraph().getSubGraphFromSelectedCells();
                    } else if (e.getKeyCode() == KeyEvent.VK_V && savedGraph != null) {
                        graphPane.importGraph(savedGraph);
                    }
                }
            }
        });
        graphPane.setGraphChangedAction(vertexEditPanel::updatePanel);
        graphPane.setMaskVertexSelectionAction(vertex -> {
            vertexEditPanel.setVertex(vertex);
            if (vertex != null && vertex.isComputed()) {
                entryPanel.setMask(vertex.getImmutableResult(MaskGraphVertex.SELF));
            }
        });
        CloseableTabComponent closeableTabComponent = new CloseableTabComponent(tabbedPane);
        tabbedPane.addTab("", graphPane);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, closeableTabComponent);
        closeableTabComponent.setTitle("New");
        tabbedPane.addTab("+", null);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
        vertexEditPanel.setGraphPane(graphPane);
    }

    private void setupVertexEditPanel() {
        vertexEditPanel.setPreferredSize(new Dimension(400, 300));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridy = 0;
        constraints.weighty = 1;
        frame.add(vertexEditPanel, constraints);
    }

    private void setupEntryPanel() {
        entryPanel.setPreferredSize(new Dimension(400, 400));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridy = 1;
        constraints.weighty = 1;
        frame.add(entryPanel, constraints);
    }

    private void setupMapOptions() {
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(0, 2));
        JLabel mapSizeLabel = new JLabel();
        mapSizeLabel.setText("Map Size");
        optionsPanel.add(mapSizeLabel);
        optionsPanel.add(mapSizeComboBox);
        IntStream.range(4, 17).forEach(i -> mapSizeComboBox.addItem(i * 64));
        JLabel spawnCountLabel = new JLabel();
        spawnCountLabel.setText("Spawn Count");
        optionsPanel.add(spawnCountLabel);
        optionsPanel.add(spawnCountComboBox);
        IntStream.range(2, 17).forEach(spawnCountComboBox::addItem);
        spawnCountComboBox.addActionListener(e -> {
            Object selected = numTeamsComboBox.getSelectedItem();
            numTeamsComboBox.removeAllItems();
            IntStream.range(1, 9).filter(i -> ((int) spawnCountComboBox.getSelectedItem() % i) == 0).forEach(numTeamsComboBox::addItem);
            if (selected != null) {
                numTeamsComboBox.setSelectedItem(selected);
            }
        });
        JLabel numTeamsLabel = new JLabel();
        numTeamsLabel.setText("Num Teams");
        optionsPanel.add(numTeamsLabel);
        optionsPanel.add(numTeamsComboBox);
        numTeamsComboBox.addActionListener(e -> {
            Object selected = terrainSymmetryComboBox.getSelectedItem();
            terrainSymmetryComboBox.removeAllItems();
            if (numTeamsComboBox.getSelectedItem() != null) {
                Arrays.stream(Symmetry.values()).filter(symmetry -> (symmetry.getNumSymPoints() % (int) numTeamsComboBox.getSelectedItem()) == 0).forEach(terrainSymmetryComboBox::addItem);
                if (selected != null) {
                    terrainSymmetryComboBox.setSelectedItem(selected);
                }
            }
        });
        spawnCountComboBox.setSelectedIndex(0);
        JLabel terrainSymmetryLabel = new JLabel();
        terrainSymmetryLabel.setText("Terrain Symmetry");
        optionsPanel.add(terrainSymmetryLabel);
        optionsPanel.add(terrainSymmetryComboBox);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridy = 2;
        constraints.weighty = 0;
        frame.add(optionsPanel, constraints);
    }

    private void setupButtons() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 2));
        JButton layoutButton = new JButton();
        layoutButton.setText("Layout Graph");
        layoutButton.addActionListener(e -> ((GraphPane) tabbedPane.getSelectedComponent()).layoutGraph());
        buttonPanel.add(layoutButton);
        JButton runButton = new JButton();
        runButton.setText("Test Run");
        runButton.addActionListener(e -> runGraph());
        buttonPanel.add(runButton);
        JButton loadButton = new JButton();
        loadButton.setText("Load");
        loadButton.addActionListener(e -> importGraph());
        buttonPanel.add(loadButton);
        JButton saveButton = new JButton();
        saveButton.setText("Save");
        saveButton.addActionListener(e -> exportGraph());
        buttonPanel.add(saveButton);
        JButton importButton = new JButton();
        importButton.setText("Clear Graph");
        importButton.addActionListener(e -> ((GraphPane) tabbedPane.getSelectedComponent()).clearGraph());
        buttonPanel.add(importButton);
        JButton exportButton = new JButton();
        exportButton.setText("Export Selected Nodes");
        exportButton.addActionListener(e -> exportSelectedCells());
        buttonPanel.add(exportButton);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.gridy = 3;
        constraints.weighty = 0;
        frame.add(buttonPanel, constraints);
    }

    private void runGraph() {
        ((GraphPane) tabbedPane.getSelectedComponent()).runGraph((Integer) numTeamsComboBox.getSelectedItem(), (Integer) mapSizeComboBox.getSelectedItem(), (Integer) spawnCountComboBox.getSelectedItem(), (Symmetry) terrainSymmetryComboBox.getSelectedItem());
    }

    private void exportGraph() {
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            int index = tabbedPane.getSelectedIndex();
            ((GraphPane) tabbedPane.getComponentAt(index)).exportGraph(file);
            ((CloseableTabComponent) tabbedPane.getTabComponentAt(index)).setTitle(file.getName());
        }
    }

    private void importGraph() {
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            int index = tabbedPane.getSelectedIndex();
            if (((GraphPane) tabbedPane.getComponentAt(index)).getGraph().vertexSet().isEmpty()) {
                ((CloseableTabComponent) tabbedPane.getTabComponentAt(index)).setTitle(file.getName());
            }
            ((GraphPane) tabbedPane.getComponentAt(index)).importGraph(file);
        }
    }

    private void exportSelectedCells() {
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            ((GraphPane) tabbedPane.getSelectedComponent()).exportSelectedCells(file);
        }
    }
}
