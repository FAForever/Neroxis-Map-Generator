package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.generator.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.generator.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.generator.graph.domain.MaskMethodVertex;
import com.faforever.neroxis.generator.graph.domain.MaskVertexResult;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.style.Style;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jgrapht.GraphType;
import org.jgrapht.ListenableGraph;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultListenableGraph;
import org.jgrapht.graph.DirectedAcyclicGraph;

public class PipelineGraph extends Graph implements GraphListener<MaskGraphVertex<?>, MaskMethodEdge>, Iterable<MaskGraphVertex<?>>, org.jgrapht.Graph<MaskGraphVertex<?>, MaskMethodEdge> {
    private final DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> dag = new DirectedAcyclicGraph<>(MaskMethodEdge.class);
    private final ListenableGraph<MaskGraphVertex<?>, MaskMethodEdge> listenableGraph = new DefaultListenableGraph<>(dag);
    private final Map<MaskGraphVertex<?>, ICell> vertexToCell = new HashMap<>();
    private final Map<MaskMethodEdge, ICell> edgeToCell = new HashMap<>();
    private final Map<ICell, MaskGraphVertex<?>> cellToVertex = new HashMap<>();
    private final Map<ICell, MaskMethodEdge> cellToEdge = new HashMap<>();
    private final int baseSize = 100;
    private final float heightFactor = .8f;
    private final float widthFactor = .3f;
    private final float widthPadding = -.3f;
    private final float heightPadding = .1f;

    public PipelineGraph() {
        listenableGraph.addGraphListener(this);
    }

    public void addGraphListener(GraphListener<MaskGraphVertex<?>, MaskMethodEdge> l) {
        listenableGraph.addGraphListener(l);
    }

    @Override
    public void edgeAdded(GraphEdgeChangeEvent<MaskGraphVertex<?>, MaskMethodEdge> e) {
        MaskMethodEdge edge = e.getEdge();
        String parameterName = edge.getParameterName();
        MaskGraphVertex<?> edgeTarget = e.getEdgeTarget();
        MaskGraphVertex<?> edgeSource = e.getEdgeSource();
        edgeTarget.setParameter(parameterName, new MaskVertexResult(edge.getResultName(), edgeSource));
        addVisualEdgeIfNecessary(edgeSource, edgeTarget);
        updateVertexDefinedStyle(edgeTarget);
        refresh();
    }

    private void addVisualEdgeIfNecessary(MaskGraphVertex<?> edgeSource, MaskGraphVertex<?> edgeTarget) {
        MaskMethodEdge edge = getEdge(edgeSource, edgeTarget);
        ICell parentSourceCell = getCellForVertex(edgeSource);
        ICell parentTargetCell = getCellForVertex(edgeTarget);
        if (parentSourceCell == null) {
            throw new IllegalStateException(String.format("Cell does not exist for vertex `%s`", edgeSource.getIdentifier()));
        }
        if (parentTargetCell == null) {
            throw new IllegalStateException(String.format("Cell does not exist for vertex `%s`", edgeTarget.getIdentifier()));
        }
        List<ICell> knownEdges = getEdgesBetween(parentSourceCell, parentTargetCell);
        ICell cell;
        if (knownEdges.isEmpty()) {
            cell = insertEdge(getDefaultParent(), null, null, parentSourceCell, parentTargetCell);
        } else {
            cell = knownEdges.get(0);
        }
        cell.setVisible(false);
        edgeToCell.put(edge, cell);
        cellToEdge.put(cell, edge);
        ICell resultCell = getCellForVertex(edgeSource, edge.getResultName());
        ICell parameterCell = getCellForVertex(edgeTarget, edge.getParameterName());
        if (resultCell == null) {
            throw new IllegalStateException(String.format("Cell does not exist for vertex `%s` `%s`", edgeSource.getIdentifier(), edge.getResultName()));
        }
        if (parameterCell == null) {
            throw new IllegalStateException(String.format("Cell does not exist for vertex `%s` `%s`", edgeTarget.getIdentifier(), edge.getParameterName()));
        }
        List<ICell> knownSubEdges = getEdgesBetween(resultCell, parameterCell);
        if (knownSubEdges.isEmpty()) {
            insertEdge(getDefaultParent(), null, null, resultCell, parameterCell);
        }
    }

    @Override
    public void edgeRemoved(GraphEdgeChangeEvent<MaskGraphVertex<?>, MaskMethodEdge> e) {
        MaskMethodEdge edge = e.getEdge();
        MaskGraphVertex<?> edgeTarget = e.getEdgeTarget();
        edgeTarget.clearParameter(edge.getParameterName());
        removeVisualEdges(edge, e.getEdgeSource(), edgeTarget);
        updateVertexDefinedStyle(edgeTarget);
        refresh();
    }

    private void removeVisualEdges(MaskMethodEdge edge, MaskGraphVertex<?> edgeSource, MaskGraphVertex<?> edgeTarget) {
        if (edge == null) {
            return;
        }
        ICell cell = edgeToCell.remove(edge);
        cellToEdge.remove(cell);
        if (cell != null) {
            removeCells(List.of(cell));
        }
        ICell resultCell = getCellForVertex(edgeSource, edge.getResultName());
        ICell parameterCell = getCellForVertex(edgeTarget, edge.getParameterName());
        if (resultCell != null && parameterCell != null) {
            List<ICell> subEdges = getEdgesBetween(resultCell, parameterCell);
            removeCells(subEdges);
        }
    }

    @Override
    public void vertexAdded(GraphVertexChangeEvent<MaskGraphVertex<?>> e) {
        addVisualVertexIfNecessary(e.getVertex());
        refresh();
    }

    private void addVisualVertexIfNecessary(MaskGraphVertex<?> vertex) {
        ICell cell = getCellForVertex(vertex);
        if (cell == null) {
            cell = insertVertex(getDefaultParent(), null, vertex.getIdentifier(), 0, 0, baseSize, baseSize);
            int numMaskParameters = vertex.getMaskParameters().size();
            for (int i = 0; i < numMaskParameters; ++i) {
                insertVertex(cell, null, vertex.getMaskParameters().get(i), widthPadding, getSubCellYPadding(numMaskParameters, i), baseSize * widthFactor, getSubCellHeight(numMaskParameters), null, true);
            }
            int numResults = vertex.getResultNames().size();
            for (int i = 0; i < numResults; ++i) {
                insertVertex(cell, null, vertex.getResultNames().get(i), 1 - widthFactor - widthPadding, getSubCellYPadding(numResults, i), baseSize * widthFactor, getSubCellHeight(numResults), null, true);
            }
            vertexToCell.put(vertex, cell);
            cellToVertex.put(cell, vertex);
        }
        updateVertexDefinedStyle(vertex);
    }

    private double getSubCellYPadding(int numMaskParameters, double i) {
        return i / numMaskParameters * heightFactor + heightPadding;
    }

    private double getSubCellHeight(int numMaskParameters) {
        return (heightFactor - heightPadding) * baseSize / numMaskParameters;
    }

    @Override
    public void vertexRemoved(GraphVertexChangeEvent<MaskGraphVertex<?>> e) {
        removeVisualVertex(e.getVertex());
        refresh();
    }

    private void removeVisualVertex(MaskGraphVertex<?> vertex) {
        ICell cell = vertexToCell.remove(vertex);
        cellToVertex.remove(cell);
        removeCells(cell.getChildren(), true);
        removeCells(List.of(cell), true);
    }

    @Override
    public Iterator<MaskGraphVertex<?>> iterator() {
        return dag.iterator();
    }

    @Override
    public Set<MaskMethodEdge> getAllEdges(MaskGraphVertex<?> sourceVertex, MaskGraphVertex<?> targetVertex) {
        return listenableGraph.getAllEdges(sourceVertex, targetVertex);
    }

    @Override
    public MaskMethodEdge getEdge(MaskGraphVertex<?> sourceVertex, MaskGraphVertex<?> targetVertex) {
        return listenableGraph.getEdge(sourceVertex, targetVertex);
    }

    @Override
    public Supplier<MaskGraphVertex<?>> getVertexSupplier() {
        return listenableGraph.getVertexSupplier();
    }

    @Override
    public Supplier<MaskMethodEdge> getEdgeSupplier() {
        return listenableGraph.getEdgeSupplier();
    }

    @Override
    public MaskMethodEdge addEdge(MaskGraphVertex<?> sourceVertex, MaskGraphVertex<?> targetVertex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addEdge(MaskGraphVertex<?> sourceVertex, MaskGraphVertex<?> targetVertex, MaskMethodEdge maskMethodEdge) {
        return listenableGraph.addEdge(sourceVertex, targetVertex, maskMethodEdge);
    }

    @Override
    public MaskGraphVertex<?> addVertex() {
        return listenableGraph.addVertex();
    }

    @Override
    public boolean addVertex(MaskGraphVertex<?> maskGraphVertex) {
        return listenableGraph.addVertex(maskGraphVertex);
    }

    @Override
    public boolean containsEdge(MaskGraphVertex<?> sourceVertex, MaskGraphVertex<?> targetVertex) {
        return listenableGraph.containsEdge(sourceVertex, targetVertex);
    }

    @Override
    public boolean containsEdge(MaskMethodEdge maskMethodEdge) {
        return listenableGraph.containsEdge(maskMethodEdge);
    }

    @Override
    public boolean containsVertex(MaskGraphVertex<?> maskGraphVertex) {
        return listenableGraph.containsVertex(maskGraphVertex);
    }

    @Override
    public Set<MaskMethodEdge> edgeSet() {
        return listenableGraph.edgeSet();
    }

    @Override
    public int degreeOf(MaskGraphVertex<?> vertex) {
        return listenableGraph.degreeOf(vertex);
    }

    @Override
    public Set<MaskMethodEdge> edgesOf(MaskGraphVertex<?> vertex) {
        return listenableGraph.edgesOf(vertex);
    }

    @Override
    public int inDegreeOf(MaskGraphVertex<?> vertex) {
        return listenableGraph.inDegreeOf(vertex);
    }

    @Override
    public Set<MaskMethodEdge> incomingEdgesOf(MaskGraphVertex<?> vertex) {
        return listenableGraph.incomingEdgesOf(vertex);
    }

    @Override
    public int outDegreeOf(MaskGraphVertex<?> vertex) {
        return listenableGraph.outDegreeOf(vertex);
    }

    @Override
    public Set<MaskMethodEdge> outgoingEdgesOf(MaskGraphVertex<?> vertex) {
        return listenableGraph.outgoingEdgesOf(vertex);
    }

    @Override
    public boolean removeAllEdges(Collection<? extends MaskMethodEdge> edges) {
        return listenableGraph.removeAllEdges(edges);
    }

    @Override
    public Set<MaskMethodEdge> removeAllEdges(MaskGraphVertex<?> sourceVertex, MaskGraphVertex<?> targetVertex) {
        return listenableGraph.removeAllEdges(sourceVertex, targetVertex);
    }

    @Override
    public boolean removeAllVertices(Collection<? extends MaskGraphVertex<?>> vertices) {
        return listenableGraph.removeAllVertices(vertices);
    }

    @Override
    public MaskMethodEdge removeEdge(MaskGraphVertex<?> sourceVertex, MaskGraphVertex<?> targetVertex) {
        return listenableGraph.removeEdge(sourceVertex, targetVertex);
    }

    @Override
    public boolean removeEdge(MaskMethodEdge maskMethodEdge) {
        return listenableGraph.removeEdge(maskMethodEdge);
    }

    @Override
    public boolean removeVertex(MaskGraphVertex<?> maskGraphVertex) {
        return listenableGraph.removeVertex(maskGraphVertex);
    }

    @Override
    public Set<MaskGraphVertex<?>> vertexSet() {
        return listenableGraph.vertexSet();
    }

    @Override
    public MaskGraphVertex<?> getEdgeSource(MaskMethodEdge maskMethodEdge) {
        return listenableGraph.getEdgeSource(maskMethodEdge);
    }

    @Override
    public MaskGraphVertex<?> getEdgeTarget(MaskMethodEdge maskMethodEdge) {
        return listenableGraph.getEdgeTarget(maskMethodEdge);
    }

    @Override
    public GraphType getType() {
        return listenableGraph.getType();
    }

    @Override
    public double getEdgeWeight(MaskMethodEdge maskMethodEdge) {
        return listenableGraph.getEdgeWeight(maskMethodEdge);
    }

    @Override
    public void setEdgeWeight(MaskMethodEdge maskMethodEdge, double weight) {
        listenableGraph.setEdgeWeight(maskMethodEdge, weight);
    }

    /**
     * Returns true if the given cell is a valid source for new connections.
     * This implementation returns true for all non-null values and is
     * called by is called by <isValidConnection>.
     *
     * @param cell ICell that represents a possible source or null.
     * @return Returns true if the given cell is a valid source terminal.
     */
    @Override
    public boolean isValidSource(ICell cell) {
        MaskGraphVertex<?> vertex = getVertexForCell(cell);
        return vertex != null && cell.getParent() != getDefaultParent() && vertex.getResultNames().contains((String) cell.getValue());
    }

    @Override
    public boolean isValidTarget(ICell cell) {
        MaskGraphVertex<?> vertex = getVertexForCell(cell);
        return vertex != null && cell.getParent() != getDefaultParent() && vertex.getMaskParameters().contains((String) cell.getValue()) && !vertex.isMaskParameterSet((String) cell.getValue());
    }

    @Override
    public boolean isValidConnection(ICell source, ICell target) {
        return isValidSource(source) && isValidTarget(target) && source != target && !getVertexForCell(target).isMaskParameterSet((String) source.getValue());
    }

    public MaskGraphVertex<?> getVertexForCell(ICell cell) {
        while (cell != null && cell.getParent() != getDefaultParent()) {
            cell = cell.getParent();
        }
        return cellToVertex.get(cell);
    }

    public MaskMethodEdge getEdgeForCell(ICell cell) {
        return cellToEdge.get(cell);
    }

    public ICell getCellForVertex(MaskGraphVertex<?> vertex) {
        return getCellForVertex(vertex, null);
    }

    public ICell getCellForVertex(MaskGraphVertex<?> vertex, String subName) {
        ICell parentCell = vertexToCell.get(vertex);
        if (subName == null || parentCell == null) {
            return parentCell;
        }
        return parentCell.getChildren().stream().filter(child -> subName.equals(child.getValue())).findFirst().orElse(null);
    }

    @Override
    public String getLabel(ICell cell) {
        String result = "";
        if (cell != null) {
            CellState state = view.getState(cell);
            Style style = (state != null) ? state.getStyle() : getCellStyle(cell);
            if (labelsVisible && style.getLabel().isVisible()) {
                if (cell.isVertex() && getDefaultParent().equals(cell.getParent()) && getVertexForCell(cell) != null) {
                    MaskGraphVertex<?> vertex = getVertexForCell(cell);
                    result = String.format("%s\n%s\n%s", convertValueToString(cell), vertex.getExecutableName(), vertex.getExecutorClass().getSimpleName());
                } else {
                    result = convertValueToString(cell);
                }
            }
        }
        return result;
    }

    public String getToolTipForCell(ICell cell) {
        MaskGraphVertex<?> vertex = getVertexForCell(cell);
        if (vertex == null) {
            return "";
        }
        if (cell.getParent() != getDefaultParent()) {
            return vertex.getIdentifier() + " -> " + vertex.getExecutableName() + " -> " + cell.getValue();
        }
        return vertex.getIdentifier() + " -> " + vertex.getExecutableName();
    }

    public MaskGraphVertex<?> getDirectDescendant(MaskGraphVertex<?> vertex) {
        return outgoingEdgesOf(vertex).stream().filter(edge -> MaskGraphVertex.SELF.equals(edge.getResultName()) && MaskMethodVertex.EXECUTOR.equals(edge.getParameterName())).map(this::getEdgeTarget).findFirst().orElse(null);
    }

    public MaskGraphVertex<?> getDirectAncestor(MaskGraphVertex<?> vertex) {
        return incomingEdgesOf(vertex).stream().filter(edge -> MaskGraphVertex.SELF.equals(edge.getResultName()) && MaskMethodVertex.EXECUTOR.equals(edge.getParameterName())).map(this::getEdgeSource).findFirst().orElse(null);
    }

    public Set<MaskGraphVertex<?>> getDirectRelationships(MaskGraphVertex<?> vertex) {
        Set<MaskGraphVertex<?>> directRelationships = new HashSet<>();
        MaskGraphVertex<?> nextVertex = vertex;
        while (nextVertex != null) {
            directRelationships.add(nextVertex);
            nextVertex = getDirectDescendant(nextVertex);
        }
        MaskGraphVertex<?> previousVertex = vertex;
        while (previousVertex != null) {
            directRelationships.add(previousVertex);
            previousVertex = getDirectAncestor(previousVertex);
        }
        return Set.copyOf(directRelationships);
    }

    public PipelineGraph getSubGraphFromSelectedCells() {
        PipelineGraph subGraph = new PipelineGraph();
        Map<MaskGraphVertex<?>, MaskGraphVertex<?>> vertexCopyMap = new HashMap<>();
        Set<MaskGraphVertex<?>> selectedVertices = getSelectionCells().stream().map(this::getVertexForCell).filter(Objects::nonNull).collect(Collectors.toSet());
        selectedVertices.forEach(vertex -> {
            MaskGraphVertex<?> vertexCopy = vertex.copy();
            subGraph.addVertex(vertexCopy);
            vertexCopyMap.put(vertex, vertexCopy);
        });
        selectedVertices.stream().flatMap(vertex -> outgoingEdgesOf(vertex).stream()).filter(edge -> selectedVertices.contains(getEdgeTarget(edge))).forEach(edge -> subGraph.addEdge(vertexCopyMap.get(getEdgeSource(edge)), vertexCopyMap.get(getEdgeTarget(edge)), edge.copy()));
        return subGraph;
    }

    public void addGraph(PipelineGraph graph) {
        Map<MaskGraphVertex<?>, MaskGraphVertex<?>> vertexCopyMap = new HashMap<>();
        Set<MaskGraphVertex<?>> newVertices = graph.vertexSet();
        newVertices.forEach(vertex -> {
            MaskGraphVertex<?> vertexCopy = vertex.copy();
            addVertex(vertexCopy);
            vertexCopyMap.put(vertex, vertexCopy);
        });
        newVertices.stream().flatMap(vertex -> graph.outgoingEdgesOf(vertex).stream()).forEach(edge -> addEdge(vertexCopyMap.get(graph.getEdgeSource(edge)), vertexCopyMap.get(graph.getEdgeTarget(edge)), edge.copy()));
    }

    public void updateVertexDefinedStyle(MaskGraphVertex<?> vertex) {
        ICell vertexCell = getCellForVertex(vertex);
        if (vertexCell == null) {
            return;
        }
        Set<ICell> cells = new HashSet<>(vertexCell.getChildren());
        cells.add(vertexCell);
        cells.forEach(cell -> {
            if (!vertex.isDefined()) {
                model.setStyle(cell, "undefined");
            } else {
                model.setStyle(cell, null);
            }
        });
    }
}
