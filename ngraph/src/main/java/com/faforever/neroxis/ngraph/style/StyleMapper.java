package com.faforever.neroxis.ngraph.style;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.control.DeepClone;
import org.mapstruct.control.NoComplexMapping;
import org.mapstruct.factory.Mappers;

@Mapper(mappingControl = DeepClone.class)
public interface StyleMapper {
    StyleMapper INSTANCE = Mappers.getMapper(StyleMapper.class);

    @Mapping(source = "edgeStyleFunction", target = "edgeStyleFunction", mappingControl = NoComplexMapping.class)
    @Mapping(source = "loopStyleFunction", target = "loopStyleFunction", mappingControl = NoComplexMapping.class)
    @Mapping(source = "startArrow", target = "startArrow", mappingControl = NoComplexMapping.class)
    @Mapping(source = "endArrow", target = "endArrow", mappingControl = NoComplexMapping.class)
    @Mapping(source = "sourcePort", target = "sourcePort", mappingControl = NoComplexMapping.class)
    @Mapping(source = "targetPort", target = "targetPort", mappingControl = NoComplexMapping.class)
    void copy(EdgeStyle source, @MappingTarget EdgeStyle target);

    void copy(StencilStyle source, @MappingTarget StencilStyle target);

    @Mapping(source = "textShape", target = "textShape", mappingControl = NoComplexMapping.class)
    void copy(LabelStyle source, @MappingTarget LabelStyle target);

    @Mapping(source = "shape", target = "shape", mappingControl = NoComplexMapping.class)
    void copy(IndicatorStyle source, @MappingTarget IndicatorStyle target);

    void copy(ImageStyle source, @MappingTarget ImageStyle target);

    @Mapping(source = "perimeter", target = "perimeter", mappingControl = NoComplexMapping.class)
    void copy(PerimeterStyle source, @MappingTarget PerimeterStyle target);

    void copy(SwimlaneStyle source, @MappingTarget SwimlaneStyle target);

    @Mapping(source = "shape", target = "shape", mappingControl = NoComplexMapping.class)
    void copy(ShapeStyle source, @MappingTarget ShapeStyle target);

    void copy(CellProperties source, @MappingTarget CellProperties target);
}
