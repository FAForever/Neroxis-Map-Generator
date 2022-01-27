package com.faforever.neroxis.ui.panel;

import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.graph.domain.MaskMethodVertex;
import com.faforever.neroxis.ui.control.VertexAwareEditingModalGraphMouse;
import com.faforever.neroxis.util.Pipeline;
import lombok.Getter;
import lombok.Setter;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jungrapht.visualization.VisualizationScrollPane;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.EditingModalGraphMouse;
import org.jungrapht.visualization.layout.algorithms.SugiyamaLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.sugiyama.Layering;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public strictfp class PipelinePanel extends JPanel {
    private final DirectedAcyclicGraph<MaskGraphVertex, MaskMethodEdge> pipelineGraph = new DirectedAcyclicGraph<>(MaskMethodEdge.class);
    private final VisualizationViewer<MaskGraphVertex, MaskMethodEdge> pipelineGraphViewer = VisualizationViewer.builder(pipelineGraph)
            .layoutAlgorithm(SugiyamaLayoutAlgorithm.<MaskGraphVertex, MaskMethodEdge>builder()
                    .layering(Layering.NETWORK_SIMPLEX)
                    .build())
            .layoutSize(new Dimension(1000, 1000))
            .build();
    private final VertexAwareEditingModalGraphMouse<MaskGraphVertex, MaskMethodEdge> graphMouse = VertexAwareEditingModalGraphMouse.<MaskGraphVertex, MaskMethodEdge>builder()
            .renderContextSupplier(pipelineGraphViewer::getRenderContext)
            .multiLayerTransformerSupplier(pipelineGraphViewer.getRenderContext()::getMultiLayerTransformer)
            .vertexFactory(new VertexFactory())
            .edgeFactory(new EdgeFactory())
            .build();
    private final VisualizationScrollPane pipelineGraphPane = new VisualizationScrollPane(pipelineGraphViewer);
    private final MaskGraphVertexEditPanel vertexEditPanel = new MaskGraphVertexEditPanel();
    @Getter
    @Setter
    private Consumer<Pipeline.Entry> entryVertexSelectionAction;

    public PipelinePanel() {
        graphMouse.setModeKeyListener(new EditingModalGraphMouse.ModeKeyAdapter(graphMouse));
        pipelineGraphViewer.setGraphMouse(graphMouse);
        pipelineGraphViewer.addKeyListener(graphMouse.getModeKeyListener());
        pipelineGraphViewer.getSelectedVertexState().addItemListener(this::vertexSelected);
        setLayout(new GridBagLayout());
        setupGraph();
        setupVertexEditPanel();
    }

    private void setupGraph() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.gridy = 0;
        constraints.weighty = 1;

        add(pipelineGraphPane, constraints);
    }

    private void setupVertexEditPanel() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.gridy = 0;
        constraints.weighty = 1;

        add(vertexEditPanel, constraints);
    }

    public void vertexSelected(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof MaskGraphVertex) {
            vertexEditPanel.setVertex((MaskGraphVertex) e.getItem());
        }
    }

    static class VertexFactory implements Supplier<MaskGraphVertex> {

        int i = 0;

        public MaskGraphVertex get() {
            return new MaskMethodVertex(i++);
        }
    }

    static class EdgeFactory implements BiFunction<MaskGraphVertex, MaskGraphVertex, MaskMethodEdge> {

        int i = 0;

        @Override
        public MaskMethodEdge apply(MaskGraphVertex maskGraphVertex, MaskGraphVertex maskGraphVertex2) {
            return new MaskMethodEdge(i++);
        }
    }


}
