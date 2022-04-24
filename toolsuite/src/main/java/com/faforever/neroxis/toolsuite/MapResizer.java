package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.CLIUtils;
import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.OutputFolderMixin;
import com.faforever.neroxis.cli.RequiredMapPathMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.exporter.MapExporter;
import com.faforever.neroxis.importer.MapImporter;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.toolsuite.cli.LocationOptions;
import com.faforever.neroxis.util.vector.Vector2;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import static picocli.CommandLine.ArgGroup;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Command(name = "resize", mixinStandardHelpOptions = true, description = "Change the map size", versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public strictfp class MapResizer implements Callable<Integer> {

    @Spec
    private CommandLine.Model.CommandSpec spec;
    @Mixin
    private RequiredMapPathMixin requiredMapPathMixin;
    @Mixin
    private OutputFolderMixin outputFolderMixin;
    @Mixin
    private DebugMixin debugMixin;
    @ArgGroup(exclusive = false, heading = "X and Y coordinate to place the center of the map content, default is the center of the new map size%n")
    private LocationOptions locationOptions;
    private int newMapSize;
    private int scaledSize;

    @Option(names = "--scaled-size", required = true, description = "Size to scale the map content to, can be specified in oGrids (e.g 512) or km (e.g 10km)")
    private void setScaledSize(String mapSizeString) {
        this.scaledSize = CLIUtils.convertMapSizeString(mapSizeString, CLIUtils.MapSizeStrictness.NONE, spec);
    }

    @Option(names = "--map-size", required = true, description = "New map size, can be specified in oGrids (e.g 512) or km (e.g 10km), must result in a power of 2 in oGrids")
    private void setNewMapSize(String mapSizeString) {
        this.newMapSize = CLIUtils.convertMapSizeString(mapSizeString, CLIUtils.MapSizeStrictness.POWER_OF_2, spec);
    }

    @Override
    public Integer call() throws Exception {
        SCMap map = MapImporter.importMap(requiredMapPathMixin.getMapPath());
        resizeMap(map);
        MapExporter.exportMap(outputFolderMixin.getOutputPath(), map, true, false);
        return 0;
    }

    private void resizeMap(SCMap map) {
        Vector2 location = locationOptions.getLocation();
        location = location == null ? new Vector2(newMapSize / 2f, newMapSize / 2f) : location;
        map.changeMapSize(scaledSize, newMapSize, location);
    }
}
