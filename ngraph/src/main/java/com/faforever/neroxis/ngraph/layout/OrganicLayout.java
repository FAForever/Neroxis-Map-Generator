/**
 * Copyright (c) 2007-2013, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.layout;

import com.faforever.neroxis.ngraph.model.GraphModel;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.Graph;
import com.faforever.neroxis.ngraph.view.GraphView;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * An implementation of a simulated annealing layout, based on "Drawing Graphs
 * Nicely Using Simulated Annealing" by Davidson and Harel (1996). This
 * paper describes these criteria as being favourable in a graph layout: (1)
 * distributing nodes evenly, (2) making edge-lengths uniform, (3)
 * minimizing cross-crossings, and (4) keeping nodes from coming too close
 * to edges. These criteria are translated into energy cost functions in the
 * layout. Nodes or edges breaking these criteria create a larger cost function
 * , the total cost they contribute related to the extent that they break it.
 * The idea of the algorithm is to minimise the total system energy. Factors
 * are assigned to each of the criteria describing how important that
 * criteria is. Higher factors mean that those criteria are deemed to be
 * relatively preferable in the final layout. Most of  the criteria conflict
 * with the others to some extent and so the setting of the factors determines
 * the general look of the resulting graph.
 * <p>
 * In addition to the four aesthetic criteria the concept of a border line
 * which induces an energy cost to nodes in proximity to the graph bounds is
 * introduced to attempt to restrain the graph. All of the 5 factors can be
 * switched on or off using the <code>isOptimize...</code> variables.
 * <p>
 * Simulated Annealing is a force-directed layout and is one of the more
 * expensive, but generally effective layouts of this type. Layouts like
 * the spring layout only really factor in edge length and inter-node
 * distance being the lowest CPU intensive for the most aesthetic gain. The
 * additional factors are more expensive but can have very attractive results.
 * <p>
 * The main loop of the algorithm consist of processing the nodes in a
 * deterministic order. During the processing of each node a circle of radius
 * <code>moveRadius</code> is made around the node and split into
 * <code>triesPerCell</code> equal segments. Each point between neighbour
 * segments is determined and the new energy of the system if the node were
 * moved to that position calculated. Only the necessary nodes and edges are
 * processed new energy values resulting in quadratic performance, O(VE),
 * whereas calculating the total system energy would be cubic. The default
 * implementation only checks 8 points around the radius of the circle, as
 * opposed to the suggested 30 in the paper. Doubling the number of points
 * double the CPU load and 8 works almost as well as 30.
 * <p>
 * The <code>moveRadius</code> replaces the temperature as the influencing
 * factor in the way the graph settles in later iterations. If the user does
 * not set the initial move radius it is set to half the maximum dimension
 * of the graph. Thus, in 2 iterations a node may traverse the entire graph,
 * and it is more sensible to find minima this way that uphill moves, which
 * are little more than an expensive 'tilt' method. The factor by which
 * the radius is multiplied by after each iteration is important, lowering
 * it improves performance but raising it towards 1.0 can improve the
 * resulting graph aesthetics. When the radius hits the minimum move radius
 * defined, the layout terminates. The minimum move radius should be set
 * a value where the move distance is too minor to be of interest.
 * <p>
 * Also, the idea of a fine tuning phase is used, as described in the paper.
 * This involves only calculating the edge to node distance energy cost
 * at the end of the algorithm since it is an expensive calculation and
 * it really an 'optimizating' function. <code>fineTuningRadius</code>
 * defines the radius value that, when reached, causes the edge to node
 * distance to be calculated.
 * <p>
 * There are other special cases that are processed after each iteration.
 * <code>unchangedEnergyRoundTermination</code> defines the number of
 * iterations, after which the layout terminates. If nothing is being moved
 * it is assumed a good layout has been found. In addition to this if
 * no nodes are moved during an iteration the move radius is halved, presuming
 * that a finer granularity is required.
 */
@Getter
@Setter
public class OrganicLayout extends GraphLayout {
    /**
     * Whether or not the distance between edge and nodes will be calculated
     * as an energy cost function. This function is CPU intensive and is best
     * only used in the fine tuning phase.
     */
    protected boolean isOptimizeEdgeDistance = true;
    /**
     * Whether or not edges crosses will be calculated as an energy cost
     * function. This function is CPU intensive, though if some iterations
     * without it are required, it is best to have a few cycles at the start
     * of the algorithm using it, then use it intermittantly through the rest
     * of the layout.
     */
    protected boolean isOptimizeEdgeCrossing = true;
    /**
     * Whether or not edge lengths will be calculated as an energy cost
     * function. This function not CPU intensive.
     */
    protected boolean isOptimizeEdgeLength = true;
    /**
     * Whether or not nodes will contribute an energy cost as they approach
     * the bound of the graph. The cost increases to a limit close to the
     * border and stays constant outside the bounds of the graph. This function
     * is not CPU intensive
     */
    protected boolean isOptimizeBorderLine = true;
    /**
     * Whether or not node distribute will contribute an energy cost where
     * nodes are close together. The function is moderately CPU intensive.
     */
    protected boolean isOptimizeNodeDistribution = true;
    /**
     * when {@link #moveRadius}reaches this value, the algorithm is terminated
     */
    protected double minMoveRadius = 2.0;
    /**
     * The current radius around each node where the next position energy
     * values will be calculated for a possible move
     */
    protected double moveRadius;
    /**
     * The initial value of <code>moveRadius</code>. If this is set to zero
     * the layout will automatically determine a suitable value.
     */
    protected double initialMoveRadius = 0.0;
    /**
     * The factor by which the <code>moveRadius</code> is multiplied by after
     * every iteration. A value of 0.75 is a good balance between performance
     * and aesthetics. Increasing the value provides more chances to find
     * minimum energy positions and decreasing it causes the minimum radius
     * termination condition to occur more quickly.
     */
    protected double radiusScaleFactor = 0.75;
    /**
     * The average amount of area allocated per node. If <code> bounds</code>
     * is not set this value mutiplied by the number of nodes to find
     * the total graph area. The graph is assumed square.
     */
    protected double averageNodeArea = 160000;
    /**
     * The radius below which fine tuning of the layout should start
     * This involves allowing the distance between nodes and edges to be
     * taken into account in the total energy calculation. If this is set to
     * zero, the layout will automatically determine a suitable value
     */
    protected double fineTuningRadius = 40.0;
    /**
     * Limit to the number of iterations that may take place. This is only
     * reached if one of the termination conditions does not occur first.
     */
    protected int maxIterations = 1000;
    /**
     * Cost factor applied to energy calculations involving the distance
     * nodes and edges. Increasing this value tends to cause nodes to move away
     * from edges, at the partial cost of other graph aesthetics.
     * <code>isOptimizeEdgeDistance</code> must be true for edge to nodes
     * distances to be taken into account.
     */
    protected double edgeDistanceCostFactor = 3000;
    /**
     * Cost factor applied to energy calculations involving edges that cross
     * over one another. Increasing this value tends to result in fewer edge
     * crossings, at the partial cost of other graph aesthetics.
     * <code>isOptimizeEdgeCrossing</code> must be true for edge crossings
     * to be taken into account.
     */
    protected double edgeCrossingCostFactor = 6000;
    /**
     * Cost factor applied to energy calculations involving the general node
     * distribution of the graph. Increasing this value tends to result in
     * a better distribution of nodes across the available space, at the
     * partial cost of other graph aesthetics.
     * <code>isOptimizeNodeDistribution</code> must be true for this general
     * distribution to be applied.
     */
    protected double nodeDistributionCostFactor = 30000;
    /**
     * Cost factor applied to energy calculations for node promixity to the
     * notional border of the graph. Increasing this value results in
     * nodes tending towards the centre of the drawing space, at the
     * partial cost of other graph aesthetics.
     * <code>isOptimizeBorderLine</code> must be true for border
     * repulsion to be applied.
     */
    protected double borderLineCostFactor = 5;
    /**
     * Cost factor applied to energy calculations for the edge lengths.
     * Increasing this value results in the layout attempting to shorten all
     * edges to the minimum edge length, at the partial cost of other graph
     * aesthetics.
     * <code>isOptimizeEdgeLength</code> must be true for edge length
     * shortening to be applied.
     */
    protected double edgeLengthCostFactor = 0.02;
    /**
     * The x coordinate of the final graph
     */
    protected double boundsX = 0.0;
    /**
     * The y coordinate of the final graph
     */
    protected double boundsY = 0.0;
    /**
     * The width coordinate of the final graph
     */
    protected double boundsWidth = 0.0;
    /**
     * The height coordinate of the final graph
     */
    protected double boundsHeight = 0.0;
    /**
     * current iteration number of the layout
     */
    protected int iteration;
    /**
     * determines, in how many segments the circle around cells is divided, to
     * find a new position for the cell. Doubling this value doubles the CPU
     * load. Increasing it beyond 16 might mean a change to the
     * <code>performRound</code> method might further improve accuracy for a
     * small performance hit. The change is described in the method comment.
     */
    protected int triesPerCell = 8;
    /**
     * prevents from dividing with zero and from creating excessive energy
     * values
     */
    protected double minDistanceLimit = 2;
    /**
     * cached version of <code>minDistanceLimit</code> squared
     */
    protected double minDistanceLimitSquared;
    /**
     * distance limit beyond which energy costs due to object repulsive is
     * not calculated as it would be too insignificant
     */
    protected double maxDistanceLimit = 100;
    /**
     * cached version of <code>maxDistanceLimit</code> squared
     */
    protected double maxDistanceLimitSquared;
    /**
     * Keeps track of how many consecutive round have passed without any energy
     * changes
     */
    protected int unchangedEnergyRoundCount;
    /**
     * The number of round of no node moves taking placed that the layout
     * terminates
     */
    protected int unchangedEnergyRoundTermination = 5;
    /**
     * Whether or not to use approximate node dimensions or not. Set to true
     * the radius squared of the smaller dimension is used. Set to false the
     * radiusSquared variable of the CellWrapper contains the width squared
     * and heightSquared is used in the obvious manner.
     */
    protected boolean approxNodeDimensions = true;
    /**
     * Internal models collection of nodes ( vertices ) to be laid out
     */
    protected List<CellWrapper> vertices;
    /**
     * Internal models collection of edges to be laid out
     */
    protected List<CellWrapper> edges;
    /**
     * Array of the x portion of the normalised test vectors that
     * are tested for a lower energy around each vertex. The vector
     * of the combined x and y normals are multipled by the current
     * radius to obtain test points for each vector in the array.
     */
    protected double[] xNormTry;
    /**
     * Array of the y portion of the normalised test vectors that
     * are tested for a lower energy around each vertex. The vector
     * of the combined x and y normals are multipled by the current
     * radius to obtain test points for each vector in the array.
     */
    protected double[] yNormTry;
    /**
     * Whether or not fine tuning is on. The determines whether or not
     * node to edge distances are calculated in the total system energy.
     * This cost function , besides detecting line intersection, is a
     * performance intensive component of this algorithm and best left
     * to optimization phase. <code>isFineTuning</code> is switched to
     * <code>true</code> if and when the <code>fineTuningRadius</code>
     * radius is reached. Switching this variable to <code>true</code>
     * before the algorithm runs mean the node to edge cost function
     * is always calculated.
     */
    protected boolean isFineTuning = true;
    /**
     * Specifies if the STYLE_NOEDGESTYLE flag should be set on edges that are
     * modified by the result. Default is true.
     */
    protected boolean disableEdgeStyle = true;
    /**
     * Specifies if all edge points of traversed edges should be removed.
     * Default is true.
     */
    protected boolean resetEdges = false;

    /**
     * Constructor for OrganicLayout.
     */
    public OrganicLayout(Graph graph) {
        super(graph);
    }

    /**
     * Constructor for OrganicLayout.
     */
    public OrganicLayout(Graph graph, Rectangle2D bounds) {
        super(graph);
        boundsX = bounds.getX();
        boundsY = bounds.getY();
        boundsWidth = bounds.getWidth();
        boundsHeight = bounds.getHeight();
    }

    /**
     * Implements <GraphLayout.execute>.
     */
    @Override
    public void execute(ICell parent) {
        IGraphModel model = graph.getModel();
        GraphView view = graph.getView();
        List<ICell> vertices = graph.getChildVertices(parent);
        HashSet<ICell> vertexSet = new HashSet<>(vertices);

        Set<ICell> validEdges = new HashSet<>();

        // Remove edges that do not have both source and target terminals visible
        for (ICell vertex : vertices) {
            List<ICell> edges = GraphModel.getEdges(model, vertex, false, true, false);

            for (ICell edge : edges) {
                // Only deal with sources. To be valid in the layout, each edge must be attached
                // at both source and target to a vertex in the layout. Doing this avoids processing
                // each edge twice.
                if (view.getVisibleTerminal(edge, true) == vertex && vertexSet.contains(
                        view.getVisibleTerminal(edge, false))) {
                    validEdges.add(edge);
                }
            }
        }
        List<ICell> edges = List.copyOf(validEdges);
        // If the bounds dimensions have not been set see if the average area
        // per node has been
        RectangleDouble totalBounds = null;
        RectangleDouble bounds = null;
        // Form internal model of nodes
        Map<Object, Integer> vertexMap = new HashMap<>();
        this.vertices = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            this.vertices.add(new CellWrapper(vertices.get(i)));
            vertexMap.put(vertices.get(i), i);
            bounds = getVertexBounds(vertices.get(i));
            if (totalBounds == null) {
                totalBounds = bounds.clone();
            } else {
                totalBounds.add(bounds);
            }

            // Set the X,Y value of the internal version of the cell to
            // the center point of the vertex for better positioning
            double width = bounds.getWidth();
            double height = bounds.getHeight();
            this.vertices.get(i).x = bounds.getX() + width / 2.0;
            this.vertices.get(i).y = bounds.getY() + height / 2.0;
            if (approxNodeDimensions) {
                this.vertices.get(i).radiusSquared = Math.min(width, height);
                this.vertices.get(i).radiusSquared *= this.vertices.get(i).radiusSquared;
            } else {
                this.vertices.get(i).radiusSquared = width * width;
                this.vertices.get(i).heightSquared = height * height;
            }
        }

        if (averageNodeArea == 0.0) {
            if (boundsWidth == 0.0 && totalBounds != null) {
                // Just use current bounds of graph
                boundsX = totalBounds.getX();
                boundsY = totalBounds.getY();
                boundsWidth = totalBounds.getWidth();
                boundsHeight = totalBounds.getHeight();
            }
        } else {
            // find the center point of the current graph
            // based the new graph bounds on the average node area set
            double newArea = averageNodeArea * vertices.size();
            double squareLength = Math.sqrt(newArea);
            if (bounds != null) {
                double centreX = totalBounds.getX() + totalBounds.getWidth() / 2.0;
                double centreY = totalBounds.getY() + totalBounds.getHeight() / 2.0;
                boundsX = centreX - squareLength / 2.0;
                boundsY = centreY - squareLength / 2.0;
            } else {
                boundsX = 0;
                boundsY = 0;
            }
            boundsWidth = squareLength;
            boundsHeight = squareLength;
            // Ensure x and y are 0 or positive
            if (boundsX < 0.0 || boundsY < 0.0) {
                double maxNegativeAxis = Math.min(boundsX, boundsY);
                double axisOffset = -maxNegativeAxis;
                boundsX += axisOffset;
                boundsY += axisOffset;
            }
        }

        // If the initial move radius has not been set find a suitable value.
        // A good value is half the maximum dimension of the final graph area
        if (initialMoveRadius == 0.0) {
            initialMoveRadius = Math.max(boundsWidth, boundsHeight) / 2.0;
        }

        moveRadius = initialMoveRadius;

        minDistanceLimitSquared = minDistanceLimit * minDistanceLimit;
        maxDistanceLimitSquared = maxDistanceLimit * maxDistanceLimit;

        unchangedEnergyRoundCount = 0;

        // Form internal model of edges
        this.edges = new ArrayList<>();

        for (int i = 0; i < edges.size(); i++) {
            this.edges.add(new CellWrapper(edges.get(i)));

            ICell sourceCell = model.getTerminal(edges.get(i), true);
            ICell targetCell = model.getTerminal(edges.get(i), false);
            Integer source = null;
            Integer target = null;
            // Check if either end of the edge is not connected
            if (sourceCell != null) {
                source = vertexMap.get(sourceCell);
            }
            if (targetCell != null) {
                target = vertexMap.get(targetCell);
            }

            this.edges.get(i).source = source != null ? source : -1;
            this.edges.get(i).target = target != null ? target : -1;
        }

        // Set up internal nodes with information about whether edges
        // are connected to them or not
        for (int i = 0; i < this.vertices.size(); i++) {
            this.vertices.get(i).relevantEdges = getRelevantEdges(i);
            this.vertices.get(i).connectedEdges = getConnectedEdges(i);
        }

        // Setup the normal vectors for the test points to move each vertex to
        xNormTry = new double[triesPerCell];
        yNormTry = new double[triesPerCell];

        for (int i = 0; i < triesPerCell; i++) {
            double angle = i * ((2.0 * Math.PI) / triesPerCell);
            xNormTry[i] = Math.cos(angle);
            yNormTry[i] = Math.sin(angle);
        }

        int childCount = model.getChildCount(parent);

        for (int i = 0; i < childCount; i++) {
            ICell cell = model.getChildAt(parent, i);

            if (!isEdgeIgnored(cell)) {
                if (isResetEdges()) {
                    graph.resetEdge(cell);
                }

                if (isDisableEdgeStyle()) {
                    setEdgeStyleEnabled(cell, false);
                }
            }
        }

        // The main layout loop
        for (iteration = 0; iteration < maxIterations; iteration++) {
            performRound();
        }

        // Obtain the final positions
        double[][] result = new double[this.vertices.size()][2];
        vertices.clear();
        for (int i = 0; i < this.vertices.size(); i++) {
            vertices.add(this.vertices.get(i).cell);
            bounds = getVertexBounds(vertices.get(i));

            result[i][0] = this.vertices.get(i).x - bounds.getWidth() / 2;
            result[i][1] = this.vertices.get(i).y - bounds.getHeight() / 2;
        }

        model.beginUpdate();
        try {
            for (int i = 0; i < vertices.size(); i++) {
                setVertexLocation(vertices.get(i), result[i][0], result[i][1]);
            }
        } finally {
            model.endUpdate();
        }
    }

    /**
     * Returns true if the given vertex has no connected edges.
     *
     * @param vertex Object that represents the vertex to be tested.
     * @return Returns true if the vertex should be ignored.
     */
    @Override
    public boolean isVertexIgnored(ICell vertex) {
        return false;
    }

    /**
     * The main round of the algorithm. Firstly, a permutation of nodes
     * is created and worked through in that random order. Then, for each node
     * a number of point of a circle of radius <code>moveRadius</code> are
     * selected and the total energy of the system calculated if that node
     * were moved to that new position. If a lower energy position is found
     * this is accepted and the algorithm moves onto the next node. There
     * may be a slightly lower energy value yet to be found, but forcing
     * the loop to check all possible positions adds nearly the current
     * processing time again, and for little benefit. Another possible
     * strategy would be to take account of the fact that the energy values
     * around the circle decrease for half the loop and increase for the
     * other, as a general rule. If part of the decrease were seen, then
     * when the energy of a node increased, the previous node position was
     * almost always the lowest energy position. This adds about two loop
     * iterations to the inner loop and only makes sense with 16 tries or more.
     */
    protected void performRound() {
        // sequential order cells are computed (every round the same order)

        // boolean to keep track of whether any moves were made in this round
        boolean energyHasChanged = false;
        for (int i = 0; i < vertices.size(); i++) {

            // Obtain the energies for the node is its current position
            // TODO The energy could be stored from the last iteration
            // and used again, rather than re-calculate
            double oldNodeDistribution = getNodeDistribution(i);
            double oldEdgeDistance = getEdgeDistanceFromNode(i);
            oldEdgeDistance += getEdgeDistanceAffectedNodes(i);
            double oldEdgeCrossing = getEdgeCrossingAffectedEdges(i);
            double oldBorderLine = getBorderline(i);
            double oldEdgeLength = getEdgeLengthAffectedEdges(i);
            double oldAdditionFactors = getAdditionFactorsEnergy(i);

            for (int j = 0; j < triesPerCell; j++) {
                double movex = moveRadius * xNormTry[j];
                double movey = moveRadius * yNormTry[j];

                // applying new move
                double oldx = vertices.get(i).x;
                double oldy = vertices.get(i).y;
                vertices.get(i).x = vertices.get(i).x + movex;
                vertices.get(i).y = vertices.get(i).y + movey;

                // calculate the energy delta from this move
                double energyDelta = calcEnergyDelta(i, oldNodeDistribution, oldEdgeDistance, oldEdgeCrossing,
                                                     oldBorderLine, oldEdgeLength, oldAdditionFactors);

                if (energyDelta < 0) {
                    // energy of moved node is lower, finish tries for this
                    // node
                    energyHasChanged = true;
                    break; // exits loop
                } else {
                    // Revert node coordinates
                    vertices.get(i).x = oldx;
                    vertices.get(i).y = oldy;
                }
            }
        }
        // Check if we've hit the limit number of unchanged rounds that cause
        // a termination condition
        if (energyHasChanged) {
            unchangedEnergyRoundCount = 0;
        } else {
            unchangedEnergyRoundCount++;
            // Half the move radius in case assuming it's set too high for
            // what might be an optimisation case
            moveRadius /= 2.0;
        }
        if (unchangedEnergyRoundCount >= unchangedEnergyRoundTermination) {
            iteration = maxIterations;
        }

        // decrement radius in controlled manner
        double newMoveRadius = moveRadius * radiusScaleFactor;
        // Don't waste time on tiny decrements, if the final pixel resolution
        // is 50 then there's no point doing 55,54.1, 53.2 etc
        if (moveRadius - newMoveRadius < minMoveRadius) {
            newMoveRadius = moveRadius - minMoveRadius;
        }
        // If the temperature reaches its minimum temperature then finish
        if (newMoveRadius <= minMoveRadius) {
            iteration = maxIterations;
        }
        // Switch on fine tuning below the specified temperature
        if (newMoveRadius < fineTuningRadius) {
            isFineTuning = true;
        }

        moveRadius = newMoveRadius;
    }

    /**
     * Calculates the change in energy for the specified node. The new energy is
     * calculated from the cost function methods and the old energy values for
     * each cost function are passed in as parameters
     *
     * @param index                      The index of the node in the <code>vertices</code> array
     * @param oldNodeDistribution        The previous node distribution energy cost of this node
     * @param oldEdgeDistance            The previous edge distance energy cost of this node
     * @param oldEdgeCrossing            The previous edge crossing energy cost for edges connected to
     *                                   this node
     * @param oldBorderLine              The previous border line energy cost for this node
     * @param oldEdgeLength              The previous edge length energy cost for edges connected to
     *                                   this node
     * @param oldAdditionalFactorsEnergy The previous energy cost for additional factors from
     *                                   sub-classes
     * @return the delta of the new energy cost to the old energy cost
     */
    protected double calcEnergyDelta(int index, double oldNodeDistribution, double oldEdgeDistance,
                                     double oldEdgeCrossing, double oldBorderLine, double oldEdgeLength,
                                     double oldAdditionalFactorsEnergy) {
        double energyDelta = 0.0;
        energyDelta += getNodeDistribution(index) * 2.0;
        energyDelta -= oldNodeDistribution * 2.0;

        energyDelta += getBorderline(index);
        energyDelta -= oldBorderLine;

        energyDelta += getEdgeDistanceFromNode(index);
        energyDelta += getEdgeDistanceAffectedNodes(index);
        energyDelta -= oldEdgeDistance;

        energyDelta -= oldEdgeLength;
        energyDelta += getEdgeLengthAffectedEdges(index);

        energyDelta -= oldEdgeCrossing;
        energyDelta += getEdgeCrossingAffectedEdges(index);

        energyDelta -= oldAdditionalFactorsEnergy;
        energyDelta += getAdditionFactorsEnergy(index);

        return energyDelta;
    }

    /**
     * Calculates the energy cost of the specified node relative to all other
     * nodes. Basically produces a higher energy the closer nodes are together.
     *
     * @param i the index of the node in the array <code>v</code>
     * @return the total node distribution energy of the specified node
     */
    protected double getNodeDistribution(int i) {
        double energy = 0.0;

        // This check is placed outside of the inner loop for speed, even
        // though the code then has to be duplicated
        if (isOptimizeNodeDistribution) {
            if (approxNodeDimensions) {
                for (int j = 0; j < vertices.size(); j++) {
                    if (i != j) {
                        double vx = vertices.get(i).x - vertices.get(j).x;
                        double vy = vertices.get(i).y - vertices.get(j).y;
                        double distanceSquared = vx * vx + vy * vy;
                        distanceSquared -= vertices.get(i).radiusSquared;
                        distanceSquared -= vertices.get(j).radiusSquared;

                        // prevents from dividing with Zero.
                        if (distanceSquared < minDistanceLimitSquared) {
                            distanceSquared = minDistanceLimitSquared;
                        }

                        energy += nodeDistributionCostFactor / distanceSquared;
                    }
                }
            } else {
                for (int j = 0; j < vertices.size(); j++) {
                    if (i != j) {
                        double vx = vertices.get(i).x - vertices.get(j).x;
                        double vy = vertices.get(i).y - vertices.get(j).y;
                        double distanceSquared = vx * vx + vy * vy;
                        distanceSquared -= vertices.get(i).radiusSquared;
                        distanceSquared -= vertices.get(j).radiusSquared;
                        // If the height separation indicates overlap, subtract
                        // the widths from the distance. Same for width overlap
                        // TODO						if ()

                        // prevents from dividing with Zero.
                        if (distanceSquared < minDistanceLimitSquared) {
                            distanceSquared = minDistanceLimitSquared;
                        }

                        energy += nodeDistributionCostFactor / distanceSquared;
                    }
                }
            }
        }
        return energy;
    }

    /**
     * This method calculates the energy of the distance of the specified
     * node to the notional border of the graph. The energy increases up to
     * a limited maximum close to the border and stays at that maximum
     * up to and over the border.
     *
     * @param i the index of the node in the array <code>v</code>
     * @return the total border line energy of the specified node
     */
    protected double getBorderline(int i) {
        double energy = 0.0;
        if (isOptimizeBorderLine) {
            // Avoid very small distances and convert negative distance (i.e
            // outside the border to small positive ones )
            double l = vertices.get(i).x - boundsX;
            if (l < minDistanceLimit) {
                l = minDistanceLimit;
            }
            double t = vertices.get(i).y - boundsY;
            if (t < minDistanceLimit) {
                t = minDistanceLimit;
            }
            double r = boundsX + boundsWidth - vertices.get(i).x;
            if (r < minDistanceLimit) {
                r = minDistanceLimit;
            }
            double b = boundsY + boundsHeight - vertices.get(i).y;
            if (b < minDistanceLimit) {
                b = minDistanceLimit;
            }
            energy += borderLineCostFactor * ((1000000.0 / (t * t)) + (1000000.0 / (l * l)) + (1000000.0 / (b * b)) + (
                    1000000.0
                    / (r * r)));
        }
        return energy;
    }

    /**
     * Obtains the energy cost function for the specified node being moved.
     * This involves calling <code>getEdgeLength</code> for all
     * edges connected to the specified node
     *
     * @param node the node whose connected edges cost functions are to be
     *             calculated
     * @return the total edge length energy of the connected edges
     */
    protected double getEdgeLengthAffectedEdges(int node) {
        double energy = 0.0;
        for (int i = 0; i < vertices.get(node).connectedEdges.length; i++) {
            energy += getEdgeLength(vertices.get(node).connectedEdges[i]);
        }
        return energy;
    }

    /**
     * This method calculates the energy of the distance between Cells and
     * Edges. This version of the edge distance cost calculates the energy
     * cost from a specified <strong>node</strong>. The distance cost to all
     * unconnected edges is calculated and the total returned.
     *
     * @param i the index of the node in the array <code>v</code>
     * @return the total edge distance energy of the node
     */
    protected double getEdgeDistanceFromNode(int i) {
        double energy = 0.0;
        // This function is only performed during fine tuning for performance
        if (isOptimizeEdgeDistance && isFineTuning) {
            int[] edges = vertices.get(i).relevantEdges;
            for (int edge : edges) {
                // Note that the distance value is squared
                double distSquare = Line2D.ptSegDistSq(vertices.get(this.edges.get(edge).source).x,
                                                       vertices.get(this.edges.get(edge).source).y,
                                                       vertices.get(this.edges.get(edge).target).x,
                                                       vertices.get(this.edges.get(edge).target).y, vertices.get(i).x,
                                                       vertices.get(i).y);

                distSquare -= vertices.get(i).radiusSquared;

                // prevents from dividing with Zero. No Math.abs() call
                // for performance
                if (distSquare < minDistanceLimitSquared) {
                    distSquare = minDistanceLimitSquared;
                }

                // Only bother with the divide if the node and edge are
                // fairly close together
                if (distSquare < maxDistanceLimitSquared) {
                    energy += edgeDistanceCostFactor / distSquare;
                }
            }
        }
        return energy;
    }

    /**
     * Obtains the energy cost function for the specified node being moved.
     * This involves calling <code>getEdgeCrossing</code> for all
     * edges connected to the specified node
     *
     * @param node the node whose connected edges cost functions are to be
     *             calculated
     * @return the total edge crossing energy of the connected edges
     */
    protected double getEdgeCrossingAffectedEdges(int node) {
        double energy = 0.0;
        for (int i = 0; i < vertices.get(node).connectedEdges.length; i++) {
            energy += getEdgeCrossing(vertices.get(node).connectedEdges[i]);
        }

        return energy;
    }

    /**
     * This method calculates the energy due to the length of the specified
     * edge. The energy is proportional to the length of the edge, making
     * shorter edges preferable in the layout.
     *
     * @param i the index of the edge in the array <code>e</code>
     * @return the total edge length energy of the specified edge
     */
    protected double getEdgeLength(int i) {
        if (isOptimizeEdgeLength) {
            double edgeLength = Point2D.distance(vertices.get(edges.get(i).source).x,
                                                 vertices.get(edges.get(i).source).y,
                                                 vertices.get(edges.get(i).target).x,
                                                 vertices.get(edges.get(i).target).y);
            return (edgeLengthCostFactor * edgeLength * edgeLength);
        } else {
            return 0.0;
        }
    }

    /**
     * This method calculates the energy of the distance from the specified
     * edge crossing any other edges. Each crossing add a constant factor
     * to the total energy
     *
     * @param i the index of the edge in the array <code>e</code>
     * @return the total edge crossing energy of the specified edge
     */
    protected double getEdgeCrossing(int i) {
        // TODO Could have a cost function per edge
        int n = 0; // counts energy of edgecrossings through edge i

        // max and min variable for minimum bounding rectangles overlapping
        // checks
        double minjX, minjY, miniX, miniY, maxjX, maxjY, maxiX, maxiY;

        if (isOptimizeEdgeCrossing) {
            double iP1X = vertices.get(edges.get(i).source).x;
            double iP1Y = vertices.get(edges.get(i).source).y;
            double iP2X = vertices.get(edges.get(i).target).x;
            double iP2Y = vertices.get(edges.get(i).target).y;

            for (int j = 0; j < edges.size(); j++) {
                double jP1X = vertices.get(edges.get(j).source).x;
                double jP1Y = vertices.get(edges.get(j).source).y;
                double jP2X = vertices.get(edges.get(j).target).x;
                double jP2Y = vertices.get(edges.get(j).target).y;
                if (j != i) {
                    // First check is to see if the minimum bounding rectangles
                    // of the edges overlap at all. Since the layout tries
                    // to separate nodes and shorten edges, the majority do not
                    // overlap and this is a cheap way to avoid most of the
                    // processing
                    // Some long code to avoid a Math.max call...
                    if (checkBoundingPoints(iP1X, iP2X, jP1X, jP2X)) {
                        continue;
                    }

                    if (checkBoundingPoints(iP1Y, iP2Y, jP1Y, jP2Y)) {
                        continue;
                    }

                    // Ignore if any end points are coincident
                    if (((iP1X != jP1X) && (iP1Y != jP1Y)) && ((iP1X != jP2X) && (iP1Y != jP2Y)) && ((iP2X != jP1X) && (
                            iP2Y
                            != jP1Y)) && ((iP2X != jP2X) && (iP2Y != jP2Y))) {
                        // Values of zero returned from Line2D.relativeCCW are
                        // ignored because the point being exactly on the line
                        // is very rare for double and we've already checked if
                        // any end point share the same vertex. Should zero
                        // ever be returned, it would be the vertex connected
                        // to the edge that's actually on the edge and this is
                        // dealt with by the node to edge distance cost
                        // function. The worst case is that the vertex is
                        // pushed off the edge faster than it would be
                        // otherwise. Because of ignoring the zero this code
                        // below can behave like only a 1 or -1 will be
                        // returned. See Lines2D.linesIntersects().
                        boolean intersects = ((Line2D.relativeCCW(iP1X, iP1Y, iP2X, iP2Y, jP1X, jP1Y)
                                               != Line2D.relativeCCW(iP1X, iP1Y, iP2X, iP2Y, jP2X, jP2Y))
                                              && (Line2D.relativeCCW(jP1X, jP1Y, jP2X, jP2Y, iP1X, iP1Y)
                                                  != Line2D.relativeCCW(jP1X, jP1Y, jP2X, jP2Y, iP2X, iP2Y)));

                        if (intersects) {
                            n++;
                        }
                    }
                }
            }
        }
        return edgeCrossingCostFactor * n;
    }

    private boolean checkBoundingPoints(double i1, double i2, double j1, double j2) {
        double mini = Math.min(i1, i2);
        double maxi = Math.max(i1, i2);
        double minj = Math.min(j1, j2);
        double maxj = Math.max(j1, j2);
        return maxi < minj || mini > maxj;
    }

    /**
     * Obtains the energy cost function for the specified node being moved.
     * This involves calling <code>getEdgeDistanceFromEdge</code> for all
     * edges connected to the specified node
     *
     * @param node the node whose connected edges cost functions are to be
     *             calculated
     * @return the total edge distance energy of the connected edges
     */
    protected double getEdgeDistanceAffectedNodes(int node) {
        double energy = 0.0;
        for (int i = 0; i < (vertices.get(node).connectedEdges.length); i++) {
            energy += getEdgeDistanceFromEdge(vertices.get(node).connectedEdges[i]);
        }

        return energy;
    }

    /**
     * This method calculates the energy of the distance between Cells and
     * Edges. This version of the edge distance cost calculates the energy
     * cost from a specified <strong>edge</strong>. The distance cost to all
     * unconnected nodes is calculated and the total returned.
     *
     * @param i the index of the edge in the array <code>e</code>
     * @return the total edge distance energy of the edge
     */
    protected double getEdgeDistanceFromEdge(int i) {
        double energy = 0.0;
        // This function is only performed during fine tuning for performance
        if (isOptimizeEdgeDistance && isFineTuning) {
            for (int j = 0; j < vertices.size(); j++) {
                // Don't calculate for connected nodes
                if (edges.get(i).source != j && edges.get(i).target != j) {
                    double distSquare = Line2D.ptSegDistSq(vertices.get(edges.get(i).source).x,
                                                           vertices.get(edges.get(i).source).y,
                                                           vertices.get(edges.get(i).target).x,
                                                           vertices.get(edges.get(i).target).y, vertices.get(j).x,
                                                           vertices.get(j).y);

                    distSquare -= vertices.get(j).radiusSquared;

                    // prevents from dividing with Zero. No Math.abs() call
                    // for performance
                    if (distSquare < minDistanceLimitSquared) {
                        distSquare = minDistanceLimitSquared;
                    }

                    // Only bother with the divide if the node and edge are
                    // fairly close together
                    if (distSquare < maxDistanceLimitSquared) {
                        energy += edgeDistanceCostFactor / distSquare;
                    }
                }
            }
        }
        return energy;
    }

    /**
     * Hook method to adding additional energy factors into the layout.
     * Calculates the energy just for the specified node.
     *
     * @param i the nodes whose energy is being calculated
     * @return the energy of this node caused by the additional factors
     */
    protected double getAdditionFactorsEnergy(int i) {
        return 0.0;
    }

    /**
     * Returns all Edges that are not connected to the specified cell
     *
     * @param cellIndex the cell index to which the edges are not connected
     * @return Array of all interesting Edges
     */
    protected int[] getRelevantEdges(int cellIndex) {
        ArrayList<Integer> relevantEdgeList = new ArrayList<>(edges.size());

        for (int i = 0; i < edges.size(); i++) {
            if (edges.get(i).source != cellIndex && edges.get(i).target != cellIndex) {
                // Add non-connected edges
                relevantEdgeList.add(i);
            }
        }

        int[] relevantEdgeArray = new int[relevantEdgeList.size()];
        Iterator<Integer> iter = relevantEdgeList.iterator();

        //Reform the list into an array but replace Integer values with ints
        for (int i = 0; i < relevantEdgeArray.length; i++) {
            if (iter.hasNext()) {
                relevantEdgeArray[i] = iter.next();
            }
        }

        return relevantEdgeArray;
    }

    /**
     * Returns all Edges that are connected with the specified cell
     *
     * @param cellIndex the cell index to which the edges are connected
     * @return Array of all connected Edges
     */
    protected int[] getConnectedEdges(int cellIndex) {
        ArrayList<Integer> connectedEdgeList = new ArrayList<Integer>(edges.size());

        for (int i = 0; i < edges.size(); i++) {
            if (edges.get(i).source == cellIndex || edges.get(i).target == cellIndex) {
                // Add connected edges to list by their index number
                connectedEdgeList.add(i);
            }
        }

        int[] connectedEdgeArray = new int[connectedEdgeList.size()];
        Iterator<Integer> iter = connectedEdgeList.iterator();

        // Reform the list into an array but replace Integer values with ints
        for (int i = 0; i < connectedEdgeArray.length; i++) {
            if (iter.hasNext()) {
                connectedEdgeArray[i] = iter.next();
            }
        }

        return connectedEdgeArray;
    }

    /**
     * Returns <code>Organic</code>, the name of this algorithm.
     */
    public String toString() {
        return "Organic";
    }

    /**
     * Internal representation of a node or edge that holds cached information
     * to enable the layout to perform more quickly and to simplify the code
     */
    @Getter
    @Setter
    public static class CellWrapper {
        /**
         * The actual graph cell this wrapper represents
         */
        protected ICell cell;
        /**
         * All edge that repel this cell, only used for nodes. This array
         * is equivalent to all edges unconnected to this node
         */
        protected int[] relevantEdges = null;
        /**
         * the index of all connected edges in the <code>e</code> array
         * to this node. This is only used for nodes.
         */
        protected int[] connectedEdges = null;
        /**
         * The x-coordinate position of this cell, nodes only
         */
        protected double x;
        /**
         * The y-coordinate position of this cell, nodes only
         */
        protected double y;
        /**
         * The approximate radius squared of this cell, nodes only. If
         * approxNodeDimensions is true on the layout this value holds the
         * width of the node squared
         */
        protected double radiusSquared;
        /**
         * The height of the node squared, only used if approxNodeDimensions
         * is set to true.
         */
        protected double heightSquared;
        /**
         * The index of the node attached to this edge as source, edges only
         */
        protected int source;
        /**
         * The index of the node attached to this edge as target, edges only
         */
        protected int target;

        /**
         * Constructs a new CellWrapper
         *
         * @param cell the graph cell this wrapper represents
         */
        public CellWrapper(ICell cell) {
            this.cell = cell;
        }
    }
}
