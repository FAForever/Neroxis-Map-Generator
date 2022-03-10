package com.faforever.neroxis.graph.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public strictfp class MaskMethodEdge {
    private final String resultName;
    private final String parameterName;
}
