/**
 * Copyright (c) 2008, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.swing.handler;

import com.faforever.neroxis.ngraph.swing.GraphComponent;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Event handler that highlights cells. Inherits from CellMarker.
 */
public class CellTracker extends CellMarker implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 7372144804885125688L;

    /**
     * Constructs an event handler that highlights cells.
     */
    public CellTracker(GraphComponent graphComponent, Color color) {
        super(graphComponent, color);

        graphComponent.getGraphControl().addMouseListener(this);
        graphComponent.getGraphControl().addMouseMotionListener(this);
    }

    public void destroy() {
        graphComponent.getGraphControl().removeMouseListener(this);
        graphComponent.getGraphControl().removeMouseMotionListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // empty
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // empty
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        reset();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // empty
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // empty
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // empty
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        process(e);
    }
}
