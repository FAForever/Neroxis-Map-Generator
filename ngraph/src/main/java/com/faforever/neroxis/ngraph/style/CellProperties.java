package com.faforever.neroxis.ngraph.style;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

@Data
@FieldNameConstants(level = AccessLevel.PRIVATE)
public class CellProperties implements PropertyChangeListener {
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private boolean orthogonal = true;
    private boolean glass;
    private boolean shadow;
    private boolean rounded;
    private boolean dashed;
    private boolean deletable = true;
    private boolean cloneable = true;
    private boolean resizable = true;
    private boolean movable = true;
    private boolean bendable = true;
    private boolean editable = true;
    private boolean foldable = true;
    private boolean autosize = true;
    /**
     * This value only applies to vertices. If the shape is swimlane
     * false indicates that the swimlane should be drawn vertically, true indicates to draw it horizontally. If the
     * shape is not a swimlane, this value affects only whether the label is drawn horizontally or vertically.
     */
    private boolean horizontal = true;

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
        if (propertyName.equals(Fields.orthogonal) && Objects.equals(oldValue, orthogonal)) {
            setOrthogonal((boolean) newValue);
        } else if (propertyName.equals(Fields.glass) && Objects.equals(oldValue, glass)) {
            setGlass((boolean) newValue);
        } else if (propertyName.equals(Fields.shadow) && Objects.equals(oldValue, shadow)) {
            setShadow((boolean) newValue);
        } else if (propertyName.equals(Fields.dashed) && Objects.equals(oldValue, dashed)) {
            setDashed((boolean) newValue);
        } else if (propertyName.equals(Fields.deletable) && Objects.equals(oldValue, deletable)) {
            setDeletable((boolean) newValue);
        } else if (propertyName.equals(Fields.cloneable) && Objects.equals(oldValue, cloneable)) {
            setCloneable((boolean) newValue);
        } else if (propertyName.equals(Fields.resizable) && Objects.equals(oldValue, resizable)) {
            setResizable((boolean) newValue);
        } else if (propertyName.equals(Fields.movable) && Objects.equals(oldValue, movable)) {
            setMovable((boolean) newValue);
        } else if (propertyName.equals(Fields.bendable) && Objects.equals(oldValue, bendable)) {
            setBendable((boolean) newValue);
        } else if (propertyName.equals(Fields.editable) && Objects.equals(oldValue, editable)) {
            setEditable((boolean) newValue);
        } else if (propertyName.equals(Fields.foldable) && Objects.equals(oldValue, foldable)) {
            setFoldable((boolean) newValue);
        } else if (propertyName.equals(Fields.autosize) && Objects.equals(oldValue, autosize)) {
            setAutosize((boolean) newValue);
        }
    }

    public void setOrthogonal(boolean orthogonal) {
        boolean old = this.orthogonal;
        this.orthogonal = orthogonal;
        changeSupport.firePropertyChange(Fields.orthogonal, old, orthogonal);
    }

    public void setGlass(boolean glass) {
        boolean old = this.glass;
        this.glass = glass;
        changeSupport.firePropertyChange(Fields.glass, old, glass);
    }

    public void setShadow(boolean shadow) {
        boolean old = this.shadow;
        this.shadow = shadow;
        changeSupport.firePropertyChange(Fields.shadow, old, shadow);
    }

    public void setDashed(boolean dashed) {
        boolean old = this.dashed;
        this.dashed = dashed;
        changeSupport.firePropertyChange(Fields.dashed, old, dashed);
    }

    public void setDeletable(boolean deletable) {
        boolean old = this.deletable;
        this.deletable = deletable;
        changeSupport.firePropertyChange(Fields.deletable, old, deletable);
    }

    public void setCloneable(boolean cloneable) {
        boolean old = this.cloneable;
        this.cloneable = cloneable;
        changeSupport.firePropertyChange(Fields.cloneable, old, cloneable);
    }

    public void setResizable(boolean resizable) {
        boolean old = this.resizable;
        this.resizable = resizable;
        changeSupport.firePropertyChange(Fields.resizable, old, resizable);
    }

    public void setMovable(boolean movable) {
        boolean old = this.movable;
        this.movable = movable;
        changeSupport.firePropertyChange(Fields.movable, old, movable);
    }

    public void setBendable(boolean bendable) {
        boolean old = this.bendable;
        this.bendable = bendable;
        changeSupport.firePropertyChange(Fields.bendable, old, bendable);
    }

    public void setEditable(boolean editable) {
        boolean old = this.editable;
        this.editable = editable;
        changeSupport.firePropertyChange(Fields.editable, old, editable);
    }

    public void setFoldable(boolean foldable) {
        boolean old = this.foldable;
        this.foldable = foldable;
        changeSupport.firePropertyChange(Fields.foldable, old, foldable);
    }

    public void setAutosize(boolean autosize) {
        boolean old = this.autosize;
        this.autosize = autosize;
        changeSupport.firePropertyChange(Fields.autosize, old, autosize);
    }

    public void setRounded(boolean rounded) {
        boolean old = this.rounded;
        this.rounded = rounded;
        changeSupport.firePropertyChange(Fields.rounded, old, rounded);
    }

    public void setHorizontal(boolean horizontal) {
        boolean old = this.horizontal;
        this.horizontal = horizontal;
        changeSupport.firePropertyChange(Fields.horizontal, old, horizontal);
    }
}
