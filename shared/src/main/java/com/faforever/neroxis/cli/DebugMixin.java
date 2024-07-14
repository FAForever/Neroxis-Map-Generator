package com.faforever.neroxis.cli;

import com.faforever.neroxis.util.DebugUtil;

import static picocli.CommandLine.Option;

public class DebugMixin {
    @Option(names = "--debug", description = "Enable debugging")
    public void setDebugging(boolean debug) {
        DebugUtil.DEBUG = debug;
    }

    @Option(names = "--visualize", description = "Enable visualization", split = ",")
    public void setVizualize(String... masksToVisualize) {
        DebugUtil.allowVisualization();
        for (String maskName : masksToVisualize) {
            DebugUtil.visualizeMask(maskName);
        }
    }
}
