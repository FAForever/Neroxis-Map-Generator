package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.CLIUtils;
import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.OutputFolderMixin;
import com.faforever.neroxis.cli.RequiredMapPathMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.exporter.MapExporter;
import com.faforever.neroxis.importer.MapImporter;
import com.faforever.neroxis.map.SCMap;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Command(name = "resize-stratum", mixinStandardHelpOptions = true, description = "Change the map stratum size", versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public class MapStratumResizer implements Callable<Integer> {
    @Spec
    private CommandLine.Model.CommandSpec spec;
    @Mixin
    private RequiredMapPathMixin requiredMapPathMixin;
    @Mixin
    private OutputFolderMixin outputFolderMixin;
    @Mixin
    private DebugMixin debugMixin;
    private int stratumSize;

    @Option(names = "--stratum-size", required = true, description = "New stratum size, can be specified in pixels (e.g. 512)")
    private void setStratumSize(String mapSizeString) {
        this.stratumSize = CLIUtils.convertMapSizeString(mapSizeString, CLIUtils.MapSizeStrictness.POWER_OF_2, spec);
    }

    @Override
    public Integer call() throws Exception {
        SCMap map = MapImporter.importMap(requiredMapPathMixin.getMapPath());
        map.changeStratumSize(stratumSize);
        MapExporter.exportMap(outputFolderMixin.getOutputPath(), map, false, false);
        return 0;
    }

}
