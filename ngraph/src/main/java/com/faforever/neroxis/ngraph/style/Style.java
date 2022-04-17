package com.faforever.neroxis.ngraph.style;

import lombok.Value;

@Value
public class Style {
    LabelStyle label = new LabelStyle();
    EdgeStyle edge = new EdgeStyle();
    IndicatorStyle indicator = new IndicatorStyle();
    StencilStyle stencil = new StencilStyle();
    ImageStyle image = new ImageStyle();
    PerimeterStyle perimeter = new PerimeterStyle();
    SwimlaneStyle swimlane = new SwimlaneStyle();
    ShapeStyle shape = new ShapeStyle();
    CellProperties cellProperties = new CellProperties();

    public Style copy() {
        StyleMapper styleMapper = StyleMapper.INSTANCE;
        Style copy = new Style();
        styleMapper.copy(label, copy.label);
        styleMapper.copy(edge, copy.edge);
        styleMapper.copy(indicator, copy.indicator);
        styleMapper.copy(stencil, copy.stencil);
        styleMapper.copy(image, copy.image);
        styleMapper.copy(perimeter, copy.perimeter);
        styleMapper.copy(swimlane, copy.swimlane);
        styleMapper.copy(shape, copy.shape);
        styleMapper.copy(cellProperties, copy.cellProperties);
        return copy;
    }
}
