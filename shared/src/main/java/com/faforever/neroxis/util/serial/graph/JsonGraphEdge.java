package com.faforever.neroxis.util.serial.graph;

import com.dslplatform.json.CompiledJson;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@CompiledJson
public class JsonGraphEdge {
    private final String resultName;
    private final String parameterName;
}
