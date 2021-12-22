package com.faforever.neroxis.graph;

import com.faforever.neroxis.util.Pipeline;
import lombok.Value;

@Value
public class EntryVertex {
    Pipeline.Entry entry;

    public String toString() {
        return String.valueOf(entry.getIndex());
    }
}
