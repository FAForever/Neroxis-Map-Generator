package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.style.util.HorizontalAlignment;
import com.faforever.neroxis.ngraph.style.util.VerticalAlignment;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

@Data
@FieldNameConstants(level = AccessLevel.PRIVATE)
public class ImageStyle implements PropertyChangeListener {
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private String image;
    private int width;
    private int height;
    private Color backgroundColor;
    private Color borderColor;
    private boolean flipHorizontal;
    private boolean flipVertical;
    private VerticalAlignment verticalAlignment = VerticalAlignment.MIDDLE;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.CENTER;

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
        } else if (propertyName.equals(Fields.width) && Objects.equals(oldValue, width)) {
            setWidth((int) newValue);
        } else if (propertyName.equals(Fields.height) && Objects.equals(oldValue, height)) {
            setHeight((int) newValue);
        } else if (propertyName.equals(Fields.backgroundColor) && Objects.equals(oldValue, backgroundColor)) {
            setBackgroundColor((Color) newValue);
        } else if (propertyName.equals(Fields.borderColor) && Objects.equals(oldValue, borderColor)) {
            setBorderColor((Color) newValue);
        } else if (propertyName.equals(Fields.flipHorizontal) && Objects.equals(oldValue, flipHorizontal)) {
            setFlipHorizontal((boolean) newValue);
        } else if (propertyName.equals(Fields.flipVertical) && Objects.equals(oldValue, flipVertical)) {
            setFlipVertical((boolean) newValue);
        } else if (propertyName.equals(Fields.verticalAlignment) && Objects.equals(oldValue, verticalAlignment)) {
            setVerticalAlignment((VerticalAlignment) newValue);
        } else if (propertyName.equals(Fields.horizontalAlignment) && Objects.equals(oldValue, horizontalAlignment)) {
            setHorizontalAlignment((HorizontalAlignment) newValue);
        }
    }

    public void setImage(String image) {
        String old = this.image;
        this.image = image;
        changeSupport.firePropertyChange(Fields.image, old, image);
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

    public void setBackgroundColor(Color backgroundColor) {
        Color old = this.backgroundColor;
        this.backgroundColor = backgroundColor;
        changeSupport.firePropertyChange(Fields.backgroundColor, old, backgroundColor);
    }

    public void setBorderColor(Color borderColor) {
        Color old = this.borderColor;
        this.borderColor = borderColor;
        changeSupport.firePropertyChange(Fields.borderColor, old, borderColor);
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

    public void setVerticalAlignment(VerticalAlignment verticalAlignment) {
        VerticalAlignment old = this.verticalAlignment;
        this.verticalAlignment = verticalAlignment;
        changeSupport.firePropertyChange(Fields.verticalAlignment, old, verticalAlignment);
    }

    public void setHorizontalAlignment(HorizontalAlignment horizontalAlignment) {
        HorizontalAlignment old = this.horizontalAlignment;
        this.horizontalAlignment = horizontalAlignment;
        changeSupport.firePropertyChange(Fields.horizontalAlignment, old, horizontalAlignment);
    }
}
