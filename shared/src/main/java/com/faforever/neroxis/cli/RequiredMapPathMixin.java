package com.faforever.neroxis.cli;

import lombok.Getter;
import picocli.CommandLine;

import java.nio.file.Path;

import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

public class RequiredMapPathMixin {
    @Spec
    private CommandLine.Model.CommandSpec spec;
    @Getter
    private Path mapPath;

    @Option(names = "--map-path", required = true, description = "Path to map folder")
    public void setMapPath(Path mapPath) {
        CLIUtils.checkValidMapFolder(mapPath, spec);
        this.mapPath = mapPath;
    }
}
