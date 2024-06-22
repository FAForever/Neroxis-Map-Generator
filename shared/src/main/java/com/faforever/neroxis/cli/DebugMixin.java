package com.faforever.neroxis.cli;

import com.faforever.neroxis.util.DebugUtil;

import static picocli.CommandLine.Option;

public class DebugMixin {
    @Option(names = "--debug", description = "Enable debugging")
    public void setDebugging(boolean debug) {
        DebugUtil.DEBUG = debug;
    }

    @Option(names = "--visualize", description = "Enable visualization")
    public void setVizualize(boolean visualize) {
        DebugUtil.VISUALIZE = visualize;
    }
}
