package com.faforever.neroxis.generator.serial;

import com.dslplatform.json.CompiledJson;
import com.faforever.neroxis.util.serial.graph.JsonGraph;
import com.faforever.neroxis.util.serial.graph.JsonGraphEdge;
import com.faforever.neroxis.util.serial.graph.JsonGraphVertex;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@CompiledJson
public class JsonPipeline extends JsonGraph {
    private final String generatorClass;
    private final Map<String, Integer> endpointVertexMap;

    public JsonPipeline(Map<Integer, JsonGraphVertex> vertices, Map<JsonGraphEdge, SourceTarget> edges,
                        String generatorClass, Map<String, Integer> endpointVertexMap) {
        super(vertices, edges);
        this.generatorClass = generatorClass;
        this.endpointVertexMap = endpointVertexMap;
    }
}
