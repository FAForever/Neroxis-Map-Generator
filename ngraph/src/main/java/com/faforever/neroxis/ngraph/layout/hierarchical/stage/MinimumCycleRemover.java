/*
 * Copyright (c) 2005, David Benson
 *
 * All rights reserved.
 *
 * This file is licensed under the JGraph software license, a copy of which
 * will have been provided to you in the file LICENSE at the root of your
 * installation directory. If you are unable to locate this file please
 * contact JGraph sales for another copy.
 */
package com.faforever.neroxis.ngraph.layout.hierarchical.stage;

import com.faforever.neroxis.ngraph.layout.hierarchical.HierarchicalLayout;
import com.faforever.neroxis.ngraph.layout.hierarchical.model.GraphHierarchyModel;
import com.faforever.neroxis.ngraph.layout.hierarchical.model.GraphHierarchyNode;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.view.Graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * An implementation of the first stage of the Sugiyama layout. Straightforward
 * longest path calculation of layer assignment
 */
public class MinimumCycleRemover implements HierarchicalLayoutStage {
    /**
     * Reference to the enclosing layout algorithm
     */
    protected HierarchicalLayout layout;

    /**
     * Constructor that has the roots specified
     */
    public MinimumCycleRemover(HierarchicalLayout layout) {
        this.layout = layout;
    }

    /**
     * Produces the layer assignmment using the graph information specified
     */
    @Override
    public void execute(Object parent) {
        GraphHierarchyModel model = layout.getModel();
        final Set<GraphHierarchyNode> seenNodes = new HashSet<>();
        final Set<GraphHierarchyNode> unseenNodes = new HashSet<>(model.getVertexMapper().values());

        // Perform a dfs through the internal model. If a cycle is found,
        // reverse it.
        List<GraphHierarchyNode> rootsArray = null;

        if (model.roots != null) {
            List<ICell> modelRoots = model.roots;
            rootsArray = new ArrayList<>();

            for (ICell node : modelRoots) {
                GraphHierarchyNode internalNode = model.getVertexMapper().get(node);
                rootsArray.add(internalNode);
            }
        }

        model.visit((parent12, cell, connectingEdge, layer, seen) -> {
            // Check if the cell is in it's own ancestor list, if so
            // invert the connecting edge and reverse the target/source
            // relationship to that edge in the parent and the cell
            if ((cell).isAncestor(parent12)) {
                connectingEdge.invert();
                parent12.connectsAsSource.remove(connectingEdge);
                parent12.connectsAsTarget.add(connectingEdge);
                cell.connectsAsTarget.remove(connectingEdge);
                cell.connectsAsSource.add(connectingEdge);
            }
            seenNodes.add(cell);
            unseenNodes.remove(cell);
        }, rootsArray, true, null);

        Set<GraphHierarchyNode> possibleNewRoots = null;

        if (unseenNodes.size() > 0) {
            possibleNewRoots = new HashSet<>(unseenNodes);
        }

        // If there are any nodes that should be nodes that the dfs can miss
        // these need to be processed with the dfs and the roots assigned
        // correctly to form a correct internal model
        Set<GraphHierarchyNode> seenNodesCopy = new HashSet<>(seenNodes);

        // Pick a random cell and dfs from it

        model.visit((parent1, cell, connectingEdge, layer, seen) -> {
            // Check if the cell is in it's own ancestor list, if so
            // invert the connecting edge and reverse the target/source
            // relationship to that edge in the parent and the cell
            if ((cell).isAncestor(parent1)) {
                connectingEdge.invert();
                parent1.connectsAsSource.remove(connectingEdge);
                parent1.connectsAsTarget.add(connectingEdge);
                cell.connectsAsTarget.remove(connectingEdge);
                cell.connectsAsSource.add(connectingEdge);
            }
            seenNodes.add(cell);
            unseenNodes.remove(cell);
        }, List.copyOf(unseenNodes), true, seenNodesCopy);

        Graph graph = layout.getGraph();

        if (possibleNewRoots != null && possibleNewRoots.size() > 0) {
            Iterator<GraphHierarchyNode> iter = possibleNewRoots.iterator();
            List<ICell> roots = model.roots;

            while (iter.hasNext()) {
                GraphHierarchyNode node = iter.next();
                ICell realNode = node.cell;
                int numIncomingEdges = graph.getIncomingEdges(realNode).size();

                if (numIncomingEdges == 0) {
                    roots.add(realNode);
                }
            }
        }
    }
}
