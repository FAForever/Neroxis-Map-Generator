package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.RequiredMapPathMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.exporter.SCMapExporter;
import com.faforever.neroxis.importer.SCMapImporter;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.NormalMask;
import com.faforever.neroxis.util.ImageUtil;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "export-map-normals", mixinStandardHelpOptions = true,
        description = "Export the map normal texture.",
        versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public class MapNormalsTextureExporter implements Callable<Integer> {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;
    @CommandLine.Mixin
    private RequiredMapPathMixin requiredMapPathMixin;
    @CommandLine.Mixin
    private DebugMixin debugMixin;

    @Override
    public Integer call() throws Exception {
        generateMapNormalTexture();
        return 0;
    }

    public void generateMapNormalTexture() throws Exception {
        System.out.print("Generating map normal texture\n");
        SCMap map = SCMapImporter.importSCMAP(requiredMapPathMixin.getMapPath());

        FloatMask heightMap = new FloatMask(map.getHeightmap(), null, new SymmetrySettings(Symmetry.NONE))
                .divide(128f); // The scmap binary scales by 128
        NormalMask normals = heightMap.copyAsNormalMask(1f);

        map.setMapNormalTexture(ImageUtil.getMapNormalTexture(normals));
        SCMapExporter.exportMapNormalTexture(requiredMapPathMixin.getMapPath(), map);
    }
}
