package com.faforever.neroxis.graph.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.jgrapht.graph.DefaultEdge;

@Getter
@AllArgsConstructor
@ToString
public strictfp class MaskMethodEdge extends DefaultEdge {
    private final String resultName;
    private final String parameterName;

    @Override
    public MaskGraphVertex<?> getSource() {
        return (MaskGraphVertex<?>) super.getSource();
    }

    @Override
    public MaskGraphVertex<?> getTarget() {
        return (MaskGraphVertex<?>) super.getTarget();
    }

    public MaskMethodEdge copy() {
        return new MaskMethodEdge(resultName, parameterName);
    }
}
