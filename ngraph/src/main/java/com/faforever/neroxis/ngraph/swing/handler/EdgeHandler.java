/**
 * Copyright (c) 2008-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.swing.handler;

import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.swing.util.SwingConstants;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Point;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.ConnectionConstraint;
import com.faforever.neroxis.ngraph.view.Graph;
import com.faforever.neroxis.ngraph.view.GraphView;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 */
public class EdgeHandler extends CellHandler {
    protected boolean cloneEnabled = true;

    protected java.awt.Point[] p;

    protected transient String error;

    /**
     * Workaround for alt-key-state not correct in mouseReleased.
     */
    protected transient boolean gridEnabledEvent = false;

    /**
     * Workaround for shift-key-state not correct in mouseReleased.
     */
    protected transient boolean constrainedEvent = false;

    protected CellMarker marker = new CellMarker(graphComponent) {

        private static final long serialVersionUID = 8826073441093831764L;

        // Only returns edges if they are connectable and never returns
        // the edge that is currently being modified
        protected ICell getCell(MouseEvent e) {
            Graph graph = graphComponent.getGraph();
            IGraphModel model = graph.getModel();
            ICell cell = super.getCell(e);

            if (cell == EdgeHandler.this.state.getCell() || (!graph.isConnectableEdges() && model.isEdge(cell))) {
                cell = null;
            }

            return cell;
        }

        // Sets the highlight color according to isValidConnection
        protected boolean isValidState(CellState state) {
            GraphView view = graphComponent.getGraph().getView();
            IGraphModel model = graphComponent.getGraph().getModel();
            ICell edge = EdgeHandler.this.state.getCell();
            boolean isSource = isSource(index);

            CellState other = view.getTerminalPort(state, view.getState(model.getTerminal(edge, !isSource)), !isSource);
            ICell otherCell = (other != null) ? other.getCell() : null;
            ICell source = (isSource) ? state.getCell() : otherCell;
            ICell target = (isSource) ? otherCell : state.getCell();

            error = validateConnection(source, target);

            return error == null;
        }

    };

    public EdgeHandler(GraphComponent graphComponent, CellState state) {
        super(graphComponent, state);
    }

    public boolean isCloneEnabled() {
        return cloneEnabled;
    }

    public void setCloneEnabled(boolean cloneEnabled) {
        this.cloneEnabled = cloneEnabled;
    }

    /**
     * No flip event is ignored.
     */
    protected boolean isIgnoredEvent(MouseEvent e) {
        return !isFlipEvent(e) && super.isIgnoredEvent(e);
    }

    protected boolean isFlipEvent(MouseEvent e) {
        return false;
    }

    /**
     * Returns the error message or an empty string if the connection for the
     * given source target pair is not valid. Otherwise it returns null.
     */
    public String validateConnection(ICell source, ICell target) {
        return graphComponent.getGraph().getEdgeValidationError(state.getCell(), source, target);
    }

    /**
     * Returns true if the current index is 0.
     */
    public boolean isSource(int index) {
        return index == 0;
    }

    /**
     * Returns true if the current index is the last index.
     */
    public boolean isTarget(int index) {
        return index == getHandleCount() - 2;
    }

    /**
     * Hides the middle handle if the edge is not bendable.
     */
    protected boolean isHandleVisible(int index) {
        return super.isHandleVisible(index) && (isSource(index) || isTarget(index) || isCellBendable());
    }

    protected boolean isCellBendable() {
        return graphComponent.getGraph().isCellBendable(state.getCell());
    }

    protected Rectangle[] createHandles() {
        p = createPoints(state);
        Rectangle[] h = new Rectangle[p.length + 1];

        for (int i = 0; i < h.length - 1; i++) {
            h[i] = createHandle(p[i]);
        }

        h[p.length] = createHandle(state.getAbsoluteOffset().getPoint(), Constants.LABEL_HANDLE_SIZE);

        return h;
    }

    protected Color getHandleFillColor(int index) {
        boolean source = isSource(index);

        if (source || isTarget(index)) {
            Graph graph = graphComponent.getGraph();
            ICell terminal = graph.getModel().getTerminal(state.getCell(), source);

            if (terminal == null && !graphComponent.getGraph().isTerminalPointMovable(state.getCell(), source)) {
                return SwingConstants.LOCKED_HANDLE_FILLCOLOR;
            } else if (terminal != null) {
                return (graphComponent.getGraph().isCellDisconnectable(state.getCell(), terminal, source)) ? SwingConstants.CONNECT_HANDLE_FILLCOLOR : SwingConstants.LOCKED_HANDLE_FILLCOLOR;
            }
        }

        return super.getHandleFillColor(index);
    }

    /**
     * @param x
     * @param y
     * @return Returns the inde of the handle at the given location.
     */
    public int getIndexAt(int x, int y) {
        int index = super.getIndexAt(x, y);

        // Makes the complete label a trigger for the label handle
        if (index < 0 && handles != null && handlesVisible && isLabelMovable() && state.getLabelBounds().getRectangle().contains(x, y)) {
            index = handles.length - 1;
        }

        return index;
    }

    protected Rectangle createHandle(java.awt.Point center) {
        return createHandle(center, Constants.HANDLE_SIZE);
    }

    protected Rectangle createHandle(java.awt.Point center, int size) {
        return new Rectangle(center.x - size / 2, center.y - size / 2, size, size);
    }

    protected java.awt.Point[] createPoints(CellState s) {
        java.awt.Point[] pts = new java.awt.Point[s.getAbsolutePointCount()];

        for (int i = 0; i < pts.length; i++) {
            pts[i] = s.getAbsolutePoint(i).getPoint();
        }

        return pts;
    }

    protected JComponent createPreview() {
        JPanel preview = new JPanel() {
            /**
             *
             */
            private static final long serialVersionUID = -894546588972313020L;

            public void paint(Graphics g) {
                super.paint(g);

                if (!isLabel(index) && p != null) {
                    ((Graphics2D) g).setStroke(SwingConstants.PREVIEW_STROKE);

                    if (isSource(index) || isTarget(index)) {
                        if (marker.hasValidState() || graphComponent.getGraph().isAllowDanglingEdges()) {
                            g.setColor(SwingConstants.DEFAULT_VALID_COLOR);
                        } else {
                            g.setColor(SwingConstants.DEFAULT_INVALID_COLOR);
                        }
                    } else {
                        g.setColor(Color.BLACK);
                    }

                    java.awt.Point origin = getLocation();
                    java.awt.Point last = p[0];

                    for (int i = 1; i < p.length; i++) {
                        g.drawLine(last.x - origin.x, last.y - origin.y, p[i].x - origin.x, p[i].y - origin.y);
                        last = p[i];
                    }
                }
            }
        };

        if (isLabel(index)) {
            preview.setBorder(SwingConstants.PREVIEW_BORDER);
        }

        preview.setOpaque(false);
        preview.setVisible(false);

        return preview;
    }

    /**
     * @param point
     * @param gridEnabled
     * @return Returns the scaled, translated and grid-aligned point.
     */
    protected Point convertPoint(Point point, boolean gridEnabled) {
        Graph graph = graphComponent.getGraph();
        double scale = graph.getView().getScale();
        Point trans = graph.getView().getTranslate();
        double x = point.getX() / scale - trans.getX();
        double y = point.getY() / scale - trans.getY();

        if (gridEnabled) {
            x = graph.snap(x);
            y = graph.snap(y);
        }

        point.setX(x - state.getOrigin().getX());
        point.setY(y - state.getOrigin().getY());

        return point;
    }

    /**
     * @return Returns the bounds of the preview.
     */
    protected Rectangle getPreviewBounds() {
        Rectangle bounds = null;

        if (isLabel(index)) {
            bounds = state.getLabelBounds().getRectangle();
        } else {
            bounds = new Rectangle(p[0]);

            for (int i = 0; i < p.length; i++) {
                bounds.add(p[i]);
            }

            bounds.height += 1;
            bounds.width += 1;
        }

        return bounds;
    }

    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);

        boolean source = isSource(index);

        if (source || isTarget(index)) {
            Graph graph = graphComponent.getGraph();
            IGraphModel model = graph.getModel();
            ICell terminal = model.getTerminal(state.getCell(), source);

            if ((terminal == null && !graph.isTerminalPointMovable(state.getCell(), source)) || (terminal != null && !graph.isCellDisconnectable(state.getCell(), terminal, source))) {
                first = null;
            }
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (!e.isConsumed() && first != null) {
            gridEnabledEvent = graphComponent.isGridEnabledEvent(e);
            constrainedEvent = graphComponent.isConstrainedEvent(e);

            boolean isSource = isSource(index);
            boolean isTarget = isTarget(index);

            ICell source = null;
            ICell target = null;

            if (isLabel(index)) {
                Point abs = state.getAbsoluteOffset();
                double dx = abs.getX() - first.x;
                double dy = abs.getY() - first.y;

                Point pt = new Point(e.getPoint());

                if (gridEnabledEvent) {
                    pt = graphComponent.snapScaledPoint(pt, dx, dy);
                }

                if (constrainedEvent) {
                    if (Math.abs(e.getX() - first.x) > Math.abs(e.getY() - first.y)) {
                        pt.setY(abs.getY());
                    } else {
                        pt.setX(abs.getX());
                    }
                }

                Rectangle rect = getPreviewBounds();
                rect.translate((int) Math.round(pt.getX() - first.x), (int) Math.round(pt.getY() - first.y));
                preview.setBounds(rect);
            } else {
                // Clones the cell state and updates the absolute points using
                // the current state of this handle. This is required for
                // computing the correct perimeter points and edge style.
                Geometry geometry = graphComponent.getGraph().getCellGeometry(state.getCell());
                CellState clone = (CellState) state.clone();
                List<Point> points = geometry.getPoints();
                GraphView view = clone.getView();

                if (isSource || isTarget) {
                    marker.process(e);
                    CellState currentState = marker.getValidState();
                    target = state.getVisibleTerminal(!isSource);

                    if (currentState != null) {
                        source = currentState.getCell();
                    } else {
                        Point pt = new Point(e.getPoint());

                        if (gridEnabledEvent) {
                            pt = graphComponent.snapScaledPoint(pt);
                        }

                        clone.setAbsoluteTerminalPoint(pt, isSource);
                    }

                    if (!isSource) {
                        ICell tmp = source;
                        source = target;
                        target = tmp;
                    }
                } else {
                    Point point = convertPoint(new Point(e.getPoint()), gridEnabledEvent);

                    if (points == null) {
                        points = Arrays.asList(point);
                    } else if (index - 1 < points.size()) {
                        points = new ArrayList<Point>(points);
                        points.set(index - 1, point);
                    }

                    source = view.getVisibleTerminal(state.getCell(), true);
                    target = view.getVisibleTerminal(state.getCell(), false);
                }

                // Computes the points for the edge style and terminals
                CellState sourceState = view.getState(source);
                CellState targetState = view.getState(target);

                ConnectionConstraint sourceConstraint = graphComponent.getGraph().getConnectionConstraint(clone, sourceState, true);
                ConnectionConstraint targetConstraint = graphComponent.getGraph().getConnectionConstraint(clone, targetState, false);

				/* TODO: Implement ConstraintHandler
				ConnectionConstraint constraint = constraintHandler.currentConstraint;

				if (constraint == null)
				{
					constraint = new ConnectionConstraint();
				}
				
				if (isSource)
				{
					sourceConstraint = constraint;
				}
				else if (isTarget)
				{
					targetConstraint = constraint;
				}
				*/

                if (!isSource || sourceState != null) {
                    view.updateFixedTerminalPoint(clone, sourceState, true, sourceConstraint);
                }

                if (!isTarget || targetState != null) {
                    view.updateFixedTerminalPoint(clone, targetState, false, targetConstraint);
                }

                view.updatePoints(clone, points, sourceState, targetState);
                view.updateFloatingTerminalPoints(clone, sourceState, targetState);

                // Uses the updated points from the cloned state to draw the preview
                p = createPoints(clone);
                preview.setBounds(getPreviewBounds());
            }

            if (!preview.isVisible() && graphComponent.isSignificant(e.getX() - first.x, e.getY() - first.y)) {
                preview.setVisible(true);
            } else if (preview.isVisible()) {
                preview.repaint();
            }

            e.consume();
        }
    }

    public void mouseReleased(MouseEvent e) {
        Graph graph = graphComponent.getGraph();

        if (!e.isConsumed() && first != null) {
            double dx = e.getX() - first.x;
            double dy = e.getY() - first.y;

            if (graphComponent.isSignificant(dx, dy)) {
                if (error != null) {
                    if (error.length() > 0) {
                        JOptionPane.showMessageDialog(graphComponent, error);
                    }
                } else if (isLabel(index)) {
                    Point abs = state.getAbsoluteOffset();
                    dx = abs.getX() - first.x;
                    dy = abs.getY() - first.y;

                    Point pt = new Point(e.getPoint());

                    if (gridEnabledEvent) {
                        pt = graphComponent.snapScaledPoint(pt, dx, dy);
                    }

                    if (constrainedEvent) {
                        if (Math.abs(e.getX() - first.x) > Math.abs(e.getY() - first.y)) {
                            pt.setY(abs.getY());
                        } else {
                            pt.setX(abs.getX());
                        }
                    }

                    moveLabelTo(state, pt.getX() + dx, pt.getY() + dy);
                } else if (marker.hasValidState() && (isSource(index) || isTarget(index))) {
                    connect(state.getCell(), marker.getValidState().getCell(), isSource(index), graphComponent.isCloneEvent(e) && isCloneEnabled());
                } else if ((!isSource(index) && !isTarget(index)) || graphComponent.getGraph().isAllowDanglingEdges()) {
                    movePoint(state.getCell(), index, convertPoint(new Point(e.getPoint()), gridEnabledEvent));
                }

                e.consume();
            }
        }

        if (!e.isConsumed() && isFlipEvent(e)) {
            graph.flipEdge(state.getCell());
            e.consume();
        }

        super.mouseReleased(e);
    }

    /**
     * Extends the implementation to reset the current error and marker.
     */
    public void reset() {
        super.reset();

        marker.reset();
        error = null;
    }

    /**
     * Moves the edges control point with the given index to the given point.
     */
    protected void movePoint(ICell edge, int pointIndex, Point point) {
        IGraphModel model = graphComponent.getGraph().getModel();
        Geometry geometry = model.getGeometry(edge);

        if (geometry != null) {
            model.beginUpdate();
            try {
                geometry = geometry.clone();

                if (isSource(index) || isTarget(index)) {
                    connect(edge, null, isSource(index), false);
                    geometry.setTerminalPoint(point, isSource(index));
                } else {
                    List<Point> pts = geometry.getPoints();

                    if (pts == null) {
                        pts = new ArrayList<Point>();
                        geometry.setPoints(pts);
                    }

                    if (pts != null) {
                        if (pointIndex <= pts.size()) {
                            pts.set(pointIndex - 1, point);
                        } else if (pointIndex - 1 <= pts.size()) {
                            pts.add(pointIndex - 1, point);
                        }
                    }
                }

                model.setGeometry(edge, geometry);
            } finally {
                model.endUpdate();
            }
        }
    }

    /**
     * Connects the given edge to the given source or target terminal.
     *
     * @param edge
     * @param terminal
     * @param isSource
     */
    protected void connect(ICell edge, ICell terminal, boolean isSource, boolean isClone) {
        Graph graph = graphComponent.getGraph();
        IGraphModel model = graph.getModel();

        model.beginUpdate();
        try {
            if (isClone) {
                ICell clone = graph.cloneCells(List.of(edge)).get(0);

                ICell parent = model.getParent(edge);
                graph.addCells(List.of(clone), parent);

                ICell other = model.getTerminal(edge, !isSource);
                graph.connectCell(clone, other, !isSource);

                graph.setSelectionCell(clone);
                edge = clone;
            }

            // Passes an empty constraint to reset constraint information
            graph.connectCell(edge, terminal, isSource, new ConnectionConstraint());
        } finally {
            model.endUpdate();
        }
    }

    /**
     * Moves the label to the given position.
     */
    protected void moveLabelTo(CellState edgeState, double x, double y) {
        Graph graph = graphComponent.getGraph();
        IGraphModel model = graph.getModel();
        Geometry geometry = model.getGeometry(state.getCell());

        if (geometry != null) {
            geometry = geometry.clone();

            // Resets the relative location stored inside the geometry
            Point pt = graph.getView().getRelativePoint(edgeState, x, y);
            geometry.setX(pt.getX());
            geometry.setY(pt.getY());

            // Resets the offset inside the geometry to find the offset
            // from the resulting point
            double scale = graph.getView().getScale();
            geometry.setOffset(new Point(0, 0));
            pt = graph.getView().getPoint(edgeState, geometry);
            geometry.setOffset(new Point(Math.round((x - pt.getX()) / scale), Math.round((y - pt.getY()) / scale)));

            model.setGeometry(edgeState.getCell(), geometry);
        }
    }

    protected Cursor getCursor(MouseEvent e, int index) {
        Cursor cursor = null;

        if (isLabel(index)) {
            cursor = new Cursor(Cursor.MOVE_CURSOR);
        } else {
            cursor = new Cursor(Cursor.HAND_CURSOR);
        }

        return cursor;
    }

    public Color getSelectionColor() {
        return SwingConstants.EDGE_SELECTION_COLOR;
    }

    public Stroke getSelectionStroke() {
        return SwingConstants.EDGE_SELECTION_STROKE;
    }

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        Stroke stroke = g2.getStroke();
        g2.setStroke(getSelectionStroke());
        g.setColor(getSelectionColor());

        java.awt.Point last = state.getAbsolutePoint(0).getPoint();

        for (int i = 1; i < state.getAbsolutePointCount(); i++) {
            java.awt.Point current = state.getAbsolutePoint(i).getPoint();
            Line2D line = new Line2D.Float(last.x, last.y, current.x, current.y);

            Rectangle bounds = g2.getStroke().createStrokedShape(line).getBounds();

            if (g.hitClip(bounds.x, bounds.y, bounds.width, bounds.height)) {
                g2.draw(line);
            }

            last = current;
        }

        g2.setStroke(stroke);
        super.paint(g);
    }

}
