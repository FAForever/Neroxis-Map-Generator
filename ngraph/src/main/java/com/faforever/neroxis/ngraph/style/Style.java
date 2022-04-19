package com.faforever.neroxis.ngraph.style;

import com.faforever.neroxis.ngraph.style.util.StyleMapper;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import lombok.Data;

@Data
public class Style implements PropertyChangeListener {
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private final LabelStyle label = new LabelStyle();
    private final EdgeStyle edge = new EdgeStyle();
    private final IndicatorStyle indicator = new IndicatorStyle();
    private final StencilStyle stencil = new StencilStyle();
    private final ImageStyle image = new ImageStyle();
    private final PerimeterStyle perimeter = new PerimeterStyle();
    private final SwimlaneStyle swimlane = new SwimlaneStyle();
    private final ShapeStyle shape = new ShapeStyle();
    private final CellProperties cellProperties = new CellProperties();
    private final Style parentStyle;

    public Style(Style parentStyle) {
        this.parentStyle = parentStyle;
        if (this.parentStyle != null) {
            initializeFromParent(parentStyle);
        }
        addSelfListener();
    }

    public void resetToParent() {
        transferState(parentStyle);
    }

    private void initializeFromParent(Style parentStyle) {
        transferState(parentStyle);
        this.parentStyle.label.addPropertyChangeListener(label);
        this.parentStyle.edge.addPropertyChangeListener(edge);
        this.parentStyle.indicator.addPropertyChangeListener(indicator);
        this.parentStyle.stencil.addPropertyChangeListener(stencil);
        this.parentStyle.perimeter.addPropertyChangeListener(perimeter);
        this.parentStyle.swimlane.addPropertyChangeListener(swimlane);
        this.parentStyle.cellProperties.addPropertyChangeListener(cellProperties);
        this.parentStyle.image.addPropertyChangeListener(image);
        this.parentStyle.shape.addPropertyChangeListener(shape);
    }

    private void addSelfListener() {
        label.addPropertyChangeListener(this);
        edge.addPropertyChangeListener(this);
        indicator.addPropertyChangeListener(this);
        stencil.addPropertyChangeListener(this);
        perimeter.addPropertyChangeListener(this);
        swimlane.addPropertyChangeListener(this);
        cellProperties.addPropertyChangeListener(this);
        image.addPropertyChangeListener(this);
        shape.addPropertyChangeListener(this);
    }

    private void clearParentListeners() {
        if (this.parentStyle != null) {
            this.parentStyle.label.removePropertyChangeListener(this.label);
            this.parentStyle.edge.removePropertyChangeListener(this.edge);
            this.parentStyle.indicator.removePropertyChangeListener(this.indicator);
            this.parentStyle.stencil.removePropertyChangeListener(this.stencil);
            this.parentStyle.perimeter.removePropertyChangeListener(this.perimeter);
            this.parentStyle.swimlane.removePropertyChangeListener(this.swimlane);
            this.parentStyle.cellProperties.removePropertyChangeListener(this.cellProperties);
            this.parentStyle.image.removePropertyChangeListener(this.image);
            this.parentStyle.shape.removePropertyChangeListener(this.shape);
        }
    }

    private void transferState(Style source) {
        StyleMapper styleMapper = StyleMapper.INSTANCE;
        styleMapper.copy(source.label, label);
        styleMapper.copy(source.edge, edge);
        styleMapper.copy(source.indicator, indicator);
        styleMapper.copy(source.stencil, stencil);
        styleMapper.copy(source.image, image);
        styleMapper.copy(source.perimeter, perimeter);
        styleMapper.copy(source.swimlane, swimlane);
        styleMapper.copy(source.shape, shape);
        styleMapper.copy(source.cellProperties, cellProperties);
    }

    public Style spawnChild() {
        return new Style(this);
    }

    public Style copy() {
        Style copy = new Style(null);
        copy.transferState(this);
        return copy;
    }

    public Style getParent() {
        return parentStyle.copy();
    }

    public void clearListeners() {
        Arrays.stream(changeSupport.getPropertyChangeListeners()).forEach(changeSupport::removePropertyChangeListener);
        clearParentListeners();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        changeSupport.firePropertyChange("styleChange", null, null);
    }
}
