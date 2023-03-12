package com.faforever.neroxis.util.serial.graph;

import com.dslplatform.json.CompiledJson;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
@CompiledJson
public class JsonGraph {
    private final Map<Integer, JsonGraphVertex> vertices;
    private final Map<JsonGraphEdge, SourceTarget> edges;

    public record SourceTarget(int source, int target) {}
}
