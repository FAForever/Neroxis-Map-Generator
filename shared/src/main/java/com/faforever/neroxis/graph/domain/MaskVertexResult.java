package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.mask.Mask;
import lombok.Value;

@Value
public strictfp class MaskVertexResult {
    String resultName;
    MaskGraphVertex<?> sourceVertex;

    public Mask<?, ?> getResult() {
        return sourceVertex.getResult(resultName);
    }

    public Class<? extends Mask<?, ?>> getResultClass() {
        return sourceVertex.getResultClass(resultName);
    }
}
