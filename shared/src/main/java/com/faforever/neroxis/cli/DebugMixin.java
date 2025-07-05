package com.faforever.neroxis.cli;

import lombok.Getter;
import lombok.Setter;

import static picocli.CommandLine.Option;

@Getter
@Setter
public class DebugMixin {
    @Option(names = "--debug", description = "Enable debugging", negatable = true)
    private boolean debug;
}
