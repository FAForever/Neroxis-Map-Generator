package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.util.Pipeline;

public record EntryVertex(Pipeline.Entry entry) {
    public String toString() {
        return String.valueOf(entry.getIndex());
    }
}
