package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.generator.GeneratorGraphContext;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.graph.GeneratorPipeline;
import com.faforever.neroxis.generator.serial.GeneratorGraphSerializationUtil;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskInputVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.graph.domain.MaskOutputVertex;
import com.faforever.neroxis.graph.io.GraphSerializationUtil;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.ngraph.event.ChangeEvent;
import com.faforever.neroxis.ngraph.layout.hierarchical.HierarchicalLayout;
import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.style.Style;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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

public strictfp class PipelinePane extends JPanel implements GraphListener<MaskGraphVertex<?>, MaskMethodEdge> {
    @Getter
    private final PipelineGraph graph;
    @Getter
    private final GeneratorPipeline pipeline;
    @Getter
    private final PipelineGraphComponent graphComponent;
    private final HierarchicalLayout layout;
    @Setter
    private Consumer<MaskGraphVertex<?>> maskVertexSelectionAction = mask -> {};
    @Setter
    private Runnable graphChangedAction = () -> {};

    public PipelinePane(GeneratorPipeline pipeline) {
        this.pipeline = pipeline;
        graph = new PipelineGraph(pipeline.getGraph());
        graphComponent = new PipelineGraphComponent(graph);
        layout = new HierarchicalLayout(graph);
        setLayout(new BorderLayout());
        setFocusable(true);
        setupGraph();
    }

    private void setupGraph() {
        Style undefinedStyle = graph.getStylesheet().getDefaultVertexStyle().spawnChild();
        undefinedStyle.getShape().setFillColor(new Color(255, 128, 128));
        graph.getStylesheet().putCellStyle("undefined", undefinedStyle);

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
        layoutGraph();
    }

    private void vertexSelected(MaskGraphVertex<?> vertex) {
        if (maskVertexSelectionAction != null) {
            maskVertexSelectionAction.accept(vertex);
        }
    }

    public void clearGraph() {
        Set<MaskGraphVertex<?>> removableVertices = graph.vertexSet()
                                                         .stream()
                                                         .filter(vertex -> !(vertex instanceof MaskOutputVertex
                                                                             || vertex instanceof MaskInputVertex))
                                                         .collect(Collectors.toSet());
        graph.removeAllVertices(removableVertices);
    }

    public void exportPipeline(File file) {
        try {
            GeneratorGraphSerializationUtil.exportPipeline(pipeline, file);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not write graph", ex);
        }
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

    public void importSubGraph(File file) {
        try {
            GraphSerializationUtil.importGraph(graph, file);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read graph", ex);
        }
        layoutGraph();
    }

    public void runGraph(Long seed, int numTeams, int mapSize, int spawnCount, Symmetry terrainSymmetry) {
        Pipeline.reset();
        layoutGraph();
        Random random = seed == null ? new Random() : new Random(seed);
        ParameterConstraints parameterConstraints = ParameterConstraints.builder().build();
        GeneratorParameters generatorParameters = parameterConstraints.randomizeParameters(random,
                                                                                           GeneratorParameters.builder()
                                                                                                              .mapSize(
                                                                                                                      mapSize)
                                                                                                              .numTeams(
                                                                                                                      numTeams)
                                                                                                              .spawnCount(
                                                                                                                      spawnCount)
                                                                                                              .terrainSymmetry(
                                                                                                                      terrainSymmetry)
                                                                                                              .build());
        GeneratorGraphContext graphContext = new GeneratorGraphContext(random.nextLong(), generatorParameters,
                                                                       parameterConstraints);
        DebugUtil.timedRun("Reset results", () -> graph.forEach(MaskGraphVertex::resetResult));
        DebugUtil.timedRun("Place Spawns", graphContext::placeSpawns);
        DebugUtil.timedRun("Setup pipeline", () -> graph.forEach(vertex -> {
            graphComponent.getGraph().setVertexDefined(vertex, vertex.isDefined(graphContext));
            try {
                vertex.prepareResults(graphContext, false);
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
        graph.getDefaultParent()
             .getChildren()
             .forEach(cell -> cell.getChildren().forEach(child -> graph.getOutgoingEdges(child).forEach(edge -> {
                 ICell target = edge.getTarget();
                 ICell source = edge.getSource();
                 ICell parentEdge = graph.getEdgesBetween(cell, target.getParent())
                                         .stream()
                                         .findFirst()
                                         .orElseThrow(() -> new IllegalStateException("No parent"));
                 RectangleDouble sourceParentBoundingBox = graph.getBoundingBox(source.getParent(), false, true);
                 RectangleDouble targetParentBoundingBox = graph.getBoundingBox(target.getParent(), false, true);
                 targetParentBoundingBox.grow(20);
                 List<PointDouble> points = parentEdge.getGeometry().getPoints().stream().filter(point -> {
                     PointDouble transformedPoint = graph.getView()
                                                         .transformControlPoint(graph.getView().getState(edge), point);
                     return !sourceParentBoundingBox.contains(transformedPoint.getX(), transformedPoint.getY())
                            && !targetParentBoundingBox.contains(transformedPoint.getX(), transformedPoint.getY());
                 }).collect(Collectors.toList());
                 Geometry targetGeometry = target.getGeometry();
                 Geometry targetParentGeometry = target.getParent().getGeometry();
                 RectangleDouble targetRectangle = new RectangleDouble(
                         targetParentGeometry.getX() + targetGeometry.getX() * targetParentGeometry.getWidth(),
                         targetParentGeometry.getY() + targetGeometry.getY() * targetParentGeometry.getHeight(),
                         targetGeometry.getWidth(), targetGeometry.getHeight());
                 PointDouble endPoint = new PointDouble(
                         targetRectangle.getCenterX() - targetRectangle.getWidth() / 2 - 10,
                         targetRectangle.getCenterY());
                 points.add(endPoint);
                 edge.getGeometry().setPoints(points);
                 edge.setTarget(target);
                 edge.setSource(source);
             })));
        graph.vertexSet().forEach(this::updateVertexDefined);
        graphComponent.refresh();
    }

    public void importSubGraph(PipelineGraph subGraph) {
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

    @Override
    public void edgeAdded(GraphEdgeChangeEvent<MaskGraphVertex<?>, MaskMethodEdge> e) {
        MaskGraphVertex<?> edgeTarget = e.getEdgeTarget();
        updateVertexDefined(edgeTarget);
        updateIdentifiers(e.getEdgeSource());
        graphChangedAction.run();
    }

    @Override
    public void edgeRemoved(GraphEdgeChangeEvent<MaskGraphVertex<?>, MaskMethodEdge> e) {
        MaskGraphVertex<?> edgeTarget = e.getEdgeTarget();
        updateVertexDefined(edgeTarget);
        updateIdentifiers(edgeTarget);
        graphChangedAction.run();
    }

    private void updateVertexDefined(MaskGraphVertex<?> vertex) {
        ParameterConstraints parameterConstraints = ParameterConstraints.builder().build();
        GeneratorParameters generatorParameters = GeneratorParameters.builder()
                                                                     .terrainSymmetry(Symmetry.POINT2)
                                                                     .mapSize(512)
                                                                     .numTeams(2)
                                                                     .spawnCount(2)
                                                                     .build();
        GeneratorGraphContext graphContext = new GeneratorGraphContext(0L, generatorParameters, parameterConstraints);
        graph.setVertexDefined(vertex, vertex.isDefined(graphContext));
    }

    public void updateIdentifiers(MaskGraphVertex<?> vertex) {
        String identifier = vertex.getIdentifier();
        graph.getDirectRelationships(vertex).forEach(node -> {
            node.setIdentifier(identifier);
            graph.getCellForVertex(node).setValue(identifier);
        });
        graphComponent.refresh();
    }

    @Override
    public void vertexAdded(GraphVertexChangeEvent<MaskGraphVertex<?>> e) {
        updateVertexDefined(e.getVertex());
        graphChangedAction.run();
    }

    @Override
    public void vertexRemoved(GraphVertexChangeEvent<MaskGraphVertex<?>> e) {
        graphChangedAction.run();
    }
}
