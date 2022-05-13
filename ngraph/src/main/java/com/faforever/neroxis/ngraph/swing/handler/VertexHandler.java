/**
 * Copyright (c) 2008-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.swing.handler;

import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.swing.util.SwingConstants;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class VertexHandler extends CellHandler {
    public static Cursor[] CURSORS = new Cursor[]{new Cursor(Cursor.NW_RESIZE_CURSOR), new Cursor(
            Cursor.N_RESIZE_CURSOR), new Cursor(Cursor.NE_RESIZE_CURSOR), new Cursor(
            Cursor.W_RESIZE_CURSOR), new Cursor(Cursor.E_RESIZE_CURSOR), new Cursor(
            Cursor.SW_RESIZE_CURSOR), new Cursor(Cursor.S_RESIZE_CURSOR), new Cursor(
            Cursor.SE_RESIZE_CURSOR), new Cursor(Cursor.MOVE_CURSOR)};
    /**
     * Workaround for alt-key-state not correct in mouseReleased.
     */
    protected transient boolean gridEnabledEvent = false;
    /**
     * Workaround for shift-key-state not correct in mouseReleased.
     */
    protected transient boolean constrainedEvent = false;

    public VertexHandler(GraphComponent graphComponent, CellState state) {
        super(graphComponent, state);
    }

    @Override
    protected Rectangle[] createHandles() {
        Rectangle[] h;
        if (graphComponent.getGraph().isCellResizable(getState().getCell())) {
            Rectangle bounds = getState().getRectangle();
            int half = Constants.HANDLE_SIZE / 2;
            int left = bounds.x - half;
            int top = bounds.y - half;
            int w2 = bounds.x + (bounds.width / 2) - half;
            int h2 = bounds.y + (bounds.height / 2) - half;
            int right = bounds.x + bounds.width - half;
            int bottom = bounds.y + bounds.height - half;
            h = new Rectangle[9];
            int s = Constants.HANDLE_SIZE;
            h[0] = new Rectangle(left, top, s, s);
            h[1] = new Rectangle(w2, top, s, s);
            h[2] = new Rectangle(right, top, s, s);
            h[3] = new Rectangle(left, h2, s, s);
            h[4] = new Rectangle(right, h2, s, s);
            h[5] = new Rectangle(left, bottom, s, s);
            h[6] = new Rectangle(w2, bottom, s, s);
            h[7] = new Rectangle(right, bottom, s, s);
        } else {
            h = new Rectangle[1];
        }
        int s = Constants.LABEL_HANDLE_SIZE;
        RectangleDouble bounds = state.getLabelBounds();
        h[h.length - 1] = new Rectangle((int) (bounds.getX() + bounds.getWidth() / 2 - s),
                                        (int) (bounds.getY() + bounds.getHeight() / 2 - s), 2 * s, 2 * s);

        return h;
    }

    @Override
    protected JComponent createPreview() {
        JPanel preview = new JPanel();
        preview.setBorder(SwingConstants.PREVIEW_BORDER);
        preview.setOpaque(false);
        preview.setVisible(false);

        return preview;
    }

    @Override
    protected Cursor getCursor(MouseEvent e, int index) {
        if (index >= 0 && index <= CURSORS.length) {
            return CURSORS[index];
        }

        return null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!e.isConsumed() && first != null) {
            gridEnabledEvent = graphComponent.isGridEnabledEvent(e);
            constrainedEvent = graphComponent.isConstrainedEvent(e);

            double dx = e.getX() - first.x;
            double dy = e.getY() - first.y;

            if (isLabel(index)) {
                PointDouble pt = new PointDouble(e.getPoint());

                if (gridEnabledEvent) {
                    pt = graphComponent.snapScaledPoint(pt);
                }

                int idx = (int) Math.round(pt.getX() - first.x);
                int idy = (int) Math.round(pt.getY() - first.y);

                if (constrainedEvent) {
                    if (Math.abs(idx) > Math.abs(idy)) {
                        idy = 0;
                    } else {
                        idx = 0;
                    }
                }
                Rectangle rect = state.getLabelBounds().getRectangle();
                rect.translate(idx, idy);
                preview.setBounds(rect);
            } else {
                Graph graph = graphComponent.getGraph();
                double scale = graph.getView().getScale();

                if (gridEnabledEvent) {
                    dx = graph.snap(dx / scale) * scale;
                    dy = graph.snap(dy / scale) * scale;
                }
                RectangleDouble bounds = union(getState(), dx, dy, index);
                bounds.setWidth(bounds.getWidth() + 1);
                bounds.setHeight(bounds.getHeight() + 1);
                preview.setBounds(bounds.getRectangle());
            }

            if (!preview.isVisible() && graphComponent.isSignificant(dx, dy)) {
                preview.setVisible(true);
            }

            e.consume();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!e.isConsumed() && first != null) {
            if (preview != null && preview.isVisible()) {
                if (isLabel(index)) {
                    moveLabel(e);
                } else {
                    resizeCell(e);
                }
            }

            e.consume();
        }

        super.mouseReleased(e);
    }

    @Override
    public void paint(Graphics g) {
        Rectangle bounds = getState().getRectangle();

        if (g.hitClip(bounds.x, bounds.y, bounds.width, bounds.height)) {
            Graphics2D g2 = (Graphics2D) g;

            Stroke stroke = g2.getStroke();
            g2.setStroke(getSelectionStroke());
            g.setColor(getSelectionColor());
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g2.setStroke(stroke);
        }

        super.paint(g);
    }

    @Override
    public Color getSelectionColor() {
        return SwingConstants.VERTEX_SELECTION_COLOR;
    }

    @Override
    public Stroke getSelectionStroke() {
        return SwingConstants.VERTEX_SELECTION_STROKE;
    }

    protected void moveLabel(MouseEvent e) {
        Graph graph = graphComponent.getGraph();
        Geometry geometry = graph.getModel().getGeometry(state.getCell());

        if (geometry != null) {
            double scale = graph.getView().getScale();
            PointDouble pt = new PointDouble(e.getPoint());

            if (gridEnabledEvent) {
                pt = graphComponent.snapScaledPoint(pt);
            }

            double dx = (pt.getX() - first.x) / scale;
            double dy = (pt.getY() - first.y) / scale;

            if (constrainedEvent) {
                if (Math.abs(dx) > Math.abs(dy)) {
                    dy = 0;
                } else {
                    dx = 0;
                }
            }
            PointDouble offset = geometry.getOffset();
            if (offset == null) {
                offset = new PointDouble();
            }
            dx += offset.getX();
            dy += offset.getY();
            geometry = geometry.clone();
            geometry.setOffset(new PointDouble(Math.round(dx), Math.round(dy)));
            graph.getModel().setGeometry(state.getCell(), geometry);
        }
    }

    protected void resizeCell(MouseEvent e) {
        Graph graph = graphComponent.getGraph();
        double scale = graph.getView().getScale();

        ICell cell = state.getCell();
        Geometry geometry = graph.getModel().getGeometry(cell);

        if (geometry != null) {
            double dx = (e.getX() - first.x) / scale;
            double dy = (e.getY() - first.y) / scale;

            if (isLabel(index)) {
                geometry = geometry.clone();

                if (geometry.getOffset() != null) {
                    dx += geometry.getOffset().getX();
                    dy += geometry.getOffset().getY();
                }

                if (gridEnabledEvent) {
                    dx = graph.snap(dx);
                    dy = graph.snap(dy);
                }
                geometry.setOffset(new PointDouble(dx, dy));
                graph.getModel().setGeometry(cell, geometry);
            } else {
                RectangleDouble bounds = union(geometry, dx, dy, index);
                Rectangle rect = bounds.getRectangle();

                // Snaps new bounds to grid (unscaled)
                if (gridEnabledEvent) {
                    int x = (int) graph.snap(rect.x);
                    int y = (int) graph.snap(rect.y);
                    rect.width = (int) graph.snap(rect.width - x + rect.x);
                    rect.height = (int) graph.snap(rect.height - y + rect.y);
                    rect.x = x;
                    rect.y = y;
                }
                graph.resizeCell(cell, new RectangleDouble(rect));
            }
        }
    }

    protected RectangleDouble union(RectangleDouble bounds, double dx, double dy, int index) {
        double left = bounds.getX();
        double right = left + bounds.getWidth();
        double top = bounds.getY();
        double bottom = top + bounds.getHeight();
        if (index > 4 /* Bottom Row */) {
            bottom = bottom + dy;
        } else if (index < 3 /* Top Row */) {
            top = top + dy;
        }

        if (index == 0 || index == 3 || index == 5 /* Left */) {
            left += dx;
        } else if (index == 2 || index == 4 || index == 7 /* Right */) {
            right += dx;
        }

        double width = right - left;
        double height = bottom - top;

        // Flips over left side
        if (width < 0) {
            left += width;
            width = Math.abs(width);
        }

        // Flips over top side
        if (height < 0) {
            top += height;
            height = Math.abs(height);
        }
        return new RectangleDouble(left, top, width, height);
    }
}
