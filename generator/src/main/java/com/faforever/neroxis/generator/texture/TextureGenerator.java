package com.faforever.neroxis.generator.texture;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.exporter.PreviewGenerator;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.mask.NormalMask;
import com.faforever.neroxis.mask.Vector4Mask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.ImageUtil;
import com.faforever.neroxis.util.Pipeline;
import lombok.Getter;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Getter
public abstract class TextureGenerator implements HasParameterConstraints {
    protected SCMap map;
    protected Biome biome;
    protected Random random;
    protected GeneratorParameters generatorParameters;
    protected SymmetrySettings symmetrySettings;

    protected FloatMask heightmap;
    protected FloatMask slope;
    protected NormalMask normals;
    protected FloatMask shadows;
    protected FloatMask scaledWaterDepth;
    protected BooleanMask shadowsMask;
    protected Vector4Mask texturesLowMask;
    protected Vector4Mask texturesHighMask;
    protected IntegerMask terrainType;
    protected Vector4Mask texturesLowPreviewMask;
    protected Vector4Mask texturesHighPreviewMask;
    protected FloatMask heightmapPreview;
    protected FloatMask irradiance;

    private CompletableFuture<Void> texturesSetFuture;
    private CompletableFuture<Void> compressedDecalsSetFuture;
    private CompletableFuture<Void> previewGeneratedFuture;

    protected abstract void setupTexturePipeline();

    public CompletableFuture<Void> getTexturesSetFuture() {
        return texturesSetFuture.copy();
    }

    public CompletableFuture<Void> getCompressedDecalsSetFuture() {
        return compressedDecalsSetFuture.copy();
    }

    public CompletableFuture<Void> getPreviewGeneratedFuture() {
        return previewGeneratedFuture.copy();
    }

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters, SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        this.map = map;
        this.biome = loadBiome();
        this.random = new Random(seed);
        this.generatorParameters = generatorParameters;
        this.symmetrySettings = symmetrySettings;
        heightmap = terrainGenerator.getHeightmap();
        slope = terrainGenerator.getSlope();

        FloatMask heightMapSize = heightmap.copy().resample(map.getSize());

        normals = new NormalMask(1, random.nextLong(), symmetrySettings, "normals", true);
        shadowsMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "shadowsMask", true);

        normals.init(heightMapSize.copy().addGaussianNoise(.025f).blur(1).copyAsNormalMask(2f));
        shadowsMask.init(heightMapSize.copyAsShadowMask(biome.lightingSettings().sunDirection())).inflate(0.5f);
        shadows = shadowsMask.copyAsFloatMask(1, 0);
        float abyssDepth = biome.waterSettings().elevation() - biome.waterSettings().elevationAbyss();
        scaledWaterDepth = heightmap.copy()
                                    .subtract(biome.waterSettings().elevation())
                                    .multiply(-1f)
                                    .divide(abyssDepth)
                                    .clampMin(0f);

        texturesLowMask = new Vector4Mask(map.getSize() + 1, random.nextLong(), symmetrySettings, "texturesLow", true);
        texturesHighMask = new Vector4Mask(map.getSize() + 1, random.nextLong(), symmetrySettings, "texturesHigh",
                                           true);
        terrainType = new IntegerMask(1, random.nextLong(), symmetrySettings, "terrainType", true);
        texturesLowPreviewMask = new Vector4Mask(1, random.nextLong(), symmetrySettings,
                                                 "texturesLowPreviewMask", true);
        texturesHighPreviewMask = new Vector4Mask(1, random.nextLong(), symmetrySettings,
                                                  "texturesHighPreviewMask", true);
        heightmapPreview = new FloatMask(1, random.nextLong(), symmetrySettings,
                                         "heightmapPreviewMask", true);
        irradiance = new FloatMask(1, random.nextLong(), symmetrySettings, "irradiance",
                                   true);

        afterInitialize();

        texturesSetFuture = CompletableFuture.runAsync(this::setTextures);
        previewGeneratedFuture = CompletableFuture.runAsync(this::generatePreview);
        compressedDecalsSetFuture = CompletableFuture.runAsync(this::setCompressedDecals);

        setupPipeline();
    }

    protected void afterInitialize() {}

    public abstract Biome loadBiome();

    protected abstract void setTextures();

    private void setCompressedDecals() {
        Pipeline.await(normals, shadows);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "setCompressedDecals", () -> {
            map.setCompressedShadows(ImageUtil.compressShadow(shadows.getFinalMask(), biome.lightingSettings()));
            map.setCompressedNormal(ImageUtil.compressNormal(normals.getFinalMask()));
        });
    }

    private void generatePreview() {
        Pipeline.await(texturesLowPreviewMask, texturesHighPreviewMask, irradiance, heightmapPreview);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generatePreview", () -> {
            try {
                PreviewGenerator.generatePreview(heightmapPreview.getFinalMask(), irradiance.getFinalMask(), map,
                                                 texturesLowPreviewMask.getFinalMask(),
                                                 texturesHighPreviewMask.getFinalMask());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void setupPreviewPipeline() {
        texturesLowPreviewMask.init(texturesLowMask).resample(PreviewGenerator.PREVIEW_SIZE);
        texturesHighPreviewMask.init(texturesHighMask).resample(PreviewGenerator.PREVIEW_SIZE);
        heightmapPreview.init(heightmap).resample(PreviewGenerator.PREVIEW_SIZE);
        irradiance.init(heightmap.copyAsNormalMask(8f)
                                 .resample(PreviewGenerator.PREVIEW_SIZE)
                                 .copyAsDotProduct(map.getBiome().lightingSettings().sunDirection())).clampMin(0f);
    }

    private void setupPipeline() {
        setupTexturePipeline();
        setupPreviewPipeline();
    }
}