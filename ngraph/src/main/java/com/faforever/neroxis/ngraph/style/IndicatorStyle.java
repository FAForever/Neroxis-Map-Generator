package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.shape.IShape;
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
public class IndicatorStyle implements PropertyChangeListener {
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private String image;
    private Color color;
    private Color gradientColor;
    private int spacing;
    private int width;
    private int height;
    private IShape shape;

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
        if (propertyName.equals(Fields.image) && Objects.equals(oldValue, image)) {
            setImage((String) newValue);
        } else if (propertyName.equals(Fields.color) && Objects.equals(oldValue, color)) {
            setColor((Color) newValue);
        } else if (propertyName.equals(Fields.gradientColor) && Objects.equals(oldValue, gradientColor)) {
            setGradientColor((Color) newValue);
        } else if (propertyName.equals(Fields.spacing) && Objects.equals(oldValue, spacing)) {
            setSpacing((int) newValue);
        } else if (propertyName.equals(Fields.width) && Objects.equals(oldValue, width)) {
            setWidth((int) newValue);
        } else if (propertyName.equals(Fields.height) && Objects.equals(oldValue, height)) {
            setHeight((int) newValue);
        } else if (propertyName.equals(Fields.shape) && Objects.equals(oldValue, shape)) {
            setShape((IShape) newValue);
        }
    }

    public void setImage(String image) {
        String old = this.image;
        this.image = image;
        changeSupport.firePropertyChange(Fields.image, old, image);
    }

    public void setColor(Color color) {
        Color old = this.color;
        this.color = color;
        changeSupport.firePropertyChange(Fields.color, old, color);
    }

    public void setGradientColor(Color gradientColor) {
        Color old = this.gradientColor;
        this.gradientColor = gradientColor;
        changeSupport.firePropertyChange(Fields.gradientColor, old, gradientColor);
    }

    public void setSpacing(int spacing) {
        int old = this.spacing;
        this.spacing = spacing;
        changeSupport.firePropertyChange(Fields.spacing, old, spacing);
    }

    public void setWidth(int width) {
        int old = this.width;
        this.width = width;
        changeSupport.firePropertyChange(Fields.width, old, width);
    }

    public void setHeight(int height) {
        int old = this.height;
        this.height = height;
        changeSupport.firePropertyChange(Fields.height, old, height);
    }

    public void setShape(IShape shape) {
        IShape old = this.shape;
        this.shape = shape;
        changeSupport.firePropertyChange(Fields.shape, old, shape);
    }
}
