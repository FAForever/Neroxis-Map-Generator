package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.shape.ITextShape;
import com.faforever.neroxis.ngraph.style.util.FontModifier;
import com.faforever.neroxis.ngraph.style.util.HorizontalAlignment;
import com.faforever.neroxis.ngraph.style.util.Overflow;
import com.faforever.neroxis.ngraph.style.util.VerticalAlignment;
import com.faforever.neroxis.ngraph.style.util.WhiteSpace;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(level = AccessLevel.PRIVATE)
public class LabelStyle implements PropertyChangeListener {

    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private WhiteSpace whiteSpace = WhiteSpace.NO_WRAP;
    private Overflow overflow = Overflow.VISIBLE;
    private ITextShape textShape;
    private int topSpacing;
    private int bottomSpacing;
    private int leftSpacing;
    private int rightSpacing;
    private Color borderColor;
    private Color backgroundColor;
    private VerticalAlignment verticalAlignment = VerticalAlignment.MIDDLE;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.CENTER;
    private VerticalAlignment verticalAlignmentPosition = VerticalAlignment.MIDDLE;
    private HorizontalAlignment horizontalAlignmentPosition = HorizontalAlignment.CENTER;
    private boolean visible = true;
    private float textOpacity = 100;
    private Set<FontModifier> fontModifiers = new HashSet<>();
    private int fontSize = 11;
    private String fontFamily = "Arial,Helvetica";
    private Color textColor = Color.BLACK;

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
        if (propertyName.equals(Fields.whiteSpace) && Objects.equals(oldValue, whiteSpace)) {
            setWhiteSpace((WhiteSpace) newValue);
        } else if (propertyName.equals(Fields.overflow) && Objects.equals(oldValue, overflow)) {
            setOverflow((Overflow) newValue);
        } else if (propertyName.equals(Fields.textShape) && Objects.equals(oldValue, textShape)) {
            setTextShape((ITextShape) newValue);
        } else if (propertyName.equals(Fields.topSpacing) && Objects.equals(oldValue, topSpacing)) {
            setTopSpacing((int) newValue);
        } else if (propertyName.equals(Fields.bottomSpacing) && Objects.equals(oldValue, bottomSpacing)) {
            setBottomSpacing((int) newValue);
        } else if (propertyName.equals(Fields.leftSpacing) && Objects.equals(oldValue, leftSpacing)) {
            setLeftSpacing((int) newValue);
        } else if (propertyName.equals(Fields.rightSpacing) && Objects.equals(oldValue, rightSpacing)) {
            setRightSpacing((int) newValue);
        } else if (propertyName.equals(Fields.borderColor) && Objects.equals(oldValue, borderColor)) {
            setBorderColor((Color) newValue);
        } else if (propertyName.equals(Fields.backgroundColor) && Objects.equals(oldValue, backgroundColor)) {
            setBackgroundColor((Color) newValue);
        } else if (propertyName.equals(Fields.verticalAlignment) && Objects.equals(oldValue, verticalAlignment)) {
            setVerticalAlignment((VerticalAlignment) newValue);
        } else if (propertyName.equals(Fields.horizontalAlignment) && Objects.equals(oldValue, horizontalAlignment)) {
            setHorizontalAlignment((HorizontalAlignment) newValue);
        } else if (propertyName.equals(Fields.verticalAlignmentPosition) && Objects.equals(oldValue,
                                                                                           verticalAlignmentPosition)) {
            setVerticalAlignmentPosition((VerticalAlignment) newValue);
        } else if (propertyName.equals(Fields.horizontalAlignmentPosition) && Objects.equals(oldValue,
                                                                                             horizontalAlignmentPosition)) {
            setHorizontalAlignmentPosition((HorizontalAlignment) newValue);
        } else if (propertyName.equals(Fields.visible) && Objects.equals(oldValue, visible)) {
            setVisible((boolean) newValue);
        } else if (propertyName.equals(Fields.textOpacity) && Objects.equals(oldValue, textOpacity)) {
            setTextOpacity((float) newValue);
        } else if (propertyName.equals(Fields.fontModifiers) && Objects.equals(oldValue, fontModifiers)) {
            setFontModifiers((Set<FontModifier>) newValue);
        } else if (propertyName.equals(Fields.fontSize) && Objects.equals(oldValue, fontSize)) {
            setFontSize((int) newValue);
        } else if (propertyName.equals(Fields.fontFamily) && Objects.equals(oldValue, fontFamily)) {
            setFontFamily((String) newValue);
        } else if (propertyName.equals(Fields.textColor) && Objects.equals(oldValue, textColor)) {
            setTextColor((Color) newValue);
        }
    }

    public void setWhiteSpace(WhiteSpace whiteSpace) {
        WhiteSpace old = this.whiteSpace;
        this.whiteSpace = whiteSpace;
        changeSupport.firePropertyChange(Fields.whiteSpace, old, whiteSpace);
    }

    public void setOverflow(Overflow overflow) {
        Overflow old = this.overflow;
        this.overflow = overflow;
        changeSupport.firePropertyChange(Fields.overflow, old, overflow);
    }

    public void setTextShape(ITextShape textShape) {
        ITextShape old = this.textShape;
        this.textShape = textShape;
        changeSupport.firePropertyChange(Fields.textShape, old, textShape);
    }

    public void setTopSpacing(int topSpacing) {
        int old = this.topSpacing;
        this.topSpacing = topSpacing;
        changeSupport.firePropertyChange(Fields.topSpacing, old, topSpacing);
    }

    public void setBottomSpacing(int bottomSpacing) {
        int old = this.bottomSpacing;
        this.bottomSpacing = bottomSpacing;
        changeSupport.firePropertyChange(Fields.bottomSpacing, old, bottomSpacing);
    }

    public void setLeftSpacing(int leftSpacing) {
        int old = this.leftSpacing;
        this.leftSpacing = leftSpacing;
        changeSupport.firePropertyChange(Fields.leftSpacing, old, leftSpacing);
    }

    public void setRightSpacing(int rightSpacing) {
        int old = this.rightSpacing;
        this.rightSpacing = rightSpacing;
        changeSupport.firePropertyChange(Fields.rightSpacing, old, rightSpacing);
    }

    public void setBorderColor(Color borderColor) {
        Color old = this.borderColor;
        this.borderColor = borderColor;
        changeSupport.firePropertyChange(Fields.borderColor, old, borderColor);
    }

    public void setBackgroundColor(Color backgroundColor) {
        Color old = this.backgroundColor;
        this.backgroundColor = backgroundColor;
        changeSupport.firePropertyChange(Fields.backgroundColor, old, backgroundColor);
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

    public void setVerticalAlignmentPosition(VerticalAlignment verticalAlignmentPosition) {
        VerticalAlignment old = this.verticalAlignmentPosition;
        this.verticalAlignmentPosition = verticalAlignmentPosition;
        changeSupport.firePropertyChange(Fields.verticalAlignmentPosition, old, verticalAlignmentPosition);
    }

    public void setHorizontalAlignmentPosition(HorizontalAlignment horizontalAlignmentPosition) {
        HorizontalAlignment old = this.horizontalAlignmentPosition;
        this.horizontalAlignmentPosition = horizontalAlignmentPosition;
        changeSupport.firePropertyChange(Fields.horizontalAlignmentPosition, old, horizontalAlignmentPosition);
    }

    public void setVisible(boolean visible) {
        boolean old = this.visible;
        this.visible = visible;
        changeSupport.firePropertyChange(Fields.visible, old, visible);
    }

    public void setTextOpacity(float textOpacity) {
        float old = this.textOpacity;
        this.textOpacity = textOpacity;
        changeSupport.firePropertyChange(Fields.textOpacity, old, textOpacity);
    }

    public void setFontModifiers(Set<FontModifier> fontModifiers) {
        Set<FontModifier> old = this.fontModifiers;
        this.fontModifiers = fontModifiers;
        changeSupport.firePropertyChange(Fields.fontModifiers, old, fontModifiers);
    }

    public void setFontSize(int fontSize) {
        int old = this.fontSize;
        this.fontSize = fontSize;
        changeSupport.firePropertyChange(Fields.fontSize, old, fontSize);
    }

    public void setFontFamily(String fontFamily) {
        String old = this.fontFamily;
        this.fontFamily = fontFamily;
        changeSupport.firePropertyChange(Fields.fontFamily, old, fontFamily);
    }

    public void setTextColor(Color textColor) {
        Color old = this.textColor;
        this.textColor = textColor;
        changeSupport.firePropertyChange(Fields.textColor, old, textColor);
    }
}
