/*
 * Copyright (c) 2005-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.layout.hierarchical.model;

import com.faforever.neroxis.ngraph.layout.hierarchical.HierarchicalLayout;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.view.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Internal model of a hierarchical graph. This model stores nodes and edges
 * equivalent to the real graph nodes and edges, but also stores the rank of the
 * cells, the order within the ranks and the new candidate locations of cells.
 * The internal model also reverses edge direction were appropriate , ignores
 * self-loop and groups parallels together under one edge object.
 */
public class GraphHierarchyModel {
    /**
     * High value to start source layering scan rank value from
     */
    private final int SOURCESCANSTARTRANK = 100000000;
    /**
     * Stores the largest rank number allocated
     */
    public int maxRank;
    /**
     * Mapping from rank number to actual rank
     */
    public Map<Integer, GraphHierarchyRank> ranks = null;
    /**
     * Store of roots of this hierarchy model, these are real graph cells, not
     * internal cells
     */
    public List<ICell> roots;
    /**
     * The parent cell whose children are being laid out
     */
    public ICell parent;
    /**
     * Map from graph vertices to internal model nodes
     */
    protected Map<ICell, GraphHierarchyNode> vertexMapper;
    /**
     * Map from graph edges to internal model edges
     */
    protected Map<ICell, GraphHierarchyEdge> edgeMapper;
    /**
     * Count of the number of times the ancestor dfs has been used
     */
    protected int dfsCount = 0;

    /**
     * Creates an internal ordered graph model using the vertices passed in. If
     * there are any, leftward edge need to be inverted in the internal model
     *
     * @param layout   the enclosing layout object
     * @param vertices the vertices for this hierarchy
     */
    public GraphHierarchyModel(HierarchicalLayout layout, ICell[] vertices, List<ICell> roots, ICell parent) {
        Graph graph = layout.getGraph();
        this.roots = roots;
        this.parent = parent;

        if (vertices == null) {
            vertices = graph.getChildVertices(parent);
        }

        // map of cells to internal cell needed for second run through
        // to setup the sink of edges correctly. Guess size by number
        // of edges is roughly same as number of vertices.
        vertexMapper = new HashMap<>(vertices.length);
        edgeMapper = new HashMap<>(vertices.length);

        maxRank = SOURCESCANSTARTRANK;

        GraphHierarchyNode[] internalVertices = new GraphHierarchyNode[vertices.length];
        createInternalCells(layout, vertices, internalVertices);

        // Go through edges set their sink values. Also check the
        // ordering if and invert edges if necessary
        for (int i = 0; i < vertices.length; i++) {
            Collection<GraphHierarchyEdge> edges = internalVertices[i].connectsAsSource;

            for (GraphHierarchyEdge internalEdge : edges) {
                Collection<ICell> realEdges = internalEdge.edges;
                Iterator<ICell> iter2 = realEdges.iterator();

                // Only need to process the first real edge, since
                // all the edges connect to the same other vertex
                if (iter2.hasNext()) {
                    ICell realEdge = iter2.next();
                    Object targetCell = graph.getView().getVisibleTerminal(realEdge, false);
                    GraphHierarchyNode internalTargetCell = vertexMapper.get(targetCell);

                    if (internalVertices[i] == internalTargetCell) {
                        // The real edge is reversed relative to the internal edge
                        targetCell = graph.getView().getVisibleTerminal(realEdge, true);
                        internalTargetCell = vertexMapper.get(targetCell);
                    }

                    if (internalTargetCell != null && internalVertices[i] != internalTargetCell) {
                        internalEdge.target = internalTargetCell;

                        if (internalTargetCell.connectsAsTarget.size() == 0) {
                            internalTargetCell.connectsAsTarget = new LinkedHashSet<GraphHierarchyEdge>(4);
                        }

                        internalTargetCell.connectsAsTarget.add(internalEdge);
                    }
                }
            }

            // Use the temp variable in the internal nodes to mark this
            // internal vertex as having been visited.
            internalVertices[i].temp[0] = 1;
        }
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
    protected void createInternalCells(HierarchicalLayout layout, ICell[] vertices, GraphHierarchyNode[] internalVertices) {
        Graph graph = layout.getGraph();

        // Create internal edges
        for (int i = 0; i < vertices.length; i++) {
            internalVertices[i] = new GraphHierarchyNode(vertices[i]);
            vertexMapper.put(vertices[i], internalVertices[i]);

            // If the layout is deterministic, order the cells
            ICell[] conns = layout.getEdges(vertices[i]);
            List<ICell> outgoingCells = Arrays.asList(graph.getOpposites(conns, vertices[i]));
            internalVertices[i].connectsAsSource = new LinkedHashSet<>(outgoingCells.size());

            // Create internal edges, but don't do any rank assignment yet
            // First use the information from the greedy cycle remover to
            // invert the leftward edges internally

            for (ICell cell : outgoingCells) {
                // Don't add self-loops
                if (cell != vertices[i] && graph.getModel().isVertex(cell) && !layout.isVertexIgnored(cell)) {
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
                    ICell[] undirectEdges = graph.getEdgesBetween(vertices[i], cell, false);
                    ICell[] directedEdges = graph.getEdgesBetween(vertices[i], cell, true);

                    if (undirectEdges != null && undirectEdges.length > 0 && (edgeMapper.get(undirectEdges[0]) == null) && (directedEdges.length * 2 >= undirectEdges.length)) {

                        ArrayList<ICell> listEdges = new ArrayList<>(Arrays.asList(undirectEdges));

                        GraphHierarchyEdge internalEdge = new GraphHierarchyEdge(listEdges);

                        for (ICell edge : listEdges) {
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

    /**
     * Basic determination of minimum layer ranking by working from from sources
     * or sinks and working through each node in the relevant edge direction.
     * Starting at the sinks is basically a longest path layering algorithm.
     */
    public void initialRank() {
        Collection<GraphHierarchyNode> internalNodes = vertexMapper.values();
        LinkedList<GraphHierarchyNode> startNodes = new LinkedList<GraphHierarchyNode>();

        if (roots != null) {

            for (ICell root : roots) {
                GraphHierarchyNode internalNode = vertexMapper.get(root);

                if (internalNode != null) {
                    startNodes.add(internalNode);
                }
            }
        }

        Iterator<GraphHierarchyNode> iter = internalNodes.iterator();

        while (iter.hasNext()) {
            GraphHierarchyNode internalNode = iter.next();
            // Mark the node as not having had a layer assigned
            internalNode.temp[0] = -1;
        }

        List<GraphHierarchyNode> startNodesCopy = new ArrayList<GraphHierarchyNode>(startNodes);

        while (!startNodes.isEmpty()) {
            GraphHierarchyNode internalNode = startNodes.getFirst();
            Collection<GraphHierarchyEdge> layerDeterminingEdges;
            Collection<GraphHierarchyEdge> edgesToBeMarked;

            layerDeterminingEdges = internalNode.connectsAsTarget;
            edgesToBeMarked = internalNode.connectsAsSource;

            // flag to keep track of whether or not all layer determining
            // edges have been scanned
            boolean allEdgesScanned = true;

            // Work out the layer of this node from the layer determining
            // edges
            Iterator<GraphHierarchyEdge> iter2 = layerDeterminingEdges.iterator();

            // The minimum layer number of any node connected by one of
            // the layer determining edges variable. If we are starting
            // from sources, need to start at some huge value and
            // normalise down afterwards
            int minimumLayer = SOURCESCANSTARTRANK;

            while (allEdgesScanned && iter2.hasNext()) {
                GraphHierarchyEdge internalEdge = iter2.next();

                if (internalEdge.temp[0] == 5270620) {
                    // This edge has been scanned, get the layer of the
                    // node on the other end
                    GraphHierarchyNode otherNode = internalEdge.source;
                    minimumLayer = Math.min(minimumLayer, otherNode.temp[0] - 1);
                } else {
                    allEdgesScanned = false;
                }
            }

            // If all edge have been scanned, assign the layer, mark all
            // edges in the other direction and remove from the nodes list
            if (allEdgesScanned) {
                internalNode.temp[0] = minimumLayer;
                maxRank = Math.min(maxRank, minimumLayer);

                if (edgesToBeMarked != null) {
                    Iterator<GraphHierarchyEdge> iter3 = edgesToBeMarked.iterator();

                    while (iter3.hasNext()) {
                        GraphHierarchyEdge internalEdge = iter3.next();
                        // Assign unique stamp ( y/m/d/h )
                        internalEdge.temp[0] = 5270620;

                        // Add node on other end of edge to LinkedList of
                        // nodes to be analysed
                        GraphHierarchyNode otherNode = internalEdge.target;

                        // Only add node if it hasn't been assigned a layer
                        if (otherNode.temp[0] == -1) {
                            startNodes.addLast(otherNode);

                            // Mark this other node as neither being
                            // unassigned nor assigned so it isn't
                            // added to this list again, but it's
                            // layer isn't used in any calculation.
                            otherNode.temp[0] = -2;
                        }
                    }
                }

                startNodes.removeFirst();
            } else {
                // Not all the edges have been scanned, get to the back of
                // the class and put the dunces cap on
                Object removedCell = startNodes.removeFirst();
                startNodes.addLast(internalNode);

                if (removedCell == internalNode && startNodes.size() == 1) {
                    // This is an error condition, we can't get out of
                    // this loop. It could happen for more than one node
                    // but that's a lot harder to detect. Log the error
                    // TODO make log comment
                    break;
                }
            }
        }

        // Normalize the ranks down from their large starting value to place
        // at least 1 sink on layer 0
        iter = internalNodes.iterator();
        while (iter.hasNext()) {
            GraphHierarchyNode internalNode = iter.next();
            // Mark the node as not having had a layer assigned
            internalNode.temp[0] -= maxRank;
        }

        // Tighten the roots as far as possible
        for (int i = 0; i < startNodesCopy.size(); i++) {
            GraphHierarchyNode internalNode = startNodesCopy.get(i);
            int currentMaxLayer = 0;
            Iterator<GraphHierarchyEdge> iter2 = internalNode.connectsAsSource.iterator();

            while (iter2.hasNext()) {
                GraphHierarchyEdge internalEdge = iter2.next();
                GraphHierarchyNode otherNode = internalEdge.target;
                internalNode.temp[0] = Math.max(currentMaxLayer, otherNode.temp[0] + 1);
                currentMaxLayer = internalNode.temp[0];
            }
        }

        // Reset the maxRank to that which would be expected for a from-sink
        // scan
        maxRank = SOURCESCANSTARTRANK - maxRank;
    }

    /**
     * Fixes the layer assignments to the values stored in the nodes. Also needs
     * to create dummy nodes for edges that cross layers.
     */
    public void fixRanks() {
        final GraphHierarchyRank[] rankList = new GraphHierarchyRank[maxRank + 1];
        ranks = new LinkedHashMap<Integer, GraphHierarchyRank>(maxRank + 1);

        for (int i = 0; i < maxRank + 1; i++) {
            rankList[i] = new GraphHierarchyRank();
            ranks.put(new Integer(i), rankList[i]);
        }

        // Perform a DFS to obtain an initial ordering for each rank.
        // Without doing this you would end up having to process
        // crossings for a standard tree.
        GraphHierarchyNode[] rootsArray = null;

        if (roots != null) {
            Object[] oldRootsArray = roots.toArray();
            rootsArray = new GraphHierarchyNode[oldRootsArray.length];

            for (int i = 0; i < oldRootsArray.length; i++) {
                Object node = oldRootsArray[i];
                GraphHierarchyNode internalNode = vertexMapper.get(node);
                rootsArray[i] = internalNode;
            }
        }

        visit(new GraphHierarchyModel.CellVisitor() {
            public void visit(GraphHierarchyNode parent, GraphHierarchyNode cell, GraphHierarchyEdge connectingEdge, int layer, int seen) {
                GraphHierarchyNode node = cell;

                if (seen == 0 && node.maxRank < 0 && node.minRank < 0) {
                    rankList[node.temp[0]].add(cell);
                    node.maxRank = node.temp[0];
                    node.minRank = node.temp[0];

                    // Set temp[0] to the nodes position in the rank
                    node.temp[0] = rankList[node.maxRank].size() - 1;
                }

                if (parent != null && connectingEdge != null) {
                    int parentToCellRankDifference = (parent).maxRank - node.maxRank;

                    if (parentToCellRankDifference > 1) {
                        // There are ranks in between the parent and current cell
                        GraphHierarchyEdge edge = connectingEdge;
                        edge.maxRank = (parent).maxRank;
                        edge.minRank = (cell).maxRank;
                        edge.temp = new int[parentToCellRankDifference - 1];
                        edge.x = new double[parentToCellRankDifference - 1];
                        edge.y = new double[parentToCellRankDifference - 1];

                        for (int i = edge.minRank + 1; i < edge.maxRank; i++) {
                            // The connecting edge must be added to the
                            // appropriate ranks
                            rankList[i].add(edge);
                            edge.setGeneralPurposeVariable(i, rankList[i].size() - 1);
                        }
                    }
                }
            }
        }, rootsArray, false, null);
    }

    /**
     * A depth first search through the internal hierarchy model
     *
     * @param visitor        the visitor pattern to be called for each node
     * @param trackAncestors whether or not the search is to keep track all nodes directly
     *                       above this one in the search path
     */
    public void visit(CellVisitor visitor, GraphHierarchyNode[] dfsRoots, boolean trackAncestors, Set<GraphHierarchyNode> seenNodes) {
        // Run dfs through on all roots
        if (dfsRoots != null) {
            for (int i = 0; i < dfsRoots.length; i++) {
                GraphHierarchyNode internalNode = dfsRoots[i];

                if (internalNode != null) {
                    if (seenNodes == null) {
                        seenNodes = new HashSet<GraphHierarchyNode>();
                    }

                    if (trackAncestors) {
                        // Set up hash code for root
                        internalNode.hashCode = new int[2];
                        internalNode.hashCode[0] = dfsCount;
                        internalNode.hashCode[1] = i;
                        dfs(null, internalNode, null, visitor, seenNodes, internalNode.hashCode, i, 0);
                    } else {
                        dfs(null, internalNode, null, visitor, seenNodes, 0);
                    }
                }
            }

            dfsCount++;
        }
    }

    /**
     * Performs a depth first search on the internal hierarchy model
     *
     * @param parent         the parent internal node of the current internal node
     * @param root           the current internal node
     * @param connectingEdge the internal edge connecting the internal node and the parent
     *                       internal node, if any
     * @param visitor        the visitor pattern to be called for each node
     * @param seen           a set of all nodes seen by this dfs a set of all of the
     *                       ancestor node of the current node
     * @param layer          the layer on the dfs tree ( not the same as the model ranks )
     */
    public void dfs(GraphHierarchyNode parent, GraphHierarchyNode root, GraphHierarchyEdge connectingEdge, CellVisitor visitor, Set<GraphHierarchyNode> seen, int layer) {
        if (root != null) {
            if (!seen.contains(root)) {
                visitor.visit(parent, root, connectingEdge, layer, 0);
                seen.add(root);

                // Copy the connects as source list so that visitors
                // can change the original for edge direction inversions
                final Object[] outgoingEdges = root.connectsAsSource.toArray();

                for (int i = 0; i < outgoingEdges.length; i++) {
                    GraphHierarchyEdge internalEdge = (GraphHierarchyEdge) outgoingEdges[i];
                    GraphHierarchyNode targetNode = internalEdge.target;

                    // Root check is O(|roots|)
                    dfs(root, targetNode, internalEdge, visitor, seen, layer + 1);
                }
            } else {
                // Use the int field to indicate this node has been seen
                visitor.visit(parent, root, connectingEdge, layer, 1);
            }
        }
    }

    /**
     * Performs a depth first search on the internal hierarchy model. This dfs
     * extends the default version by keeping track of cells ancestors, but it
     * should be only used when necessary because of it can be computationally
     * intensive for deep searches.
     *
     * @param parent         the parent internal node of the current internal node
     * @param root           the current internal node
     * @param connectingEdge the internal edge connecting the internal node and the parent
     *                       internal node, if any
     * @param visitor        the visitor pattern to be called for each node
     * @param seen           a set of all nodes seen by this dfs
     * @param ancestors      the parent hash code
     * @param childHash      the new hash code for this node
     * @param layer          the layer on the dfs tree ( not the same as the model ranks )
     */
    public void dfs(GraphHierarchyNode parent, GraphHierarchyNode root, GraphHierarchyEdge connectingEdge, CellVisitor visitor, Set<GraphHierarchyNode> seen, int[] ancestors, int childHash, int layer) {
        // Explanation of custom hash set. Previously, the ancestors variable
        // was passed through the dfs as a HashSet. The ancestors were copied
        // into a new HashSet and when the new child was processed it was also
        // added to the set. If the current node was in its ancestor list it
        // meant there is a cycle in the graph and this information is passed
        // to the visitor.visit() in the seen parameter. The HashSet clone was
        // very expensive on CPU so a custom hash was developed using primitive
        // types. temp[] couldn't be used so hashCode[] was added to each node.
        // Each new child adds another int to the array, copying the prefix
        // from its parent. Child of the same parent add different ints (the
        // limit is therefore 2^32 children per parent...). If a node has a
        // child with the hashCode already set then the child code is compared
        // to the same portion of the current nodes array. If they match there
        // is a loop.
        // Note that the basic mechanism would only allow for 1 use of this
        // functionality, so the root nodes have two ints. The second int is
        // incremented through each node root and the first is incremented
        // through each run of the dfs algorithm (therefore the dfs is not
        // thread safe). The hash code of each node is set if not already set,
        // or if the first int does not match that of the current run.
        if (root != null) {
            if (parent != null) {
                // Form this nodes hash code if necessary, that is, if the
                // hashCode variable has not been initialized or if the
                // start of the parent hash code does not equal the start of
                // this nodes hash code, indicating the code was set on a
                // previous run of this dfs.
                if (root.hashCode == null || root.hashCode[0] != parent.hashCode[0]) {
                    int hashCodeLength = parent.hashCode.length + 1;
                    root.hashCode = new int[hashCodeLength];
                    System.arraycopy(parent.hashCode, 0, root.hashCode, 0, parent.hashCode.length);
                    root.hashCode[hashCodeLength - 1] = childHash;
                }
            }

            if (!seen.contains(root)) {
                visitor.visit(parent, root, connectingEdge, layer, 0);
                seen.add(root);
                // Copy the connects as source list so that visitors
                // can change the original for edge direction inversions
                final Object[] outgoingEdges = root.connectsAsSource.toArray();

                for (int i = 0; i < outgoingEdges.length; i++) {
                    GraphHierarchyEdge internalEdge = (GraphHierarchyEdge) outgoingEdges[i];
                    GraphHierarchyNode targetNode = internalEdge.target;

                    // Root check is O(|roots|)
                    dfs(root, targetNode, internalEdge, visitor, seen, root.hashCode, i, layer + 1);
                }
            } else {
                // Use the int field to indicate this node has been seen
                visitor.visit(parent, root, connectingEdge, layer, 1);
            }
        }
    }

    /**
     * @return Returns the vertexMapping.
     */
    public Map<ICell, GraphHierarchyNode> getVertexMapper() {
        if (vertexMapper == null) {
            vertexMapper = new HashMap<>();
        }
        return vertexMapper;
    }

    /**
     * @param vertexMapping The vertexMapping to set.
     */
    public void setVertexMapper(Map<ICell, GraphHierarchyNode> vertexMapping) {
        this.vertexMapper = vertexMapping;
    }

    /**
     * @return Returns the edgeMapper.
     */
    public Map<ICell, GraphHierarchyEdge> getEdgeMapper() {
        return edgeMapper;
    }

    /**
     * @param edgeMapper The edgeMapper to set.
     */
    public void setEdgeMapper(Map<ICell, GraphHierarchyEdge> edgeMapper) {
        this.edgeMapper = edgeMapper;
    }

    /**
     * @return Returns the dfsCount.
     */
    public int getDfsCount() {
        return dfsCount;
    }

    /**
     * @param dfsCount The dfsCount to set.
     */
    public void setDfsCount(int dfsCount) {
        this.dfsCount = dfsCount;
    }

    /**
     * Defines the interface that visitors use to perform operations upon the
     * graph information during depth first search (dfs) or other tree-traversal
     * strategies implemented by subclassers.
     */
    public interface CellVisitor {

        /**
         * The method within which the visitor will perform operations upon the
         * graph model
         *
         * @param parent         the parent cell the current cell
         * @param cell           the current cell visited
         * @param connectingEdge the edge that led the last cell visited to this cell
         * @param layer          the current layer of the tree
         * @param seen           an int indicating whether this cell has been seen
         *                       previously
         */
        void visit(GraphHierarchyNode parent, GraphHierarchyNode cell, GraphHierarchyEdge connectingEdge, int layer, int seen);
    }
}
