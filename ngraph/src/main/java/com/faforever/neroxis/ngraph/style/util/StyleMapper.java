package com.faforever.neroxis.ngraph.style.util;

import com.faforever.neroxis.ngraph.style.CellProperties;
import com.faforever.neroxis.ngraph.style.EdgeStyle;
import com.faforever.neroxis.ngraph.style.ImageStyle;
import com.faforever.neroxis.ngraph.style.IndicatorStyle;
import com.faforever.neroxis.ngraph.style.LabelStyle;
import com.faforever.neroxis.ngraph.style.PerimeterStyle;
import com.faforever.neroxis.ngraph.style.ShapeStyle;
import com.faforever.neroxis.ngraph.style.StencilStyle;
import com.faforever.neroxis.ngraph.style.SwimlaneStyle;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StyleMapper {

    StyleMapper INSTANCE = Mappers.getMapper(StyleMapper.class);

    void copy(EdgeStyle source, @MappingTarget EdgeStyle target);

    void copy(StencilStyle source, @MappingTarget StencilStyle target);

    void copy(LabelStyle source, @MappingTarget LabelStyle target);

    void copy(IndicatorStyle source, @MappingTarget IndicatorStyle target);

    void copy(ImageStyle source, @MappingTarget ImageStyle target);

    void copy(PerimeterStyle source, @MappingTarget PerimeterStyle target);

    void copy(SwimlaneStyle source, @MappingTarget SwimlaneStyle target);

    void copy(ShapeStyle source, @MappingTarget ShapeStyle target);

    void copy(CellProperties source, @MappingTarget CellProperties target);
}
