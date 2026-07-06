package com.faforever.neroxis.cli;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

import static picocli.CommandLine.Option;

@Getter
@Setter
public class RequiredMapPathMixin {
    @Option(names = "--map-path", required = true, description = "Map folder containing map to modify", converter = MapFolderConverter.class)
    private @Nullable Path mapPath;
}
