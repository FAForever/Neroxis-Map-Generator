package com.faforever.neroxis.cli;

import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

@Getter
@Setter
public class VisualizeMixin {
    @CommandLine.Option(names = "--visualize", description = "Enable visualization", negatable = true)
    private boolean visualize;
}
