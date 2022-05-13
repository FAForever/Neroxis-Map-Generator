package com.faforever.neroxis.generator.graph.domain;

import java.util.Comparator;
import java.util.Objects;

public class MaskMethodEdgeComparator implements Comparator<MaskMethodEdge> {
    @Override
    public int compare(MaskMethodEdge o1, MaskMethodEdge o2) {
        if (o1 == o2 || (Objects.equals(o1.getResultName(), o2.getResultName()) && Objects.equals(o1.getParameterName(),
                                                                                                  o2.getParameterName()))) {
            return 0;
        }

        if (!Objects.equals(o1.getResultName(), o2.getResultName())) {
            if (o1.getResultName().equals(MaskGraphVertex.SELF)) {
                return 1;
            }

            if (o2.getResultName().equals(MaskGraphVertex.SELF)) {
                return -1;
            }
        }

        if (!Objects.equals(o1.getParameterName(), o2.getParameterName())) {
            if (o1.getParameterName().equals(MaskMethodVertex.EXECUTOR)) {
                return 1;
            }

            if (o2.getParameterName().equals(MaskMethodVertex.EXECUTOR)) {
                return -1;
            }
        }

        return 0;
    }
}
