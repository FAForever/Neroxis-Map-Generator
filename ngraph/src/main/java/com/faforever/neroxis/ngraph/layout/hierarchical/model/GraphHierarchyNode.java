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
import java.util.Collection;
import java.util.List;

/**
 * An abstraction of an internal node in the hierarchy layout
 */
public class GraphHierarchyNode extends GraphAbstractHierarchyCell {

    /**
     * Shared empty connection map to return instead of null in applyMap.
     */
    public static Collection<GraphHierarchyEdge> emptyConnectionMap = List.of();
    /**
     * The graph cell this object represents.
     */
    public ICell cell;
    /**
     * Collection of hierarchy edges that have this node as a target
     */
    public Collection<GraphHierarchyEdge> connectsAsTarget = emptyConnectionMap;
    /**
     * Collection of hierarchy edges that have this node as a source
     */
    public Collection<GraphHierarchyEdge> connectsAsSource = emptyConnectionMap;
    /**
     * Assigns a unique hashcode for each node. Used by the model dfs instead
     * of copying HashSets
     */
    public int[] hashCode;

    /**
     * Constructs an internal node to represent the specified real graph cell
     *
     * @param cell the real graph cell this node represents
     */
    public GraphHierarchyNode(ICell cell) {
        this.cell = cell;
    }

    /**
     * Returns the integer value of the layer that this node resides in
     *
     * @return the integer value of the layer that this node resides in
     */
    public int getRankValue() {
        return maxRank;
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
            nextLayerConnectedCells = new ArrayList[1];
            nextLayerConnectedCells[0] = new ArrayList<>(connectsAsTarget.size());

            for (GraphHierarchyEdge edge : connectsAsTarget) {
                if (edge.maxRank == -1 || edge.maxRank == layer + 1) {
                    // Either edge is not in any rank or
                    // no dummy nodes in edge, add node of other side of edge
                    nextLayerConnectedCells[0].add(edge.source);
                } else {
                    // Edge spans at least two layers, add edge
                    nextLayerConnectedCells[0].add(edge);
                }
            }
        }

        return nextLayerConnectedCells[0];
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
            previousLayerConnectedCells = new ArrayList[1];
            previousLayerConnectedCells[0] = new ArrayList<>(connectsAsSource.size());

            for (GraphHierarchyEdge edge : connectsAsSource) {
                if (edge.minRank == -1 || edge.minRank == layer - 1) {
                    // No dummy nodes in edge, add node of other side of edge
                    previousLayerConnectedCells[0].add(edge.target);
                } else {
                    // Edge spans at least two layers, add edge
                    previousLayerConnectedCells[0].add(edge);
                }
            }
        }

        return previousLayerConnectedCells[0];
    }

    /**
     * Gets the value of temp for the specified layer
     *
     * @param layer the layer relating to a specific entry into temp
     * @return the value for that layer
     */
    @Override
    public int getGeneralPurposeVariable(int layer) {
        return temp[0];
    }

    /**
     * Set the value of temp for the specified layer
     *
     * @param layer the layer relating to a specific entry into temp
     * @param value the value for that layer
     */
    @Override
    public void setGeneralPurposeVariable(int layer, int value) {
        temp[0] = value;
    }

    /**
     * @return whether or not this cell is a node
     */
    @Override
    public boolean isVertex() {
        return true;
    }

    /**
     * @return whether or not this cell is an edge
     */
    @Override
    public boolean isEdge() {
        return false;
    }

    public boolean isAncestor(GraphHierarchyNode otherNode) {
        // Firstly, the hash code of this node needs to be shorter than the
        // other node
        if (otherNode != null
            && hashCode != null
            && otherNode.hashCode != null
            && hashCode.length < otherNode.hashCode.length) {

            // Secondly, this hash code must match the start of the other
            // node's hash code. Arrays.equals cannot be used here since
            // the arrays are different length, and we do not want to
            // perform another array copy.
            for (int i = 0; i < hashCode.length; i++) {
                if (hashCode[i] != otherNode.hashCode[i]) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }
}
