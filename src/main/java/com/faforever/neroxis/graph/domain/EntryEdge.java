package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.util.Pipeline;
import lombok.Value;

@Value
public class EntryEdge {
    EntryVertex from;
    EntryVertex to;
    Pipeline.Entry entry;

    public String toString() {
        return String.format("%d -> %d", from.getEntry().getIndex(), to.getEntry().getIndex());
    }
}
