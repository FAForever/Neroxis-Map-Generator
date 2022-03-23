package com.faforever.neroxis.cli;

import lombok.Getter;
import picocli.CommandLine;

import java.nio.file.Path;

import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

public class OutputFolderMixin {

    @Spec
    private CommandLine.Model.CommandSpec spec;

    @Getter
    private Path outputPath;

    @Option(names = {"--out-path", "--folder-path"}, order = 1, description = "Folder to save the map to", defaultValue = ".")
    public void setOutputPath(Path outputPath) {
        CLIUtils.checkWritableDirectory(outputPath, spec);
        this.outputPath = outputPath;
    }
}
