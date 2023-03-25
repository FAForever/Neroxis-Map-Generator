package com.faforever.neroxis.util.serial.graph;

import com.dslplatform.json.CompiledJson;

import java.util.List;

@CompiledJson
public record JsonGraphVertex(String identifier,
                              String clazz,
                              String maskClass,
                              List<String> parameterClasses,
                              List<String> parameterValues,
                              String executable) {}
