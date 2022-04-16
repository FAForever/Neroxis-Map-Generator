package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.graph.domain.GraphContext;
import com.faforever.neroxis.generator.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.generator.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.generator.graph.io.GraphSerializationUtil;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.ngraph.event.ChangeEvent;
import com.faforever.neroxis.ngraph.layout.hierarchical.HierarchicalLayout;
import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import lombok.Getter;
import lombok.Setter;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;

public strictfp class GraphPane extends JPanel implements GraphListener<MaskGraphVertex<?>, MaskMethodEdge> {
    @Getter
    private final PipelineGraph graph = new PipelineGraph();
    @Getter
    private final PipelineGraphComponent graphComponent = new PipelineGraphComponent(graph);
    private final HierarchicalLayout layout = new HierarchicalLayout(graph);
    @Setter
    private Consumer<MaskGraphVertex<?>> maskVertexSelectionAction = mask -> {};
    @Setter
    private Runnable graphChangedAction = () -> {};

    public GraphPane() {
        setLayout(new BorderLayout());
        setFocusable(true);
        setupGraph();
    }

    private void setupGraph() {
        graph.addGraphListener(this);
        graphComponent.setTolerance(1);
        graphComponent.setViewportBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        graphComponent.setPreferredSize(new Dimension(800, 800));
        add(graphComponent);
        graph.getSelectionModel().addListener(ChangeEvent.class, (sender, event) -> {
            List<ICell> added = event.getAdded();
            if (added != null && !added.isEmpty()) {
                ICell selectionChange = added.get(0);
                vertexSelected(graph.getVertexForCell(selectionChange));
                graphComponent.scrollCellToVisible(selectionChange);
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
        layout.setTraverseAncestors(false);
        layout.setOrientation(SwingConstants.WEST);
        layout.setInterRankCellSpacing(100);
        layout.setInterHierarchySpacing(50);
        layout.setIntraCellSpacing(50);
        layout.setParallelEdgeSpacing(10);
    }

    public void clearGraph() {
        Set<MaskGraphVertex<?>> vertices = new HashSet<>(graph.vertexSet());
        graph.removeAllVertices(vertices);
    }

    public void exportGraph(File file) {
        removeUnusedVertices();
        try {
            GraphSerializationUtil.exportGraph(graph, file);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not write graph");
        }
    }

    public void importGraph(File file) {
        try {
            GraphSerializationUtil.importGraph(graph, file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        layoutGraph();
    }

    public void importGraph(PipelineGraph subGraph) {
        graph.addGraph(subGraph);
        layoutGraph();
    }

    public void exportSelectedCells(File file) {
        PipelineGraph subGraph = graph.getSubGraphFromSelectedCells();
        try {
            GraphSerializationUtil.exportGraph(subGraph, file);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not write graph");
        }
    }

    public void runGraph(int numTeams, int mapSize, int spawnCount, Symmetry terrainSymmetry) {
        Pipeline.reset();
        layoutGraph();
        Random random = new Random();
        ParameterConstraints parameterConstraints = ParameterConstraints.builder().build();
        GeneratorParameters generatorParameters = parameterConstraints.randomizeParameters(random, GeneratorParameters.builder().mapSize(mapSize).numTeams(numTeams).spawnCount(spawnCount).terrainSymmetry(terrainSymmetry).build());
        GraphContext graphContext = new GraphContext(random.nextLong(), generatorParameters, parameterConstraints);
        DebugUtil.timedRun("Reset results", () -> graph.forEach(MaskGraphVertex::resetResult));
        DebugUtil.timedRun("Setup pipeline", () -> graph.forEach(vertex -> {
            if (!vertex.isDefined()) {
                return;
            }
            try {
                vertex.prepareResults(graphContext);
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException ex) {
                throw new RuntimeException(ex);
            }
        }));
        Pipeline.start();
        Pipeline.join();
        MaskGraphVertex<?> vertex = graph.getVertexForCell(graph.getSelectionCell());
        if (maskVertexSelectionAction != null && vertex != null && vertex.isComputed()) {
            maskVertexSelectionAction.accept(vertex);
        }
    }

    public void layoutGraph() {
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
        if (maskVertexSelectionAction != null) {
            maskVertexSelectionAction.accept(vertex);
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
        updateIdentifiers(e.getEdgeSource());
        graphChangedAction.run();
    }

    @Override
    public void edgeRemoved(GraphEdgeChangeEvent<MaskGraphVertex<?>, MaskMethodEdge> e) {
        updateIdentifiers(e.getEdgeTarget());
        graphChangedAction.run();
    }

    @Override
    public void vertexAdded(GraphVertexChangeEvent<MaskGraphVertex<?>> e) {
        graphChangedAction.run();
    }

    @Override
    public void vertexRemoved(GraphVertexChangeEvent<MaskGraphVertex<?>> e) {
        graphChangedAction.run();
    }
}
