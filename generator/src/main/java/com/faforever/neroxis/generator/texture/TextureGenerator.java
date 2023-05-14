package com.faforever.neroxis.generator.texture;

import com.faforever.neroxis.exporter.PreviewGenerator;
import com.faforever.neroxis.generator.ElementGenerator;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
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
    protected FloatMask ambient;
    protected BooleanMask shadowsMask;
    protected Vector4Mask texturesLowMask;
    protected Vector4Mask texturesHighMask;
    protected Vector4Mask texturesLowPreviewMask;
    protected Vector4Mask texturesHighPreviewMask;
    protected FloatMask heightmapPreview;
    protected FloatMask reflectance;
    protected FloatMask perlinNoise;

    protected abstract void setupTexturePipeline();

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        heightmap = terrainGenerator.getHeightmap();
        slope = terrainGenerator.getSlope();

        FloatMask heightMapSize = heightmap.copy().resample(map.getSize());

        FloatMask heightDiff = heightMapSize.copy()
                                            .clampMin(generatorParameters.biome()
                                                                         .waterSettings()
                                                                         .getElevation())
                                            .normalize();

        normals = heightMapSize.copy()
                               .addPerlinNoise(64, 12f)
                               .addGaussianNoise(.025f)
                               .blur(1)
                               .copyAsNormalMask(2f);
        shadowsMask = heightMapSize
                .copyAsShadowMask(
                        generatorParameters.biome().lightingSettings().getSunDirection()).inflate(0.5f);
        shadows = shadowsMask.copyAsFloatMask(0, 1).blur(1);
        ambient = new FloatMask(map.getSize(), random.nextLong(), symmetrySettings, "ambient", true).add(1f);

        ambient.subtract(heightDiff.copy().multiply(-1f).add(1f).multiply(.05f))
               .subtract(heightDiff.copy().blur(1).subtract(heightDiff).clampMin(0f).multiply(4f))
               .subtract(heightDiff.copy().blur(2).subtract(heightDiff).clampMin(0f).multiply(2f))
               .subtract(heightDiff.copy().blur(4).subtract(heightDiff).clampMin(0f))
               .subtract(heightDiff.copy().blur(8).subtract(heightDiff).clampMin(0f))
               .subtract(heightDiff.copy().blur(16).subtract(heightDiff).clampMin(0f))
               .subtract(heightDiff.copy().blur(32).subtract(heightDiff).clampMin(0f))
               .subtract(heightDiff.copy().blur(64).subtract(heightDiff).clampMin(0f).multiply(.5f))
               .subtract(heightDiff.copy().blur(128).subtract(heightDiff).clampMin(0f).multiply(.5f))
               .blur(2)
               .clampMin(0f);

        texturesLowMask = new Vector4Mask(map.getSize() + 1, random.nextLong(), symmetrySettings, "texturesLow", true);
        texturesHighMask = new Vector4Mask(map.getSize() + 1, random.nextLong(), symmetrySettings, "texturesHigh",
                                           true);

        perlinNoise = new FloatMask(map.getWaterDepthBiasMap()
                                       .getHeight(), random.nextLong(), new SymmetrySettings(Symmetry.NONE), "perlin", true).addPerlinNoise(24, 1);
    }

    public void setTextures() {
        Pipeline.await(texturesLowMask, texturesHighMask, normals, shadows, ambient, perlinNoise);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateTextures", () -> {
            map.setTextureMasksScaled(map.getTextureMasksLow(), texturesLowMask.getFinalMask());
            map.setTextureMasksScaled(map.getTextureMasksHigh(), texturesHighMask.getFinalMask());
            map.setRawPBRTexture(ImageUtil.getPBRTextureBytes(normals.getFinalMask(), shadows.getFinalMask(), ambient.getFinalMask()));
            map.setWaterDepthBiasMap(perlinNoise.getFinalMask().toImage());
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