/*
 * Copyright (c) 2005-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.layout.hierarchical;

import com.faforever.neroxis.ngraph.layout.GraphLayout;
import com.faforever.neroxis.ngraph.layout.hierarchical.model.GraphHierarchyModel;
import com.faforever.neroxis.ngraph.layout.hierarchical.stage.CoordinateAssignment;
import com.faforever.neroxis.ngraph.layout.hierarchical.stage.HierarchicalLayoutStage;
import com.faforever.neroxis.ngraph.layout.hierarchical.stage.MedianHybridCrossingReduction;
import com.faforever.neroxis.ngraph.layout.hierarchical.stage.MinimumCycleRemover;
import com.faforever.neroxis.ngraph.model.GraphModel;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;
import com.faforever.neroxis.ngraph.view.GraphView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingConstants;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * The top level compound layout of the hierarchical layout. The individual
 * elements of the layout are called in sequence.
 */
@Getter
@Setter
public class HierarchicalLayout extends GraphLayout {
    /**
     * The root nodes of the layout
     */
    protected List<ICell> roots = null;
    /**
     * Specifies if the parent should be resized after the layout so that it
     * contains all the child cells. Default is true. @See parentBorder.
     */
    protected boolean resizeParent = true;
    /**
     * Specifies if the parent should be moved if resizeParent is enabled.
     * Default is false. @See resizeParent.
     */
    protected boolean moveParent = false;
    /**
     * The border to be added around the children if the parent is to be
     * resized using resizeParent. Default is 0. @See resizeParent.
     */
    protected int parentBorder = 0;
    /**
     * The spacing buffer added between cells on the same layer
     */
    protected double intraCellSpacing = 30.0;
    /**
     * The spacing buffer added between cell on adjacent layers
     */
    protected double interRankCellSpacing = 50.0;
    /**
     * The spacing buffer between unconnected hierarchies
     */
    protected double interHierarchySpacing = 60.0;
    /**
     * The distance between each parallel edge on each ranks for long edges
     */
    protected double parallelEdgeSpacing = 10.0;
    /**
     * The position of the root node(s) relative to the laid out graph in.
     * Default is <code>SwingConstants.NORTH</code>, i.e. top-down.
     */
    protected int orientation;
    /**
     * Specifies if the STYLE_NOEDGESTYLE flag should be set on edges that are
     * modified by the result. Default is true.
     */
    protected boolean disableEdgeStyle = true;
    /**
     * Whether or not to perform local optimisations and iterate multiple times
     * through the algorithm
     */
    protected boolean fineTuning = true;
    /**
     * Whether or not to navigate edges whose terminal vertices
     * have different parents but are in the same ancestry chain
     */
    protected boolean traverseAncestors = true;
    /**
     * The internal model formed of the layout
     */
    @Setter(AccessLevel.NONE)
    protected GraphHierarchyModel model = null;

    /**
     * Constructs a hierarchical layout
     *
     * @param graph the graph to lay out
     */
    public HierarchicalLayout(Graph graph) {
        this(graph, SwingConstants.NORTH);
    }

    /**
     * Constructs a hierarchical layout
     *
     * @param graph       the graph to lay out
     * @param orientation <code>SwingConstants.NORTH, SwingConstants.EAST, SwingConstants.SOUTH</code> or <code> SwingConstants.WEST</code>
     */
    public HierarchicalLayout(Graph graph, int orientation) {
        super(graph);
        this.orientation = orientation;
    }

    /**
     * Returns the model for this layout algorithm.
     */
    public GraphHierarchyModel getModel() {
        return model;
    }

    /**
     * Executes the layout for the children of the specified parent.
     *
     * @param parent Parent cell that contains the children to be laid out.
     */
    @Override
    public void execute(ICell parent) {
        execute(parent, null);
    }

    /**
     * Executes the layout for the children of the specified parent.
     *
     * @param parent Parent cell that contains the children to be laid out.
     * @param roots  the starting roots of the layout
     */
    public void execute(ICell parent, List<ICell> roots) {
        super.execute(parent);
        IGraphModel model = graph.getModel();

        // If the roots are set and the parent is set, only
        // use the roots that are some dependent of the that
        // parent.
        // If just the root are set, use them as-is
        // If just the parent is set use it's immediate
        // children as the initial set

        if (roots == null && parent == null) {
            return;
        }

        if (roots != null && parent != null) {
            roots = new ArrayList<>(roots);
            roots.removeIf(root -> !model.isAncestor(parent, root));
        }

        this.roots = roots;

        model.beginUpdate();
        try {
            run(parent);

            if (parent != null && isResizeParent() && !graph.isCellCollapsed(parent)) {
                graph.updateGroupBounds(List.of(parent), getParentBorder(), isMoveParent());
            }
        } finally {
            model.endUpdate();
        }
    }

    /**
     * Returns all vertices in the given set which do not have
     * incoming edges. If the result is empty then the children with the
     * maximum difference between incoming and outgoing edges are returned.
     * This takes into account edges that are being promoted to the given
     * root due to invisible children or collapsed cells.
     *
     * @return List of tree roots in parent.
     */
    public List<ICell> findRoots(Set<ICell> vertices) {
        List<ICell> roots = new ArrayList<>();
        ICell best = null;
        int maxDiff = Integer.MIN_VALUE;
        IGraphModel model = graph.getModel();
        for (ICell vertex : vertices) {
            if (model.isVertex(vertex) && graph.isCellVisible(vertex)) {
                List<ICell> conns = this.getEdges(vertex);
                int fanOut = 0;
                int fanIn = 0;

                for (ICell conn : conns) {
                    ICell src = graph.getView().getVisibleTerminal(conn, true);

                    if (src == vertex) {
                        fanOut++;
                    } else {
                        fanIn++;
                    }
                }

                if (fanIn == 0 && fanOut > 0) {
                    roots.add(vertex);
                }

                int diff = fanOut - fanIn;

                if (diff > maxDiff) {
                    maxDiff = diff;
                    best = vertex;
                }
            }
        }

        if (roots.isEmpty() && best != null) {
            roots.add(best);
        }

        return roots;
    }

    public List<ICell> getEdges(ICell cell) {
        IGraphModel model = graph.getModel();
        boolean isCollapsed = graph.isCellCollapsed(cell);
        List<ICell> edges = new ArrayList<>();
        int childCount = model.getChildCount(cell);

        for (int i = 0; i < childCount; i++) {
            ICell child = model.getChildAt(cell, i);

            if (isCollapsed || !graph.isCellVisible(child)) {
                edges.addAll(GraphModel.getEdges(model, child, true, true, false));
            }
        }

        edges.addAll(GraphModel.getEdges(model, cell, true, true, false));
        List<ICell> result = new ArrayList<>(edges.size());

        for (ICell edge : edges) {
            CellState state = graph.getView().getState(edge);
            ICell source = (state != null) ? state.getVisibleTerminal(true) : graph.getView()
                                                                                   .getVisibleTerminal(edge, true);
            ICell target = (state != null) ? state.getVisibleTerminal(false) : graph.getView()
                                                                                    .getVisibleTerminal(edge, false);

            if (((source != target) && ((target == cell && (parent == null || graph.isValidAncestor(source, parent,
                                                                                                    traverseAncestors)))
                                        || (source == cell && (parent == null || graph.isValidAncestor(target, parent,
                                                                                                       traverseAncestors)))))) {
                result.add(edge);
            }
        }

        return result;
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
                List<ICell> candidateRoots = findRoots(filledVertexSet);

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
            this.model = new GraphHierarchyModel(this, List.copyOf(vertexSet), roots, parent);

            cycleStage(parent);
            layeringStage();
            crossingStage(parent);
            initialX = placementStage(initialX, parent);
        }
    }

    /**
     * Creates a set of descendant cells
     *
     * @param cell The cell whose descendants are to be calculated
     * @return the descendants of the cell (not the cell)
     */
    public Set<ICell> filterDescendants(ICell cell) {
        IGraphModel model = graph.getModel();
        Set<ICell> result = new LinkedHashSet<>();

        if (model.isVertex(cell) && cell != this.parent && graph.isCellVisible(cell)) {
            result.add(cell);
        }

        if (this.traverseAncestors || cell == this.parent && graph.isCellVisible(cell)) {
            int childCount = model.getChildCount(cell);

            for (int i = 0; i < childCount; i++) {
                ICell child = model.getChildAt(cell, i);
                result.addAll(filterDescendants(child));
            }
        }

        return result;
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
    protected void traverse(ICell vertex, boolean directed, ICell edge, Set<ICell> allVertices, Set<ICell> currentComp,
                            List<Set<ICell>> hierarchyVertices, Set<ICell> filledVertexSet) {
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
     * Executes the cycle stage. This implementation uses the
     * MinimumCycleRemover.
     */
    public void cycleStage(Object parent) {
        HierarchicalLayoutStage cycleStage = new MinimumCycleRemover(this);
        cycleStage.execute(parent);
    }

    /**
     * Implements first stage of a Sugiyama layout.
     */
    public void layeringStage() {
        model.initialRank();
        model.fixRanks();
    }

    /**
     * Executes the crossing stage using MedianHybridCrossingReduction.
     */
    public void crossingStage(Object parent) {
        HierarchicalLayoutStage crossingStage = new MedianHybridCrossingReduction(this);
        crossingStage.execute(parent);
    }

    /**
     * Executes the placement stage using CoordinateAssignment.
     */
    public double placementStage(double initialX, Object parent) {
        CoordinateAssignment placementStage = new CoordinateAssignment(this, intraCellSpacing, interRankCellSpacing,
                                                                       orientation, initialX, parallelEdgeSpacing);
        placementStage.setFineTuning(fineTuning);
        placementStage.execute(parent);

        return placementStage.getLimitX() + interHierarchySpacing;
    }

    /**
     * Returns <code>Hierarchical</code>, the name of this algorithm.
     */
    public String toString() {
        return "Hierarchical";
    }
}
