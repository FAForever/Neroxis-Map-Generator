/*
 * Copyright (c) 2005-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.layout.hierarchical;

import com.faforever.neroxis.ngraph.layout.hierarchical.model.GraphSingleLayerHierarchyModel;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.view.Graph;
import com.faforever.neroxis.ngraph.view.GraphView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingConstants;
import lombok.Getter;
import lombok.Setter;

/**
 * Hierarchical Layout that only acts on cells that share the indicated parent
 */
@Getter
@Setter
public class SingleLayerHierarchicalLayout extends HierarchicalLayout {
    /**
     * Constructs a hierarchical layout
     *
     * @param graph the graph to lay out
     */
    public SingleLayerHierarchicalLayout(Graph graph) {
        this(graph, SwingConstants.NORTH);
    }

    /**
     * Constructs a hierarchical layout
     *
     * @param graph       the graph to lay out
     * @param orientation <code>SwingConstants.NORTH, SwingConstants.EAST, SwingConstants.SOUTH</code> or <code> SwingConstants.WEST</code>
     */
    public SingleLayerHierarchicalLayout(Graph graph, int orientation) {
        super(graph);
        this.orientation = orientation;
    }

    /**
     * Executes the layout for the children of the specified parent.
     *
     * @param parent Parent cell that contains the children to be laid out.
     * @param roots  the starting roots of the layout
     */
    public void execute(ICell parent, List<ICell> roots) {
        this.parent = parent;
        IGraphModel model = graph.getModel();
        // If the roots are set and the parent is set, only
        // use the roots that are some dependent of the that
        // parent.
        // If just the root are set, use them as-is
        // If just the parent is set use it's immediate
        // children as the initial set
        if (parent == null) {
            throw new IllegalArgumentException("Parent is null");
        }
        if (roots != null) {
            roots = new ArrayList<>(roots);
            roots.removeIf(root -> !model.isAncestor(parent, root));
        }
        this.roots = roots;
        model.beginUpdate();
        try {
            run(parent);
            if (isResizeParent() && !graph.isCellCollapsed(parent)) {
                graph.updateGroupBounds(List.of(parent), getParentBorder(), isMoveParent());
            }
        } finally {
            model.endUpdate();
        }
    }

    /**
     * The API method used to exercise the layout upon the graph description
     * and produce a separate description of the vertex position and edge
     * routing changes made.
     */
    public void run(ICell parent) {
        // Separate out unconnected hierarchies
        List<Set<ICell>> hierarchyVertices = new ArrayList<>();
        Set<ICell> allVertexSet = new LinkedHashSet<>();
        if (this.roots == null && parent != null) {
            Set<ICell> filledVertexSet = filterDescendants(parent);
            this.roots = new ArrayList<>();
            while (!filledVertexSet.isEmpty()) {
                List<ICell> candidateRoots = findRoots(parent, filledVertexSet);
                for (ICell root : candidateRoots) {
                    Set<ICell> vertexSet = new LinkedHashSet<>();
                    hierarchyVertices.add(vertexSet);
                    traverse(root, true, null, allVertexSet, vertexSet, hierarchyVertices, filledVertexSet);
                }
                this.roots.addAll(candidateRoots);
            }
        } else {
            // Find vertex set as directed traversal from roots
            for (ICell root : roots) {
                Set<ICell> vertexSet = new LinkedHashSet<>();
                hierarchyVertices.add(vertexSet);
                traverse(root, true, null, allVertexSet, vertexSet, hierarchyVertices, null);
            }
        }
        // Iterate through the result removing parents who have children in this layout
        // Perform a layout for each separate hierarchy
        // Track initial coordinate x-positioning
        double initialX = 0;
        for (Set<ICell> vertexSet : hierarchyVertices) {
            this.model = new GraphSingleLayerHierarchyModel(this, List.copyOf(vertexSet), roots, parent);
            cycleStage(parent);
            layeringStage();
            crossingStage(parent);
            initialX = placementStage(initialX, parent);
        }
    }

    /**
     * Traverses the (directed) graph invoking the given function for each
     * visited vertex and edge. The function is invoked with the current vertex
     * and the incoming edge as a parameter. This implementation makes sure
     * each vertex is only visited once. The function may return false if the
     * traversal should stop at the given vertex.
     *
     * @param vertex      <Cell> that represents the vertex where the traversal starts.
     * @param directed    Optional boolean indicating if edges should only be traversed
     *                    from source to target. Default is true.
     * @param edge        Optional <Cell> that represents the incoming edge. This is
     *                    null for the first step of the traversal.
     * @param allVertices Array of cell paths for the visited cells.
     */
    protected void traverse(ICell vertex, boolean directed, ICell edge, Set<ICell> allVertices, Set<ICell> currentComp, List<Set<ICell>> hierarchyVertices, Set<ICell> filledVertexSet) {
        if (!this.parent.equals(vertex.getParent())) {
            if (filledVertexSet != null) {
                filledVertexSet.remove(vertex);
            }
            allVertices.add(vertex);
            return;
        }
        GraphView view = graph.getView();
        IGraphModel model = graph.getModel();
        if (vertex != null && allVertices != null) {
            // Has this vertex been seen before in any traversal
            // And if the filled vertex set is populated, only
            // process vertices in that it contains
            if (!allVertices.contains(vertex) && (filledVertexSet == null || filledVertexSet.contains(vertex))) {
                currentComp.add(vertex);
                allVertices.add(vertex);
                if (filledVertexSet != null) {
                    filledVertexSet.remove(vertex);
                }
                int edgeCount = model.getEdgeCount(vertex);
                if (edgeCount > 0) {
                    for (int i = 0; i < edgeCount; i++) {
                        ICell e = model.getEdgeAt(vertex, i);
                        boolean isSource = view.getVisibleTerminal(e, true) == vertex;
                        if (!directed || isSource) {
                            ICell next = view.getVisibleTerminal(e, !isSource);
                            traverse(next, directed, e, allVertices, currentComp, hierarchyVertices, filledVertexSet);
                        }
                    }
                }
            } else {
                if (!currentComp.contains(vertex)) {
                    // We've seen this vertex before, but not in the current component
                    // This component and the one it's in need to be merged
                    Set<ICell> matchComp = null;
                    for (Set<ICell> comp : hierarchyVertices) {
                        if (comp.contains(vertex)) {
                            currentComp.addAll(comp);
                            matchComp = comp;
                            break;
                        }
                    }
                    if (matchComp != null) {
                        hierarchyVertices.remove(matchComp);
                    }
                }
            }
        }
    }

    /**
     * Returns <code>SingleLayerHierarchical</code>, the name of this algorithm.
     */
    public String toString() {
        return "SingleLayerHierarchical";
    }
}
