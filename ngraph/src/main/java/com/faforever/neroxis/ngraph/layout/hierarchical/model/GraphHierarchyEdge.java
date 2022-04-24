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
package com.faforever.neroxis.ngraph.layout.hierarchical.model;

import com.faforever.neroxis.ngraph.model.ICell;
import java.util.ArrayList;
import java.util.List;

/**
 * An abstraction of a hierarchical edge for the hierarchy layout
 */
public class GraphHierarchyEdge extends GraphAbstractHierarchyCell {

    /**
     * The graph edge(s) this object represents. Parallel edges are all grouped
     * together within one hierarchy edge.
     */
    public List<ICell> edges;
    /**
     * The node this edge is sourced at
     */
    public GraphHierarchyNode source;
    /**
     * The node this edge targets
     */
    public GraphHierarchyNode target;
    /**
     * Whether or not the direction of this edge has been reversed
     * internally to create a DAG for the hierarchical layout
     */
    protected boolean isReversed = false;

    /**
     * Constructs a hierarchy edge
     *
     * @param edges a list of real graph edges this abstraction represents
     */
    public GraphHierarchyEdge(List<ICell> edges) {
        this.edges = edges;
    }

    /**
     * Inverts the direction of this internal edge(s)
     */
    public void invert() {
        GraphHierarchyNode temp = source;
        source = target;
        target = temp;
        isReversed = !isReversed;
    }

    /**
     * @return Returns the isReversed.
     */
    public boolean isReversed() {
        return isReversed;
    }

    /**
     * @param isReversed The isReversed to set.
     */
    public void setReversed(boolean isReversed) {
        this.isReversed = isReversed;
    }

    /**
     * Returns the cells this cell connects to on the next layer up
     *
     * @param layer the layer this cell is on
     * @return the cells this cell connects to on the next layer up
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<GraphAbstractHierarchyCell> getNextLayerConnectedCells(int layer) {
        if (nextLayerConnectedCells == null) {
            nextLayerConnectedCells = new ArrayList[temp.length];

            for (int i = 0; i < nextLayerConnectedCells.length; i++) {
                nextLayerConnectedCells[i] = new ArrayList<GraphAbstractHierarchyCell>(2);

                if (i == nextLayerConnectedCells.length - 1) {
                    nextLayerConnectedCells[i].add(source);
                } else {
                    nextLayerConnectedCells[i].add(this);
                }
            }
        }

        return nextLayerConnectedCells[layer - minRank - 1];
    }

    /**
     * Returns the cells this cell connects to on the next layer down
     *
     * @param layer the layer this cell is on
     * @return the cells this cell connects to on the next layer down
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<GraphAbstractHierarchyCell> getPreviousLayerConnectedCells(int layer) {
        if (previousLayerConnectedCells == null) {
            previousLayerConnectedCells = new ArrayList[temp.length];

            for (int i = 0; i < previousLayerConnectedCells.length; i++) {
                previousLayerConnectedCells[i] = new ArrayList<GraphAbstractHierarchyCell>(2);

                if (i == 0) {
                    previousLayerConnectedCells[i].add(target);
                } else {
                    previousLayerConnectedCells[i].add(this);
                }
            }
        }

        return previousLayerConnectedCells[layer - minRank - 1];
    }

    /**
     * Gets the value of temp for the specified layer
     *
     * @param layer the layer relating to a specific entry into temp
     * @return the value for that layer
     */
    @Override
    public int getGeneralPurposeVariable(int layer) {
        return temp[layer - minRank - 1];
    }

    /**
     * Set the value of temp for the specified layer
     *
     * @param layer the layer relating to a specific entry into temp
     * @param value the value for that layer
     */
    @Override
    public void setGeneralPurposeVariable(int layer, int value) {
        temp[layer - minRank - 1] = value;
    }

    /**
     * @return whether or not this cell is a node
     */
    @Override
    public boolean isVertex() {
        return false;
    }

    /**
     * @return whether or not this cell is an edge
     */
    @Override
    public boolean isEdge() {
        return true;
    }
}
