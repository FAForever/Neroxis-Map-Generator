package com.faforever.neroxis.util.serial.graph;

import com.dslplatform.json.CompiledJson;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@CompiledJson
public class JsonGraphVertex {
    private final String identifier;
    private final String clazz;
    private final String maskClass;
    private final List<String> parameterClasses;
    private final List<String> parameterValues;
    private final String executable;
}
