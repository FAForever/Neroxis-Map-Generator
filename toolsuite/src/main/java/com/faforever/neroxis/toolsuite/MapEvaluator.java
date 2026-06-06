package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.RequiredMapPathMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.importer.MapImporter;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.MapSymmetryTester;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Spec;

@Command(name = "evaluate", mixinStandardHelpOptions = true, description = "Evaluates a map's symmetry error. Higher values represent greater asymmetry", versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public class MapEvaluator implements Callable<Integer> {
    @Spec
    private CommandLine.Model.CommandSpec spec;
    @Mixin
    private RequiredMapPathMixin requiredMapPathMixin;
    @Mixin
    private DebugMixin debugMixin = new DebugMixin();

    @Override
    public Integer call() throws IOException {
        System.out.printf("Evaluating map %s%n", requiredMapPathMixin.getMapPath());
        evaluate(MapImporter.importMap(requiredMapPathMixin.getMapPath()));
        System.out.println("Done");
        return 0;
    }

    private void evaluate(SCMap map) {
        List<Symmetry> symmetries = Arrays.stream(Symmetry.values())
                                          .filter(symmetry -> symmetry.getNumSymPoints() == 2)
                                          .toList();
        for (Symmetry symmetry : symmetries) {
            MapSymmetryTester.Result result = MapSymmetryTester.evaluate(map, new SymmetrySettings(symmetry));

            System.out.println();
            System.out.printf("Spawns Odd vs Even for Symmetry %s: %s%n", symmetry, result.oddVsEven());
            System.out.printf("Terrain Difference for Symmetry %s: %.8f%n", symmetry, result.terrainScore());
            System.out.printf("Spawn Difference for Symmetry %s: %.2f%n", symmetry, result.spawnScore());
            System.out.printf("Mex Difference for Symmetry %s: %.2f%n", symmetry, result.mexScore());
            System.out.printf("Hydro Difference for Symmetry %s: %.2f%n", symmetry, result.hydroScore());
            System.out.printf("Prop Difference for Symmetry %s: %.2f%n", symmetry, result.propScore());
            System.out.printf("Unit Difference for Symmetry %s: %.2f%n", symmetry, result.unitScore());
        }
    }
}
