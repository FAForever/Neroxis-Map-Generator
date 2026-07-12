package com.faforever.neroxis.generator.util.serial;

import java.util.Objects;

public record GeneratorParseOutput(
        MapNameParameters parameters,
        String mapName
) {

    public GeneratorParseOutput(MapNameParameters parameters) {
        this(parameters, GeneratedMapNameEncoder.encode(parameters));
    }

    public GeneratorParseOutput {
        if (!Objects.equals(GeneratedMapNameEncoder.encode(parameters), mapName)) {
            throw new IllegalArgumentException("Map name does not match parameters");
        }
    }
}
