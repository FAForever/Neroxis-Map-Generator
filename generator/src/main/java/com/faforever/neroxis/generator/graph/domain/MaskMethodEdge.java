package com.faforever.neroxis.generator.graph.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public strictfp class MaskMethodEdge {
    String resultName;
    String parameterName;
}
