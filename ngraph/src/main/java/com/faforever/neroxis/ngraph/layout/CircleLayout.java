package com.faforever.neroxis.ngraph.layout;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.Graph;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CircleLayout extends GraphLayout {
    /**
     * Integer specifying the size of the radius. Default is 100.
     */
    protected double radius;
    /**
     * Boolean specifying if the circle should be moved to the top,
     * left corner specified by x0 and y0. Default is false.
     */
    protected boolean moveCircle = true;
    /**
     * Integer specifying the left coordinate of the circle.
     * Default is 0.
     */
    protected double x0 = 0;
    /**
     * Integer specifying the top coordinate of the circle.
     * Default is 0.
     */
    protected double y0 = 0;
    /**
     * Specifies if all edge points of traversed edges should be removed.
     * Default is true.
     */
    protected boolean resetEdges = false;
    /**
     * Specifies if the STYLE_NOEDGESTYLE flag should be set on edges that are
     * modified by the result. Default is true.
     */
    protected boolean disableEdgeStyle = true;

    /**
     * Constructs a new stack layout layout for the specified graph,
     * spacing, orientation and offset.
     */
    public CircleLayout(Graph graph) {
        this(graph, 100);
    }

    /**
     * Constructs a new stack layout layout for the specified graph,
     * spacing, orientation and offset.
     */
    public CircleLayout(Graph graph, double radius) {
        super(graph);
        this.radius = radius;
    }

    @Override
    public void execute(ICell parent) {
        IGraphModel model = graph.getModel();

        // Moves the vertices to build a circle. Makes sure the
        // radius is large enough for the vertices to not
        // overlap
        model.beginUpdate();
        try {
            // Gets all vertices inside the parent and finds
            // the maximum dimension of the largest vertex
            double max = 0;
            Double top = null;
            Double left = null;
            List<ICell> vertices = new ArrayList<>();
            int childCount = model.getChildCount(parent);

            for (int i = 0; i < childCount; i++) {
                ICell cell = model.getChildAt(parent, i);

                if (!isVertexIgnored(cell)) {
                    vertices.add(cell);
                    RectangleDouble bounds = getVertexBounds(cell);

                    if (top == null) {
                        top = bounds.getY();
                    } else {
                        top = Math.min(top, bounds.getY());
                    }

                    if (left == null) {
                        left = bounds.getX();
                    } else {
                        left = Math.min(left, bounds.getX());
                    }

                    max = Math.max(max, Math.max(bounds.getWidth(), bounds.getHeight()));
                } else if (!isEdgeIgnored(cell)) {
                    if (isResetEdges()) {
                        graph.resetEdge(cell);
                    }

                    if (isDisableEdgeStyle()) {
                        setEdgeStyleEnabled(cell, false);
                    }
                }
            }

            int vertexCount = vertices.size();
            double trueRadius = Math.max(vertexCount * max / Math.PI, radius);

            // Moves the circle to the specified origin
            if (moveCircle) {
                left = x0;
                top = y0;
            }

            circle(vertices, trueRadius, left, top);
        } finally {
            model.endUpdate();
        }
    }

    /**
     * Executes the circular layout for the specified array
     * of vertices and the given radius.
     */
    public void circle(List<ICell> vertices, double r, double left, double top) {
        int vertexCount = vertices.size();
        double phi = 2 * Math.PI / vertexCount;

        for (int i = 0; i < vertexCount; i++) {
            ICell vertex = vertices.get(i);
            if (isVertexMovable(vertex)) {
                setVertexLocation(vertex, left + r + r * Math.sin(i * phi), top + r + r * Math.cos(i * phi));
            }
        }
    }
}
