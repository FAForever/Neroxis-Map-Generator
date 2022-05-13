package com.faforever.neroxis.util.serial.graph;

import com.dslplatform.json.CompiledJson;
import java.util.Map;
import lombok.Value;

@Value
@CompiledJson
public class JsonGraph {
    Map<Integer, JsonGraphVertex> vertices;
    Map<JsonGraphEdge, SourceTarget> edges;

    @Value
    public static class SourceTarget {
        int source;
        int target;
    }
}
