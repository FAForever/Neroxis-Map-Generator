/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.w3c.dom.Node;

/**
 * Cells are the elements of the graph model. They represent the state
 * of the groups, vertices and edges in a graph.
 *
 * <h4>Edge Labels</h4>
 * <p>
 * Using the x- and y-coordinates of a cell's geometry it is
 * possible to position the label on edges on a specific location
 * on the actual edge shape as it appears on the screen. The
 * x-coordinate of an edge's geometry is used to describe the
 * distance from the center of the edge from -1 to 1 with 0
 * being the center of the edge and the default value. The
 * y-coordinate of an edge's geometry is used to describe
 * the absolute, orthogonal distance in pixels from that
 * point. In addition, the Geometry.offset is used
 * as a absolute offset vector from the resulting point.
 * <p>
 * The width and height of an edge geometry are ignored.
 * <p>
 * To add more than one edge label, add a child vertex with
 * a relative geometry. The x- and y-coordinates of that
 * geometry will have the same semantics as above for
 * edge labels.
 */
@Getter
@Setter
public class Cell implements ICell, Cloneable, Serializable {
    @Serial
    private static final long serialVersionUID = 910211337632342672L;
    /**
     * Holds the child cells and connected edges.
     */
    protected final List<ICell> children = new ArrayList<>();
    protected final List<ICell> edges = new ArrayList<>();
    /**
     * Holds the Id. Default is null.
     */
    protected String id;
    /**
     * Holds the user object. Default is null.
     */
    protected Object value;
    /**
     * Holds the geometry. Default is null.
     */
    protected Geometry geometry;
    /**
     * Holds the style as a string of the form
     * stylename[;key=value]. Default is null.
     */
    protected String style;
    /**
     * Specifies whether the cell is a vertex or edge and whether it is
     * connectable, visible and collapsed. Default values are false, false,
     * true, true and false respectively.
     */
    protected boolean vertex = false;
    protected boolean edge = false;
    protected boolean connectable = true;
    protected boolean visible = true;
    protected boolean collapsed = false;
    /**
     * Reference to the parent cell and source and target terminals for edges.
     */
    protected ICell parent;
    protected ICell source;
    protected ICell target;

    /**
     * Constructs a new cell with an empty user object.
     */
    public Cell() {
        this(null);
    }

    /**
     * Constructs a new cell for the given user object.
     *
     * @param value Object that represents the value of the cell.
     */
    public Cell(Object value) {
        this(value, null, null);
    }

    /**
     * Constructs a new cell for the given parameters.
     *
     * @param value    Object that represents the value of the cell.
     * @param geometry Specifies the geometry of the cell.
     * @param style    Specifies the style as a formatted string.
     */
    public Cell(Object value, Geometry geometry, String style) {
        setValue(value);
        setGeometry(geometry);
        setStyle(style);
    }

    /**
     * Returns a clone of the cell.
     */
    @Override
    public Cell clone() throws CloneNotSupportedException {
        Cell clone = (Cell) super.clone();
        clone.setValue(cloneValue());
        clone.setStyle(getStyle());
        clone.setCollapsed(isCollapsed());
        clone.setConnectable(isConnectable());
        clone.setEdge(isEdge());
        clone.setVertex(isVertex());
        clone.setVisible(isVisible());
        for (ICell child : getChildren()) {
            clone.remove(child);
            clone.insert((ICell) child.clone());
        }
        clone.setParent(null);
        clone.setSource(null);
        clone.setTarget(null);
        Geometry geometry = getGeometry();
        if (geometry != null) {
            clone.setGeometry(geometry.clone());
        }

        return clone;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + "id=" + id + ", value=" + value + ", geometry=" + geometry + "]";
    }

    /**
     * Returns a clone of the user object. This implementation clones any XML
     * nodes or otherwise returns the same user object instance.
     */
    protected Object cloneValue() {
        Object value = getValue();

        if (value instanceof Node) {
            value = ((Node) value).cloneNode(true);
        }

        return value;
    }

    @Override
    @Deprecated
    public ICell getTerminal(boolean source) {
        return (source) ? getSource() : getTarget();
    }

    @Override
    @Deprecated
    public ICell setTerminal(ICell terminal, boolean isSource) {
        if (isSource) {
            setSource(terminal);
        } else {
            setTarget(terminal);
        }

        return terminal;
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public List<ICell> getChildren() {
        return List.copyOf(children);
    }

    @Override
    public int getIndex(ICell child) {
        return children.indexOf(child);
    }

    @Override
    public ICell getChildAt(int index) {
        return children.get(index);
    }

    @Override
    public ICell insert(ICell child) {
        int index = getChildCount();

        if (child.getParent() == this) {
            index--;
        }

        return insert(child, index);
    }

    @Override
    public ICell insert(ICell child, int index) {
        if (child != null) {
            child.removeFromParent();
            child.setParent(this);
            children.add(index, child);
        }

        return child;
    }

    @Override
    public ICell remove(int index) {
        ICell child = null;
        if (index >= 0) {
            child = getChildAt(index);
            remove(child);
        }

        return child;
    }

    @Override
    public ICell remove(ICell child) {
        if (child != null) {
            children.remove(child);
            child.setParent(null);
        }

        return child;
    }

    @Override
    public void removeFromParent() {
        if (parent != null) {
            parent.remove(this);
        }
    }

    @Override
    public int getEdgeCount() {
        return edges.size();
    }

    @Override
    public int getEdgeIndex(ICell edge) {
        return edges.indexOf(edge);
    }

    @Override
    public ICell getEdgeAt(int index) {
        return edges.get(index);
    }

    @Override
    public ICell insertEdge(ICell edge, boolean isOutgoing) {
        if (edge != null) {
            edge.removeFromTerminal(isOutgoing);
            edge.setTerminal(this, isOutgoing);
            if (edge.getTerminal(!isOutgoing) != this || !edges.contains(edge)) {
                edges.add(edge);
            }
        }

        return edge;
    }

    @Override
    public ICell removeEdge(ICell edge, boolean isOutgoing) {
        if (edge != null) {
            if (edge.getTerminal(!isOutgoing) != this) {
                edges.remove(edge);
            }

            edge.setTerminal(null, isOutgoing);
        }

        return edge;
    }

    @Override
    public void removeFromTerminal(boolean isSource) {
        ICell terminal = getTerminal(isSource);

        if (terminal != null) {
            terminal.removeEdge(this, isSource);
        }
    }
}
