/**
 * Copyright (c) 2008-2012, JGraph Ltd
 * <p>
 * Known issue: Drag image size depends on the initial position and may sometimes
 * not align with the grid when dragging. This is because the rounding of the width
 * and height at the initial position may be different than that at the current
 * position as the left and bottom side of the shape must align to the grid lines.
 */
package com.faforever.neroxis.ngraph.swing.handler;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.swing.util.GraphTransferable;
import com.faforever.neroxis.ngraph.swing.util.MouseAdapter;
import com.faforever.neroxis.ngraph.swing.util.SwingConstants;
import com.faforever.neroxis.ngraph.util.CellRenderer;
import com.faforever.neroxis.ngraph.util.Event;
import com.faforever.neroxis.ngraph.util.EventObject;
import com.faforever.neroxis.ngraph.util.EventSource.IEventListener;
import com.faforever.neroxis.ngraph.util.Point;
import com.faforever.neroxis.ngraph.util.Rectangle;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraphHandler extends MouseAdapter implements DropTargetListener {

    /**
     *
     */
    private static final long serialVersionUID = 3241109976696510225L;
    private static final Logger log = Logger.getLogger(GraphHandler.class.getName());

    /**
     * Default is Cursor.DEFAULT_CURSOR.
     */
    public static Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

    /**
     * Default is Cursor.MOVE_CURSOR.
     */
    public static Cursor MOVE_CURSOR = new Cursor(Cursor.MOVE_CURSOR);

    /**
     * Default is Cursor.HAND_CURSOR.
     */
    public static Cursor FOLD_CURSOR = new Cursor(Cursor.HAND_CURSOR);

    /**
     * Reference to the enclosing graph component.
     */
    protected GraphComponent graphComponent;

    /**
     * Specifies if the handler is enabled. Default is true.
     */
    protected boolean enabled = true;

    /**
     * Specifies if cloning by control-drag is enabled. Default is true.
     */
    protected boolean cloneEnabled = true;

    /**
     * Specifies if moving is enabled. Default is true.
     */
    protected boolean moveEnabled = true;

    /**
     * Specifies if moving is enabled. Default is true.
     */
    protected boolean selectEnabled = true;

    /**
     * Specifies if the cell marker should be called (for splitting edges and
     * dropping cells into groups). Default is true.
     */
    protected boolean markerEnabled = true;

    /**
     * Specifies if cells may be moved out of their parents. Default is true.
     */
    protected boolean removeCellsFromParent = true;

    /**
     *
     */
    protected MovePreview movePreview;

    /**
     * Specifies if live preview should be used if possible. Default is false.
     */
    protected boolean livePreview = false;

    /**
     * Specifies if an image should be used for preview. Default is true.
     */
    protected boolean imagePreview = true;

    /**
     * Specifies if the preview should be centered around the mouse cursor if there
     * was no mouse click to define the offset within the shape (eg. drag from
     * external source). Default is true.
     */
    protected boolean centerPreview = true;

    /**
     * Specifies if this handler should be painted on top of all other components.
     * Default is true.
     */
    protected boolean keepOnTop = true;

    /**
     * Holds the cells that are being moved by this handler.
     */
    protected transient ICell[] cells;

    /**
     * Holds the image that is being used for the preview.
     */
    protected transient ImageIcon dragImage;

    /**
     * Holds the start location of the mouse gesture.
     */
    protected transient java.awt.Point first;

    /**
     *
     */
    protected transient ICell cell;

    /**
     *
     */
    protected transient ICell initialCell;

    /**
     *
     */
    protected transient ICell[] dragCells;

    /**
     *
     */
    protected transient CellMarker marker;

    /**
     *
     */
    protected transient boolean canImport;

    /**
     * Scaled, translated bounds of the selection cells.
     */
    protected transient Rectangle cellBounds;

    /**
     * Scaled, translated bounding box of the selection cells.
     */
    protected transient Rectangle bbox;

    /**
     * Unscaled, untranslated bounding box of the selection cells.
     */
    protected transient Rectangle transferBounds;

    /**
     *
     */
    protected transient boolean visible = false;

    /**
     *
     */
    protected transient java.awt.Rectangle previewBounds = null;

    /**
     * Workaround for alt-key-state not correct in mouseReleased. Note: State
     * of the alt-key is not available during drag-and-drop.
     */
    protected transient boolean gridEnabledEvent = false;

    /**
     * Workaround for shift-key-state not correct in mouseReleased.
     */
    protected transient boolean constrainedEvent = false;

    /**
     * Reference to the current drop target.
     */
    protected transient DropTarget currentDropTarget = null;

    /**
     * @param graphComponent
     */
    public GraphHandler(final GraphComponent graphComponent) {
        this.graphComponent = graphComponent;
        marker = createMarker();
        movePreview = createMovePreview();

        // Installs the paint handler
        graphComponent.addListener(Event.AFTER_PAINT, new IEventListener() {
            public void invoke(Object sender, EventObject evt) {
                Graphics g = (Graphics) evt.getProperty("g");
                paint(g);
            }
        });

        // Listens to all mouse events on the rendering control
        graphComponent.getGraphControl().addMouseListener(this);
        graphComponent.getGraphControl().addMouseMotionListener(this);

        // Drag target creates preview image
        installDragGestureHandler();

        // Listens to dropped graph cells
        installDropTargetHandler();

        // Listens to changes of the transferhandler
        graphComponent.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("transferHandler")) {
                    if (currentDropTarget != null) {
                        currentDropTarget.removeDropTargetListener(GraphHandler.this);
                    }

                    installDropTargetHandler();
                }
            }
        });

        setVisible(false);
    }

    /**
     * Helper method to return the component for a drop target event.
     */
    protected static GraphTransferHandler getGraphTransferHandler(DropTargetEvent e) {
        JComponent component = getDropTarget(e);
        TransferHandler transferHandler = component.getTransferHandler();

        if (transferHandler instanceof GraphTransferHandler) {
            return (GraphTransferHandler) transferHandler;
        }

        return null;
    }

    /**
     * Helper method to return the component for a drop target event.
     */
    protected static JComponent getDropTarget(DropTargetEvent e) {
        return (JComponent) e.getDropTargetContext().getComponent();
    }

    /**
     *
     */
    protected void installDragGestureHandler() {
        DragGestureListener dragGestureListener = new DragGestureListener() {
            public void dragGestureRecognized(DragGestureEvent e) {
                if (graphComponent.isDragEnabled() && first != null) {
                    final TransferHandler th = graphComponent.getTransferHandler();

                    if (th instanceof GraphTransferHandler) {
                        final GraphTransferable t = (GraphTransferable) ((GraphTransferHandler) th).createTransferable(graphComponent);

                        if (t != null) {
                            e.startDrag(null, SwingConstants.EMPTY_IMAGE, new java.awt.Point(), t, new DragSourceAdapter() {

                                /**
                                 *
                                 */
                                public void dragDropEnd(DragSourceDropEvent dsde) {
                                    ((GraphTransferHandler) th).exportDone(graphComponent, t, TransferHandler.NONE);
                                    first = null;
                                }
                            });
                        }
                    }
                }
            }
        };

        DragSource dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(graphComponent.getGraphControl(), (isCloneEnabled()) ? DnDConstants.ACTION_COPY_OR_MOVE : DnDConstants.ACTION_MOVE, dragGestureListener);
    }

    /**
     *
     */
    protected void installDropTargetHandler() {
        DropTarget dropTarget = graphComponent.getDropTarget();

        try {
            if (dropTarget != null) {
                dropTarget.addDropTargetListener(this);
                currentDropTarget = dropTarget;
            }
        } catch (TooManyListenersException e) {
            // should not happen... swing drop target is multicast
            log.log(Level.SEVERE, "Failed to install drop target handler", e);
        }
    }

    /**
     *
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     *
     */
    public void setVisible(boolean value) {
        if (visible != value) {
            visible = value;

            if (previewBounds != null) {
                graphComponent.getGraphControl().repaint(previewBounds);
            }
        }
    }

    /**
     *
     */
    public void setPreviewBounds(java.awt.Rectangle bounds) {
        if ((bounds == null && previewBounds != null) || (bounds != null && previewBounds == null) || (bounds != null && previewBounds != null && !bounds.equals(previewBounds))) {
            java.awt.Rectangle dirty = null;

            if (isVisible()) {
                dirty = previewBounds;

                if (dirty != null) {
                    dirty.add(bounds);
                } else {
                    dirty = bounds;
                }
            }

            previewBounds = bounds;

            if (dirty != null) {
                graphComponent.getGraphControl().repaint(dirty.x - 1, dirty.y - 1, dirty.width + 2, dirty.height + 2);
            }
        }
    }

    /**
     *
     */
    protected MovePreview createMovePreview() {
        return new MovePreview(graphComponent);
    }

    /**
     *
     */
    public MovePreview getMovePreview() {
        return movePreview;
    }

    /**
     *
     */
    protected CellMarker createMarker() {
        CellMarker marker = new CellMarker(graphComponent, Color.BLUE) {
            /**
             *
             */
            private static final long serialVersionUID = -8451338653189373347L;

            /**
             *
             */
            public boolean isEnabled() {
                return graphComponent.getGraph().isDropEnabled();
            }

            /**
             *
             */
            public ICell getCell(MouseEvent e) {
                IGraphModel model = graphComponent.getGraph().getModel();
                TransferHandler th = graphComponent.getTransferHandler();
                boolean isLocal = th instanceof GraphTransferHandler && ((GraphTransferHandler) th).isLocalDrag();

                Graph graph = graphComponent.getGraph();
                ICell cell = super.getCell(e);
                ICell[] cells = (isLocal) ? graph.getSelectionCells() : dragCells;
                cell = graph.getDropTarget(cells, e.getPoint(), cell);

                // Checks if parent is dropped into child
                ICell parent = cell;

                while (parent != null) {
                    if (Utils.contains(cells, parent)) {
                        return null;
                    }

                    parent = model.getParent(parent);
                }

                boolean clone = graphComponent.isCloneEvent(e) && isCloneEnabled();

                if (isLocal && cell != null && cells.length > 0 && !clone && graph.getModel().getParent(cells[0]) == cell) {
                    cell = null;
                }

                return cell;
            }

        };

        // Swimlane content area will not be transparent drop targets
        marker.setSwimlaneContentEnabled(true);

        return marker;
    }

    /**
     *
     */
    public GraphComponent getGraphComponent() {
        return graphComponent;
    }

    /**
     *
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     *
     */
    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     *
     */
    public boolean isCloneEnabled() {
        return cloneEnabled;
    }

    /**
     *
     */
    public void setCloneEnabled(boolean value) {
        cloneEnabled = value;
    }

    /**
     *
     */
    public boolean isMoveEnabled() {
        return moveEnabled;
    }

    /**
     *
     */
    public void setMoveEnabled(boolean value) {
        moveEnabled = value;
    }

    /**
     *
     */
    public boolean isMarkerEnabled() {
        return markerEnabled;
    }

    /**
     *
     */
    public void setMarkerEnabled(boolean value) {
        markerEnabled = value;
    }

    /**
     *
     */
    public CellMarker getMarker() {
        return marker;
    }

    /**
     *
     */
    public void setMarker(CellMarker value) {
        marker = value;
    }

    /**
     *
     */
    public boolean isSelectEnabled() {
        return selectEnabled;
    }

    /**
     *
     */
    public void setSelectEnabled(boolean value) {
        selectEnabled = value;
    }

    /**
     *
     */
    public boolean isRemoveCellsFromParent() {
        return removeCellsFromParent;
    }

    /**
     *
     */
    public void setRemoveCellsFromParent(boolean value) {
        removeCellsFromParent = value;
    }

    /**
     *
     */
    public boolean isLivePreview() {
        return livePreview;
    }

    /**
     *
     */
    public void setLivePreview(boolean value) {
        livePreview = value;
    }

    /**
     *
     */
    public boolean isImagePreview() {
        return imagePreview;
    }

    /**
     *
     */
    public void setImagePreview(boolean value) {
        imagePreview = value;
    }

    /**
     *
     */
    public boolean isCenterPreview() {
        return centerPreview;
    }

    /**
     *
     */
    public void setCenterPreview(boolean value) {
        centerPreview = value;
    }

    /**
     *
     */
    public void updateDragImage(ICell[] cells) {
        dragImage = null;

        if (cells != null && cells.length > 0) {
            Image img = CellRenderer.createBufferedImage(graphComponent.getGraph(), cells, graphComponent.getGraph().getView().getScale(), null, graphComponent.isAntiAlias(), null, graphComponent.getCanvas());

            if (img != null) {
                dragImage = new ImageIcon(img);
                previewBounds.setSize(dragImage.getIconWidth(), dragImage.getIconHeight());
            }
        }
    }

    /**
     *
     */
    public void mouseMoved(MouseEvent e) {
        if (graphComponent.isEnabled() && isEnabled() && !e.isConsumed()) {
            Cursor cursor = getCursor(e);

            if (cursor != null) {
                graphComponent.getGraphControl().setCursor(cursor);
                e.consume();
            } else {
                graphComponent.getGraphControl().setCursor(DEFAULT_CURSOR);
            }
        }
    }

    /**
     *
     */
    protected Cursor getCursor(MouseEvent e) {
        Cursor cursor = null;

        if (isMoveEnabled()) {
            ICell cell = graphComponent.getCellAt(e.getX(), e.getY(), false);

            if (cell != null) {
                if (graphComponent.isFoldingEnabled() && graphComponent.hitFoldingIcon(cell, e.getX(), e.getY())) {
                    cursor = FOLD_CURSOR;
                } else if (graphComponent.getGraph().isCellMovable(cell)) {
                    cursor = MOVE_CURSOR;
                }
            }
        }

        return cursor;
    }

    /**
     *
     */
    public void dragEnter(DropTargetDragEvent e) {
        JComponent component = getDropTarget(e);
        TransferHandler th = component.getTransferHandler();
        boolean isLocal = th instanceof GraphTransferHandler && ((GraphTransferHandler) th).isLocalDrag();

        if (isLocal) {
            canImport = true;
        } else {
            canImport = graphComponent.isImportEnabled() && th.canImport(component, e.getCurrentDataFlavors());
        }

        if (canImport) {
            transferBounds = null;
            setVisible(false);

            try {
                Transferable t = e.getTransferable();

                if (t.isDataFlavorSupported(GraphTransferable.dataFlavor)) {
                    GraphTransferable gt = (GraphTransferable) t.getTransferData(GraphTransferable.dataFlavor);
                    dragCells = gt.getCells();

                    if (gt.getBounds() != null) {
                        Graph graph = graphComponent.getGraph();
                        double scale = graph.getView().getScale();
                        transferBounds = gt.getBounds();
                        int w = (int) Math.ceil((transferBounds.getWidth() + 1) * scale);
                        int h = (int) Math.ceil((transferBounds.getHeight() + 1) * scale);
                        setPreviewBounds(new java.awt.Rectangle((int) transferBounds.getX(), (int) transferBounds.getY(), w, h));

                        if (imagePreview) {
                            // Does not render fixed cells for local preview
                            // but ignores movable state for non-local previews
                            if (isLocal) {
                                if (!isLivePreview()) {
                                    updateDragImage(graph.getMovableCells(dragCells));
                                }
                            } else {
                                ICell[] tmp = graphComponent.getImportableCells(dragCells);
                                updateDragImage(tmp);

                                // Shows no drag icon if import is allowed but none
                                // of the cells can be imported
                                if (tmp == null || tmp.length == 0) {
                                    canImport = false;
                                    e.rejectDrag();

                                    return;
                                }
                            }
                        }

                        setVisible(true);
                    }
                }

                e.acceptDrag(TransferHandler.COPY_OR_MOVE);
            } catch (Exception ex) {
                // do nothing
                log.log(Level.SEVERE, "Failed to handle dragEnter", ex);
            }

        } else {
            e.rejectDrag();
        }
    }

    /**
     *
     */
    public void mousePressed(MouseEvent e) {
        if (graphComponent.isEnabled() && isEnabled() && !e.isConsumed() && !graphComponent.isForceMarqueeEvent(e)) {
            cell = graphComponent.getCellAt(e.getX(), e.getY(), false);
            initialCell = cell;

            if (cell != null) {
                if (isSelectEnabled() && !graphComponent.getGraph().isCellSelected(cell)) {
                    graphComponent.selectCellForEvent(cell, e);
                    cell = null;
                }

                // Starts move if the cell under the mouse is movable and/or any
                // cells of the selection are movable
                if (isMoveEnabled() && !e.isPopupTrigger()) {
                    start(e);
                    e.consume();
                }
            } else if (e.isPopupTrigger()) {
                graphComponent.getGraph().clearSelection();
            }
        }
    }

    /**
     *
     */
    public ICell[] getCells(ICell initialCell) {
        Graph graph = graphComponent.getGraph();

        return graph.getMovableCells(graph.getSelectionCells());
    }

    /**
     *
     */
    public void start(MouseEvent e) {
        if (isLivePreview()) {
            movePreview.start(e, graphComponent.getGraph().getView().getState(initialCell));
        } else {
            Graph graph = graphComponent.getGraph();

            // Constructs an array with cells that are indeed movable
            cells = getCells(initialCell);
            cellBounds = graph.getView().getBounds(cells);

            if (cellBounds != null) {
                // Updates the size of the graph handler that is in
                // charge of painting all other handlers
                bbox = graph.getView().getBoundingBox(cells);

                java.awt.Rectangle bounds = cellBounds.getRectangle();
                bounds.width += 1;
                bounds.height += 1;
                setPreviewBounds(bounds);
            }
        }

        first = e.getPoint();
    }

    /**
     *
     */
    public void dropActionChanged(DropTargetDragEvent e) {
        // do nothing
    }

    /**
     * @param e
     */
    public void dragOver(DropTargetDragEvent e) {
        if (canImport) {
            mouseDragged(createEvent(e));
            GraphTransferHandler handler = getGraphTransferHandler(e);

            if (handler != null) {
                Graph graph = graphComponent.getGraph();
                double scale = graph.getView().getScale();
                java.awt.Point pt = SwingUtilities.convertPoint(graphComponent, e.getLocation(), graphComponent.getGraphControl());

                pt = graphComponent.snapScaledPoint(new Point(pt)).getPoint();
                handler.setLocation(new java.awt.Point(pt));

                int dx = 0;
                int dy = 0;

                // Centers the preview image
                if (centerPreview && transferBounds != null) {
                    dx -= Math.round(transferBounds.getWidth() * scale / 2);
                    dy -= Math.round(transferBounds.getHeight() * scale / 2);
                }

                // Sets the drop offset so that the location in the transfer
                // handler reflects the actual mouse position
                handler.setOffset(new java.awt.Point((int) graph.snap(dx / scale), (int) graph.snap(dy / scale)));
                pt.translate(dx, dy);

                // Shifts the preview so that overlapping parts do not
                // affect the centering
                if (transferBounds != null && dragImage != null) {
                    dx = (int) Math.round((dragImage.getIconWidth() - 2 - transferBounds.getWidth() * scale) / 2);
                    dy = (int) Math.round((dragImage.getIconHeight() - 2 - transferBounds.getHeight() * scale) / 2);
                    pt.translate(-dx, -dy);
                }

                if (!handler.isLocalDrag() && previewBounds != null) {
                    setPreviewBounds(new java.awt.Rectangle(pt, previewBounds.getSize()));
                }
            }
        } else {
            e.rejectDrag();
        }
    }

    /**
     *
     */
    public java.awt.Point convertPoint(java.awt.Point pt) {
        pt = SwingUtilities.convertPoint(graphComponent, pt, graphComponent.getGraphControl());

        pt.x -= graphComponent.getHorizontalScrollBar().getValue();
        pt.y -= graphComponent.getVerticalScrollBar().getValue();

        return pt;
    }

    /**
     *
     */
    public void mouseDragged(MouseEvent e) {
        // LATER: Check scrollborder, use scroll-increments, do not
        // scroll when over ruler dragging from library
        if (graphComponent.isAutoScroll()) {
            graphComponent.getGraphControl().scrollRectToVisible(new java.awt.Rectangle(e.getPoint()));
        }

        if (!e.isConsumed()) {
            gridEnabledEvent = graphComponent.isGridEnabledEvent(e);
            constrainedEvent = graphComponent.isConstrainedEvent(e);

            if (constrainedEvent && first != null) {
                int x = e.getX();
                int y = e.getY();

                if (Math.abs(e.getX() - first.x) > Math.abs(e.getY() - first.y)) {
                    y = first.y;
                } else {
                    x = first.x;
                }

                e = new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiers(), x, y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
            }

            if (isVisible() && isMarkerEnabled()) {
                marker.process(e);
            }

            if (first != null) {
                if (movePreview.isActive()) {
                    double dx = e.getX() - first.x;
                    double dy = e.getY() - first.y;

                    if (graphComponent.isGridEnabledEvent(e)) {
                        Graph graph = graphComponent.getGraph();

                        dx = graph.snap(dx);
                        dy = graph.snap(dy);
                    }

                    boolean clone = isCloneEnabled() && graphComponent.isCloneEvent(e);
                    movePreview.update(e, dx, dy, clone);
                    e.consume();
                } else if (cellBounds != null) {
                    double dx = e.getX() - first.x;
                    double dy = e.getY() - first.y;

                    if (previewBounds != null) {
                        setPreviewBounds(new java.awt.Rectangle(getPreviewLocation(e, gridEnabledEvent), previewBounds.getSize()));
                    }

                    if (!isVisible() && graphComponent.isSignificant(dx, dy)) {
                        if (imagePreview && dragImage == null && !graphComponent.isDragEnabled()) {
                            updateDragImage(cells);
                        }

                        setVisible(true);
                    }

                    e.consume();
                }
            }
        }
    }

    /**
     *
     */
    protected java.awt.Point getPreviewLocation(MouseEvent e, boolean gridEnabled) {
        int x = 0;
        int y = 0;

        if (first != null && cellBounds != null) {
            Graph graph = graphComponent.getGraph();
            double scale = graph.getView().getScale();
            Point trans = graph.getView().getTranslate();

            // LATER: Drag image _size_ depends on the initial position and may sometimes
            // not align with the grid when dragging. This is because the rounding of the width
            // and height at the initial position may be different than that at the current
            // position as the left and bottom side of the shape must align to the grid lines.
            // Only fix is a full repaint of the drag cells at each new mouse location.
            double dx = e.getX() - first.x;
            double dy = e.getY() - first.y;

            double dxg = ((cellBounds.getX() + dx) / scale) - trans.getX();
            double dyg = ((cellBounds.getY() + dy) / scale) - trans.getY();

            if (gridEnabled) {
                dxg = graph.snap(dxg);
                dyg = graph.snap(dyg);
            }

            x = (int) Math.round((dxg + trans.getX()) * scale) + (int) Math.round(bbox.getX()) - (int) Math.round(cellBounds.getX());
            y = (int) Math.round((dyg + trans.getY()) * scale) + (int) Math.round(bbox.getY()) - (int) Math.round(cellBounds.getY());
        }

        return new java.awt.Point(x, y);
    }

    /**
     * @param e
     */
    public void dragExit(DropTargetEvent e) {
        GraphTransferHandler handler = getGraphTransferHandler(e);

        if (handler != null) {
            handler.setLocation(null);
        }

        dragCells = null;
        setVisible(false);
        marker.reset();
        reset();
    }

    /**
     * @param e
     */
    public void drop(DropTargetDropEvent e) {
        if (canImport) {
            GraphTransferHandler handler = getGraphTransferHandler(e);
            MouseEvent event = createEvent(e);

            // Ignores the event in mouseReleased if it is
            // handled by the transfer handler as a drop
            if (handler != null && !handler.isLocalDrag()) {
                event.consume();
            }

            mouseReleased(event);
        }
    }

    /**
     *
     */
    public void mouseReleased(MouseEvent e) {
        if (graphComponent.isEnabled() && isEnabled() && !e.isConsumed()) {
            Graph graph = graphComponent.getGraph();
            double dx = 0;
            double dy = 0;

            if (first != null && (cellBounds != null || movePreview.isActive())) {
                double scale = graph.getView().getScale();
                Point trans = graph.getView().getTranslate();

                // TODO: Simplify math below, this was copy pasted from
                // getPreviewLocation with the rounding removed
                dx = e.getX() - first.x;
                dy = e.getY() - first.y;

                if (cellBounds != null) {
                    double dxg = ((cellBounds.getX() + dx) / scale) - trans.getX();
                    double dyg = ((cellBounds.getY() + dy) / scale) - trans.getY();

                    if (gridEnabledEvent) {
                        dxg = graph.snap(dxg);
                        dyg = graph.snap(dyg);
                    }

                    double x = ((dxg + trans.getX()) * scale) + (bbox.getX()) - (cellBounds.getX());
                    double y = ((dyg + trans.getY()) * scale) + (bbox.getY()) - (cellBounds.getY());

                    dx = Math.round((x - bbox.getX()) / scale);
                    dy = Math.round((y - bbox.getY()) / scale);
                }
            }

            if (first == null || !graphComponent.isSignificant(e.getX() - first.x, e.getY() - first.y)) {
                // Delayed handling of selection
                if (cell != null && !e.isPopupTrigger() && isSelectEnabled() && (first != null || !isMoveEnabled())) {
                    graphComponent.selectCellForEvent(cell, e);
                }

                // Delayed folding for cell that was initially under the mouse
                if (graphComponent.isFoldingEnabled() && graphComponent.hitFoldingIcon(initialCell, e.getX(), e.getY())) {
                    fold(initialCell);
                } else {
                    // Handles selection if no cell was initially under the mouse
                    ICell tmp = graphComponent.getCellAt(e.getX(), e.getY(), graphComponent.isSwimlaneSelectionEnabled());

                    if (cell == null && first == null) {
                        if (tmp == null) {
                            if (!graphComponent.isToggleEvent(e)) {
                                graph.clearSelection();
                            }
                        } else if (graph.isSwimlane(tmp) && graphComponent.getCanvas().hitSwimlaneContent(graphComponent, graph.getView().getState(tmp), e.getX(), e.getY())) {
                            graphComponent.selectCellForEvent(tmp, e);
                        }
                    }

                    if (graphComponent.isFoldingEnabled() && graphComponent.hitFoldingIcon(tmp, e.getX(), e.getY())) {
                        fold(tmp);
                        e.consume();
                    }
                }
            } else {
                boolean xGreater = Math.abs(dx) > Math.abs(dy);
                if (movePreview.isActive()) {
                    if (graphComponent.isConstrainedEvent(e)) {
                        if (xGreater) {
                            dy = 0;
                        } else {
                            dx = 0;
                        }
                    }

                    CellState markedState = marker.getMarkedState();
                    ICell target = (markedState != null) ? markedState.getCell() : null;

                    // FIXME: Cell is null if selection was carried out, need other variable
                    //trace("cell", cell);

                    if (target == null && isRemoveCellsFromParent() && shouldRemoveCellFromParent(graph.getModel().getParent(initialCell), cells, e)) {
                        target = graph.getDefaultParent();
                    }

                    boolean clone = isCloneEnabled() && graphComponent.isCloneEvent(e);
                    ICell[] result = movePreview.stop(true, e, dx, dy, clone, target);

                    if (cells != result) {
                        graph.setSelectionCells(result);
                    }

                    e.consume();
                } else if (isVisible()) {
                    if (constrainedEvent) {
                        if (xGreater) {
                            dy = 0;
                        } else {
                            dx = 0;
                        }
                    }

                    CellState targetState = marker.getValidState();
                    ICell target = (targetState != null) ? targetState.getCell() : null;

                    if (graph.isSplitEnabled() && graph.isSplitTarget(target, cells)) {
                        graph.splitEdge(target, cells, dx, dy);
                    } else {
                        moveCells(cells, dx, dy, target, e);
                    }

                    e.consume();
                }
            }
        }

        reset();
    }

    /**
     *
     */
    protected void fold(ICell cell) {
        boolean collapse = !graphComponent.getGraph().isCellCollapsed(cell);
        graphComponent.getGraph().foldCells(collapse, false, new ICell[]{cell});
    }

    /**
     *
     */
    public void reset() {
        if (movePreview.isActive()) {
            movePreview.stop(false, null, 0, 0, false, null);
        }

        setVisible(false);
        marker.reset();
        initialCell = null;
        dragCells = null;
        dragImage = null;
        cells = null;
        first = null;
        cell = null;
    }

    /**
     * Returns true if the given cells should be removed from the parent for the specified
     * mousereleased event.
     */
    protected boolean shouldRemoveCellFromParent(ICell parent, ICell[] cells, MouseEvent e) {
        if (graphComponent.getGraph().getModel().isVertex(parent)) {
            CellState pState = graphComponent.getGraph().getView().getState(parent);

            return pState != null && !pState.contains(e.getX(), e.getY());
        }

        return false;
    }

    /**
     * @param dx
     * @param dy
     * @param e
     */
    protected void moveCells(ICell[] cells, double dx, double dy, ICell target, MouseEvent e) {
        Graph graph = graphComponent.getGraph();
        boolean clone = e.isControlDown() && isCloneEnabled();

        if (clone) {
            cells = graph.getCloneableCells(cells);
        }

        if (cells.length > 0) {
            // Removes cells from parent
            if (target == null && isRemoveCellsFromParent() && shouldRemoveCellFromParent(graph.getModel().getParent(initialCell), cells, e)) {
                target = graph.getDefaultParent();
            }

            ICell[] tmp = graph.moveCells(cells, dx, dy, clone, target, e.getPoint());

            if (isSelectEnabled() && clone && tmp != null && tmp.length == cells.length) {
                graph.setSelectionCells(tmp);
            }
        }
    }

    /**
     *
     */
    public void paint(Graphics g) {
        if (isVisible() && previewBounds != null) {
            if (dragImage != null) {
                // LATER: Clipping with Utils doesnt fix the problem
                // of the drawImage being painted over the scrollbars
                Graphics2D tmp = (Graphics2D) g.create();

                if (graphComponent.getPreviewAlpha() < 1) {
                    tmp.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, graphComponent.getPreviewAlpha()));
                }

                tmp.drawImage(dragImage.getImage(), previewBounds.x, previewBounds.y, dragImage.getIconWidth(), dragImage.getIconHeight(), null);
                tmp.dispose();
            } else if (!imagePreview) {
                SwingConstants.PREVIEW_BORDER.paintBorder(graphComponent, g, previewBounds.x, previewBounds.y, previewBounds.width, previewBounds.height);
            }
        }
    }

    /**
     *
     */
    protected MouseEvent createEvent(DropTargetEvent e) {
        JComponent component = getDropTarget(e);
        java.awt.Point location = null;
        int action = 0;

        if (e instanceof DropTargetDropEvent) {
            location = ((DropTargetDropEvent) e).getLocation();
            action = ((DropTargetDropEvent) e).getDropAction();
        } else if (e instanceof DropTargetDragEvent) {
            location = ((DropTargetDragEvent) e).getLocation();
            action = ((DropTargetDragEvent) e).getDropAction();
        }

        if (location != null) {
            location = convertPoint(location);
            java.awt.Rectangle r = graphComponent.getViewport().getViewRect();
            location.translate(r.x, r.y);
        }

        // LATER: Fetch state of modifier keys from event or via global
        // key listener using Toolkit.getDefaultToolkit().addAWTEventListener(
        // new AWTEventListener() {...}, AWTEvent.KEY_EVENT_MASK). Problem
        // is the event does not contain the modifier keys and the global
        // handler is not called during drag and drop.
        int mod = (action == TransferHandler.COPY) ? InputEvent.CTRL_MASK : 0;

        return new MouseEvent(component, 0, System.currentTimeMillis(), mod, location.x, location.y, 1, false, MouseEvent.BUTTON1);
    }

}
