package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.MapSizeConverter;
import com.faforever.neroxis.cli.OutputFolderMixin;
import com.faforever.neroxis.cli.PowerOfTwoMapSizeConverter;
import com.faforever.neroxis.cli.RequiredMapPathMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.exporter.MapExporter;
import com.faforever.neroxis.importer.MapImporter;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.toolsuite.cli.LocationOptions;
import com.faforever.neroxis.util.vector.Vector2;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static picocli.CommandLine.ArgGroup;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Command(name = "resize", mixinStandardHelpOptions = true, description = "Change the map size", versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public class MapResizer implements Callable<Integer> {
    @Spec
    private CommandLine.Model.CommandSpec spec;
    @Mixin
    private RequiredMapPathMixin requiredMapPathMixin;
    @Mixin
    private OutputFolderMixin outputFolderMixin;
    @Mixin
    private DebugMixin debugMixin;
    @ArgGroup(exclusive = false, heading = "X and Y coordinate to place the center of the map content, default is the center of the new map size%n")
    private @Nullable LocationOptions locationOptions;
    @Option(names = "--map-size", required = true, description = "New map size, can be specified in oGrids (e.g 512) or km (e.g 10km), must result in a power of 2 in oGrids", converter = PowerOfTwoMapSizeConverter.class)
    private int newMapSize;
    @Option(names = "--scaled-size", required = true, description = "Size to scale the map content to, can be specified in oGrids (e.g 512) or km (e.g 10km)", converter = MapSizeConverter.class)
    private int scaledSize;

    @Override
    public Integer call() throws Exception {
        SCMap map = MapImporter.importMap(requiredMapPathMixin.getMapPath());
        resizeMap(map);
        MapExporter.exportMap(outputFolderMixin.getOutputPath(), map, true);
        return 0;
    }

    private void resizeMap(SCMap map) {
        Vector2 location = locationOptions == null ?
                           new Vector2(newMapSize / 2f, newMapSize / 2f) :
                           locationOptions.getLocation();
        map.changeMapSize(scaledSize, newMapSize, location);
    }
}
