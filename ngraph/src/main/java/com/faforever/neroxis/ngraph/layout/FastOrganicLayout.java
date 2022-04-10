/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.layout;

import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.Graph;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

/**
 * Fast organic layout algorithm.
 */
@Getter
@Setter
public class FastOrganicLayout extends GraphLayout {
    /**
     * Specifies if the top left corner of the input cells should be the origin
     * of the layout result. Default is true.
     */
    protected boolean useInputOrigin = true;

    /**
     * Specifies if all edge points of traversed edges should be removed.
     * Default is true.
     */
    protected boolean resetEdges = true;

    /**
     * Specifies if the STYLE_NOEDGESTYLE flag should be set on edges that are
     * modified by the result. Default is true.
     */
    protected boolean disableEdgeStyle = true;

    /**
     * The force constant by which the attractive forces are divided and the
     * replusive forces are multiple by the square of. The value equates to the
     * average radius there is of free space around each node. Default is 50.
     */
    protected double forceConstant = 50;

    /**
     * Cache of <forceConstant>^2 for performance.
     */
    protected double forceConstantSquared = 0;

    /**
     * Minimal distance limit. Default is 2. Prevents of
     * dividing by zero.
     */
    protected double minDistanceLimit = 2;

    /**
     * Cached version of <minDistanceLimit> squared.
     */
    protected double minDistanceLimitSquared = 0;

    /**
     * The maximum distance between vertices, beyond which their
     * repulsion no longer has an effect
     */
    protected double maxDistanceLimit = 500;

    /**
     * Start value of temperature. Default is 200.
     */
    protected double initialTemp = 200;

    /**
     * Temperature to limit displacement at later stages of layout.
     */
    protected double temperature = 0;

    /**
     * Total number of iterations to run the layout though.
     */
    protected double maxIterations = 0;

    /**
     * Current iteration count.
     */
    protected double iteration = 0;

    /**
     * An array of all vertices to be laid out.
     */
    protected List<ICell> vertexArray;

    /**
     * An array of locally stored X co-ordinate displacements for the vertices.
     */
    protected double[] dispX;

    /**
     * An array of locally stored Y co-ordinate displacements for the vertices.
     */
    protected double[] dispY;

    /**
     * An array of locally stored co-ordinate positions for the vertices.
     */
    protected double[][] cellLocation;

    /**
     * The approximate radius of each cell, nodes only.
     */
    protected double[] radius;

    /**
     * The approximate radius squared of each cell, nodes only.
     */
    protected double[] radiusSquared;

    /**
     * Array of booleans representing the movable states of the vertices.
     */
    protected boolean[] isMoveable;

    /**
     * Local copy of cell neighbours.
     */
    protected int[][] neighbours;

    /**
     * Boolean flag that specifies if the layout is allowed to run. If this is
     * set to false, then the layout exits in the following iteration.
     */
    protected boolean allowedToRun = true;

    /**
     * Maps from vertices to indices.
     */
    protected HashMap<ICell, Integer> indices = new HashMap<>();

    /**
     * Constructs a new fast organic layout for the specified graph.
     */
    public FastOrganicLayout(Graph graph) {
        super(graph);
    }

    /**
     * Returns a boolean indicating if the given <Cell> should be ignored as a
     * vertex. This returns true if the cell has no connections.
     *
     * @param vertex Object that represents the vertex to be tested.
     * @return Returns true if the vertex should be ignored.
     */
    public boolean isVertexIgnored(ICell vertex) {
        return super.isVertexIgnored(vertex) || graph.getConnections(vertex).isEmpty();
    }

    /**
     * Reduces the temperature of the layout from an initial setting in a linear
     * fashion to zero.
     */
    protected void reduceTemperature() {
        temperature = initialTemp * (1.0 - iteration / maxIterations);
    }


    public void moveCell(ICell cell, double x, double y) {
        // TODO: Map the position to a child index for
        // the cell to be placed closest to the position
    }


    public void execute(ICell parent) {
        IGraphModel model = graph.getModel();

        vertexArray = graph.getChildVertices(parent).stream().filter(vertex -> !isVertexIgnored(vertex)).collect(Collectors.toList());
        RectangleDouble initialBounds = (useInputOrigin) ? graph.getBoundsForCells(vertexArray, false, false, true) : null;
        int n = vertexArray.size();

        dispX = new double[n];
        dispY = new double[n];
        cellLocation = new double[n][];
        isMoveable = new boolean[n];
        neighbours = new int[n][];
        radius = new double[n];
        radiusSquared = new double[n];

        minDistanceLimitSquared = minDistanceLimit * minDistanceLimit;

        if (forceConstant < 0.001) {
            forceConstant = 0.001;
        }

        forceConstantSquared = forceConstant * forceConstant;

        // Create a map of vertices first. This is required for the array of
        // arrays called neighbours which holds, for each vertex, a list of
        // ints which represents the neighbours cells to that vertex as
        // the indices into vertexArray
        for (int i = 0; i < vertexArray.size(); i++) {
            ICell vertex = vertexArray.get(i);
            cellLocation[i] = new double[2];

            // Set up the mapping from array indices to cells
            indices.put(vertex, i);
            RectangleDouble bounds = getVertexBounds(vertex);

            // Set the X,Y value of the internal version of the cell to
            // the center point of the vertex for better positioning
            double width = bounds.getWidth();
            double height = bounds.getHeight();

            // Randomize (0, 0) locations
            double x = bounds.getX();
            double y = bounds.getY();

            cellLocation[i][0] = x + width / 2.0;
            cellLocation[i][1] = y + height / 2.0;

            radius[i] = Math.min(width, height);
            radiusSquared[i] = radius[i] * radius[i];
        }

        // Moves cell location back to top-left from center locations used in
        // algorithm, resetting the edge points is part of the transaction
        model.beginUpdate();
        try {
            for (int i = 0; i < n; i++) {
                dispX[i] = 0;
                dispY[i] = 0;
                isMoveable[i] = isVertexMovable(vertexArray.get(i));

                // Get lists of neighbours to all vertices, translate the cells
                // obtained in indices into vertexArray and store as an array
                // against the original cell index
                List<ICell> edges = graph.getConnections(vertexArray.get(i), parent);
                for (ICell edge : edges) {
                    if (isResetEdges()) {
                        graph.resetEdge(edge);
                    }

                    if (isDisableEdgeStyle()) {
                        setEdgeStyleEnabled(edge, false);
                    }
                }

                List<ICell> cells = graph.getOpposites(edges, vertexArray.get(i));

                neighbours[i] = new int[cells.size()];

                for (int j = 0; j < cells.size(); j++) {
                    Integer index = indices.get(cells.get(j));

                    // Check the connected cell in part of the vertex list to be
                    // acted on by this layout
                    if (index != null) {
                        neighbours[i][j] = index;
                    }

                    // Else if index of the other cell doesn't correspond to
                    // any cell listed to be acted upon in this layout. Set
                    // the index to the value of this vertex (a dummy self-loop)
                    // so the attraction force of the edge is not calculated
                    else {
                        neighbours[i][j] = i;
                    }
                }
            }

            temperature = initialTemp;

            // If max number of iterations has not been set, guess it
            if (maxIterations == 0) {
                maxIterations = 20.0 * Math.sqrt(n);
            }

            // Main iteration loop
            for (iteration = 0; iteration < maxIterations; iteration++) {
                if (!allowedToRun) {
                    return;
                }

                // Calculate repulsive forces on all vertices
                calcRepulsion();

                // Calculate attractive forces through edges
                calcAttraction();

                calcPositions();
                reduceTemperature();
            }

            Double minx = null;
            Double miny = null;

            for (int i = 0; i < vertexArray.size(); i++) {
                ICell vertex = vertexArray.get(i);
                Geometry geo = model.getGeometry(vertex);

                if (geo != null) {
                    cellLocation[i][0] -= geo.getWidth() / 2.0;
                    cellLocation[i][1] -= geo.getHeight() / 2.0;

                    double x = graph.snap(cellLocation[i][0]);
                    double y = graph.snap(cellLocation[i][1]);
                    setVertexLocation(vertex, x, y);

                    if (minx == null) {
                        minx = x;
                    } else {
                        minx = Math.min(minx, x);
                    }

                    if (miny == null) {
                        miny = y;
                    } else {
                        miny = Math.min(miny, y);
                    }
                }
            }

            // Modifies the cloned geometries in-place. Not needed
            // to clone the geometries again as we're in the same
            // undoable change.
            double dx = (minx != null) ? -minx - 1 : 0;
            double dy = (miny != null) ? -miny - 1 : 0;

            if (initialBounds != null) {
                dx += initialBounds.getX();
                dy += initialBounds.getY();
            }

            graph.moveCells(vertexArray, dx, dy);
        } finally {
            model.endUpdate();
        }
    }

    /**
     * Takes the displacements calculated for each cell and applies them to the
     * local cache of cell positions. Limits the displacement to the current
     * temperature.
     */
    protected void calcPositions() {
        for (int index = 0; index < vertexArray.size(); index++) {
            if (isMoveable[index]) {
                // Get the distance of displacement for this node for this
                // iteration
                double deltaLength = Math.sqrt(dispX[index] * dispX[index] + dispY[index] * dispY[index]);

                if (deltaLength < 0.001) {
                    deltaLength = 0.001;
                }

                // Scale down by the current temperature if less than the
                // displacement distance
                double newXDisp = dispX[index] / deltaLength * Math.min(deltaLength, temperature);
                double newYDisp = dispY[index] / deltaLength * Math.min(deltaLength, temperature);

                // reset displacements
                dispX[index] = 0;
                dispY[index] = 0;

                // Update the cached cell locations
                cellLocation[index][0] += newXDisp;
                cellLocation[index][1] += newYDisp;
            }
        }
    }

    /**
     * Calculates the attractive forces between all laid out nodes linked by
     * edges
     */
    protected void calcAttraction() {
        // Check the neighbours of each vertex and calculate the attractive
        // force of the edge connecting them
        for (int i = 0; i < vertexArray.size(); i++) {
            for (int k = 0; k < neighbours[i].length; k++) {
                // Get the index of the othe cell in the vertex array
                int j = neighbours[i][k];

                // Do not proceed self-loops
                if (i != j) {
                    double xDelta = cellLocation[i][0] - cellLocation[j][0];
                    double yDelta = cellLocation[i][1] - cellLocation[j][1];

                    // The distance between the nodes
                    double deltaLengthSquared = xDelta * xDelta + yDelta * yDelta - radiusSquared[i] - radiusSquared[j];

                    if (deltaLengthSquared < minDistanceLimitSquared) {
                        deltaLengthSquared = minDistanceLimitSquared;
                    }

                    double deltaLength = Math.sqrt(deltaLengthSquared);
                    double force = (deltaLengthSquared) / forceConstant;

                    double displacementX = (xDelta / deltaLength) * force;
                    double displacementY = (yDelta / deltaLength) * force;

                    if (isMoveable[i]) {
                        this.dispX[i] -= displacementX;
                        this.dispY[i] -= displacementY;
                    }

                    if (isMoveable[j]) {
                        dispX[j] += displacementX;
                        dispY[j] += displacementY;
                    }
                }
            }
        }
    }

    /**
     * Calculates the repulsive forces between all laid out nodes
     */
    protected void calcRepulsion() {
        int vertexCount = vertexArray.size();

        for (int i = 0; i < vertexCount; i++) {
            for (int j = i; j < vertexCount; j++) {
                // Exits if the layout is no longer allowed to run
                if (!allowedToRun) {
                    return;
                }

                if (j != i) {
                    double xDelta = cellLocation[i][0] - cellLocation[j][0];
                    double yDelta = cellLocation[i][1] - cellLocation[j][1];

                    if (xDelta == 0) {
                        xDelta = 0.01 + Math.random();
                    }

                    if (yDelta == 0) {
                        yDelta = 0.01 + Math.random();
                    }

                    // Distance between nodes
                    double deltaLength = Math.sqrt((xDelta * xDelta) + (yDelta * yDelta));

                    double deltaLengthWithRadius = deltaLength - radius[i] - radius[j];

                    if (deltaLengthWithRadius > maxDistanceLimit) {
                        // Ignore vertices too far apart
                        continue;
                    }

                    if (deltaLengthWithRadius < minDistanceLimit) {
                        deltaLengthWithRadius = minDistanceLimit;
                    }

                    double force = forceConstantSquared / deltaLengthWithRadius;

                    double displacementX = (xDelta / deltaLength) * force;
                    double displacementY = (yDelta / deltaLength) * force;

                    if (isMoveable[i]) {
                        dispX[i] += displacementX;
                        dispY[i] += displacementY;
                    }

                    if (isMoveable[j]) {
                        dispX[j] -= displacementX;
                        dispY[j] -= displacementY;
                    }
                }
            }
        }
    }

}
