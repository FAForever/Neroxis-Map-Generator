/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.canvas.ICanvas;
import com.faforever.neroxis.ngraph.canvas.ImageCanvas;
import com.faforever.neroxis.ngraph.event.AddCellsEvent;
import com.faforever.neroxis.ngraph.event.AlignCellsEvent;
import com.faforever.neroxis.ngraph.event.CellConnectedEvent;
import com.faforever.neroxis.ngraph.event.CellsAddedEvent;
import com.faforever.neroxis.ngraph.event.CellsFoldedEvent;
import com.faforever.neroxis.ngraph.event.CellsMovedEvent;
import com.faforever.neroxis.ngraph.event.CellsOrderedEvent;
import com.faforever.neroxis.ngraph.event.CellsRemovedEvent;
import com.faforever.neroxis.ngraph.event.CellsResizedEvent;
import com.faforever.neroxis.ngraph.event.ChangeEvent;
import com.faforever.neroxis.ngraph.event.ConnectCellEvent;
import com.faforever.neroxis.ngraph.event.DownEvent;
import com.faforever.neroxis.ngraph.event.EventSource;
import com.faforever.neroxis.ngraph.event.FlipEdgeEvent;
import com.faforever.neroxis.ngraph.event.FoldCellsEvent;
import com.faforever.neroxis.ngraph.event.GroupCellsEvent;
import com.faforever.neroxis.ngraph.event.MoveCellsEvent;
import com.faforever.neroxis.ngraph.event.OrderCellsEvent;
import com.faforever.neroxis.ngraph.event.RemoveCellsEvent;
import com.faforever.neroxis.ngraph.event.RemoveCellsFromParentEvent;
import com.faforever.neroxis.ngraph.event.RepaintEvent;
import com.faforever.neroxis.ngraph.event.ResizeCellsEvent;
import com.faforever.neroxis.ngraph.event.RootEvent;
import com.faforever.neroxis.ngraph.event.ScaleAndTranslateEvent;
import com.faforever.neroxis.ngraph.event.ScaleEvent;
import com.faforever.neroxis.ngraph.event.SplitEdgeEvent;
import com.faforever.neroxis.ngraph.event.ToggleCellsEvent;
import com.faforever.neroxis.ngraph.event.TranslateEvent;
import com.faforever.neroxis.ngraph.event.UngroupCellsEvent;
import com.faforever.neroxis.ngraph.event.UpEvent;
import com.faforever.neroxis.ngraph.event.UpdateCellSizeEvent;
import com.faforever.neroxis.ngraph.model.Cell;
import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.GraphModel;
import com.faforever.neroxis.ngraph.model.GraphModel.ChildChange;
import com.faforever.neroxis.ngraph.model.GraphModel.CollapseChange;
import com.faforever.neroxis.ngraph.model.GraphModel.GeometryChange;
import com.faforever.neroxis.ngraph.model.GraphModel.RootChange;
import com.faforever.neroxis.ngraph.model.GraphModel.StyleChange;
import com.faforever.neroxis.ngraph.model.GraphModel.TerminalChange;
import com.faforever.neroxis.ngraph.model.GraphModel.ValueChange;
import com.faforever.neroxis.ngraph.model.GraphModel.VisibleChange;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Point;
import com.faforever.neroxis.ngraph.util.Rectangle;
import com.faforever.neroxis.ngraph.util.Resources;
import com.faforever.neroxis.ngraph.util.StyleUtils;
import com.faforever.neroxis.ngraph.util.UndoableEdit.UndoableChange;
import com.faforever.neroxis.ngraph.util.Utils;
import java.awt.Graphics;
import java.awt.Shape;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import lombok.Getter;
import org.w3c.dom.Element;

/**
 * Implements a graph object that allows to create diagrams from a graph model
 * and stylesheet.
 *
 * <h3>Images</h3>
 * To create an image from a graph, use the following code for a given
 * XML document (doc) and File (file):
 *
 * <code>
 * Image img = CellRenderer.createBufferedImage(
 * graph, null, 1, Color.WHITE, false, null);
 * ImageIO.write(img, "png", file);
 * </code>
 * <p>
 * If the XML is given as a string rather than a document, the document can
 * be obtained using Utils.parse.
 * <p>
 * This class fires the following events:
 * <p>
 * Event.ROOT fires if the root in the model has changed. This event has no
 * properties.
 * <p>
 * Event.ALIGN_CELLS fires between begin- and endUpdate in alignCells. The
 * <code>cells</code> and <code>align</code> properties contain the respective
 * arguments that were passed to alignCells.
 * <p>
 * Event.FLIP_EDGE fires between begin- and endUpdate in flipEdge. The
 * <code>edge</code> property contains the edge passed to flipEdge.
 * <p>
 * Event.ORDER_CELLS fires between begin- and endUpdate in orderCells. The
 * <code>cells</code> and <code>back</code> properties contain the respective
 * arguments that were passed to orderCells.
 * <p>
 * Event.CELLS_ORDERED fires between begin- and endUpdate in cellsOrdered.
 * The <code>cells</code> and <code>back</code> arguments contain the
 * respective arguments that were passed to cellsOrdered.
 * <p>
 * Event.GROUP_CELLS fires between begin- and endUpdate in groupCells. The
 * <code>group</code>, <code>cells</code> and <code>border</code> arguments
 * contain the respective arguments that were passed to groupCells.
 * <p>
 * Event.UNGROUP_CELLS fires between begin- and endUpdate in ungroupCells.
 * The <code>cells</code> property contains the array of cells that was passed
 * to ungroupCells.
 * <p>
 * Event.REMOVE_CELLS_FROM_PARENT fires between begin- and endUpdate in
 * removeCellsFromParent. The <code>cells</code> property contains the array of
 * cells that was passed to removeCellsFromParent.
 * <p>
 * Event.ADD_CELLS fires between begin- and endUpdate in addCells. The
 * <code>cells</code>, <code>parent</code>, <code>index</code>,
 * <code>source</code> and <code>target</code> properties contain the
 * respective arguments that were passed to addCells.
 * <p>
 * Event.CELLS_ADDED fires between begin- and endUpdate in cellsAdded. The
 * <code>cells</code>, <code>parent</code>, <code>index</code>,
 * <code>source</code>, <code>target</code> and <code>absolute</code>
 * properties contain the respective arguments that were passed to cellsAdded.
 * <p>
 * Event.REMOVE_CELLS fires between begin- and endUpdate in removeCells. The
 * <code>cells</code> and <code>includeEdges</code> arguments contain the
 * respective arguments that were passed to removeCells.
 * <p>
 * Event.CELLS_REMOVED fires between begin- and endUpdate in cellsRemoved.
 * The <code>cells</code> argument contains the array of cells that was
 * removed.
 * <p>
 * Event.SPLIT_EDGE fires between begin- and endUpdate in splitEdge. The
 * <code>edge</code> property contains the edge to be splitted, the
 * <code>cells</code>, <code>newEdge</code>, <code>dx</code> and
 * <code>dy</code> properties contain the respective arguments that were passed
 * to splitEdge.
 * <p>
 * Event.TOGGLE_CELLS fires between begin- and endUpdate in toggleCells. The
 * <code>show</code>, <code>cells</code> and <code>includeEdges</code>
 * properties contain the respective arguments that were passed to toggleCells.
 * <p>
 * Event.FOLD_CELLS fires between begin- and endUpdate in foldCells. The
 * <code>collapse</code>, <code>cells</code> and <code>recurse</code>
 * properties contain the respective arguments that were passed to foldCells.
 * <p>
 * Event.CELLS_FOLDED fires between begin- and endUpdate in cellsFolded. The
 * <code>collapse</code>, <code>cells</code> and <code>recurse</code>
 * properties contain the respective arguments that were passed to cellsFolded.
 * <p>
 * Event.UPDATE_CELL_SIZE fires between begin- and endUpdate in
 * updateCellSize. The <code>cell</code> and <code>ignoreChildren</code>
 * properties contain the respective arguments that were passed to
 * updateCellSize.
 * <p>
 * Event.RESIZE_CELLS fires between begin- and endUpdate in resizeCells. The
 * <code>cells</code> and <code>bounds</code> properties contain the respective
 * arguments that were passed to resizeCells.
 * <p>
 * Event.CELLS_RESIZED fires between begin- and endUpdate in cellsResized.
 * The <code>cells</code> and <code>bounds</code> properties contain the
 * respective arguments that were passed to cellsResized.
 * <p>
 * Event.MOVE_CELLS fires between begin- and endUpdate in moveCells. The
 * <code>cells</code>, <code>dx</code>, <code>dy</code>, <code>clone</code>,
 * <code>target</code> and <code>location</code> properties contain the
 * respective arguments that were passed to moveCells.
 * <p>
 * Event.CELLS_MOVED fires between begin- and endUpdate in cellsMoved. The
 * <code>cells</code>, <code>dx</code>, <code>dy</code> and
 * <code>disconnect</code> properties contain the respective arguments that
 * were passed to cellsMoved.
 * <p>
 * Event.CONNECT_CELL fires between begin- and endUpdate in connectCell. The
 * <code>edge</code>, <code>terminal</code> and <code>source</code> properties
 * contain the respective arguments that were passed to connectCell.
 * <p>
 * Event.CELL_CONNECTED fires between begin- and endUpdate in cellConnected.
 * The <code>edge</code>, <code>terminal</code> and <code>source</code>
 * properties contain the respective arguments that were passed to
 * cellConnected.
 * <p>
 * Event.REPAINT fires if a repaint was requested by calling repaint. The
 * <code>region</code> property contains the optional Rectangle that was
 * passed to repaint to define the dirty region.
 */
@SuppressWarnings("unused")
@Getter
public class Graph extends EventSource {
    private static final Logger log = Logger.getLogger(Graph.class.getName());
    /**
     * Property change event handling.
     */
    protected PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /**
     * Holds the model that contains the cells to be displayed.
     */
    protected IGraphModel model;

    /**
     * Holds the view that caches the cell states.
     */
    protected GraphView view;

    /**
     * Holds the stylesheet that defines the appearance of the cells.
     */
    protected Stylesheet stylesheet;

    /**
     * Holds the <GraphSelection> that models the current selection.
     */
    protected GraphSelectionModel selectionModel;

    /**
     * Specifies the grid size. Default is 10.
     */
    protected int gridSize = 10;

    /**
     * Specifies if the grid is enabled. Default is true.
     */
    protected boolean gridEnabled = true;

    /**
     * Specifies if ports are enabled. This is used in <cellConnected> to update
     * the respective style. Default is true.
     */
    protected boolean portsEnabled = true;

    /**
     * Value returned by getOverlap if isAllowOverlapParent returns
     * true for the given cell. getOverlap is used in keepInside if
     * isKeepInsideParentOnMove returns true. The value specifies the
     * portion of the child which is allowed to overlap the parent.
     */
    protected double defaultOverlap = 0.5;

    /**
     * Specifies the default parent to be used to insert new cells.
     * This is used in getDefaultParent. Default is null.
     */
    protected ICell defaultParent;

    /**
     * Specifies the alternate edge style to be used if the main control point
     * on an edge is being doubleclicked. Default is null.
     */
    protected String alternateEdgeStyle;

    /**
     * Specifies the return value for isEnabled. Default is true.
     */
    protected boolean enabled = true;

    /**
     * Specifies the return value for isCell(s)Locked. Default is false.
     */
    protected boolean cellsLocked = false;

    /**
     * Specifies if editing is allowed in this graph. Default is true.
     */
    protected boolean cellsEditable = true;

    /**
     * Specifies if resizing is allowed in this graph. Default is true.
     */
    protected boolean cellsResizable = true;

    /**
     * Specifies the return value for isCell(s)Movable. Default is true.
     */
    protected boolean cellsMovable = true;

    /**
     * Specifies the return value for isCell(s)Bendable. Default is true.
     */
    protected boolean cellsBendable = true;

    /**
     * Specifies the return value for isCell(s)Selectable. Default is true.
     */
    protected boolean cellsSelectable = true;

    /**
     * Specifies the return value for isCell(s)Deletable. Default is true.
     */
    protected boolean cellsDeletable = true;

    /**
     * Specifies if the graph should allow cloning of cells by holding down the
     * control key while cells are being moved. Default is true.
     */
    protected boolean cellsCloneable = true;

    /**
     * Specifies if the graph should allow disconnecting of edges. Default is true.
     */
    protected boolean cellsDisconnectable = true;

    /**
     * Specifies the return value for isLabel(s)Clipped. Default is false.
     */
    protected boolean labelsClipped = false;

    /**
     * Specifies the return value for edges in isLabelMovable. Default is true.
     */
    protected boolean edgeLabelsMovable = true;

    /**
     * Specifies the return value for vertices in isLabelMovable. Default is false.
     */
    protected boolean vertexLabelsMovable = false;

    /**
     * Specifies if the graph allows drop into other cells. Default is true.
     */
    protected boolean dropEnabled = true;

    /**
     * Specifies if dropping onto edges should be enabled. Default is true.
     */
    protected boolean splitEnabled = true;

    /**
     * Specifies if the graph should automatically update the cell size
     * after an edit. Default is false.
     */
    protected boolean autoSizeCells = false;

    /**
     * <Rectangle> that specifies the area in which all cells in the
     * diagram should be placed. Uses in getMaximumGraphBounds. Use a width
     * or height of 0 if you only want to give a upper, left corner.
     */
    protected Rectangle maximumGraphBounds = null;

    /**
     * Rectangle that specifies the minimum size of the graph canvas inside
     * the scrollpane.
     */
    protected Rectangle minimumGraphSize = null;

    /**
     * Border to be added to the bottom and right side when the container is
     * being resized after the graph has been changed. Default is 0.
     */
    protected int border = 0;

    /**
     * Specifies if edges should appear in the foreground regardless of their
     * order in the model. This has precendence over keepEdgeInBackground
     * Default is false.
     */
    protected boolean keepEdgesInForeground = false;

    /**
     * Specifies if edges should appear in the background regardless of their
     * order in the model. Default is false.
     */
    protected boolean keepEdgesInBackground = false;

    /**
     * Specifies if the cell size should be changed to the preferred size when
     * a cell is first collapsed. Default is true.
     */
    protected boolean collapseToPreferredSize = true;

    /**
     * Specifies if negative coordinates for vertices are allowed. Default is true.
     */
    protected boolean allowNegativeCoordinates = true;

    /**
     * Specifies the return value for isConstrainChildren. Default is true.
     */
    protected boolean constrainChildren = true;

    /**
     * Specifies if a parent should contain the child bounds after a resize of
     * the child. Default is true.
     */
    protected boolean extendParents = true;

    /**
     * Specifies if parents should be extended according to the <extendParents>
     * switch if cells are added. Default is true.
     */
    protected boolean extendParentsOnAdd = true;

    /**
     * Specifies if the scale and translate should be reset if
     * the root changes in the model. Default is true.
     */
    protected boolean resetViewOnRootChange = true;

    /**
     * Specifies if loops (aka self-references) are allowed.
     * Default is false.
     */
    protected boolean resetEdgesOnResize = false;

    /**
     * Specifies if edge control points should be reset after
     * the move of a connected cell. Default is false.
     */
    protected boolean resetEdgesOnMove = false;

    /**
     * Specifies if edge control points should be reset after
     * the the edge has been reconnected. Default is true.
     */
    protected boolean resetEdgesOnConnect = true;

    /**
     * Specifies if loops (aka self-references) are allowed.
     * Default is false.
     */
    protected boolean allowLoops = false;

    /**
     * Specifies the multiplicities to be used for validation of the graph.
     */
    protected Multiplicity[] multiplicities;

    /**
     * Specifies the default style for loops.
     */
    protected EdgeStyle.EdgeStyleFunction defaultLoopStyle = EdgeStyle.Loop;

    /**
     * Specifies if multiple edges in the same direction between
     * the same pair of vertices are allowed. Default is true.
     */
    protected boolean multigraph = true;
    /**
     * Specifies if edges are connectable. Default is false.
     * This overrides the connectable field in edges.
     */
    protected boolean connectableEdges = false;
    /**
     * Specifies if edges with disconnected terminals are
     * allowed in the graph. Default is false.
     */
    protected boolean allowDanglingEdges = false;

    /**
     * Specifies if edges that are cloned should be validated and only inserted
     * if they are valid. Default is true.
     */
    protected boolean cloneInvalidEdges = false;

    /**
     * Specifies if edges should be disconnected from their terminals when they
     * are moved. Default is true.
     */
    protected boolean disconnectOnMove = true;

    /**
     * Specifies if labels should be visible. This is used in
     * getLabel. Default is true.
     */
    protected boolean labelsVisible = true;

    /**
     * Specifies the return value for isHtmlLabel. Default is false.
     */
    protected boolean htmlLabels = false;

    /**
     * Specifies if nesting of swimlanes is allowed. Default is true.
     */
    protected boolean swimlaneNesting = true;

    /**
     * Specifies the maximum number of changes that should be processed to find
     * the dirty region. If the number of changes is larger, then the complete
     * grah is repainted. A value of zero will always compute the dirty region
     * for any number of changes. Default is 1000.
     */
    protected int changesRepaintThreshold = 1000;

    /**
     * Specifies if the origin should be automatically updated.
     */
    protected boolean autoOrigin = false;
    /**
     * Holds the current automatic origin.
     */
    protected Point origin = new Point();
    /**
     * Fires repaint events for full repaints.
     */
    protected IEventListener<?> fullRepaintHandler = (sender, evt) -> repaint();
    /**
     * Fires repaint events for full repaints.
     */
    protected IEventListener<?> updateOriginHandler = (sender, evt) -> {
        if (isAutoOrigin()) {
            updateOrigin();
        }
    };
    /**
     * Fires repaint events for model changes.
     */
    protected IEventListener<ChangeEvent> graphModelChangeHandler = (sender, evt) -> {
        Rectangle dirty = graphModelChanged((IGraphModel) sender, evt.getEdit().getChanges());
        repaint(dirty);
    };

    /**
     * Constructs a new graph with an empty
     * {@link GraphModel}.
     */
    public Graph() {
        this(null, null);
    }

    /**
     * Constructs a new graph for the specified model. If no model is
     * specified, then a new, empty {@link GraphModel} is
     * used.
     *
     * @param model Model that contains the graph data
     */
    public Graph(IGraphModel model) {
        this(model, null);
    }

    /**
     * Constructs a new graph for the specified model. If no model is
     * specified, then a new, empty {@link GraphModel} is
     * used.
     *
     * @param stylesheet The stylesheet to use for the graph.
     */
    public Graph(Stylesheet stylesheet) {
        this(null, stylesheet);
    }

    /**
     * Constructs a new graph for the specified model. If no model is
     * specified, then a new, empty {@link GraphModel} is
     * used.
     *
     * @param model Model that contains the graph data
     */
    public Graph(IGraphModel model, Stylesheet stylesheet) {
        selectionModel = createSelectionModel();
        setModel((model != null) ? model : new GraphModel());
        setStylesheet((stylesheet != null) ? stylesheet : createStylesheet());
        setView(createGraphView());
    }

    /**
     * Constructs a new selection model to be used in this graph.
     */
    protected GraphSelectionModel createSelectionModel() {
        return new GraphSelectionModel(this);
    }

    /**
     * Constructs a new stylesheet to be used in this graph.
     */
    protected Stylesheet createStylesheet() {
        return new Stylesheet();
    }

    /**
     * Constructs a new view to be used in this graph.
     */
    protected GraphView createGraphView() {
        return new GraphView(this);
    }

    /**
     * Sets the graph model that contains the data, and fires an
     * Event.CHANGE followed by an Event.REPAINT event.
     *
     * @param value Model that contains the graph data
     */
    public void setModel(IGraphModel value) {
        if (model != null) {
            model.removeListener(graphModelChangeHandler);
        }

        IGraphModel oldModel = model;
        model = value;

        if (view != null) {
            view.revalidate();
        }
        model.addListener(ChangeEvent.class, graphModelChangeHandler);
        changeSupport.firePropertyChange("model", oldModel, model);
        repaint();
    }

    /**
     * Sets the view that contains the cell states.
     *
     * @param value View that contains the cell states
     */
    public void setView(GraphView value) {
        if (view != null) {
            view.removeListener(fullRepaintHandler);
            view.removeListener(updateOriginHandler);
        }

        GraphView oldView = view;
        view = value;

        if (view != null) {
            view.revalidate();
            // Listens to changes in the view
            view.addListener(ScaleEvent.class, (IEventListener<ScaleEvent>) fullRepaintHandler);
            view.addListener(ScaleEvent.class, (IEventListener<ScaleEvent>) updateOriginHandler);
            view.addListener(TranslateEvent.class, (IEventListener<TranslateEvent>) fullRepaintHandler);
            view.addListener(ScaleAndTranslateEvent.class, (IEventListener<ScaleAndTranslateEvent>) fullRepaintHandler);
            view.addListener(ScaleAndTranslateEvent.class, (IEventListener<ScaleAndTranslateEvent>) updateOriginHandler);
            view.addListener(UpEvent.class, (IEventListener<UpEvent>) fullRepaintHandler);
            view.addListener(DownEvent.class, (IEventListener<DownEvent>) fullRepaintHandler);
        }

        changeSupport.firePropertyChange("view", oldView, view);
    }

    /**
     * Sets the stylesheet that provides the style.
     *
     * @param value Stylesheet that provides the style.
     */
    public void setStylesheet(Stylesheet value) {
        Stylesheet oldValue = stylesheet;
        stylesheet = value;

        changeSupport.firePropertyChange("stylesheet", oldValue, stylesheet);
    }

    /**
     * Returns the cells to be selected for the given list of changes.
     */
    public void addTopmostVerticesAndEdges(ICell cell, List<ICell> cells) {
        if (!cells.contains(cell) && model.contains(cell)) {
            if (model.isVertex(cell) || model.isEdge(cell)) {
                cells.add(cell);
            } else {
                int childCount = model.getChildCount(cell);

                for (int i = 0; i < childCount; i++) {
                    addTopmostVerticesAndEdges(model.getChildAt(cell, i), cells);
                }
            }
        }
    }

    /**
     * Returns the cells to be selected for the given list of changes.
     */
    public List<ICell> getSelectionCellsForChanges(List<UndoableChange> changes) {
        List<ICell> cells = new ArrayList<>();

        for (UndoableChange change : changes) {
            if (change instanceof ChildChange) {
                addTopmostVerticesAndEdges(((ChildChange) change).getChild(), cells);
            } else if (change instanceof TerminalChange) {
                addTopmostVerticesAndEdges(((TerminalChange) change).getCell(), cells);
            } else if (change instanceof ValueChange) {
                addTopmostVerticesAndEdges(((ValueChange) change).getCell(), cells);
            } else if (change instanceof StyleChange) {
                addTopmostVerticesAndEdges(((StyleChange) change).getCell(), cells);
            } else if (change instanceof GeometryChange) {
                addTopmostVerticesAndEdges(((GeometryChange) change).getCell(), cells);
            } else if (change instanceof CollapseChange) {
                addTopmostVerticesAndEdges(((CollapseChange) change).getCell(), cells);
            } else if (change instanceof VisibleChange) {
                VisibleChange vc = (VisibleChange) change;

                if (vc.isVisible()) {
                    addTopmostVerticesAndEdges(((VisibleChange) change).getCell(), cells);
                }
            }
        }

        return cells;
    }

    /**
     * Called when the graph model changes. Invokes processChange on each
     * item of the given array to update the view accordingly.
     */
    public Rectangle graphModelChanged(IGraphModel sender, List<UndoableChange> changes) {
        int thresh = getChangesRepaintThreshold();
        boolean ignoreDirty = thresh > 0 && changes.size() > thresh;

        // Ignores dirty rectangle if there was a root change
        if (!ignoreDirty) {

            for (UndoableChange change : changes) {
                if (change instanceof RootChange) {
                    ignoreDirty = true;
                    break;
                }
            }
        }

        Rectangle dirty = processChanges(changes, true, ignoreDirty);
        view.validate();

        if (isAutoOrigin()) {
            updateOrigin();
        }

        if (!ignoreDirty) {
            Rectangle tmp = processChanges(changes, false, ignoreDirty);

            if (tmp != null) {
                if (dirty == null) {
                    dirty = tmp;
                } else {
                    dirty.add(tmp);
                }
            }
        }

        updateSelection();

        return dirty;
    }

    /**
     * Function: updateSelection
     * <p>
     * Removes selection cells that are not in the model from the selection.
     */
    protected void updateSelection() {
        List<ICell> removed = getSelectionCells().stream().filter(cell -> !model.contains(cell)).collect(Collectors.toList());

        removeSelectionCells(removed);
    }

    /**
     * Extends the canvas by doing another validation with a shifted
     * global translation if the bounds of the graph are below (0,0).
     * <p>
     * The first validation is required to compute the bounds of the graph
     * while the second validation is required to apply the new translate.
     */
    protected void updateOrigin() {
        Rectangle bounds = getGraphBounds();

        if (bounds != null) {
            double scale = getView().getScale();
            double x = bounds.getX() / scale - getBorder();
            double y = bounds.getY() / scale - getBorder();

            if (x < 0 || y < 0) {
                double x0 = Math.min(0, x);
                double y0 = Math.min(0, y);

                origin.setX(origin.getX() + x0);
                origin.setY(origin.getY() + y0);

                Point t = getView().getTranslate();
                getView().setTranslate(new Point(t.getX() - x0, t.getY() - y0));
            } else if ((x > 0 || y > 0) && (origin.getX() < 0 || origin.getY() < 0)) {
                double dx = Math.min(-origin.getX(), x);
                double dy = Math.min(-origin.getY(), y);

                origin.setX(origin.getX() + dx);
                origin.setY(origin.getY() + dy);

                Point t = getView().getTranslate();
                getView().setTranslate(new Point(t.getX() - dx, t.getY() - dy));
            }
        }
    }

    /**
     * Processes the changes and returns the minimal rectangle to be
     * repainted in the buffer. A return value of null means no repaint
     * is required.
     */
    public Rectangle processChanges(List<UndoableChange> changes, boolean invalidate, boolean ignoreDirty) {
        Rectangle bounds = null;

        for (UndoableChange change : changes) {
            Rectangle rect = processChange(change, invalidate, ignoreDirty);

            if (bounds == null) {
                bounds = rect;
            } else {
                bounds.add(rect);
            }
        }

        return bounds;
    }

    /**
     * Processes the given change and invalidates the respective cached data
     * in <view>. This fires a <root> event if the root has changed in the
     * model.
     */
    public Rectangle processChange(UndoableChange change, boolean invalidate, boolean ignoreDirty) {
        Rectangle result = null;

        if (change instanceof RootChange) {
            result = (ignoreDirty) ? null : getGraphBounds();

            if (invalidate) {
                clearSelection();
                removeStateForCell(((RootChange) change).getPrevious());

                if (isResetViewOnRootChange()) {
                    view.setEventsEnabled(false);

                    try {
                        view.scaleAndTranslate(1, 0, 0);
                    } finally {
                        view.setEventsEnabled(true);
                    }
                }

            }
            fireEvent(new RootEvent());
        } else if (change instanceof ChildChange) {
            ChildChange cc = (ChildChange) change;

            // Repaints the parent area if it is a rendered cell (vertex or
            // edge) otherwise only the child area is repainted, same holds
            // if the parent and previous are the same object, in which case
            // only the child area needs to be repainted (change of order)
            if (!ignoreDirty) {
                if (cc.getParent() != cc.getPrevious()) {
                    if (model.isVertex(cc.getParent()) || model.isEdge(cc.getParent())) {
                        result = getBoundingBox(cc.getParent(), true, true);
                    }

                    if (model.isVertex(cc.getPrevious()) || model.isEdge(cc.getPrevious())) {
                        if (result != null) {
                            result.add(getBoundingBox(cc.getPrevious(), true, true));
                        } else {
                            result = getBoundingBox(cc.getPrevious(), true, true);
                        }
                    }
                }

                if (result == null) {
                    result = getBoundingBox(cc.getChild(), true, true);
                }
            }

            if (invalidate) {
                if (model.contains(cc.getParent())) {
                    view.clear(cc.getChild(), false, true);
                } else {
                    removeStateForCell(cc.getChild());
                }
            }
        } else if (change instanceof TerminalChange) {
            ICell cell = ((TerminalChange) change).getCell();

            if (!ignoreDirty) {
                result = getBoundingBox(cell, true);
            }

            if (invalidate) {
                view.invalidate(cell);
            }
        } else if (change instanceof ValueChange) {
            ICell cell = ((ValueChange) change).getCell();

            if (!ignoreDirty) {
                result = getBoundingBox(cell);
            }

            if (invalidate) {
                view.clear(cell, false, false);
            }
        } else if (change instanceof StyleChange) {
            ICell cell = ((StyleChange) change).getCell();

            if (!ignoreDirty) {
                result = getBoundingBox(cell, true);
            }

            if (invalidate) {
                // TODO: Add includeEdges argument to clear method for
                // not having to call invalidate in this case (where it
                // is possible that the perimeter has changed, which
                // means the connected edges need to be invalidated)
                view.clear(cell, false, false);
                view.invalidate(cell);
            }
        } else if (change instanceof GeometryChange) {
            ICell cell = ((GeometryChange) change).getCell();

            if (!ignoreDirty) {
                result = getBoundingBox(cell, true, true);
            }

            if (invalidate) {
                view.invalidate(cell);
            }
        } else if (change instanceof CollapseChange) {
            ICell cell = ((CollapseChange) change).getCell();

            if (!ignoreDirty) {
                result = getBoundingBox(((CollapseChange) change).getCell(), true, true);
            }

            if (invalidate) {
                removeStateForCell(cell);
            }
        } else if (change instanceof VisibleChange) {
            ICell cell = ((VisibleChange) change).getCell();

            if (!ignoreDirty) {
                result = getBoundingBox(((VisibleChange) change).getCell(), true, true);
            }

            if (invalidate) {
                removeStateForCell(cell);
            }
        }

        return result;
    }

    /**
     * Removes all cached information for the given cell and its descendants.
     * This is called when a cell was removed from the model.
     *
     * @param cell Cell that was removed from the model.
     */
    protected void removeStateForCell(ICell cell) {
        int childCount = model.getChildCount(cell);

        for (int i = 0; i < childCount; i++) {
            removeStateForCell(model.getChildAt(cell, i));
        }

        view.invalidate(cell);
        view.removeState(cell);
    }

    /**
     * Returns an array of key, value pairs representing the cell style for the
     * given cell. If no string is defined in the model that specifies the
     * style, then the default style for the cell is returned or <EMPTY_ARRAY>,
     * if not style can be found.
     *
     * @param cell Cell whose style should be returned.
     * @return Returns the style of the cell.
     */
    public Map<String, Object> getCellStyle(ICell cell) {
        Map<String, Object> style = (model.isEdge(cell)) ? stylesheet.getDefaultEdgeStyle() : stylesheet.getDefaultVertexStyle();

        String name = model.getStyle(cell);

        if (name != null) {
            style = stylesheet.getCellStyle(name, style);
        }

        if (style == null) {
            style = Stylesheet.EMPTY_STYLE;
        }

        return style;
    }

    //
    // Cell styles
    //

    /**
     * Sets the style of the selection cells to the given value.
     *
     * @param style String representing the new style of the cells.
     */
    public List<ICell> setCellStyle(String style) {
        return setCellStyle(style, null);
    }

    /**
     * Sets the style of the specified cells. If no cells are given, then the
     * selection cells are changed.
     *
     * @param style String representing the new style of the cells.
     * @param cells Optional array of <Cells> to set the style for. Default is the
     *              selection cells.
     */
    public List<ICell> setCellStyle(String style, List<ICell> cells) {
        if (cells == null) {
            cells = getSelectionCells();
        }

        if (cells != null) {
            model.beginUpdate();
            try {
                for (ICell cell : cells) {
                    model.setStyle(cell, style);
                }
            } finally {
                model.endUpdate();
            }
        }

        return cells;
    }

    /**
     * Toggles the boolean value for the given key in the style of the
     * given cell. If no cell is specified then the selection cell is
     * used.
     *
     * @param key          Key for the boolean value to be toggled.
     * @param defaultValue Default boolean value if no value is defined.
     * @param cell         Cell whose style should be modified.
     */
    public ICell toggleCellStyle(String key, boolean defaultValue, ICell cell) {
        return toggleCellStyles(key, defaultValue, List.of(cell)).get(0);
    }

    /**
     * Toggles the boolean value for the given key in the style of the
     * selection cells.
     *
     * @param key          Key for the boolean value to be toggled.
     * @param defaultValue Default boolean value if no value is defined.
     */
    public List<ICell> toggleCellStyles(String key, boolean defaultValue) {
        return toggleCellStyles(key, defaultValue, null);
    }

    /**
     * Toggles the boolean value for the given key in the style of the given
     * cells. If no cells are specified, then the selection cells are used. For
     * example, this can be used to toggle Constants.STYLE_ROUNDED or any
     * other style with a boolean value.
     *
     * @param key          String representing the key of the boolean style to be toggled.
     * @param defaultValue Default boolean value if no value is defined.
     * @param cells        Cells whose styles should be modified.
     */
    public List<ICell> toggleCellStyles(String key, boolean defaultValue, List<ICell> cells) {
        if (cells == null) {
            cells = getSelectionCells();
        }

        if (cells != null && cells.size() > 0) {
            CellState state = view.getState(cells.get(0));
            Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cells.get(0));

            if (style != null) {
                String value = (Utils.isTrue(style, key, defaultValue)) ? "0" : "1";
                setCellStyles(key, value, cells);
            }
        }

        return cells;
    }

    /**
     * Sets the key to value in the styles of the selection cells.
     *
     * @param key   String representing the key to be assigned.
     * @param value String representing the new value for the key.
     */
    public List<ICell> setCellStyles(String key, String value) {
        return setCellStyles(key, value, null);
    }

    /**
     * Sets the key to value in the styles of the given cells. This will modify
     * the existing cell styles in-place and override any existing assignment
     * for the given key. If no cells are specified, then the selection cells
     * are changed. If no value is specified, then the respective key is
     * removed from the styles.
     *
     * @param key   String representing the key to be assigned.
     * @param value String representing the new value for the key.
     * @param cells Array of cells to change the style for.
     */
    public List<ICell> setCellStyles(String key, String value, List<ICell> cells) {
        if (cells == null) {
            cells = getSelectionCells();
        }

        StyleUtils.setCellStyles(model, cells, key, value);

        return cells;
    }

    /**
     * Toggles the given bit for the given key in the styles of the selection
     * cells.
     *
     * @param key  String representing the key to toggle the flag in.
     * @param flag Integer that represents the bit to be toggled.
     */
    public List<ICell> toggleCellStyleFlags(String key, int flag) {
        return toggleCellStyleFlags(key, flag, null);
    }

    /**
     * Toggles the given bit for the given key in the styles of the specified
     * cells.
     *
     * @param key   String representing the key to toggle the flag in.
     * @param flag  Integer that represents the bit to be toggled.
     * @param cells Optional array of <Cells> to change the style for. Default is
     *              the selection cells.
     */
    public List<ICell> toggleCellStyleFlags(String key, int flag, List<ICell> cells) {
        return setCellStyleFlags(key, flag, null, cells);
    }

    /**
     * Sets or toggles the given bit for the given key in the styles of the
     * selection cells.
     *
     * @param key   String representing the key to toggle the flag in.
     * @param flag  Integer that represents the bit to be toggled.
     * @param value Boolean value to be used or null if the value should be
     *              toggled.
     */
    public List<ICell> setCellStyleFlags(String key, int flag, boolean value) {
        return setCellStyleFlags(key, flag, value, null);
    }

    /**
     * Sets or toggles the given bit for the given key in the styles of the
     * specified cells.
     *
     * @param key   String representing the key to toggle the flag in.
     * @param flag  Integer that represents the bit to be toggled.
     * @param value Boolean value to be used or null if the value should be
     *              toggled.
     * @param cells Optional array of cells to change the style for. If no
     *              cells are specified then the selection cells are used.
     */
    public List<ICell> setCellStyleFlags(String key, int flag, Boolean value, List<ICell> cells) {
        if (cells == null) {
            cells = getSelectionCells();
        }

        if (cells != null && cells.size() > 0) {
            if (value == null) {
                CellState state = view.getState(cells.get(0));
                Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cells.get(0));

                if (style != null) {
                    int current = Utils.getInt(style, key);
                    value = !((current & flag) == flag);
                }
            }

            StyleUtils.setCellStyleFlags(model, cells, key, flag, value);
        }

        return cells;
    }

    /**
     * Aligns the selection cells vertically or horizontally according to the
     * given alignment.
     *
     * @param align Specifies the alignment. Possible values are all constants
     *              in Constants with an ALIGN prefix.
     */
    public List<ICell> alignCells(String align) {
        return alignCells(align, null);
    }

    //
    // Cell alignment and orientation
    //

    /**
     * Aligns the given cells vertically or horizontally according to the given
     * alignment.
     *
     * @param align Specifies the alignment. Possible values are all constants
     *              in Constants with an ALIGN prefix.
     * @param cells Array of cells to be aligned.
     */
    public List<ICell> alignCells(String align, List<ICell> cells) {
        return alignCells(align, cells, null);
    }

    /**
     * Aligns the given cells vertically or horizontally according to the given
     * alignment using the optional parameter as the coordinate.
     *
     * @param align Specifies the alignment. Possible values are all constants
     *              in Constants with an ALIGN prefix.
     * @param cells Array of cells to be aligned.
     * @param param Optional coordinate for the alignment.
     */
    public List<ICell> alignCells(String align, List<ICell> cells, Object param) {
        if (cells == null) {
            cells = getSelectionCells();
        }

        if (cells != null && cells.size() > 1) {
            // Finds the required coordinate for the alignment
            if (param == null) {
                for (ICell cell : cells) {
                    Geometry geo = getCellGeometry(cell);

                    if (geo != null && !model.isEdge(cell)) {
                        if (param == null) {
                            if (align == null || align.equals(Constants.ALIGN_LEFT)) {
                                param = geo.getX();
                            } else if (align.equals(Constants.ALIGN_CENTER)) {
                                param = geo.getX() + geo.getWidth() / 2;
                                break;
                            } else if (align.equals(Constants.ALIGN_RIGHT)) {
                                param = geo.getX() + geo.getWidth();
                            } else if (align.equals(Constants.ALIGN_TOP)) {
                                param = geo.getY();
                            } else if (align.equals(Constants.ALIGN_MIDDLE)) {
                                param = geo.getY() + geo.getHeight() / 2;
                                break;
                            } else if (align.equals(Constants.ALIGN_BOTTOM)) {
                                param = geo.getY() + geo.getHeight();
                            }
                        } else {
                            double tmp = Double.parseDouble(String.valueOf(param));

                            if (align == null || align.equals(Constants.ALIGN_LEFT)) {
                                param = Math.min(tmp, geo.getX());
                            } else if (align.equals(Constants.ALIGN_RIGHT)) {
                                param = Math.max(tmp, geo.getX() + geo.getWidth());
                            } else if (align.equals(Constants.ALIGN_TOP)) {
                                param = Math.min(tmp, geo.getY());
                            } else if (align.equals(Constants.ALIGN_BOTTOM)) {
                                param = Math.max(tmp, geo.getY() + geo.getHeight());
                            }
                        }
                    }
                }
            }

            // Aligns the cells to the coordinate
            model.beginUpdate();
            try {
                double tmp = Double.parseDouble(String.valueOf(param));

                for (ICell cell : cells) {
                    Geometry geo = getCellGeometry(cell);

                    if (geo != null && !model.isEdge(cell)) {
                        geo = geo.clone();

                        if (align == null || align.equals(Constants.ALIGN_LEFT)) {
                            geo.setX(tmp);
                        } else if (align.equals(Constants.ALIGN_CENTER)) {
                            geo.setX(tmp - geo.getWidth() / 2);
                        } else if (align.equals(Constants.ALIGN_RIGHT)) {
                            geo.setX(tmp - geo.getWidth());
                        } else if (align.equals(Constants.ALIGN_TOP)) {
                            geo.setY(tmp);
                        } else if (align.equals(Constants.ALIGN_MIDDLE)) {
                            geo.setY(tmp - geo.getHeight() / 2);
                        } else if (align.equals(Constants.ALIGN_BOTTOM)) {
                            geo.setY(tmp - geo.getHeight());
                        }

                        model.setGeometry(cell, geo);

                        if (isResetEdgesOnMove()) {
                            resetEdges(List.of(cell));
                        }
                    }
                }
                fireEvent(new AlignCellsEvent(cells, align));
            } finally {
                model.endUpdate();
            }
        }

        return cells;
    }

    /**
     * Called when the main control point of the edge is double-clicked. This
     * implementation switches between null (default) and alternateEdgeStyle
     * and resets the edges control points. Finally, a flip event is fired
     * before endUpdate is called on the model.
     *
     * @param edge Cell that represents the edge to be flipped.
     * @return Returns the edge that has been flipped.
     */
    public ICell flipEdge(ICell edge) {
        if (edge != null && alternateEdgeStyle != null) {
            model.beginUpdate();
            try {
                String style = model.getStyle(edge);

                if (style == null || style.length() == 0) {
                    model.setStyle(edge, alternateEdgeStyle);
                } else {
                    model.setStyle(edge, null);
                }

                // Removes all existing control points
                resetEdge(edge);
                fireEvent(new FlipEdgeEvent(edge));
            } finally {
                model.endUpdate();
            }
        }

        return edge;
    }

    /**
     * Moves the selection cells to the front or back. This is a shortcut method.
     *
     * @param back Specifies if the cells should be moved to back.
     */
    public List<ICell> orderCells(boolean back) {
        return orderCells(back, null);
    }

    //
    // Order
    //

    /**
     * Moves the given cells to the front or back. The change is carried out
     * using cellsOrdered. This method fires Event.ORDER_CELLS while the
     * transaction is in progress.
     *
     * @param back  Specifies if the cells should be moved to back.
     * @param cells Array of cells whose order should be changed. If null is
     *              specified then the selection cells are used.
     */
    public List<ICell> orderCells(boolean back, List<ICell> cells) {
        if (cells == null) {
            cells = Utils.sortCells(getSelectionCells(), true);
        }

        model.beginUpdate();
        try {
            cellsOrdered(cells, back);
            fireEvent(new OrderCellsEvent(cells, back));
        } finally {
            model.endUpdate();
        }

        return cells;
    }

    /**
     * Moves the given cells to the front or back. This method fires
     * Event.CELLS_ORDERED while the transaction is in progress.
     *
     * @param cells Array of cells whose order should be changed.
     * @param back  Specifies if the cells should be moved to back.
     */
    public void cellsOrdered(List<ICell> cells, boolean back) {
        if (cells != null) {
            model.beginUpdate();
            try {
                for (int i = 0; i < cells.size(); i++) {
                    ICell parent = model.getParent(cells.get(i));

                    if (back) {
                        model.add(parent, cells.get(i), i);
                    } else {
                        model.add(parent, cells.get(i), model.getChildCount(parent) - 1);
                    }
                }
                fireEvent(new CellsOrderedEvent(cells, back));
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Groups the selection cells. This is a shortcut method.
     *
     * @return Returns the new group.
     */
    public Object groupCells() {
        return groupCells(null);
    }

    //
    // Grouping
    //

    /**
     * Groups the selection cells and adds them to the given group. This is a
     * shortcut method.
     *
     * @return Returns the new group.
     */
    public ICell groupCells(ICell group) {
        return groupCells(group, 0);
    }

    /**
     * Groups the selection cells and adds them to the given group. This is a
     * shortcut method.
     *
     * @return Returns the new group.
     */
    public ICell groupCells(ICell group, double border) {
        return groupCells(group, border, null);
    }

    /**
     * Adds the cells into the given group. The change is carried out using
     * cellsAdded, cellsMoved and cellsResized. This method fires
     * Event.GROUP_CELLS while the transaction is in progress. Returns the
     * new group. A group is only created if there is at least one entry in the
     * given array of cells.
     *
     * @param group  Cell that represents the target group. If null is specified
     *               then a new group is created using createGroupCell.
     * @param border Integer that specifies the border between the child area
     *               and the group bounds.
     * @param cells  Optional array of cells to be grouped. If null is specified
     *               then the selection cells are used.
     */
    public ICell groupCells(ICell group, double border, List<ICell> cells) {
        if (cells == null) {
            cells = Utils.sortCells(getSelectionCells(), true);
        }

        cells = getCellsForGroup(cells);

        if (group == null) {
            group = createGroupCell(cells);
        }

        Rectangle bounds = getBoundsForGroup(group, cells, border);

        if (cells.size() > 0 && bounds != null) {
            // Uses parent of group or previous parent of first child
            ICell parent = model.getParent(group);

            if (parent == null) {
                parent = model.getParent(cells.get(0));
            }

            model.beginUpdate();
            try {
                // Checks if the group has a geometry and
                // creates one if one does not exist
                if (getCellGeometry(group) == null) {
                    model.setGeometry(group, new Geometry());
                }

                // Adds the children into the group and moves
                int index = model.getChildCount(group);
                cellsAdded(cells, group, index, null, null, false);
                cellsMoved(cells, -bounds.getX(), -bounds.getY(), false, true);

                // Adds the group into the parent and resizes
                index = model.getChildCount(parent);
                cellsAdded(List.of(group), parent, index, null, null, false, false);
                cellsResized(List.of(group), new Rectangle[]{bounds});
                fireEvent(new GroupCellsEvent(cells, group, border));
            } finally {
                model.endUpdate();
            }
        }

        return group;
    }

    /**
     * Returns the cells with the same parent as the first cell
     * in the given array.
     */
    public List<ICell> getCellsForGroup(List<ICell> cells) {
        if (cells.size() == 0) {
            return List.of();
        }

        ICell parent = model.getParent(cells.get(0));
        return cells.stream().filter(cell -> parent.equals(model.getParent(cell))).collect(Collectors.toList());
    }

    /**
     * Returns the bounds to be used for the given group and children. This
     * implementation computes the bounding box of the geometries of all
     * vertices in the given children array. Edges are ignored. If the group
     * cell is a swimlane the title region is added to the bounds.
     */
    private Rectangle getBoundsForGroup(ICell group, List<ICell> children, double border) {
        Rectangle result = getBoundingBoxFromGeometry(children);
        if (result != null) {
            if (isSwimlane(group)) {
                Rectangle size = getStartSize(group);
                result.setX(result.getX() - size.getWidth());
                result.setY(result.getY() - size.getHeight());
                result.setWidth(result.getWidth() + size.getWidth());
                result.setHeight(result.getHeight() + size.getHeight());
            }

            // Adds the border
            result.setX(result.getX() - border);
            result.setY(result.getY() - border);
            result.setWidth(result.getWidth() + 2 * border);
            result.setHeight(result.getHeight() + 2 * border);
        }

        return result;
    }

    /**
     * Hook for creating the group cell to hold the given array of <Cells> if
     * no group cell was given to the <group> function. The children are just
     * for informational purpose, they will be added to the returned group
     * later. Note that the returned group should have a geometry. The
     * coordinates of which are later overridden.
     *
     * @return Returns a new group cell.
     */
    private ICell createGroupCell(List<ICell> cells) {
        Cell group = new Cell("", new Geometry(), null);
        group.setVertex(true);
        group.setConnectable(false);
        return group;
    }

    /**
     * Ungroups the selection cells. This is a shortcut method.
     */
    public List<ICell> ungroupCells() {
        return ungroupCells(null);
    }

    /**
     * Ungroups the given cells by moving the children the children to their
     * parents parent and removing the empty groups.
     *
     * @param cells Array of cells to be ungrouped. If null is specified then
     *              the selection cells are used.
     * @return Returns the children that have been removed from the groups.
     */
    public List<ICell> ungroupCells(List<ICell> cells) {
        List<ICell> result = new ArrayList<>();

        if (cells == null) {
            cells = getSelectionCells().stream().filter(cell -> model.getChildCount(cell) > 0).collect(Collectors.toList());
        }

        if (!cells.isEmpty()) {
            model.beginUpdate();
            try {
                for (ICell cell : cells) {
                    List<ICell> children = GraphModel.getChildren(model, cell);

                    if (!children.isEmpty()) {
                        ICell parent = model.getParent(cell);
                        int index = model.getChildCount(parent);

                        cellsAdded(children, parent, index, null, null, true);
                        result.addAll(children);
                    }
                }
                cellsRemoved(addAllEdges(cells));
                fireEvent(new UngroupCellsEvent(cells));
            } finally {
                model.endUpdate();
            }
        }

        return result;
    }

    /**
     * Removes the selection cells from their parents and adds them to the
     * default parent returned by getDefaultParent.
     */
    public List<ICell> removeCellsFromParent() {
        return removeCellsFromParent(null);
    }

    /**
     * Removes the specified cells from their parents and adds them to the
     * default parent.
     *
     * @param cells Array of cells to be removed from their parents.
     * @return Returns the cells that were removed from their parents.
     */
    public List<ICell> removeCellsFromParent(List<ICell> cells) {
        if (cells == null) {
            cells = getSelectionCells();
        }

        model.beginUpdate();
        try {
            ICell parent = getDefaultParent();
            int index = model.getChildCount(parent);
            cellsAdded(cells, parent, index, null, null, true);
            fireEvent(new RemoveCellsFromParentEvent(cells));
        } finally {
            model.endUpdate();
        }

        return cells;
    }

    /**
     * Updates the bounds of the given array of groups so that it includes
     * all child vertices.
     */
    public List<ICell> updateGroupBounds() {
        return updateGroupBounds(null);
    }

    /**
     * Updates the bounds of the given array of groups so that it includes
     * all child vertices.
     *
     * @param cells The groups whose bounds should be updated.
     */
    public List<ICell> updateGroupBounds(List<ICell> cells) {
        return updateGroupBounds(cells, 0);
    }

    /**
     * Updates the bounds of the given array of groups so that it includes
     * all child vertices.
     *
     * @param cells  The groups whose bounds should be updated.
     * @param border The border to be added in the group.
     */
    public List<ICell> updateGroupBounds(List<ICell> cells, int border) {
        return updateGroupBounds(cells, border, false);
    }

    /**
     * Updates the bounds of the given array of groups so that it includes
     * all child vertices.
     *
     * @param cells      The groups whose bounds should be updated.
     * @param border     The border to be added in the group.
     * @param moveParent Specifies if the group should be moved.
     */
    public List<ICell> updateGroupBounds(List<ICell> cells, int border, boolean moveParent) {
        if (cells == null) {
            cells = getSelectionCells();
        }

        model.beginUpdate();
        try {
            for (ICell cell : cells) {
                Geometry geo = getCellGeometry(cell);

                if (geo != null) {
                    List<ICell> children = getChildCells(cell);

                    if (children != null && !children.isEmpty()) {
                        Rectangle childBounds = getBoundingBoxFromGeometry(children);

                        if (childBounds.getWidth() > 0 && childBounds.getHeight() > 0) {
                            Rectangle size = (isSwimlane(cell)) ? getStartSize(cell) : new Rectangle();

                            geo = geo.clone();

                            if (moveParent) {
                                geo.setX(geo.getX() + childBounds.getX() - size.getWidth() - border);
                                geo.setY(geo.getY() + childBounds.getY() - size.getHeight() - border);
                            }

                            geo.setWidth(childBounds.getWidth() + size.getWidth() + 2 * border);
                            geo.setHeight(childBounds.getHeight() + size.getHeight() + 2 * border);

                            model.setGeometry(cell, geo);
                            moveCells(children, -childBounds.getX() + size.getWidth() + border, -childBounds.getY() + size.getHeight() + border);
                        }
                    }
                }
            }
        } finally {
            model.endUpdate();
        }

        return cells;
    }

    /**
     * Clones all cells in the given array. To clone all children in a cell and
     * add them to another graph:
     *
     * <code>
     * graph2.addCells(graph.cloneCells(new Object[] { parent }));
     * </code>
     * <p>
     * To clone all children in a graph layer if graph g1 and put them into the
     * default parent (typically default layer) of another graph g2, the
     * following code is used:
     *
     * <code>
     * g2.addCells(g1.cloneCells(g1.cloneCells(g1.getChildCells(g1.getDefaultParent()));
     * </code>
     */
    public List<ICell> cloneCells(List<ICell> cells) {

        return cloneCells(cells, true);
    }

    //
    // Cell cloning, insertion and removal
    //

    /**
     * Returns the clones for the given cells. The clones are created
     * recursively using <GraphModel.cloneCells>. If the terminal of an edge
     * is not in the given array, then the respective end is assigned a
     * terminal point and the terminal is removed. If a cloned edge is invalid
     * and allowInvalidEdges is false, then a null pointer will be at this
     * position in the returned array. Use getCloneableCells on the input array
     * to only clone the cells where isCellCloneable returns true.
     *
     * @param cells Array of Cells to be cloned.
     * @return Returns the clones of the given cells.
     */
    public List<ICell> cloneCells(List<ICell> cells, boolean allowInvalidEdges) {
        List<ICell> clones = null;

        if (cells != null) {
            Collection<ICell> tmp = new LinkedHashSet<>(cells.size());
            tmp.addAll(cells);

            if (!tmp.isEmpty()) {
                double scale = view.getScale();
                Point trans = view.getTranslate();
                clones = model.cloneCells(cells, true);

                for (int i = 0; i < cells.size(); i++) {
                    ICell clone = clones.get(i);
                    if (!allowInvalidEdges && model.isEdge(clone) && getEdgeValidationError(clone, model.getTerminal(clone, true), model.getTerminal(clone, false)) != null) {
                        clones.set(i, null);
                    } else {
                        Geometry g = model.getGeometry(clone);

                        if (g != null) {
                            CellState state = view.getState(cells.get(i));
                            CellState pstate = view.getState(model.getParent(cells.get(i)));

                            if (state != null && pstate != null) {
                                double dx = pstate.getOrigin().getX();
                                double dy = pstate.getOrigin().getY();

                                if (model.isEdge(clone)) {
                                    // Checks if the source is cloned or sets the terminal point
                                    ICell src = model.getTerminal(cells.get(i), true);

                                    while (src != null && !tmp.contains(src)) {
                                        src = model.getParent(src);
                                    }

                                    if (src == null) {
                                        Point pt = state.getAbsolutePoint(0);
                                        g.setTerminalPoint(new Point(pt.getX() / scale - trans.getX(), pt.getY() / scale - trans.getY()), true);
                                    }

                                    // Checks if the target is cloned or sets the terminal point
                                    ICell trg = model.getTerminal(cells.get(i), false);

                                    while (trg != null && !tmp.contains(trg)) {
                                        trg = model.getParent(trg);
                                    }

                                    if (trg == null) {
                                        Point pt = state.getAbsolutePoint(state.getAbsolutePointCount() - 1);
                                        g.setTerminalPoint(new Point(pt.getX() / scale - trans.getX(), pt.getY() / scale - trans.getY()), false);
                                    }

                                    // Translates the control points
                                    List<Point> points = g.getPoints();

                                    if (points != null) {

                                        for (Point pt : points) {
                                            pt.setX(pt.getX() + dx);
                                            pt.setY(pt.getY() + dy);
                                        }
                                    }
                                } else {
                                    g.setX(g.getX() + dx);
                                    g.setY(g.getY() + dy);
                                }
                            }
                        }
                    }
                }
            } else {
                clones = new ArrayList<>();
            }
        }

        return clones;
    }

    /**
     * Creates and adds a new vertex with an empty style.
     */
    public ICell insertVertex(ICell parent, String id, Object value, double x, double y, double width, double height) {
        return insertVertex(parent, id, value, x, y, width, height, null);
    }

    /**
     * Adds a new vertex into the given parent using value as the user object
     * and the given coordinates as the geometry of the new vertex. The id and
     * style are used for the respective properties of the new cell, which is
     * returned.
     *
     * @param parent Cell that specifies the parent of the new vertex.
     * @param id     Optional string that defines the Id of the new vertex.
     * @param value  Object to be used as the user object.
     * @param x      Integer that defines the x coordinate of the vertex.
     * @param y      Integer that defines the y coordinate of the vertex.
     * @param width  Integer that defines the width of the vertex.
     * @param height Integer that defines the height of the vertex.
     * @param style  Optional string that defines the cell style.
     * @return Returns the new vertex that has been inserted.
     */
    public ICell insertVertex(ICell parent, String id, Object value, double x, double y, double width, double height, String style) {
        return insertVertex(parent, id, value, x, y, width, height, style, false);
    }

    /**
     * Adds a new vertex into the given parent using value as the user object
     * and the given coordinates as the geometry of the new vertex. The id and
     * style are used for the respective properties of the new cell, which is
     * returned.
     *
     * @param parent   Cell that specifies the parent of the new vertex.
     * @param id       Optional string that defines the Id of the new vertex.
     * @param value    Object to be used as the user object.
     * @param x        Integer that defines the x coordinate of the vertex.
     * @param y        Integer that defines the y coordinate of the vertex.
     * @param width    Integer that defines the width of the vertex.
     * @param height   Integer that defines the height of the vertex.
     * @param style    Optional string that defines the cell style.
     * @param relative Specifies if the geometry should be relative.
     * @return Returns the new vertex that has been inserted.
     */
    public ICell insertVertex(ICell parent, String id, Object value, double x, double y, double width, double height, String style, boolean relative) {
        ICell vertex = createVertex(parent, id, value, x, y, width, height, style, relative);

        return addCell(vertex, parent);
    }

    /**
     * Hook method that creates the new vertex for insertVertex.
     *
     * @param parent Cell that specifies the parent of the new vertex.
     * @param id     Optional string that defines the Id of the new vertex.
     * @param value  Object to be used as the user object.
     * @param x      Integer that defines the x coordinate of the vertex.
     * @param y      Integer that defines the y coordinate of the vertex.
     * @param width  Integer that defines the width of the vertex.
     * @param height Integer that defines the height of the vertex.
     * @param style  Optional string that defines the cell style.
     * @return Returns the new vertex to be inserted.
     */
    public Object createVertex(Object parent, String id, Object value, double x, double y, double width, double height, String style) {
        return createVertex(parent, id, value, x, y, width, height, style, false);
    }

    /**
     * Hook method that creates the new vertex for insertVertex.
     *
     * @param parent   Cell that specifies the parent of the new vertex.
     * @param id       Optional string that defines the Id of the new vertex.
     * @param value    Object to be used as the user object.
     * @param x        Integer that defines the x coordinate of the vertex.
     * @param y        Integer that defines the y coordinate of the vertex.
     * @param width    Integer that defines the width of the vertex.
     * @param height   Integer that defines the height of the vertex.
     * @param style    Optional string that defines the cell style.
     * @param relative Specifies if the geometry should be relative.
     * @return Returns the new vertex to be inserted.
     */
    public Cell createVertex(Object parent, String id, Object value, double x, double y, double width, double height, String style, boolean relative) {
        Geometry geometry = new Geometry(x, y, width, height);
        geometry.setRelative(relative);

        Cell vertex = new Cell(value, geometry, style);
        vertex.setId(id);
        vertex.setVertex(true);
        vertex.setConnectable(true);

        return vertex;
    }

    /**
     * Creates and adds a new edge with an empty style.
     */
    public ICell insertEdge(ICell parent, String id, Object value, ICell source, ICell target) {
        return insertEdge(parent, id, value, source, target, null);
    }

    /**
     * Adds a new edge into the given parent using value as the user object and
     * the given source and target as the terminals of the new edge. The Id and
     * style are used for the respective properties of the new cell, which is
     * returned.
     *
     * @param parent Cell that specifies the parent of the new edge.
     * @param id     Optional string that defines the Id of the new edge.
     * @param value  Object to be used as the user object.
     * @param source Cell that defines the source of the edge.
     * @param target Cell that defines the target of the edge.
     * @param style  Optional string that defines the cell style.
     * @return Returns the new edge that has been inserted.
     */
    public ICell insertEdge(ICell parent, String id, Object value, ICell source, ICell target, String style) {
        ICell edge = createEdge(parent, id, value, source, target, style);

        return addEdge(edge, parent, source, target, null);
    }

    /**
     * Hook method that creates the new edge for insertEdge. This
     * implementation does not set the source and target of the edge, these
     * are set when the edge is added to the model.
     *
     * @param parent Cell that specifies the parent of the new edge.
     * @param id     Optional string that defines the Id of the new edge.
     * @param value  Object to be used as the user object.
     * @param source Cell that defines the source of the edge.
     * @param target Cell that defines the target of the edge.
     * @param style  Optional string that defines the cell style.
     * @return Returns the new edge to be inserted.
     */
    public ICell createEdge(ICell parent, String id, Object value, ICell source, ICell target, String style) {
        Cell edge = new Cell(value, new Geometry(), style);

        edge.setId(id);
        edge.setEdge(true);
        edge.getGeometry().setRelative(true);

        return edge;
    }

    /**
     * Adds the edge to the parent and connects it to the given source and
     * target terminals. This is a shortcut method.
     *
     * @param edge   Edge to be inserted into the given parent.
     * @param parent Object that represents the new parent. If no parent is
     *               given then the default parent is used.
     * @param source Optional cell that represents the source terminal.
     * @param target Optional cell that represents the target terminal.
     * @param index  Optional index to insert the cells at. Default is to append.
     * @return Returns the edge that was added.
     */
    public ICell addEdge(ICell edge, ICell parent, ICell source, ICell target, Integer index) {
        return addCell(edge, parent, index, source, target);
    }

    /**
     * Adds the cell to the default parent. This is a shortcut method.
     *
     * @param cell Cell to be inserted.
     * @return Returns the cell that was added.
     */
    public ICell addCell(ICell cell) {
        return addCell(cell, null);
    }

    /**
     * Adds the cell to the parent. This is a shortcut method.
     *
     * @param cell   Cell tobe inserted.
     * @param parent Object that represents the new parent. If no parent is
     *               given then the default parent is used.
     * @return Returns the cell that was added.
     */
    public ICell addCell(ICell cell, ICell parent) {
        return addCell(cell, parent, null, null, null);
    }

    /**
     * Adds the cell to the parent and connects it to the given source and
     * target terminals. This is a shortcut method.
     *
     * @param cell   Cell to be inserted into the given parent.
     * @param parent Object that represents the new parent. If no parent is
     *               given then the default parent is used.
     * @param index  Optional index to insert the cells at. Default is to append.
     * @param source Optional cell that represents the source terminal.
     * @param target Optional cell that represents the target terminal.
     * @return Returns the cell that was added.
     */
    public ICell addCell(ICell cell, ICell parent, Integer index, ICell source, ICell target) {
        return addCells(List.of(cell), parent, index, source, target).get(0);
    }

    /**
     * Adds the cells to the default parent. This is a shortcut method.
     *
     * @param cells Array of cells to be inserted.
     * @return Returns the cells that were added.
     */
    public List<ICell> addCells(List<ICell> cells) {
        return addCells(cells, null);
    }

    /**
     * Adds the cells to the parent. This is a shortcut method.
     *
     * @param cells  Array of cells to be inserted.
     * @param parent Optional cell that represents the new parent. If no parent
     *               is specified then the default parent is used.
     * @return Returns the cells that were added.
     */
    public List<ICell> addCells(List<ICell> cells, ICell parent) {
        return addCells(cells, parent, null);
    }

    /**
     * Adds the cells to the parent at the given index. This is a shortcut method.
     *
     * @param cells  Array of cells to be inserted.
     * @param parent Optional cell that represents the new parent. If no parent
     *               is specified then the default parent is used.
     * @param index  Optional index to insert the cells at. Default is to append.
     * @return Returns the cells that were added.
     */
    public List<ICell> addCells(List<ICell> cells, ICell parent, Integer index) {
        return addCells(cells, parent, index, null, null);
    }

    /**
     * Adds the cells to the parent at the given index, connecting each cell to
     * the optional source and target terminal. The change is carried out using
     * cellsAdded. This method fires Event.ADD_CELLS while the transaction
     * is in progress.
     *
     * @param cells  Array of cells to be added.
     * @param parent Optional cell that represents the new parent. If no parent
     *               is specified then the default parent is used.
     * @param index  Optional index to insert the cells at. Default is to append.
     * @param source Optional source terminal for all inserted cells.
     * @param target Optional target terminal for all inserted cells.
     * @return Returns the cells that were added.
     */
    public List<ICell> addCells(List<ICell> cells, ICell parent, Integer index, ICell source, ICell target) {
        if (parent == null) {
            parent = getDefaultParent();
        }

        if (index == null) {
            index = model.getChildCount(parent);
        }

        model.beginUpdate();
        try {
            cellsAdded(cells, parent, index, source, target, false, true);
            fireEvent(new AddCellsEvent(cells, parent, index, source, target));
        } finally {
            model.endUpdate();
        }

        return cells;
    }

    /**
     * Adds the specified cells to the given parent. This method fires
     * Event.CELLS_ADDED while the transaction is in progress.
     */
    public void cellsAdded(List<ICell> cells, ICell parent, Integer index, ICell source, ICell target, boolean absolute) {
        cellsAdded(cells, parent, index, source, target, absolute, true);
    }

    /**
     * Adds the specified cells to the given parent. This method fires
     * Event.CELLS_ADDED while the transaction is in progress.
     */
    public void cellsAdded(List<ICell> cells, ICell parent, Integer index, ICell source, ICell target, boolean absolute, boolean constrain) {
        if (cells != null && parent != null && index != null) {
            model.beginUpdate();
            try {
                CellState parentState = (absolute) ? view.getState(parent) : null;
                Point o1 = (parentState != null) ? parentState.getOrigin() : null;
                Point zero = new Point(0, 0);

                for (int i = 0; i < cells.size(); i++) {
                    ICell cell = cells.get(i);
                    if (cell == null) {
                        index--;
                    } else {
                        ICell previous = model.getParent(cell);

                        // Keeps the cell at its absolute location
                        if (o1 != null && cell != parent && parent != previous) {
                            CellState oldState = view.getState(previous);
                            Point o2 = (oldState != null) ? oldState.getOrigin() : zero;
                            Geometry geo = model.getGeometry(cell);

                            if (geo != null) {
                                double dx = o2.getX() - o1.getX();
                                double dy = o2.getY() - o1.getY();

                                geo = geo.clone();
                                geo.translate(dx, dy);

                                if (!geo.isRelative() && model.isVertex(cell) && !isAllowNegativeCoordinates()) {
                                    geo.setX(Math.max(0, geo.getX()));
                                    geo.setY(Math.max(0, geo.getY()));
                                }

                                model.setGeometry(cell, geo);
                            }
                        }

                        // Decrements all following indices
                        // if cell is already in parent
                        if (parent == previous) {
                            index--;
                        }

                        model.add(parent, cell, index + i);

                        // Extends the parent
                        if (isExtendParentsOnAdd() && isExtendParent(cell)) {
                            extendParent(cell);
                        }

                        // Constrains the child
                        if (constrain) {
                            constrainChild(cell);
                        }

                        // Sets the source terminal
                        if (source != null) {
                            cellConnected(cell, source, true, null);
                        }

                        // Sets the target terminal
                        if (target != null) {
                            cellConnected(cell, target, false, null);
                        }
                    }
                }
                fireEvent(new CellsAddedEvent(cells, parent, index, source, target, absolute));

            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Removes the selection cells from the graph.
     *
     * @return Returns the cells that have been removed.
     */
    public List<ICell> removeCells() {
        return removeCells(null);
    }

    /**
     * Removes the given cells from the graph.
     *
     * @param cells Array of cells to remove.
     * @return Returns the cells that have been removed.
     */
    public List<ICell> removeCells(List<ICell> cells) {
        return removeCells(cells, true);
    }

    /**
     * Removes the given cells from the graph including all connected edges if
     * includeEdges is true. The change is carried out using cellsRemoved. This
     * method fires Event.REMOVE_CELLS while the transaction is in progress.
     *
     * @param cells        Array of cells to remove. If null is specified then the
     *                     selection cells which are deletable are used.
     * @param includeEdges Specifies if all connected edges should be removed as
     *                     well.
     */
    public List<ICell> removeCells(List<ICell> cells, boolean includeEdges) {
        if (cells == null) {
            cells = getDeletableCells(getSelectionCells());
        }

        // Adds all edges to the cells
        if (includeEdges) {
            cells = getDeletableCells(addAllEdges(cells));
        }

        model.beginUpdate();
        try {
            cellsRemoved(cells);
            fireEvent(new RemoveCellsEvent(cells, includeEdges));
        } finally {
            model.endUpdate();
        }

        return cells;
    }

    /**
     * Removes the given cells from the model. This method fires
     * Event.CELLS_REMOVED while the transaction is in progress.
     *
     * @param cells Array of cells to remove.
     */
    public void cellsRemoved(List<ICell> cells) {
        if (cells != null && !cells.isEmpty()) {
            double scale = view.getScale();
            Point tr = view.getTranslate();

            model.beginUpdate();
            try {
                for (ICell cell : cells) {
                    // Disconnects edges which are not in cells
                    Collection<ICell> cellSet = new HashSet<>(cells);
                    List<ICell> edges = getConnections(cell);

                    for (ICell edge : edges) {
                        if (!cellSet.contains(edge)) {
                            Geometry geo = model.getGeometry(edge);

                            if (geo != null) {
                                CellState state = view.getState(edge);

                                if (state != null) {
                                    // Checks which side of the edge is being disconnected
                                    ICell tmp = state.getVisibleTerminal(true);
                                    boolean source = false;

                                    while (tmp != null) {
                                        if (cell == tmp) {
                                            source = true;
                                            break;
                                        }

                                        tmp = model.getParent(tmp);
                                    }

                                    geo = geo.clone();
                                    int n = (source) ? 0 : state.getAbsolutePointCount() - 1;
                                    Point pt = state.getAbsolutePoint(n);

                                    geo.setTerminalPoint(new Point(pt.getX() / scale - tr.getX(), pt.getY() / scale - tr.getY()), source);
                                    model.setTerminal(edge, null, source);
                                    model.setGeometry(edge, geo);
                                }
                            }
                        }
                    }

                    model.remove(cell);
                }
                fireEvent(new CellsRemovedEvent(cells));
            } finally {
                model.endUpdate();
            }
        }
    }

    public ICell splitEdge(ICell edge, List<ICell> cells) {
        return splitEdge(edge, cells, null, 0, 0);
    }

    public ICell splitEdge(ICell edge, List<ICell> cells, double dx, double dy) {
        return splitEdge(edge, cells, null, dx, dy);
    }

    /**
     * Splits the given edge by adding a newEdge between the previous source
     * and the given cell and reconnecting the source of the given edge to the
     * given cell. Fires Event.SPLIT_EDGE while the transaction is in
     * progress.
     *
     * @param edge    Object that represents the edge to be splitted.
     * @param cells   Array that contains the cells to insert into the edge.
     * @param newEdge Object that represents the edge to be inserted.
     * @return Returns the new edge that has been inserted.
     */
    public ICell splitEdge(ICell edge, List<ICell> cells, ICell newEdge, double dx, double dy) {
        if (newEdge == null) {
            newEdge = cloneCells(List.of(edge)).get(0);
        }

        ICell parent = model.getParent(edge);
        ICell source = model.getTerminal(edge, true);

        model.beginUpdate();
        try {
            cellsMoved(cells, dx, dy, false, false);
            cellsAdded(cells, parent, model.getChildCount(parent), null, null, true);
            cellsAdded(List.of(newEdge), parent, model.getChildCount(parent), source, cells.get(0), false);
            cellConnected(edge, cells.get(0), true, null);
            fireEvent(new SplitEdgeEvent(cells, dx, dy, edge, newEdge));
        } finally {
            model.endUpdate();
        }

        return newEdge;
    }

    /**
     * Sets the visible state of the selection cells. This is a shortcut
     * method.
     *
     * @param show Boolean that specifies the visible state to be assigned.
     * @return Returns the cells whose visible state was changed.
     */
    public List<ICell> toggleCells(boolean show) {
        return toggleCells(show, null);
    }

    //
    // Cell visibility
    //

    /**
     * Sets the visible state of the specified cells. This is a shortcut
     * method.
     *
     * @param show  Boolean that specifies the visible state to be assigned.
     * @param cells Array of cells whose visible state should be changed.
     * @return Returns the cells whose visible state was changed.
     */
    public List<ICell> toggleCells(boolean show, List<ICell> cells) {
        return toggleCells(show, cells, true);
    }

    /**
     * Sets the visible state of the specified cells and all connected edges
     * if includeEdges is true. The change is carried out using cellsToggled.
     * This method fires Event.TOGGLE_CELLS while the transaction is in
     * progress.
     *
     * @param show  Boolean that specifies the visible state to be assigned.
     * @param cells Array of cells whose visible state should be changed. If
     *              null is specified then the selection cells are used.
     * @return Returns the cells whose visible state was changed.
     */
    public List<ICell> toggleCells(boolean show, List<ICell> cells, boolean includeEdges) {
        if (cells == null) {
            cells = getSelectionCells();
        }

        // Adds all connected edges recursively
        if (includeEdges) {
            cells = addAllEdges(cells);
        }

        model.beginUpdate();
        try {
            cellsToggled(cells, show);
            fireEvent(new ToggleCellsEvent(cells, show, includeEdges));
        } finally {
            model.endUpdate();
        }

        return cells;
    }

    /**
     * Sets the visible state of the specified cells.
     *
     * @param cells Array of cells whose visible state should be changed.
     * @param show  Boolean that specifies the visible state to be assigned.
     */
    public void cellsToggled(List<ICell> cells, boolean show) {
        if (cells != null && !cells.isEmpty()) {
            model.beginUpdate();
            try {
                cells.forEach(cell -> model.setVisible(cell, show));
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Sets the collapsed state of the selection cells without recursion.
     * This is a shortcut method.
     *
     * @param collapse Boolean that specifies the collapsed state to be
     *                 assigned.
     * @return Returns the cells whose collapsed state was changed.
     */
    public List<ICell> foldCells(boolean collapse) {
        return foldCells(collapse, false);
    }

    //
    // Folding
    //

    /**
     * Sets the collapsed state of the selection cells. This is a shortcut
     * method.
     *
     * @param collapse Boolean that specifies the collapsed state to be
     *                 assigned.
     * @param recurse  Boolean that specifies if the collapsed state should
     *                 be assigned to all descendants.
     * @return Returns the cells whose collapsed state was changed.
     */
    public List<ICell> foldCells(boolean collapse, boolean recurse) {
        return foldCells(collapse, recurse, null);
    }

    /**
     * Invokes foldCells with checkFoldable set to false.
     */
    public List<ICell> foldCells(boolean collapse, boolean recurse, List<ICell> cells) {
        return foldCells(collapse, recurse, cells, false);
    }

    /**
     * Sets the collapsed state of the specified cells and all descendants
     * if recurse is true. The change is carried out using cellsFolded.
     * This method fires Event.FOLD_CELLS while the transaction is in
     * progress. Returns the cells whose collapsed state was changed.
     *
     * @param collapse      Boolean indicating the collapsed state to be assigned.
     * @param recurse       Boolean indicating if the collapsed state of all
     *                      descendants should be set.
     * @param cells         Array of cells whose collapsed state should be set. If
     *                      null is specified then the foldable selection cells are used.
     * @param checkFoldable Boolean indicating of isCellFoldable should be
     *                      checked. Default is false.
     */
    public List<ICell> foldCells(boolean collapse, boolean recurse, List<ICell> cells, boolean checkFoldable) {
        if (cells == null) {
            cells = getFoldableCells(getSelectionCells(), collapse);
        }

        model.beginUpdate();
        try {
            cellsFolded(cells, collapse, recurse, checkFoldable);
            fireEvent(new FoldCellsEvent(cells, collapse, recurse));
        } finally {
            model.endUpdate();
        }

        return cells;
    }

    /**
     * Invokes cellsFoldable with checkFoldable set to false.
     */
    public void cellsFolded(List<ICell> cells, boolean collapse, boolean recurse) {
        cellsFolded(cells, collapse, recurse, false);
    }

    /**
     * Sets the collapsed state of the specified cells. This method fires
     * Event.CELLS_FOLDED while the transaction is in progress. Returns the
     * cells whose collapsed state was changed.
     *
     * @param cells         Array of cells whose collapsed state should be set.
     * @param collapse      Boolean indicating the collapsed state to be assigned.
     * @param recurse       Boolean indicating if the collapsed state of all
     *                      descendants should be set.
     * @param checkFoldable Boolean indicating of isCellFoldable should be
     *                      checked. Default is false.
     */
    public void cellsFolded(List<ICell> cells, boolean collapse, boolean recurse, boolean checkFoldable) {
        if (cells != null && !cells.isEmpty()) {
            model.beginUpdate();
            try {
                for (ICell cell : cells) {
                    if ((!checkFoldable || isCellFoldable(cell, collapse)) && collapse != isCellCollapsed(cell)) {
                        model.setCollapsed(cell, collapse);
                        swapBounds(cell, collapse);

                        if (isExtendParent(cell)) {
                            extendParent(cell);
                        }

                        if (recurse) {
                            List<ICell> children = GraphModel.getChildren(model, cell);
                            cellsFolded(children, collapse, true);
                        }
                    }
                }
                fireEvent(new CellsFoldedEvent(cells, collapse, recurse));
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Swaps the alternate and the actual bounds in the geometry of the given
     * cell invoking updateAlternateBounds before carrying out the swap.
     *
     * @param cell         Cell for which the bounds should be swapped.
     * @param willCollapse Boolean indicating if the cell is going to be collapsed.
     */
    public void swapBounds(ICell cell, boolean willCollapse) {
        if (cell != null) {
            Geometry geo = model.getGeometry(cell);

            if (geo != null) {
                geo = geo.clone();

                updateAlternateBounds(cell, geo, willCollapse);
                geo.swap();

                model.setGeometry(cell, geo);
            }
        }
    }

    /**
     * Updates or sets the alternate bounds in the given geometry for the given
     * cell depending on whether the cell is going to be collapsed. If no
     * alternate bounds are defined in the geometry and
     * collapseToPreferredSize is true, then the preferred size is used for
     * the alternate bounds. The top, left corner is always kept at the same
     * location.
     *
     * @param cell         Cell for which the geometry is being udpated.
     * @param geo          Geometry for which the alternate bounds should be updated.
     * @param willCollapse Boolean indicating if the cell is going to be collapsed.
     */
    public void updateAlternateBounds(ICell cell, Geometry geo, boolean willCollapse) {
        if (cell != null && geo != null) {
            if (geo.getAlternateBounds() == null) {
                Rectangle bounds = null;

                if (isCollapseToPreferredSize()) {
                    bounds = getPreferredSizeForCell(cell);

                    if (isSwimlane(cell)) {
                        Rectangle size = getStartSize(cell);

                        bounds.setHeight(Math.max(bounds.getHeight(), size.getHeight()));
                        bounds.setWidth(Math.max(bounds.getWidth(), size.getWidth()));
                    }
                }

                if (bounds == null) {
                    bounds = geo;
                }

                geo.setAlternateBounds(new Rectangle(geo.getX(), geo.getY(), bounds.getWidth(), bounds.getHeight()));
            } else {
                geo.getAlternateBounds().setX(geo.getX());
                geo.getAlternateBounds().setY(geo.getY());
            }
        }
    }

    /**
     * Returns an array with the given cells and all edges that are connected
     * to a cell or one of its descendants.
     */
    public List<ICell> addAllEdges(List<ICell> cells) {
        List<ICell> allCells = new ArrayList<>();
        allCells.addAll(cells);
        allCells.addAll(getAllEdges(cells));

        return allCells;
    }

    /**
     * Returns all edges connected to the given cells or their descendants.
     */
    public List<ICell> getAllEdges(List<ICell> cells) {
        List<ICell> edges = new ArrayList<>();

        if (cells != null) {
            for (ICell cell : cells) {
                int edgeCount = model.getEdgeCount(cell);

                for (int j = 0; j < edgeCount; j++) {
                    edges.add(model.getEdgeAt(cell, j));
                }

                // Recurses
                List<ICell> children = GraphModel.getChildren(model, cell);
                edges.addAll(getAllEdges(children));
            }
        }

        return edges;
    }

    /**
     * Updates the size of the given cell in the model using
     * getPreferredSizeForCell to get the new size. This function
     * fires beforeUpdateSize and afterUpdateSize events.
     *
     * @param cell <Cell> for which the size should be changed.
     */
    public ICell updateCellSize(ICell cell) {
        return updateCellSize(cell, false);
    }

    //
    // Cell sizing
    //

    /**
     * Updates the size of the given cell in the model using
     * getPreferredSizeForCell to get the new size. This function
     * fires Event.UPDATE_CELL_SIZE.
     *
     * @param cell Cell for which the size should be changed.
     */
    public ICell updateCellSize(ICell cell, boolean ignoreChildren) {
        model.beginUpdate();
        try {
            cellSizeUpdated(cell, ignoreChildren);
            fireEvent(new UpdateCellSizeEvent(cell, ignoreChildren));
        } finally {
            model.endUpdate();
        }

        return cell;
    }

    /**
     * Updates the size of the given cell in the model using
     * getPreferredSizeForCell to get the new size.
     *
     * @param cell Cell for which the size should be changed.
     */
    public void cellSizeUpdated(ICell cell, boolean ignoreChildren) {
        if (cell != null) {
            model.beginUpdate();
            try {
                Rectangle size = getPreferredSizeForCell(cell);
                Geometry geo = model.getGeometry(cell);

                if (size != null && geo != null) {
                    boolean collapsed = isCellCollapsed(cell);
                    geo = geo.clone();

                    if (isSwimlane(cell)) {
                        CellState state = view.getState(cell);
                        Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cell);
                        String cellStyle = model.getStyle(cell);

                        if (cellStyle == null) {
                            cellStyle = "";
                        }

                        if (Utils.isTrue(style, Constants.STYLE_HORIZONTAL, true)) {
                            cellStyle = StyleUtils.setStyle(cellStyle, Constants.STYLE_STARTSIZE, String.valueOf(size.getHeight() + 8));

                            if (collapsed) {
                                geo.setHeight(size.getHeight() + 8);
                            }

                            geo.setWidth(size.getWidth());
                        } else {
                            cellStyle = StyleUtils.setStyle(cellStyle, Constants.STYLE_STARTSIZE, String.valueOf(size.getWidth() + 8));

                            if (collapsed) {
                                geo.setWidth(size.getWidth() + 8);
                            }

                            geo.setHeight(size.getHeight());
                        }

                        model.setStyle(cell, cellStyle);
                    } else {
                        geo.setWidth(size.getWidth());
                        geo.setHeight(size.getHeight());
                    }

                    if (!ignoreChildren && !collapsed) {
                        Rectangle bounds = view.getBounds(GraphModel.getChildren(model, cell));

                        if (bounds != null) {
                            Point tr = view.getTranslate();
                            double scale = view.getScale();

                            double width = (bounds.getX() + bounds.getWidth()) / scale - geo.getX() - tr.getX();
                            double height = (bounds.getY() + bounds.getHeight()) / scale - geo.getY() - tr.getY();

                            geo.setWidth(Math.max(geo.getWidth(), width));
                            geo.setHeight(Math.max(geo.getHeight(), height));
                        }
                    }

                    cellsResized(List.of(cell), new Rectangle[]{geo});
                }
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Returns the preferred width and height of the given <Cell> as an
     * <Rectangle>.
     *
     * @param cell <Cell> for which the preferred size should be returned.
     */
    public Rectangle getPreferredSizeForCell(ICell cell) {
        Rectangle result = null;

        if (cell != null) {
            CellState state = view.getState(cell);
            Map<String, Object> style = (state != null) ? state.style : getCellStyle(cell);

            if (style != null && !model.isEdge(cell)) {
                double dx = 0;
                double dy = 0;

                // Adds dimension of image if shape is a label
                if (getImage(state) != null || Utils.getString(style, Constants.STYLE_IMAGE) != null) {
                    if (Utils.getString(style, Constants.STYLE_SHAPE, "").equals(Constants.SHAPE_LABEL)) {
                        if (Utils.getString(style, Constants.STYLE_VERTICAL_ALIGN, "").equals(Constants.ALIGN_MIDDLE)) {
                            dx += Utils.getDouble(style, Constants.STYLE_IMAGE_WIDTH, Constants.DEFAULT_IMAGESIZE);
                        }

                        if (Utils.getString(style, Constants.STYLE_ALIGN, "").equals(Constants.ALIGN_CENTER)) {
                            dy += Utils.getDouble(style, Constants.STYLE_IMAGE_HEIGHT, Constants.DEFAULT_IMAGESIZE);
                        }
                    }
                }

                // Adds spacings
                double spacing = Utils.getDouble(style, Constants.STYLE_SPACING);
                dx += 2 * spacing;
                dx += Utils.getDouble(style, Constants.STYLE_SPACING_LEFT);
                dx += Utils.getDouble(style, Constants.STYLE_SPACING_RIGHT);

                dy += 2 * spacing;
                dy += Utils.getDouble(style, Constants.STYLE_SPACING_TOP);
                dy += Utils.getDouble(style, Constants.STYLE_SPACING_BOTTOM);

                // LATER: Add space for collapse/expand icon if applicable

                // Adds space for label
                String value = getLabel(cell);

                if (value != null && value.length() > 0) {
                    Rectangle size = Utils.getLabelSize(value, style, isHtmlLabel(cell), 1);
                    double width = size.getWidth() + dx;
                    double height = size.getHeight() + dy;

                    if (!Utils.isTrue(style, Constants.STYLE_HORIZONTAL, true)) {
                        double tmp = height;

                        height = width;
                        width = tmp;
                    }

                    if (gridEnabled) {
                        width = snap(width + gridSize / 2d);
                        height = snap(height + gridSize / 2d);
                    }

                    result = new Rectangle(0, 0, width, height);
                } else {
                    double gs2 = 4 * gridSize;
                    result = new Rectangle(0, 0, gs2, gs2);
                }
            }
        }

        return result;
    }

    /**
     * Sets the bounds of the given cell using resizeCells. Returns the
     * cell which was passed to the function.
     *
     * @param cell   <Cell> whose bounds should be changed.
     * @param bounds <Rectangle> that represents the new bounds.
     */
    public ICell resizeCell(ICell cell, Rectangle bounds) {
        return resizeCells(List.of(cell), new Rectangle[]{bounds}).get(0);
    }

    /**
     * Sets the bounds of the given cells and fires a Event.RESIZE_CELLS
     * event. while the transaction is in progress. Returns the cells which
     * have been passed to the function.
     *
     * @param cells  Array of cells whose bounds should be changed.
     * @param bounds Array of rectangles that represents the new bounds.
     */
    public List<ICell> resizeCells(List<ICell> cells, Rectangle[] bounds) {
        model.beginUpdate();
        try {
            cellsResized(cells, bounds);
            fireEvent(new ResizeCellsEvent(cells, bounds));
        } finally {
            model.endUpdate();
        }

        return cells;
    }

    /**
     * Sets the bounds of the given cells and fires a <Event.CELLS_RESIZED>
     * event. If extendParents is true, then the parent is extended if a child
     * size is changed so that it overlaps with the parent.
     *
     * @param cells  Array of <Cells> whose bounds should be changed.
     * @param bounds Array of <Rectangles> that represents the new bounds.
     */
    public void cellsResized(List<ICell> cells, Rectangle[] bounds) {
        if (cells != null && bounds != null && cells.size() == bounds.length) {
            model.beginUpdate();
            try {
                for (int i = 0; i < cells.size(); i++) {
                    Rectangle tmp = bounds[i];
                    ICell cell = cells.get(i);
                    Geometry geo = model.getGeometry(cell);

                    if (geo != null && (geo.getX() != tmp.getX() || geo.getY() != tmp.getY() || geo.getWidth() != tmp.getWidth() || geo.getHeight() != tmp.getHeight())) {
                        geo = geo.clone();

                        if (geo.isRelative()) {
                            Point offset = geo.getOffset();

                            if (offset != null) {
                                offset.setX(offset.getX() + tmp.getX());
                                offset.setY(offset.getY() + tmp.getY());
                            }
                        } else {
                            geo.setX(tmp.getX());
                            geo.setY(tmp.getY());
                        }

                        geo.setWidth(tmp.getWidth());
                        geo.setHeight(tmp.getHeight());

                        if (!geo.isRelative() && model.isVertex(cell) && !isAllowNegativeCoordinates()) {
                            geo.setX(Math.max(0, geo.getX()));
                            geo.setY(Math.max(0, geo.getY()));
                        }

                        model.setGeometry(cell, geo);

                        if (isExtendParent(cell)) {
                            extendParent(cell);
                        }
                    }
                }

                if (isResetEdgesOnResize()) {
                    resetEdges(cells);
                }
                fireEvent(new CellsResizedEvent(cells, bounds));
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Resizes the parents recursively so that they contain the complete area
     * of the resized child cell.
     *
     * @param cell <Cell> that has been resized.
     */
    public void extendParent(ICell cell) {
        if (cell != null) {
            ICell parent = model.getParent(cell);
            Geometry p = model.getGeometry(parent);

            if (parent != null && p != null && !isCellCollapsed(parent)) {
                Geometry geo = model.getGeometry(cell);

                if (geo != null && (p.getWidth() < geo.getX() + geo.getWidth() || p.getHeight() < geo.getY() + geo.getHeight())) {
                    p = p.clone();

                    p.setWidth(Math.max(p.getWidth(), geo.getX() + geo.getWidth()));
                    p.setHeight(Math.max(p.getHeight(), geo.getY() + geo.getHeight()));

                    cellsResized(List.of(parent), new Rectangle[]{p});
                }
            }
        }
    }

    /**
     * Moves the cells by the given amount. This is a shortcut method.
     */
    public List<ICell> moveCells(List<ICell> cells, double dx, double dy) {
        return moveCells(cells, dx, dy, false);
    }

    //
    // Cell moving
    //

    /**
     * Moves or clones the cells and moves the cells or clones by the given
     * amount. This is a shortcut method.
     */
    public List<ICell> moveCells(List<ICell> cells, double dx, double dy, boolean clone) {
        return moveCells(cells, dx, dy, clone, null, null);
    }

    /**
     * Moves or clones the specified cells and moves the cells or clones by the
     * given amount, adding them to the optional target cell. The location is
     * the position of the mouse pointer as the mouse was released. The change
     * is carried out using cellsMoved. This method fires Event.MOVE_CELLS
     * while the transaction is in progress.
     *
     * @param cells    Array of cells to be moved, cloned or added to the target.
     * @param dx       Integer that specifies the x-coordinate of the vector.
     * @param dy       Integer that specifies the y-coordinate of the vector.
     * @param clone    Boolean indicating if the cells should be cloned.
     * @param target   Cell that represents the new parent of the cells.
     * @param location Location where the mouse was released.
     * @return Returns the cells that were moved.
     */
    public List<ICell> moveCells(List<ICell> cells, double dx, double dy, boolean clone, ICell target, java.awt.Point location) {
        if (cells != null && (dx != 0 || dy != 0 || clone || target != null)) {
            model.beginUpdate();
            try {
                if (clone) {
                    cells = cloneCells(cells, isCloneInvalidEdges());

                    if (target == null) {
                        target = getDefaultParent();
                    }
                }

                // Need to disable allowNegativeCoordinates if target not null to
                // allow for temporary negative numbers until cellsAdded is called.
                boolean previous = isAllowNegativeCoordinates();

                if (target != null) {
                    setAllowNegativeCoordinates(true);
                }

                cellsMoved(cells, dx, dy, !clone && isDisconnectOnMove() && isAllowDanglingEdges(), target == null);

                setAllowNegativeCoordinates(previous);

                if (target != null) {
                    Integer index = model.getChildCount(target);
                    cellsAdded(cells, target, index, null, null, true);
                }
                fireEvent(new MoveCellsEvent(cells, dx, dy, clone, target, location));
            } finally {
                model.endUpdate();
            }
        }

        return cells;
    }

    /**
     * Moves the specified cells by the given vector, disconnecting the cells
     * using disconnectGraph if disconnect is true. This method fires
     * Event.CELLS_MOVED while the transaction is in progress.
     */
    public void cellsMoved(List<ICell> cells, double dx, double dy, boolean disconnect, boolean constrain) {
        if (cells != null && (dx != 0 || dy != 0)) {
            model.beginUpdate();
            try {
                if (disconnect) {
                    disconnectGraph(cells);
                }

                for (ICell cell : cells) {
                    translateCell(cell, dx, dy);

                    if (constrain) {
                        constrainChild(cell);
                    }
                }

                if (isResetEdgesOnMove()) {
                    resetEdges(cells);
                }
                fireEvent(new CellsMovedEvent(cells, dx, dy, disconnect));
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Translates the geometry of the given cell and stores the new,
     * translated geometry in the model as an atomic change.
     */
    public void translateCell(ICell cell, double dx, double dy) {
        Geometry geo = model.getGeometry(cell);

        if (geo != null) {
            geo = geo.clone();
            geo.translate(dx, dy);

            if (!geo.isRelative() && model.isVertex(cell) && !isAllowNegativeCoordinates()) {
                geo.setX(Math.max(0, geo.getX()));
                geo.setY(Math.max(0, geo.getY()));
            }

            if (geo.isRelative() && !model.isEdge(cell)) {
                if (geo.getOffset() == null) {
                    geo.setOffset(new Point(dx, dy));
                } else {
                    Point offset = geo.getOffset();

                    offset.setX(offset.getX() + dx);
                    offset.setY(offset.getY() + dy);
                }
            }

            model.setGeometry(cell, geo);
        }
    }

    /**
     * Returns the Rectangle inside which a cell is to be kept.
     */
    public Rectangle getCellContainmentArea(ICell cell) {
        if (cell != null && !model.isEdge(cell)) {
            ICell parent = model.getParent(cell);

            if (parent == getDefaultParent() || parent == getCurrentRoot()) {
                return getMaximumGraphBounds();
            } else if (parent != null && parent != getDefaultParent()) {
                Geometry g = model.getGeometry(parent);

                if (g != null) {
                    double x = 0;
                    double y = 0;
                    double w = g.getWidth();
                    double h = g.getHeight();

                    if (isSwimlane(parent)) {
                        Rectangle size = getStartSize(parent);

                        x = size.getWidth();
                        w -= size.getWidth();
                        y = size.getHeight();
                        h -= size.getHeight();
                    }

                    return new Rectangle(x, y, w, h);
                }
            }
        }

        return null;
    }

    /**
     * @return the maximumGraphBounds
     */
    public Rectangle getMaximumGraphBounds() {
        return maximumGraphBounds;
    }

    /**
     * @param value the maximumGraphBounds to set
     */
    public void setMaximumGraphBounds(Rectangle value) {
        Rectangle oldValue = maximumGraphBounds;
        maximumGraphBounds = value;

        changeSupport.firePropertyChange("maximumGraphBounds", oldValue, maximumGraphBounds);
    }

    /**
     * Keeps the given cell inside the bounds returned by
     * getCellContainmentArea for its parent, according to the rules defined by
     * getOverlap and isConstrainChild. This modifies the cell's geometry
     * in-place and does not clone it.
     *
     * @param cell Cell which should be constrained.
     */
    public void constrainChild(ICell cell) {
        if (cell != null) {
            Geometry geo = model.getGeometry(cell);
            Rectangle area = (isConstrainChild(cell)) ? getCellContainmentArea(cell) : getMaximumGraphBounds();

            if (geo != null && area != null) {
                // Keeps child within the content area of the parent
                if (!geo.isRelative() && (geo.getX() < area.getX() || geo.getY() < area.getY() || area.getWidth() < geo.getX() + geo.getWidth() || area.getHeight() < geo.getY() + geo.getHeight())) {
                    double overlap = getOverlap(cell);

                    if (area.getWidth() > 0) {
                        geo.setX(Math.min(geo.getX(), area.getX() + area.getWidth() - (1 - overlap) * geo.getWidth()));
                    }

                    if (area.getHeight() > 0) {
                        geo.setY(Math.min(geo.getY(), area.getY() + area.getHeight() - (1 - overlap) * geo.getHeight()));
                    }

                    geo.setX(Math.max(geo.getX(), area.getX() - geo.getWidth() * overlap));
                    geo.setY(Math.max(geo.getY(), area.getY() - geo.getHeight() * overlap));
                }
            }
        }
    }

    /**
     * Resets the control points of the edges that are connected to the given
     * cells if not both ends of the edge are in the given cells array.
     *
     * @param cells Array of Cells for which the connected edges should be
     *              reset.
     */
    public void resetEdges(List<ICell> cells) {
        if (cells != null) {
            // Prepares a hashtable for faster cell lookups
            HashSet<ICell> set = new HashSet<>(cells);

            model.beginUpdate();
            try {
                for (ICell cell : cells) {
                    List<ICell> edges = GraphModel.getEdges(model, cell);

                    for (ICell edge : edges) {
                        CellState state = view.getState(edge);
                        ICell source = (state != null) ? state.getVisibleTerminal(true) : view.getVisibleTerminal(edge, true);
                        ICell target = (state != null) ? state.getVisibleTerminal(false) : view.getVisibleTerminal(edge, false);

                        // Checks if one of the terminals is not in the given array
                        if (!set.contains(source) || !set.contains(target)) {
                            resetEdge(edge);
                        }
                    }

                    resetEdges(GraphModel.getChildren(model, cell));
                }
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Resets the control points of the given edge.
     */
    public ICell resetEdge(ICell edge) {
        Geometry geo = model.getGeometry(edge);

        if (geo != null) {
            // Resets the control points
            List<Point> points = geo.getPoints();

            if (points != null && !points.isEmpty()) {
                geo = geo.clone();
                geo.setPoints(null);
                model.setGeometry(edge, geo);
            }
        }

        return edge;
    }

    /**
     * Returns an array of all constraints for the given terminal.
     *
     * @param terminal Cell state that represents the terminal.
     * @param source   Specifies if the terminal is the source or target.
     */
    public ConnectionConstraint[] getAllConnectionConstraints(CellState terminal, boolean source) {
        return null;
    }

    //
    // Cell connecting and connection constraints
    //

    /**
     * Returns an connection constraint that describes the given connection
     * point. This result can then be passed to getConnectionPoint.
     *
     * @param edge     Cell state that represents the edge.
     * @param terminal Cell state that represents the terminal.
     * @param source   Boolean indicating if the terminal is the source or target.
     */
    public ConnectionConstraint getConnectionConstraint(CellState edge, CellState terminal, boolean source) {
        Point point = null;
        Object x = edge.getStyle().get((source) ? Constants.STYLE_EXIT_X : Constants.STYLE_ENTRY_X);

        if (x != null) {
            Object y = edge.getStyle().get((source) ? Constants.STYLE_EXIT_Y : Constants.STYLE_ENTRY_Y);

            if (y != null) {
                point = new Point(Double.parseDouble(x.toString()), Double.parseDouble(y.toString()));
            }
        }

        boolean perimeter = false;

        if (point != null) {
            perimeter = Utils.isTrue(edge.style, (source) ? Constants.STYLE_EXIT_PERIMETER : Constants.STYLE_ENTRY_PERIMETER, true);
        }

        return new ConnectionConstraint(point, perimeter);
    }

    /**
     * Sets the connection constraint that describes the given connection point.
     * If no constraint is given then nothing is changed. To remove an existing
     * constraint from the given edge, use an empty constraint instead.
     *
     * @param edge       Cell that represents the edge.
     * @param terminal   Cell that represents the terminal.
     * @param source     Boolean indicating if the terminal is the source or target.
     * @param constraint Optional connection constraint to be used for this connection.
     */
    public void setConnectionConstraint(ICell edge, ICell terminal, boolean source, ConnectionConstraint constraint) {
        if (constraint != null) {
            model.beginUpdate();
            try {
                List<ICell> cells = List.of(edge);

                if (constraint.point == null) {
                    setCellStyles((source) ? Constants.STYLE_EXIT_X : Constants.STYLE_ENTRY_X, null, cells);
                    setCellStyles((source) ? Constants.STYLE_EXIT_Y : Constants.STYLE_ENTRY_Y, null, cells);
                    setCellStyles((source) ? Constants.STYLE_EXIT_PERIMETER : Constants.STYLE_ENTRY_PERIMETER, null, cells);
                } else {
                    setCellStyles((source) ? Constants.STYLE_EXIT_X : Constants.STYLE_ENTRY_X, String.valueOf(constraint.point.getX()), cells);
                    setCellStyles((source) ? Constants.STYLE_EXIT_Y : Constants.STYLE_ENTRY_Y, String.valueOf(constraint.point.getY()), cells);

                    // Only writes 0 since 1 is default
                    if (!constraint.perimeter) {
                        setCellStyles((source) ? Constants.STYLE_EXIT_PERIMETER : Constants.STYLE_ENTRY_PERIMETER, "0", cells);
                    } else {
                        setCellStyles((source) ? Constants.STYLE_EXIT_PERIMETER : Constants.STYLE_ENTRY_PERIMETER, null, cells);
                    }
                }
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Sets the connection constraint that describes the given connection point.
     * If no constraint is given then nothing is changed. To remove an existing
     * constraint from the given edge, use an empty constraint instead.
     *
     * @param vertex     Cell state that represents the vertex.
     * @param constraint Connection constraint that represents the connection point
     *                   constraint as returned by getConnectionConstraint.
     */
    public Point getConnectionPoint(CellState vertex, ConnectionConstraint constraint) {
        Point point = null;

        if (vertex != null && constraint.point != null) {
            Rectangle bounds = this.view.getPerimeterBounds(vertex, 0);
            Point cx = new Point(bounds.getCenterX(), bounds.getCenterY());
            String direction = Utils.getString(vertex.getStyle(), Constants.STYLE_DIRECTION);

            double r1 = 0;

            // Bounds need to be rotated by 90 degrees for further computation
            if (direction != null) {
                switch (direction) {
                    case Constants.DIRECTION_NORTH -> r1 += 270;
                    case Constants.DIRECTION_WEST -> r1 += 180;
                    case Constants.DIRECTION_SOUTH -> r1 += 90;
                }

                // Bounds need to be rotated by 90 degrees for further computation
                if (direction.equals(Constants.DIRECTION_NORTH) || direction.equals(Constants.DIRECTION_SOUTH)) {
                    bounds.rotate90();
                }
            }

            point = new Point(bounds.getX() + constraint.point.getX() * bounds.getWidth(), bounds.getY() + constraint.point.getY() * bounds.getHeight());

            // Rotation for direction before projection on perimeter
            double r2 = Utils.getDouble(vertex.getStyle(), Constants.STYLE_ROTATION);

            if (constraint.perimeter) {
                if (r1 != 0) {
                    // Only 90 degrees steps possible here so no trig needed
                    double cos = 0;
                    double sin = 0;

                    if (r1 == 90) {
                        sin = 1;
                    } else if (r1 == 180) {
                        cos = -1;
                    } else if (r1 == 270) {
                        sin = -1;
                    }

                    point = Utils.getRotatedPoint(point, cos, sin, cx);
                }

                point = this.view.getPerimeterPoint(vertex, point, false);
            } else {
                r2 += r1;

                if (this.getModel().isVertex(vertex.cell)) {
                    boolean flipH = Utils.getString(vertex.getStyle(), Constants.STYLE_FLIPH).equals("1");
                    boolean flipV = Utils.getString(vertex.getStyle(), Constants.STYLE_FLIPV).equals("1");

                    if (flipH) {
                        point.setX(2 * bounds.getCenterX() - point.getX());
                    }

                    if (flipV) {
                        point.setY(2 * bounds.getCenterY() - point.getY());
                    }
                }

            }
            // Generic rotation after projection on perimeter
            if (r2 != 0 && point != null) {
                double rad = Math.toRadians(2);
                double cos = Math.cos(rad);
                double sin = Math.sin(rad);

                point = Utils.getRotatedPoint(point, cos, sin, cx);
            }
        }

        if (point != null) {
            point.setX(Math.round(point.getX()));
            point.setY(Math.round(point.getY()));
        }

        return point;
    }

    /**
     * Connects the specified end of the given edge to the given terminal
     * using cellConnected and fires Event.CONNECT_CELL while the transaction
     * is in progress.
     */
    public ICell connectCell(ICell edge, ICell terminal, boolean source) {
        return connectCell(edge, terminal, source, null);
    }

    /**
     * Connects the specified end of the given edge to the given terminal
     * using cellConnected and fires Event.CONNECT_CELL while the transaction
     * is in progress.
     *
     * @param edge       Edge whose terminal should be updated.
     * @param terminal   New terminal to be used.
     * @param source     Specifies if the new terminal is the source or target.
     * @param constraint Optional constraint to be used for this connection.
     * @return Returns the update edge.
     */
    public ICell connectCell(ICell edge, ICell terminal, boolean source, ConnectionConstraint constraint) {
        model.beginUpdate();
        try {
            ICell previous = model.getTerminal(edge, source);
            cellConnected(edge, terminal, source, constraint);
            fireEvent(new ConnectCellEvent(edge, terminal, previous, source));
        } finally {
            model.endUpdate();
        }

        return edge;
    }

    /**
     * Sets the new terminal for the given edge and resets the edge points if
     * isResetEdgesOnConnect returns true. This method fires
     * <Event.CELL_CONNECTED> while the transaction is in progress.
     *
     * @param edge       Edge whose terminal should be updated.
     * @param terminal   New terminal to be used.
     * @param source     Specifies if the new terminal is the source or target.
     * @param constraint Constraint to be used for this connection.
     */
    public void cellConnected(ICell edge, ICell terminal, boolean source, ConnectionConstraint constraint) {
        if (edge != null) {
            model.beginUpdate();
            try {
                ICell previous = model.getTerminal(edge, source);

                // Updates the constraint
                setConnectionConstraint(edge, terminal, source, constraint);

                // Checks if the new terminal is a port, uses the ID of the port in the
                // style and the parent of the port as the actual terminal of the edge.
                if (isPortsEnabled()) {
                    // Checks if the new terminal is a port
                    String id = null;

                    if (terminal != null && isPort(terminal)) {
                        id = terminal.getId();
                        terminal = getTerminalForPort(terminal, source);
                    }

                    // Sets or resets all previous information for connecting to a child port
                    String key = (source) ? Constants.STYLE_SOURCE_PORT : Constants.STYLE_TARGET_PORT;
                    setCellStyles(key, id, List.of(edge));
                }

                model.setTerminal(edge, terminal, source);

                if (isResetEdgesOnConnect()) {
                    resetEdge(edge);
                }
                fireEvent(new CellConnectedEvent(edge, terminal, previous, source));
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Disconnects the given edges from the terminals which are not in the
     * given array.
     *
     * @param cells Array of <Cells> to be disconnected.
     */
    public void disconnectGraph(List<ICell> cells) {
        if (cells != null) {
            model.beginUpdate();
            try {
                double scale = view.getScale();
                Point tr = view.getTranslate();

                // Prepares a hashtable for faster cell lookups

                Set<ICell> hash = new HashSet<>(cells);

                for (ICell cell : cells) {
                    if (model.isEdge(cell)) {
                        Geometry geo = model.getGeometry(cell);

                        if (geo != null) {
                            CellState state = view.getState(cell);
                            CellState pstate = view.getState(model.getParent(cell));

                            if (state != null && pstate != null) {
                                geo = geo.clone();

                                double dx = -pstate.getOrigin().getX();
                                double dy = -pstate.getOrigin().getY();

                                ICell src = model.getTerminal(cell, true);

                                if (src != null && isCellDisconnectable(cell, src, true)) {
                                    while (src != null && !hash.contains(src)) {
                                        src = model.getParent(src);
                                    }

                                    if (src == null) {
                                        Point pt = state.getAbsolutePoint(0);
                                        geo.setTerminalPoint(new Point(pt.getX() / scale - tr.getX() + dx, pt.getY() / scale - tr.getY() + dy), true);
                                        model.setTerminal(cell, null, true);
                                    }
                                }

                                ICell trg = model.getTerminal(cell, false);

                                if (trg != null && isCellDisconnectable(cell, trg, false)) {
                                    while (trg != null && !hash.contains(trg)) {
                                        trg = model.getParent(trg);
                                    }

                                    if (trg == null) {
                                        int n = state.getAbsolutePointCount() - 1;
                                        Point pt = state.getAbsolutePoint(n);
                                        geo.setTerminalPoint(new Point(pt.getX() / scale - tr.getX() + dx, pt.getY() / scale - tr.getY() + dy), false);
                                        model.setTerminal(cell, null, false);
                                    }
                                }
                            }

                            model.setGeometry(cell, geo);
                        }
                    }
                }
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Returns the current root of the displayed cell hierarchy. This is a
     * shortcut to <GraphView.currentRoot> in <view>.
     *
     * @return Returns the current root in the view.
     */
    public ICell getCurrentRoot() {
        return view.getCurrentRoot();
    }

    //
    // Drilldown
    //

    /**
     * Returns the translation to be used if the given cell is the root cell as
     * an <Point>. This implementation returns null.
     *
     * @param cell Cell that represents the root of the view.
     * @return Returns the translation of the graph for the given root cell.
     */
    public Point getTranslateForRoot(ICell cell) {
        return null;
    }

    /**
     * Returns true if the given cell is a "port", that is, when connecting to
     * it, the cell returned by getTerminalForPort should be used as the
     * terminal and the port should be referenced by the ID in either the
     * Constants.STYLE_SOURCE_PORT or the or the
     * Constants.STYLE_TARGET_PORT. Note that a port should not be movable.
     * This implementation always returns false.
     * <p>
     * A typical implementation of this method looks as follows:
     *
     * <code>
     * public boolean isPort(ICell cell)
     * {
     * Geometry geo = getCellGeometry(cell);
     * <p>
     * return (geo != null) ? geo.isRelative() : false;
     * }
     * </code>
     *
     * @param cell Cell that represents the port.
     * @return Returns true if the cell is a port.
     */
    public boolean isPort(ICell cell) {
        return false;
    }

    /**
     * Returns the terminal to be used for a given port. This implementation
     * always returns the parent cell.
     *
     * @param cell   Cell that represents the port.
     * @param source If the cell is the source or target port.
     * @return Returns the terminal to be used for the given port.
     */
    public ICell getTerminalForPort(ICell cell, boolean source) {
        return getModel().getParent(cell);
    }

    /**
     * Returns the offset to be used for the cells inside the given cell. The
     * root and layer cells may be identified using GraphModel.isRoot and
     * GraphModel.isLayer. This implementation returns null.
     *
     * @param cell Cell whose offset should be returned.
     * @return Returns the child offset for the given cell.
     */
    public Point getChildOffsetForCell(ICell cell) {
        return null;
    }

    public void enterGroup() {
        enterGroup(null);
    }

    /**
     * Uses the given cell as the root of the displayed cell hierarchy. If no
     * cell is specified then the selection cell is used. The cell is only used
     * if <isValidRoot> returns true.
     */
    public void enterGroup(ICell cell) {
        if (cell == null) {
            cell = getSelectionCell();
        }

        if (cell != null && isValidRoot(cell)) {
            view.setCurrentRoot(cell);
            clearSelection();
        }
    }

    /**
     * Changes the current root to the next valid root in the displayed cell
     * hierarchy.
     */
    public void exitGroup() {
        ICell root = model.getRoot();
        ICell current = getCurrentRoot();

        if (current != null) {
            ICell next = model.getParent(current);

            // Finds the next valid root in the hierarchy
            while (next != root && !isValidRoot(next) && model.getParent(next) != root) {
                next = model.getParent(next);
            }

            // Clears the current root if the new root is
            // the model's root or one of the layers.
            if (next == root || model.getParent(next) == root) {
                view.setCurrentRoot(null);
            } else {
                view.setCurrentRoot(next);
            }

            CellState state = view.getState(current);

            // Selects the previous root in the graph
            if (state != null) {
                setSelectionCell(current);
            }
        }
    }

    /**
     * Uses the root of the model as the root of the displayed cell hierarchy
     * and selects the previous root.
     */
    public void home() {
        ICell current = getCurrentRoot();

        if (current != null) {
            view.setCurrentRoot(null);
            CellState state = view.getState(current);

            if (state != null) {
                setSelectionCell(current);
            }
        }
    }

    /**
     * Returns true if the given cell is a valid root for the cell display
     * hierarchy. This implementation returns true for all non-null values.
     *
     * @param cell <Cell> which should be checked as a possible root.
     * @return Returns true if the given cell is a valid root.
     */
    public boolean isValidRoot(ICell cell) {
        return (cell != null);
    }

    /**
     * Returns the bounds of the visible graph.
     */
    public Rectangle getGraphBounds() {
        return view.getGraphBounds();
    }

    //
    // Graph display
    //

    /**
     * Returns the bounds of the given cell.
     */
    public Rectangle getCellBounds(ICell cell) {
        return getCellBounds(cell, false);
    }

    /**
     * Returns the bounds of the given cell including all connected edges
     * if includeEdge is true.
     */
    public Rectangle getCellBounds(ICell cell, boolean includeEdges) {
        return getCellBounds(cell, includeEdges, false);
    }

    /**
     * Returns the bounds of the given cell including all connected edges
     * if includeEdge is true.
     */
    public Rectangle getCellBounds(ICell cell, boolean includeEdges, boolean includeDescendants) {
        return getCellBounds(cell, includeEdges, includeDescendants, false);
    }

    /**
     * Returns the bounding box for the geometries of the vertices in the
     * given array of cells.
     */
    public Rectangle getBoundingBoxFromGeometry(List<ICell> cells) {
        Rectangle result = null;

        if (cells != null) {
            for (ICell cell : cells) {
                if (getModel().isVertex(cell)) {
                    Geometry geo = getCellGeometry(cell);

                    if (result == null) {
                        result = new Rectangle(geo);
                    } else {
                        result.add(geo);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns the bounds of the given cell.
     */
    public Rectangle getBoundingBox(ICell cell) {
        return getBoundingBox(cell, false);
    }

    /**
     * Returns the bounding box of the given cell including all connected edges
     * if includeEdge is true.
     */
    public Rectangle getBoundingBox(ICell cell, boolean includeEdges) {
        return getBoundingBox(cell, includeEdges, false);
    }

    /**
     * Returns the bounding box of the given cell including all connected edges
     * if includeEdge is true.
     */
    public Rectangle getBoundingBox(ICell cell, boolean includeEdges, boolean includeDescendants) {
        return getCellBounds(cell, includeEdges, includeDescendants, true);
    }

    /**
     * Returns the bounding box of the given cells and their descendants.
     */
    public Rectangle getPaintBounds(List<ICell> cells) {
        return getBoundsForCells(cells, false, true, true);
    }

    /**
     * Returns the bounds for the given cells.
     */
    public Rectangle getBoundsForCells(List<ICell> cells, boolean includeEdges, boolean includeDescendants, boolean boundingBox) {
        Rectangle result = null;

        if (cells != null && !cells.isEmpty()) {
            for (ICell cell : cells) {
                Rectangle tmp = getCellBounds(cell, includeEdges, includeDescendants, boundingBox);

                if (tmp != null) {
                    if (result == null) {
                        result = new Rectangle(tmp);
                    } else {
                        result.add(tmp);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns the bounds of the given cell including all connected edges
     * if includeEdge is true.
     */
    public Rectangle getCellBounds(ICell cell, boolean includeEdges, boolean includeDescendants, boolean boundingBox) {
        List<ICell> cells;

        // Recursively includes connected edges
        if (includeEdges) {
            Set<ICell> allCells = new HashSet<>();
            allCells.add(cell);

            Set<ICell> edges = new HashSet<>(getEdges(cell));

            while (!edges.isEmpty() && !allCells.containsAll(edges)) {
                allCells.addAll(edges);

                Set<ICell> tmp = new HashSet<>();

                for (ICell edge : edges) {
                    tmp.addAll(getEdges(edge));
                }

                edges = tmp;
            }

            cells = List.copyOf(allCells);
        } else {
            cells = List.of(cell);
        }

        Rectangle result = view.getBounds(cells, boundingBox);

        // Recursively includes the bounds of the children
        if (includeDescendants) {
            for (ICell o : cells) {
                int childCount = model.getChildCount(o);

                for (int j = 0; j < childCount; j++) {
                    Rectangle tmp = getCellBounds(model.getChildAt(o, j), includeEdges, true, boundingBox);

                    if (result != null) {
                        result.add(tmp);
                    } else {
                        result = tmp;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Clears all cell states or the states for the hierarchy starting at the
     * given cell and validates the graph.
     */
    public void refresh() {
        view.reload();
        repaint();
    }

    /**
     * Fires a repaint event.
     */
    public void repaint() {
        repaint(null);
    }

    /**
     * Fires a repaint event. The optional region is the rectangle that needs
     * to be repainted.
     */
    public void repaint(Rectangle region) {
        fireEvent(new RepaintEvent(region));
    }

    /**
     * Snaps the given numeric value to the grid if <gridEnabled> is true.
     *
     * @param value Numeric value to be snapped to the grid.
     * @return Returns the value aligned to the grid.
     */
    public double snap(double value) {
        if (gridEnabled) {
            value = Math.round(value / gridSize) * gridSize;
        }

        return value;
    }

    /**
     * Returns the geometry for the given cell.
     *
     * @param cell Cell whose geometry should be returned.
     * @return Returns the geometry of the cell.
     */
    public Geometry getCellGeometry(ICell cell) {
        return model.getGeometry(cell);
    }

    /**
     * Returns true if the given cell is visible in this graph. This
     * implementation uses <GraphModel.isVisible>. Subclassers can override
     * this to implement specific visibility for cells in only one graph, that
     * is, without affecting the visible state of the cell.
     * <p>
     * When using dynamic filter expressions for cell visibility, then the
     * graph should be revalidated after the filter expression has changed.
     *
     * @param cell Cell whose visible state should be returned.
     * @return Returns the visible state of the cell.
     */
    public boolean isCellVisible(ICell cell) {
        return model.isVisible(cell);
    }

    /**
     * Returns true if the given cell is collapsed in this graph. This
     * implementation uses <GraphModel.isCollapsed>. Subclassers can override
     * this to implement specific collapsed states for cells in only one graph,
     * that is, without affecting the collapsed state of the cell.
     * <p>
     * When using dynamic filter expressions for the collapsed state, then the
     * graph should be revalidated after the filter expression has changed.
     *
     * @param cell Cell whose collapsed state should be returned.
     * @return Returns the collapsed state of the cell.
     */
    public boolean isCellCollapsed(ICell cell) {
        return model.isCollapsed(cell);
    }

    /**
     * Returns true if the given cell is connectable in this graph. This
     * implementation uses <GraphModel.isConnectable>. Subclassers can override
     * this to implement specific connectable states for cells in only one graph,
     * that is, without affecting the connectable state of the cell in the model.
     *
     * @param cell Cell whose connectable state should be returned.
     * @return Returns the connectable state of the cell.
     */
    public boolean isCellConnectable(ICell cell) {
        return model.isConnectable(cell);
    }

    /**
     * Returns true if perimeter points should be computed such that the
     * resulting edge has only horizontal or vertical segments.
     *
     * @param edge Cell state that represents the edge.
     */
    public boolean isOrthogonal(CellState edge) {
        if (edge.getStyle().containsKey(Constants.STYLE_ORTHOGONAL)) {
            return Utils.isTrue(edge.getStyle(), Constants.STYLE_ORTHOGONAL);
        }

        EdgeStyle.EdgeStyleFunction tmp = view.getEdgeStyle(edge, null, null, null);

        return tmp == EdgeStyle.SegmentConnector || tmp == EdgeStyle.ElbowConnector || tmp == EdgeStyle.SideToSide || tmp == EdgeStyle.TopToBottom || tmp == EdgeStyle.EntityRelation || tmp == EdgeStyle.OrthConnector;
    }

    /**
     * Returns true if the given cell state is a loop.
     *
     * @param state <CellState> that represents a potential loop.
     * @return Returns true if the given cell is a loop.
     */
    public boolean isLoop(CellState state) {
        CellState src = state.getVisibleTerminalState(true);
        CellState trg = state.getVisibleTerminalState(false);

        return (src != null && src == trg);
    }

    //
    // Cell validation
    //

    public void setMultiplicities(Multiplicity[] value) {
        Multiplicity[] oldValue = multiplicities;
        multiplicities = value;

        changeSupport.firePropertyChange("multiplicities", oldValue, multiplicities);
    }

    /**
     * Checks if the return value of getEdgeValidationError for the given
     * arguments is null.
     *
     * @param edge   Cell that represents the edge to validate.
     * @param source Cell that represents the source terminal.
     * @param target Cell that represents the target terminal.
     */
    public boolean isEdgeValid(ICell edge, ICell source, ICell target) {
        return getEdgeValidationError(edge, source, target) == null;
    }

    /**
     * Returns the validation error message to be displayed when inserting or
     * changing an edges' connectivity. A return value of null means the edge
     * is valid, a return value of '' means it's not valid, but do not display
     * an error message. Any other (non-empty) string returned from this method
     * is displayed as an error message when trying to connect an edge to a
     * source and target. This implementation uses the multiplicities, as
     * well as multigraph and allowDanglingEdges to generate validation
     * errors.
     *
     * @param edge   Cell that represents the edge to validate.
     * @param source Cell that represents the source terminal.
     * @param target Cell that represents the target terminal.
     */
    public String getEdgeValidationError(ICell edge, ICell source, ICell target) {
        if (edge != null && !isAllowDanglingEdges() && (source == null || target == null)) {
            return "";
        }

        if (edge != null && model.getTerminal(edge, true) == null && model.getTerminal(edge, false) == null) {
            return null;
        }

        // Checks if we're dealing with a loop
        if (!isAllowLoops() && source == target && source != null) {
            return "";
        }

        // Checks if the connection is generally allowed
        if (!isValidConnection(source, target)) {
            return "";
        }

        if (source != null && target != null) {
            StringBuilder error = new StringBuilder();

            // Checks if the cells are already connected
            // and adds an error message if required
            if (!multigraph) {
                List<ICell> tmp = GraphModel.getEdgesBetween(model, source, target, true);

                // Checks if the source and target are not connected by another edge
                if (tmp.size() > 1 || (tmp.size() == 1 && tmp.get(0) != edge)) {
                    error.append(Resources.get("alreadyConnected", "Already Connected")).append("\n");
                }
            }

            // Gets the number of outgoing edges from the source
            // and the number of incoming edges from the target
            // without counting the edge being currently changed.
            int sourceOut = GraphModel.getDirectedEdgeCount(model, source, true, edge);
            int targetIn = GraphModel.getDirectedEdgeCount(model, target, false, edge);

            // Checks the change against each multiplicity rule
            if (multiplicities != null) {
                for (Multiplicity multiplicity : multiplicities) {
                    String err = multiplicity.check(this, edge, source, target, sourceOut, targetIn);

                    if (err != null) {
                        error.append(err);
                    }
                }
            }

            // Validates the source and target terminals independently
            String err = validateEdge(edge, source, target);

            if (err != null) {
                error.append(err);
            }

            return (error.length() > 0) ? error.toString() : null;
        }

        return (allowDanglingEdges) ? null : "";
    }

    /**
     * Hook method for subclassers to return an error message for the given
     * edge and terminals. This implementation returns null.
     *
     * @param edge   Cell that represents the edge to validate.
     * @param source Cell that represents the source terminal.
     * @param target Cell that represents the target terminal.
     */
    public String validateEdge(ICell edge, ICell source, ICell target) {
        return null;
    }

    /**
     * Checks all multiplicities that cannot be enforced while the graph is
     * being modified, namely, all multiplicities that require a minimum of
     * 1 edge.
     *
     * @param cell Cell for which the multiplicities should be checked.
     */
    public String getCellValidationError(ICell cell) {
        int outCount = GraphModel.getDirectedEdgeCount(model, cell, true);
        int inCount = GraphModel.getDirectedEdgeCount(model, cell, false);
        StringBuilder error = new StringBuilder();
        Object value = model.getValue(cell);

        if (multiplicities != null) {
            for (Multiplicity rule : multiplicities) {
                int max = rule.getMaxValue();

                if (rule.source && Utils.isNode(value, rule.type, rule.attr, rule.value) && ((max == 0 && outCount > 0) || (rule.min == 1 && outCount == 0) || (max == 1 && outCount > 1))) {
                    error.append(rule.countError).append('\n');
                } else if (!rule.source && Utils.isNode(value, rule.type, rule.attr, rule.value) && ((max == 0 && inCount > 0) || (rule.min == 1 && inCount == 0) || (max == 1 && inCount > 1))) {
                    error.append(rule.countError).append('\n');
                }
            }
        }

        return (error.length() > 0) ? error.toString() : null;
    }

    /**
     * Hook method for subclassers to return an error message for the given
     * cell and validation context. This implementation returns null.
     *
     * @param cell    Cell that represents the cell to validate.
     * @param context HashMap that represents the global validation state.
     */
    public String validateCell(ICell cell, HashMap<Object, Object> context) {
        return null;
    }

    /**
     * @return the labelsVisible
     */
    public boolean isLabelsVisible() {
        return labelsVisible;
    }

    //
    // Graph appearance
    //

    /**
     * @param value the labelsVisible to set
     */
    public void setLabelsVisible(boolean value) {
        boolean oldValue = labelsVisible;
        labelsVisible = value;

        changeSupport.firePropertyChange("labelsVisible", oldValue, labelsVisible);
    }

    public boolean isHtmlLabels() {
        return htmlLabels;
    }

    /**
     * @param value the htmlLabels to set
     */
    public void setHtmlLabels(boolean value) {
        boolean oldValue = htmlLabels;
        htmlLabels = value;

        changeSupport.firePropertyChange("htmlLabels", oldValue, htmlLabels);
    }

    /**
     * Returns the textual representation for the given cell.
     *
     * @param cell Cell to be converted to a string.
     * @return Returns the textual representation of the cell.
     */
    public String convertValueToString(ICell cell) {
        Object result = model.getValue(cell);

        return (result != null) ? result.toString() : "";
    }

    /**
     * Returns a string or DOM node that represents the label for the given
     * cell. This implementation uses <convertValueToString> if <labelsVisible>
     * is true. Otherwise it returns an empty string.
     *
     * @param cell <Cell> whose label should be returned.
     * @return Returns the label for the given cell.
     */
    public String getLabel(ICell cell) {
        String result = "";

        if (cell != null) {
            CellState state = view.getState(cell);
            Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cell);

            if (labelsVisible && !Utils.isTrue(style, Constants.STYLE_NOLABEL, false)) {
                result = convertValueToString(cell);
            }
        }

        return result;
    }

    /**
     * Sets the new label for a cell. If autoSize is true then
     * <cellSizeUpdated> will be called.
     *
     * @param cell     Cell whose label should be changed.
     * @param value    New label to be assigned.
     * @param autoSize Specifies if cellSizeUpdated should be called.
     */
    public void cellLabelChanged(ICell cell, Object value, boolean autoSize) {
        model.beginUpdate();
        try {
            getModel().setValue(cell, value);

            if (autoSize) {
                cellSizeUpdated(cell, false);
            }
        } finally {
            model.endUpdate();
        }
    }

    /**
     * Returns true if the label must be rendered as HTML markup. The default
     * implementation returns <htmlLabels>.
     *
     * @param cell <Cell> whose label should be displayed as HTML markup.
     * @return Returns true if the given cell label is HTML markup.
     */
    public boolean isHtmlLabel(ICell cell) {
        return isHtmlLabels();
    }

    /**
     * Returns the tooltip to be used for the given cell.
     */
    public String getToolTipForCell(ICell cell) {
        return convertValueToString(cell);
    }

    /**
     * Returns the start size of the given swimlane, that is, the width or
     * height of the part that contains the title, depending on the
     * horizontal style. The return value is an <Rectangle> with either
     * width or height set as appropriate.
     *
     * @param swimlane <Cell> whose start size should be returned.
     * @return Returns the startsize for the given swimlane.
     */
    public Rectangle getStartSize(ICell swimlane) {
        Rectangle result = new Rectangle();
        CellState state = view.getState(swimlane);
        Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(swimlane);

        if (style != null) {
            double size = Utils.getDouble(style, Constants.STYLE_STARTSIZE, Constants.DEFAULT_STARTSIZE);

            if (Utils.isTrue(style, Constants.STYLE_HORIZONTAL, true)) {
                result.setHeight(size);
            } else {
                result.setWidth(size);
            }
        }

        return result;
    }

    /**
     * Returns the image URL for the given cell state. This implementation
     * returns the value stored under <Constants.STYLE_IMAGE> in the cell
     * style.
     *
     * @return Returns the image associated with the given cell state.
     */
    public String getImage(CellState state) {
        return (state != null && state.getStyle() != null) ? Utils.getString(state.getStyle(), Constants.STYLE_IMAGE) : null;
    }

    /**
     * Sets the value of <border>.
     *
     * @param value Positive integer that represents the border to be used.
     */
    public void setBorder(int value) {
        border = value;
    }

    /**
     * Returns the default edge style used for loops.
     *
     * @return Returns the default loop style.
     */
    public EdgeStyle.EdgeStyleFunction getDefaultLoopStyle() {
        return defaultLoopStyle;
    }

    /**
     * Sets the default style used for loops.
     *
     * @param value Default style to be used for loops.
     */
    public void setDefaultLoopStyle(EdgeStyle.EdgeStyleFunction value) {
        EdgeStyle.EdgeStyleFunction oldValue = defaultLoopStyle;
        defaultLoopStyle = value;

        changeSupport.firePropertyChange("defaultLoopStyle", oldValue, defaultLoopStyle);
    }

    /**
     * Returns true if the given cell is a swimlane. This implementation always
     * returns false.
     *
     * @param cell Cell that should be checked.
     * @return Returns true if the cell is a swimlane.
     */
    public boolean isSwimlane(ICell cell) {
        if (cell != null) {
            if (model.getParent(cell) != model.getRoot()) {
                CellState state = view.getState(cell);
                Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cell);

                if (style != null && !model.isEdge(cell)) {
                    return Utils.getString(style, Constants.STYLE_SHAPE, "").equals(Constants.SHAPE_SWIMLANE);
                }
            }
        }

        return false;
    }

    /**
     * Returns true if the given cell may not be moved, sized, bended,
     * disconnected, edited or selected. This implementation returns true for
     * all vertices with a relative geometry if cellsLocked is false.
     *
     * @param cell Cell whose locked state should be returned.
     * @return Returns true if the given cell is locked.
     */
    public boolean isCellLocked(ICell cell) {
        Geometry geometry = model.getGeometry(cell);

        return isCellsLocked() || (geometry != null && model.isVertex(cell) && geometry.isRelative());
    }

    //
    // Cells and labels control options
    //

    /**
     * Sets cellsLocked, the default return value for isCellLocked and fires a
     * property change event for cellsLocked.
     */
    public void setCellsLocked(boolean value) {
        boolean oldValue = cellsLocked;
        cellsLocked = value;

        changeSupport.firePropertyChange("cellsLocked", oldValue, cellsLocked);
    }

    /**
     * Returns true if the given cell is movable. This implementation returns editable.
     *
     * @param cell Cell whose editable state should be returned.
     * @return Returns true if the cell is editable.
     */
    public boolean isCellEditable(ICell cell) {
        CellState state = view.getState(cell);
        Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cell);

        return isCellsEditable() && !isCellLocked(cell) && Utils.isTrue(style, Constants.STYLE_EDITABLE, true);
    }

    public void setCellsEditable(boolean value) {
        boolean oldValue = cellsEditable;
        cellsEditable = value;

        changeSupport.firePropertyChange("cellsEditable", oldValue, cellsEditable);
    }

    /**
     * Returns true if the given cell is resizable.
     *
     * @param cell Cell whose resizable state should be returned.
     * @return Returns true if the cell is sizable.
     */
    public boolean isCellResizable(ICell cell) {
        CellState state = view.getState(cell);
        Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cell);

        return isCellsResizable() && !isCellLocked(cell) && Utils.isTrue(style, Constants.STYLE_RESIZABLE, true);
    }

    public void setCellsResizable(boolean value) {
        boolean oldValue = cellsResizable;
        cellsResizable = value;

        changeSupport.firePropertyChange("cellsResizable", oldValue, cellsResizable);
    }

    /**
     * Returns the cells which are movable in the given array of cells.
     */
    public List<ICell> getMovableCells(List<ICell> cells) {
        return GraphModel.filterCells(cells, this::isCellMovable);
    }

    /**
     * Returns true if the given cell is movable. This implementation
     * returns movable.
     *
     * @param cell Cell whose movable state should be returned.
     * @return Returns true if the cell is movable.
     */
    public boolean isCellMovable(ICell cell) {
        CellState state = view.getState(cell);
        Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cell);

        return isCellsMovable() && !isCellLocked(cell) && Utils.isTrue(style, Constants.STYLE_MOVABLE, true);
    }

    public void setCellsMovable(boolean value) {
        boolean oldValue = cellsMovable;
        cellsMovable = value;

        changeSupport.firePropertyChange("cellsMovable", oldValue, cellsMovable);
    }

    /**
     * Function: isTerminalPointMovable
     * <p>
     * Returns true if the given terminal point is movable. This is independent
     * from isCellConnectable and isCellDisconnectable and controls if terminal
     * points can be moved in the graph if the edge is not connected. Note that
     * it is required for this to return true to connect unconnected edges.
     * This implementation returns true.
     *
     * @param cell   Cell whose terminal point should be moved.
     * @param source Boolean indicating if the source or target terminal should be moved.
     */
    public boolean isTerminalPointMovable(ICell cell, boolean source) {
        return true;
    }

    /**
     * Returns true if the given cell is bendable. This implementation returns
     * bendable. This is used in ElbowEdgeHandler to determine if the middle
     * handle should be shown.
     *
     * @param cell Cell whose bendable state should be returned.
     * @return Returns true if the cell is bendable.
     */
    public boolean isCellBendable(ICell cell) {
        CellState state = view.getState(cell);
        Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cell);

        return isCellsBendable() && !isCellLocked(cell) && Utils.isTrue(style, Constants.STYLE_BENDABLE, true);
    }

    public void setCellsBendable(boolean value) {
        boolean oldValue = cellsBendable;
        cellsBendable = value;

        changeSupport.firePropertyChange("cellsBendable", oldValue, cellsBendable);
    }

    /**
     * Returns true if the given cell is selectable. This implementation returns
     * <selectable>.
     *
     * @param cell <Cell> whose selectable state should be returned.
     * @return Returns true if the given cell is selectable.
     */
    public boolean isCellSelectable(ICell cell) {
        return isCellsSelectable();
    }

    public void setCellsSelectable(boolean value) {
        boolean oldValue = cellsSelectable;
        cellsSelectable = value;

        changeSupport.firePropertyChange("cellsSelectable", oldValue, cellsSelectable);
    }

    /**
     * Returns the cells which are deleatable in the given array of cells.
     */
    public List<ICell> getDeletableCells(List<ICell> cells) {
        return GraphModel.filterCells(cells, this::isCellDeletable);
    }

    /**
     * Returns true if the given cell is movable. This implementation always
     * returns true.
     *
     * @param cell Cell whose movable state should be returned.
     * @return Returns true if the cell is movable.
     */
    public boolean isCellDeletable(ICell cell) {
        CellState state = view.getState(cell);
        Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cell);

        return isCellsDeletable() && Utils.isTrue(style, Constants.STYLE_DELETABLE, true);
    }

    public void setCellsDeletable(boolean value) {
        boolean oldValue = cellsDeletable;
        cellsDeletable = value;

        changeSupport.firePropertyChange("cellsDeletable", oldValue, cellsDeletable);
    }

    /**
     * Returns the cells which are cloneable in the given array of cells.
     */
    public List<ICell> getCloneableCells(List<ICell> cells) {
        return GraphModel.filterCells(cells, this::isCellCloneable);
    }

    /**
     * Returns the constant true. This does not use the cloneable field to
     * return a value for a given cell, it is simply a hook for subclassers
     * to disallow cloning of individual cells.
     */
    public boolean isCellCloneable(ICell cell) {
        CellState state = view.getState(cell);
        Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cell);

        return isCellsCloneable() && Utils.isTrue(style, Constants.STYLE_CLONEABLE, true);
    }

    public void setCellsCloneable(boolean value) {
        boolean oldValue = cellsCloneable;
        cellsCloneable = value;

        changeSupport.firePropertyChange("cellsCloneable", oldValue, cellsCloneable);
    }

    /**
     * Returns true if the given cell is disconnectable from the source or
     * target terminal. This returns <disconnectable> for all given cells if
     * <isLocked> does not return true for the given cell.
     *
     * @param cell     <Cell> whose disconnectable state should be returned.
     * @param terminal <Cell> that represents the source or target terminal.
     * @param source   Boolean indicating if the source or target terminal is to be
     *                 disconnected.
     * @return Returns true if the given edge can be disconnected from the given
     * terminal.
     */
    public boolean isCellDisconnectable(ICell cell, ICell terminal, boolean source) {
        return isCellsDisconnectable() && !isCellLocked(cell);
    }

    public void setCellsDisconnectable(boolean value) {
        boolean oldValue = cellsDisconnectable;
        cellsDisconnectable = value;

        changeSupport.firePropertyChange("cellsDisconnectable", oldValue, cellsDisconnectable);
    }

    /**
     * Returns true if the overflow portion of labels should be hidden. If this
     * returns true then vertex labels will be clipped to the size of the vertices.
     * This implementation returns true if <Constants.STYLE_OVERFLOW> in the
     * style of the given cell is "hidden".
     *
     * @param cell Cell whose label should be clipped.
     * @return Returns true if the cell label should be clipped.
     */
    public boolean isLabelClipped(ICell cell) {
        if (!isLabelsClipped()) {
            CellState state = view.getState(cell);
            Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cell);

            return style != null && Utils.getString(style, Constants.STYLE_OVERFLOW, "").equals("hidden");
        }

        return isLabelsClipped();
    }

    public void setLabelsClipped(boolean value) {
        boolean oldValue = labelsClipped;
        labelsClipped = value;

        changeSupport.firePropertyChange("labelsClipped", oldValue, labelsClipped);
    }

    /**
     * Returns true if the given edges's label is moveable. This returns
     * <movable> for all given cells if <isLocked> does not return true
     * for the given cell.
     *
     * @param cell <Cell> whose label should be moved.
     * @return Returns true if the label of the given cell is movable.
     */
    public boolean isLabelMovable(ICell cell) {
        return !isCellLocked(cell) && ((model.isEdge(cell) && isEdgeLabelsMovable()) || (model.isVertex(cell) && isVertexLabelsMovable()));
    }

    public void setVertexLabelsMovable(boolean value) {
        boolean oldValue = vertexLabelsMovable;
        vertexLabelsMovable = value;

        changeSupport.firePropertyChange("vertexLabelsMovable", oldValue, vertexLabelsMovable);
    }

    public void setEdgeLabelsMovable(boolean value) {
        boolean oldValue = edgeLabelsMovable;
        edgeLabelsMovable = value;

        changeSupport.firePropertyChange("edgeLabelsMovable", oldValue, edgeLabelsMovable);
    }

    //
    // Graph control options
    //

    /**
     * Specifies if the graph should allow any interactions. This
     * implementation updates <enabled>.
     *
     * @param value Boolean indicating if the graph should be enabled.
     */
    public void setEnabled(boolean value) {
        boolean oldValue = enabled;
        enabled = value;

        changeSupport.firePropertyChange("enabled", oldValue, enabled);
    }

    public void setDropEnabled(boolean value) {
        boolean oldValue = dropEnabled;
        dropEnabled = value;

        changeSupport.firePropertyChange("dropEnabled", oldValue, dropEnabled);
    }

    public void setSplitEnabled(boolean value) {
        splitEnabled = value;
    }

    public void setMultigraph(boolean value) {
        boolean oldValue = multigraph;
        multigraph = value;

        changeSupport.firePropertyChange("multigraph", oldValue, multigraph);
    }

    public void setSwimlaneNesting(boolean value) {
        boolean oldValue = swimlaneNesting;
        swimlaneNesting = value;

        changeSupport.firePropertyChange("swimlaneNesting", oldValue, swimlaneNesting);
    }

    public void setAllowDanglingEdges(boolean value) {
        boolean oldValue = allowDanglingEdges;
        allowDanglingEdges = value;

        changeSupport.firePropertyChange("allowDanglingEdges", oldValue, allowDanglingEdges);
    }

    public void setCloneInvalidEdges(boolean value) {
        boolean oldValue = cloneInvalidEdges;
        cloneInvalidEdges = value;

        changeSupport.firePropertyChange("cloneInvalidEdges", oldValue, cloneInvalidEdges);
    }

    public void setDisconnectOnMove(boolean value) {
        boolean oldValue = disconnectOnMove;
        disconnectOnMove = value;

        changeSupport.firePropertyChange("disconnectOnMove", oldValue, disconnectOnMove);

    }

    public void setAllowLoops(boolean value) {
        boolean oldValue = allowLoops;
        allowLoops = value;

        changeSupport.firePropertyChange("allowLoops", oldValue, allowLoops);
    }

    public void setConnectableEdges(boolean value) {
        boolean oldValue = connectableEdges;
        connectableEdges = value;

        changeSupport.firePropertyChange("connectableEdges", oldValue, connectableEdges);

    }

    public void setResetEdgesOnMove(boolean value) {
        boolean oldValue = resetEdgesOnMove;
        resetEdgesOnMove = value;

        changeSupport.firePropertyChange("resetEdgesOnMove", oldValue, resetEdgesOnMove);
    }

    public void setResetViewOnRootChange(boolean value) {
        boolean oldValue = resetViewOnRootChange;
        resetViewOnRootChange = value;

        changeSupport.firePropertyChange("resetViewOnRootChange", oldValue, resetViewOnRootChange);
    }

    public void setResetEdgesOnResize(boolean value) {
        boolean oldValue = resetEdgesOnResize;
        resetEdgesOnResize = value;

        changeSupport.firePropertyChange("resetEdgesOnResize", oldValue, resetEdgesOnResize);
    }

    /**
     * Sets resetEdgesOnConnect.
     */
    public void setResetEdgesOnConnect(boolean value) {
        boolean oldValue = resetEdgesOnConnect;
        resetEdgesOnConnect = value;

        changeSupport.firePropertyChange("resetEdgesOnConnect", oldValue, resetEdgesOnResize);
    }

    /**
     * Returns true if the size of the given cell should automatically be
     * updated after a change of the label. This implementation returns
     * autoSize for all given cells or checks if the cell style does specify
     * Constants.STYLE_AUTOSIZE to be 1.
     *
     * @param cell Cell that should be resized.
     * @return Returns true if the size of the given cell should be updated.
     */
    public boolean isAutoSizeCell(ICell cell) {
        CellState state = view.getState(cell);
        Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cell);

        return isAutoSizeCells() || Utils.isTrue(style, Constants.STYLE_AUTOSIZE, false);
    }

    /**
     * Specifies if cell sizes should be automatically updated after a label
     * change. This implementation sets autoSize to the given parameter.
     *
     * @param value Boolean indicating if cells should be resized
     *              automatically.
     */
    public void setAutoSizeCells(boolean value) {
        boolean oldValue = autoSizeCells;
        autoSizeCells = value;

        changeSupport.firePropertyChange("autoSizeCells", oldValue, autoSizeCells);
    }

    /**
     * Returns true if the parent of the given cell should be extended if the
     * child has been resized so that it overlaps the parent. This
     * implementation returns ExtendParents if cell is not an edge.
     *
     * @param cell Cell that has been resized.
     */
    public boolean isExtendParent(ICell cell) {
        return !getModel().isEdge(cell) && isExtendParents();
    }

    /**
     * Sets extendParents.
     */
    public void setExtendParents(boolean value) {
        boolean oldValue = extendParents;
        extendParents = value;

        changeSupport.firePropertyChange("extendParents", oldValue, extendParents);
    }

    /**
     * Sets extendParentsOnAdd.
     */
    public void setExtendParentsOnAdd(boolean value) {
        boolean oldValue = extendParentsOnAdd;
        extendParentsOnAdd = value;

        changeSupport.firePropertyChange("extendParentsOnAdd", oldValue, extendParentsOnAdd);
    }

    /**
     * Returns true if the given cell should be kept inside the bounds of its
     * parent according to the rules defined by getOverlap and
     * isAllowOverlapParent. This implementation returns false for all children
     * of edges and isConstrainChildren() otherwise.
     */
    public boolean isConstrainChild(ICell cell) {
        return isConstrainChildren() && !getModel().isEdge(getModel().getParent(cell));
    }

    /**
     * @param value the constrainChildren to set
     */
    public void setConstrainChildren(boolean value) {
        boolean oldValue = constrainChildren;
        constrainChildren = value;

        changeSupport.firePropertyChange("constrainChildren", oldValue, constrainChildren);
    }

    /**
     * @param value the autoOrigin to set
     */
    public void setAutoOrigin(boolean value) {
        boolean oldValue = autoOrigin;
        autoOrigin = value;

        changeSupport.firePropertyChange("autoOrigin", oldValue, autoOrigin);
    }

    /**
     * @param value the origin to set
     */
    public void setOrigin(Point value) {
        Point oldValue = origin;
        origin = value;

        changeSupport.firePropertyChange("origin", oldValue, origin);
    }

    /**
     * @param value the changesRepaintThreshold to set
     */
    public void setChangesRepaintThreshold(int value) {
        int oldValue = changesRepaintThreshold;
        changesRepaintThreshold = value;

        changeSupport.firePropertyChange("changesRepaintThreshold", oldValue, changesRepaintThreshold);
    }

    /**
     * @param value the allowNegativeCoordinates to set
     */
    public void setAllowNegativeCoordinates(boolean value) {
        boolean oldValue = allowNegativeCoordinates;
        allowNegativeCoordinates = value;

        changeSupport.firePropertyChange("allowNegativeCoordinates", oldValue, allowNegativeCoordinates);
    }

    /**
     * @param value the collapseToPreferredSize to set
     */
    public void setCollapseToPreferredSize(boolean value) {
        boolean oldValue = collapseToPreferredSize;
        collapseToPreferredSize = value;

        changeSupport.firePropertyChange("collapseToPreferredSize", oldValue, collapseToPreferredSize);
    }

    /**
     * @param value the keepEdgesInForeground to set
     */
    public void setKeepEdgesInForeground(boolean value) {
        boolean oldValue = keepEdgesInForeground;
        keepEdgesInForeground = value;

        changeSupport.firePropertyChange("keepEdgesInForeground", oldValue, keepEdgesInForeground);
    }

    /**
     * @param value the keepEdgesInBackground to set
     */
    public void setKeepEdgesInBackground(boolean value) {
        boolean oldValue = keepEdgesInBackground;
        keepEdgesInBackground = value;

        changeSupport.firePropertyChange("keepEdgesInBackground", oldValue, keepEdgesInBackground);
    }

    /**
     * Returns true if the given cell is a valid source for new connections.
     * This implementation returns true for all non-null values and is
     * called by is called by <isValidConnection>.
     *
     * @param cell ICell that represents a possible source or null.
     * @return Returns true if the given cell is a valid source terminal.
     */
    public boolean isValidSource(ICell cell) {
        return (cell == null && allowDanglingEdges) || (cell != null && (!model.isEdge(cell) || isConnectableEdges()) && isCellConnectable(cell));
    }

    /**
     * Returns isValidSource for the given cell. This is called by
     * isValidConnection.
     *
     * @param cell ICell that represents a possible target or null.
     * @return Returns true if the given cell is a valid target.
     */
    public boolean isValidTarget(ICell cell) {
        return isValidSource(cell);
    }

    /**
     * Returns true if the given target cell is a valid target for source.
     * This is a boolean implementation for not allowing connections between
     * certain pairs of vertices and is called by <getEdgeValidationError>.
     * This implementation returns true if <isValidSource> returns true for
     * the source and <isValidTarget> returns true for the target.
     *
     * @param source ICell that represents the source cell.
     * @param target ICell that represents the target cell.
     * @return Returns true if the the connection between the given terminals
     * is valid.
     */
    public boolean isValidConnection(ICell source, ICell target) {
        return isValidSource(source) && isValidTarget(target) && (isAllowLoops() || source != target);
    }

    /**
     * @param value the minimumGraphSize to set
     */
    public void setMinimumGraphSize(Rectangle value) {
        Rectangle oldValue = minimumGraphSize;
        minimumGraphSize = value;

        changeSupport.firePropertyChange("minimumGraphSize", oldValue, value);
    }

    /**
     * Returns a decimal number representing the amount of the width and height
     * of the given cell that is allowed to overlap its parent. A value of 0
     * means all children must stay inside the parent, 1 means the child is
     * allowed to be placed outside of the parent such that it touches one of
     * the parents sides. If <isAllowOverlapParent> returns false for the given
     * cell, then this method returns 0.
     *
     * @return Returns the overlapping value for the given cell inside its
     * parent.
     */
    public double getOverlap(ICell cell) {
        return (isAllowOverlapParent(cell)) ? getDefaultOverlap() : 0;
    }

    /**
     * Sets defaultOverlap.
     */
    public void setDefaultOverlap(double value) {
        double oldValue = defaultOverlap;
        defaultOverlap = value;

        changeSupport.firePropertyChange("defaultOverlap", oldValue, value);
    }

    /**
     * Returns true if the given cell is allowed to be placed outside of the
     * parents area.
     *
     * @return Returns true if the given cell may overlap its parent.
     */
    public boolean isAllowOverlapParent(ICell cell) {
        return false;
    }

    /**
     * Returns the cells which are movable in the given array of cells.
     */
    public List<ICell> getFoldableCells(List<ICell> cells, final boolean collapse) {
        return GraphModel.filterCells(cells, cell -> isCellFoldable(cell, collapse));
    }

    /**
     * Returns true if the given cell is expandable. This implementation
     * returns true if the cell has at least one child and its style
     * does not specify Constants.STYLE_FOLDABLE to be 0.
     *
     * @param cell <Cell> whose expandable state should be returned.
     * @return Returns true if the given cell is expandable.
     */
    public boolean isCellFoldable(ICell cell, boolean collapse) {
        CellState state = view.getState(cell);
        Map<String, Object> style = (state != null) ? state.getStyle() : getCellStyle(cell);

        return model.getChildCount(cell) > 0 && Utils.isTrue(style, Constants.STYLE_FOLDABLE, true);
    }

    /**
     * Sets if the grid is enabled.
     *
     * @param value Specifies if the grid should be enabled.
     */
    public void setGridEnabled(boolean value) {
        boolean oldValue = gridEnabled;
        gridEnabled = value;

        changeSupport.firePropertyChange("gridEnabled", oldValue, gridEnabled);
    }

    /**
     * Sets if ports are enabled.
     *
     * @param value Specifies if the ports should be enabled.
     */
    public void setPortsEnabled(boolean value) {
        boolean oldValue = portsEnabled;
        portsEnabled = value;

        changeSupport.firePropertyChange("portsEnabled", oldValue, portsEnabled);
    }

    /**
     * Sets the grid size and fires a property change event for gridSize.
     *
     * @param value New grid size to be used.
     */
    public void setGridSize(int value) {
        int oldValue = gridSize;
        gridSize = value;

        changeSupport.firePropertyChange("gridSize", oldValue, gridSize);
    }

    /**
     * Sets alternateEdgeStyle.
     */
    public void setAlternateEdgeStyle(String value) {
        String oldValue = alternateEdgeStyle;
        alternateEdgeStyle = value;

        changeSupport.firePropertyChange("alternateEdgeStyle", oldValue, alternateEdgeStyle);
    }

    /**
     * Returns true if the given cell is a valid drop target for the specified
     * cells. This returns true if the cell is a swimlane, has children and is
     * not collapsed, or if splitEnabled is true and isSplitTarget returns
     * true for the given arguments
     *
     * @param cell  ICell that represents the possible drop target.
     * @param cells ICells that are going to be dropped.
     * @return Returns true if the cell is a valid drop target for the given
     * cells.
     */
    public boolean isValidDropTarget(ICell cell, List<ICell> cells) {
        return cell != null && ((isSplitEnabled() && isSplitTarget(cell, cells)) || (!model.isEdge(cell) && (isSwimlane(cell) || (model.getChildCount(cell) > 0 && !isCellCollapsed(cell)))));
    }

    /**
     * Returns true if split is enabled and the given edge may be splitted into
     * two edges with the given cell as a new terminal between the two.
     *
     * @param target ICell that represents the edge to be splitted.
     * @param cells  Array of cells to add into the given edge.
     * @return Returns true if the given edge may be splitted by the given
     * cell.
     */
    public boolean isSplitTarget(ICell target, List<ICell> cells) {
        if (target != null && cells != null && cells.size() == 1) {
            ICell src = model.getSource(target);
            ICell trg = model.getTarget(target);
            return (model.isEdge(target) && isCellConnectable(cells.get(0)) && getEdgeValidationError(target, model.getTerminal(target, true), cells.get(0)) == null && !model.isAncestor(cells.get(0), src) && !model.isAncestor(cells.get(0), trg));
        }

        return false;
    }

    /**
     * Returns the given cell if it is a drop target for the given cells or the
     * nearest ancestor that may be used as a drop target for the given cells.
     * If the given array contains a swimlane and swimlaneNesting is false
     * then this always returns null. If no cell is given, then the bottommost
     * swimlane at the location of the given event is returned.
     * <p>
     * This function should only be used if isDropEnabled returns true.
     */
    public ICell getDropTarget(List<ICell> cells, java.awt.Point pt, ICell cell) {
        if (!isSwimlaneNesting()) {
            for (ICell o : cells) {
                if (isSwimlane(o)) {
                    return null;
                }
            }
        }

        // FIXME the else below does nothing if swimlane is null
        ICell swimlane = null; //getSwimlaneAt(pt.x, pt.y);

        if (cell == null) {
            cell = swimlane;
        }
		/*else if (swimlane != null)
		{
			// Checks if the cell is an ancestor of the swimlane
			// under the mouse and uses the swimlane in that case
			ICell tmp = model.getParent(swimlane);

			while (tmp != null && isSwimlane(tmp) && tmp != cell)
			{
				tmp = model.getParent(tmp);
			}

			if (tmp == cell)
			{
				cell = swimlane;
			}
		}*/

        while (cell != null && !isValidDropTarget(cell, cells) && model.getParent(cell) != model.getRoot()) {
            cell = model.getParent(cell);
        }
        return (model.getParent(cell) != model.getRoot() && cell != null && cells != null && !cells.contains(cell)) ? cell : null;
    }

    /**
     * Returns the first child of the root in the model, that is, the first or
     * default layer of the diagram.
     *
     * @return Returns the default parent for new cells.
     */
    public ICell getDefaultParent() {
        ICell parent = defaultParent;

        if (parent == null) {
            parent = view.getCurrentRoot();

            if (parent == null) {
                ICell root = model.getRoot();
                parent = model.getChildAt(root, 0);
            }
        }

        return parent;
    }

    //
    // Cell retrieval
    //

    /**
     * Sets the default parent to be returned by getDefaultParent.
     * Set this to null to return the first child of the root in
     * getDefaultParent.
     */
    public void setDefaultParent(ICell value) {
        defaultParent = value;
    }

    /**
     * Returns the visible child vertices of the given parent.
     *
     * @param parent Cell whose children should be returned.
     */
    public List<ICell> getChildVertices(ICell parent) {
        return getChildCells(parent, true, false);
    }

    /**
     * Returns the visible child edges of the given parent.
     *
     * @param parent Cell whose children should be returned.
     */
    public List<ICell> getChildEdges(ICell parent) {
        return getChildCells(parent, false, true);
    }

    /**
     * Returns the visible children of the given parent.
     *
     * @param parent Cell whose children should be returned.
     */
    public List<ICell> getChildCells(ICell parent) {
        return getChildCells(parent, false, false);
    }

    /**
     * Returns the visible child vertices or edges in the given parent. If
     * vertices and edges is false, then all children are returned.
     *
     * @param parent   Cell whose children should be returned.
     * @param vertices Specifies if child vertices should be returned.
     * @param edges    Specifies if child edges should be returned.
     * @return Returns the child vertices and edges.
     */
    public List<ICell> getChildCells(ICell parent, boolean vertices, boolean edges) {
        List<ICell> cells = GraphModel.getChildCells(model, parent, vertices, edges);
        List<ICell> result = new ArrayList<>(cells.size());

        // Filters out the non-visible child cells
        for (ICell cell : cells) {
            if (isCellVisible(cell)) {
                result.add(cell);
            }
        }

        return result;
    }

    /**
     * Returns all visible edges connected to the given cell without loops.
     *
     * @param cell Cell whose connections should be returned.
     * @return Returns the connected edges for the given cell.
     */
    public List<ICell> getConnections(ICell cell) {
        return getConnections(cell, null);
    }

    /**
     * Returns all visible edges connected to the given cell without loops.
     * If the optional parent argument is specified, then only child
     * edges of the given parent are returned.
     *
     * @param cell   Cell whose connections should be returned.
     * @param parent Optional parent of the opposite end for a connection
     *               to be returned.
     * @return Returns the connected edges for the given cell.
     */
    public List<ICell> getConnections(ICell cell, ICell parent) {
        return getConnections(cell, parent, false);
    }

    /**
     * Returns all visible edges connected to the given cell without loops.
     * If the optional parent argument is specified, then only child
     * edges of the given parent are returned.
     *
     * @param cell   Cell whose connections should be returned.
     * @param parent Optional parent of the opposite end for a connection
     *               to be returned.
     * @return Returns the connected edges for the given cell.
     */
    public List<ICell> getConnections(ICell cell, ICell parent, boolean recurse) {
        return getEdges(cell, parent, true, true, false, recurse);
    }

    /**
     * Returns all incoming visible edges connected to the given cell without
     * loops.
     *
     * @param cell Cell whose incoming edges should be returned.
     * @return Returns the incoming edges of the given cell.
     */
    public List<ICell> getIncomingEdges(ICell cell) {
        return getIncomingEdges(cell, null);
    }

    /**
     * Returns the visible incoming edges for the given cell. If the optional
     * parent argument is specified, then only child edges of the given parent
     * are returned.
     *
     * @param cell   Cell whose incoming edges should be returned.
     * @param parent Optional parent of the opposite end for an edge
     *               to be returned.
     * @return Returns the incoming edges of the given cell.
     */
    public List<ICell> getIncomingEdges(ICell cell, ICell parent) {
        return getEdges(cell, parent, true, false, false);
    }

    /**
     * Returns all outgoing visible edges connected to the given cell without
     * loops.
     *
     * @param cell Cell whose outgoing edges should be returned.
     * @return Returns the outgoing edges of the given cell.
     */
    public List<ICell> getOutgoingEdges(ICell cell) {
        return getOutgoingEdges(cell, null);
    }

    /**
     * Returns the visible outgoing edges for the given cell. If the optional
     * parent argument is specified, then only child edges of the given parent
     * are returned.
     *
     * @param cell   Cell whose outgoing edges should be returned.
     * @param parent Optional parent of the opposite end for an edge
     *               to be returned.
     * @return Returns the outgoing edges of the given cell.
     */
    public List<ICell> getOutgoingEdges(ICell cell, ICell parent) {
        return getEdges(cell, parent, false, true, false);
    }

    /**
     * Returns all visible edges connected to the given cell including loops.
     *
     * @param cell Cell whose edges should be returned.
     * @return Returns the edges of the given cell.
     */
    public List<ICell> getEdges(ICell cell) {
        return getEdges(cell, null);
    }

    /**
     * Returns all visible edges connected to the given cell including loops.
     *
     * @param cell   Cell whose edges should be returned.
     * @param parent Optional parent of the opposite end for an edge
     *               to be returned.
     * @return Returns the edges of the given cell.
     */
    public List<ICell> getEdges(ICell cell, ICell parent) {
        return getEdges(cell, parent, true, true, true);
    }

    /**
     * Returns the incoming and/or outgoing edges for the given cell.
     * If the optional parent argument is specified, then only edges are returned
     * where the opposite is in the given parent cell.
     *
     * @param cell         Cell whose edges should be returned.
     * @param parent       Optional parent. If specified the opposite end of any edge
     *                     must be a direct child of that parent in order for the edge to be returned.
     * @param incoming     Specifies if incoming edges should be included in the
     *                     result.
     * @param outgoing     Specifies if outgoing edges should be included in the
     *                     result.
     * @param includeLoops Specifies if loops should be included in the result.
     * @return Returns the edges connected to the given cell.
     */
    public List<ICell> getEdges(ICell cell, ICell parent, boolean incoming, boolean outgoing, boolean includeLoops) {
        return getEdges(cell, parent, incoming, outgoing, includeLoops, false);
    }

    /**
     * Returns the incoming and/or outgoing edges for the given cell.
     * If the optional parent argument is specified, then only edges are returned
     * where the opposite is in the given parent cell.
     *
     * @param cell         Cell whose edges should be returned.
     * @param parent       Optional parent. If specified the opposite end of any edge
     *                     must be a child of that parent in order for the edge to be returned. The
     *                     recurse parameter specifies whether or not it must be the direct child
     *                     or the parent just be an ancestral parent.
     * @param incoming     Specifies if incoming edges should be included in the
     *                     result.
     * @param outgoing     Specifies if outgoing edges should be included in the
     *                     result.
     * @param includeLoops Specifies if loops should be included in the result.
     * @param recurse      Specifies if the parent specified only need be an ancestral
     *                     parent, <code>true</code>, or the direct parent, <code>false</code>
     * @return Returns the edges connected to the given cell.
     */
    public List<ICell> getEdges(ICell cell, ICell parent, boolean incoming, boolean outgoing, boolean includeLoops, boolean recurse) {
        boolean isCollapsed = isCellCollapsed(cell);
        List<ICell> edges = new ArrayList<>();
        int childCount = model.getChildCount(cell);

        for (int i = 0; i < childCount; i++) {
            ICell child = model.getChildAt(cell, i);

            if (isCollapsed || !isCellVisible(child)) {
                edges.addAll(GraphModel.getEdges(model, child, incoming, outgoing, includeLoops));
            }
        }

        edges.addAll(GraphModel.getEdges(model, cell, incoming, outgoing, includeLoops));
        List<ICell> result = new ArrayList<>(edges.size());

        for (ICell edge : edges) {
            CellState state = view.getState(edge);
            ICell source = (state != null) ? state.getVisibleTerminal(true) : view.getVisibleTerminal(edge, true);
            ICell target = (state != null) ? state.getVisibleTerminal(false) : view.getVisibleTerminal(edge, false);

            if ((includeLoops && source == target) || ((source != target) && ((incoming && target == cell && (parent == null || isValidAncestor(source, parent, recurse))) || (outgoing && source == cell && (parent == null || isValidAncestor(target, parent, recurse)))))) {
                result.add(edge);
            }
        }

        return result;
    }

    /**
     * Returns whether or not the specified parent is a valid
     * ancestor of the specified cell, either direct or indirectly
     * based on whether ancestor recursion is enabled.
     *
     * @param cell    the possible child cell
     * @param parent  the possible parent cell
     * @param recurse whether or not to recurse the child ancestors
     * @return whether or not the specified parent is a valid
     * ancestor of the specified cell, either direct or indirectly
     * based on whether ancestor recursion is enabled.
     */
    public boolean isValidAncestor(ICell cell, ICell parent, boolean recurse) {
        return (recurse ? model.isAncestor(parent, cell) : model.getParent(cell) == parent);
    }

    /**
     * Returns all distinct visible opposite cells of the terminal on the given
     * edges.
     *
     * @return Returns the terminals at the opposite ends of the given edges.
     */
    public List<ICell> getOpposites(List<ICell> edges, ICell terminal) {
        return getOpposites(edges, terminal, true, true);
    }

    /**
     * Returns all distinct visible opposite cells for the specified terminal
     * on the given edges.
     *
     * @param edges    Edges whose opposite terminals should be returned.
     * @param terminal Terminal that specifies the end whose opposite should be
     *                 returned.
     * @param sources  Specifies if source terminals should be included in the
     *                 result.
     * @param targets  Specifies if target terminals should be included in the
     *                 result.
     * @return Returns the cells at the opposite ends of the given edges.
     */
    public List<ICell> getOpposites(List<ICell> edges, ICell terminal, boolean sources, boolean targets) {
        Collection<ICell> terminals = new LinkedHashSet<>();

        if (edges != null) {
            for (ICell edge : edges) {
                CellState state = view.getState(edge);
                ICell source = (state != null) ? state.getVisibleTerminal(true) : view.getVisibleTerminal(edge, true);
                ICell target = (state != null) ? state.getVisibleTerminal(false) : view.getVisibleTerminal(edge, false);

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

        return List.copyOf(terminals);
    }

    /**
     * Returns the edges between the given source and target. This takes into
     * account collapsed and invisible cells and returns the connected edges
     * as displayed on the screen.
     *
     * @return Returns all edges between the given terminals.
     */
    public List<ICell> getEdgesBetween(ICell source, ICell target) {
        return getEdgesBetween(source, target, false);
    }

    /**
     * Returns the edges between the given source and target. This takes into
     * account collapsed and invisible cells and returns the connected edges
     * as displayed on the screen.
     *
     * @return Returns all edges between the given terminals.
     */
    public List<ICell> getEdgesBetween(ICell source, ICell target, boolean directed) {
        List<ICell> edges = getEdges(source);
        List<ICell> result = new ArrayList<>(edges.size());

        // Checks if the edge is connected to the correct
        // cell and adds any match to the result
        for (ICell edge : edges) {
            CellState state = view.getState(edge);
            ICell src = (state != null) ? state.getVisibleTerminal(true) : view.getVisibleTerminal(edge, true);
            ICell trg = (state != null) ? state.getVisibleTerminal(false) : view.getVisibleTerminal(edge, false);

            if ((src == source && trg == target) || (!directed && src == target && trg == source)) {
                result.add(edge);
            }
        }

        return result;
    }

    /**
     * Returns the children of the given parent that are contained in the
     * halfpane from the given point (x0, y0) rightwards and downwards
     * depending on rightHalfpane and bottomHalfpane.
     *
     * @param x0             X-coordinate of the origin.
     * @param y0             Y-coordinate of the origin.
     * @param parent         <Cell> whose children should be checked.
     * @param rightHalfpane  Boolean indicating if the cells in the right halfpane
     *                       from the origin should be returned.
     * @param bottomHalfpane Boolean indicating if the cells in the bottom halfpane
     *                       from the origin should be returned.
     * @return Returns the cells beyond the given halfpane.
     */
    public List<ICell> getCellsBeyond(double x0, double y0, ICell parent, boolean rightHalfpane, boolean bottomHalfpane) {
        if (parent == null) {
            parent = getDefaultParent();
        }

        int childCount = model.getChildCount(parent);
        List<ICell> result = new ArrayList<>(childCount);

        if (rightHalfpane || bottomHalfpane) {

            if (parent != null) {
                for (int i = 0; i < childCount; i++) {
                    ICell child = model.getChildAt(parent, i);
                    CellState state = view.getState(child);

                    if (isCellVisible(child) && state != null) {
                        if ((!rightHalfpane || state.getX() >= x0) && (!bottomHalfpane || state.getY() >= y0)) {
                            result.add(child);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns all visible children in the given parent which do not have
     * incoming edges. If the result is empty then the with the greatest
     * difference between incoming and outgoing edges is returned. This
     * takes into account edges that are being promoted to the given
     * root due to invisible children or collapsed cells.
     *
     * @param parent Cell whose children should be checked.
     * @return List of tree roots in parent.
     */
    public List<ICell> findTreeRoots(ICell parent) {
        return findTreeRoots(parent, false);
    }

    /**
     * Returns all visible children in the given parent which do not have
     * incoming edges. If the result is empty then the children with the
     * maximum difference between incoming and outgoing edges are returned.
     * This takes into account edges that are being promoted to the given
     * root due to invisible children or collapsed cells.
     *
     * @param parent  Cell whose children should be checked.
     * @param isolate Specifies if edges should be ignored if the opposite
     *                end is not a child of the given parent cell.
     * @return List of tree roots in parent.
     */
    public List<ICell> findTreeRoots(ICell parent, boolean isolate) {
        return findTreeRoots(parent, isolate, false);
    }

    /**
     * Returns all visible children in the given parent which do not have
     * incoming edges. If the result is empty then the children with the
     * maximum difference between incoming and outgoing edges are returned.
     * This takes into account edges that are being promoted to the given
     * root due to invisible children or collapsed cells.
     *
     * @param parent  Cell whose children should be checked.
     * @param isolate Specifies if edges should be ignored if the opposite
     *                end is not a child of the given parent cell.
     * @param invert  Specifies if outgoing or incoming edges should be counted
     *                for a tree root. If false then outgoing edges will be counted.
     * @return List of tree roots in parent.
     */
    public List<ICell> findTreeRoots(ICell parent, boolean isolate, boolean invert) {
        List<ICell> roots = new ArrayList<>();

        if (parent != null) {
            int childCount = model.getChildCount(parent);
            ICell best = null;
            int maxDiff = 0;

            for (int i = 0; i < childCount; i++) {
                ICell cell = model.getChildAt(parent, i);

                if (model.isVertex(cell) && isCellVisible(cell)) {
                    List<ICell> conns = getConnections(cell, (isolate) ? parent : null);
                    int fanOut = 0;
                    int fanIn = 0;

                    for (ICell conn : conns) {
                        ICell src = view.getVisibleTerminal(conn, true);

                        if (src == cell) {
                            fanOut++;
                        } else {
                            fanIn++;
                        }
                    }

                    if ((invert && fanOut == 0 && fanIn > 0) || (!invert && fanIn == 0 && fanOut > 0)) {
                        roots.add(cell);
                    }

                    int diff = (invert) ? fanIn - fanOut : fanOut - fanIn;

                    if (diff > maxDiff) {
                        maxDiff = diff;
                        best = cell;
                    }
                }
            }

            if (roots.isEmpty() && best != null) {
                roots.add(best);
            }
        }

        return roots;
    }

    /**
     * Traverses the tree starting at the given vertex. Here is how to use this
     * method for a given vertex (root) which is typically the root of a tree:
     * <code>
     * graph.traverse(root, true, new ICellVisitor()
     * {
     * public boolean visit(ICell vertex, ICell edge)
     * {
     * System.out.println("edge="+graph.convertValueToString(edge)+
     * " vertex="+graph.convertValueToString(vertex));
     * <p>
     * return true;
     * }
     * });
     * </code>
     */
    public void traverse(ICell vertex, boolean directed, ICellVisitor visitor) {
        traverse(vertex, directed, visitor, null, null);
    }

    /**
     * Traverses the (directed) graph invoking the given function for each
     * visited vertex and edge. The function is invoked with the current vertex
     * and the incoming edge as a parameter. This implementation makes sure
     * each vertex is only visited once. The function may return false if the
     * traversal should stop at the given vertex.
     *
     * @param vertex   <Cell> that represents the vertex where the traversal starts.
     * @param directed Optional boolean indicating if edges should only be traversed
     *                 from source to target. Default is true.
     * @param visitor  Visitor that takes the current vertex and the incoming edge.
     *                 The traversal stops if the function returns false.
     * @param edge     Optional <Cell> that represents the incoming edge. This is
     *                 null for the first step of the traversal.
     * @param visited  Optional array of cell paths for the visited cells.
     */
    public void traverse(ICell vertex, boolean directed, ICellVisitor visitor, ICell edge, Set<ICell> visited) {
        if (vertex != null && visitor != null) {
            if (visited == null) {
                visited = new HashSet<>();
            }

            if (!visited.contains(vertex)) {
                visited.add(vertex);

                if (visitor.visit(vertex, edge)) {
                    int edgeCount = model.getEdgeCount(vertex);

                    if (edgeCount > 0) {
                        for (int i = 0; i < edgeCount; i++) {
                            ICell e = model.getEdgeAt(vertex, i);
                            boolean isSource = model.getTerminal(e, true) == vertex;

                            if (!directed || isSource) {
                                ICell next = model.getTerminal(e, !isSource);
                                traverse(next, directed, visitor, e, visited);
                            }
                        }
                    }
                }
            }
        }
    }

    //
    // Selection
    //

    public int getSelectionCount() {
        return selectionModel.size();
    }

    /**
     * @return Returns true if the given cell is selected.
     */
    public boolean isCellSelected(ICell cell) {
        return selectionModel.isSelected(cell);
    }

    /**
     * @return Returns true if the selection is empty.
     */
    public boolean isSelectionEmpty() {
        return selectionModel.isEmpty();
    }

    public void clearSelection() {
        selectionModel.clear();
    }

    /**
     * @return Returns the selection cell.
     */
    public ICell getSelectionCell() {
        return selectionModel.getCell();
    }

    public void setSelectionCell(ICell cell) {
        selectionModel.setCell(cell);
    }

    /**
     * @return Returns the selection cells.
     */
    public List<ICell> getSelectionCells() {
        return List.copyOf(selectionModel.getCells());
    }

    public void setSelectionCells(List<ICell> cells) {
        selectionModel.setCells(cells);
    }

    public void addSelectionCell(ICell cell) {
        selectionModel.addCell(cell);
    }

    public void addSelectionCells(List<ICell> cells) {
        selectionModel.addCells(cells);
    }

    public void removeSelectionCell(ICell cell) {
        selectionModel.removeCell(cell);
    }

    public void removeSelectionCells(List<ICell> cells) {
        selectionModel.removeCells(cells);
    }

    /**
     * Selects the next cell.
     */
    public void selectNextCell() {
        selectCell(true, false, false);
    }

    /**
     * Selects the previous cell.
     */
    public void selectPreviousCell() {
        selectCell(false, false, false);
    }

    /**
     * Selects the parent cell.
     */
    public void selectParentCell() {
        selectCell(false, true, false);
    }

    /**
     * Selects the first child cell.
     */
    public void selectChildCell() {
        selectCell(false, false, true);
    }

    /**
     * Selects the next, parent, first child or previous cell, if all arguments
     * are false.
     */
    public void selectCell(boolean isNext, boolean isParent, boolean isChild) {
        ICell cell = getSelectionCell();

        if (getSelectionCount() > 1) {
            clearSelection();
        }

        ICell parent = (cell != null) ? model.getParent(cell) : getDefaultParent();
        int childCount = model.getChildCount(parent);

        if (cell == null && childCount > 0) {
            ICell child = model.getChildAt(parent, 0);
            setSelectionCell(child);
        } else if ((cell == null || isParent) && view.getState(parent) != null && model.getGeometry(parent) != null) {
            if (getCurrentRoot() != parent) {
                setSelectionCell(parent);
            }
        } else if (cell != null && isChild) {
            int tmp = model.getChildCount(cell);

            if (tmp > 0) {
                ICell child = model.getChildAt(cell, 0);
                setSelectionCell(child);
            }
        } else if (childCount > 0) {
            int i = parent.getIndex(cell);

            if (isNext) {
                i++;
                setSelectionCell(model.getChildAt(parent, i % childCount));
            } else {
                i--;
                int index = (i < 0) ? childCount - 1 : i;
                setSelectionCell(model.getChildAt(parent, index));
            }
        }
    }

    /**
     * Selects all vertices inside the default parent.
     */
    public void selectVertices() {
        selectVertices(null);
    }

    /**
     * Selects all vertices inside the given parent or the default parent
     * if no parent is given.
     */
    public void selectVertices(ICell parent) {
        selectCells(true, false, parent);
    }

    /**
     * Selects all vertices inside the default parent.
     */
    public void selectEdges() {
        selectEdges(null);
    }

    /**
     * Selects all vertices inside the given parent or the default parent
     * if no parent is given.
     */
    public void selectEdges(ICell parent) {
        selectCells(false, true, parent);
    }

    /**
     * Selects all vertices and/or edges depending on the given boolean
     * arguments recursively, starting at the default parent. Use
     * <code>selectAll</code> to select all cells.
     *
     * @param vertices Boolean indicating if vertices should be selected.
     * @param edges    Boolean indicating if edges should be selected.
     */
    public void selectCells(boolean vertices, boolean edges) {
        selectCells(vertices, edges, null);
    }

    /**
     * Selects all vertices and/or edges depending on the given boolean
     * arguments recursively, starting at the given parent or the default
     * parent if no parent is specified. Use <code>selectAll</code> to select
     * all cells.
     *
     * @param vertices Boolean indicating if vertices should be selected.
     * @param edges    Boolean indicating if edges should be selected.
     * @param parent   Optional cell that acts as the root of the recursion.
     *                 Default is <code>defaultParent</code>.
     */
    public void selectCells(final boolean vertices, final boolean edges, ICell parent) {

        List<ICell> cells = GraphModel.filterDescendants(getModel(), cell -> view.getState(cell) != null && model.getChildCount(cell) == 0 && ((model.isVertex(cell) && vertices) || (model.isEdge(cell) && edges)));
        setSelectionCells(cells);
    }

    public void selectAll() {
        selectAll(null);
    }

    /**
     * Selects all children of the given parent cell or the children of the
     * default parent if no parent is specified. To select leaf vertices and/or
     * edges use <selectCells>.
     *
     * @param parent Optional <Cell> whose children should be selected.
     *               Default is <defaultParent>.
     */
    public void selectAll(ICell parent) {
        if (parent == null) {
            parent = getDefaultParent();
        }

        List<ICell> children = GraphModel.getChildren(model, parent);

        setSelectionCells(children);
    }

    /**
     * Draws the graph onto the given canvas.
     *
     * @param canvas Canvas onto which the graph should be drawn.
     */
    public void drawGraph(ICanvas canvas) {
        drawCell(canvas, getModel().getRoot());
    }

    //
    // Images and drawing
    //

    /**
     * Draws the given cell and its descendants onto the specified canvas.
     *
     * @param canvas Canvas onto which the cell should be drawn.
     * @param cell   Cell that should be drawn onto the canvas.
     */
    public void drawCell(ICanvas canvas, ICell cell) {
        drawState(canvas, getView().getState(cell), true);

        // Draws the children on top of their parent
        int childCount = model.getChildCount(cell);

        for (int i = 0; i < childCount; i++) {
            ICell child = model.getChildAt(cell, i);
            drawCell(canvas, child);
        }
    }

    /**
     * Draws the cell state with the given label onto the canvas. No
     * children or descendants are painted here. This method invokes
     * cellDrawn after the cell, but not its descendants have been
     * painted.
     *
     * @param canvas    Canvas onto which the cell should be drawn.
     * @param state     State of the cell to be drawn.
     * @param drawLabel Indicates if the label should be drawn.
     */
    public void drawState(ICanvas canvas, CellState state, boolean drawLabel) {
        ICell cell = (state != null) ? state.getCell() : null;

        if (cell != null && cell != view.getCurrentRoot() && cell != model.getRoot() && (model.isVertex(cell) || model.isEdge(cell))) {
            Object obj = canvas.drawCell(state);
            Object lab = null;

            // Holds the current clipping region in case the label will
            // be clipped
            Shape clip = null;
            java.awt.Rectangle newClip = state.getRectangle();

            // Indirection for image canvas that contains a graphics canvas
            ICanvas clippedCanvas = (isLabelClipped(state.getCell())) ? canvas : null;

            if (clippedCanvas instanceof ImageCanvas) {
                clippedCanvas = ((ImageCanvas) clippedCanvas).getGraphicsCanvas();
                // TODO: Shift newClip to match the image offset
                //Point pt = ((ImageCanvas) canvas).getTranslate();
                //newClip.translate(-pt.x, -pt.y);
            }

            if (clippedCanvas instanceof Graphics2DCanvas) {
                Graphics g = ((Graphics2DCanvas) clippedCanvas).getGraphics();
                clip = g.getClip();

                // Ensure that our new clip resides within our old clip
                if (clip instanceof java.awt.Rectangle) {
                    g.setClip(newClip.intersection((java.awt.Rectangle) clip));
                }
                // Otherwise, default to original implementation
                else {
                    g.setClip(newClip);
                }
            }

            if (drawLabel) {
                String label = state.getLabel();

                if (label != null && state.getLabelBounds() != null) {
                    lab = canvas.drawLabel(label, state, isHtmlLabel(cell));
                }
            }

            // Restores the previous clipping region
            if (clippedCanvas instanceof Graphics2DCanvas) {
                ((Graphics2DCanvas) clippedCanvas).getGraphics().setClip(clip);
            }

            // Invokes the cellDrawn callback with the object which was created
            // by the canvas to represent the cell graphically
            if (obj != null) {
                cellDrawn(canvas, state, obj, lab);
            }
        }
    }

    /**
     * Called when a cell has been painted as the specified object, typically a
     * DOM node that represents the given cell graphically in a document.
     */
    protected void cellDrawn(ICanvas canvas, CellState state, Object element, Object labelElement) {
        if (element instanceof Element) {
            String link = getLinkForCell(state.getCell());

            if (link != null) {
                String title = getToolTipForCell(state.getCell());
                Element elem = (Element) element;

                if (elem.getNodeName().startsWith("v:")) {
                    elem.setAttribute("href", link);

                    if (title != null) {
                        elem.setAttribute("title", title);
                    }
                } else if (elem.getOwnerDocument().getElementsByTagName("svg").getLength() > 0) {
                    Element xlink = elem.getOwnerDocument().createElement("a");
                    xlink.setAttribute("xlink:href", link);

                    elem.getParentNode().replaceChild(xlink, elem);
                    xlink.appendChild(elem);

                    if (title != null) {
                        xlink.setAttribute("xlink:title", title);
                    }

                    elem = xlink;
                } else {
                    Element a = elem.getOwnerDocument().createElement("a");
                    a.setAttribute("href", link);
                    a.setAttribute("style", "text-decoration:none;");

                    elem.getParentNode().replaceChild(a, elem);
                    a.appendChild(elem);

                    if (title != null) {
                        a.setAttribute("title", title);
                    }

                    elem = a;
                }

                String target = getTargetForCell(state.getCell());

                if (target != null) {
                    elem.setAttribute("target", target);
                }
            }
        }
    }

    /**
     * Returns the hyperlink to be used for the given cell.
     */
    protected String getLinkForCell(ICell cell) {
        return null;
    }

    /**
     * Returns the hyperlink to be used for the given cell.
     */
    protected String getTargetForCell(ICell cell) {
        return null;
    }

    /**
     * @see java.beans.PropertyChangeSupport#addPropertyChangeListener(java.beans.PropertyChangeListener)
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    //
    // Redirected to change support
    //

    /**
     * @see java.beans.PropertyChangeSupport#addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
     */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * @see java.beans.PropertyChangeSupport#removePropertyChangeListener(java.beans.PropertyChangeListener)
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    /**
     * @see java.beans.PropertyChangeSupport#removePropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
     */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [model=" + model + ", view=" + view + "]";
    }

    public interface ICellVisitor {

        boolean visit(ICell vertex, ICell edge);

    }
}
