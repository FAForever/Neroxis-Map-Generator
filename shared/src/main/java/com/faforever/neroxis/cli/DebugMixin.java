package com.faforever.neroxis.cli;

import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

import static picocli.CommandLine.Option;

public class DebugMixin {

    @Option(names = "--debug", description = "Enable debugging")
    public void setDebugging(boolean debug) {
        DebugUtil.DEBUG = debug;
        Pipeline.HASH_MASK = debug;
    }
}
