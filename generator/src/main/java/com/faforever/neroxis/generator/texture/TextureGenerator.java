package com.faforever.neroxis.generator.texture;

import com.faforever.neroxis.exporter.PreviewGenerator;
import com.faforever.neroxis.generator.ElementGenerator;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.NormalMask;
import com.faforever.neroxis.mask.Vector4Mask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.ImageUtil;
import com.faforever.neroxis.util.Pipeline;
import lombok.Getter;

import java.io.IOException;

@Getter
public abstract strictfp class TextureGenerator extends ElementGenerator {
    protected FloatMask heightmap;
    protected FloatMask slope;
    protected NormalMask normals;
    protected FloatMask shadows;
    protected FloatMask ambient;
    protected BooleanMask shadowsMask;
    protected Vector4Mask texturesLowMask;
    protected Vector4Mask texturesHighMask;
    protected Vector4Mask texturesLowPreviewMask;
    protected Vector4Mask texturesHighPreviewMask;
    protected FloatMask heightmapPreview;
    protected FloatMask reflectance;
    protected Vector4Mask temp;

    protected abstract void setupTexturePipeline();

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        heightmap = terrainGenerator.getHeightmap();
        slope = terrainGenerator.getSlope();
        normals = heightmap.copy()
                           .resample(512)
                           .copyAsNormalMask(2f);
        shadowsMask = heightmap.copy()
                               .resample(512)
                               .copyAsShadowMask(
                                       generatorParameters.getBiome().getLightingSettings().getSunDirection());
        shadows = shadowsMask.copyAsFloatMask(0, 1);
        ambient = new FloatMask(map.getSize(), random.nextLong(), symmetrySettings, "ambient", true).add(1f);
        texturesLowMask = new Vector4Mask(map.getSize() + 1, random.nextLong(), symmetrySettings, "texturesLow", true);
        texturesHighMask = new Vector4Mask(map.getSize() + 1, random.nextLong(), symmetrySettings, "texturesHigh",
                                           true);
        FloatMask mask = new FloatMask(map.getNormalMap()
                                          .getHeight(), random.nextLong(), symmetrySettings, "temp", true).addPerlinNoise(16, 1);
        temp = new Vector4Mask(mask.getSize(), random.nextLong(), symmetrySettings, "temp", true);
        temp.setComponents(mask, mask, mask, mask).startVisualDebugger();
    }

    public void setTextures() {
        Pipeline.await(texturesLowMask, texturesHighMask, normals, shadows);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateTextures", () -> {
            map.setTextureMasksScaled(map.getTextureMasksLow(), texturesLowMask.getFinalMask());
            map.setTextureMasksScaled(map.getTextureMasksHigh(), texturesHighMask.getFinalMask());
            map.setCompressedPBRTexture(ImageUtil.compressPBRTexture(shadows.getFinalMask(), ambient.getFinalMask(), normals.getFinalMask()));
            map.setNormalMap(temp.toImage());
        });
    }

    public void generatePreview() {
        Pipeline.await(texturesLowPreviewMask, texturesHighPreviewMask, reflectance, heightmapPreview);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generatePreview", () -> {
            try {
                PreviewGenerator.generatePreview(heightmapPreview.getFinalMask(), reflectance.getFinalMask(), map,
                                                 texturesLowPreviewMask.getFinalMask(), texturesHighPreviewMask.getFinalMask());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    protected void setupPreviewPipeline() {
        texturesLowPreviewMask = texturesLowMask.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        texturesHighPreviewMask = texturesHighMask.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        heightmapPreview = heightmap.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        reflectance = heightmap.copy()
                               .copyAsNormalMask(8f)
                               .resample(PreviewGenerator.PREVIEW_SIZE)
                               .copyAsDotProduct(map.getBiome().getLightingSettings().getSunDirection())
                               .add(1f)
                               .divide(2f);
    }

    @Override
    public void setupPipeline() {
        setupTexturePipeline();
        setupPreviewPipeline();
    }
}