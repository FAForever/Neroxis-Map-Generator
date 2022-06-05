package com.faforever.neroxis.ui;

import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.generator.graph.GeneratorPipeline;
import com.faforever.neroxis.generator.serial.GeneratorGraphSerializationUtil;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.ui.components.CloseableTabComponent;
import com.faforever.neroxis.ui.components.MaskGraphVertexEditPanel;
import com.faforever.neroxis.ui.components.PipelineGraph;
import com.faforever.neroxis.ui.components.PipelinePane;
import com.faforever.neroxis.visualization.EntryPanel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import picocli.CommandLine;

@CommandLine.Command(name = "Editor", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class, description = "Tool for creating generator pipelines")
public strictfp class PipelineEditor implements Callable<Integer> {
    public static final String NEW_TAB_TITLE = "+";
    private final JFrame frame = new JFrame();
    private final MaskGraphVertexEditPanel vertexEditPanel = new MaskGraphVertexEditPanel();
    private final EntryPanel entryPanel = new EntryPanel();
    private final JFileChooser fileChooser = new JFileChooser();
    private final JTextField seedTextField = new JTextField();
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
        addNewPaneTabButtonToEnd();
        tabbedPane.setPreferredSize(new Dimension(800, 800));
        tabbedPane.addChangeListener(e -> {
            int plusTabIndex = tabbedPane.indexOfTab(NEW_TAB_TITLE);
            if (tabbedPane.getSelectedIndex() == plusTabIndex && plusTabIndex != -1) {
                JPopupMenu typePopupMenu = new JPopupMenu();
                GeneratorPipeline.getPipelineTypes().forEach(pipelineClass -> {
                    AbstractAction action = new AbstractAction(pipelineClass.getSimpleName().replace("Pipeline", "")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            addNewGraphTab(GeneratorPipeline.createNew(pipelineClass));
                        }
                    };
                    typePopupMenu.add(action);
                });
                Point location = tabbedPane.getMousePosition();
                typePopupMenu.show(tabbedPane, location.x, location.y);
                tabbedPane.setSelectedIndex(-1);
            } else {
                PipelinePane pipelinePane = (PipelinePane) tabbedPane.getSelectedComponent();
                vertexEditPanel.setPipelinePane(pipelinePane);
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

    private void addNewGraphTab(GeneratorPipeline pipeline) {
        PipelinePane pipelinePane = new PipelinePane(pipeline);
        pipelinePane.getGraphComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                boolean controlDown = e.isControlDown();
                PipelinePane pipelinePane = (PipelinePane) tabbedPane.getSelectedComponent();
                if (controlDown && pipelinePane != null) {
                    if (e.getKeyCode() == KeyEvent.VK_C) {
                        savedGraph = pipelinePane.getGraph().getSubGraphFromSelectedCells();
                    } else if (e.getKeyCode() == KeyEvent.VK_V && savedGraph != null) {
                        pipelinePane.importSubGraph(savedGraph);
                    }
                }
            }
        });
        pipelinePane.setGraphChangedAction(vertexEditPanel::updatePanel);
        pipelinePane.setMaskVertexSelectionAction(vertex -> {
            vertexEditPanel.setVertex(vertex);
            if (vertex != null && vertex.isComputed()) {
                entryPanel.setMask(vertex.getImmutableResult(MaskGraphVertex.SELF));
            }
        });
        CloseableTabComponent closeableTabComponent = new CloseableTabComponent(tabbedPane);
        tabbedPane.addTab("", pipelinePane);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, closeableTabComponent);
        closeableTabComponent.setTitle(
                String.format("New (%s)", pipeline.getClass().getSimpleName().replace("Pipeline", "")));
        addNewPaneTabButtonToEnd();
        vertexEditPanel.setPipelinePane(pipelinePane);
    }

    private void addNewPaneTabButtonToEnd() {
        int plusTabIndex = tabbedPane.indexOfTab(NEW_TAB_TITLE);
        if (plusTabIndex >= 0) {
            tabbedPane.removeTabAt(plusTabIndex);
        }
        tabbedPane.addTab(NEW_TAB_TITLE, null);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
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

        optionsPanel.add(new JLabel("Seed"));
        optionsPanel.add(seedTextField);

        optionsPanel.add(new JLabel("Map Size"));
        optionsPanel.add(mapSizeComboBox);

        IntStream.range(4, 17).forEach(i -> mapSizeComboBox.addItem(i * 64));
        optionsPanel.add(new JLabel("Spawn Count"));

        optionsPanel.add(spawnCountComboBox);
        IntStream.range(2, 17).forEach(spawnCountComboBox::addItem);

        spawnCountComboBox.addActionListener(e -> {
            Object selected = numTeamsComboBox.getSelectedItem();
            numTeamsComboBox.removeAllItems();
            IntStream.range(1, 9)
                     .filter(i -> ((int) spawnCountComboBox.getSelectedItem() % i) == 0)
                     .forEach(numTeamsComboBox::addItem);
            if (selected != null) {
                numTeamsComboBox.setSelectedItem(selected);
            }
        });

        optionsPanel.add(new JLabel("Num Teams"));
        optionsPanel.add(numTeamsComboBox);

        numTeamsComboBox.addActionListener(e -> {
            Object selected = terrainSymmetryComboBox.getSelectedItem();
            terrainSymmetryComboBox.removeAllItems();
            if (numTeamsComboBox.getSelectedItem() != null) {
                Arrays.stream(Symmetry.values())
                      .filter(symmetry -> (symmetry.getNumSymPoints() % (int) numTeamsComboBox.getSelectedItem()) == 0)
                      .forEach(terrainSymmetryComboBox::addItem);
                if (selected != null) {
                    terrainSymmetryComboBox.setSelectedItem(selected);
                }
            }
        });

        spawnCountComboBox.setSelectedIndex(0);

        optionsPanel.add(new JLabel("Terrain Symmetry"));
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

        JButton layoutButton = new JButton("Layout Graph");
        layoutButton.addActionListener(e -> ((PipelinePane) tabbedPane.getSelectedComponent()).layoutGraph());
        buttonPanel.add(layoutButton);

        JButton runButton = new JButton("Test Run");
        runButton.addActionListener(e -> runGraph());
        buttonPanel.add(runButton);

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> importPipeline());
        buttonPanel.add(loadButton);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> exportPipeline());
        buttonPanel.add(saveButton);

        JButton clearButton = new JButton("Clear Graph");
        clearButton.addActionListener(e -> ((PipelinePane) tabbedPane.getSelectedComponent()).clearGraph());
        buttonPanel.add(clearButton);

        JButton exportButton = new JButton("Export Selected Nodes");
        exportButton.addActionListener(e -> exportSelectedCells());
        buttonPanel.add(exportButton);

        JButton importButton = new JButton("Import SubGraph");
        importButton.addActionListener(e -> importSubGraph());
        buttonPanel.add(importButton);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.gridy = 3;
        constraints.weighty = 0;
        frame.add(buttonPanel, constraints);
    }

    private void runGraph() {
        String seedString = seedTextField.getText();
        Long seed = seedString == null || seedString.isBlank() ? null : Long.parseLong(seedString);
        ((PipelinePane) tabbedPane.getSelectedComponent()).runGraph(seed, (Integer) numTeamsComboBox.getSelectedItem(),
                                                                    (Integer) mapSizeComboBox.getSelectedItem(),
                                                                    (Integer) spawnCountComboBox.getSelectedItem(),
                                                                    (Symmetry) terrainSymmetryComboBox.getSelectedItem());
    }

    private void importPipeline() {
        fileChooser.setFileFilter(new FileNameExtensionFilter("Pipeline File", "pipe"));
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                GeneratorPipeline pipeline = GeneratorGraphSerializationUtil.importPipeline(file);
                addNewGraphTab(pipeline);
                ((CloseableTabComponent) tabbedPane.getTabComponentAt(tabbedPane.getSelectedIndex())).setTitle(
                        String.format("%s (%s)", file.getName(),
                                      pipeline.getClass().getSimpleName().replace("Pipeline", "")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void exportPipeline() {
        fileChooser.setFileFilter(new FileNameExtensionFilter("Pipeline File", "pipe"));
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            int index = tabbedPane.getSelectedIndex();
            PipelinePane pipelinePane = (PipelinePane) tabbedPane.getComponentAt(index);
            pipelinePane.exportPipeline(file);
            ((CloseableTabComponent) tabbedPane.getTabComponentAt(index)).setTitle(
                    String.format("%s (%s)", file.getName(),
                                  pipelinePane.getPipeline().getClass().getSimpleName().replace("Pipeline", "")));
        }
    }

    private void exportSelectedCells() {
        fileChooser.setFileFilter(new FileNameExtensionFilter("Graph File", "graph"));
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            ((PipelinePane) tabbedPane.getSelectedComponent()).exportSelectedCells(file);
        }
    }

    private void importSubGraph() {
        fileChooser.setFileFilter(new FileNameExtensionFilter("Graph File", "graph"));
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            ((PipelinePane) tabbedPane.getSelectedComponent()).importSubGraph(file);
        }
    }
}
