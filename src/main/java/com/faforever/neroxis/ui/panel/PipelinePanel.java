package com.faforever.neroxis.ui.panel;

import com.faforever.neroxis.graph.domain.GraphContext;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.ui.control.MaskGraphEditingModalGraphMouse;
import com.faforever.neroxis.util.GraphSerializationUtil;
import com.faforever.neroxis.util.Pipeline;
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public strictfp class PipelinePanel extends JPanel {
    private final DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> rawGraph = new DirectedAcyclicGraph<>(MaskMethodEdge.class);
    private final DefaultListenableGraph<MaskGraphVertex<?>, MaskMethodEdge> graph = new DefaultListenableGraph<>(rawGraph);
    private final VisualizationViewer<MaskGraphVertex<?>, MaskMethodEdge> graphViewer = VisualizationViewer.builder(graph)
            .layoutAlgorithm(SugiyamaLayoutAlgorithm.<MaskGraphVertex<?>, MaskMethodEdge>builder()
                    .layering(Layering.NETWORK_SIMPLEX)
                    .build())
            .layoutSize(new Dimension(100, 100))
            .build();
    private final MaskGraphEditingModalGraphMouse graphMouse = MaskGraphEditingModalGraphMouse.builder()
            .renderContextSupplier(graphViewer::getRenderContext)
            .multiLayerTransformerSupplier(graphViewer.getRenderContext()::getMultiLayerTransformer)
            .build();
    private final VisualizationScrollPane pipelineGraphPane = new VisualizationScrollPane(graphViewer);
    private final MaskGraphVertexEditPanel vertexEditPanel = new MaskGraphVertexEditPanel();
    private final EntryPanel entryPanel = new EntryPanel(new Dimension(450, 450));
    private final JFileChooser fileChooser = new JFileChooser();
    @Getter
    @Setter
    private Consumer<Pipeline.Entry> entryVertexSelectionAction;

    public PipelinePanel() {
        graph.addGraphListener(new GraphListener<>() {
            @Override
            public void edgeAdded(GraphEdgeChangeEvent<MaskGraphVertex<?>, MaskMethodEdge> e) {
                e.getEdgeTarget().setParameter(e.getEdge().getParameterName(), e.getEdgeSource());
                vertexEditPanel.updatePanel();
            }

            @Override
            public void edgeRemoved(GraphEdgeChangeEvent<MaskGraphVertex<?>, MaskMethodEdge> e) {
                e.getEdgeTarget().clearParameter(e.getEdge().getParameterName());
                vertexEditPanel.updatePanel();
            }

            @Override
            public void vertexAdded(GraphVertexChangeEvent<MaskGraphVertex<?>> e) {
            }

            @Override
            public void vertexRemoved(GraphVertexChangeEvent<MaskGraphVertex<?>> e) {
            }
        });
        setLayout(new GridBagLayout());
        setupGraph();
        setupEntryPanel();
        setupVertexEditPanel();
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

            if (vertex.getExecutorClass() == null || vertex.getExecutable() == null) {
                return Color.RED;
            } else if (!vertex.isFullyDefined()) {
                return Color.CYAN;
            } else {
                return Color.GREEN;
            }
        });

        Function<MaskMethodEdge, Paint> edgePaintFunction = edge -> {
            if ("executor".equals(edge.getParameterName())) {
                return Color.RED;
            }

            return Color.BLACK;
        };
        renderContext.setEdgeDrawPaintFunction(edgePaintFunction);
        renderContext.setArrowFillPaintFunction(edgePaintFunction);
        renderContext.setArrowDrawPaintFunction(edgePaintFunction);
        renderContext.setEdgeLabelFunction(MaskMethodEdge::getParameterName);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.gridheight = 2;

        add(pipelineGraphPane, constraints);
    }

    private void setupEntryPanel() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 4;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.gridheight = 2;

        add(entryPanel, constraints);
    }

    private void setupVertexEditPanel() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 0;
        constraints.gridy = 0;
        constraints.weighty = 1;

        vertexEditPanel.setVisualizationViewer(graphViewer);
        add(vertexEditPanel, constraints);
    }

    private void setupButtons() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 2));

        JButton layoutButton = new JButton();
        layoutButton.setText("Layout Graph");
        layoutButton.addActionListener(e -> {
            LayoutAlgorithm<MaskGraphVertex<?>> layoutAlgorithm = graphViewer.getVisualizationModel().getLayoutAlgorithm();
            graphViewer.getVisualizationModel().getLayoutModel().setSize(100, 100);
            layoutAlgorithm.visit(graphViewer.getVisualizationModel().getLayoutModel());
        });

        buttonPanel.add(layoutButton);

        JButton runButton = new JButton();
        runButton.setText("Test Run");
        runButton.addActionListener(e -> {
            Pipeline.reset();
            rawGraph.forEach(maskGraphVertex -> {
                maskGraphVertex.resetResult();
                if (graph.outgoingEdgesOf(maskGraphVertex).isEmpty() && graph.incomingEdgesOf(maskGraphVertex).isEmpty()) {
                    graph.removeVertex(maskGraphVertex);
                }
            });
            rawGraph.forEach(vertex -> {
                try {
                    vertex.prepareResult(new GraphContext(0L, new SymmetrySettings(Symmetry.POINT2)));
                } catch (InvocationTargetException | IllegalAccessException | InstantiationException ex) {
                    throw new RuntimeException(ex);
                }
            });
            Pipeline.start();
            Pipeline.join();
            graphViewer.getSelectedVertices().stream().findFirst().map(MaskGraphVertex::getEntry).ifPresent(entryPanel::setEntry);
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
                    refreshGraph();
                    layoutButton.doClick();
                    graphViewer.repaint();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        buttonPanel.add(importButton);

        JButton exportButton = new JButton();
        exportButton.setText("Export");
        exportButton.addActionListener(e -> {
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
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.gridy = 1;
        constraints.weighty = 0;

        add(buttonPanel, constraints);
    }

    private void refreshGraph() {
        graphViewer.getVisualizationModel().setGraph(graph, true);
    }

    private void vertexSelected(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof MaskGraphVertex) {
            MaskGraphVertex<?> vertex = (MaskGraphVertex<?>) e.getItem();
            vertexEditPanel.setVertex(vertex);
            if (vertex.getEntry() != null) {
                entryPanel.setEntry(vertex.getEntry());
            } else if (vertex.getResult() != null) {
                entryPanel.setMask(vertex.getResult());
            }
        }
    }
}
