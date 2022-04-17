package com.faforever.neroxis.ngraph.swing;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.canvas.ICanvas;
import com.faforever.neroxis.ngraph.event.AfterPaintEvent;
import com.faforever.neroxis.ngraph.event.BeforePaintEvent;
import com.faforever.neroxis.ngraph.event.PaintEvent;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.util.Resources;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

public class GraphControl extends JComponent {
    @Serial
    private static final long serialVersionUID = -8916603170766739124L;
    private final GraphComponent graphComponent;
    /**
     * Specifies a translation for painting. This should only be used during
     * mouse drags and must be reset after any interactive repaints. Default
     * is (0,0). This should not be null.
     */
    protected java.awt.Point translate = new java.awt.Point(0, 0);

    public GraphControl(GraphComponent graphComponent) {
        this.graphComponent = graphComponent;
        addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (translate.x != 0 || translate.y != 0) {
                    translate = new java.awt.Point(0, 0);
                    repaint();
                }
            }
        });
    }

    /**
     * Returns the translate.
     */
    public java.awt.Point getTranslate() {
        return translate;
    }

    /**
     * Sets the translate.
     */
    public void setTranslate(java.awt.Point value) {
        translate = value;
    }

    public GraphComponent getGraphContainer() {
        return graphComponent;
    }

    /**
     * Overrides parent method to add extend flag for making the control
     * larger during previews.
     */
    public void scrollRectToVisible(java.awt.Rectangle aRect, boolean extend) {
        super.scrollRectToVisible(aRect);
        if (extend) {
            extendComponent(aRect);
        }
    }

    /**
     * Implements extension of the component in all directions. For
     * extension below the origin (into negative space) the translate will
     * temporaly be used and reset with the next mouse released event.
     */
    protected void extendComponent(java.awt.Rectangle rect) {
        int right = rect.x + rect.width;
        int bottom = rect.y + rect.height;
        Dimension d = new Dimension(getPreferredSize());
        Dimension sp = graphComponent.getScaledPreferredSizeForGraph();
        RectangleDouble min = graphComponent.graph.getMinimumGraphSize();
        double scale = graphComponent.graph.getView().getScale();
        boolean update = false;
        if (rect.x < 0) {
            translate.x = Math.max(translate.x, Math.max(0, -rect.x));
            d.width = sp.width;
            if (min != null) {
                d.width = (int) Math.max(d.width, Math.round(min.getWidth() * scale));
            }
            d.width += translate.x;
            update = true;
        } else if (right > getWidth()) {
            d.width = Math.max(right, getWidth());
            update = true;
        }
        if (rect.y < 0) {
            translate.y = Math.max(translate.y, Math.max(0, -rect.y));
            d.height = sp.height;
            if (min != null) {
                d.height = (int) Math.max(d.height, Math.round(min.getHeight() * scale));
            }
            d.height += translate.y;
            update = true;
        } else if (bottom > getHeight()) {
            d.height = Math.max(bottom, getHeight());
            update = true;
        }
        if (update) {
            setPreferredSize(d);
            setMinimumSize(d);
            revalidate();
        }
    }

    public String getToolTipText(MouseEvent e) {
        String tip = graphComponent.getSelectionCellsHandler().getToolTipText(e);
        if (tip == null) {
            ICell cell = graphComponent.getCellAt(e.getX(), e.getY());
            if (cell != null) {
                if (graphComponent.hitFoldingIcon(cell, e.getX(), e.getY())) {
                    tip = Resources.get("collapse-expand");
                } else {
                    tip = graphComponent.graph.getToolTipForCell(cell);
                }
            }
        }
        if (tip != null && tip.length() > 0) {
            return tip;
        }
        return super.getToolTipText(e);
    }

    /**
     * Updates the preferred size for the given scale if the page size
     * should be preferred or the page is visible.
     */
    public void updatePreferredSize() {
        double scale = graphComponent.graph.getView().getScale();
        Dimension d;
        if (graphComponent.preferPageSize || graphComponent.pageVisible) {
            Dimension page = graphComponent.getPreferredSizeForPage();
            if (!graphComponent.preferPageSize) {
                page.width += 2 * graphComponent.getHorizontalPageBorder();
                page.height += 2 * graphComponent.getVerticalPageBorder();
            }
            d = new Dimension((int) (page.width * scale), (int) (page.height * scale));
        } else {
            d = graphComponent.getScaledPreferredSizeForGraph();
        }
        RectangleDouble min = graphComponent.graph.getMinimumGraphSize();
        if (min != null) {
            d.width = (int) Math.max(d.width, Math.round(min.getWidth() * scale));
            d.height = (int) Math.max(d.height, Math.round(min.getHeight() * scale));
        }
        if (!getPreferredSize().equals(d)) {
            setPreferredSize(d);
            setMinimumSize(d);
            revalidate();
        }
    }

    public void paint(Graphics graphics) {
        graphics.translate(translate.x, translate.y);
        graphComponent.eventSource.fireEvent(new BeforePaintEvent(graphics));
        super.paint(graphics);
        graphComponent.eventSource.fireEvent(new AfterPaintEvent(graphics));
        graphics.translate(-translate.x, -translate.y);
    }

    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        // Draws the background
        graphComponent.paintBackground(graphics);
        // Creates or destroys the triple buffer as needed
        if (graphComponent.tripleBuffered) {
            graphComponent.checkTripleBuffer();
        } else if (graphComponent.tripleBuffer != null) {
            graphComponent.destroyTripleBuffer();
        }
        // Paints the buffer in the canvas onto the dirty region
        if (graphComponent.tripleBuffer != null) {
            Utils.drawImageClip(graphics, graphComponent.tripleBuffer, this);
        }
        // Paints the graph directly onto the graphics
        else {
            Graphics2D g2 = (Graphics2D) graphics;
            RenderingHints tmp = g2.getRenderingHints();
            // Sets the graphics in the canvas
            try {
                Utils.setAntiAlias(g2, graphComponent.antiAlias, graphComponent.textAntiAlias);
                drawGraph(g2, true);
            } finally {
                // Restores the graphics state
                g2.setRenderingHints(tmp);
            }
        }
        graphComponent.eventSource.fireEvent(new PaintEvent(graphics));
    }

    public void drawGraph(Graphics2D g, boolean drawLabels) {
        Graphics2D previousGraphics = graphComponent.canvas.getGraphics();
        boolean previousDrawLabels = graphComponent.canvas.isDrawLabels();
        PointDouble previousTranslate = graphComponent.canvas.getTranslate();
        double previousScale = graphComponent.canvas.getScale();
        try {
            graphComponent.canvas.setScale(graphComponent.graph.getView().getScale());
            graphComponent.canvas.setDrawLabels(drawLabels);
            graphComponent.canvas.setTranslate(0, 0);
            graphComponent.canvas.setGraphics(g);
            // Draws the graph using the graphics canvas
            drawFromRootCell();
        } finally {
            graphComponent.canvas.setScale(previousScale);
            graphComponent.canvas.setTranslate(previousTranslate.getX(), previousTranslate.getY());
            graphComponent.canvas.setDrawLabels(previousDrawLabels);
            graphComponent.canvas.setGraphics(previousGraphics);
        }
    }

    /**
     * Hook to draw the root cell into the canvas.
     */
    protected void drawFromRootCell() {
        drawCell(graphComponent.canvas, graphComponent.graph.getModel().getRoot());
    }

    protected boolean hitClip(Graphics2DCanvas canvas, CellState state) {
        java.awt.Rectangle rect = getExtendedCellBounds(state);
        return (rect == null || canvas.getGraphics().hitClip(rect.x, rect.y, rect.width, rect.height));
    }

    /**
     * @param state the cached state of the cell whose extended bounds are to be calculated
     * @return the bounds of the cell, including the label and shadow and allowing for rotation
     */
    protected java.awt.Rectangle getExtendedCellBounds(CellState state) {
        java.awt.Rectangle rect = null;
        // Takes rotation into account
        double rotation = state.getStyle().getShape().getRotation();
        RectangleDouble tmp = Utils.getBoundingBox(new RectangleDouble(state), rotation);
        // Adds scaled stroke width
        int border = (int) Math.ceil(state.getStyle().getShape().getStrokeWidth() * graphComponent.graph.getView().getScale()) + 1;
        tmp.grow(border);
        if (state.getStyle().getCellProperties().isShadow()) {
            tmp.setWidth(tmp.getWidth() + Constants.SHADOW_OFFSETX);
            tmp.setHeight(tmp.getHeight() + Constants.SHADOW_OFFSETX);
        }
        // Adds the bounds of the label
        if (state.getLabelBounds() != null) {
            tmp.add(state.getLabelBounds());
        }
        rect = tmp.getRectangle();
        return rect;
    }

    /**
     * Draws the given cell onto the specified canvas. This is a modified
     * version of Graph.drawCell which paints the label only if the
     * corresponding cell is not being edited and invokes the cellDrawn hook
     * after all descendants have been painted.
     *
     * @param canvas Canvas onto which the cell should be drawn.
     * @param cell   Cell that should be drawn onto the canvas.
     */
    public void drawCell(ICanvas canvas, ICell cell) {
        CellState state = graphComponent.graph.getView().getState(cell);
        if (state != null && isCellDisplayable(state.getCell()) && (!(canvas instanceof Graphics2DCanvas) || hitClip((Graphics2DCanvas) canvas, state))) {
            graphComponent.graph.drawState(canvas, state, cell != graphComponent.cellEditor.getEditingCell());
        }
        // Handles special ordering for edges (all in foreground
        // or background) or draws all children in order
        boolean edgesFirst = graphComponent.graph.isKeepEdgesInBackground();
        boolean edgesLast = graphComponent.graph.isKeepEdgesInForeground();
        if (edgesFirst) {
            drawChildren(cell, true, false);
        }
        drawChildren(cell, !edgesFirst && !edgesLast, true);
        if (edgesLast) {
            drawChildren(cell, true, false);
        }
        if (state != null) {
            cellDrawn(canvas, state);
        }
    }

    /**
     * Draws the child edges and/or all other children in the given cell
     * depending on the boolean arguments.
     */
    protected void drawChildren(ICell cell, boolean edges, boolean others) {
        IGraphModel model = graphComponent.graph.getModel();
        int childCount = model.getChildCount(cell);
        for (int i = 0; i < childCount; i++) {
            ICell child = model.getChildAt(cell, i);
            boolean isEdge = model.isEdge(child);
            if ((others && !isEdge) || (edges && isEdge)) {
                drawCell(graphComponent.canvas, model.getChildAt(cell, i));
            }
        }
    }

    protected void cellDrawn(ICanvas canvas, CellState state) {
        if (graphComponent.isFoldingEnabled() && canvas instanceof Graphics2DCanvas) {
            IGraphModel model = graphComponent.graph.getModel();
            Graphics2DCanvas g2c = (Graphics2DCanvas) canvas;
            Graphics2D g2 = g2c.getGraphics();
            // Draws the collapse/expand icons
            boolean isEdge = model.isEdge(state.getCell());
            if (state.getCell() != graphComponent.graph.getCurrentRoot() && (model.isVertex(state.getCell()) || isEdge)) {
                ImageIcon icon = graphComponent.getFoldingIcon(state);
                if (icon != null) {
                    java.awt.Rectangle bounds = graphComponent.getFoldingIconBounds(state, icon);
                    g2.drawImage(icon.getImage(), bounds.x, bounds.y, bounds.width, bounds.height, this);
                }
            }
        }
    }

    /**
     * Returns true if the given cell is not the current root or the root in
     * the model. This can be overridden to not render certain cells in the
     * graph display.
     */
    protected boolean isCellDisplayable(Object cell) {
        return cell != graphComponent.graph.getView().getCurrentRoot() && cell != graphComponent.graph.getModel().getRoot();
    }
}
