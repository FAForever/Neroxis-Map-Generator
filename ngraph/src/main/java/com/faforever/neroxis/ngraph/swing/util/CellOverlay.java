package com.faforever.neroxis.ngraph.swing.util;

import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Point;
import com.faforever.neroxis.ngraph.util.Rectangle;
import com.faforever.neroxis.ngraph.view.CellState;

import javax.swing.*;
import java.awt.*;

public class CellOverlay extends JComponent implements ICellOverlay {

    /**
     *
     */
    private static final long serialVersionUID = 921991820491141221L;

    /**
     *
     */
    protected ImageIcon imageIcon;

    /**
     * Holds the horizontal alignment for the overlay.
     * Default is ALIGN_RIGHT. For edges, the overlay
     * always appears in the center of the edge.
     */
    protected Object align = Constants.ALIGN_RIGHT;

    /**
     * Holds the vertical alignment for the overlay.
     * Default is bottom. For edges, the overlay
     * always appears in the center of the edge.
     */
    protected Object verticalAlign = Constants.ALIGN_BOTTOM;

    /**
     * Defines the overlapping for the overlay, that is,
     * the proportional distance from the origin to the
     * point defined by the alignment. Default is 0.5.
     */
    protected double defaultOverlap = 0.5;

    /**
     *
     */
    public CellOverlay(ImageIcon icon, String warning) {
        this.imageIcon = icon;
        setToolTipText(warning);
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * @return the alignment of the overlay, see <code>Constants.ALIGN_*****</code>
     */
    public Object getAlign() {
        return align;
    }

    /**
     * @param value the alignment to set, see <code>Constants.ALIGN_*****</code>
     */
    public void setAlign(Object value) {
        align = value;
    }

    /**
     * @return the vertical alignment, see <code>Constants.ALIGN_*****</code>
     */
    public Object getVerticalAlign() {
        return verticalAlign;
    }

    /**
     * @param value the vertical alignment to set, see <code>Constants.ALIGN_*****</code>
     */
    public void setVerticalAlign(Object value) {
        verticalAlign = value;
    }

    /**
     *
     */
    public void paint(Graphics g) {
        g.drawImage(imageIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
    }

    /*
     * (non-Javadoc)
     * @see com.faforever.neroxis.ngraph.swing.util.IOverlay#getBounds(com.faforever.neroxis.ngraph.view.CellState)
     */
    public Rectangle getBounds(CellState state) {
        boolean isEdge = state.getView().getGraph().getModel().isEdge(state.getCell());
        double s = state.getView().getScale();
        Point pt = null;

        int w = imageIcon.getIconWidth();
        int h = imageIcon.getIconHeight();

        if (isEdge) {
            int n = state.getAbsolutePointCount();

            if (n % 2 == 1) {
                pt = state.getAbsolutePoint(n / 2 + 1);
            } else {
                int idx = n / 2;
                Point p0 = state.getAbsolutePoint(idx - 1);
                Point p1 = state.getAbsolutePoint(idx);
                pt = new Point(p0.getX() + (p1.getX() - p0.getX()) / 2, p0.getY() + (p1.getY() - p0.getY()) / 2);
            }
        } else {
            pt = new Point();

            if (align.equals(Constants.ALIGN_LEFT)) {
                pt.setX(state.getX());
            } else if (align.equals(Constants.ALIGN_CENTER)) {
                pt.setX(state.getX() + state.getWidth() / 2);
            } else {
                pt.setX(state.getX() + state.getWidth());
            }

            if (verticalAlign.equals(Constants.ALIGN_TOP)) {
                pt.setY(state.getY());
            } else if (verticalAlign.equals(Constants.ALIGN_MIDDLE)) {
                pt.setY(state.getY() + state.getHeight() / 2);
            } else {
                pt.setY(state.getY() + state.getHeight());
            }
        }

        return new Rectangle(pt.getX() - w * defaultOverlap * s, pt.getY() - h * defaultOverlap * s, w * s, h * s);
    }

}
