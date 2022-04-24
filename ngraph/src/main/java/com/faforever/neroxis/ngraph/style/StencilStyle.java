package com.faforever.neroxis.ngraph.style;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(level = AccessLevel.PRIVATE)
public class StencilStyle implements PropertyChangeListener {

    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private boolean flipHorizontal;
    private boolean flipVertical;

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
        if (propertyName.equals(Fields.flipHorizontal) && Objects.equals(oldValue, flipHorizontal)) {
            setFlipHorizontal((boolean) newValue);
        } else if (propertyName.equals(Fields.flipVertical) && Objects.equals(oldValue, flipVertical)) {
            setFlipVertical((boolean) newValue);
        }
    }

    public void setFlipHorizontal(boolean flipHorizontal) {
        boolean old = this.flipHorizontal;
        this.flipHorizontal = flipHorizontal;
        changeSupport.firePropertyChange(Fields.flipHorizontal, old, flipHorizontal);
    }

    public void setFlipVertical(boolean flipVertical) {
        boolean old = this.flipVertical;
        this.flipVertical = flipVertical;
        changeSupport.firePropertyChange(Fields.flipVertical, old, flipVertical);
    }
}
