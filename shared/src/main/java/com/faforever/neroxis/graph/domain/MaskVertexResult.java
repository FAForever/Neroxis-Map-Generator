package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.mask.Mask;
import lombok.Value;

@Value
public strictfp class MaskVertexResult {
    String parameterName;
    String resultName;
    MaskGraphVertex<?> sourceVertex;

    public Mask<?, ?> getResult() {
        if (MaskMethodVertex.EXECUTOR.equals(parameterName)) {
            return sourceVertex.getResult(resultName);
        } else {
            return sourceVertex.getImmutableResult(resultName);
        }
    }

    public Class<? extends Mask<?, ?>> getResultClass() {
        return sourceVertex.getResultClass(resultName);
    }
}
