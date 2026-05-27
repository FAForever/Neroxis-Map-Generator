package com.faforever.neroxis.cli;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

import static picocli.CommandLine.Option;

@Getter
@Setter
public class OutputFolderMixin {
    @Option(names = {"--out-path", "--folder-path"}, order = 1, description = "Folder to save the map to", defaultValue = ".")
    private @Nullable Path outputPath;
}
