/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.model;

import com.faforever.neroxis.ngraph.event.BeforeUndoEvent;
import com.faforever.neroxis.ngraph.event.BeginUpdateEvent;
import com.faforever.neroxis.ngraph.event.ChangeEvent;
import com.faforever.neroxis.ngraph.event.EndUpdateEvent;
import com.faforever.neroxis.ngraph.event.EventSource;
import com.faforever.neroxis.ngraph.event.ExecuteEvent;
import com.faforever.neroxis.ngraph.event.UndoEvent;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.UndoableEdit;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Extends EventSource to implement a graph model. The graph model acts as
 * a wrapper around the cells which are in charge of storing the actual graph
 * datastructure. The model acts as a transactional wrapper with event
 * notification for all changes, whereas the cells contain the atomic
 * operations for updating the actual datastructure.
 * <p>
 * Layers:
 * <p>
 * The cell hierarchy in the model must have a top-level root cell which
 * contains the layers (typically one default layer), which in turn contain the
 * top-level cells of the layers. This means each cell is contained in a layer.
 * If no layers are required, then all new cells should be added to the default
 * layer.
 * <p>
 * Layers are useful for hiding and showing groups of cells, or for placing
 * groups of cells on top of other cells in the display. To identify a layer,
 * the <isLayer> function is used. It returns true if the parent of the given
 * cell is the root of the model.
 * <p>
 * This class fires the following events:
 * <p>
 * Event.CHANGE fires when an undoable edit is dispatched. The <code>edit</code>
 * property contains the UndoableEdit. The <code>changes</code> property
 * contains the list of undoable changes inside the undoable edit. The changes
 * property is deprecated, please use edit.getChanges() instead.
 * <p>
 * Event.EXECUTE fires between begin- and endUpdate and after an atomic
 * change was executed in the model. The <code>change</code> property contains
 * the atomic change that was executed.
 * <p>
 * Event.BEGIN_UPDATE fires after the updateLevel was incremented in
 * beginUpdate. This event contains no properties.
 * <p>
 * Event.END_UPDATE fires after the updateLevel was decreased in endUpdate
 * but before any notification or change dispatching. The <code>edit</code>
 * property contains the current UndoableEdit.
 * <p>
 * Event.BEFORE_UNDO fires before the change is dispatched after the update
 * level has reached 0 in endUpdate. The <code>edit</code> property contains
 * the current UndoableEdit.
 * <p>
 * Event.UNDO fires after the change was dispatched in endUpdate. The
 * <code>edit</code> property contains the current UndoableEdit.
 */
public class GraphModel extends EventSource implements IGraphModel, Serializable {
    private static final Logger log = Logger.getLogger(GraphModel.class.getName());
    /**
     * Holds the root cell, which in turn contains the cells that represent the
     * layers of the diagram as child cells. That is, the actual element of the
     * diagram are supposed to live in the third generation of cells and below.
     */
    protected ICell root;
    /**
     * Maps from Ids to cells.
     */
    protected Map<String, ICell> cells;
    /**
     * Specifies if edges should automatically be moved into the nearest common
     * ancestor of their terminals. Default is true.
     */
    protected boolean maintainEdgeParent = true;
    /**
     * Specifies if the model should automatically create Ids for new cells.
     * Default is true.
     */
    protected boolean createIds = true;
    /**
     * Specifies the next Id to be created. Initial value is 0.
     */
    protected int nextId = 0;
    /**
     * Holds the changes for the current transaction. If the transaction is
     * closed then a new object is created for this variable using
     * createUndoableEdit.
     */
    protected transient UndoableEdit currentEdit;
    /**
     * Counter for the depth of nested transactions. Each call to beginUpdate
     * increments this counter and each call to endUpdate decrements it. When
     * the counter reaches 0, the transaction is closed and the respective
     * events are fired. Initial value is 0.
     */
    protected transient int updateLevel = 0;
    protected transient boolean endingUpdate = false;

    /**
     * Constructs a new empty graph model.
     */
    public GraphModel() {
        this(null);
    }

    /**
     * Constructs a new graph model. If no root is specified
     * then a new root Cell with a default layer is created.
     *
     * @param root Cell that represents the root cell.
     */
    public GraphModel(ICell root) {
        currentEdit = createUndoableEdit();

        if (root != null) {
            setRoot(root);
        } else {
            clear();
        }
    }

    /**
     * Returns the number of incoming or outgoing edges.
     *
     * @param model    Graph model that contains the connection data.
     * @param cell     Cell whose edges should be counted.
     * @param outgoing Boolean that specifies if the number of outgoing or
     *                 incoming edges should be returned.
     * @return Returns the number of incoming or outgoing edges.
     */
    public static int getDirectedEdgeCount(IGraphModel model, ICell cell, boolean outgoing) {
        return getDirectedEdgeCount(model, cell, outgoing, null);
    }

    /**
     * Returns the number of incoming or outgoing edges, ignoring the given
     * edge.
     *
     * @param model       Graph model that contains the connection data.
     * @param cell        Cell whose edges should be counted.
     * @param outgoing    Boolean that specifies if the number of outgoing or
     *                    incoming edges should be returned.
     * @param ignoredEdge Object that represents an edge to be ignored.
     * @return Returns the number of incoming or outgoing edges.
     */
    public static int getDirectedEdgeCount(IGraphModel model, ICell cell, boolean outgoing, ICell ignoredEdge) {
        int count = 0;
        int edgeCount = model.getEdgeCount(cell);

        for (int i = 0; i < edgeCount; i++) {
            ICell edge = model.getEdgeAt(cell, i);

            if (edge != ignoredEdge && model.getTerminal(edge, outgoing) == cell) {
                count++;
            }
        }

        return count;
    }

    /**
     * Returns all edges connected to this cell including loops.
     *
     * @param model Model that contains the connection information.
     * @param cell  Cell whose connections should be returned.
     * @return Returns the array of connected edges for the given cell.
     */
    public static List<ICell> getEdges(IGraphModel model, ICell cell) {
        return getEdges(model, cell, true, true, true);
    }

    /**
     * Returns all distinct edges connected to this cell.
     *
     * @param model        Model that contains the connection information.
     * @param cell         Cell whose connections should be returned.
     * @param incoming     Specifies if incoming edges should be returned.
     * @param outgoing     Specifies if outgoing edges should be returned.
     * @param includeLoops Specifies if loops should be returned.
     * @return Returns the array of connected edges for the given cell.
     */
    public static List<ICell> getEdges(IGraphModel model, ICell cell, boolean incoming, boolean outgoing,
                                       boolean includeLoops) {
        int edgeCount = model.getEdgeCount(cell);
        List<ICell> result = new ArrayList<>(edgeCount);

        for (int i = 0; i < edgeCount; i++) {
            ICell edge = model.getEdgeAt(cell, i);
            ICell source = model.getTerminal(edge, true);
            ICell target = model.getTerminal(edge, false);

            if ((includeLoops && source == target) || ((source != target) && ((incoming && target == cell) || (outgoing
                                                                                                               && source
                                                                                                                  == cell)))) {
                result.add(edge);
            }
        }

        return result;
    }

    /**
     * Returns all edges connected to this cell without loops.
     *
     * @param model Model that contains the connection information.
     * @param cell  Cell whose connections should be returned.
     * @return Returns the connected edges for the given cell.
     */
    public static List<ICell> getConnections(IGraphModel model, ICell cell) {
        return getEdges(model, cell, true, true, false);
    }

    /**
     * Returns the incoming edges of the given cell without loops.
     *
     * @param model Graphmodel that contains the edges.
     * @param cell  Cell whose incoming edges should be returned.
     * @return Returns the incoming edges for the given cell.
     */
    public static List<ICell> getIncomingEdges(IGraphModel model, ICell cell) {
        return getEdges(model, cell, true, false, false);
    }

    /**
     * Returns the outgoing edges of the given cell without loops.
     *
     * @param model Graphmodel that contains the edges.
     * @param cell  Cell whose outgoing edges should be returned.
     * @return Returns the outgoing edges for the given cell.
     */
    public static List<ICell> getOutgoingEdges(IGraphModel model, ICell cell) {
        return getEdges(model, cell, false, true, false);
    }

    /**
     * Returns all edges from the given source to the given target.
     *
     * @param model  The graph model that contains the graph.
     * @param source Object that defines the source cell.
     * @param target Object that defines the target cell.
     * @return Returns all edges from source to target.
     */
    public static List<ICell> getEdgesBetween(IGraphModel model, ICell source, ICell target) {
        return getEdgesBetween(model, source, target, false);
    }

    /**
     * Returns all edges between the given source and target pair. If directed
     * is true, then only edges from the source to the target are returned,
     * otherwise, all edges between the two cells are returned.
     *
     * @param model    The graph model that contains the graph.
     * @param source   Object that defines the source cell.
     * @param target   Object that defines the target cell.
     * @param directed Boolean that specifies if the direction of the edge
     *                 should be taken into account.
     * @return Returns all edges between the given source and target.
     */
    public static List<ICell> getEdgesBetween(IGraphModel model, ICell source, ICell target, boolean directed) {
        int tmp1 = model.getEdgeCount(source);
        int tmp2 = model.getEdgeCount(target);

        // Assumes the source has less connected edges
        ICell terminal = source;
        int edgeCount = tmp1;

        // Uses the smaller array of connected edges
        // for searching the edge
        if (tmp2 < tmp1) {
            edgeCount = tmp2;
            terminal = target;
        }

        List<ICell> result = new ArrayList<>(edgeCount);

        // Checks if the edge is connected to the correct
        // cell and returns the first match
        for (int i = 0; i < edgeCount; i++) {
            ICell edge = model.getEdgeAt(terminal, i);
            ICell src = model.getTerminal(edge, true);
            ICell trg = model.getTerminal(edge, false);
            boolean directedMatch = (src == source) && (trg == target);
            boolean oppositeMatch = (trg == source) && (src == target);

            if (directedMatch || (!directed && oppositeMatch)) {
                result.add(edge);
            }
        }

        return result;
    }

    /**
     * Returns all opposite cells of terminal for the given edges.
     *
     * @param model    Model that contains the connection information.
     * @param edges    Array of edges to be examined.
     * @param terminal Cell that specifies the known end of the edges.
     * @return Returns the opposite cells of the given terminal.
     */
    public static List<ICell> getOpposites(IGraphModel model, List<ICell> edges, ICell terminal) {
        return getOpposites(model, edges, terminal, true, true);
    }

    /**
     * Returns all opposite vertices wrt terminal for the given edges, only
     * returning sources and/or targets as specified. The result is returned as
     * an array of Cells.
     *
     * @param model    Model that contains the connection information.
     * @param edges    Array of edges to be examined.
     * @param terminal Cell that specifies the known end of the edges.
     * @param sources  Boolean that specifies if source terminals should
     *                 be contained in the result. Default is true.
     * @param targets  Boolean that specifies if target terminals should
     *                 be contained in the result. Default is true.
     * @return Returns the array of opposite terminals for the given edges.
     */
    public static List<ICell> getOpposites(IGraphModel model, List<ICell> edges, ICell terminal, boolean sources,
                                           boolean targets) {
        List<ICell> terminals = new ArrayList<>();

        if (edges != null) {
            for (ICell edge : edges) {
                ICell source = model.getTerminal(edge, true);
                ICell target = model.getTerminal(edge, false);

                // Checks if the terminal is the source of
                // the edge and if the target should be
                // stored in the result
                if (targets && source == terminal && target != null && target != terminal) {
                    terminals.add(target);
                }

                // Checks if the terminal is the taget of
                // the edge and if the source should be
                // stored in the result
                else if (sources && target == terminal && source != null && source != terminal) {
                    terminals.add(source);
                }
            }
        }

        return terminals;
    }

    /**
     * Sets the source and target of the given edge in a single atomic change.
     *
     * @param edge   Cell that specifies the edge.
     * @param source Cell that specifies the new source terminal.
     * @param target Cell that specifies the new target terminal.
     */
    public static void setTerminals(IGraphModel model, ICell edge, ICell source, ICell target) {
        model.beginUpdate();
        try {
            model.setTerminal(edge, source, true);
            model.setTerminal(edge, target, false);
        } finally {
            model.endUpdate();
        }
    }

    /**
     * Returns all children of the given cell regardless of their type.
     *
     * @param model  Model that contains the hierarchical information.
     * @param parent Cell whose child vertices or edges should be returned.
     * @return Returns the child vertices and/or edges of the given parent.
     */
    public static List<ICell> getChildren(IGraphModel model, ICell parent) {
        return getChildCells(model, parent, false, false);
    }

    /**
     * Returns the children of the given cell that are vertices and/or edges
     * depending on the arguments. If both arguments are false then all
     * children are returned regardless of their type.
     *
     * @param model    Model that contains the hierarchical information.
     * @param parent   Cell whose child vertices or edges should be returned.
     * @param vertices Boolean indicating if child vertices should be returned.
     * @param edges    Boolean indicating if child edges should be returned.
     * @return Returns the child vertices and/or edges of the given parent.
     */
    public static List<ICell> getChildCells(IGraphModel model, ICell parent, boolean vertices, boolean edges) {
        return model.getChildren(parent)
                    .stream()
                    .filter(child -> (!edges && !vertices) || (edges && model.isEdge(child)) || (vertices
                                                                                                 && model.isVertex(
                            child)))
                    .collect(Collectors.toList());
    }

    /**
     * Returns the child vertices of the given parent.
     *
     * @param model  Model that contains the hierarchical information.
     * @param parent Cell whose child vertices should be returned.
     * @return Returns the child vertices of the given parent.
     */
    public static List<ICell> getChildVertices(IGraphModel model, ICell parent) {
        return getChildCells(model, parent, true, false);
    }

    /**
     * Returns the child edges of the given parent.
     *
     * @param model  Model that contains the hierarchical information.
     * @param parent Cell whose child edges should be returned.
     * @return Returns the child edges of the given parent.
     */
    public static List<ICell> getChildEdges(IGraphModel model, ICell parent) {
        return getChildCells(model, parent, false, true);
    }

    public static List<ICell> getParents(IGraphModel model, List<ICell> cells) {
        HashSet<ICell> parents = new HashSet<>();

        if (cells != null) {
            for (ICell cell : cells) {
                ICell parent = model.getParent(cell);

                if (parent != null) {
                    parents.add(parent);
                }
            }
        }

        return List.copyOf(parents);
    }

    public static List<ICell> filterCells(List<ICell> cells, Function<ICell, Boolean> filter) {
        ArrayList<ICell> result = null;
        if (cells != null) {
            result = new ArrayList<>(cells.size());
            for (ICell cell : cells) {
                if (filter.apply(cell)) {
                    result.add(cell);
                }
            }
        }

        return result;
    }

    /**
     * Returns a all descendants of the given cell and the cell itself
     * as a collection.
     */
    public static List<ICell> getDescendants(IGraphModel model, ICell parent) {
        return filterDescendants(model, null, parent);
    }

    /**
     * Creates a collection of cells using the visitor pattern.
     */
    public static List<ICell> filterDescendants(IGraphModel model, Function<ICell, Boolean> filter) {
        return filterDescendants(model, filter, model.getRoot());
    }

    /**
     * Creates a collection of cells using the visitor pattern.
     */
    public static List<ICell> filterDescendants(IGraphModel model, Function<ICell, Boolean> filter, ICell parent) {
        List<ICell> result = new ArrayList<>();
        if (filter == null || filter.apply(parent)) {
            result.add(parent);
        }
        int childCount = model.getChildCount(parent);
        for (int i = 0; i < childCount; i++) {
            ICell child = model.getChildAt(parent, i);
            result.addAll(filterDescendants(model, filter, child));
        }

        return result;
    }

    /**
     * Function: getTopmostCells
     * <p>
     * Returns the topmost cells of the hierarchy in an array that contains no
     * desceandants for each <Cell> that it contains. Duplicates should be
     * removed in the cells array to improve performance.
     * <p>
     * Parameters:
     * <p>
     * cells - Array of <Cells> whose topmost ancestors should be returned.
     */
    public static List<ICell> getTopmostCells(IGraphModel model, List<ICell> cells) {
        Set<ICell> hash = new HashSet<>(cells);
        List<ICell> result = new ArrayList<>(cells.size());

        for (ICell cell : cells) {
            boolean topmost = true;
            ICell parent = model.getParent(cell);

            while (parent != null) {
                if (hash.contains(parent)) {
                    topmost = false;
                    break;
                }

                parent = model.getParent(parent);
            }

            if (topmost) {
                result.add(cell);
            }
        }

        return result;
    }

    /**
     * Sets a new root using createRoot.
     */
    public void clear() {
        setRoot(createRoot());
    }

    public int getUpdateLevel() {
        return updateLevel;
    }

    /**
     * Creates a new root cell with a default layer (child 0).
     */
    public ICell createRoot() {
        Cell root = new Cell();
        root.insert(new Cell());

        return root;
    }

    /**
     * Returns the internal lookup table that is used to map from Ids to cells.
     */
    public Map<String, ICell> getCells() {
        return cells;
    }

    /**
     * Returns the cell for the specified Id or null if no cell can be
     * found for the given Id.
     *
     * @param id A string representing the Id of the cell.
     * @return Returns the cell for the given Id.
     */
    public ICell getCell(String id) {
        ICell result = null;

        if (cells != null) {
            result = cells.get(id);
        }
        return result;
    }

    /**
     * Returns true if the model automatically update parents of edges so that
     * the edge is contained in the nearest-common-ancestor of its terminals.
     *
     * @return Returns true if the model maintains edge parents.
     */
    public boolean isMaintainEdgeParent() {
        return maintainEdgeParent;
    }

    /**
     * Specifies if the model automatically updates parents of edges so that
     * the edge is contained in the nearest-common-ancestor of its terminals.
     *
     * @param maintainEdgeParent Boolean indicating if the model should
     *                           maintain edge parents.
     */
    public void setMaintainEdgeParent(boolean maintainEdgeParent) {
        this.maintainEdgeParent = maintainEdgeParent;
    }

    /**
     * Returns true if the model automatically creates Ids and resolves Id
     * collisions.
     *
     * @return Returns true if the model creates Ids.
     */
    public boolean isCreateIds() {
        return createIds;
    }

    /**
     * Specifies if the model automatically creates Ids for new cells and
     * resolves Id collisions.
     *
     * @param value Boolean indicating if the model should created Ids.
     */
    public void setCreateIds(boolean value) {
        createIds = value;
    }

    /**
     * Inner callback to change the root of the model and update the internal
     * datastructures, such as cells and nextId. Returns the previous root.
     */
    protected ICell rootChanged(ICell root) {
        ICell oldRoot = this.root;
        this.root = root;

        // Resets counters and datastructures
        nextId = 0;
        cells = null;
        cellAdded(root);

        return oldRoot;
    }

    /**
     * Clones the children of the source cell into the given target cell in
     * this model and adds an entry to the mapping that maps from the source
     * cell to the target cell with the same id or the clone of the source cell
     * that was inserted into this model.
     */
    protected void mergeChildrenImpl(ICell from, ICell to, boolean cloneAllEdges,
                                     HashMap<ICell, ICell> mapping) throws CloneNotSupportedException {
        beginUpdate();
        try {
            int childCount = from.getChildCount();

            for (int i = 0; i < childCount; i++) {
                ICell cell = from.getChildAt(i);
                String id = cell.getId();
                ICell target = (id != null && (!isEdge(cell) || !cloneAllEdges)) ? getCell(id) : null;

                // Clones and adds the child if no cell exists for the id
                if (target == null) {
                    Cell clone = (Cell) cell.clone();
                    clone.setId(id);

                    // Do *NOT* use model.add as this will move the edge away
                    // from the parent in updateEdgeParent if maintainEdgeParent
                    // is enabled in the target model
                    target = to.insert(clone);
                    cellAdded(target);
                }

                // Stores the mapping for later reconnecting edges
                mapping.put(cell, target);

                // Recurses
                mergeChildrenImpl(cell, target, cloneAllEdges, mapping);
            }
        } finally {
            endUpdate();
        }
    }

    @Override
    public ICell getRoot() {
        return root;
    }

    @Override
    public ICell setRoot(ICell root) {
        execute(new RootChange(this, root));

        return root;
    }

    @Override
    public List<ICell> cloneCells(List<ICell> cells, boolean includeChildren) {
        Map<ICell, ICell> mapping = new HashMap<>();
        List<ICell> clones = new ArrayList<>();

        for (ICell cell : cells) {
            try {
                clones.add(cloneCell(cell, mapping));
            } catch (CloneNotSupportedException e) {
                log.log(Level.SEVERE, "Failed to clone cells", e);
            }
        }

        for (int i = 0; i < cells.size(); i++) {
            restoreClone(clones.get(i), cells.get(i), mapping);
        }

        return clones;
    }

    @Override
    public boolean isAncestor(ICell parent, ICell child) {
        while (child != null && child != parent) {
            child = getParent(child);
        }

        return child == parent;
    }

    @Override
    public boolean contains(ICell cell) {
        return isAncestor(getRoot(), cell);
    }

    @Override
    public ICell getParent(ICell child) {
        return (child != null) ? child.getParent() : null;
    }

    @Override
    public ICell add(ICell parent, ICell child, int index) {
        if (child != parent && parent != null && child != null) {
            boolean parentChanged = parent != getParent(child);
            execute(new ChildChange(this, parent, child, index));

            // Maintains the edges parents by moving the edges
            // into the nearest common ancestor of its
            // terminals
            if (maintainEdgeParent && parentChanged) {
                updateEdgeParents(child);
            }
        }

        return child;
    }

    @Override
    public ICell remove(ICell cell) {
        if (cell == root) {
            setRoot(null);
        } else if (getParent(cell) != null) {
            execute(new ChildChange(this, null, cell));
        }

        return cell;
    }

    @Override
    public int getChildCount(ICell cell) {
        return (cell != null) ? cell.getChildCount() : 0;
    }

    @Override
    public List<ICell> getChildren(ICell cell) {
        return (cell != null) ? cell.getChildren() : List.of();
    }

    @Override
    public ICell getChildAt(ICell parent, int index) {
        return (parent != null) ? parent.getChildAt(index) : null;
    }

    @Override
    public ICell getSource(ICell edge) {
        return edge.getSource();
    }

    @Override
    public void setSource(ICell edge, ICell source) {
        edge.setSource(source);
    }

    @Override
    public ICell getTarget(ICell edge) {
        return edge.getTarget();
    }

    @Override
    public void setTarget(ICell edge, ICell target) {
        edge.setTarget(target);
    }

    @Override
    public ICell getTerminal(ICell edge, boolean isSource) {
        return (edge != null) ? edge.getTerminal(isSource) : null;
    }

    @Override
    public ICell setTerminal(ICell edge, ICell terminal, boolean isSource) {
        boolean terminalChanged = terminal != getTerminal(edge, isSource);
        execute(new TerminalChange(this, edge, terminal, isSource));
        if (maintainEdgeParent && terminalChanged) {
            updateEdgeParent(edge, getRoot());
        }

        return terminal;
    }

    @Override
    public int getEdgeCount(ICell cell) {
        return (cell != null) ? cell.getEdgeCount() : 0;
    }

    @Override
    public ICell getEdgeAt(ICell parent, int index) {
        return (parent != null) ? parent.getEdgeAt(index) : null;
    }

    @Override
    public boolean isVertex(ICell cell) {
        return cell != null && cell.isVertex();
    }

    @Override
    public boolean isEdge(ICell cell) {
        return cell != null && cell.isEdge();
    }

    @Override
    public boolean isConnectable(ICell cell) {
        return cell == null || cell.isConnectable();
    }

    @Override
    public Object getValue(ICell cell) {
        return (cell != null) ? cell.getValue() : null;
    }

    @Override
    public Object setValue(ICell cell, Object value) {
        execute(new ValueChange(this, cell, value));

        return value;
    }

    @Override
    public Geometry getGeometry(ICell cell) {
        return (cell != null) ? cell.getGeometry() : null;
    }

    @Override
    public Geometry setGeometry(ICell cell, Geometry geometry) {
        if (geometry != getGeometry(cell)) {
            execute(new GeometryChange(this, cell, geometry));
        }

        return geometry;
    }

    @Override
    public String getStyle(ICell cell) {
        return (cell != null) ? cell.getStyle() : null;
    }

    @Override
    public String setStyle(ICell cell, String style) {
        if (style == null || !style.equals(getStyle(cell))) {
            execute(new StyleChange(this, cell, style));
        }

        return style;
    }

    @Override
    public boolean isCollapsed(ICell cell) {
        return cell != null && cell.isCollapsed();
    }

    @Override
    public boolean setCollapsed(ICell cell, boolean collapsed) {
        if (collapsed != isCollapsed(cell)) {
            execute(new CollapseChange(this, cell, collapsed));
        }

        return collapsed;
    }

    @Override
    public boolean isVisible(ICell cell) {
        return cell != null && cell.isVisible();
    }

    @Override
    public boolean setVisible(ICell cell, boolean visible) {
        if (visible != isVisible(cell)) {
            execute(new VisibleChange(this, cell, visible));
        }

        return visible;
    }

    @Override
    public void beginUpdate() {
        updateLevel++;
        fireEvent(new BeginUpdateEvent());
    }

    @Override
    public void endUpdate() {
        updateLevel--;

        if (!endingUpdate) {
            endingUpdate = updateLevel == 0;
            fireEvent(new EndUpdateEvent(currentEdit));

            try {
                if (endingUpdate && !currentEdit.isEmpty()) {
                    fireEvent(new BeforeUndoEvent(currentEdit));
                    UndoableEdit tmp = currentEdit;
                    currentEdit = createUndoableEdit();
                    tmp.dispatch();
                    fireEvent(new UndoEvent(tmp));
                }
            } finally {
                endingUpdate = false;
            }
        }
    }

    /**
     * Creates a new undoable edit.
     */
    protected UndoableEdit createUndoableEdit() {
        return new UndoableEdit(this) {
            @Override
            public void dispatch() {
                // LATER: Remove changes property (deprecated)
                ((GraphModel) source).fireEvent(new ChangeEvent(this, changes, null, null));
            }
        };
    }

    /**
     * Executes the given atomic change and adds it to the current edit.
     *
     * @param change Atomic change to be executed.
     */
    public void execute(AtomicGraphModelChange change) {
        change.execute();
        beginUpdate();
        currentEdit.add(change);
        fireEvent(new ExecuteEvent(change));
        endUpdate();
    }

    /**
     * Inner helper method for cloning cells recursively.
     */
    protected ICell cloneCell(ICell cell, Map<ICell, ICell> mapping) throws CloneNotSupportedException {
        if (cell != null) {
            ICell c = mapping.get(cell);
            if (c == null) {
                c = (ICell) cell.clone();
                mapping.put(cell, c);
            }
            return c;
        }

        return null;
    }

    /**
     * Inner helper method for restoring the connections in
     * a network of cloned cells.
     */
    protected void restoreClone(ICell clone, ICell cell, Map<ICell, ICell> mapping) {
        if (clone != null) {
            Object source = getTerminal(cell, true);

            if (source != null) {
                ICell tmp = mapping.get(source);

                if (tmp != null) {
                    tmp.insertEdge(clone, true);
                }
            }

            Object target = getTerminal(cell, false);

            if (target != null) {
                ICell tmp = mapping.get(target);

                if (tmp != null) {
                    tmp.insertEdge(clone, false);
                }
            }
        }

        int childCount = getChildCount(clone);

        for (int i = 0; i < childCount; i++) {
            restoreClone(getChildAt(clone, i), getChildAt(cell, i), mapping);
        }
    }

    /**
     * Invoked after a cell has been added to a parent. This recursively
     * creates an Id for the new cell and/or resolves Id collisions.
     *
     * @param cell Cell that has been added.
     */
    protected void cellAdded(ICell cell) {
        if (cell != null) {

            if (cell.getId() == null && isCreateIds()) {
                cell.setId(createId(cell));
            }

            if (cell.getId() != null) {
                Object collision = getCell(cell.getId());

                if (collision != cell) {
                    while (collision != null) {
                        cell.setId(createId(cell));
                        collision = getCell(cell.getId());
                    }

                    if (cells == null) {
                        cells = new HashMap<>();
                    }

                    cells.put(cell.getId(), cell);
                }
            }

            // Makes sure IDs of deleted cells are not reused
            try {
                int id = Integer.parseInt(cell.getId());
                nextId = Math.max(nextId, id + 1);
            } catch (NumberFormatException e) {
                // most likely this just means a custom cell id and that it's
                // not a simple number - should be safe to skip
                log.log(Level.FINEST, "Failed to parse cell id", e);
            }

            int childCount = cell.getChildCount();

            for (int i = 0; i < childCount; i++) {
                cellAdded(cell.getChildAt(i));
            }
        }
    }

    /**
     * Creates a new Id for the given cell and increments the global counter
     * for creating new Ids.
     *
     * @param cell Cell for which a new Id should be created.
     * @return Returns a new Id for the given cell.
     */
    public String createId(Object cell) {
        String id = String.valueOf(nextId);
        nextId++;

        return id;
    }

    /**
     * Invoked after a cell has been removed from the model. This recursively
     * removes the cell from its terminals and removes the mapping from the Id
     * to the cell.
     *
     * @param cell Cell that has been removed.
     */
    protected void cellRemoved(Object cell) {
        if (cell instanceof ICell c) {
            int childCount = c.getChildCount();

            for (int i = 0; i < childCount; i++) {
                cellRemoved(c.getChildAt(i));
            }

            if (cells != null && c.getId() != null) {
                cells.remove(c.getId());
            }
        }
    }

    /**
     * Inner callback to update the parent of a cell using Cell.insert
     * on the parent and return the previous parent.
     */
    protected ICell parentForCellChanged(ICell cell, Object parent, int index) {
        ICell previous = getParent(cell);

        if (parent != null) {
            if (parent != previous || previous.getIndex(cell) != index) {
                ((ICell) parent).insert(cell, index);
            }
        } else if (previous != null) {
            int oldIndex = previous.getIndex(cell);
            previous.remove(oldIndex);
        }

        // Checks if the previous parent was already in the
        // model and avoids calling cellAdded if it was.
        if (!contains(previous) && parent != null) {
            cellAdded(cell);
        } else if (parent == null) {
            cellRemoved(cell);
        }

        return previous;
    }

    /**
     * Inner helper function to update the terminal of the edge using
     * Cell.insertEdge and return the previous terminal.
     */
    protected ICell terminalForCellChanged(ICell edge, ICell terminal, boolean isSource) {
        ICell previous = getTerminal(edge, isSource);

        if (terminal != null) {
            terminal.insertEdge(edge, isSource);
        } else if (previous != null) {
            previous.removeEdge(edge, isSource);
        }

        return previous;
    }

    /**
     * Updates the parents of the edges connected to the given cell and all its
     * descendants so that each edge is contained in the nearest common
     * ancestor.
     *
     * @param cell Cell whose edges should be checked and updated.
     */
    public void updateEdgeParents(ICell cell) {
        updateEdgeParents(cell, getRoot());
    }

    /**
     * Updates the parents of the edges connected to the given cell and all its
     * descendants so that the edge is contained in the nearest-common-ancestor.
     *
     * @param cell Cell whose edges should be checked and updated.
     * @param root Root of the cell hierarchy that contains all cells.
     */
    public void updateEdgeParents(ICell cell, ICell root) {
        // Updates edges on children first
        int childCount = getChildCount(cell);

        for (int i = 0; i < childCount; i++) {
            ICell child = getChildAt(cell, i);
            updateEdgeParents(child, root);
        }

        // Updates the parents of all connected edges
        int edgeCount = getEdgeCount(cell);
        List<ICell> edges = new ArrayList<>(edgeCount);

        for (int i = 0; i < edgeCount; i++) {
            edges.add(getEdgeAt(cell, i));
        }

        for (ICell edge : edges) {
            // Updates edge parent if edge and child have
            // a common root node (does not need to be the
            // model root node)
            if (isAncestor(root, edge)) {
                updateEdgeParent(edge, root);
            }
        }
    }

    /**
     * Inner helper method to update the parent of the specified edge to the
     * nearest-common-ancestor of its two terminals.
     *
     * @param edge Specifies the edge to be updated.
     * @param root Current root of the model.
     */
    public void updateEdgeParent(ICell edge, ICell root) {
        ICell source = getTerminal(edge, true);
        ICell target = getTerminal(edge, false);
        ICell cell;

        // Uses the first non-relative descendants of the source terminal
        while (source != null && !isEdge(source) && getGeometry(source) != null && getGeometry(source).isRelative()) {
            source = getParent(source);
        }

        // Uses the first non-relative descendants of the target terminal
        while (target != null && !isEdge(target) && getGeometry(target) != null && getGeometry(target).isRelative()) {
            target = getParent(target);
        }

        if (isAncestor(root, source) && isAncestor(root, target)) {
            if (source == target) {
                cell = getParent(source);
            } else {
                cell = getNearestCommonAncestor(source, target);
            }

            // Keeps the edge in the same layer
            if (cell != null && (getParent(cell) != root || isAncestor(cell, edge)) && getParent(edge) != cell) {
                Geometry geo = getGeometry(edge);

                if (geo != null) {
                    PointDouble origin1 = getOrigin(getParent(edge));
                    PointDouble origin2 = getOrigin(cell);
                    double dx = origin2.getX() - origin1.getX();
                    double dy = origin2.getY() - origin1.getY();
                    geo = geo.clone();
                    geo.translate(-dx, -dy);
                    setGeometry(edge, geo);
                }

                add(cell, edge, getChildCount(cell));
            }
        }
    }

    /**
     * Returns the absolute, accumulated origin for the children inside the
     * given parent.
     */
    public PointDouble getOrigin(ICell cell) {
        PointDouble result = null;
        if (cell != null) {
            result = getOrigin(getParent(cell));
            if (!isEdge(cell)) {
                Geometry geo = getGeometry(cell);
                if (geo != null) {
                    result.setX(result.getX() + geo.getX());
                    result.setY(result.getY() + geo.getY());
                }
            }
        } else {
            result = new PointDouble();
        }

        return result;
    }

    /**
     * Returns the nearest common ancestor for the specified cells.
     *
     * @param cell1 Cell that specifies the first cell in the tree.
     * @param cell2 Cell that specifies the second cell in the tree.
     * @return Returns the nearest common ancestor of the given cells.
     */
    public ICell getNearestCommonAncestor(ICell cell1, ICell cell2) {
        if (cell1 != null && cell2 != null) {
            // Creates the cell path for the second cell
            String path = CellPath.create(cell2);

            if (path.length() > 0) {
                // Bubbles through the ancestors of the first
                // cell to find the nearest common ancestor.
                ICell cell = cell1;
                String current = CellPath.create(cell);

                while (cell != null) {
                    ICell parent = getParent(cell);

                    // Checks if the cell path is equal to the beginning
                    // of the given cell path
                    if (path.indexOf(current + CellPath.PATH_SEPARATOR) == 0 && parent != null) {
                        return cell;
                    }

                    current = CellPath.getParentPath(current);
                    cell = parent;
                }
            }
        }

        return null;
    }

    /**
     * Inner callback to update the user object of the given Cell
     * using Cell.setValue and return the previous value,
     * that is, the return value of Cell.getValue.
     */
    protected Object valueForCellChanged(ICell cell, Object value) {
        Object oldValue = cell.getValue();
        cell.setValue(value);

        return oldValue;
    }

    /**
     * Inner callback to update the Geometry of the given Cell using
     * Cell.setGeometry and return the previous Geometry.
     */
    protected Geometry geometryForCellChanged(ICell cell, Geometry geometry) {
        Geometry previous = getGeometry(cell);
        cell.setGeometry(geometry);

        return previous;
    }

    /**
     * Inner callback to update the style of the given Cell
     * using Cell.setStyle and return the previous style.
     */
    protected String styleForCellChanged(ICell cell, String style) {
        String previous = getStyle(cell);
        cell.setStyle(style);

        return previous;
    }

    /**
     * Inner callback to update the collapsed state of the
     * given Cell using Cell.setCollapsed and return
     * the previous collapsed state.
     */
    protected boolean collapsedStateForCellChanged(ICell cell, boolean collapsed) {
        boolean previous = isCollapsed(cell);
        cell.setCollapsed(collapsed);

        return previous;
    }

    /**
     * Sets the visible state of the given Cell using VisibleChange and
     * adds the change to the current transaction.
     */
    protected boolean visibleStateForCellChanged(ICell cell, boolean visible) {
        boolean previous = isVisible(cell);
        cell.setVisible(visible);

        return previous;
    }

    /**
     * Merges the children of the given cell into the given target cell inside
     * this model. All cells are cloned unless there is a corresponding cell in
     * the model with the same id, in which case the source cell is ignored and
     * all edges are connected to the corresponding cell in this model. Edges
     * are considered to have no identity and are always cloned unless the
     * cloneAllEdges flag is set to false, in which case edges with the same
     * id in the target model are reconnected to reflect the terminals of the
     * source edges.
     */
    public void mergeChildren(ICell from, ICell to, boolean cloneAllEdges) throws CloneNotSupportedException {
        beginUpdate();
        try {
            HashMap<ICell, ICell> mapping = new HashMap<>();
            mergeChildrenImpl(from, to, cloneAllEdges, mapping);

            // Post-processes all edges in the mapping and
            // reconnects the terminals to the corresponding
            // cells in the target model

            for (ICell edge : mapping.keySet()) {
                ICell cell = mapping.get(edge);
                ICell terminal = getTerminal(edge, true);

                if (terminal != null) {
                    terminal = mapping.get(terminal);
                    setTerminal(cell, terminal, true);
                }

                terminal = getTerminal(edge, false);

                if (terminal != null) {
                    terminal = mapping.get(terminal);
                    setTerminal(cell, terminal, false);
                }
            }
        } finally {
            endUpdate();
        }
    }

    /**
     * Initializes the currentEdit field if the model is deserialized.
     */
    @Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        currentEdit = createUndoableEdit();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [");
        builder.append("root=");
        builder.append(root);
        builder.append(", cells=");

        if (cells != null) {
            builder.append("<");
            builder.append(cells.size());
            builder.append(" entries>");
        } else {
            builder.append("null");
        }

        builder.append(", maintainEdgeParent=");
        builder.append(maintainEdgeParent);
        builder.append(", createIds=");
        builder.append(createIds);
        builder.append(", nextId=");
        builder.append(nextId);
        builder.append(", currentEdit=");
        builder.append(currentEdit);
        builder.append(", updateLevel=");
        builder.append(updateLevel);
        builder.append(", endingUpdate=");
        builder.append(endingUpdate);
        builder.append("]");

        return builder.toString();
    }
}
