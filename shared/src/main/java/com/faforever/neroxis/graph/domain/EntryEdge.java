package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.util.Pipeline;

public record EntryEdge(EntryVertex from, EntryVertex to, Pipeline.Entry entry) {
    public String toString() {
        return String.format("%d -> %d", from.entry().getIndex(), to.entry().getIndex());
    }
}
