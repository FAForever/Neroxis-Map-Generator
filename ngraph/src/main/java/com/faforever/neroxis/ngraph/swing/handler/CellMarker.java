/**
 * Copyright (c) 2008-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.swing.handler;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.swing.util.SwingConstants;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Event;
import com.faforever.neroxis.ngraph.util.EventObject;
import com.faforever.neroxis.ngraph.util.EventSource;
import com.faforever.neroxis.ngraph.util.EventSource.IEventListener;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.GraphView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Implements a mouse tracker that marks cells under the mouse.
 * <p>
 * This class fires the following event:
 * <p>
 * Event.MARK fires in mark and unmark to notify the listener of a new cell
 * under the mouse. The <code>state</code> property contains the CellState
 * of the respective cell or null if no cell is under the mouse.
 * <p>
 * To create a cell marker which highlights cells "in-place", the following
 * code can be used:
 * <code>
 * CellMarker highlighter = new CellMarker(graphComponent) {
 * <p>
 * protected Map<String, Object> lastStyle;
 * <p>
 * public CellState process(MouseEvent e)
 * {
 * CellState state = null;
 * <p>
 * if (isEnabled())
 * {
 * state = getState(e);
 * boolean isValid = (state != null) ? isValidState(state) : false;
 * <p>
 * if (!isValid)
 * {
 * state = null;
 * }
 * <p>
 * highlight(state);
 * }
 * <p>
 * return state;
 * }
 * <p>
 * public void highlight(CellState state)
 * {
 * if (validState != state)
 * {
 * Rectangle dirty = null;
 * <p>
 * if (validState != null)
 * {
 * validState.setStyle(lastStyle);
 * dirty = validState.getBoundingBox().getRectangle();
 * dirty.grow(4, 4);
 * }
 * <p>
 * if (state != null)
 * {
 * lastStyle = state.getStyle();
 * state.setStyle(new Hashtable<String, Object>(state.getStyle()));
 * state.getStyle().put("strokeColor", "#00ff00");
 * state.getStyle().put("fontColor", "#00ff00");
 * state.getStyle().put("strokeWidth", "3");
 * <p>
 * Rectangle tmp = state.getBoundingBox().getRectangle();
 * <p>
 * if (dirty != null)
 * {
 * dirty.add(tmp);
 * }
 * else
 * {
 * dirty = tmp;
 * }
 * <p>
 * dirty.grow(4, 4);
 * }
 * <p>
 * validState = state;
 * graphComponent.repaint(dirty);
 * }
 * }
 * <p>
 * public void reset()
 * {
 * highlight(null);
 * }
 * <p>
 * public void paint(Graphics g)
 * {
 * // do nothing
 * }
 * };
 * <p>
 * graphComponent.getConnectionHandler().setMarker(highlighter);
 * </code>
 */
public class CellMarker extends JComponent {

    /**
     *
     */
    private static final long serialVersionUID = 614473367053597572L;

    /**
     * Specifies if the highlights should appear on top of everything
     * else in the overlay pane. Default is false.
     */
    public static boolean KEEP_ON_TOP = false;

    /**
     * Specifies the default stroke for the marker.
     */
    public static Stroke DEFAULT_STROKE = new BasicStroke(3);

    /**
     * Holds the event source.
     */
    protected EventSource eventSource = new EventSource(this);

    /**
     * Holds the enclosing graph component.
     */
    protected GraphComponent graphComponent;

    /**
     * Specifies if the marker is enabled. Default is true.
     */
    protected boolean enabled = true;

    /**
     * Specifies the portion of the width and height that should trigger
     * a highlight. The area around the center of the cell to be marked is used
     * as the hotspot. Possible values are between 0 and 1. Default is
     * Constants.DEFAULT_HOTSPOT.
     */
    protected double hotspot;

    /**
     * Specifies if the hotspot is enabled. Default is false.
     */
    protected boolean hotspotEnabled = false;

    /**
     * Specifies if the the content area of swimlane should be non-transparent
     * to mouse events. Default is false.
     */
    protected boolean swimlaneContentEnabled = false;

    /**
     * Specifies the valid- and invalidColor for the marker.
     */
    protected Color validColor, invalidColor;

    /**
     * Holds the current marker color.
     */
    protected transient Color currentColor;

    /**
     * Holds the marked state if it is valid.
     */
    protected transient CellState validState;

    /**
     * Holds the marked state.
     */
    protected transient CellState markedState;

    /**
     * Constructs a new marker for the given graph component.
     *
     * @param graphComponent
     */
    public CellMarker(GraphComponent graphComponent) {
        this(graphComponent, SwingConstants.DEFAULT_VALID_COLOR);
    }

    /**
     * Constructs a new marker for the given graph component.
     */
    public CellMarker(GraphComponent graphComponent, Color validColor) {
        this(graphComponent, validColor, SwingConstants.DEFAULT_INVALID_COLOR);
    }

    /**
     * Constructs a new marker for the given graph component.
     */
    public CellMarker(GraphComponent graphComponent, Color validColor, Color invalidColor) {
        this(graphComponent, validColor, invalidColor, Constants.DEFAULT_HOTSPOT);
    }

    /**
     * Constructs a new marker for the given graph component.
     */
    public CellMarker(GraphComponent graphComponent, Color validColor, Color invalidColor, double hotspot) {
        this.graphComponent = graphComponent;
        this.validColor = validColor;
        this.invalidColor = invalidColor;
        this.hotspot = hotspot;
    }

    /**
     * Returns true if the marker is enabled, that is, if it processes events
     * in process.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled state of the marker.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the hotspot.
     */
    public double getHotspot() {
        return hotspot;
    }

    /**
     * Sets the hotspot.
     */
    public void setHotspot(double hotspot) {
        this.hotspot = hotspot;
    }

    /**
     * Returns true if hotspot is used in intersects.
     */
    public boolean isHotspotEnabled() {
        return hotspotEnabled;
    }

    /**
     * Specifies whether the hotspot should be used in intersects.
     */
    public void setHotspotEnabled(boolean enabled) {
        this.hotspotEnabled = enabled;
    }

    /**
     * Returns true if the content area of swimlanes is non-transparent to
     * events.
     */
    public boolean isSwimlaneContentEnabled() {
        return swimlaneContentEnabled;
    }

    /**
     * Sets if the content area of swimlanes should not be transparent to
     * events.
     */
    public void setSwimlaneContentEnabled(boolean swimlaneContentEnabled) {
        this.swimlaneContentEnabled = swimlaneContentEnabled;
    }

    /**
     * Returns the color used for valid highlights.
     */
    public Color getValidColor() {
        return validColor;
    }

    /**
     * Sets the color used for valid highlights.
     */
    public void setValidColor(Color value) {
        validColor = value;
    }

    /**
     * Returns the color used for invalid highlights.
     */
    public Color getInvalidColor() {
        return invalidColor;
    }

    /**
     * Sets the color used for invalid highlights.
     */
    public void setInvalidColor(Color value) {
        invalidColor = value;
    }

    /**
     * Returns true if validState is not null.
     */
    public boolean hasValidState() {
        return (validState != null);
    }

    /**
     * Returns the valid state.
     */
    public CellState getValidState() {
        return validState;
    }

    /**
     * Returns the current color.
     */
    public Color getCurrentColor() {
        return currentColor;
    }

    /**
     * Sets the current color.
     */
    public void setCurrentColor(Color value) {
        currentColor = value;
    }

    /**
     * Returns the marked state.
     */
    public CellState getMarkedState() {
        return markedState;
    }

    /**
     * Sets the marked state.
     */
    public void setMarkedState(CellState value) {
        markedState = value;
    }

    /**
     * Resets the state of the cell marker.
     */
    public void reset() {
        validState = null;

        if (markedState != null) {
            markedState = null;
            unmark();
        }
    }

    /**
     * Processes the given event and marks the state returned by getStateAt
     * with the color returned by getMarkerColor. If the markerColor is not
     * null, then the state is stored in markedState. If isValidState returns
     * true, then the state is stored in validState regardless of the marker
     * color. The state is returned regardless of the marker color and
     * valid state.
     */
    public CellState process(MouseEvent e) {
        CellState state = null;

        if (isEnabled()) {
            state = getState(e);
            boolean valid = state != null && isValidState(state);
            Color color = getMarkerColor(e, state, valid);

            highlight(state, color, valid);
        }

        return state;
    }

    /**
     *
     */
    public void highlight(CellState state, Color color) {
        highlight(state, color, true);
    }

    /**
     *
     */
    public void highlight(CellState state, Color color, boolean valid) {
        if (valid) {
            validState = state;
        } else {
            validState = null;
        }

        if (state != markedState || color != currentColor) {
            currentColor = color;

            if (state != null && currentColor != null) {
                markedState = state;
                mark();
            } else if (markedState != null) {
                markedState = null;
                unmark();
            }
        }
    }

    /**
     * Marks the markedState and fires a Event.MARK event.
     */
    public void mark() {
        if (markedState != null) {
            Rectangle bounds = markedState.getRectangle();
            bounds.grow(3, 3);
            bounds.width += 1;
            bounds.height += 1;
            setBounds(bounds);

            if (getParent() == null) {
                setVisible(true);

                if (KEEP_ON_TOP) {
                    graphComponent.getGraphControl().add(this, 0);
                } else {
                    graphComponent.getGraphControl().add(this);
                }
            }

            repaint();
            eventSource.fireEvent(new EventObject(Event.MARK, "state", markedState));
        }
    }

    /**
     * Hides the marker and fires a Event.MARK event.
     */
    public void unmark() {
        if (getParent() != null) {
            setVisible(false);
            getParent().remove(this);
            eventSource.fireEvent(new EventObject(Event.MARK));
        }
    }

    /**
     * Returns true if the given state is a valid state. If this returns true,
     * then the state is stored in validState. The return value of this method
     * is used as the argument for getMarkerColor.
     */
    protected boolean isValidState(CellState state) {
        return true;
    }

    /**
     * Returns the valid- or invalidColor depending on the value of isValid.
     * The given state is ignored by this implementation.
     */
    protected Color getMarkerColor(MouseEvent e, CellState state, boolean isValid) {
        return (isValid) ? validColor : invalidColor;
    }

    /**
     * Uses getCell, getMarkedState and intersects to return the state for
     * the given event.
     */
    protected CellState getState(MouseEvent e) {
        ICell cell = getCell(e);
        GraphView view = graphComponent.getGraph().getView();
        CellState state = getStateToMark(view.getState(cell));

        return (state != null && intersects(state, e)) ? state : null;
    }

    /**
     * Returns the state at the given location. This uses Graph.getCellAt.
     */
    protected ICell getCell(MouseEvent e) {
        return graphComponent.getCellAt(e.getX(), e.getY(), swimlaneContentEnabled);
    }

    /**
     * Returns the state to be marked for the given state under the mouse. This
     * returns the given state.
     */
    protected CellState getStateToMark(CellState state) {
        return state;
    }

    /**
     * Returns true if the given mouse event intersects the given state. This
     * returns true if the hotspot is 0 or the event is inside the hotspot for
     * the given cell state.
     */
    protected boolean intersects(CellState state, MouseEvent e) {
        if (isHotspotEnabled()) {
            return Utils.intersectsHotspot(state, e.getX(), e.getY(), hotspot, Constants.MIN_HOTSPOT_SIZE, Constants.MAX_HOTSPOT_SIZE);
        }

        return true;
    }

    /**
     * Adds the given event listener.
     */
    public void addListener(String eventName, IEventListener listener) {
        eventSource.addListener(eventName, listener);
    }

    /**
     * Removes the given event listener.
     */
    public void removeListener(IEventListener listener) {
        eventSource.removeListener(listener);
    }

    /**
     * Removes the given event listener for the specified event name.
     */
    public void removeListener(IEventListener listener, String eventName) {
        eventSource.removeListener(listener, eventName);
    }

    /**
     * Paints the outline of the markedState with the currentColor.
     */
    public void paint(Graphics g) {
        if (markedState != null && currentColor != null) {
            ((Graphics2D) g).setStroke(DEFAULT_STROKE);
            g.setColor(currentColor);

            if (markedState.getAbsolutePointCount() > 0) {
                Point last = markedState.getAbsolutePoint(0).getPoint();

                for (int i = 1; i < markedState.getAbsolutePointCount(); i++) {
                    Point current = markedState.getAbsolutePoint(i).getPoint();
                    g.drawLine(last.x - getX(), last.y - getY(), current.x - getX(), current.y - getY());
                    last = current;
                }
            } else {
                g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            }
        }
    }

}
