package com.faforever.neroxis.generator.graph.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public strictfp class MaskMethodEdge {

    private final String resultName;
    private final String parameterName;

    public MaskMethodEdge copy() {
        return new MaskMethodEdge(resultName, parameterName);
    }
}
