package com.faforever.neroxis.map;

import io.avaje.jsonb.Json;

@Json
public record CubeMap(
        String name,
        String path
) {}

