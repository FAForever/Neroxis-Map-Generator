package com.faforever.neroxis.ngraph.style;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(level = AccessLevel.PRIVATE)
public class SwimlaneStyle implements PropertyChangeListener {
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    /**
     * Specifies whether the line between the title region of a swimlane should be visible.
     */
    private boolean line = true;
    private Color color;

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
        if (propertyName.equals(Fields.color) && Objects.equals(oldValue, color)) {
            setColor((Color) newValue);
        } else if (propertyName.equals(Fields.line) && Objects.equals(oldValue, line)) {
            setLine((boolean) newValue);
        }
    }

    public void setLine(boolean line) {
        boolean old = this.line;
        this.line = line;
        changeSupport.firePropertyChange(Fields.line, old, line);
    }

    public void setColor(Color color) {
        Color old = this.color;
        this.color = color;
        changeSupport.firePropertyChange(Fields.color, old, color);
    }
}
