/*
 * Copyright (c) 2005-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.layout.hierarchical.model;

import com.faforever.neroxis.ngraph.layout.hierarchical.HierarchicalLayout;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.view.Graph;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal model of a hierarchical graph. This model stores nodes and edges
 * equivalent to the real graph nodes and edges, but also stores the rank of the
 * cells, the order within the ranks and the new candidate locations of cells.
 * The internal model also reverses edge direction were appropriate , ignores
 * self-loop and groups parallels together under one edge object.
 */
public class GraphSingleLayerHierarchyModel extends GraphHierarchyModel {
    /**
     * Creates an internal ordered graph model using the vertices passed in. If
     * there are any, leftward edge need to be inverted in the internal model
     *
     * @param layout   the enclosing layout object
     * @param vertices the vertices for this hierarchy
     */
    public GraphSingleLayerHierarchyModel(HierarchicalLayout layout, List<ICell> vertices, List<ICell> roots, ICell parent) {
        super(layout, vertices, roots, parent);
    }

    /**
     * Creates all edges in the internal model
     *
     * @param layout           reference to the layout algorithm
     * @param vertices         the vertices whom are to have an internal representation
     *                         created
     * @param internalVertices the blank internal vertices to have their information filled
     *                         in using the real vertices
     */
    protected void createInternalCells(HierarchicalLayout layout, List<ICell> vertices, GraphHierarchyNode[] internalVertices) {
        Graph graph = layout.getGraph();
        // Create internal edges
        for (int i = 0; i < vertices.size(); i++) {
            ICell vertex = vertices.get(i);
            internalVertices[i] = new GraphHierarchyNode(vertex);
            vertexMapper.put(vertex, internalVertices[i]);
            // If the layout is deterministic, order the cells
            List<ICell> conns = layout.getEdges(vertex);
            List<ICell> outgoingCells = graph.getOpposites(conns, vertex).stream().filter(cell -> parent.equals(cell.getParent())).collect(Collectors.toList());
            internalVertices[i].connectsAsSource = new LinkedHashSet<>(outgoingCells.size());
            // Create internal edges, but don't do any rank assignment yet
            // First use the information from the greedy cycle remover to
            // invert the leftward edges internally
            for (ICell cell : outgoingCells) {
                // Don't add self-loops
                if (cell != vertex && graph.getModel().isVertex(cell) && !layout.isVertexIgnored(cell)) {
                    // We process all edge between this source and its targets
                    // If there are edges going both ways, we need to collect
                    // them all into one internal edges to avoid looping problems
                    // later. We assume this direction (source -> target) is the
                    // natural direction if at least half the edges are going in
                    // that direction.
                    // The check below for edgeMapper.get(edges[0]) == null is
                    // in case we've processed this the other way around
                    // (target -> source) and the number of edges in each direction
                    // are the same. All the graph edges will have been assigned to
                    // an internal edge going the other way, so we don't want to
                    // process them again
                    List<ICell> undirectedEdges = graph.getEdgesBetween(vertex, cell, false);
                    List<ICell> directedEdges = graph.getEdgesBetween(vertex, cell, true);
                    if (undirectedEdges != null && !undirectedEdges.isEmpty() && (edgeMapper.get(undirectedEdges.get(0)) == null) && (directedEdges.size() * 2 >= undirectedEdges.size())) {
                        GraphHierarchyEdge internalEdge = new GraphHierarchyEdge(undirectedEdges);
                        for (ICell edge : undirectedEdges) {
                            edgeMapper.put(edge, internalEdge);
                            // Resets all point on the edge and disables the edge style
                            // without deleting it from the cell style
                            graph.resetEdge(edge);
                            if (layout.isDisableEdgeStyle()) {
                                layout.setEdgeStyleEnabled(edge, false);
                                layout.setOrthogonalEdge(edge, true);
                            }
                        }
                        internalEdge.source = internalVertices[i];
                        internalVertices[i].connectsAsSource.add(internalEdge);
                    }
                }
            }
            // Ensure temp variable is cleared from any previous use
            internalVertices[i].temp[0] = 0;
        }
    }
}
