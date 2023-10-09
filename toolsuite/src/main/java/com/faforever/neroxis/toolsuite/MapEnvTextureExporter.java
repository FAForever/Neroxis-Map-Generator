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
        
        FloatMask heightMapSize = new FloatMask(map.getHeightmap(), (long) 0, new SymmetrySettings(Symmetry.NONE))
                .resample(map.getSize())
                .divide(128f); // No idea why this is necessary
        NormalMask normals = heightMapSize.copyAsNormalMask(2f);

        BooleanMask realLand = heightMapSize.copyAsBooleanMask(map.getBiome().waterSettings().getElevation());
        BooleanMask realWater = realLand.copy().invert();
        BooleanMask shadowsMask = heightMapSize
                .copyAsShadowMask(map.getBiome().lightingSettings().getSunDirection()).inflate(0.5f);
        FloatMask shadows = shadowsMask.copyAsFloatMask(1, 0);
        BooleanMask shadowsInWater = shadowsMask.copy().multiply(realWater.copy().setSize(map.getSize()));
        shadows.setToValue(shadowsInWater.copy(), 1f);
        shadowsInWater.add(realLand.copy().setSize(map.getSize()), shadowsInWater.copy().inflate(6));
        shadows.subtract(realWater.copy().setSize(map.getSize()),
                        shadowsInWater.copyAsFloatMask(0, 1).blur(6))
                .blur(1);

        float abyssDepth = map.getBiome().waterSettings().getElevation() -
                map.getBiome().waterSettings().getElevationAbyss();
        FloatMask scaledWaterDepth = heightMapSize.copy()
                .subtract(map.getBiome().waterSettings().getElevation())
                .multiply(-1f)
                .divide(abyssDepth)
                .clampMin(0f)
                .clampMax(1f);

        map.setRawMapTexture(ImageUtil.getMapwideTextureBytes(normals, scaledWaterDepth, shadows));
        SCMapExporter.exportMapwideTexture(requiredMapPathMixin.getMapPath(), map);
    }
}
