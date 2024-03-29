package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.RequiredMapPathMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.exporter.SCMapExporter;
import com.faforever.neroxis.importer.MapImporter;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.NormalMask;
import com.faforever.neroxis.util.ImageUtil;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "export-env-map", mixinStandardHelpOptions = true,
                     description = "Export the mapwide normal, waterDepth and shadow texture",
                     versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public class MapEnvTextureExporter implements Callable<Integer> {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;
    @CommandLine.Mixin
    private RequiredMapPathMixin requiredMapPathMixin;

    @Override
    public Integer call() throws Exception {
        generateEnvTexture();
        return 0;
    }

    public void generateEnvTexture() throws Exception {
        System.out.print("Generating env texture\n");
        SCMap map = MapImporter.importMap(requiredMapPathMixin.getMapPath());

        FloatMask heightMap = new FloatMask(map.getHeightmap(), (long) 0, new SymmetrySettings(Symmetry.NONE))
                .resample(map.getSize())
                .divide(128f); // The scmap binary scales by 128
        NormalMask normals = heightMap.copyAsNormalMask(2f);

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

        map.setMapwideTexture(ImageUtil.getMapwideTexture(normals, scaledWaterDepth, shadows));
        SCMapExporter.exportMapwideTexture(requiredMapPathMixin.getMapPath(), map);
    }
}
