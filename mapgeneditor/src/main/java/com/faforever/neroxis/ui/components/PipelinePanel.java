package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.debugger.EntryPanel;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.graph.domain.GraphContext;
import com.faforever.neroxis.generator.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.generator.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.generator.graph.domain.MaskMethodVertex;
import com.faforever.neroxis.generator.graph.domain.MaskVertexResult;
import com.faforever.neroxis.generator.graph.io.GraphSerializationUtil;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.ngraph.layout.hierarchical.HierarchicalLayout;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.view.Graph;
import com.faforever.neroxis.ui.control.MaskGraphEditingModalGraphMouse;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.SymmetrySelector;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.Setter;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultListenableGraph;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jungrapht.visualization.RenderContext;
import org.jungrapht.visualization.VisualizationScrollPane;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.EditingModalGraphMouse;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.SugiyamaLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.sugiyama.Layering;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.renderers.Renderer;

public strictfp class PipelinePanel extends JPanel {
    private final DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> rawGraph = new DirectedAcyclicGraph<>(MaskMethodEdge.class);
    private final DefaultListenableGraph<MaskGraphVertex<?>, MaskMethodEdge> graph = new DefaultListenableGraph<>(rawGraph);
    private final Graph mxGraph = new Graph();
    private final GraphComponent graphComponent = new GraphComponent(mxGraph);
    private final VisualizationViewer<MaskGraphVertex<?>, MaskMethodEdge> graphViewer = VisualizationViewer.builder(graph).layoutAlgorithm(SugiyamaLayoutAlgorithm.<MaskGraphVertex<?>, MaskMethodEdge>builder().layering(Layering.NETWORK_SIMPLEX).build()).layoutSize(new Dimension(100, 100)).viewSize(new Dimension(750, 750)).build();
    private final MaskGraphEditingModalGraphMouse graphMouse = MaskGraphEditingModalGraphMouse.builder().renderContextSupplier(graphViewer::getRenderContext).multiLayerTransformerSupplier(graphViewer.getRenderContext()::getMultiLayerTransformer).build();
    private final VisualizationScrollPane pipelineGraphPane = new VisualizationScrollPane(graphViewer);
    private final Map<ICell, MaskGraphVertex<?>> cellToVertex = new HashMap<>();
    private final MaskGraphVertexEditPanel vertexEditPanel = new MaskGraphVertexEditPanel();
    private final EntryPanel entryPanel = new EntryPanel(new Dimension(550, 550));
    private final JFileChooser fileChooser = new JFileChooser();
    @Getter
    @Setter
    private Consumer<Pipeline.Entry> entryVertexSelectionAction;
    private final JComboBox<Integer> mapSizeComboBox = new JComboBox<>();
    private final JComboBox<Integer> spawnCountComboBox = new JComboBox<>();
    private final JComboBox<Integer> numTeamsComboBox = new JComboBox<>();
    private final JComboBox<Symmetry> terrainSymmetryComboBox = new JComboBox<>();
    private final Map<MaskGraphVertex<?>, ICell> vertexToCell = new HashMap<>();
    private final Map<ICell, MaskMethodEdge> cellToEdge = new HashMap<>();
    private final Map<MaskMethodEdge, ICell> edgeToCell = new HashMap<>();
    HierarchicalLayout layout = new HierarchicalLayout(mxGraph);

    public PipelinePanel() {
        graph.addGraphListener(new GraphListener<>() {
            @Override
            public void edgeAdded(GraphEdgeChangeEvent<MaskGraphVertex<?>, MaskMethodEdge> e) {
                MaskMethodEdge edge = e.getEdge();
                MaskGraphVertex<?> edgeTarget = e.getEdgeTarget();
                ICell cell = mxGraph.insertEdge(mxGraph.getDefaultParent(), null, null, vertexToCell.get(e.getEdgeSource()), vertexToCell.get(e.getEdgeTarget()));
                edgeToCell.put(edge, cell);
                cellToEdge.put(cell, edge);
                String parameterName = edge.getParameterName();
                edgeTarget.setParameter(parameterName, new MaskVertexResult(edge.getResultName(), e.getEdgeSource()));
                if (MaskGraphVertex.SELF.equals(edge.getResultName()) && MaskMethodVertex.EXECUTOR.equals(parameterName)) {
                    MaskGraphVertex<?> nextVertex = edgeTarget;
                    while (nextVertex != null) {
                        nextVertex.setIdentifier(e.getEdgeSource().getIdentifier());
                        nextVertex = graph.outgoingEdgesOf(nextVertex).stream().filter(methodEdge -> MaskGraphVertex.SELF.equals(methodEdge.getResultName()) && MaskMethodVertex.EXECUTOR.equals(methodEdge.getParameterName())).map(graph::getEdgeTarget).findFirst().orElse(null);
                    }
                    graphViewer.repaint();
                }
                vertexEditPanel.updatePanel();
            }

            @Override
            public void edgeRemoved(GraphEdgeChangeEvent<MaskGraphVertex<?>, MaskMethodEdge> e) {
                MaskGraphVertex<?> edgeTarget = e.getEdgeTarget();
                edgeTarget.clearParameter(e.getEdge().getParameterName());
                vertexEditPanel.updatePanel();
                graphViewer.repaint();
                ICell cell = edgeToCell.remove(e.getEdge());
                cellToEdge.remove(cell);
                mxGraph.removeCells(List.of(cell));
            }

            @Override
            public void vertexAdded(GraphVertexChangeEvent<MaskGraphVertex<?>> e) {
                ICell cell = mxGraph.insertVertex(mxGraph.getDefaultParent(), null, e.getVertex().getIdentifier(), 0, 0, 100, 100);
                MaskGraphVertex<?> vertex = e.getVertex();
                vertexToCell.put(vertex, cell);
                cellToVertex.put(cell, e.getVertex());
                graphViewer.getSelectedVertexState().clear();
                graphViewer.getSelectedVertexState().select(vertex);
            }

            @Override
            public void vertexRemoved(GraphVertexChangeEvent<MaskGraphVertex<?>> e) {
                MaskGraphVertex<?> vertex = e.getVertex();
                graphViewer.getSelectedVertexState().deselect(vertex);
                ICell cell = vertexToCell.remove(e.getVertex());
                cellToVertex.remove(cell);
                mxGraph.removeCells(List.of(cell));
            }
        });
        setLayout(new GridBagLayout());
        setupGraph();
        setupEntryPanel();
        setupVertexEditPanel();
        setupMapOptions();
        setupButtons();
    }

    private void setupGraph() {
        graphMouse.setModeKeyListener(new EditingModalGraphMouse.ModeKeyAdapter(graphMouse));
        graphViewer.setGraphMouse(graphMouse);
        graphViewer.addKeyListener(graphMouse.getModeKeyListener());
        graphViewer.getSelectedVertexState().addItemListener(this::vertexSelected);

        RenderContext<MaskGraphVertex<?>, MaskMethodEdge> renderContext = graphViewer.getRenderContext();
        renderContext.setVertexFillPaintFunction(vertex -> {
            if (graphViewer.getSelectedVertices().contains(vertex)) {
                return Color.YELLOW;
            }


            if (vertex.isNotDefined()) {
                return Color.RED;
            } else {
                return Color.GREEN;
            }
        });

        Function<MaskMethodEdge, Paint> edgePaintFunction = edge -> {
            if (MaskMethodVertex.EXECUTOR.equals(edge.getParameterName())) {
                if (MaskGraphVertex.SELF.equals(edge.getResultName())) {
                    return Color.RED;
                } else {
                    return Color.BLUE;
                }
            }

            return Color.BLACK;
        };
        renderContext.setEdgeDrawPaintFunction(edgePaintFunction);
        renderContext.setArrowFillPaintFunction(edgePaintFunction);
        renderContext.setArrowDrawPaintFunction(edgePaintFunction);
        renderContext.setVertexLabelFunction(vertex -> String.format("<html>%s<br/>%s</html>", vertex.getIdentifier(), vertex.getExecutableName()));
        renderContext.setVertexLabelPosition(Renderer.VertexLabel.Position.E);
        graphViewer.setEdgeToolTipFunction(edge -> String.format("%s -> %s", edge.getResultName(), edge.getParameterName()));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.weightx = 2;
        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.gridheight = 5;

        //        add(pipelineGraphPane, constraints);
        add(graphComponent, constraints);
        graphComponent.addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) {
                graphComponent.zoomIn();
            } else {
                graphComponent.zoomOut();
            }
        });

        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ICell cell = graphComponent.getCellAt(e.getX(), e.getY());
                if (cell != null && cell.isVertex()) {
                    MaskGraphVertex<?> vertex = cellToVertex.get(cell);
                    vertexEditPanel.setVertex(vertex);
                    if (vertex.getExecutable() != null && vertex.isComputed()) {
                        entryPanel.setMask(vertex.getImmutableResult(MaskGraphVertex.SELF));
                    }
                }
            }
        });
    }

    private void setupEntryPanel() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.weightx = 1;
        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.gridheight = 5;

        add(entryPanel, constraints);
    }

    private void setupVertexEditPanel() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = .5;
        constraints.gridy = 0;
        constraints.weighty = 1;

        vertexEditPanel.setVisualizationViewer(graphViewer);
        add(vertexEditPanel, constraints);
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
                    IntStream.range(1, 9).filter(i -> ((int) spawnCountComboBox.getSelectedItem() % i) == 0)
                            .forEach(numTeamsComboBox::addItem);
                    if (selected != null) {
                        numTeamsComboBox.setSelectedItem(selected);
                    }
                }
        );

        JLabel numTeamsLabel = new JLabel();
        numTeamsLabel.setText("Num Teams");

        optionsPanel.add(numTeamsLabel);
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
                }

        );

        spawnCountComboBox.setSelectedIndex(0);

        JLabel terrainSymmetryLabel = new JLabel();
        terrainSymmetryLabel.setText("Terrain Symmetry");

        optionsPanel.add(terrainSymmetryLabel);

        optionsPanel.add(terrainSymmetryComboBox);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = .25;
        constraints.gridy = 3;
        constraints.weighty = 0;

        add(optionsPanel, constraints);
    }

    private void setupButtons() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 2));

        JButton layoutButton = new JButton();
        layoutButton.setText("Layout Graph");
        layoutButton.addActionListener(e -> layoutGraph());

        buttonPanel.add(layoutButton);

        JButton runButton = new JButton();
        runButton.setText("Test Run");
        runButton.addActionListener(e -> {
            Pipeline.reset();
            removeUnusedVertices();
            Random random = new Random();
            int numTeams = (int) numTeamsComboBox.getSelectedItem();
            SymmetrySettings symmetrySettings = SymmetrySelector.getSymmetrySettingsFromTerrainSymmetry(random, (Symmetry) terrainSymmetryComboBox.getSelectedItem(), numTeams);
            GeneratorParameters generatorParameters = GeneratorParameters.builder()
                    .mapSize((Integer) mapSizeComboBox.getSelectedItem())
                    .numTeams(numTeams)
                    .spawnCount((Integer) spawnCountComboBox.getSelectedItem())
                    .terrainSymmetry((Symmetry) terrainSymmetryComboBox.getSelectedItem())
                    .build();

            GraphContext graphContext = new GraphContext(random.nextLong(), generatorParameters, symmetrySettings);
            DebugUtil.timedRun("Setup pipeline", () -> rawGraph.forEach(vertex -> {
                try {
                    vertex.prepareResults(graphContext);
                } catch (InvocationTargetException | IllegalAccessException | InstantiationException ex) {
                    throw new RuntimeException(ex);
                }
            }));
            Pipeline.start();
            Pipeline.join();
            graphViewer.getSelectedVertices().stream().findFirst().map(vertex -> vertex.getImmutableResult(MaskGraphVertex.SELF)).ifPresent(entryPanel::setMask);
        });

        buttonPanel.add(runButton);

        JButton importButton = new JButton();
        importButton.setText("Import");
        importButton.addActionListener(e -> {
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    Set<MaskGraphVertex<?>> vertices = new HashSet<>(graph.vertexSet());
                    graph.removeAllVertices(vertices);
                    GraphSerializationUtil.importGraph(graph, file);
                    vertexEditPanel.setVertex(null);
                    layoutGraph();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        buttonPanel.add(importButton);

        JButton exportButton = new JButton();
        exportButton.setText("Export");
        exportButton.addActionListener(e -> {
            removeUnusedVertices();
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    GraphSerializationUtil.exportGraph(graph, file);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        buttonPanel.add(exportButton);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.gridy = 4;
        constraints.weighty = 0;

        add(buttonPanel, constraints);
    }

    private void layoutGraph() {
        LayoutAlgorithm<MaskGraphVertex<?>> layoutAlgorithm = graphViewer.getVisualizationModel().getLayoutAlgorithm();
        LayoutModel<MaskGraphVertex<?>> layoutModel = graphViewer.getVisualizationModel().getLayoutModel();
        layoutModel.setGraph(graph);
        layoutModel.setSize(100, 100);
        layoutAlgorithm.visit(layoutModel);
        graphViewer.repaint();

        layout.execute(mxGraph.getDefaultParent());
    }

    private void removeUnusedVertices() {
        List<MaskGraphVertex<?>> verticesToRemove = new ArrayList<>();
        rawGraph.forEach(maskGraphVertex -> {
            maskGraphVertex.resetResult();
            if (graph.outgoingEdgesOf(maskGraphVertex).isEmpty() && graph.incomingEdgesOf(maskGraphVertex).isEmpty()) {
                verticesToRemove.add(maskGraphVertex);
            }
        });
        graphViewer.getVisualizationModel().getGraph().removeAllVertices(verticesToRemove);
    }

    private void vertexSelected(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof MaskGraphVertex) {
            MaskGraphVertex<?> vertex = (MaskGraphVertex<?>) e.getItem();
            vertexEditPanel.setVertex(vertex);
            if (vertex.getExecutable() != null && vertex.isComputed()) {
                entryPanel.setMask(vertex.getImmutableResult(MaskGraphVertex.SELF));
            }
        } else if (e.getStateChange() == ItemEvent.DESELECTED && e.getItem() instanceof MaskGraphVertex) {
            vertexEditPanel.setVertex(null);
        }
    }
}
