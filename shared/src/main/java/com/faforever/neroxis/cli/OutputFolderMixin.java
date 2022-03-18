package com.faforever.neroxis.cli;

import lombok.Getter;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;

import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

public class OutputFolderMixin {

    @Spec
    private CommandLine.Model.CommandSpec spec;

    @Getter
    private Path outputPath;

    @Option(names = {"--out-path", "--folder-path"}, required = true, description = "Folder to save the map to")
    public void setOutputPath(Path outputPath) throws IOException {
        CLIUtils.checkWritableDirectory(outputPath, spec);
        this.outputPath = outputPath;
    }
}
