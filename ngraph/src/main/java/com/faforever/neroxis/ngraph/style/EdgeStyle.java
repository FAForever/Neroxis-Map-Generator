package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.style.arrow.Arrow;
import com.faforever.neroxis.ngraph.style.edge.EdgeStyleFunction;
import com.faforever.neroxis.ngraph.style.util.Direction;
import com.faforever.neroxis.ngraph.style.util.Elbow;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(level = AccessLevel.PRIVATE)
public class EdgeStyle implements PropertyChangeListener {
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private boolean noEdgeStyle;
    private EdgeStyleFunction edgeStyleFunction;
    private EdgeStyleFunction loopStyleFunction;
    private Elbow elbow;
    private Arrow startArrow;
    private Arrow endArrow;
    private boolean dashed;
    /**
     * Represents the size of the horizontal
     * segment of the entity relation style. Default is ENTITY_SEGMENT.
     */
    private float segmentSize = 30;
    /**
     * Represents the size of the start marker
     * or the size of the swimlane title region depending on the shape it is
     * used for.
     */
    private float startSize = 40;
    /**
     * Represents the size of the end
     * marker in pixels.
     */
    private float endSize = 6;
    /**
     * Defines the cell that should be used for computing the
     * perimeter point of the source for an edge. This allows for graphically
     * connecting to a cell while keeping the actual terminal of the edge.
     */
    private ICell sourcePort;
    /**
     * Defines the cell that should be used for computing the
     * perimeter point of the target for an edge. This allows for graphically
     * connecting to a cell while keeping the actual terminal of the edge.
     */
    private ICell targetPort;
    /**
     * Defines if the perimeter should be used to find the exact entry point
     * along the perimeter of the source.
     */
    private boolean entryPerimeter = true;
    /**
     * Defines the horizontal relative coordinate connection point
     * of an edge with its target terminal.
     */
    private Float entryX;
    /**
     * Defines the vertical relative coordinate connection point
     * of an edge with its target terminal.
     */
    private Float entryY;
    /**
     * Defines if the perimeter should be used to find the exact entry point
     * along the perimeter of the target.
     */
    private boolean exitPerimeter = true;
    /**
     * Defines the horizontal relative coordinate connection point
     * of an edge with its source terminal.
     */
    private Float exitX;
    /**
     * Defines the vertical relative coordinate connection point
     * of an edge with its source terminal.
     */
    private Float exitY;
    /**
     * Specifies the dashed pattern to apply to edges drawn with this style. This style allows the user
     * to specify a custom-defined dash pattern. This is done using a series
     * of numbers. Dash styles are defined in terms of the length of the dash
     * (the drawn part of the stroke) and the length of the space between the
     * dashes. The lengths are relative to the line width: a length of "1" is
     * equal to the line width.
     */
    private float[] dashPattern = new float[]{3f, 3f};
    /**
     * Defines the direction(s) that edges are allowed to connect to cells in.
     * Possible values are <code>DIRECTION_NORTH, DIRECTION_SOUTH,
     * DIRECTION_EAST</code> and <code>DIRECTION_WEST</code>.
     */
    private Set<Direction> portConstraints;
    /**
     * This is the relative horizaontal offset from the center used for connecting edges.
     * Possible values are between -0.5 and 0.5.
     */
    private float routingCenterX;
    /**
     * This is the relative vertical offset from the center used for connecting edges.
     * Possible values are between -0.5 and 0.5.
     */
    private float routingCenterY;

    void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Object oldValue = evt.getOldValue();
        Object newValue = evt.getNewValue();
        String propertyName = evt.getPropertyName();
        if (propertyName.equals(Fields.noEdgeStyle) && Objects.equals(oldValue, noEdgeStyle)) {
            setNoEdgeStyle((boolean) newValue);
        } else if (propertyName.equals(Fields.edgeStyleFunction) && Objects.equals(oldValue, edgeStyleFunction)) {
            setEdgeStyleFunction((EdgeStyleFunction) newValue);
        } else if (propertyName.equals(Fields.loopStyleFunction) && Objects.equals(oldValue, loopStyleFunction)) {
            setLoopStyleFunction((EdgeStyleFunction) newValue);
        } else if (propertyName.equals(Fields.elbow) && Objects.equals(oldValue, elbow)) {
            setElbow((Elbow) newValue);
        } else if (propertyName.equals(Fields.startArrow) && Objects.equals(oldValue, startArrow)) {
            setStartArrow((Arrow) newValue);
        } else if (propertyName.equals(Fields.endArrow) && Objects.equals(oldValue, endArrow)) {
            setEndArrow((Arrow) newValue);
        } else if (propertyName.equals(Fields.dashed) && Objects.equals(oldValue, dashed)) {
            setDashed((boolean) newValue);
        } else if (propertyName.equals(Fields.segmentSize) && Objects.equals(oldValue, segmentSize)) {
            setSegmentSize((float) newValue);
        } else if (propertyName.equals(Fields.startSize) && Objects.equals(oldValue, startSize)) {
            setStartSize((float) newValue);
        } else if (propertyName.equals(Fields.endSize) && Objects.equals(oldValue, endSize)) {
            setEndSize((float) newValue);
        } else if (propertyName.equals(Fields.sourcePort) && Objects.equals(oldValue, sourcePort)) {
            setSourcePort((ICell) newValue);
        } else if (propertyName.equals(Fields.targetPort) && Objects.equals(oldValue, targetPort)) {
            setTargetPort((ICell) newValue);
        } else if (propertyName.equals(Fields.entryPerimeter) && Objects.equals(oldValue, entryPerimeter)) {
            setEntryPerimeter((boolean) newValue);
        } else if (propertyName.equals(Fields.entryX) && Objects.equals(oldValue, entryX)) {
            setEntryX((Float) newValue);
        } else if (propertyName.equals(Fields.entryY) && Objects.equals(oldValue, entryY)) {
            setEntryY((Float) newValue);
        } else if (propertyName.equals(Fields.exitPerimeter) && Objects.equals(oldValue, exitPerimeter)) {
            setExitPerimeter((boolean) newValue);
        } else if (propertyName.equals(Fields.exitX) && Objects.equals(oldValue, exitX)) {
            setExitX((Float) newValue);
        } else if (propertyName.equals(Fields.exitY) && Objects.equals(oldValue, exitY)) {
            setExitY((Float) newValue);
        } else if (propertyName.equals(Fields.dashPattern) && Objects.equals(oldValue, dashPattern)) {
            setDashPattern((float[]) newValue);
        } else if (propertyName.equals(Fields.portConstraints) && Objects.equals(oldValue, portConstraints)) {
            setPortConstraints((Set<Direction>) newValue);
        } else if (propertyName.equals(Fields.routingCenterX) && Objects.equals(oldValue, routingCenterX)) {
            setRoutingCenterX((float) newValue);
        } else if (propertyName.equals(Fields.routingCenterY) && Objects.equals(oldValue, routingCenterY)) {
            setRoutingCenterY((float) newValue);
        }
    }

    public void setNoEdgeStyle(boolean noEdgeStyle) {
        boolean old = this.noEdgeStyle;
        this.noEdgeStyle = noEdgeStyle;
        changeSupport.firePropertyChange(Fields.noEdgeStyle, old, noEdgeStyle);
    }

    public void setEdgeStyleFunction(EdgeStyleFunction edgeStyleFunction) {
        EdgeStyleFunction old = this.edgeStyleFunction;
        this.edgeStyleFunction = edgeStyleFunction;
        changeSupport.firePropertyChange(Fields.edgeStyleFunction, old, edgeStyleFunction);
    }

    public void setLoopStyleFunction(EdgeStyleFunction loopStyleFunction) {
        EdgeStyleFunction old = this.loopStyleFunction;
        this.loopStyleFunction = loopStyleFunction;
        changeSupport.firePropertyChange(Fields.loopStyleFunction, old, loopStyleFunction);
    }

    public void setElbow(Elbow elbow) {
        Elbow old = this.elbow;
        this.elbow = elbow;
        changeSupport.firePropertyChange(Fields.elbow, old, elbow);
    }

    public void setStartArrow(Arrow startArrow) {
        Arrow old = this.startArrow;
        this.startArrow = startArrow;
        changeSupport.firePropertyChange(Fields.startArrow, old, startArrow);
    }

    public void setEndArrow(Arrow endArrow) {
        Arrow old = this.endArrow;
        this.endArrow = endArrow;
        changeSupport.firePropertyChange(Fields.endArrow, old, endArrow);
    }

    public void setDashed(boolean dashed) {
        boolean old = this.dashed;
        this.dashed = dashed;
        changeSupport.firePropertyChange(Fields.dashed, old, dashed);
    }

    public void setSegmentSize(float segmentSize) {
        float old = this.segmentSize;
        this.segmentSize = segmentSize;
        changeSupport.firePropertyChange(Fields.segmentSize, old, segmentSize);
    }

    public void setStartSize(float startSize) {
        float old = this.startSize;
        this.startSize = startSize;
        changeSupport.firePropertyChange(Fields.startSize, old, startSize);
    }

    public void setEndSize(float endSize) {
        float old = this.endSize;
        this.endSize = endSize;
        changeSupport.firePropertyChange(Fields.endSize, old, endSize);
    }

    public void setSourcePort(ICell sourcePort) {
        ICell old = this.sourcePort;
        this.sourcePort = sourcePort;
        changeSupport.firePropertyChange(Fields.sourcePort, old, sourcePort);
    }

    public void setTargetPort(ICell targetPort) {
        ICell old = this.targetPort;
        this.targetPort = targetPort;
        changeSupport.firePropertyChange(Fields.targetPort, old, targetPort);
    }

    public void setEntryPerimeter(boolean entryPerimeter) {
        boolean old = this.entryPerimeter;
        this.entryPerimeter = entryPerimeter;
        changeSupport.firePropertyChange(Fields.entryPerimeter, old, entryPerimeter);
    }

    public void setEntryX(Float entryX) {
        Float old = this.entryX;
        this.entryX = entryX;
        changeSupport.firePropertyChange(Fields.entryX, old, entryX);
    }

    public void setEntryY(Float entryY) {
        Float old = this.entryY;
        this.entryY = entryY;
        changeSupport.firePropertyChange(Fields.entryY, old, entryY);
    }

    public void setExitPerimeter(boolean exitPerimeter) {
        boolean old = this.exitPerimeter;
        this.exitPerimeter = exitPerimeter;
        changeSupport.firePropertyChange(Fields.exitPerimeter, old, exitPerimeter);
    }

    public void setExitX(Float exitX) {
        Float old = this.exitX;
        this.exitX = exitX;
        changeSupport.firePropertyChange(Fields.exitX, old, exitX);
    }

    public void setExitY(Float exitY) {
        Float old = this.exitY;
        this.exitY = exitY;
        changeSupport.firePropertyChange(Fields.exitY, old, exitY);
    }

    public void setDashPattern(float[] dashPattern) {
        float[] old = this.dashPattern;
        this.dashPattern = dashPattern;
        changeSupport.firePropertyChange(Fields.dashPattern, old, dashPattern);
    }

    public void setPortConstraints(Set<Direction> portConstraints) {
        Set<Direction> old = this.portConstraints;
        this.portConstraints = portConstraints;
        changeSupport.firePropertyChange(Fields.portConstraints, old, portConstraints);
    }

    public void setRoutingCenterX(float routingCenterX) {
        float old = this.routingCenterX;
        this.routingCenterX = routingCenterX;
        changeSupport.firePropertyChange(Fields.routingCenterX, old, routingCenterX);
    }

    public void setRoutingCenterY(float routingCenterY) {
        float old = this.routingCenterY;
        this.routingCenterY = routingCenterY;
        changeSupport.firePropertyChange(Fields.routingCenterY, old, routingCenterY);
    }
}
