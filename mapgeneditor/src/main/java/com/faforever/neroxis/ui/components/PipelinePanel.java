package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.debugger.EntryPanel;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.graph.domain.GraphContext;
import com.faforever.neroxis.generator.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.generator.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.generator.graph.io.GraphSerializationUtil;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.ngraph.layout.hierarchical.HierarchicalLayout;
import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.SymmetrySelector;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import lombok.Getter;
import lombok.Setter;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DirectedAcyclicGraph;

public strictfp class PipelinePanel extends JPanel implements GraphListener<MaskGraphVertex<?>, MaskMethodEdge> {
    private final PipelineGraph graph = new PipelineGraph();
    private final PipelineGraphComponent graphComponent = new PipelineGraphComponent(graph);
    private final MaskGraphVertexEditPanel vertexEditPanel = new MaskGraphVertexEditPanel(this);
    private final EntryPanel entryPanel = new EntryPanel();
    private final JFileChooser fileChooser = new JFileChooser();
    private final JComboBox<Integer> mapSizeComboBox = new JComboBox<>();
    private final JComboBox<Integer> spawnCountComboBox = new JComboBox<>();
    private final JComboBox<Integer> numTeamsComboBox = new JComboBox<>();
    private final JComboBox<Symmetry> terrainSymmetryComboBox = new JComboBox<>();
    HierarchicalLayout layout = new HierarchicalLayout(graph);
    @Getter
    @Setter
    private Consumer<Pipeline.Entry> entryVertexSelectionAction;

    public PipelinePanel() {
        setLayout(new GridBagLayout());
        setupGraph();
        setupVertexEditPanel();
        setupEntryPanel();
        setupMapOptions();
        setupButtons();
    }

    private void setupGraph() {
        layout.setTraverseAncestors(false);
        graph.addGraphListener(this);
        graphComponent.setTolerance(1);
        graphComponent.setViewportBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        graphComponent.setPreferredSize(new Dimension(800, 800));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.weightx = 4;
        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.gridheight = 5;
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
                if (cell == null) {
                    return;
                }
                while (!graph.getDefaultParent().equals(cell.getParent())) {
                    cell = cell.getParent();
                    if (cell == null) {
                        return;
                    }
                }
                if (cell.isVertex()) {
                    vertexSelected(graph.getVertexForCell(cell));
                }
            }
        });
        layout.setOrientation(SwingConstants.WEST);
        layout.setInterRankCellSpacing(100);
        layout.setInterHierarchySpacing(50);
        layout.setIntraCellSpacing(50);
    }

    private void setupVertexEditPanel() {
        vertexEditPanel.setPreferredSize(new Dimension(400, 300));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridy = 0;
        constraints.weighty = 1;
        vertexEditPanel.setGraphComponent(graphComponent);
        add(vertexEditPanel, constraints);
    }

    private void setupEntryPanel() {
        entryPanel.setPreferredSize(new Dimension(400, 400));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridy = 1;
        constraints.weighty = 1;
        add(entryPanel, constraints);
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
        buttonPanel.add(runButton);
        JButton importButton = new JButton();
        importButton.setText("Clear Graph");
        importButton.addActionListener(e -> clearGraph());
        buttonPanel.add(importButton);
        JButton exportButton = new JButton();
        exportButton.setText("Export Selected Nodes");
        exportButton.addActionListener(e -> exportSubGraph());
        buttonPanel.add(exportButton);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.gridy = 3;
        constraints.weighty = 0;
        add(buttonPanel, constraints);
    }

    private void exportGraph() {
        removeUnusedVertices();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                GraphSerializationUtil.exportGraph(graph, file);
            } catch (IOException ex) {
                throw new IllegalArgumentException("Could not write graph");
            }
        }
    }

    private void clearGraph() {
        Set<MaskGraphVertex<?>> vertices = new HashSet<>(graph.vertexSet());
        graph.removeAllVertices(vertices);
    }

    private void importGraph() {
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                GraphSerializationUtil.importGraph(graph, file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            layoutGraph();
        }
    }

    private void exportSubGraph() {
        DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> subGraph = new DirectedAcyclicGraph<>(MaskMethodEdge.class);
        Set<MaskGraphVertex<?>> selectedVertices = graph.getSelectionCells().stream().map(graph::getVertexForCell).filter(Objects::nonNull).collect(Collectors.toSet());
        selectedVertices.forEach(subGraph::addVertex);
        selectedVertices.stream().flatMap(vertex -> graph.outgoingEdgesOf(vertex).stream()).filter(edge -> selectedVertices.contains(graph.getEdgeTarget(edge))).forEach(edge -> subGraph.addEdge(graph.getEdgeSource(edge), graph.getEdgeTarget(edge), edge));
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                GraphSerializationUtil.exportGraph(subGraph, file);
            } catch (IOException ex) {
                throw new IllegalArgumentException("Could not write graph");
            }
        }
    }

    private void runGraph() {
        Pipeline.reset();
        removeUnusedVertices();
        Random random = new Random();
        int numTeams = (int) numTeamsComboBox.getSelectedItem();
        SymmetrySettings symmetrySettings = SymmetrySelector.getSymmetrySettingsFromTerrainSymmetry(random, (Symmetry) terrainSymmetryComboBox.getSelectedItem(), numTeams);
        GeneratorParameters generatorParameters = GeneratorParameters.builder().mapSize((Integer) mapSizeComboBox.getSelectedItem()).numTeams(numTeams).spawnCount((Integer) spawnCountComboBox.getSelectedItem()).terrainSymmetry((Symmetry) terrainSymmetryComboBox.getSelectedItem()).build();
        GraphContext graphContext = new GraphContext(random.nextLong(), generatorParameters, ParameterConstraints.builder().build(), symmetrySettings);
        DebugUtil.timedRun("Setup pipeline", () -> graph.forEach(vertex -> {
            try {
                vertex.prepareResults(graphContext);
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException ex) {
                throw new RuntimeException(ex);
            }
        }));
        Pipeline.start();
        Pipeline.join();
        vertexEditPanel.updatePanel();
        MaskGraphVertex<?> vertex = graph.getVertexForCell(graph.getSelectionCell());
        if (vertex != null && vertex.getExecutable() != null && vertex.isComputed()) {
            entryPanel.setMask(vertex.getImmutableResult(MaskGraphVertex.SELF));
        }
    }

    private void layoutGraph() {
        removeUnusedVertices();
        layout.execute(graph.getDefaultParent());
        graph.getDefaultParent().getChildren().forEach(cell -> cell.getChildren().forEach(child -> graph.getOutgoingEdges(child).forEach(edge -> {
            ICell target = edge.getTarget();
            ICell source = edge.getSource();
            ICell parentEdge = graph.getEdgesBetween(cell, target.getParent()).stream().findFirst().orElseThrow(() -> new IllegalStateException("No parent"));
            RectangleDouble sourceParentBoundingBox = graph.getBoundingBox(source.getParent(), false, true);
            RectangleDouble targetParentBoundingBox = graph.getBoundingBox(target.getParent(), false, true);
            targetParentBoundingBox.grow(20);
            List<PointDouble> points = parentEdge.getGeometry().getPoints().stream().filter(point -> {
                PointDouble transformedPoint = graph.getView().transformControlPoint(graph.getView().getState(edge), point);
                return !sourceParentBoundingBox.contains(transformedPoint.getX(), transformedPoint.getY()) && !targetParentBoundingBox.contains(transformedPoint.getX(), transformedPoint.getY());
            }).collect(Collectors.toList());
            Geometry targetGeometry = target.getGeometry();
            Geometry targetParentGeometry = target.getParent().getGeometry();
            RectangleDouble targetRectangle = new RectangleDouble(targetParentGeometry.getX() + targetGeometry.getX() * targetParentGeometry.getWidth(), targetParentGeometry.getY() + targetGeometry.getY() * targetParentGeometry.getHeight(), targetGeometry.getWidth(), targetGeometry.getHeight());
            PointDouble endPoint = new PointDouble(targetRectangle.getCenterX() - targetRectangle.getWidth() / 2 - 10, targetRectangle.getCenterY());
            points.add(endPoint);
            edge.getGeometry().setPoints(points);
            edge.setTarget(target);
            edge.setSource(source);
        })));
        graph.refresh();
    }

    private void removeUnusedVertices() {
        List<MaskGraphVertex<?>> verticesToRemove = new ArrayList<>();
        graph.forEach(maskGraphVertex -> {
            maskGraphVertex.resetResult();
            if (graph.outgoingEdgesOf(maskGraphVertex).isEmpty() && graph.incomingEdgesOf(maskGraphVertex).isEmpty()) {
                verticesToRemove.add(maskGraphVertex);
            }
        });
        graph.removeAllVertices(verticesToRemove);
    }

    private void vertexSelected(MaskGraphVertex<?> vertex) {
        vertexEditPanel.setVertex(vertex);
        if (vertex != null && vertex.getExecutable() != null && vertex.isComputed()) {
            entryPanel.setMask(vertex.getImmutableResult(MaskGraphVertex.SELF));
        }
    }

    public void updateIdentifiers(MaskGraphVertex<?> vertex) {
        String identifier = vertex.getIdentifier();
        graph.getDirectRelationships(vertex).forEach(node -> {
            node.setIdentifier(identifier);
            graph.getCellForVertex(node).setValue(identifier);
        });
        graph.refresh();
    }

    @Override
    public void edgeAdded(GraphEdgeChangeEvent<MaskGraphVertex<?>, MaskMethodEdge> e) {
        vertexEditPanel.updatePanel();
        updateIdentifiers(e.getEdgeSource());
    }

    @Override
    public void edgeRemoved(GraphEdgeChangeEvent<MaskGraphVertex<?>, MaskMethodEdge> e) {
        vertexEditPanel.updatePanel();
        updateIdentifiers(e.getEdgeTarget());
    }

    @Override
    public void vertexAdded(GraphVertexChangeEvent<MaskGraphVertex<?>> e) {
        vertexEditPanel.updatePanel();
    }

    @Override
    public void vertexRemoved(GraphVertexChangeEvent<MaskGraphVertex<?>> e) {
        vertexEditPanel.updatePanel();
    }
}
