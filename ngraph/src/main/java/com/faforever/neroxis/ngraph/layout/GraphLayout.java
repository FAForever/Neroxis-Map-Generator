/**
 * Copyright (c) 2008-2009, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.layout;

import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;
import com.faforever.neroxis.ngraph.view.GraphView;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract bass class for layouts
 */
@Getter
@Setter
public abstract class GraphLayout implements IGraphLayout {

    /**
     * Holds the enclosing graph.
     */
    protected final Graph graph;
    /**
     * The parent cell of the layout, if any
     */
    protected ICell parent;
    /**
     * Boolean indicating if the bounding box of the label should be used if
     * its available. Default is true.
     */
    protected boolean useBoundingBox = true;

    /**
     * Constructs a new fast organic layout for the specified graph.
     */
    public GraphLayout(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void execute(ICell parent) {
        this.parent = parent;
    }

    @Override
    public void moveCell(ICell cell, double x, double y) {
        // TODO: Map the position to a child index for
        // the cell to be placed closest to the position
    }

    /**
     * @return the useBoundingBox
     */
    public boolean isUseBoundingBox() {
        return useBoundingBox;
    }

    /**
     * @param useBoundingBox the useBoundingBox to set
     */
    public void setUseBoundingBox(boolean useBoundingBox) {
        this.useBoundingBox = useBoundingBox;
    }

    /**
     * Returns true if the given vertex may be moved by the layout.
     *
     * @param vertex Object that represents the vertex to be tested.
     * @return Returns true if the vertex can be moved.
     */
    public boolean isVertexMovable(ICell vertex) {
        return graph.isCellMovable(vertex);
    }

    /**
     * Returns true if the given vertex has no connected edges.
     *
     * @param vertex Object that represents the vertex to be tested.
     * @return Returns true if the vertex should be ignored.
     */
    public boolean isVertexIgnored(ICell vertex) {
        return !graph.getModel().isVertex(vertex) || !graph.isCellVisible(vertex);
    }

    /**
     * Returns true if the given edge has no source or target terminal.
     *
     * @param edge Object that represents the edge to be tested.
     * @return Returns true if the edge should be ignored.
     */
    public boolean isEdgeIgnored(ICell edge) {
        IGraphModel model = graph.getModel();

        return !model.isEdge(edge)
               || !graph.isCellVisible(edge)
               || model.getTerminal(edge, true) == null
               || model.getTerminal(edge, false) == null;
    }

    /**
     * Disables or enables the edge style of the given edge.
     */
    public void setEdgeStyleEnabled(ICell edge, boolean value) {
        graph.setCellStyles(Constants.STYLE_NOEDGESTYLE, (value) ? "0" : "1", List.of(edge));
    }

    /**
     * Disables or enables orthogonal end segments of the given edge
     */
    public void setOrthogonalEdge(ICell edge, boolean value) {
        graph.setCellStyles(Constants.STYLE_ORTHOGONAL, (value) ? "1" : "0", List.of(edge));
    }

    /**
     * Sets the control points of the given edge to the given
     * list of Points. Set the points to null to remove all
     * existing points for an edge.
     */
    public void setEdgePoints(ICell edge, List<PointDouble> points) {
        IGraphModel model = graph.getModel();
        Geometry geometry = model.getGeometry(edge);
        if (geometry == null) {
            geometry = new Geometry();
            geometry.setRelative(true);
        } else {
            geometry = geometry.clone();
        }

        if (this.parent != null && points != null) {
            ICell parent = graph.getModel().getParent(edge);
            PointDouble parentOffset = getParentOffset(parent);
            for (PointDouble point : points) {
                point.setX(point.getX() - parentOffset.getX());
                point.setY(point.getY() - parentOffset.getY());
            }
        }

        geometry.setPoints(points);
        model.setGeometry(edge, geometry);
    }

    /**
     * Returns an <Rectangle> that defines the bounds of the given cell
     * or the bounding box if <useBoundingBox> is true.
     */
    public RectangleDouble getVertexBounds(ICell vertex) {
        RectangleDouble geo = graph.getModel().getGeometry(vertex);
        // Checks for oversize label bounding box and corrects
        // the return value accordingly
        if (useBoundingBox) {
            CellState state = graph.getView().getState(vertex);
            if (state != null) {
                double scale = graph.getView().getScale();
                RectangleDouble tmp = state.getBoundingBox();

                double dx0 = (tmp.getX() - state.getX()) / scale;
                double dy0 = (tmp.getY() - state.getY()) / scale;
                double dx1 = (tmp.getX() + tmp.getWidth() - state.getX() - state.getWidth()) / scale;
                double dy1 = (tmp.getY() + tmp.getHeight() - state.getY() - state.getHeight()) / scale;
                geo = new RectangleDouble(geo.getX() + dx0, geo.getY() + dy0, geo.getWidth() - dx0 + dx1,
                                          geo.getHeight() + -dy0 + dy1);
            }
        }

        if (this.parent != null) {
            ICell parent = graph.getModel().getParent(vertex);
            geo = geo.clone();

            if (parent != null && parent != this.parent) {
                PointDouble parentOffset = getParentOffset(parent);
                geo.setX(geo.getX() + parentOffset.getX());
                geo.setY(geo.getY() + parentOffset.getY());
            }
        }
        return new RectangleDouble(geo);
    }

    public PointDouble getParentOffset(ICell parent) {
        PointDouble result = new PointDouble();
        if (parent != null && parent != this.parent) {
            IGraphModel model = graph.getModel();
            if (model.isAncestor(this.parent, parent)) {
                Geometry parentGeo = model.getGeometry(parent);
                while (parent != this.parent) {
                    result.setX(result.getX() + parentGeo.getX());
                    result.setY(result.getY() + parentGeo.getY());

                    parent = model.getParent(parent);
                    parentGeo = model.getGeometry(parent);
                }
            }
        }

        return result;
    }

    /**
     * Sets the new position of the given cell taking into account the size of
     * the bounding box if <useBoundingBox> is true. The change is only carried
     * out if the new location is not equal to the existing location, otherwise
     * the geometry is not replaced with an updated instance. The new or old
     * bounds are returned (including overlapping labels).
     * <p>
     * Parameters:
     * <p>
     * cell - <Cell> whose geometry is to be set.
     * x - Integer that defines the x-coordinate of the new location.
     * y - Integer that defines the y-coordinate of the new location.
     */
    public RectangleDouble setVertexLocation(ICell vertex, double x, double y) {
        IGraphModel model = graph.getModel();
        Geometry geometry = model.getGeometry(vertex);
        RectangleDouble result = null;
        if (geometry != null) {
            result = new RectangleDouble(x, y, geometry.getWidth(), geometry.getHeight());
            GraphView graphView = graph.getView();
            // Checks for oversize labels and offset the result
            if (useBoundingBox) {
                CellState state = graphView.getState(vertex);

                if (state != null) {
                    double scale = graph.getView().getScale();
                    RectangleDouble box = state.getBoundingBox();

                    if (state.getBoundingBox().getX() < state.getX()) {
                        x += (state.getX() - box.getX()) / scale;
                        result.setWidth(box.getWidth());
                    }
                    if (state.getBoundingBox().getY() < state.getY()) {
                        y += (state.getY() - box.getY()) / scale;
                        result.setHeight(box.getHeight());
                    }
                }
            }

            if (this.parent != null) {
                ICell parent = model.getParent(vertex);

                if (parent != null && parent != this.parent) {
                    PointDouble parentOffset = getParentOffset(parent);

                    x = x - parentOffset.getX();
                    y = y - parentOffset.getY();
                }
            }

            if (geometry.getX() != x || geometry.getY() != y) {
                geometry = geometry.clone();
                geometry.setX(x);
                geometry.setY(y);

                model.setGeometry(vertex, geometry);
            }
        }

        return result;
    }

    /**
     * Updates the bounds of the given groups to include all children. Call
     * this with the groups in parent to child order, top-most group first, eg.
     * <p>
     * arrangeGroups(graph, Utils.sortCells(Arrays.asList(
     * new Object[] { v1, v3 }), true).toArray(), 10);
     *
     * @param groups the groups to adjust
     * @param border the border applied to the adjusted groups
     */
    public void arrangeGroups(List<ICell> groups, int border) {
        graph.getModel().beginUpdate();
        try {
            for (int i = groups.size() - 1; i >= 0; i--) {
                ICell group = groups.get(i);
                List<ICell> children = graph.getChildVertices(group);
                RectangleDouble bounds = graph.getBoundingBoxFromGeometry(children);

                Geometry geometry = graph.getCellGeometry(group);
                double left = 0;
                double top = 0;

                // Adds the size of the title area for swimlanes
                if (this.graph.isSwimlane(group)) {
                    RectangleDouble size = graph.getStartSize(group);
                    left = size.getWidth();
                    top = size.getHeight();
                }

                if (bounds != null && geometry != null) {
                    geometry = geometry.clone();
                    geometry.setX(geometry.getX() + bounds.getX() - border - left);
                    geometry.setY(geometry.getY() + bounds.getY() - border - top);
                    geometry.setWidth(bounds.getWidth() + 2 * border + left);
                    geometry.setHeight(bounds.getHeight() + 2 * border + top);
                    graph.getModel().setGeometry(group, geometry);
                    graph.moveCells(children, border + left - bounds.getX(), border + top - bounds.getY());
                }
            }
        } finally {
            graph.getModel().endUpdate();
        }
    }
}
