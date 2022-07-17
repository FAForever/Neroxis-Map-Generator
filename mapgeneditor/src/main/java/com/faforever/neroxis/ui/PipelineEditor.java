package com.faforever.neroxis.ui;

import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.generator.graph.GeneratorPipeline;
import com.faforever.neroxis.generator.serial.GeneratorGraphSerializationUtil;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ui.components.*;
import com.faforever.neroxis.visualization.EntryPanel;
import picocli.CommandLine;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@CommandLine.Command(name = "Editor", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class, description = "Tool for creating generator pipelines")
public strictfp class PipelineEditor implements Callable<Integer> {
    public static final String NEW_TAB_TITLE = "+";
    private final JFrame frame = new JFrame();
    private final MaskGraphVertexEditPanel vertexEditPanel = new MaskGraphVertexEditPanel();
    private final PipelineSettingsPanel pipelineSettingsPanel = new PipelineSettingsPanel();
    private final EntryPanel entryPanel = new EntryPanel();
    private final PipelinePane neighborPane = new PipelinePane();
    private final JFileChooser fileChooser = new JFileChooser();
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final MethodListPanel methodListPanel = new MethodListPanel();
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
        frame.setPreferredSize(new Dimension(1600, 1000));
        setupGraphTabPane();
        setupVertexEditPanel();
        setupNeighborPanel();
        setupEntryPanel();
        setupMapOptions();
        setupButtons();
        setupMethodSelection();
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        return 0;
    }

    private void setupGraphTabPane() {
        addNewPaneTabButtonToEnd();
        tabbedPane.addChangeListener(e -> addOrSelectNewTab());
        GridBagConstraints constraints = defaultConstraints();
        constraints.gridx = 1;
        constraints.weightx = 10;
        constraints.weighty = 1;
        constraints.gridheight = 6;
        frame.add(tabbedPane, constraints);
    }

    private void addOrSelectNewTab() {
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
            PipelinePane pipelinePane = getSelectedPipelinePane();
            vertexEditPanel.setPipelinePane(pipelinePane);
        }
    }

    private void addNewGraphTab(GeneratorPipeline pipeline) {
        PipelinePane pipelinePane = new PipelinePane(pipeline);
        pipelinePane.getGraphComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                boolean controlDown = e.isControlDown();
                PipelinePane pipelinePane = getSelectedPipelinePane();
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
        pipelinePane.setMaskVertexSelectionAction(this::onVertexSelected);
        CloseableTabComponent closeableTabComponent = new CloseableTabComponent(tabbedPane);
        tabbedPane.addTab("", pipelinePane);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, closeableTabComponent);
        closeableTabComponent.setTitle(
                String.format("New (%s)", pipeline.getClass().getSimpleName().replace("Pipeline", "")));
        addNewPaneTabButtonToEnd();
        vertexEditPanel.setPipelinePane(pipelinePane);
    }

    private void onVertexSelected(PipelinePane pipelinePane, MaskGraphVertex<?> vertex) {
        if (pipelinePane == neighborPane) {
            PipelineGraph selectedGraph = getSelectedPipelinePane().getGraph();

            if (!selectedGraph.isVertexSelected(vertex)) {
                selectedGraph.selectVertexIfExists(vertex);
            }

            return;
        }

        vertexEditPanel.setVertex(vertex);
        if (vertex != null && vertex.isComputed()) {
            entryPanel.setMask(vertex.getImmutableResult(MaskGraphVertex.SELF));
        }
        methodListPanel.setVertex(vertex);
        PipelineGraph selectedGraph = pipelinePane.getGraph();
        PipelineGraph neighborGraph = neighborPane.getGraph();
        neighborGraph.clear();
        HashSet<MaskMethodEdge> edges = new HashSet<>();
        edges.addAll(selectedGraph.outgoingEdgesOf(vertex));
        edges.addAll(selectedGraph.incomingEdgesOf(vertex));

        selectedGraph.getCellForVertex(vertex);

        edges.stream()
                .flatMap(edge -> Stream.of(edge.getSource(), edge.getTarget()))
                .peek(neighborGraph::addVisualVertexOnly)
                .forEach(vert -> {
                    ICell selectedCell = selectedGraph.getCellForVertex(vert);
                    ICell neighborCell = neighborGraph.getCellForVertex(vert);

                    String selectedCellStyle = selectedCell.getStyle();
                    neighborCell.setStyle(selectedCellStyle);
                    neighborCell.getChildren().forEach(child -> child.setStyle(selectedCellStyle));
                });
        edges.forEach(neighborGraph::addVisualEdgeOnly);

        if (!neighborGraph.isVertexSelected(vertex)) {
            neighborGraph.selectVertexIfExists(vertex);
        }

        neighborPane.layoutGraph();
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
        GridBagConstraints constraints = defaultConstraints();
        constraints.weighty = 1;
        frame.add(vertexEditPanel, constraints);
    }

    private void setupNeighborPanel() {
        neighborPane.setMaskVertexSelectionAction(this::onVertexSelected);
        GridBagConstraints constraints = defaultConstraints();
        constraints.gridy = 1;
        constraints.weighty = 4;
        frame.add(neighborPane, constraints);
    }

    private void setupEntryPanel() {
        GridBagConstraints constraints = defaultConstraints();
        constraints.gridy = 2;
        constraints.weighty = 4;
        frame.add(entryPanel, constraints);
    }

    private void setupMapOptions() {
        GridBagConstraints constraints = defaultConstraints();
        constraints.gridy = 3;

        frame.add(pipelineSettingsPanel, constraints);
    }

    private void setupButtons() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 2));

        JButton layoutButton = new JButton("Layout Graph");
        layoutButton.addActionListener(e -> getSelectedPipelinePane().layoutGraph());
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
        clearButton.addActionListener(e -> getSelectedPipelinePane().clearGraph());
        buttonPanel.add(clearButton);

        JButton exportButton = new JButton("Export Selected Nodes");
        exportButton.addActionListener(e -> exportSelectedCells());
        buttonPanel.add(exportButton);

        JButton importButton = new JButton("Import SubGraph");
        importButton.addActionListener(e -> importSubGraph());
        buttonPanel.add(importButton);

        GridBagConstraints constraints = defaultConstraints();
        constraints.gridy = 4;
        frame.add(buttonPanel, constraints);
    }

    private void setupMethodSelection() {
        methodListPanel.setMinimumSize(new Dimension(250, 0));
        methodListPanel.setPreferredSize(new Dimension(250, 0));
        GridBagConstraints constraints = defaultConstraints();
        constraints.gridx = 2;
        constraints.gridheight = 5;
        constraints.weighty = 1;
        frame.add(methodListPanel, constraints);
    }

    private GridBagConstraints defaultConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.gridy = 0;
        constraints.weighty = 0;
        return constraints;
    }

    private void runGraph() {
        getSelectedPipelinePane().runGraph(pipelineSettingsPanel.getSeed(), pipelineSettingsPanel.getNumTeams(),
                pipelineSettingsPanel.getMapSize(), pipelineSettingsPanel.getSpawnCount(),
                pipelineSettingsPanel.getSymmetry());
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
            PipelinePane pipelinePane = getSelectedPipelinePane();
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
            getSelectedPipelinePane().exportSelectedCells(file);
        }
    }

    private void importSubGraph() {
        fileChooser.setFileFilter(new FileNameExtensionFilter("Graph File", "graph"));
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            getSelectedPipelinePane().importSubGraph(file);
        }
    }

    private PipelinePane getSelectedPipelinePane() {
        return (PipelinePane) tabbedPane.getSelectedComponent();
    }
}
