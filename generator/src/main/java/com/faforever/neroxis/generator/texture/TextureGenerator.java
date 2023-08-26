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
public abstract class TextureGenerator extends ElementGenerator {
    protected FloatMask heightmap;
    protected FloatMask slope;
    protected NormalMask normals;
    protected FloatMask shadows;
    protected BooleanMask shadowsMask;
    protected Vector4Mask texturesLowMask;
    protected Vector4Mask texturesHighMask;
    protected Vector4Mask texturesLowPreviewMask;
    protected Vector4Mask texturesHighPreviewMask;
    protected FloatMask heightmapPreview;
    protected FloatMask reflectance;

    protected abstract void setupTexturePipeline();

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        heightmap = terrainGenerator.getHeightmap();
        slope = terrainGenerator.getSlope();

        FloatMask heightMapSize = heightmap.copy().resample(map.getSize());

        normals = heightMapSize.copy()
                               .addGaussianNoise(.025f)
                               .blur(1)
                               .copyAsNormalMask(2f);
        shadowsMask = heightMapSize
                .copyAsShadowMask(
                        generatorParameters.biome().lightingSettings().getSunDirection()).inflate(0.5f);
        shadows = shadowsMask.copyAsFloatMask(1, 0).blur(1);

        texturesLowMask = new Vector4Mask(map.getSize() + 1, random.nextLong(), symmetrySettings, "texturesLow", true);
        texturesHighMask = new Vector4Mask(map.getSize() + 1, random.nextLong(), symmetrySettings, "texturesHigh",
                                           true);
    }

    public void setTextures() {
        Pipeline.await(texturesLowMask, texturesHighMask, normals, shadows);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateTextures", () -> {
            map.setTextureMasksScaled(map.getTextureMasksLow(), texturesLowMask.getFinalMask());
            map.setTextureMasksScaled(map.getTextureMasksHigh(), texturesHighMask.getFinalMask());
            map.setRawMapTexture(ImageUtil.getMapwideTextureBytes(normals.getFinalMask(), shadows.getFinalMask()));
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
                               .copyAsDotProduct(map.getBiome().lightingSettings().getSunDirection())
                               .add(1f)
                               .divide(2f);
    }

    @Override
    public void setupPipeline() {
        setupTexturePipeline();
        setupPreviewPipeline();
    }
}