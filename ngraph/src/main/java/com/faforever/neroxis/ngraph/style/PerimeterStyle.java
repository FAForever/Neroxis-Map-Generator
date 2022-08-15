package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.style.perimeter.Perimeter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(level = AccessLevel.PRIVATE)
public class PerimeterStyle implements PropertyChangeListener {
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    /**
     * This is the distance between the source
     * connection point of an edge and the perimeter of the source vertex in
     * pixels. This only applies to edges.
     */
    double sourceSpacing;
    /**
     * This is the distance between the target
     * connection of an edge and the perimeter of the target vertex in
     * pixels. This style only applies to edges.
     */
    double targetSpacing;
    /**
     * This is the distance between
     * the connection point and the perimeter in pixels. When used in a vertex
     * style, this applies to all incoming edges to floating ports (edges that
     * terminate on the perimeter of the vertex). When used in an edge style,
     * this spacing applies to the source and target separately, if they
     * terminate in floating ports (on the perimeter of the vertex).
     */
    double vertexSpacing;
    private Perimeter perimeter;

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
        if (propertyName.equals(Fields.sourceSpacing) && Objects.equals(oldValue, sourceSpacing)) {
            setSourceSpacing((double) newValue);
        } else if (propertyName.equals(Fields.targetSpacing) && Objects.equals(oldValue, targetSpacing)) {
            setTargetSpacing((double) newValue);
        } else if (propertyName.equals(Fields.vertexSpacing) && Objects.equals(oldValue, vertexSpacing)) {
            setVertexSpacing((double) newValue);
        } else if (propertyName.equals(Fields.perimeter) && Objects.equals(oldValue, perimeter)) {
            setPerimeter((Perimeter) newValue);
        }
    }

    public void setSourceSpacing(double sourceSpacing) {
        double old = this.sourceSpacing;
        this.sourceSpacing = sourceSpacing;
        changeSupport.firePropertyChange(Fields.sourceSpacing, old, sourceSpacing);
    }

    public void setTargetSpacing(double targetSpacing) {
        double old = this.targetSpacing;
        this.targetSpacing = targetSpacing;
        changeSupport.firePropertyChange(Fields.targetSpacing, old, targetSpacing);
    }

    public void setVertexSpacing(double vertexSpacing) {
        double old = this.vertexSpacing;
        this.vertexSpacing = vertexSpacing;
        changeSupport.firePropertyChange(Fields.vertexSpacing, old, vertexSpacing);
    }

    public void setPerimeter(Perimeter perimeter) {
        Perimeter old = this.perimeter;
        this.perimeter = perimeter;
        changeSupport.firePropertyChange(Fields.perimeter, old, perimeter);
    }
}
