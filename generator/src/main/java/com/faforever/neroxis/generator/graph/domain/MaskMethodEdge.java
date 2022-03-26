package com.faforever.neroxis.generator.graph.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public strictfp class MaskMethodEdge {
    private final String resultName;
    private final String parameterName;

    public String toString() {
        return resultName;
    }
}
