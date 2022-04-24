/**
 * Copyright (c) 2007-2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.event.CellStateEvent;
import com.faforever.neroxis.ngraph.event.DownEvent;
import com.faforever.neroxis.ngraph.event.EventSource;
import com.faforever.neroxis.ngraph.event.ScaleAndTranslateEvent;
import com.faforever.neroxis.ngraph.event.ScaleEvent;
import com.faforever.neroxis.ngraph.event.TranslateEvent;
import com.faforever.neroxis.ngraph.event.UndoEvent;
import com.faforever.neroxis.ngraph.event.UpEvent;
import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.model.UndoableChange;
import com.faforever.neroxis.ngraph.shape.ArrowShape;
import com.faforever.neroxis.ngraph.shape.LabelShape;
import com.faforever.neroxis.ngraph.style.Style;
import com.faforever.neroxis.ngraph.style.edge.EdgeStyleFunction;
import com.faforever.neroxis.ngraph.style.perimeter.Perimeter;
import com.faforever.neroxis.ngraph.style.util.HorizontalAlignment;
import com.faforever.neroxis.ngraph.style.util.Overflow;
import com.faforever.neroxis.ngraph.style.util.VerticalAlignment;
import com.faforever.neroxis.ngraph.style.util.WhiteSpace;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.util.UndoableEdit;
import com.faforever.neroxis.ngraph.util.Utils;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implements a view for the graph. This class is in charge of computing the
 * absolute coordinates for the relative child geometries, the points for
 * perimeters and edge styles and keeping them cached in cell states for faster
 * retrieval. The states are updated whenever the model or the view state
 * (translate, scale) changes. The scale and translate are honoured in the
 * bounds.
 * <p>
 * This class fires the following events:
 * <p>
 * Event.UNDO fires after the root was changed in setCurrentRoot. The
 * <code>edit</code> property contains the UndoableEdit which contains the
 * CurrentRootChange.
 * <p>
 * Event.SCALE_AND_TRANSLATE fires after the scale and transle have been
 * changed in scaleAndTranslate. The <code>scale</code>,
 * <code>previousScale</code>, <code>translate</code> and
 * <code>previousTranslate</code> properties contain the new and previous scale
 * and translate, respectively.
 * <p>
 * Event.SCALE fires after the scale was changed in setScale. The
 * <code>scale</code> and <code>previousScale</code> properties contain the new
 * and previous scale.
 * <p>
 * Event.TRANSLATE fires after the translate was changed in setTranslate. The
 * <code>translate</code> and <code>previousTranslate</code> properties contain
 * the new and previous value for translate.
 * <p>
 * Event.UP and Event.DOWN fire if the current root is changed by executing
 * a CurrentRootChange. The event name depends on the location of the root in
 * the cell hierarchy with respect to the current root. The <code>root</code>
 * and <code>previous</code> properties contain the new and previous root,
 * respectively.
 */
public class GraphView extends EventSource {

    private static final PointDouble EMPTY_POINT = new PointDouble();
    /**
     * Reference to the enclosing graph.
     */
    protected Graph graph;
    /**
     * Cell that acts as the root of the displayed cell hierarchy.
     */
    protected ICell currentRoot;
    /**
     * Caches the current bounds of the graph.
     */
    protected RectangleDouble graphBounds = new RectangleDouble();
    /**
     * Specifies the scale. Default is 1 (100%).
     */
    protected double scale = 1;
    /**
     * Point that specifies the current translation. Default is a new empty
     * point.
     */
    protected PointDouble translate = new PointDouble(0, 0);
    /**
     * Maps from cells to cell states.
     */
    protected HashMap<ICell, CellState> states = new HashMap<>();

    /**
     * Constructs a new view for the given graph.
     *
     * @param graph Reference to the enclosing graph.
     */
    public GraphView(Graph graph) {
        this.graph = graph;
    }

    /**
     * Returns the enclosing graph.
     *
     * @return Returns the enclosing graph.
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * Returns the dictionary that maps from cells to states.
     */
    public HashMap<ICell, CellState> getStates() {
        return states;
    }

    /**
     * Returns the dictionary that maps from cells to states.
     */
    public void setStates(HashMap<ICell, CellState> states) {
        this.states = states;
    }

    /**
     * Returns the cached diagram bounds.
     *
     * @return Returns the diagram bounds.
     */
    public RectangleDouble getGraphBounds() {
        return graphBounds;
    }

    /**
     * Sets the graph bounds.
     */
    public void setGraphBounds(RectangleDouble value) {
        graphBounds = value;
    }

    /**
     * Returns the current root.
     */
    public ICell getCurrentRoot() {
        return currentRoot;
    }

    /**
     * Sets and returns the current root and fires an undo event.
     *
     * @param root Cell that specifies the root of the displayed cell
     *             hierarchy.
     * @return Returns the object that represents the current root.
     */
    public ICell setCurrentRoot(ICell root) {
        if (currentRoot != root) {
            CurrentRootChange change = new CurrentRootChange(this, root);
            change.execute();
            UndoableEdit edit = new UndoableEdit(this, false);
            edit.add(change);
            fireEvent(new UndoEvent(edit));
        }

        return root;
    }

    /**
     * Sets the scale and translation. Fires a "scaleAndTranslate" event after
     * calling revalidate. Revalidate is only called if isEventsEnabled.
     *
     * @param scale Decimal value that specifies the new scale (1 is 100%).
     * @param dx    X-coordinate of the translation.
     * @param dy    Y-coordinate of the translation.
     */
    public void scaleAndTranslate(double scale, double dx, double dy) {
        double previousScale = this.scale;
        PointDouble previousTranslate = (PointDouble) translate.clone();

        if (scale != this.scale || dx != translate.getX() || dy != translate.getY()) {
            this.scale = scale;
            translate = new PointDouble(dx, dy);

            if (isEventsEnabled()) {
                revalidate();
            }
        }
        fireEvent(new ScaleAndTranslateEvent(translate, previousTranslate, scale, previousScale));
    }

    /**
     * Returns the current scale.
     *
     * @return Returns the scale.
     */
    public double getScale() {
        return scale;
    }

    /**
     * Sets the current scale and revalidates the view. Fires a "scale" event
     * after calling revalidate. Revalidate is only called if isEventsEnabled.
     *
     * @param value New scale to be used.
     */
    public void setScale(double value) {
        double previousScale = scale;

        if (scale != value) {
            scale = value;

            if (isEventsEnabled()) {
                revalidate();
            }
        }
        fireEvent(new ScaleEvent(scale, previousScale));
    }

    /**
     * Returns the current translation.
     *
     * @return Returns the translation.
     */
    public PointDouble getTranslate() {
        return translate;
    }

    /**
     * Sets the current translation and invalidates the view. Fires a property
     * change event for "translate" after calling revalidate. Revalidate is only
     * called if isEventsEnabled.
     *
     * @param value New translation to be used.
     */
    public void setTranslate(PointDouble value) {
        PointDouble previousTranslate = (PointDouble) translate.clone();
        if (value != null && (value.getX() != translate.getX() || value.getY() != translate.getY())) {
            translate = value;
            if (isEventsEnabled()) {
                revalidate();
            }
        }
        fireEvent(new TranslateEvent(translate, previousTranslate));
    }

    /**
     * Returns the bounding box for an array of cells or null, if no cells are
     * specified.
     *
     * @return Returns the bounding box for the given cells.
     */
    public RectangleDouble getBounds(List<ICell> cells) {
        return getBounds(cells, false);
    }

    /**
     * Returns the bounding box for an array of cells or null, if no cells are
     * specified.
     *
     * @return Returns the bounding box for the given cells.
     */
    public RectangleDouble getBounds(List<ICell> cells, boolean boundingBox) {
        RectangleDouble result = null;
        if (cells != null && !cells.isEmpty()) {
            IGraphModel model = graph.getModel();
            for (ICell cell : cells) {
                if (model.isVertex(cell) || model.isEdge(cell)) {
                    CellState state = getState(cell);
                    if (state != null) {
                        RectangleDouble tmp = (boundingBox) ? state.getBoundingBox() : state;

                        if (tmp != null) {
                            if (result == null) {
                                result = new RectangleDouble(tmp);
                            } else {
                                result.add(tmp);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns the bounding box for an array of cells or null, if no cells are
     * specified.
     *
     * @return Returns the bounding box for the given cells.
     */
    public RectangleDouble getBoundingBox(List<ICell> cells) {
        return getBounds(cells, true);
    }

    /**
     * First validates all bounds and then validates all points recursively on
     * all visible cells.
     */
    public void validate() {
        RectangleDouble graphBounds = getBoundingBox(
                validateCellState(validateCell((currentRoot != null) ? currentRoot : graph.getModel().getRoot())));
        setGraphBounds((graphBounds != null) ? graphBounds : new RectangleDouble());
    }

    /**
     * Recursively creates the cell state for the given cell if visible is true
     * and the given cell is visible. If the cell is not visible but the state
     * exists then it is removed using removeState.
     *
     * @param cell    Cell whose cell state should be created.
     * @param visible Boolean indicating if the cell should be visible.
     */
    public ICell validateCell(ICell cell, boolean visible) {
        if (cell != null) {
            visible = visible && graph.isCellVisible(cell);
            CellState state = getState(cell, visible);

            if (state != null && !visible) {
                removeState(cell);
            } else {
                IGraphModel model = graph.getModel();
                int childCount = model.getChildCount(cell);

                for (int i = 0; i < childCount; i++) {
                    validateCell(model.getChildAt(cell, i),
                                 visible && (!graph.isCellCollapsed(cell) || cell == currentRoot));
                }
            }
        }

        return cell;
    }

    /**
     * Updates the given cell state.
     *
     * @param state Cell state to be updated.
     */
    public void updateCellState(CellState state) {
        state.getAbsoluteOffset().setX(0);
        state.getAbsoluteOffset().setY(0);
        state.getOrigin().setX(0);
        state.getOrigin().setY(0);
        state.setLength(0);

        if (state.getCell() != currentRoot) {
            IGraphModel model = graph.getModel();
            CellState pState = getState(model.getParent(state.getCell()));

            if (pState != null && pState.getCell() != currentRoot) {
                state.getOrigin().setX(state.getOrigin().getX() + pState.getOrigin().getX());
                state.getOrigin().setY(state.getOrigin().getY() + pState.getOrigin().getY());
            }
            PointDouble offset = graph.getChildOffsetForCell(state.getCell());

            if (offset != null) {
                state.getOrigin().setX(state.getOrigin().getX() + offset.getX());
                state.getOrigin().setY(state.getOrigin().getY() + offset.getY());
            }

            Geometry geo = graph.getCellGeometry(state.getCell());

            if (geo != null) {
                if (!model.isEdge(state.getCell())) {
                    PointDouble origin = state.getOrigin();
                    offset = geo.getOffset();

                    if (offset == null) {
                        offset = EMPTY_POINT;
                    }

                    if (geo.isRelative() && pState != null) {
                        if (model.isEdge(pState.getCell())) {
                            PointDouble orig = getPoint(pState, geo);

                            if (orig != null) {
                                origin.setX(origin.getX() + (orig.getX() / scale)
                                            - pState.getOrigin().getX()
                                            - translate.getX());
                                origin.setY(origin.getY() + (orig.getY() / scale)
                                            - pState.getOrigin().getY()
                                            - translate.getY());
                            }
                        } else {
                            origin.setX(origin.getX() + geo.getX() * pState.getWidth() / scale + offset.getX());
                            origin.setY(origin.getY() + geo.getY() * pState.getHeight() / scale + offset.getY());
                        }
                    } else {
                        state.setAbsoluteOffset(new PointDouble(scale * offset.getX(), scale * offset.getY()));
                        origin.setX(origin.getX() + geo.getX());
                        origin.setY(origin.getY() + geo.getY());
                    }
                }

                state.setX(scale * (translate.getX() + state.getOrigin().getX()));
                state.setY(scale * (translate.getY() + state.getOrigin().getY()));
                state.setWidth(scale * geo.getWidth());
                state.setHeight(scale * geo.getHeight());

                if (model.isVertex(state.getCell())) {
                    updateVertexState(state, geo);
                }

                if (model.isEdge(state.getCell())) {
                    updateEdgeState(state, geo);
                }

                // Updates the cached label
                updateLabel(state);
            }
        }
    }

    /**
     * Removes all existing cell states and invokes validate.
     */
    public void reload() {
        states.clear();
        validate();
    }

    public void revalidate() {
        invalidate();
        validate();
    }

    /**
     * Invalidates all cell states.
     */
    public void invalidate() {
        invalidate(null);
    }

    /**
     * Removes the state of the given cell and all descendants if the given cell
     * is not the current root.
     */
    public void clear(ICell cell, boolean force, boolean recurse) {
        removeState(cell);

        if (recurse && (force || cell != currentRoot)) {
            IGraphModel model = graph.getModel();
            int childCount = model.getChildCount(cell);

            for (int i = 0; i < childCount; i++) {
                clear(model.getChildAt(cell, i), force, recurse);
            }
        } else {
            invalidate(cell);
        }
    }

    /**
     * Invalidates the state of the given cell, all its descendants and
     * connected edges.
     */
    public void invalidate(ICell cell) {
        IGraphModel model = graph.getModel();
        cell = (cell != null) ? cell : model.getRoot();
        CellState state = getState(cell);

        if (state == null || !state.isInvalid()) {
            if (state != null) {
                state.setInvalid(true);
            }

            // Recursively invalidates all descendants
            int childCount = model.getChildCount(cell);

            for (int i = 0; i < childCount; i++) {
                ICell child = model.getChildAt(cell, i);
                invalidate(child);
            }

            // Propagates invalidation to all connected edges
            int edgeCount = model.getEdgeCount(cell);

            for (int i = 0; i < edgeCount; i++) {
                invalidate(model.getEdgeAt(cell, i));
            }
        }
    }

    /**
     * Returns the state for the given cell or null if no state is defined for
     * the cell.
     *
     * @param cell Cell whose state should be returned.
     * @return Returns the state for the given cell.
     */
    public CellState getState(ICell cell) {
        return getState(cell, false);
    }

    /**
     * Shortcut to validateCell with visible set to true.
     */
    public RectangleDouble getBoundingBox(CellState state) {
        return getBoundingBox(state, true);
    }

    /**
     * Returns the bounding box of the shape and the label for the given cell
     * state and its children if recurse is true.
     *
     * @param state   Cell state whose bounding box should be returned.
     * @param recurse Boolean indicating if the children should be included.
     */
    public RectangleDouble getBoundingBox(CellState state, boolean recurse) {
        RectangleDouble bbox = null;
        if (state != null) {
            if (state.getBoundingBox() != null) {
                bbox = state.getBoundingBox().clone();
            }
            if (recurse) {
                IGraphModel model = graph.getModel();
                int childCount = model.getChildCount(state.getCell());

                for (int i = 0; i < childCount; i++) {
                    RectangleDouble bounds = getBoundingBox(getState(model.getChildAt(state.getCell(), i)), true);

                    if (bounds != null) {
                        if (bbox == null) {
                            bbox = bounds;
                        } else {
                            bbox.add(bounds);
                        }
                    }
                }
            }
        }

        return bbox;
    }

    /**
     * Shortcut to validateCell with visible set to true.
     */
    public ICell validateCell(ICell cell) {
        return validateCell(cell, true);
    }

    /**
     * Returns the cell state for the given cell. If create is true, then the
     * state is created if it does not yet exist.
     *
     * @param cell   Cell for which a new state should be returned.
     * @param create Boolean indicating if a new state should be created if it does
     *               not yet exist.
     * @return Returns the state for the given cell.
     */
    public CellState getState(ICell cell, boolean create) {
        CellState state;
        state = states.get(cell);
        if (state == null && create && graph.isCellVisible(cell)) {
            state = createState(cell);
            states.put(cell, state);
        }
        return state;
    }

    /**
     * Shortcut to validateCellState with recurse set to true.
     */
    public CellState validateCellState(ICell cell) {
        return validateCellState(cell, true);
    }

    /**
     * Validates the cell state for the given cell.
     *
     * @param cell    Cell whose cell state should be validated.
     * @param recurse Boolean indicating if the children of the cell should be
     *                validated.
     */
    public CellState validateCellState(ICell cell, boolean recurse) {
        CellState state = null;

        if (cell != null) {
            state = getState(cell);

            if (state != null) {
                IGraphModel model = graph.getModel();

                if (state.isInvalid()) {
                    state.setInvalid(false);

                    if (cell != currentRoot) {
                        validateCellState(model.getParent(cell), false);
                    }

                    state.setVisibleTerminalState(validateCellState(getVisibleTerminal(cell, true), false), true);
                    state.setVisibleTerminalState(validateCellState(getVisibleTerminal(cell, false), false), false);

                    updateCellState(state);

                    if (model.isEdge(cell) || model.isVertex(cell)) {
                        updateLabelBounds(state);
                        updateBoundingBox(state);
                    }
                }

                if (recurse) {
                    int childCount = model.getChildCount(cell);

                    for (int i = 0; i < childCount; i++) {
                        validateCellState(model.getChildAt(cell, i));
                    }
                }
            }
        }

        return state;
    }

    /**
     * Creates and returns a cell state for the given cell.
     *
     * @param cell Cell for which a new state should be created.
     * @return Returns a new state for the given cell.
     */
    public CellState createState(ICell cell) {
        CellState cellState = new CellState(this, cell, graph.getCellStyle(cell));
        cellState.getStyle().addPropertyChangeListener(evt -> fireEvent(new CellStateEvent(cellState)));
        return cellState;
    }

    /**
     * Validates the given cell state.
     */
    public void updateVertexState(CellState state, Geometry geo) {
        // LATER: Add support for rotation
        updateVertexLabelOffset(state);
    }

    /**
     * Validates the given cell state.
     */
    public void updateEdgeState(CellState state, Geometry geo) {
        CellState source = state.getVisibleTerminalState(true);
        CellState target = state.getVisibleTerminalState(false);

        // This will remove edges with no terminals and no terminal points
        // as such edges are invalid and produce NPEs in the edge styles.
        // Also removes connected edges that have no visible terminals.
        if ((graph.getModel().getTerminal(state.getCell(), true) != null && source == null) || (source == null
                                                                                                && geo.getTerminalPoint(
                true) == null) || (graph.getModel().getTerminal(state.getCell(), false) != null && target == null) || (
                    target == null
                    && geo.getTerminalPoint(false) == null)) {
            clear(state.getCell(), true, true);
        } else {
            updateFixedTerminalPoints(state, source, target);
            updatePoints(state, geo.getPoints(), source, target);
            updateFloatingTerminalPoints(state, source, target);

            if (state.getCell() != getCurrentRoot() && (state.getAbsolutePointCount() < 2
                                                        || state.getAbsolutePoint(0) == null
                                                        || state.getAbsolutePoint(state.getAbsolutePointCount() - 1)
                                                           == null)) {
                // This will remove edges with invalid points from the list of
                // states in the view.
                // Happens if the one of the terminals and the corresponding
                // terminal point is null.
                clear(state.getCell(), true, true);
            } else {
                updateEdgeBounds(state);
                state.setAbsoluteOffset(getPoint(state, geo));
            }
        }
    }

    /**
     * Updates the absoluteOffset of the given vertex cell state. This takes
     * into account the label position styles.
     *
     * @param state Cell state whose absolute offset should be updated.
     */
    public void updateVertexLabelOffset(CellState state) {
        HorizontalAlignment horizontalAlignment = state.getStyle().getLabel().getHorizontalAlignmentPosition();
        if (horizontalAlignment == HorizontalAlignment.LEFT) {
            state.absoluteOffset.setX(state.absoluteOffset.getX() - state.getWidth());
        } else if (horizontalAlignment == HorizontalAlignment.RIGHT) {
            state.absoluteOffset.setX(state.absoluteOffset.getX() + state.getWidth());
        }
        VerticalAlignment verticalAlignment = state.getStyle().getLabel().getVerticalAlignment();
        if (verticalAlignment == VerticalAlignment.TOP) {
            state.absoluteOffset.setY(state.absoluteOffset.getY() - state.getHeight());
        } else if (verticalAlignment == VerticalAlignment.BOTTOM) {
            state.absoluteOffset.setY(state.absoluteOffset.getY() + state.getHeight());
        }
    }

    /**
     * Updates the label of the given state.
     */
    public void updateLabel(CellState state) {
        String label = graph.getLabel(state.getCell());
        Style style = state.getStyle();
        // Applies word wrapping to labels and stores the result in the
        // state
        if (label != null
            && label.length() > 0
            && !graph.getModel().isEdge(state.getCell())
            && style.getLabel().getWhiteSpace() == WhiteSpace.WRAP) {
            double w = getWordWrapWidth(state);
            // The lines for wrapping within the given width are calculated for
            // no
            // scale. The reason for this is the granularity of actual displayed
            // font can cause the displayed lines to change based on scale. A
            // factor
            // is used to allow for different overalls widths, it ensures the
            // largest
            // font size/scale factor still stays within the bounds. All this
            // ensures
            // the wrapped lines are constant overing scaling, at the expense
            // the
            // label bounds will vary.
            String[] lines = Utils.wordWrap(label, Utils.getFontMetrics(Utils.getFont(state.getStyle())),
                                            w * Constants.LABEL_SCALE_BUFFER);

            if (lines.length > 0) {
                StringBuilder buffer = new StringBuilder();

                for (String line : lines) {
                    buffer.append(line).append('\n');
                }

                label = buffer.substring(0, buffer.length() - 1);
            }
        }

        state.setLabel(label);
    }

    /**
     * Returns the width for wrapping the label of the given state at scale 1.
     */
    public double getWordWrapWidth(CellState state) {
        Style style = state.getStyle();
        boolean horizontal = style.getCellProperties().isHorizontal();
        double w = 0;
        // Computes the available width for the wrapped label
        if (horizontal) {
            w = (state.getWidth() / scale)
                - 2 * Constants.LABEL_INSET
                - style.getLabel().getLeftSpacing()
                - style.getLabel().getRightSpacing();
        } else {
            w = (state.getHeight() / scale) - 2 * Constants.LABEL_INSET - style.getLabel().getTopSpacing()
                + style.getLabel().getBottomSpacing();
        }
        return w;
    }

    /**
     * Returns the absolute point on the edge for the given relative geometry as
     * a point. The edge is represented by the given cell state.
     *
     * @param state    Represents the state of the parent edge.
     * @param geometry Optional geometry that represents the relative location.
     * @return Returns the point that represents the absolute location of the
     * given relative geometry.
     */
    public PointDouble getPoint(CellState state, Geometry geometry) {
        double x = state.getCenterX();
        double y = state.getCenterY();
        if (state.getSegments() != null && (geometry == null || geometry.isRelative())) {
            double gx = (geometry != null) ? geometry.getX() / 2 : 0;
            int pointCount = state.getAbsolutePointCount();
            double dist = (gx + 0.5) * state.getLength();
            double[] segments = state.getSegments();
            double segment = segments[0];
            double length = 0;
            int index = 1;
            while (dist > length + segment && index < pointCount - 1) {
                length += segment;
                segment = segments[index++];
            }
            double factor = (segment == 0) ? 0 : (dist - length) / segment;
            PointDouble p0 = state.getAbsolutePoint(index - 1);
            PointDouble pe = state.getAbsolutePoint(index);
            if (p0 != null && pe != null) {
                double gy = 0;
                double offsetX = 0;
                double offsetY = 0;
                if (geometry != null) {
                    gy = geometry.getY();
                    PointDouble offset = geometry.getOffset();

                    if (offset != null) {
                        offsetX = offset.getX();
                        offsetY = offset.getY();
                    }
                }

                double dx = pe.getX() - p0.getX();
                double dy = pe.getY() - p0.getY();
                double nx = (segment == 0) ? 0 : dy / segment;
                double ny = (segment == 0) ? 0 : dx / segment;

                x = p0.getX() + dx * factor + (nx * gy + offsetX) * scale;
                y = p0.getY() + dy * factor - (ny * gy - offsetY) * scale;
            }
        } else if (geometry != null) {
            PointDouble offset = geometry.getOffset();

            if (offset != null) {
                x += offset.getX();
                y += offset.getY();
            }
        }
        return new PointDouble(x, y);
    }

    /**
     * Updates the bounding box in the given cell state.
     *
     * @param state Cell state whose bounding box should be updated.
     */
    public RectangleDouble updateBoundingBox(CellState state) {
        // Gets the cell bounds and adds shadows and markers
        RectangleDouble rect = new RectangleDouble(state);
        Style style = state.getStyle();
        // Adds extra pixels for the marker and stroke assuming
        // that the border stroke is centered around the bounds
        // and the first pixel is drawn inside the bounds
        double strokeWidth = Math.max(1, Math.round(style.getShape().getStrokeWidth() * scale));
        strokeWidth -= Math.max(1, strokeWidth / 2);

        if (graph.getModel().isEdge(state.getCell())) {
            int ms = 0;
            if (style.getEdge().getStartArrow() != null || style.getEdge().getEndArrow() != null) {
                ms = (int) Math.round(style.getEdge().getEndSize() * scale);
            }
            // Adds the strokewidth
            rect.grow(ms + strokeWidth);
            // Adds worst case border for an arrow shape
            if (style.getShape().getShape() instanceof ArrowShape) {
                rect.grow(Constants.ARROW_WIDTH / 2d);
            }
        } else {
            rect.grow(strokeWidth);
        }
        // Adds extra pixels for the shadow
        if (style.getCellProperties().isShadow()) {
            rect.setWidth(rect.getWidth() + Constants.SHADOW_OFFSETX);
            rect.setHeight(rect.getHeight() + Constants.SHADOW_OFFSETY);
        }
        // Adds oversize images in labels
        if (style.getShape().getShape() instanceof LabelShape) {
            if (style.getImage().getImage() != null) {
                double w = style.getImage().getWidth() * scale;
                double h = style.getImage().getHeight() * scale;
                double x = state.getX();
                double y = 0;
                HorizontalAlignment imgAlign = style.getImage().getHorizontalAlignment();
                VerticalAlignment imgValign = style.getImage().getVerticalAlignment();
                if (imgAlign == HorizontalAlignment.RIGHT) {
                    x += state.getWidth() - w;
                } else if (imgAlign == HorizontalAlignment.CENTER) {
                    x += (state.getWidth() - w) / 2;
                }
                if (imgValign == VerticalAlignment.TOP) {
                    y = state.getY();
                } else if (imgValign == VerticalAlignment.BOTTOM) {
                    y = state.getY() + state.getHeight() - h;
                } else
                // MIDDLE
                {
                    y = state.getY() + (state.getHeight() - h) / 2;
                }
                rect.add(new RectangleDouble(x, y, w, h));
            }
        }

        // Adds the rotated bounds to the bounding box if the
        // shape is rotated
        double rotation = style.getShape().getRotation();
        RectangleDouble bbox = Utils.getBoundingBox(rect, rotation);

        // Add the rotated bounding box to the non-rotated so
        // that all handles are also covered
        rect.add(bbox);

        // Unifies the cell bounds and the label bounds
        rect.add(state.getLabelBounds());

        state.setBoundingBox(rect);

        return rect;
    }

    /**
     * Sets the initial absolute terminal points in the given state before the
     * edge style is computed.
     *
     * @param edge   Cell state whose initial terminal points should be updated.
     * @param source Cell state which represents the source terminal.
     * @param target Cell state which represents the target terminal.
     */
    public void updateFixedTerminalPoints(CellState edge, CellState source, CellState target) {
        updateFixedTerminalPoint(edge, source, true, graph.getConnectionConstraint(edge, source, true));
        updateFixedTerminalPoint(edge, target, false, graph.getConnectionConstraint(edge, target, false));
    }

    /**
     * Updates the label bounds in the given state.
     */
    public void updateLabelBounds(CellState state) {
        ICell cell = state.getCell();
        Style style = state.getStyle();
        Overflow overflow = style.getLabel().getOverflow();
        if (overflow == Overflow.FILL) {
            state.setLabelBounds(new RectangleDouble(state));
        } else if (state.getLabel() != null) {
            // For edges, the width of the geometry is used for wrapping HTML
            // labels or no wrapping is applied if the width is set to 0
            RectangleDouble vertexBounds = state;
            if (graph.getModel().isEdge(cell)) {
                Geometry geo = graph.getCellGeometry(cell);
                if (geo != null && geo.getWidth() > 0) {
                    vertexBounds = new RectangleDouble(0, 0, geo.getWidth() * this.getScale(), 0);
                } else {
                    vertexBounds = null;
                }
            }
            state.setLabelBounds(
                    Utils.getLabelPaintBounds(state.getLabel(), style, state.getAbsoluteOffset(), vertexBounds, scale,
                                              graph.getModel().isEdge(cell)));

            if (overflow.equals("width")) {
                state.getLabelBounds().setX(state.getX());
                state.getLabelBounds().setWidth(state.getWidth());
            }
        }
    }

    /**
     * Updates the absolute points in the given state using the specified array
     * of points as the relative points.
     *
     * @param edge   Cell state whose absolute points should be updated.
     * @param points Array of points that constitute the relative points.
     * @param source Cell state that represents the source terminal.
     * @param target Cell state that represents the target terminal.
     */
    public void updatePoints(CellState edge, List<PointDouble> points, CellState source, CellState target) {
        if (edge != null) {
            List<PointDouble> pts = new ArrayList<>();
            pts.add(edge.getAbsolutePoint(0));
            EdgeStyleFunction edgeStyleFunction = getEdgeStyle(edge, points, source, target);
            if (edgeStyleFunction != null) {
                CellState src = getTerminalPort(edge, source, true);
                CellState trg = getTerminalPort(edge, target, false);
                edgeStyleFunction.apply(edge, src, trg, points, pts);
            } else if (points != null) {
                for (PointDouble point : points) {
                    pts.add(transformControlPoint(edge, point));
                }
            }
            pts.add(edge.getAbsolutePoint(edge.getAbsolutePointCount() - 1));
            edge.setAbsolutePoints(pts);
        }
    }

    /**
     * Sets the fixed source or target terminal point on the given edge.
     *
     * @param edge Cell state whose initial terminal points should be updated.
     */
    public void updateFixedTerminalPoint(CellState edge, CellState terminal, boolean source,
                                         ConnectionConstraint constraint) {
        PointDouble pt = null;

        if (constraint != null) {
            pt = graph.getConnectionPoint(terminal, constraint);
        }

        if (pt == null && terminal == null) {
            PointDouble orig = edge.getOrigin();
            Geometry geo = graph.getCellGeometry(edge.getCell());
            pt = geo.getTerminalPoint(source);

            if (pt != null) {
                pt = new PointDouble(scale * (translate.getX() + pt.getX() + orig.getX()),
                                     scale * (translate.getY() + pt.getY() + orig.getY()));
            }
        }

        edge.setAbsoluteTerminalPoint(pt, source);
    }

    /**
     * Returns the edge style function to be used to compute the absolute points
     * for the given state, control points and terminals.
     */
    public EdgeStyleFunction getEdgeStyle(CellState edge, List<PointDouble> points, Object source, Object target) {
        EdgeStyleFunction edgeStyle = null;
        if (source != null && source == target) {
            edgeStyle = edge.getStyle().getEdge().getLoopStyleFunction();
            if (edgeStyle == null) {
                edgeStyle = graph.getDefaultLoopStyle();
            }
        } else if (!edge.getStyle().getEdge().isNoEdgeStyle()) {
            edgeStyle = edge.getStyle().getEdge().getEdgeStyleFunction();
        }
        return edgeStyle;
    }

    /**
     * Updates the terminal points in the given state after the edge style was
     * computed for the edge.
     *
     * @param state  Cell state whose terminal points should be updated.
     * @param source Cell state that represents the source terminal.
     * @param target Cell state that represents the target terminal.
     */
    public void updateFloatingTerminalPoints(CellState state, CellState source, CellState target) {
        PointDouble p0 = state.getAbsolutePoint(0);
        PointDouble pe = state.getAbsolutePoint(state.getAbsolutePointCount() - 1);
        if (pe == null && target != null) {
            updateFloatingTerminalPoint(state, target, source, false);
        }
        if (p0 == null && source != null) {
            updateFloatingTerminalPoint(state, source, target, true);
        }
    }

    /**
     * Transforms the given control point to an absolute point.
     */
    public PointDouble transformControlPoint(CellState state, PointDouble pt) {
        PointDouble origin = state.getOrigin();
        return new PointDouble(scale * (pt.getX() + translate.getX() + origin.getX()),
                               scale * (pt.getY() + translate.getY() + origin.getY()));
    }

    /**
     * Updates the absolute terminal point in the given state for the given
     * start and end state, where start is the source if source is true.
     *
     * @param edge   Cell state whose terminal point should be updated.
     * @param start  Cell state for the terminal on "this" side of the edge.
     * @param end    Cell state for the terminal on the other side of the edge.
     * @param source Boolean indicating if start is the source terminal state.
     */
    public void updateFloatingTerminalPoint(CellState edge, CellState start, CellState end, boolean source) {
        start = getTerminalPort(edge, start, source);
        PointDouble next = getNextPoint(edge, end, source);
        double border = edge.getStyle().getPerimeter().getVertexSpacing();
        border += source ? edge.getStyle().getPerimeter().getSourceSpacing() : edge.getStyle()
                                                                                   .getPerimeter()
                                                                                   .getTargetSpacing();
        PointDouble pt = getPerimeterPoint(start, next, graph.isOrthogonal(edge), border);
        edge.setAbsoluteTerminalPoint(pt, source);
    }

    /**
     * Returns a point that defines the location of the intersection point
     * between the perimeter and the line between the center of the shape and
     * the given point.
     */
    public PointDouble getPerimeterPoint(CellState terminal, PointDouble next, boolean orthogonal) {
        return getPerimeterPoint(terminal, next, orthogonal, 0);
    }

    /**
     * Returns a point that defines the location of the intersection point
     * between the perimeter and the line between the center of the shape and
     * the given point.
     *
     * @param terminal   Cell state for the source or target terminal.
     * @param next       Point that lies outside of the given terminal.
     * @param orthogonal Boolean that specifies if the orthogonal projection onto the
     *                   perimeter should be returned. If this is false then the
     *                   intersection of the perimeter and the line between the next
     *                   and the center point is returned.
     * @param border     Optional border between the perimeter and the shape.
     */
    public PointDouble getPerimeterPoint(CellState terminal, PointDouble next, boolean orthogonal, double border) {
        PointDouble point = null;
        if (terminal != null) {
            Perimeter perimeter = getPerimeterFunction(terminal);
            if (perimeter != null && next != null) {
                RectangleDouble bounds = getPerimeterBounds(terminal, border);
                if (bounds.getWidth() > 0 || bounds.getHeight() > 0) {
                    point = perimeter.apply(bounds, terminal, next, orthogonal);
                }
            }

            if (point == null) {
                point = getPoint(terminal);
            }
        }

        return point;
    }

    /**
     * Returns a cell state that represents the source or target terminal or
     * port for the given edge.
     */
    public CellState getTerminalPort(CellState state, CellState terminal, boolean source) {
        ICell portCell = source ? state.getStyle().getEdge().getSourcePort() : state.getStyle()
                                                                                    .getEdge()
                                                                                    .getTargetPort();
        if (portCell == null) {
            return terminal;
        }
        return getState(portCell);
    }

    /**
     * Returns the perimeter function for the given state.
     */
    public Perimeter getPerimeterFunction(CellState state) {
        return state.getStyle().getPerimeter().getPerimeter();
    }

    /**
     * Returns the perimeter bounds for the given terminal, edge pair.
     */
    public RectangleDouble getPerimeterBounds(CellState terminal, double border) {
        if (terminal != null) {
            border += terminal.getStyle().getPerimeter().getVertexSpacing();
        }
        return terminal.getPerimeterBounds(border * scale);
    }

    /**
     * Returns the absolute center point along the given edge.
     */
    public PointDouble getPoint(CellState state) {
        return getPoint(state, null);
    }

    /**
     * Returns the x-coordinate of the center point for automatic routing.
     *
     * @return Returns the x-coordinate of the routing center point.
     */
    public double getRoutingCenterX(CellState state) {
        float f = (state.getStyle() != null) ? state.getStyle().getEdge().getRoutingCenterX() : 0;

        return state.getCenterX() + f * state.getWidth();
    }

    /**
     * Returns the y-coordinate of the center point for automatic routing.
     *
     * @return Returns the y-coordinate of the routing center point.
     */
    public double getRoutingCenterY(CellState state) {
        float f = (state.getStyle() != null) ? state.getStyle().getEdge().getRoutingCenterY() : 0;

        return state.getCenterY() + f * state.getHeight();
    }

    /**
     * Returns the nearest point in the list of absolute points or the center of
     * the opposite terminal.
     *
     * @param edge     Cell state that represents the edge.
     * @param opposite Cell state that represents the opposite terminal.
     * @param source   Boolean indicating if the next point for the source or target
     *                 should be returned.
     * @return Returns the nearest point of the opposite side.
     */
    public PointDouble getNextPoint(CellState edge, CellState opposite, boolean source) {
        List<PointDouble> pts = edge.getAbsolutePoints();
        PointDouble point = null;
        if (pts != null && pts.size() >= 2) {
            int count = pts.size();
            int index = (source) ? Math.min(1, count - 1) : Math.max(0, count - 2);
            point = pts.get(index);
        }
        if (point == null && opposite != null) {
            point = new PointDouble(opposite.getCenterX(), opposite.getCenterY());
        }

        return point;
    }

    /**
     * Returns the nearest ancestor terminal that is visible. The edge appears
     * to be connected to this terminal on the display.
     *
     * @param edge   Cell whose visible terminal should be returned.
     * @param source Boolean that specifies if the source or target terminal should
     *               be returned.
     * @return Returns the visible source or target terminal.
     */
    public ICell getVisibleTerminal(ICell edge, boolean source) {
        IGraphModel model = graph.getModel();
        ICell result = model.getTerminal(edge, source);
        ICell best = result;

        while (result != null && result != currentRoot) {
            if (!graph.isCellVisible(best) || graph.isCellCollapsed(result)) {
                best = result;
            }

            result = model.getParent(result);
        }

        // Checks if the result is not a layer
        if (model.getParent(best) == model.getRoot()) {
            best = null;
        }

        return best;
    }

    /**
     * Updates the given state using the bounding box of the absolute points.
     * Also updates terminal distance, length and segments.
     *
     * @param state Cell state whose bounds should be updated.
     */
    public void updateEdgeBounds(CellState state) {
        List<PointDouble> points = state.getAbsolutePoints();
        PointDouble p0 = points.get(0);
        PointDouble pe = points.get(points.size() - 1);
        if (p0.getX() != pe.getX() || p0.getY() != pe.getY()) {
            double dx = pe.getX() - p0.getX();
            double dy = pe.getY() - p0.getY();
            state.setTerminalDistance(Math.sqrt(dx * dx + dy * dy));
        } else {
            state.setTerminalDistance(0);
        }
        double length = 0;
        double[] segments = new double[points.size() - 1];
        PointDouble pt = p0;

        double minX = pt.getX();
        double minY = pt.getY();
        double maxX = minX;
        double maxY = minY;

        for (int i = 1; i < points.size(); i++) {
            PointDouble tmp = points.get(i);

            if (tmp != null) {
                double dx = pt.getX() - tmp.getX();
                double dy = pt.getY() - tmp.getY();

                double segment = Math.sqrt(dx * dx + dy * dy);
                segments[i - 1] = segment;
                length += segment;
                pt = tmp;

                minX = Math.min(pt.getX(), minX);
                minY = Math.min(pt.getY(), minY);
                maxX = Math.max(pt.getX(), maxX);
                maxY = Math.max(pt.getY(), maxY);
            }
        }

        state.setLength(length);
        state.setSegments(segments);
        double markerSize = 1; // TODO: include marker size

        state.setX(minX);
        state.setY(minY);
        state.setWidth(Math.max(markerSize, maxX - minX));
        state.setHeight(Math.max(markerSize, maxY - minY));
    }

    /**
     * Gets the relative point that describes the given, absolute label position
     * for the given edge state.
     */
    public PointDouble getRelativePoint(CellState edgeState, double x, double y) {
        IGraphModel model = graph.getModel();
        Geometry geometry = model.getGeometry(edgeState.getCell());
        if (geometry != null) {
            int pointCount = edgeState.getAbsolutePointCount();
            if (geometry.isRelative() && pointCount > 1) {
                double totalLength = edgeState.getLength();
                double[] segments = edgeState.getSegments();
                // Works which line segment the point of the label is closest to
                PointDouble p0 = edgeState.getAbsolutePoint(0);
                PointDouble pe = edgeState.getAbsolutePoint(1);
                Line2D line = new Line2D.Double(p0.toPoint(), pe.toPoint());
                double minDist = line.ptSegDistSq(x, y);
                int index = 0;
                double tmp = 0;
                double length = 0;
                for (int i = 2; i < pointCount; i++) {
                    tmp += segments[i - 2];
                    pe = edgeState.getAbsolutePoint(i);
                    line = new Line2D.Double(p0.toPoint(), pe.toPoint());
                    double dist = line.ptSegDistSq(x, y);

                    if (dist < minDist) {
                        minDist = dist;
                        index = i - 1;
                        length = tmp;
                    }

                    p0 = pe;
                }

                double seg = segments[index];
                p0 = edgeState.getAbsolutePoint(index);
                pe = edgeState.getAbsolutePoint(index + 1);

                double x2 = p0.getX();
                double y2 = p0.getY();

                double x1 = pe.getX();
                double y1 = pe.getY();

                double px = x;
                double py = y;

                double xSegment = x2 - x1;
                double ySegment = y2 - y1;

                px -= x1;
                py -= y1;
                double projlenSq = 0;

                px = xSegment - px;
                py = ySegment - py;
                double dotprod = px * xSegment + py * ySegment;

                if (dotprod <= 0.0) {
                    projlenSq = 0;
                } else {
                    projlenSq = dotprod * dotprod / (xSegment * xSegment + ySegment * ySegment);
                }

                double projlen = Math.sqrt(projlenSq);

                if (projlen > seg) {
                    projlen = seg;
                }

                double yDistance = Line2D.ptLineDist(p0.getX(), p0.getY(), pe.getX(), pe.getY(), x, y);
                int direction = Line2D.relativeCCW(p0.getX(), p0.getY(), pe.getX(), pe.getY(), x, y);

                if (direction == -1) {
                    yDistance = -yDistance;
                }

                // Constructs the relative point for the label
                return new PointDouble(Math.round(((totalLength / 2 - length - projlen) / totalLength) * -2),
                                       Math.round(yDistance / scale));
            }
        }
        return new PointDouble();
    }

    /**
     * Returns the states for the given array of cells. The array contains all
     * states that are not null, that is, the returned array may have less
     * elements than the given array.
     */
    public List<CellState> getCellStates(List<ICell> cells) {
        return cells.stream().map(this::getState).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Removes and returns the CellState for the given cell.
     *
     * @param cell Cell for which the CellState should be removed.
     * @return Returns the CellState that has been removed.
     */
    public CellState removeState(ICell cell) {
        return states.remove(cell);
    }

    public void refreshStyle(ICell cell) {
        CellState state = getState(cell);
        if (state != null) {
            state.setStyle(graph.getCellStyle(cell));
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
               + " [currentRoot="
               + currentRoot
               + ", graphBounds="
               + graphBounds
               + ", scale="
               + scale
               + ", translate="
               + translate
               + "]";
    }

    /**
     * Action to change the current root in a view.
     */
    public static class CurrentRootChange implements UndoableChange {

        protected GraphView view;
        protected ICell root, previous;
        protected boolean up;

        /**
         * Constructs a change of the current root in the given view.
         */
        public CurrentRootChange(GraphView view, ICell root) {
            this.view = view;
            this.root = root;
            this.previous = this.root;
            this.up = (root == null);

            if (!up) {
                ICell tmp = view.getCurrentRoot();
                IGraphModel model = view.graph.getModel();

                while (tmp != null) {
                    if (tmp == root) {
                        up = true;
                        break;
                    }

                    tmp = model.getParent(tmp);
                }
            }
        }

        /**
         * Returns the graph view where the change happened.
         */
        public GraphView getView() {
            return view;
        }

        /**
         * Returns the root.
         */
        public Object getRoot() {
            return root;
        }

        /**
         * Returns the previous root.
         */
        public Object getPrevious() {
            return previous;
        }

        /**
         * Returns true if the drilling went upwards.
         */
        public boolean isUp() {
            return up;
        }

        /**
         * Changes the current root of the view.
         */
        @Override
        public void execute() {
            ICell tmp = view.getCurrentRoot();
            view.currentRoot = previous;
            previous = tmp;
            PointDouble translate = view.graph.getTranslateForRoot(view.getCurrentRoot());
            if (translate != null) {
                view.translate = new PointDouble(-translate.getX(), translate.getY());
            }
            // Removes all existing cell states and revalidates
            view.reload();
            up = !up;
            if (up) {
                view.fireEvent(new UpEvent(view.currentRoot, previous));
            } else {
                view.fireEvent(new DownEvent(view.currentRoot, previous));
            }
        }
    }
}
