/**
 * Copyright (c) 2009-2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.swing;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.event.AddOverlayEvent;
import com.faforever.neroxis.ngraph.event.ChangeEvent;
import com.faforever.neroxis.ngraph.event.DownEvent;
import com.faforever.neroxis.ngraph.event.EventObject;
import com.faforever.neroxis.ngraph.event.EventSource;
import com.faforever.neroxis.ngraph.event.EventSource.IEventListener;
import com.faforever.neroxis.ngraph.event.LabelChangedEvent;
import com.faforever.neroxis.ngraph.event.RemoveOverlayEvent;
import com.faforever.neroxis.ngraph.event.RepaintEvent;
import com.faforever.neroxis.ngraph.event.ScaleAndTranslateEvent;
import com.faforever.neroxis.ngraph.event.ScaleEvent;
import com.faforever.neroxis.ngraph.event.StartEditingEvent;
import com.faforever.neroxis.ngraph.event.TranslateEvent;
import com.faforever.neroxis.ngraph.event.UpEvent;
import com.faforever.neroxis.ngraph.model.GraphModel;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.style.edge.EdgeStyleFunction;
import com.faforever.neroxis.ngraph.style.edge.ElbowConnectorEdgeStyleFunction;
import com.faforever.neroxis.ngraph.style.edge.SideToSideEdgeStyleFunction;
import com.faforever.neroxis.ngraph.style.edge.TopToBottomEdgeStyleFunction;
import com.faforever.neroxis.ngraph.swing.handler.CellHandler;
import com.faforever.neroxis.ngraph.swing.handler.ConnectionHandler;
import com.faforever.neroxis.ngraph.swing.handler.EdgeHandler;
import com.faforever.neroxis.ngraph.swing.handler.ElbowEdgeHandler;
import com.faforever.neroxis.ngraph.swing.handler.GraphHandler;
import com.faforever.neroxis.ngraph.swing.handler.GraphTransferHandler;
import com.faforever.neroxis.ngraph.swing.handler.PanningHandler;
import com.faforever.neroxis.ngraph.swing.handler.SelectionCellsHandler;
import com.faforever.neroxis.ngraph.swing.handler.VertexHandler;
import com.faforever.neroxis.ngraph.swing.util.CellOverlay;
import com.faforever.neroxis.ngraph.swing.util.ICellOverlay;
import com.faforever.neroxis.ngraph.swing.view.CellEditor;
import com.faforever.neroxis.ngraph.swing.view.ICellEditor;
import com.faforever.neroxis.ngraph.swing.view.InteractiveCanvas;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.util.Resources;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;
import com.faforever.neroxis.ngraph.view.GraphView;
import com.faforever.neroxis.ngraph.view.TemporaryCellStates;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.beans.PropertyChangeListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;

/**
 * For setting the preferred size of the viewport for scrolling, use
 * Graph.setMinimumGraphSize. This component is a combined scrollpane with an
 * inner GraphControl. The control contains the actual graph display.
 * <p>
 * To set the background color of the graph, use the following code:
 *
 * <pre>
 * graphComponent.getViewport().setOpaque(true);
 * graphComponent.getViewport().setBackground(newColor);
 * </pre>
 * <p>
 * This class fires the following events:
 * <p>
 * Event.START_EDITING fires before starting the in-place editor for an
 * existing cell in startEditingAtCell. The <code>cell</code> property contains
 * the cell that is being edit and the <code>event</code> property contains
 * optional EventObject which was passed to startEditingAtCell.
 * <p>
 * Event.LABEL_CHANGED fires between begin- and endUpdate after the call to
 * Graph.cellLabelChanged in labelChanged. The <code>cell</code> property
 * contains the cell, the <code>value</code> property contains the new value for
 * the cell and the optional <code>event</code> property contains the
 * EventObject that started the edit.
 * <p>
 * Event.ADD_OVERLAY and Event.REMOVE_OVERLAY fire afer an overlay was added
 * or removed using add-/removeOverlay. The <code>cell</code> property contains
 * the cell for which the overlay was added or removed and the
 * <code>overlay</code> property contain the Overlay.
 * <p>
 * Event.BEFORE_PAINT and Event.AFTER_PAINT fire before and after the paint
 * method is called on the component. The <code>g</code> property contains the
 * graphics context which is used for painting.
 */
@SuppressWarnings("unused")
public class GraphComponent extends JScrollPane implements Printable {
    public static final int GRID_STYLE_DOT = 0;
    public static final int GRID_STYLE_CROSS = 1;
    public static final int GRID_STYLE_LINE = 2;
    public static final int GRID_STYLE_DASHED = 3;
    public static final int ZOOM_POLICY_NONE = 0;
    public static final int ZOOM_POLICY_PAGE = 1;
    public static final int ZOOM_POLICY_WIDTH = 2;
    /**
     * Specifies the default page scale. Default is 1.4
     */
    public static final double DEFAULT_PAGESCALE = 1.4;
    private static final Logger log = Logger.getLogger(GraphComponent.class.getName());
    @Serial
    private static final long serialVersionUID = -30203858391633447L;
    public static ImageIcon DEFAULT_EXPANDED_ICON = null;
    public static ImageIcon DEFAULT_COLLAPSED_ICON = null;
    public static ImageIcon DEFAULT_WARNING_ICON = null;

    /*
     * Loads the collapse and expand icons.
     */
    static {
        DEFAULT_EXPANDED_ICON = new ImageIcon(GraphComponent.class.getResource("/images/expanded.gif"));
        DEFAULT_COLLAPSED_ICON = new ImageIcon(GraphComponent.class.getResource("/images/collapsed.gif"));
        DEFAULT_WARNING_ICON = new ImageIcon(GraphComponent.class.getResource("/images/warning.gif"));
    }

    /**
     * Used for debugging the dirty region.
     */
    public boolean showDirtyRectangle = false;
    protected Graph graph;
    protected GraphControl graphControl;
    protected EventSource eventSource = new EventSource(this);
    protected ICellEditor cellEditor;
    protected ConnectionHandler connectionHandler;
    protected PanningHandler panningHandler;
    protected SelectionCellsHandler selectionCellsHandler;
    protected GraphHandler graphHandler;
    /**
     * The transparency of previewed cells from 0.0. to 0.1. 0.0 indicates
     * transparent, 1.0 indicates opaque. Default is 1.
     */
    protected float previewAlpha = 0.5f;
    /**
     * Specifies the <Image> to be returned by <getBackgroundImage>. Default
     * is null.
     */
    protected ImageIcon backgroundImage;
    /**
     * Background page format.
     */
    protected PageFormat pageFormat = new PageFormat();
    protected InteractiveCanvas canvas;
    protected BufferedImage tripleBuffer;
    protected Graphics2D tripleBufferGraphics;
    /**
     * Defines the scaling for the background page metrics. Default is
     * {@link #DEFAULT_PAGESCALE}.
     */
    protected double pageScale = DEFAULT_PAGESCALE;
    /**
     * Specifies if the background page should be visible. Default is false.
     */
    protected boolean pageVisible = false;
    /**
     * If the pageFormat should be used to determine the minimal graph bounds
     * even if the page is not visible (see pageVisible). Default is false.
     */
    protected boolean preferPageSize = false;
    /**
     * Specifies if a dashed line should be drawn between multiple pages.
     */
    protected boolean pageBreaksVisible = true;
    /**
     * Specifies the color of page breaks
     */
    protected Color pageBreakColor = Color.darkGray;
    /**
     * Specifies the number of pages in the horizontal direction.
     */
    protected int horizontalPageCount = 1;
    /**
     * Specifies the number of pages in the vertical direction.
     */
    protected int verticalPageCount = 1;
    /**
     * Specifies if the background page should be centered by automatically
     * setting the translate in the view. Default is true. This does only apply
     * if pageVisible is true.
     */
    protected boolean centerPage = true;
    /**
     * Color of the background area if layout view.
     */
    protected Color pageBackgroundColor = new Color(144, 153, 174);
    protected Color pageShadowColor = new Color(110, 120, 140);
    protected Color pageBorderColor = Color.black;
    /**
     * Specifies if the grid is visible. Default is false.
     */
    protected boolean gridVisible = false;
    protected Color gridColor = new Color(192, 192, 192);
    /**
     * Whether or not to scroll the scrollable container the graph exists in if
     * a suitable handler is active and the graph bounds already exist extended
     * in the direction of mouse travel.
     */
    protected boolean autoScroll = true;
    /**
     * Whether to extend the graph bounds and scroll towards the limit of those
     * new bounds in the direction of mouse travel if a handler is active while
     * the mouse leaves the container that the graph exists in.
     */
    protected boolean autoExtend = true;
    protected boolean dragEnabled = true;
    protected boolean importEnabled = true;
    protected boolean exportEnabled = true;
    /**
     * Specifies if folding (collapse and expand via an image icon in the graph
     * should be enabled). Default is true.
     */
    protected boolean foldingEnabled = true;
    /**
     * Specifies the tolerance for mouse clicks. Default is 4.
     */
    protected int tolerance = 4;
    /**
     * Specifies if swimlanes are selected when the mouse is released over the
     * swimlanes content area. Default is true.
     */
    protected boolean swimlaneSelectionEnabled = true;
    /**
     * Specifies if the content area should be transparent to events. Default is
     * true.
     */
    protected boolean transparentSwimlaneContent = true;
    protected int gridStyle = GRID_STYLE_DOT;
    protected ImageIcon expandedIcon = DEFAULT_EXPANDED_ICON;
    protected ImageIcon collapsedIcon = DEFAULT_COLLAPSED_ICON;
    protected ImageIcon warningIcon = DEFAULT_WARNING_ICON;
    protected boolean antiAlias = true;
    protected boolean textAntiAlias = true;
    /**
     * Specifies <escape> should be invoked when the escape key is pressed.
     * Default is true.
     */
    protected boolean escapeEnabled = true;
    /**
     * If true, when editing is to be stopped by way of selection changing, data
     * in diagram changing or other means stopCellEditing is invoked, and
     * changes are saved. This is implemented in a mouse listener in this class.
     * Default is true.
     */
    protected boolean invokesStopCellEditing = true;
    /**
     * If true, pressing the enter key without pressing control will stop
     * editing and accept the new value. This is used in <KeyHandler> to stop
     * cell editing. Default is false.
     */
    protected boolean enterStopsCellEditing = false;
    /**
     * Specifies the zoom policy. Default is ZOOM_POLICY_PAGE. The zoom policy
     * does only apply if pageVisible is true.
     */
    protected int zoomPolicy = ZOOM_POLICY_PAGE;
    /**
     * Specifies the factor used for zoomIn and zoomOut. Default is 1.2 (120%).
     */
    protected double zoomFactor = 1.2;
    /**
     * Specifies if the viewport should automatically contain the selection
     * cells after a zoom operation. Default is false.
     */
    protected boolean keepSelectionVisibleOnZoom = false;
    /**
     * Specifies if the zoom operations should go into the center of the actual
     * diagram rather than going from top, left. Default is true.
     */
    protected boolean centerZoom = true;
    /**
     * Specifies if an image buffer should be used for painting the component.
     * Default is false.
     */
    protected boolean tripleBuffered = false;
    /**
     * Maps from cells to lists of heavyweights.
     */
    protected HashMap<ICell, Component[]> components = new HashMap<>();
    /**
     * Maps from cells to lists of overlays.
     */
    protected HashMap<ICell, ICellOverlay[]> overlays = new HashMap<>();
    /**
     * Updates the heavyweight component structure after any changes.
     */
    protected IEventListener<?> updateHandler = (sender, evt) -> {
        updateComponents();
        graphControl.updatePreferredSize();
    };
    protected PropertyChangeListener viewChangeHandler = evt -> {
        if (evt.getPropertyName().equals("view")) {
            GraphView oldView = (GraphView) evt.getOldValue();
            GraphView newView = (GraphView) evt.getNewValue();
            if (oldView != null) {
                oldView.removeListener(updateHandler);
            }
            if (newView != null) {
                newView.addListener(ScaleEvent.class, (IEventListener<ScaleEvent>) updateHandler);
                newView.addListener(TranslateEvent.class, (IEventListener<TranslateEvent>) updateHandler);
                newView.addListener(ScaleAndTranslateEvent.class, (IEventListener<ScaleAndTranslateEvent>) updateHandler);
                newView.addListener(UpEvent.class, (IEventListener<UpEvent>) updateHandler);
                newView.addListener(DownEvent.class, (IEventListener<DownEvent>) updateHandler);
            }
        } else if (evt.getPropertyName().equals("model")) {
            GraphModel oldModel = (GraphModel) evt.getOldValue();
            GraphModel newModel = (GraphModel) evt.getNewValue();
            if (oldModel != null) {
                oldModel.removeListener(updateHandler);
            }
            if (newModel != null) {
                newModel.addListener(ChangeEvent.class, (IEventListener<ChangeEvent>) updateHandler);
            }
        }
    };
    protected IEventListener<RepaintEvent> repaintHandler = (sender, evt) -> {
        RectangleDouble dirty = evt.getRegion();
        java.awt.Rectangle rect = (dirty != null) ? dirty.getRectangle() : null;
        if (rect != null) {
            rect.grow(1, 1);
        }
        // Updates the triple buffer
        repaintTripleBuffer(rect);
        // Repaints the control using the optional triple buffer
        graphControl.repaint((rect != null) ? rect : getViewport().getViewRect());
        // ----------------------------------------------------------
        // Shows the dirty region as a red rectangle (for debugging)
        JPanel panel = (JPanel) getClientProperty("dirty");
        if (showDirtyRectangle) {
            if (panel == null) {
                panel = new JPanel();
                panel.setOpaque(false);
                panel.setBorder(BorderFactory.createLineBorder(Color.RED));
                putClientProperty("dirty", panel);
                graphControl.add(panel);
            }
            if (dirty != null) {
                panel.setBounds(dirty.getRectangle());
            }
            panel.setVisible(dirty != null);
        } else if (panel != null && panel.getParent() != null) {
            panel.getParent().remove(panel);
            putClientProperty("dirty", null);
            repaint();
        }
        // ----------------------------------------------------------
    };
    /**
     * Internal flag to not reset zoomPolicy when zoom was set automatically.
     */
    private transient boolean zooming = false;
    /**
     * Resets the zoom policy if the scale is changed manually.
     */
    protected IEventListener<?> scaleHandler = (sender, evt) -> {
        if (!zooming) {
            zoomPolicy = ZOOM_POLICY_NONE;
        }
    };
    /**
     * Boolean flag to disable centering after the first time.
     */
    private transient boolean centerOnResize = true;

    /**
     *
     */
    public GraphComponent(Graph graph) {
        setCellEditor(createCellEditor());
        canvas = createCanvas();
        // Initializes the buffered view and
        graphControl = createGraphControl();
        installFocusHandler();
        installKeyHandler();
        installResizeHandler();
        setGraph(graph);
        // Adds the viewport view and initializes handlers
        setViewportView(graphControl);
        createHandlers();
        installDoubleClickHandler();
    }

    /**
     * installs a handler to set the focus to the container.
     */
    protected void installFocusHandler() {
        graphControl.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (!hasFocus()) {
                    requestFocus();
                }
            }
        });
    }

    /**
     * Handles escape keystrokes.
     */
    protected void installKeyHandler() {
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && isEscapeEnabled()) {
                    escape(e);
                }
            }
        });
    }

    /**
     * Applies the zoom policy if the size of the component changes.
     */
    protected void installResizeHandler() {
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                zoomAndCenter();
            }
        });
    }

    /**
     * Adds handling of edit and stop-edit events after all other handlers have
     * been installed.
     */
    protected void installDoubleClickHandler() {
        graphControl.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    if (!e.isConsumed() && isEditEvent(e)) {
                        ICell cell = getCellAt(e.getX(), e.getY(), false);
                        if (cell != null && getGraph().isCellEditable(cell)) {
                            startEditingAtCell(cell, e);
                        }
                    } else {
                        // Other languages use focus traversal here, in Java
                        // we explicitely stop editing after a click elsewhere
                        stopEditing(!invokesStopCellEditing);
                    }
                }
            }
        });
    }

    protected ICellEditor createCellEditor() {
        return new CellEditor(this);
    }

    /**
     * @return Returns the object that contains the graph.
     */
    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph value) {
        Graph oldValue = graph;
        // Uninstalls listeners for existing graph
        if (graph != null) {
            graph.removeListener(repaintHandler);
            graph.getModel().removeListener(updateHandler);
            graph.getView().removeListener(updateHandler);
            graph.removePropertyChangeListener(viewChangeHandler);
            graph.getView().removeListener(scaleHandler);
        }
        graph = value;
        // Updates the buffer if the model changes
        graph.addListener(RepaintEvent.class, repaintHandler);
        // Installs the update handler to sync the overlays and controls
        graph.getModel().addListener(ChangeEvent.class, (IEventListener<ChangeEvent>) updateHandler);
        // Repaint after the following events is handled via
        // Graph.repaint-events
        // The respective handlers are installed in Graph.setView
        GraphView view = graph.getView();
        view.addListener(ScaleEvent.class, (IEventListener<ScaleEvent>) updateHandler);
        view.addListener(TranslateEvent.class, (IEventListener<TranslateEvent>) updateHandler);
        view.addListener(ScaleAndTranslateEvent.class, (IEventListener<ScaleAndTranslateEvent>) updateHandler);
        view.addListener(UpEvent.class, (IEventListener<UpEvent>) updateHandler);
        view.addListener(DownEvent.class, (IEventListener<DownEvent>) updateHandler);
        graph.addPropertyChangeListener(viewChangeHandler);
        // Resets the zoom policy if the scale changes
        graph.getView().addListener(ScaleEvent.class, (IEventListener<ScaleEvent>) scaleHandler);
        graph.getView().addListener(ScaleAndTranslateEvent.class, (IEventListener<ScaleAndTranslateEvent>) scaleHandler);
        // Invoke the update handler once for initial state
        updateHandler.invoke(graph.getView(), null);
        firePropertyChange("graph", oldValue, graph);
    }

    /**
     * Creates the inner control that handles tooltips, preferred size and can
     * draw cells onto a canvas.
     */
    protected GraphControl createGraphControl() {
        return new GraphControl(this);
    }

    /**
     * @return Returns the control that renders the graph.
     */
    public GraphControl getGraphControl() {
        return graphControl;
    }

    /**
     * Creates the connection-, panning and graphhandler (in this order).
     */
    protected void createHandlers() {
        setTransferHandler(createTransferHandler());
        panningHandler = createPanningHandler();
        selectionCellsHandler = createSelectionCellsHandler();
        connectionHandler = createConnectionHandler();
        graphHandler = createGraphHandler();
    }

    protected TransferHandler createTransferHandler() {
        return new GraphTransferHandler();
    }

    protected SelectionCellsHandler createSelectionCellsHandler() {
        return new SelectionCellsHandler(this);
    }

    protected GraphHandler createGraphHandler() {
        return new GraphHandler(this);
    }

    public SelectionCellsHandler getSelectionCellsHandler() {
        return selectionCellsHandler;
    }

    public GraphHandler getGraphHandler() {
        return graphHandler;
    }

    protected ConnectionHandler createConnectionHandler() {
        return new ConnectionHandler(this);
    }

    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    protected PanningHandler createPanningHandler() {
        return new PanningHandler(this);
    }

    public PanningHandler getPanningHandler() {
        return panningHandler;
    }

    public boolean isEditing() {
        return getCellEditor().getEditingCell() != null;
    }

    public ICellEditor getCellEditor() {
        return cellEditor;
    }

    public void setCellEditor(ICellEditor value) {
        ICellEditor oldValue = cellEditor;
        cellEditor = value;
        firePropertyChange("cellEditor", oldValue, cellEditor);
    }

    /**
     * @return the tolerance
     */
    public int getTolerance() {
        return tolerance;
    }

    /**
     * @param value the tolerance to set
     */
    public void setTolerance(int value) {
        int oldValue = tolerance;
        tolerance = value;
        firePropertyChange("tolerance", oldValue, tolerance);
    }

    public PageFormat getPageFormat() {
        return pageFormat;
    }

    public void setPageFormat(PageFormat value) {
        PageFormat oldValue = pageFormat;
        pageFormat = value;
        firePropertyChange("pageFormat", oldValue, pageFormat);
    }

    public double getPageScale() {
        return pageScale;
    }

    public void setPageScale(double value) {
        double oldValue = pageScale;
        pageScale = value;
        firePropertyChange("pageScale", oldValue, pageScale);
    }

    /**
     * Returns the size of the area that layouts can operate in.
     */
    public RectangleDouble getLayoutAreaSize() {
        if (pageVisible) {
            Dimension d = getPreferredSizeForPage();
            return new RectangleDouble(new java.awt.Rectangle(d));
        } else {
            return new RectangleDouble(new java.awt.Rectangle(graphControl.getSize()));
        }
    }

    public ImageIcon getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(ImageIcon value) {
        ImageIcon oldValue = backgroundImage;
        backgroundImage = value;
        firePropertyChange("backgroundImage", oldValue, backgroundImage);
    }

    /**
     * @return the pageVisible
     */
    public boolean isPageVisible() {
        return pageVisible;
    }

    /**
     * Fires a property change event for <code>pageVisible</code>. zoomAndCenter
     * should be called if this is set to true.
     *
     * @param value the pageVisible to set
     */
    public void setPageVisible(boolean value) {
        boolean oldValue = pageVisible;
        pageVisible = value;
        firePropertyChange("pageVisible", oldValue, pageVisible);
    }

    /**
     * @return the preferPageSize
     */
    public boolean isPreferPageSize() {
        return preferPageSize;
    }

    /**
     * Fires a property change event for <code>preferPageSize</code>.
     *
     * @param value the preferPageSize to set
     */
    public void setPreferPageSize(boolean value) {
        boolean oldValue = preferPageSize;
        preferPageSize = value;
        firePropertyChange("preferPageSize", oldValue, preferPageSize);
    }

    /**
     * @return the pageBreaksVisible
     */
    public boolean isPageBreaksVisible() {
        return pageBreaksVisible;
    }

    /**
     * @param value the pageBreaksVisible to set
     */
    public void setPageBreaksVisible(boolean value) {
        boolean oldValue = pageBreaksVisible;
        pageBreaksVisible = value;
        firePropertyChange("pageBreaksVisible", oldValue, pageBreaksVisible);
    }

    /**
     * @return the pageBreakColor
     */
    public Color getPageBreakColor() {
        return pageBreakColor;
    }

    /**
     * @param pageBreakColor the pageBreakColor to set
     */
    public void setPageBreakColor(Color pageBreakColor) {
        this.pageBreakColor = pageBreakColor;
    }

    public int getHorizontalPageCount() {
        return horizontalPageCount;
    }

    /**
     * @param value the horizontalPageCount to set
     */
    public void setHorizontalPageCount(int value) {
        int oldValue = horizontalPageCount;
        horizontalPageCount = value;
        firePropertyChange("horizontalPageCount", oldValue, horizontalPageCount);
    }

    public int getVerticalPageCount() {
        return verticalPageCount;
    }

    /**
     * @param value the verticalPageCount to set
     */
    public void setVerticalPageCount(int value) {
        int oldValue = verticalPageCount;
        verticalPageCount = value;
        firePropertyChange("verticalPageCount", oldValue, verticalPageCount);
    }

    /**
     * @return the centerPage
     */
    public boolean isCenterPage() {
        return centerPage;
    }

    /**
     * zoomAndCenter should be called if this is set to true.
     *
     * @param value the centerPage to set
     */
    public void setCenterPage(boolean value) {
        boolean oldValue = centerPage;
        centerPage = value;
        firePropertyChange("centerPage", oldValue, centerPage);
    }

    /**
     * @return the pageBackgroundColor
     */
    public Color getPageBackgroundColor() {
        return pageBackgroundColor;
    }

    /**
     * Sets the color that appears behind the page.
     *
     * @param value the pageBackgroundColor to set
     */
    public void setPageBackgroundColor(Color value) {
        Color oldValue = pageBackgroundColor;
        pageBackgroundColor = value;
        firePropertyChange("pageBackgroundColor", oldValue, pageBackgroundColor);
    }

    /**
     * @return the pageShadowColor
     */
    public Color getPageShadowColor() {
        return pageShadowColor;
    }

    /**
     * @param value the pageShadowColor to set
     */
    public void setPageShadowColor(Color value) {
        Color oldValue = pageShadowColor;
        pageShadowColor = value;
        firePropertyChange("pageShadowColor", oldValue, pageShadowColor);
    }

    /**
     * @return the pageShadowColor
     */
    public Color getPageBorderColor() {
        return pageBorderColor;
    }

    /**
     * @param value the pageBorderColor to set
     */
    public void setPageBorderColor(Color value) {
        Color oldValue = pageBorderColor;
        pageBorderColor = value;
        firePropertyChange("pageBorderColor", oldValue, pageBorderColor);
    }

    /**
     * @return the keepSelectionVisibleOnZoom
     */
    public boolean isKeepSelectionVisibleOnZoom() {
        return keepSelectionVisibleOnZoom;
    }

    /**
     * @param value the keepSelectionVisibleOnZoom to set
     */
    public void setKeepSelectionVisibleOnZoom(boolean value) {
        boolean oldValue = keepSelectionVisibleOnZoom;
        keepSelectionVisibleOnZoom = value;
        firePropertyChange("keepSelectionVisibleOnZoom", oldValue, keepSelectionVisibleOnZoom);
    }

    /**
     * @return the zoomFactor
     */
    public double getZoomFactor() {
        return zoomFactor;
    }

    /**
     * @param value the zoomFactor to set
     */
    public void setZoomFactor(double value) {
        double oldValue = zoomFactor;
        zoomFactor = value;
        firePropertyChange("zoomFactor", oldValue, zoomFactor);
    }

    /**
     * @return the centerZoom
     */
    public boolean isCenterZoom() {
        return centerZoom;
    }

    /**
     * @param value the centerZoom to set
     */
    public void setCenterZoom(boolean value) {
        boolean oldValue = centerZoom;
        centerZoom = value;
        firePropertyChange("centerZoom", oldValue, centerZoom);
    }

    public int getZoomPolicy() {
        return zoomPolicy;
    }

    public void setZoomPolicy(int value) {
        int oldValue = zoomPolicy;
        zoomPolicy = value;
        if (zoomPolicy != ZOOM_POLICY_NONE) {
            zoom(zoomPolicy == ZOOM_POLICY_PAGE, true);
        }
        firePropertyChange("zoomPolicy", oldValue, zoomPolicy);
    }

    /**
     * Callback to process an escape keystroke.
     */
    public void escape(KeyEvent e) {
        if (selectionCellsHandler != null) {
            selectionCellsHandler.reset();
        }
        if (connectionHandler != null) {
            connectionHandler.reset();
        }
        if (graphHandler != null) {
            graphHandler.reset();
        }
        if (cellEditor != null) {
            cellEditor.stopEditing(true);
        }
    }

    /**
     * Clones and inserts the given cells into the graph using the move method
     * and returns the inserted cells. This shortcut is used if cells are
     * inserted via data transfer.
     */
    public List<ICell> importCells(List<ICell> cells, double dx, double dy, ICell target, java.awt.Point location) {
        return graph.moveCells(cells, dx, dy, true, target, location);
    }

    /**
     * Refreshes the display and handles.
     */
    public void refresh() {
        graph.refresh();
        selectionCellsHandler.refresh();
    }

    /**
     * Returns an Point representing the given event in the unscaled,
     * non-translated coordinate space and applies the grid.
     */
    public PointDouble getPointForEvent(MouseEvent e) {
        return getPointForEvent(e, true);
    }

    /**
     * Returns an Point representing the given event in the unscaled,
     * non-translated coordinate space and applies the grid.
     */
    public PointDouble getPointForEvent(MouseEvent e, boolean addOffset) {
        double s = graph.getView().getScale();
        PointDouble tr = graph.getView().getTranslate();
        double off = (addOffset) ? graph.getGridSize() / 2 : 0;
        double x = graph.snap(e.getX() / s - tr.getX() - off);
        double y = graph.snap(e.getY() / s - tr.getY() - off);
        return new PointDouble(x, y);
    }

    public void startEditing() {
        startEditingAtCell(null);
    }

    public void startEditingAtCell(ICell cell) {
        startEditingAtCell(cell, null);
    }

    public void startEditingAtCell(ICell cell, MouseEvent evt) {
        if (cell == null) {
            cell = graph.getSelectionCell();
            if (cell != null && !graph.isCellEditable(cell)) {
                cell = null;
            }
        }
        if (cell != null) {
            eventSource.fireEvent(new StartEditingEvent(cell, evt));
            cellEditor.startEditing(cell, evt);
        }
    }

    public String getEditingValue(ICell cell, java.util.EventObject trigger) {
        return graph.convertValueToString(cell);
    }

    public void stopEditing(boolean cancel) {
        cellEditor.stopEditing(cancel);
    }

    /**
     * Sets the label of the specified cell to the given value using
     * Graph.cellLabelChanged and fires Event.LABEL_CHANGED while the
     * transaction is in progress. Returns the cell whose label was changed.
     *
     * @param cell  Cell whose label should be changed.
     * @param value New value of the label.
     * @param evt   Optional event that triggered the change.
     */
    public ICell labelChanged(ICell cell, Object value, java.util.EventObject evt) {
        IGraphModel model = graph.getModel();
        model.beginUpdate();
        try {
            graph.cellLabelChanged(cell, value, graph.isAutoSizeCell(cell));
            eventSource.fireEvent(new LabelChangedEvent(cell, value, evt));
        } finally {
            model.endUpdate();
        }
        return cell;
    }

    /**
     * Returns the (unscaled) preferred size for the current page format (scaled
     * by pageScale).
     */
    protected Dimension getPreferredSizeForPage() {
        return new Dimension((int) Math.round(pageFormat.getWidth() * pageScale * horizontalPageCount), (int) Math.round(pageFormat.getHeight() * pageScale * verticalPageCount));
    }

    /**
     * Returns the vertical border between the page and the control.
     */
    public int getVerticalPageBorder() {
        return (int) Math.round(pageFormat.getWidth() * pageScale);
    }

    /**
     * Returns the horizontal border between the page and the control.
     */
    public int getHorizontalPageBorder() {
        return (int) Math.round(0.5 * pageFormat.getHeight() * pageScale);
    }

    /**
     * Returns the scaled preferred size for the current graph.
     */
    protected Dimension getScaledPreferredSizeForGraph() {
        RectangleDouble bounds = graph.getGraphBounds();
        int border = graph.getBorder();
        return new Dimension((int) Math.round(bounds.getX() + bounds.getWidth()) + border + 1, (int) Math.round(bounds.getY() + bounds.getHeight()) + border + 1);
    }

    /**
     * Should be called by a hook inside GraphView/Graph
     */
    protected PointDouble getPageTranslate(double scale) {
        Dimension d = getPreferredSizeForPage();
        Dimension bd = new Dimension(d);
        if (!preferPageSize) {
            bd.width += 2 * getHorizontalPageBorder();
            bd.height += 2 * getVerticalPageBorder();
        }
        double width = Math.max(bd.width, (getViewport().getWidth() - 8) / scale);
        double height = Math.max(bd.height, (getViewport().getHeight() - 8) / scale);
        double dx = Math.max(0, (width - d.width) / 2);
        double dy = Math.max(0, (height - d.height) / 2);
        return new PointDouble(dx, dy);
    }

    /**
     * Invoked after the component was resized to update the zoom if the zoom
     * policy is not none and/or update the translation of the diagram if
     * pageVisible and centerPage are true.
     */
    public void zoomAndCenter() {
        if (zoomPolicy != ZOOM_POLICY_NONE) {
            // Centers only on the initial zoom call
            zoom(zoomPolicy == ZOOM_POLICY_PAGE, centerOnResize || zoomPolicy == ZOOM_POLICY_PAGE);
            centerOnResize = false;
        } else if (pageVisible && centerPage) {
            PointDouble translate = getPageTranslate(graph.getView().getScale());
            graph.getView().setTranslate(translate);
        } else {
            getGraphControl().updatePreferredSize();
        }
    }

    /**
     * Zooms into the graph by zoomFactor.
     */
    public void zoomIn() {
        zoom(zoomFactor);
    }

    /**
     * Function: zoomOut
     * <p>
     * Zooms out of the graph by <zoomFactor>.
     */
    public void zoomOut() {
        zoom(1 / zoomFactor);
    }

    public void zoom(double factor) {
        GraphView view = graph.getView();
        double newScale = (double) ((int) (view.getScale() * 100 * factor)) / 100;
        if (newScale != view.getScale() && newScale > 0.04) {
            PointDouble translate = (pageVisible && centerPage) ? getPageTranslate(newScale) : new PointDouble();
            graph.getView().scaleAndTranslate(newScale, translate.getX(), translate.getY());
            if (keepSelectionVisibleOnZoom && !graph.isSelectionEmpty()) {
                getGraphControl().scrollRectToVisible(view.getBoundingBox(graph.getSelectionCells()).getRectangle());
            } else {
                maintainScrollBar(true, factor, centerZoom);
                maintainScrollBar(false, factor, centerZoom);
            }
        }
    }

    public void zoomTo(final double newScale, final boolean center) {
        GraphView view = graph.getView();
        final double scale = view.getScale();
        PointDouble translate = (pageVisible && centerPage) ? getPageTranslate(newScale) : new PointDouble();
        graph.getView().scaleAndTranslate(newScale, translate.getX(), translate.getY());
        // Causes two repaints on the scrollpane, namely one for the scale
        // change with the new preferred size and one for the change of
        // the scrollbar position. The latter cannot be done immediately
        // because the scrollbar keeps the value <= max - extent, and if
        // max is changed the value change will trigger a syncScrollPane
        // WithViewport in BasicScrollPaneUI, which will update the value
        // for the previous maximum (ie. it must be invoked later).
        SwingUtilities.invokeLater(() -> {
            maintainScrollBar(true, newScale / scale, center);
            maintainScrollBar(false, newScale / scale, center);
        });
    }

    /**
     * Function: zoomActual
     * <p>
     * Resets the zoom and panning in the view.
     */
    public void zoomActual() {
        PointDouble translate = (pageVisible && centerPage) ? getPageTranslate(1) : new PointDouble();
        graph.getView().scaleAndTranslate(1, translate.getX(), translate.getY());
        if (isPageVisible()) {
            // Causes two repaints, see zoomTo for more details
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Dimension pageSize = getPreferredSizeForPage();
                    if (getViewport().getWidth() > pageSize.getWidth()) {
                        scrollToCenter(true);
                    } else {
                        JScrollBar scrollBar = getHorizontalScrollBar();
                        if (scrollBar != null) {
                            scrollBar.setValue((scrollBar.getMaximum() / 3) - 4);
                        }
                    }
                    if (getViewport().getHeight() > pageSize.getHeight()) {
                        scrollToCenter(false);
                    } else {
                        JScrollBar scrollBar = getVerticalScrollBar();
                        if (scrollBar != null) {
                            scrollBar.setValue((scrollBar.getMaximum() / 4) - 4);
                        }
                    }
                }
            });
        }
    }

    public void zoom(final boolean page, final boolean center) {
        if (pageVisible && !zooming) {
            zooming = true;
            try {
                int off = (getPageShadowColor() != null) ? 8 : 0;
                // Adds some extra space for the shadow and border
                double width = getViewport().getWidth() - off;
                double height = getViewport().getHeight() - off;
                Dimension d = getPreferredSizeForPage();
                double pageWidth = d.width;
                double pageHeight = d.height;
                double scaleX = width / pageWidth;
                double scaleY = (page) ? height / pageHeight : scaleX;
                // Rounds the new scale to 5% steps
                final double newScale = (double) ((int) (Math.min(scaleX, scaleY) * 20)) / 20;
                if (newScale > 0) {
                    GraphView graphView = graph.getView();
                    final double scale = graphView.getScale();
                    PointDouble translate = (centerPage) ? getPageTranslate(newScale) : new PointDouble();
                    graphView.scaleAndTranslate(newScale, translate.getX(), translate.getY());
                    // Causes two repaints, see zoomTo for more details
                    final double factor = newScale / scale;
                    SwingUtilities.invokeLater(() -> {
                        if (center) {
                            if (page) {
                                scrollToCenter(true);
                                scrollToCenter(false);
                            } else {
                                scrollToCenter(true);
                                maintainScrollBar(false, factor, false);
                            }
                        } else if (factor != 1) {
                            maintainScrollBar(true, factor, false);
                            maintainScrollBar(false, factor, false);
                        }
                    });
                }
            } finally {
                zooming = false;
            }
        }
    }

    protected void maintainScrollBar(boolean horizontal, double factor, boolean center) {
        JScrollBar scrollBar = (horizontal) ? getHorizontalScrollBar() : getVerticalScrollBar();
        if (scrollBar != null) {
            BoundedRangeModel model = scrollBar.getModel();
            int newValue = (int) Math.round(model.getValue() * factor) + (int) Math.round((center) ? (model.getExtent() * (factor - 1) / 2) : 0);
            model.setValue(newValue);
        }
    }

    public void scrollToCenter(boolean horizontal) {
        JScrollBar scrollBar = (horizontal) ? getHorizontalScrollBar() : getVerticalScrollBar();
        if (scrollBar != null) {
            final BoundedRangeModel model = scrollBar.getModel();
            final int newValue = ((model.getMaximum()) / 2) - model.getExtent() / 2;
            model.setValue(newValue);
        }
    }

    /**
     * Scrolls the graph so that it shows the given cell.
     */
    public void scrollCellToVisible(ICell cell) {
        scrollCellToVisible(cell, false);
    }

    /**
     * Scrolls the graph so that it shows the given cell.
     */
    public void scrollCellToVisible(ICell cell, boolean center) {
        CellState state = graph.getView().getState(cell);
        if (state != null) {
            RectangleDouble bounds = state;
            if (center) {
                bounds = bounds.clone();
                bounds.setX(bounds.getCenterX() - getWidth() / 2);
                bounds.setWidth(getWidth());
                bounds.setY(bounds.getCenterY() - getHeight() / 2);
                bounds.setHeight(getHeight());
            }
            getGraphControl().scrollRectToVisible(bounds.getRectangle());
        }
    }

    /**
     * @return Returns the cell at the given location.
     */
    public ICell getCellAt(int x, int y) {
        return getCellAt(x, y, true);
    }

    /**
     * @return Returns the cell at the given location.
     */
    public ICell getCellAt(int x, int y, boolean hitSwimlaneContent) {
        return getCellAt(x, y, hitSwimlaneContent, null);
    }

    /**
     * Returns the bottom-most cell that intersects the given point (x, y) in
     * the cell hierarchy starting at the given parent.
     *
     * @param x      X-coordinate of the location to be checked.
     * @param y      Y-coordinate of the location to be checked.
     * @param parent <Cell> that should be used as the root of the recursion.
     *               Default is <defaultParent>.
     * @return Returns the child at the given location.
     */
    public ICell getCellAt(int x, int y, boolean hitSwimlaneContent, ICell parent) {
        if (parent == null) {
            parent = graph.getDefaultParent();
        }
        if (parent != null) {
            PointDouble previousTranslate = canvas.getTranslate();
            double previousScale = canvas.getScale();
            try {
                canvas.setScale(graph.getView().getScale());
                canvas.setTranslate(0, 0);
                IGraphModel model = graph.getModel();
                GraphView view = graph.getView();
                java.awt.Rectangle hit = new java.awt.Rectangle(x, y, 1, 1);
                int childCount = model.getChildCount(parent);
                for (int i = childCount - 1; i >= 0; i--) {
                    ICell cell = model.getChildAt(parent, i);
                    ICell result = getCellAt(x, y, hitSwimlaneContent, cell);
                    if (result != null) {
                        return result;
                    } else if (graph.isCellVisible(cell)) {
                        CellState state = view.getState(cell);
                        if (state != null && canvas.intersects(this, hit, state) && (!graph.isSwimlane(cell) || hitSwimlaneContent || (transparentSwimlaneContent && !canvas.hitSwimlaneContent(this, state, x, y)))) {
                            return cell;
                        }
                    }
                }
            } finally {
                canvas.setScale(previousScale);
                canvas.setTranslate((int) previousTranslate.getX(), (int) previousTranslate.getY());
            }
        }
        return null;
    }

    public boolean isSwimlaneSelectionEnabled() {
        return swimlaneSelectionEnabled;
    }

    public void setSwimlaneSelectionEnabled(boolean value) {
        boolean oldValue = swimlaneSelectionEnabled;
        swimlaneSelectionEnabled = value;
        firePropertyChange("swimlaneSelectionEnabled", oldValue, swimlaneSelectionEnabled);
    }

    public List<ICell> selectRegion(java.awt.Rectangle rect, MouseEvent e) {
        List<ICell> cells = getCells(rect);
        if (!cells.isEmpty()) {
            selectCellsForEvent(cells, e);
        } else if (!graph.isSelectionEmpty() && !e.isConsumed()) {
            graph.clearSelection();
        }
        return cells;
    }

    /**
     * Returns the cells inside the given rectangle.
     *
     * @return Returns the cells inside the given rectangle.
     */
    public List<ICell> getCells(java.awt.Rectangle rect) {
        return getCells(rect, null);
    }

    /**
     * Returns the children of the given parent that are contained in the given
     * rectangle (x, y, width, height). The result is added to the optional
     * result array, which is returned from the function. If no result array is
     * specified then a new array is created and returned.
     *
     * @return Returns the children inside the given rectangle.
     */
    public List<ICell> getCells(java.awt.Rectangle rect, ICell parent) {
        List<ICell> result = new ArrayList<>();
        if (rect.width > 0 || rect.height > 0) {
            if (parent == null) {
                parent = graph.getDefaultParent();
            }
            if (parent != null) {
                PointDouble previousTranslate = canvas.getTranslate();
                double previousScale = canvas.getScale();
                try {
                    canvas.setScale(graph.getView().getScale());
                    canvas.setTranslate(0, 0);
                    IGraphModel model = graph.getModel();
                    GraphView view = graph.getView();
                    int childCount = model.getChildCount(parent);
                    for (int i = 0; i < childCount; i++) {
                        ICell cell = model.getChildAt(parent, i);
                        CellState state = view.getState(cell);
                        if (graph.isCellVisible(cell) && state != null) {
                            if (canvas.contains(this, rect, state)) {
                                result.add(cell);
                            } else {
                                result.addAll(getCells(rect, cell));
                            }
                        }
                    }
                } finally {
                    canvas.setScale(previousScale);
                    canvas.setTranslate(previousTranslate.getX(), previousTranslate.getY());
                }
            }
        }
        return result;
    }

    /**
     * Selects the cells for the given event.
     */
    public void selectCellsForEvent(List<ICell> cells, MouseEvent event) {
        if (isToggleEvent(event)) {
            graph.addSelectionCells(cells);
        } else {
            graph.setSelectionCells(cells);
        }
    }

    /**
     * Selects the cell for the given event.
     */
    public void selectCellForEvent(ICell cell, MouseEvent e) {
        boolean isSelected = graph.isCellSelected(cell);
        if (isToggleEvent(e)) {
            if (isSelected) {
                graph.removeSelectionCell(cell);
            } else {
                graph.addSelectionCell(cell);
            }
        } else if (!isSelected || graph.getSelectionCount() != 1) {
            graph.setSelectionCell(cell);
        }
    }

    /**
     * Returns true if the absolute value of one of the given parameters is
     * greater than the tolerance.
     */
    public boolean isSignificant(double dx, double dy) {
        return Math.abs(dx) > tolerance || Math.abs(dy) > tolerance;
    }

    /**
     * Returns the icon used to display the collapsed state of the specified
     * cell state. This returns null for all edges.
     */
    public ImageIcon getFoldingIcon(CellState state) {
        if (state != null && isFoldingEnabled() && !getGraph().getModel().isEdge(state.getCell())) {
            ICell cell = state.getCell();
            boolean tmp = graph.isCellCollapsed(cell);
            if (graph.isCellFoldable(cell, !tmp)) {
                return (tmp) ? collapsedIcon : expandedIcon;
            }
        }
        return null;
    }

    public java.awt.Rectangle getFoldingIconBounds(CellState state, ImageIcon icon) {
        IGraphModel model = graph.getModel();
        boolean isEdge = model.isEdge(state.getCell());
        double scale = getGraph().getView().getScale();
        int x = (int) Math.round(state.getX() + 4 * scale);
        int y = (int) Math.round(state.getY() + 4 * scale);
        int w = (int) Math.max(8, icon.getIconWidth() * scale);
        int h = (int) Math.max(8, icon.getIconHeight() * scale);
        if (isEdge) {
            PointDouble pt = graph.getView().getPoint(state);
            x = (int) pt.getX() - w / 2;
            y = (int) pt.getY() - h / 2;
        }
        return new java.awt.Rectangle(x, y, w, h);
    }

    public boolean hitFoldingIcon(ICell cell, int x, int y) {
        if (cell != null) {
            IGraphModel model = graph.getModel();
            // Draws the collapse/expand icons
            boolean isEdge = model.isEdge(cell);
            if (foldingEnabled && (model.isVertex(cell) || isEdge)) {
                CellState state = graph.getView().getState(cell);
                if (state != null) {
                    ImageIcon icon = getFoldingIcon(state);
                    if (icon != null) {
                        return getFoldingIconBounds(state, icon).contains(x, y);
                    }
                }
            }
        }
        return false;
    }

    /**
     *
     */
    public void setToolTips(boolean enabled) {
        if (enabled) {
            ToolTipManager.sharedInstance().registerComponent(graphControl);
        } else {
            ToolTipManager.sharedInstance().unregisterComponent(graphControl);
        }
    }

    public boolean isConnectable() {
        return connectionHandler.isEnabled();
    }

    public void setConnectable(boolean connectable) {
        connectionHandler.setEnabled(connectable);
    }

    public boolean isPanning() {
        return panningHandler.isEnabled();
    }

    public void setPanning(boolean enabled) {
        panningHandler.setEnabled(enabled);
    }

    /**
     * @return the autoScroll
     */
    public boolean shouldAutoScroll() {
        return autoScroll;
    }

    /**
     * @param value the autoScroll to set
     */
    public void setAutoScroll(boolean value) {
        autoScroll = value;
    }

    /**
     * @return the autoExtend
     */
    public boolean isAutoExtend() {
        return autoExtend;
    }

    /**
     * @param value the autoExtend to set
     */
    public void setAutoExtend(boolean value) {
        autoExtend = value;
    }

    /**
     * @return the escapeEnabled
     */
    public boolean isEscapeEnabled() {
        return escapeEnabled;
    }

    /**
     * @param value the escapeEnabled to set
     */
    public void setEscapeEnabled(boolean value) {
        boolean oldValue = escapeEnabled;
        escapeEnabled = value;
        firePropertyChange("escapeEnabled", oldValue, escapeEnabled);
    }

    /**
     * @return the escapeEnabled
     */
    public boolean isInvokesStopCellEditing() {
        return invokesStopCellEditing;
    }

    /**
     * @param value the invokesStopCellEditing to set
     */
    public void setInvokesStopCellEditing(boolean value) {
        boolean oldValue = invokesStopCellEditing;
        invokesStopCellEditing = value;
        firePropertyChange("invokesStopCellEditing", oldValue, invokesStopCellEditing);
    }

    /**
     * @return the enterStopsCellEditing
     */
    public boolean isEnterStopsCellEditing() {
        return enterStopsCellEditing;
    }

    /**
     * @param value the enterStopsCellEditing to set
     */
    public void setEnterStopsCellEditing(boolean value) {
        boolean oldValue = enterStopsCellEditing;
        enterStopsCellEditing = value;
        firePropertyChange("enterStopsCellEditing", oldValue, enterStopsCellEditing);
    }

    /**
     * @return the dragEnabled
     */
    public boolean isDragEnabled() {
        return dragEnabled;
    }

    /**
     * @param value the dragEnabled to set
     */
    public void setDragEnabled(boolean value) {
        boolean oldValue = dragEnabled;
        dragEnabled = value;
        firePropertyChange("dragEnabled", oldValue, dragEnabled);
    }

    /**
     * @return the gridVisible
     */
    public boolean isGridVisible() {
        return gridVisible;
    }

    /**
     * Fires a property change event for <code>gridVisible</code>.
     *
     * @param value the gridVisible to set
     */
    public void setGridVisible(boolean value) {
        boolean oldValue = gridVisible;
        gridVisible = value;
        firePropertyChange("gridVisible", oldValue, gridVisible);
    }

    /**
     * @return the gridVisible
     */
    public boolean isAntiAlias() {
        return antiAlias;
    }

    /**
     * Fires a property change event for <code>antiAlias</code>.
     *
     * @param value the antiAlias to set
     */
    public void setAntiAlias(boolean value) {
        boolean oldValue = antiAlias;
        antiAlias = value;
        firePropertyChange("antiAlias", oldValue, antiAlias);
    }

    /**
     * @return the gridVisible
     */
    public boolean isTextAntiAlias() {
        return antiAlias;
    }

    /**
     * Fires a property change event for <code>textAntiAlias</code>.
     *
     * @param value the textAntiAlias to set
     */
    public void setTextAntiAlias(boolean value) {
        boolean oldValue = textAntiAlias;
        textAntiAlias = value;
        firePropertyChange("textAntiAlias", oldValue, textAntiAlias);
    }

    public float getPreviewAlpha() {
        return previewAlpha;
    }

    public void setPreviewAlpha(float value) {
        float oldValue = previewAlpha;
        previewAlpha = value;
        firePropertyChange("previewAlpha", oldValue, previewAlpha);
    }

    /**
     * @return the tripleBuffered
     */
    public boolean isTripleBuffered() {
        return tripleBuffered;
    }

    /**
     * @param value the tripleBuffered to set
     */
    public void setTripleBuffered(boolean value) {
        boolean oldValue = tripleBuffered;
        tripleBuffered = value;
        firePropertyChange("tripleBuffered", oldValue, tripleBuffered);
    }

    /**
     * Hook for dynamic triple buffering condition.
     */
    public boolean isForceTripleBuffered() {
        // LATER: Dynamic condition (cell density) to use triple
        // buffering for a large number of cells on a small rect
        return false;
    }

    /**
     * @return the gridColor
     */
    public Color getGridColor() {
        return gridColor;
    }

    /**
     * Fires a property change event for <code>gridColor</code>.
     *
     * @param value the gridColor to set
     */
    public void setGridColor(Color value) {
        Color oldValue = gridColor;
        gridColor = value;
        firePropertyChange("gridColor", oldValue, gridColor);
    }

    /**
     * @return the gridStyle
     */
    public int getGridStyle() {
        return gridStyle;
    }

    /**
     * Fires a property change event for <code>gridStyle</code>.
     *
     * @param value the gridStyle to set
     */
    public void setGridStyle(int value) {
        int oldValue = gridStyle;
        gridStyle = value;
        firePropertyChange("gridStyle", oldValue, gridStyle);
    }

    /**
     * Returns importEnabled.
     */
    public boolean isImportEnabled() {
        return importEnabled;
    }

    /**
     * Sets importEnabled.
     */
    public void setImportEnabled(boolean value) {
        boolean oldValue = importEnabled;
        importEnabled = value;
        firePropertyChange("importEnabled", oldValue, importEnabled);
    }

    /**
     * Returns all cells which may be imported via datatransfer.
     */
    public List<ICell> getImportableCells(List<ICell> cells) {
        return GraphModel.filterCells(cells, this::canImportCell);
    }

    /**
     * Returns true if the given cell can be imported via datatransfer. This
     * returns importEnabled.
     */
    public boolean canImportCell(ICell cell) {
        return isImportEnabled();
    }

    /**
     * @return the exportEnabled
     */
    public boolean isExportEnabled() {
        return exportEnabled;
    }

    /**
     * @param value the exportEnabled to set
     */
    public void setExportEnabled(boolean value) {
        boolean oldValue = exportEnabled;
        exportEnabled = value;
        firePropertyChange("exportEnabled", oldValue, exportEnabled);
    }

    /**
     * Returns all cells which may be exported via datatransfer.
     */
    public List<ICell> getExportableCells(List<ICell> cells) {
        return GraphModel.filterCells(cells, this::canExportCell);
    }

    /**
     * Returns true if the given cell can be exported via datatransfer.
     */
    public boolean canExportCell(ICell cell) {
        return isExportEnabled();
    }

    /**
     * @return the foldingEnabled
     */
    public boolean isFoldingEnabled() {
        return foldingEnabled;
    }

    /**
     * @param value the foldingEnabled to set
     */
    public void setFoldingEnabled(boolean value) {
        boolean oldValue = foldingEnabled;
        foldingEnabled = value;
        firePropertyChange("foldingEnabled", oldValue, foldingEnabled);
    }

    public boolean isEditEvent(MouseEvent e) {
        return e != null && e.getClickCount() == 2;
    }

    /**
     * @return Returns true if the given event should toggle selected cells.
     */
    public boolean isCloneEvent(MouseEvent event) {
        return event != null && event.isControlDown();
    }

    /**
     * @return Returns true if the given event should toggle selected cells.
     */
    public boolean isToggleEvent(MouseEvent event) {
        // NOTE: IsMetaDown always returns true for right-clicks on the Mac, so
        // toggle selection for left mouse buttons requires CMD key to be pressed,
        // but toggle for right mouse buttons requires CTRL to be pressed.
        return event != null && ((Utils.IS_MAC) ? ((SwingUtilities.isLeftMouseButton(event) && event.isMetaDown()) || (SwingUtilities.isRightMouseButton(event) && event.isControlDown())) : event.isControlDown());
    }

    /**
     * @return Returns true if the given event allows the grid to be applied.
     */
    public boolean isGridEnabledEvent(MouseEvent event) {
        return event != null && !event.isAltDown();
    }

    /**
     * Note: This is not used during drag and drop operations due to limitations
     * of the underlying API. To enable this for move operations set dragEnabled
     * to false.
     *
     * @return Returns true if the given event is a panning event.
     */
    public boolean isPanningEvent(MouseEvent event) {
        return event != null && event.isShiftDown() && event.isControlDown();
    }

    /**
     * Note: This is not used during drag and drop operations due to limitations
     * of the underlying API. To enable this for move operations set dragEnabled
     * to false.
     *
     * @return Returns true if the given event is constrained.
     */
    public boolean isConstrainedEvent(MouseEvent event) {
        return event != null && event.isShiftDown();
    }

    /**
     * Note: This is not used during drag and drop operations due to limitations
     * of the underlying API. To enable this for move operations set dragEnabled
     * to false.
     *
     * @return Returns true if the given event is constrained.
     */
    public boolean isForceMarqueeEvent(MouseEvent event) {
        return event != null && event.isAltDown();
    }

    public PointDouble snapScaledPoint(PointDouble pt) {
        return snapScaledPoint(pt, 0, 0);
    }

    public PointDouble snapScaledPoint(PointDouble pt, double dx, double dy) {
        if (pt != null) {
            double scale = graph.getView().getScale();
            PointDouble trans = graph.getView().getTranslate();
            pt.setX((graph.snap(pt.getX() / scale - trans.getX() + dx / scale) + trans.getX()) * scale - dx);
            pt.setY((graph.snap(pt.getY() / scale - trans.getY() + dy / scale) + trans.getY()) * scale - dy);
        }
        return pt;
    }

    /**
     * Prints the specified page on the specified graphics using
     * <code>pageFormat</code> for the page format.
     *
     * @param g           The graphics to paint the graph on.
     * @param printFormat The page format to use for printing.
     * @param page        The page to print
     * @return Returns {@link Printable#PAGE_EXISTS} or
     * {@link Printable#NO_SUCH_PAGE}.
     */
    public int print(Graphics g, PageFormat printFormat, int page) {
        int result = NO_SUCH_PAGE;
        // Disables double-buffering before printing
        RepaintManager currentManager = RepaintManager.currentManager(GraphComponent.this);
        currentManager.setDoubleBufferingEnabled(false);
        // Gets the current state of the view
        GraphView view = graph.getView();
        // Stores the old state of the view
        boolean eventsEnabled = view.isEventsEnabled();
        PointDouble translate = view.getTranslate();
        // Disables firing of scale events so that there is no
        // repaint or update of the original graph while pages
        // are being printed
        view.setEventsEnabled(false);
        // Uses the view to create temporary cell states for each cell
        TemporaryCellStates tempStates = new TemporaryCellStates(view, 1 / pageScale);
        try {
            view.setTranslate(new PointDouble(0, 0));
            Graphics2DCanvas canvas = createCanvas();
            canvas.setGraphics((Graphics2D) g);
            canvas.setScale(1 / pageScale);
            view.revalidate();
            RectangleDouble graphBounds = graph.getGraphBounds();
            Dimension pSize = new Dimension((int) Math.ceil(graphBounds.getX() + graphBounds.getWidth()) + 1, (int) Math.ceil(graphBounds.getY() + graphBounds.getHeight()) + 1);
            int w = (int) (printFormat.getImageableWidth());
            int h = (int) (printFormat.getImageableHeight());
            int cols = (int) Math.max(Math.ceil((double) (pSize.width - 5) / (double) w), 1);
            int rows = (int) Math.max(Math.ceil((double) (pSize.height - 5) / (double) h), 1);
            if (page < cols * rows) {
                int dx = (int) ((page % cols) * printFormat.getImageableWidth());
                int dy = (int) (Math.floor(page / cols) * printFormat.getImageableHeight());
                g.translate(-dx + (int) printFormat.getImageableX(), -dy + (int) printFormat.getImageableY());
                g.setClip(dx, dy, (int) (dx + printFormat.getWidth()), (int) (dy + printFormat.getHeight()));
                graph.drawGraph(canvas);
                result = PAGE_EXISTS;
            }
        } finally {
            view.setTranslate(translate);
            tempStates.destroy();
            view.setEventsEnabled(eventsEnabled);
            // Enables double-buffering after printing
            currentManager.setDoubleBufferingEnabled(true);
        }
        return result;
    }

    public InteractiveCanvas getCanvas() {
        return canvas;
    }

    public BufferedImage getTripleBuffer() {
        return tripleBuffer;
    }

    /**
     * Hook for subclassers to replace the graphics canvas for rendering and and
     * printing. This must be overridden to return a custom canvas if there are
     * any custom shapes.
     */
    public InteractiveCanvas createCanvas() {
        // NOTE: http://forum.jgraph.com/questions/3354/ reports that we should not
        // pass image observer here as it will cause JVM to enter infinite loop.
        return new InteractiveCanvas();
    }

    /**
     * @param state Cell state for which a handler should be created.
     * @return Returns the handler to be used for the given cell state.
     */
    public CellHandler createHandler(CellState state) {
        if (graph.getModel().isVertex(state.getCell())) {
            return new VertexHandler(this, state);
        } else if (graph.getModel().isEdge(state.getCell())) {
            EdgeStyleFunction style = graph.getView().getEdgeStyle(state, null, null, null);
            if (graph.isLoop(state) || style instanceof ElbowConnectorEdgeStyleFunction || style instanceof SideToSideEdgeStyleFunction || style instanceof TopToBottomEdgeStyleFunction) {
                return new ElbowEdgeHandler(this, state);
            }
            return new EdgeHandler(this, state);
        }
        return new CellHandler(this, state);
    }
    //
    // Heavyweights
    //

    /**
     * Hook for subclassers to create the array of heavyweights for the given
     * state.
     */
    public Component[] createComponents(CellState state) {
        return null;
    }

    public void insertComponent(CellState state, Component c) {
        getGraphControl().add(c, 0);
    }

    public void removeComponent(Component c, Object cell) {
        if (c.getParent() != null) {
            c.getParent().remove(c);
        }
    }

    public void updateComponent(CellState state, Component c) {
        int x = (int) state.getX();
        int y = (int) state.getY();
        int width = (int) state.getWidth();
        int height = (int) state.getHeight();
        Dimension s = c.getMinimumSize();
        if (s.width > width) {
            x -= (s.width - width) / 2;
            width = s.width;
        }
        if (s.height > height) {
            y -= (s.height - height) / 2;
            height = s.height;
        }
        c.setBounds(x, y, width, height);
    }

    public void updateComponents() {
        ICell root = graph.getModel().getRoot();
        HashMap<ICell, Component[]> result = updateComponents(root);
        // Components now contains the mappings which are no
        // longer used, the result contains the new mappings
        removeAllComponents(components);
        components = result;
        if (!overlays.isEmpty()) {
            HashMap<ICell, ICellOverlay[]> result2 = updateCellOverlays(root);
            // Overlays now contains the mappings from cells which
            // are no longer in the model, the result contains the
            // mappings from cells which still exists, regardless
            // from whether a state exists for a particular cell
            removeAllOverlays(overlays);
            overlays = result2;
        }
    }

    public void removeAllComponents(HashMap<ICell, Component[]> map) {
        for (Map.Entry<ICell, Component[]> entry : map.entrySet()) {
            Component[] c = entry.getValue();
            for (Component component : c) {
                removeComponent(component, entry.getKey());
            }
        }
    }

    public void removeAllOverlays(HashMap<ICell, ICellOverlay[]> map) {
        for (Map.Entry<ICell, ICellOverlay[]> entry : map.entrySet()) {
            ICellOverlay[] c = entry.getValue();
            for (ICellOverlay iCellOverlay : c) {
                removeCellOverlayComponent(iCellOverlay, entry.getKey());
            }
        }
    }

    public HashMap<ICell, Component[]> updateComponents(ICell cell) {
        HashMap<ICell, Component[]> result = new HashMap<>();
        Component[] c = components.remove(cell);
        CellState state = getGraph().getView().getState(cell);
        if (state != null) {
            if (c == null) {
                c = createComponents(state);
                if (c != null) {
                    for (Component component : c) {
                        insertComponent(state, component);
                    }
                }
            }
            if (c != null) {
                result.put(cell, c);
                for (Component component : c) {
                    updateComponent(state, component);
                }
            }
        }
        // Puts the component back into the map so that it will be removed
        else if (c != null) {
            components.put(cell, c);
        }
        int childCount = getGraph().getModel().getChildCount(cell);
        for (int i = 0; i < childCount; i++) {
            result.putAll(updateComponents(getGraph().getModel().getChildAt(cell, i)));
        }
        return result;
    }
    //
    // Validation and overlays
    //

    /**
     * Validates the graph by validating each descendant of the given cell or
     * the root of the model. Context is an object that contains the validation
     * state for the complete validation run. The validation errors are attached
     * to their cells using <setWarning>. This function returns true if no
     * validation errors exist in the graph.
     */
    public String validateGraph() {
        return validateGraph(graph.getModel().getRoot(), new HashMap<>());
    }

    /**
     * Validates the graph by validating each descendant of the given cell or
     * the root of the model. Context is an object that contains the validation
     * state for the complete validation run. The validation errors are attached
     * to their cells using <setWarning>. This function returns true if no
     * validation errors exist in the graph.
     *
     * @param cell    Cell to start the validation recursion.
     * @param context Object that represents the global validation state.
     */
    public String validateGraph(ICell cell, HashMap<Object, Object> context) {
        IGraphModel model = graph.getModel();
        GraphView view = graph.getView();
        boolean isValid = true;
        int childCount = model.getChildCount(cell);
        for (int i = 0; i < childCount; i++) {
            ICell tmp = model.getChildAt(cell, i);
            HashMap<Object, Object> ctx = context;
            if (graph.isValidRoot(tmp)) {
                ctx = new HashMap<>();
            }
            String warn = validateGraph(tmp, ctx);
            if (warn != null) {
                String html = warn.replaceAll("\n", "<br>");
                int len = html.length();
                setCellWarning(tmp, html.substring(0, Math.max(0, len - 4)));
            } else {
                setCellWarning(tmp, null);
            }
            isValid = isValid && warn == null;
        }
        StringBuilder warning = new StringBuilder();
        // Adds error for invalid children if collapsed (children invisible)
        if (graph.isCellCollapsed(cell) && !isValid) {
            warning.append(Resources.get("containsValidationErrors", "Contains Validation Errors")).append("\n");
        }
        // Checks edges and cells using the defined multiplicities
        if (model.isEdge(cell)) {
            String tmp = graph.getEdgeValidationError(cell, model.getTerminal(cell, true), model.getTerminal(cell, false));
            if (tmp != null) {
                warning.append(tmp);
            }
        } else {
            String tmp = graph.getCellValidationError(cell);
            if (tmp != null) {
                warning.append(tmp);
            }
        }
        // Checks custom validation rules
        String err = graph.validateCell(cell, context);
        if (err != null) {
            warning.append(err);
        }
        // Updates the display with the warning icons before any potential
        // alerts are displayed
        if (model.getParent(cell) == null) {
            view.validate();
        }
        return (warning.length() > 0 || !isValid) ? warning.toString() : null;
    }

    /**
     * Adds an overlay for the specified cell. This method fires an addoverlay
     * event and returns the new overlay.
     *
     * @param cell    Cell to add the overlay for.
     * @param overlay Overlay to be added for the cell.
     */
    public ICellOverlay addCellOverlay(ICell cell, ICellOverlay overlay) {
        ICellOverlay[] arr = getCellOverlays(cell);
        if (arr == null) {
            arr = new ICellOverlay[]{overlay};
        } else {
            ICellOverlay[] arr2 = new ICellOverlay[arr.length + 1];
            System.arraycopy(arr, 0, arr2, 0, arr.length);
            arr2[arr.length] = overlay;
            arr = arr2;
        }
        overlays.put(cell, arr);
        CellState state = graph.getView().getState(cell);
        if (state != null) {
            updateCellOverlayComponent(state, overlay);
        }
        eventSource.fireEvent(new AddOverlayEvent(cell, overlay));
        return overlay;
    }

    /**
     * Returns the array of overlays for the given cell or null, if no overlays
     * are defined.
     *
     * @param cell Cell whose overlays should be returned.
     */
    public ICellOverlay[] getCellOverlays(Object cell) {
        return overlays.get(cell);
    }

    /**
     * Removes and returns the given overlay from the given cell. This method
     * fires a remove overlay event. If no overlay is given, then all overlays
     * are removed using removeOverlays.
     *
     * @param cell    Cell whose overlay should be removed.
     * @param overlay Optional overlay to be removed.
     */
    public ICellOverlay removeCellOverlay(ICell cell, ICellOverlay overlay) {
        if (overlay == null) {
            removeCellOverlays(cell);
        } else {
            ICellOverlay[] arr = getCellOverlays(cell);
            if (arr != null) {
                // TODO: Use arraycopy from/to same array to speed this up
                List<ICellOverlay> list = new ArrayList<>(Arrays.asList(arr));
                if (list.remove(overlay)) {
                    removeCellOverlayComponent(overlay, cell);
                }
                arr = list.toArray(new ICellOverlay[0]);
                overlays.put(cell, arr);
            }
        }
        return overlay;
    }

    /**
     * Removes all overlays from the given cell. This method fires a
     * removeoverlay event for each removed overlay and returns the array of
     * overlays that was removed from the cell.
     *
     * @param cell Cell whose overlays should be removed.
     */
    public ICellOverlay[] removeCellOverlays(ICell cell) {
        ICellOverlay[] ovls = overlays.remove(cell);
        if (ovls != null) {
            // Removes the overlays from the cell hierarchy
            for (ICellOverlay ovl : ovls) {
                removeCellOverlayComponent(ovl, cell);
            }
        }
        return ovls;
    }

    /**
     * Notified when an overlay has been removed from the graph. This
     * implementation removes the given overlay from its parent if it is a
     * component inside a component hierarchy.
     */
    protected void removeCellOverlayComponent(ICellOverlay overlay, ICell cell) {
        if (overlay instanceof Component) {
            Component comp = (Component) overlay;
            if (comp.getParent() != null) {
                comp.setVisible(false);
                comp.getParent().remove(comp);
                eventSource.fireEvent(new RemoveOverlayEvent(cell, overlay));
            }
        }
    }

    /**
     * Notified when an overlay has been removed from the graph. This
     * implementation removes the given overlay from its parent if it is a
     * component inside a component hierarchy.
     */
    protected void updateCellOverlayComponent(CellState state, ICellOverlay overlay) {
        if (overlay instanceof Component) {
            Component comp = (Component) overlay;
            if (comp.getParent() == null) {
                getGraphControl().add(comp, 0);
            }
            RectangleDouble rect = overlay.getBounds(state);
            if (rect != null) {
                comp.setBounds(rect.getRectangle());
                comp.setVisible(true);
            } else {
                comp.setVisible(false);
            }
        }
    }

    /**
     * Removes all overlays in the graph.
     */
    public void clearCellOverlays() {
        clearCellOverlays(null);
    }

    /**
     * Removes all overlays in the graph for the given cell and all its
     * descendants. If no cell is specified then all overlays are removed from
     * the graph. This implementation uses removeOverlays to remove the overlays
     * from the individual cells.
     *
     * @param cell Optional cell that represents the root of the subtree to
     *             remove the overlays from. Default is the root in the model.
     */
    public void clearCellOverlays(ICell cell) {
        IGraphModel model = graph.getModel();
        if (cell == null) {
            cell = model.getRoot();
        }
        removeCellOverlays(cell);
        // Recursively removes all overlays from the children
        int childCount = model.getChildCount(cell);
        for (int i = 0; i < childCount; i++) {
            ICell child = model.getChildAt(cell, i);
            clearCellOverlays(child); // recurse
        }
    }

    /**
     * Creates an overlay for the given cell using the warning and image or
     * warningImage and returns the new overlay. If the warning is null or a
     * zero length string, then all overlays are removed from the cell instead.
     *
     * @param cell    Cell whose warning should be set.
     * @param warning String that represents the warning to be displayed.
     */
    public ICellOverlay setCellWarning(ICell cell, String warning) {
        return setCellWarning(cell, warning, null, false);
    }

    /**
     * Creates an overlay for the given cell using the warning and image or
     * warningImage and returns the new overlay. If the warning is null or a
     * zero length string, then all overlays are removed from the cell instead.
     *
     * @param cell    Cell whose warning should be set.
     * @param warning String that represents the warning to be displayed.
     * @param icon    Optional image to be used for the overlay. Default is
     *                warningImageBasename.
     */
    public ICellOverlay setCellWarning(ICell cell, String warning, ImageIcon icon) {
        return setCellWarning(cell, warning, icon, false);
    }

    /**
     * Creates an overlay for the given cell using the warning and image or
     * warningImage and returns the new overlay. If the warning is null or a
     * zero length string, then all overlays are removed from the cell instead.
     *
     * @param cell    Cell whose warning should be set.
     * @param warning String that represents the warning to be displayed.
     * @param icon    Optional image to be used for the overlay. Default is
     *                warningImageBasename.
     * @param select  Optional boolean indicating if a click on the overlay should
     *                select the corresponding cell. Default is false.
     */
    public ICellOverlay setCellWarning(final ICell cell, String warning, ImageIcon icon, boolean select) {
        if (warning != null && warning.length() > 0) {
            icon = (icon != null) ? icon : warningIcon;
            // Creates the overlay with the image and warning
            CellOverlay overlay = new CellOverlay(icon, warning);
            // Adds a handler for single mouseclicks to select the cell
            if (select) {
                overlay.addMouseListener(new MouseAdapter() {
                    /**
                     * Selects the associated cell in the graph
                     */
                    public void mousePressed(MouseEvent e) {
                        if (getGraph().isEnabled()) {
                            getGraph().setSelectionCell(cell);
                        }
                    }
                });
                overlay.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            // Sets and returns the overlay in the graph
            return addCellOverlay(cell, overlay);
        } else {
            removeCellOverlays(cell);
        }
        return null;
    }

    /**
     * Returns a hashtable with all entries from the overlays variable where a
     * cell still exists in the model. The entries are removed from the global
     * hashtable so that the remaining entries reflect those whose cell have
     * been removed from the model. If no state is available for a given cell
     * then its overlays are temporarly removed from the rendering control, but
     * kept in the result.
     */
    public HashMap<ICell, ICellOverlay[]> updateCellOverlays(ICell cell) {
        HashMap<ICell, ICellOverlay[]> result = new HashMap<>();
        ICellOverlay[] c = overlays.remove(cell);
        CellState state = getGraph().getView().getState(cell);
        if (c != null) {
            if (state != null) {
                for (ICellOverlay iCellOverlay : c) {
                    updateCellOverlayComponent(state, iCellOverlay);
                }
            } else {
                for (ICellOverlay iCellOverlay : c) {
                    removeCellOverlayComponent(iCellOverlay, cell);
                }
            }
            result.put(cell, c);
        }
        int childCount = getGraph().getModel().getChildCount(cell);
        for (int i = 0; i < childCount; i++) {
            result.putAll(updateCellOverlays(getGraph().getModel().getChildAt(cell, i)));
        }
        return result;
    }

    protected void paintBackground(Graphics g) {
        java.awt.Rectangle clip = g.getClipBounds();
        java.awt.Rectangle rect = paintBackgroundPage(g);
        if (isPageVisible()) {
            g.clipRect(rect.x + 1, rect.y + 1, rect.width - 1, rect.height - 1);
        }
        // Paints the clipped background image
        paintBackgroundImage(g);
        // Paints the grid directly onto the graphics
        paintGrid(g);
        g.setClip(clip);
    }

    protected java.awt.Rectangle paintBackgroundPage(Graphics g) {
        PointDouble translate = graph.getView().getTranslate();
        double scale = graph.getView().getScale();
        int x0 = (int) Math.round(translate.getX() * scale) - 1;
        int y0 = (int) Math.round(translate.getY() * scale) - 1;
        Dimension d = getPreferredSizeForPage();
        int w = (int) Math.round(d.width * scale) + 2;
        int h = (int) Math.round(d.height * scale) + 2;
        if (isPageVisible()) {
            // Draws the background behind the page
            Color c = getPageBackgroundColor();
            if (c != null) {
                g.setColor(c);
                Utils.fillClippedRect(g, 0, 0, getGraphControl().getWidth(), getGraphControl().getHeight());
            }
            // Draws the page drop shadow
            c = getPageShadowColor();
            if (c != null) {
                g.setColor(c);
                Utils.fillClippedRect(g, x0 + w, y0 + 6, 6, h - 6);
                Utils.fillClippedRect(g, x0 + 8, y0 + h, w - 2, 6);
            }
            // Draws the page
            Color bg = getBackground();
            if (getViewport().isOpaque()) {
                bg = getViewport().getBackground();
            }
            g.setColor(bg);
            Utils.fillClippedRect(g, x0 + 1, y0 + 1, w, h);
            // Draws the page border
            c = getPageBorderColor();
            if (c != null) {
                g.setColor(c);
                g.drawRect(x0, y0, w, h);
            }
        }
        if (isPageBreaksVisible() && (horizontalPageCount > 1 || verticalPageCount > 1)) {
            // Draws the pagebreaks
            // TODO: Use clipping
            Graphics2D g2 = (Graphics2D) g;
            Stroke previousStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{1, 2}, 0));
            g2.setColor(pageBreakColor);
            for (int i = 1; i <= horizontalPageCount - 1; i++) {
                int dx = i * w / horizontalPageCount;
                g2.drawLine(x0 + dx, y0 + 1, x0 + dx, y0 + h);
            }
            for (int i = 1; i <= verticalPageCount - 1; i++) {
                int dy = i * h / verticalPageCount;
                g2.drawLine(x0 + 1, y0 + dy, x0 + w, y0 + dy);
            }
            // Restores the graphics
            g2.setStroke(previousStroke);
        }
        return new java.awt.Rectangle(x0, y0, w, h);
    }

    protected void paintBackgroundImage(Graphics g) {
        if (backgroundImage != null) {
            PointDouble translate = graph.getView().getTranslate();
            double scale = graph.getView().getScale();
            g.drawImage(backgroundImage.getImage(), (int) (translate.getX() * scale), (int) (translate.getY() * scale), (int) (backgroundImage.getIconWidth() * scale), (int) (backgroundImage.getIconHeight() * scale), this);
        }
    }

    /**
     * Paints the grid onto the given graphics object.
     */
    protected void paintGrid(Graphics g) {
        if (isGridVisible()) {
            g.setColor(getGridColor());
            java.awt.Rectangle clip = g.getClipBounds();
            if (clip == null) {
                clip = getGraphControl().getBounds();
            }
            double left = clip.getX();
            double top = clip.getY();
            double right = left + clip.getWidth();
            double bottom = top + clip.getHeight();
            // Double the grid line spacing if smaller than half the gridsize
            int style = getGridStyle();
            int gridSize = graph.getGridSize();
            int minStepping = gridSize;
            // Smaller stepping for certain styles
            if (style == GRID_STYLE_CROSS || style == GRID_STYLE_DOT) {
                minStepping /= 2;
            }
            // Fetches some global display state information
            PointDouble trans = graph.getView().getTranslate();
            double scale = graph.getView().getScale();
            double tx = trans.getX() * scale;
            double ty = trans.getY() * scale;
            // Sets the distance of the grid lines in pixels
            double stepping = gridSize * scale;
            if (stepping < minStepping) {
                int count = (int) Math.round(Math.ceil(minStepping / stepping) / 2) * 2;
                stepping = count * stepping;
            }
            double xs = Math.floor((left - tx) / stepping) * stepping + tx;
            double xe = Math.ceil(right / stepping) * stepping;
            double ys = Math.floor((top - ty) / stepping) * stepping + ty;
            double ye = Math.ceil(bottom / stepping) * stepping;
            switch (style) {
                case GRID_STYLE_CROSS: {
                    // Sets the dot size
                    int cs = (stepping > 16.0) ? 2 : 1;
                    for (double x = xs; x <= xe; x += stepping) {
                        for (double y = ys; y <= ye; y += stepping) {
                            // FIXME: Workaround for rounding errors when adding
                            // stepping to
                            // xs or ys multiple times (leads to double grid lines
                            // when zoom
                            // is set to eg. 121%)
                            x = Math.round((x - tx) / stepping) * stepping + tx;
                            y = Math.round((y - ty) / stepping) * stepping + ty;
                            int ix = (int) Math.round(x);
                            int iy = (int) Math.round(y);
                            g.drawLine(ix - cs, iy, ix + cs, iy);
                            g.drawLine(ix, iy - cs, ix, iy + cs);
                        }
                    }
                    break;
                }
                case GRID_STYLE_LINE: {
                    xe += (int) Math.ceil(stepping);
                    ye += (int) Math.ceil(stepping);
                    int ixs = (int) Math.round(xs);
                    int ixe = (int) Math.round(xe);
                    int iys = (int) Math.round(ys);
                    int iye = (int) Math.round(ye);
                    for (double x = xs; x <= xe; x += stepping) {
                        // FIXME: Workaround for rounding errors when adding
                        // stepping to
                        // xs or ys multiple times (leads to double grid lines when
                        // zoom
                        // is set to eg. 121%)
                        x = Math.round((x - tx) / stepping) * stepping + tx;
                        int ix = (int) Math.round(x);
                        g.drawLine(ix, iys, ix, iye);
                    }
                    for (double y = ys; y <= ye; y += stepping) {
                        // FIXME: Workaround for rounding errors when adding
                        // stepping to
                        // xs or ys multiple times (leads to double grid lines when
                        // zoom
                        // is set to eg. 121%)
                        y = Math.round((y - ty) / stepping) * stepping + ty;
                        int iy = (int) Math.round(y);
                        g.drawLine(ixs, iy, ixe, iy);
                    }
                    break;
                }
                case GRID_STYLE_DASHED: {
                    Graphics2D g2 = (Graphics2D) g;
                    Stroke stroke = g2.getStroke();
                    xe += (int) Math.ceil(stepping);
                    ye += (int) Math.ceil(stepping);
                    int ixs = (int) Math.round(xs);
                    int ixe = (int) Math.round(xe);
                    int iys = (int) Math.round(ys);
                    int iye = (int) Math.round(ye);
                    // Creates a set of strokes with individual dash offsets
                    // for each direction
                    Stroke[] strokes = new Stroke[]{new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{3, 1}, Math.max(0, iys) % 4), new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{2, 2}, Math.max(0, iys) % 4), new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{1, 1}, 0), new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{2, 2}, Math.max(0, iys) % 4)};
                    for (double x = xs; x <= xe; x += stepping) {
                        g2.setStroke(strokes[((int) (x / stepping)) % strokes.length]);
                        // FIXME: Workaround for rounding errors when adding
                        // stepping to
                        // xs or ys multiple times (leads to double grid lines when
                        // zoom
                        // is set to eg. 121%)
                        double xx = Math.round((x - tx) / stepping) * stepping + tx;
                        int ix = (int) Math.round(xx);
                        g.drawLine(ix, iys, ix, iye);
                    }
                    strokes = new Stroke[]{new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{3, 1}, Math.max(0, ixs) % 4), new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{2, 2}, Math.max(0, ixs) % 4), new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{1, 1}, 0), new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{2, 2}, Math.max(0, ixs) % 4)};
                    for (double y = ys; y <= ye; y += stepping) {
                        g2.setStroke(strokes[((int) (y / stepping)) % strokes.length]);
                        // FIXME: Workaround for rounding errors when adding
                        // stepping to
                        // xs or ys multiple times (leads to double grid lines when
                        // zoom
                        // is set to eg. 121%)
                        double yy = Math.round((y - ty) / stepping) * stepping + ty;
                        int iy = (int) Math.round(yy);
                        g.drawLine(ixs, iy, ixe, iy);
                    }
                    g2.setStroke(stroke);
                    break;
                }
                default: // DOT_GRID_MODE
                {
                    for (double x = xs; x <= xe; x += stepping) {
                        for (double y = ys; y <= ye; y += stepping) {
                            // FIXME: Workaround for rounding errors when adding
                            // stepping to
                            // xs or ys multiple times (leads to double grid lines
                            // when zoom
                            // is set to eg. 121%)
                            x = Math.round((x - tx) / stepping) * stepping + tx;
                            y = Math.round((y - ty) / stepping) * stepping + ty;
                            int ix = (int) Math.round(x);
                            int iy = (int) Math.round(y);
                            g.drawLine(ix, iy, ix, iy);
                        }
                    }
                }
            }
        }
    }
    //
    // Triple Buffering
    //

    /**
     * Updates the buffer (if one exists) and repaints the given cell state.
     */
    public void redraw(CellState state) {
        if (state != null) {
            java.awt.Rectangle dirty = state.getBoundingBox().getRectangle();
            repaintTripleBuffer(new java.awt.Rectangle(dirty));
            dirty = SwingUtilities.convertRectangle(graphControl, dirty, this);
            repaint(dirty);
        }
    }

    /**
     * Checks if the triple buffer exists and creates a new one if it does not.
     * Also compares the size of the buffer with the size of the graph and drops
     * the buffer if it has a different size.
     */
    public void checkTripleBuffer() {
        RectangleDouble bounds = graph.getGraphBounds();
        int width = (int) Math.ceil(bounds.getX() + bounds.getWidth() + 2);
        int height = (int) Math.ceil(bounds.getY() + bounds.getHeight() + 2);
        if (tripleBuffer != null) {
            if (tripleBuffer.getWidth() != width || tripleBuffer.getHeight() != height) {
                // Resizes the buffer (destroys existing and creates new)
                destroyTripleBuffer();
            }
        }
        if (tripleBuffer == null) {
            createTripleBuffer(width, height);
        }
    }

    /**
     * Creates the tripleBufferGraphics and tripleBuffer for the given dimension
     * and draws the complete graph onto the triplebuffer.
     */
    protected void createTripleBuffer(int width, int height) {
        try {
            tripleBuffer = Utils.createBufferedImage(width, height, null);
            tripleBufferGraphics = tripleBuffer.createGraphics();
            Utils.setAntiAlias(tripleBufferGraphics, antiAlias, textAntiAlias);
            // Repaints the complete buffer
            repaintTripleBuffer(null);
        } catch (OutOfMemoryError error) {
            log.log(Level.SEVERE, "Failed to create a triple buffer", error);
        }
    }

    /**
     * Destroys the tripleBuffer and tripleBufferGraphics objects.
     */
    public void destroyTripleBuffer() {
        if (tripleBuffer != null) {
            tripleBuffer = null;
            tripleBufferGraphics.dispose();
            tripleBufferGraphics = null;
        }
    }

    /**
     * Clears and repaints the triple buffer at the given rectangle or repaints
     * the complete buffer if no rectangle is specified.
     */
    public void repaintTripleBuffer(java.awt.Rectangle dirty) {
        if (tripleBuffered && tripleBufferGraphics != null) {
            if (dirty == null) {
                dirty = new java.awt.Rectangle(tripleBuffer.getWidth(), tripleBuffer.getHeight());
            }
            // Clears and repaints the dirty rectangle using the
            // graphics canvas as a renderer
            Utils.clearRect(tripleBufferGraphics, dirty, null);
            tripleBufferGraphics.setClip(dirty);
            graphControl.drawGraph(tripleBufferGraphics, true);
            tripleBufferGraphics.setClip(null);
        }
    }
    //
    // Redirected to event source
    //

    /**
     * @return Returns true if event dispatching is enabled in the event source.
     * @see EventSource#isEventsEnabled()
     */
    public boolean isEventsEnabled() {
        return eventSource.isEventsEnabled();
    }

    /**
     * @see EventSource#setEventsEnabled(boolean)
     */
    public void setEventsEnabled(boolean eventsEnabled) {
        eventSource.setEventsEnabled(eventsEnabled);
    }

    /**
     * @see EventSource#addListener(java.lang.Class,
     * IEventListener)
     */
    public <T extends EventObject> void addListener(Class<T> eventClass, IEventListener<T> listener) {
        eventSource.addListener(eventClass, listener);
    }

    /**
     * @param listener Listener instance.
     */
    public void removeListener(IEventListener<?> listener) {
        eventSource.removeListener(listener);
    }

    public static class MouseRedirector implements MouseListener, MouseMotionListener {
        protected GraphComponent graphComponent;

        public MouseRedirector(GraphComponent graphComponent) {
            this.graphComponent = graphComponent;
        }

        public void mouseClicked(MouseEvent e) {
            graphComponent.getGraphControl().dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, graphComponent.getGraphControl()));
        }

        public void mouseEntered(MouseEvent e) {
            // Redirecting this would cause problems on the Mac
            // and is technically incorrect anyway
        }

        public void mouseExited(MouseEvent e) {
            mouseClicked(e);
        }

        public void mousePressed(MouseEvent e) {
            mouseClicked(e);
        }

        public void mouseReleased(MouseEvent e) {
            mouseClicked(e);
        }

        public void mouseDragged(MouseEvent e) {
            mouseClicked(e);
        }

        public void mouseMoved(MouseEvent e) {
            mouseClicked(e);
        }
    }
}
