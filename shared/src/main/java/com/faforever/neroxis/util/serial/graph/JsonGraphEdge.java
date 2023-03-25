package com.faforever.neroxis.util.serial.graph;

import com.dslplatform.json.CompiledJson;

@CompiledJson
public record JsonGraphEdge(String resultName, String parameterName) {
}
