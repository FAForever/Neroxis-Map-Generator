package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.shape.IShape;
import com.faforever.neroxis.ngraph.style.util.Direction;
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
public class ShapeStyle implements PropertyChangeListener {

    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private IShape shape;
    private Direction direction = Direction.EAST;
    private Direction gradientDirection = Direction.SOUTH;
    private Color gradientColor;
    private Color fillColor;
    private double rotation;
    private float opacity = 100;
    private float fillOpacity = 100;
    private float strokeOpacity = 100;
    private float strokeWidth = 1;
    private Double arcSize;
    private Color strokeColor;
    private Color separatorColor;
    private boolean startFill = true;
    private boolean endFill = true;
    private boolean flipVertical;
    private boolean flipHorizontal;

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
        if (propertyName.equals(Fields.shape) && Objects.equals(oldValue, shape)) {
            setShape((IShape) newValue);
        } else if (propertyName.equals(Fields.direction) && Objects.equals(oldValue, direction)) {
            setDirection((Direction) newValue);
        } else if (propertyName.equals(Fields.gradientDirection) && Objects.equals(oldValue, gradientDirection)) {
            setGradientDirection((Direction) newValue);
        } else if (propertyName.equals(Fields.gradientColor) && Objects.equals(oldValue, gradientColor)) {
            setGradientColor((Color) newValue);
        } else if (propertyName.equals(Fields.fillColor) && Objects.equals(oldValue, fillColor)) {
            setFillColor((Color) newValue);
        } else if (propertyName.equals(Fields.rotation) && Objects.equals(oldValue, rotation)) {
            setRotation((double) newValue);
        } else if (propertyName.equals(Fields.opacity) && Objects.equals(oldValue, opacity)) {
            setOpacity((float) newValue);
        } else if (propertyName.equals(Fields.fillOpacity) && Objects.equals(oldValue, fillOpacity)) {
            setFillOpacity((float) newValue);
        } else if (propertyName.equals(Fields.strokeOpacity) && Objects.equals(oldValue, strokeOpacity)) {
            setStrokeOpacity((float) newValue);
        } else if (propertyName.equals(Fields.strokeWidth) && Objects.equals(oldValue, strokeWidth)) {
            setStrokeWidth((float) newValue);
        } else if (propertyName.equals(Fields.arcSize) && Objects.equals(oldValue, arcSize)) {
            setArcSize((Double) newValue);
        } else if (propertyName.equals(Fields.strokeColor) && Objects.equals(oldValue, strokeColor)) {
            setStrokeColor((Color) newValue);
        } else if (propertyName.equals(Fields.separatorColor) && Objects.equals(oldValue, separatorColor)) {
            setSeparatorColor((Color) newValue);
        } else if (propertyName.equals(Fields.startFill) && Objects.equals(oldValue, startFill)) {
            setStartFill((boolean) newValue);
        } else if (propertyName.equals(Fields.endFill) && Objects.equals(oldValue, endFill)) {
            setEndFill((boolean) newValue);
        } else if (propertyName.equals(Fields.flipVertical) && Objects.equals(oldValue, flipVertical)) {
            setFlipVertical((boolean) newValue);
        } else if (propertyName.equals(Fields.flipHorizontal) && Objects.equals(oldValue, flipHorizontal)) {
            setFlipHorizontal((boolean) newValue);
        }
    }

    public void setShape(IShape shape) {
        IShape old = this.shape;
        this.shape = shape;
        changeSupport.firePropertyChange(Fields.shape, old, shape);
    }

    public void setDirection(Direction direction) {
        Direction old = this.direction;
        this.direction = direction;
        changeSupport.firePropertyChange(Fields.direction, old, direction);
    }

    public void setGradientDirection(Direction gradientDirection) {
        Direction old = this.gradientDirection;
        this.gradientDirection = gradientDirection;
        changeSupport.firePropertyChange(Fields.gradientDirection, old, gradientDirection);
    }

    public void setGradientColor(Color gradientColor) {
        Color old = this.gradientColor;
        this.gradientColor = gradientColor;
        changeSupport.firePropertyChange(Fields.gradientColor, old, gradientColor);
    }

    public void setFillColor(Color fillColor) {
        Color old = this.fillColor;
        this.fillColor = fillColor;
        changeSupport.firePropertyChange(Fields.fillColor, old, fillColor);
    }

    public void setRotation(double rotation) {
        double old = this.rotation;
        this.rotation = rotation;
        changeSupport.firePropertyChange(Fields.rotation, old, rotation);
    }

    public void setOpacity(float opacity) {
        float old = this.opacity;
        this.opacity = opacity;
        changeSupport.firePropertyChange(Fields.opacity, old, opacity);
    }

    public void setFillOpacity(float fillOpacity) {
        float old = this.fillOpacity;
        this.fillOpacity = fillOpacity;
        changeSupport.firePropertyChange(Fields.fillOpacity, old, fillOpacity);
    }

    public void setStrokeOpacity(float strokeOpacity) {
        float old = this.strokeOpacity;
        this.strokeOpacity = strokeOpacity;
        changeSupport.firePropertyChange(Fields.strokeOpacity, old, strokeOpacity);
    }

    public void setStrokeWidth(float strokeWidth) {
        float old = this.strokeWidth;
        this.strokeWidth = strokeWidth;
        changeSupport.firePropertyChange(Fields.strokeWidth, old, strokeWidth);
    }

    public void setArcSize(Double arcSize) {
        Double old = this.arcSize;
        this.arcSize = arcSize;
        changeSupport.firePropertyChange(Fields.arcSize, old, arcSize);
    }

    public void setStrokeColor(Color strokeColor) {
        Color old = this.strokeColor;
        this.strokeColor = strokeColor;
        changeSupport.firePropertyChange(Fields.strokeColor, old, strokeColor);
    }

    public void setSeparatorColor(Color separatorColor) {
        Color old = this.separatorColor;
        this.separatorColor = separatorColor;
        changeSupport.firePropertyChange(Fields.separatorColor, old, separatorColor);
    }

    public void setStartFill(boolean startFill) {
        boolean old = this.startFill;
        this.startFill = startFill;
        changeSupport.firePropertyChange(Fields.startFill, old, startFill);
    }

    public void setEndFill(boolean endFill) {
        boolean old = this.endFill;
        this.endFill = endFill;
        changeSupport.firePropertyChange(Fields.endFill, old, endFill);
    }

    public void setFlipVertical(boolean flipVertical) {
        boolean old = this.flipVertical;
        this.flipVertical = flipVertical;
        changeSupport.firePropertyChange(Fields.flipVertical, old, flipVertical);
    }

    public void setFlipHorizontal(boolean flipHorizontal) {
        boolean old = this.flipHorizontal;
        this.flipHorizontal = flipHorizontal;
        changeSupport.firePropertyChange(Fields.flipHorizontal, old, flipHorizontal);
    }
}
