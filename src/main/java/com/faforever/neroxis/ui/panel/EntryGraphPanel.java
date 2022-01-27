package com.faforever.neroxis.ui.panel;

import com.faforever.neroxis.graph.domain.EntryEdge;
import com.faforever.neroxis.graph.domain.EntryVertex;
import com.faforever.neroxis.util.Pipeline;
import lombok.Getter;
import lombok.Setter;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jungrapht.visualization.RenderContext;
import org.jungrapht.visualization.VisualizationScrollPane;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.DefaultModalGraphMouse;
import org.jungrapht.visualization.control.modal.Modal;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.SugiyamaLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.sugiyama.Layering;
import org.jungrapht.visualization.renderers.Renderer;
import org.jungrapht.visualization.selection.MutableSelectedState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public strictfp class EntryGraphPanel extends JPanel {
    private final DefaultModalGraphMouse<EntryVertex, EntryEdge> graphMouse = DefaultModalGraphMouse.<EntryVertex, EntryEdge>builder()
            .vertexSelectionOnly(true)
            .build();
    private final DirectedAcyclicGraph<EntryVertex, EntryEdge> entryGraph = new DirectedAcyclicGraph<>(EntryEdge.class);
    private final VisualizationViewer<EntryVertex, EntryEdge> entryGraphViewer = VisualizationViewer.builder(entryGraph)
            .layoutAlgorithm(SugiyamaLayoutAlgorithm.<EntryVertex, EntryEdge>builder()
                    .layering(Layering.NETWORK_SIMPLEX)
                    .build())
            .layoutSize(new Dimension(10000, 10000))
            .graphMouse(graphMouse)
            .build();
    private final VisualizationScrollPane entryGraphPane = new VisualizationScrollPane(entryGraphViewer);
    private final JLabel detailsLabel = new JLabel();
    @Getter
    @Setter
    private Consumer<Pipeline.Entry> entryVertexSelectionAction;

    public EntryGraphPanel(List<Pipeline.Entry> entries) {
        entries.forEach(entry -> {
            EntryVertex fromVertex = new EntryVertex(entry);
            entryGraph.addVertex(fromVertex);
            entry.getDependants().forEach(dependant -> {
                EntryVertex toVertex = new EntryVertex(dependant);
                entryGraph.addVertex(toVertex);
                entryGraph.addEdge(fromVertex, toVertex, new EntryEdge(fromVertex, toVertex, entry));
            });
        });
        setLayout(new GridBagLayout());
        setupGraph();
        setupLabels();
    }

    private void setupLabels() {
        detailsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        GridBagConstraints detailsConstraints = new GridBagConstraints();
        detailsConstraints.fill = GridBagConstraints.BOTH;
        detailsConstraints.gridx = 0;
        detailsConstraints.weightx = 1;
        detailsConstraints.gridy = 1;
        detailsConstraints.weighty = 0;

        add(detailsLabel, detailsConstraints);
    }

    private void setupGraph() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridy = 0;
        constraints.weighty = 1;

        graphMouse.setMode(Modal.Mode.PICKING);
        entryGraphViewer.setGraphMouse(graphMouse);
        entryGraphViewer.getSelectedVertexState().addItemListener(this::vertexSelected);
        JComponent graphComponent = entryGraphViewer.getComponent();
        graphComponent.addKeyListener(new EntryGraphKeyListener());
        RenderContext<EntryVertex, EntryEdge> renderContext = entryGraphViewer.getRenderContext();
        renderContext.setVertexLabelFunction(entryVertex -> entryVertex.getEntry().getMethodName());
        renderContext.setEdgeLabelFunction(entryEdge -> entryEdge.getEntry().getExecutingMask().getName());
        renderContext.setEdgeWidth(3);
        renderContext.setVertexShapeFunction(entryVertex -> new Rectangle(-25, -25, 50, 50));
        renderContext.setVertexLabelPosition(Renderer.VertexLabel.Position.CNTR);
        renderContext.setVertexFontFunction(entryVertex -> new Font("Helvetica", Font.PLAIN, 18));
        renderContext.setEdgeFontFunction(entryVertex -> new Font("Helvetica", Font.BOLD, 14));
        entryGraphViewer.setVertexToolTipFunction(entryVertex -> {
            Pipeline.Entry entry = entryVertex.getEntry();
            return String.format("%s -> %s.%s(%s)", entry.getLine(), entry.getExecutingMask().getName(), entry.getMethodName(),
                    entry.getDependencies().stream().filter(dependecy -> !dependecy.getExecutingMask().equals(entry.getExecutingMask()))
                            .map(dependency -> dependency.getExecutingMask().getName())
                            .collect(Collectors.joining(", "))
            );
        });

        LayoutAlgorithm<EntryVertex> layoutAlgorithm = entryGraphViewer.getVisualizationModel().getLayoutAlgorithm();
        layoutAlgorithm.visit(entryGraphViewer.getVisualizationModel().getLayoutModel());

        entryGraphPane.setMinimumSize(new Dimension(450, 700));
        entryGraphPane.setPreferredSize(new Dimension(450, 700));
        add(entryGraphPane, constraints);
    }

    public void vertexSelected(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            Pipeline.Entry entry = ((EntryVertex) e.getItem()).getEntry();
            entryVertexSelectionAction.accept(entry);
            detailsLabel.setText(String.format("%s -> %s.%s(%s)", entry.getLine(), entry.getExecutingMask().getName(), entry.getMethodName(),
                    entry.getDependencies().stream().filter(dependecy -> !dependecy.getExecutingMask().equals(entry.getExecutingMask()))
                            .map(dependency -> dependency.getExecutingMask().getName())
                            .collect(Collectors.joining(", "))));
        }
    }

    private class EntryGraphKeyListener extends KeyAdapter {
        @Override
        public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() == 'n') {
                entryGraphViewer.getSelectedVertices().stream().findFirst().flatMap(entryVertex ->
                        entryGraph.outgoingEdgesOf(entryVertex).stream().map(entryGraph::getEdgeTarget)
                                .filter(targetVertex -> targetVertex.getEntry().getExecutingMask().equals(entryVertex.getEntry().getExecutingMask()))
                                .findFirst()).ifPresent(nextVertex -> {
                    MutableSelectedState<EntryVertex> selectedVertexState = entryGraphViewer.getSelectedVertexState();
                    selectedVertexState.clear();
                    selectedVertexState.select(nextVertex);
                });
            } else if (e.getKeyChar() == 'p') {
                entryGraphViewer.getSelectedVertices().stream().findFirst().flatMap(entryVertex ->
                        entryGraph.incomingEdgesOf(entryVertex).stream().map(entryGraph::getEdgeSource)
                                .filter(targetVertex -> targetVertex.getEntry().getExecutingMask().equals(entryVertex.getEntry().getExecutingMask()))
                                .findFirst()).ifPresent(nextVertex -> {
                    MutableSelectedState<EntryVertex> selectedVertexState = entryGraphViewer.getSelectedVertexState();
                    selectedVertexState.clear();
                    selectedVertexState.select(nextVertex);
                });
            }
        }
    }
}
