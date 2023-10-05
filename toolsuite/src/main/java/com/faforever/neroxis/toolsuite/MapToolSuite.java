package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.VersionProvider;
import picocli.CommandLine;

import static picocli.CommandLine.Command;

@Command(name = "maptools", mixinStandardHelpOptions = true, description = "Tools to modify maps", versionProvider = VersionProvider.class, usageHelpAutoWidth = true, synopsisSubcommandLabel = "COMMAND", subcommands = {
        MapPopulator.class, MapResizer.class, MapStratumResizer.class, MapForcer.class, MapEvaluator.class, MapEnvTextureExporter.class})
public class MapToolSuite {
    private MapToolSuite() {
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new MapToolSuite());
        commandLine.setAbbreviatedOptionsAllowed(true);
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
