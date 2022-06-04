package com.faforever.neroxis.util.serial.graph;

import com.dslplatform.json.CompiledJson;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Value;

@Getter
@Setter
@RequiredArgsConstructor
@CompiledJson
public class JsonGraph {
    private final Map<Integer, JsonGraphVertex> vertices;
    private final Map<JsonGraphEdge, SourceTarget> edges;

    @Value
    public static class SourceTarget {
        int source;
        int target;
    }
}
