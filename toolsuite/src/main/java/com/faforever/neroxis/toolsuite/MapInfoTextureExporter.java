package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.RequiredMapPathMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.exporter.SCMapExporter;
import com.faforever.neroxis.importer.SCMapImporter;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.ImageUtil;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "export-map-info", mixinStandardHelpOptions = true,
                     description = "Export the map info texture containing waterDepth, shadows, and ambient occlusion.",
                     versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public class MapInfoTextureExporter implements Callable<Integer> {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;
    @CommandLine.Mixin
    private RequiredMapPathMixin requiredMapPathMixin;
    @CommandLine.Mixin
    private DebugMixin debugMixin;

    @Override
    public Integer call() throws Exception {
        generateMapInfoTexture();
        return 0;
    }

    public void generateMapInfoTexture() throws Exception {
        System.out.print("Generating map info texture\n");
        SCMap map = SCMapImporter.importSCMAP(requiredMapPathMixin.getMapPath());

        FloatMask heightMap = new FloatMask(map.getHeightmap(), (long) 0, new SymmetrySettings(Symmetry.NONE))
                .divide(128f); // The scmap binary scales by 128

        heightMap.resample(map.getSize());
        BooleanMask realLand = heightMap.copyAsBooleanMask(map.getBiome().waterSettings().elevation());
        BooleanMask realWater = realLand.copy().invert();
        BooleanMask shadowsMask = heightMap
                .copyAsShadowMask(map.getBiome().lightingSettings().sunDirection()).inflate(0.5f);
        FloatMask shadows = shadowsMask.copyAsFloatMask(1, 0);
        BooleanMask shadowsInWater = shadowsMask.copy().multiply(realWater.copy().setSize(map.getSize()));
        shadows.setToValue(shadowsInWater.copy(), 1f);
        shadowsInWater.add(realLand, shadowsInWater.copy().inflate(6));
        shadows.subtract(realWater,
                         shadowsInWater.copyAsFloatMask(0, 1).blur(6))
               .blur(1);

        float abyssDepth = map.getBiome().waterSettings().elevation() -
                           map.getBiome().waterSettings().elevationAbyss();
        FloatMask scaledWaterDepth = heightMap.copy()
                                              .subtract(map.getBiome().waterSettings().elevation())
                                              .multiply(-1f)
                                              .divide(abyssDepth)
                                              .clampMin(0f)
                                              .clampMax(1f);

        map.setMapInfoTexture(ImageUtil.getMapInfoTexture(scaledWaterDepth, shadows));
        SCMapExporter.exportMapInfoTexture(requiredMapPathMixin.getMapPath(), map);
    }
}
