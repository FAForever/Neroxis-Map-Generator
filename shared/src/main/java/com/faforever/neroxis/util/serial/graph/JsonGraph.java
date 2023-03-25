package com.faforever.neroxis.util.serial.graph;

import com.dslplatform.json.CompiledJson;
import lombok.Getter;

import java.util.Map;

@Getter
@CompiledJson
public class JsonGraph {
    private final Map<Integer, JsonGraphVertex> vertices;
    private final Map<JsonGraphEdge, SourceTarget> edges;

    public JsonGraph(Map<Integer, JsonGraphVertex> vertices, Map<JsonGraphEdge, SourceTarget> edges) {
        this.vertices = Map.copyOf(vertices);
        this.edges = Map.copyOf(edges);
    }

    public record SourceTarget(int source, int target) {}
}
