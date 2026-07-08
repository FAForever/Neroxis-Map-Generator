package com.faforever.neroxis.generator.util.serial;

import com.faforever.neroxis.generator.GeneratedMapNameEncoder;

import java.util.Objects;

public record GeneratorParseOutput(
        GeneratorParameters parameters,
        String mapName
) {

    public GeneratorParseOutput(GeneratorParameters parameters) {
        this(parameters, GeneratedMapNameEncoder.encode(parameters));
    }

    public GeneratorParseOutput {
        if (!Objects.equals(GeneratedMapNameEncoder.encode(parameters), mapName)) {
            throw new IllegalArgumentException("Map name does not match parameters");
        }
    }
}
