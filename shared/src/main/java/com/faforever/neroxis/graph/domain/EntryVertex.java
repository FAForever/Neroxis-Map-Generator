package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.util.Pipeline;
import lombok.Value;

@Value
public strictfp class EntryVertex {
    Pipeline.Entry entry;

    public String toString() {
        return String.valueOf(entry.getIndex());
    }
}
