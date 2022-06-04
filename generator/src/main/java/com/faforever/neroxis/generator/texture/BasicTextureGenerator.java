package com.faforever.neroxis.generator.texture;

import com.faforever.neroxis.exporter.PreviewGenerator;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.ImageUtil;
import com.faforever.neroxis.util.Pipeline;
import java.io.IOException;

public strictfp class BasicTextureGenerator extends TextureGenerator {
    protected BooleanMask realLand;
    protected BooleanMask realPlateaus;
    protected FloatMask accentGroundTexture;
    protected FloatMask waterBeachTexture;
    protected FloatMask accentSlopesTexture;
    protected FloatMask accentPlateauTexture;
    protected FloatMask slopesTexture;
    protected FloatMask steepHillsTexture;
    protected FloatMask rockTexture;
    protected FloatMask accentRockTexture;
    protected FloatMask heightmapPreview;
    protected FloatMask reflectance;

    @Override
    protected void setupTexturePipeline() {
        BooleanMask flat = slope.copyAsBooleanMask(.05f).invert();
        BooleanMask slopes = slope.copyAsBooleanMask(.15f);
        BooleanMask accentSlopes = slope.copyAsBooleanMask(.55f).invert().subtract(flat);
        BooleanMask steepHills = slope.copyAsBooleanMask(.55f);
        BooleanMask rock = slope.copyAsBooleanMask(.75f);
        BooleanMask accentRock = slope.copyAsBooleanMask(.75f).inflate(2f);

        BooleanMask realWater = realLand.copy().invert();
        BooleanMask shadowsInWater = shadowsMask.copy().multiply(realWater.copy().setSize(512));
        shadows.add(shadowsInWater, 1f)
               .blur(8, shadowsInWater.inflate(8).subtract(realLand.copy().setSize(512)))
               .clampMax(1f);
        int textureSize = generatorParameters.getMapSize() + 1;
        int mapSize = generatorParameters.getMapSize();
        accentGroundTexture.setSize(textureSize)
                           .addPerlinNoise(mapSize / 8, 1f)
                           .addGaussianNoise(.05f)
                           .clampMax(1f)
                           .setToValue(realWater, 0f)
                           .blur(2);
        accentPlateauTexture.setSize(textureSize)
                            .addPerlinNoise(mapSize / 16, 1f)
                            .addGaussianNoise(.05f)
                            .clampMax(1f)
                            .setToValue(realPlateaus.copy().invert(), 0f)
                            .blur(8);
        slopesTexture.init(slopes, 0f, .75f).blur(16).add(slopes, .5f).blur(16).clampMax(1f);
        accentSlopesTexture.setSize(textureSize)
                           .addPerlinNoise(mapSize / 16, .5f)
                           .addGaussianNoise(.05f)
                           .clampMax(1f)
                           .setToValue(accentSlopes.copy().invert(), 0f)
                           .blur(16);
        steepHillsTexture.setSize(textureSize)
                         .addPerlinNoise(mapSize / 8, 1f)
                         .addGaussianNoise(.05f)
                         .clampMax(1f)
                         .setToValue(steepHills.copy().invert(), 0f)
                         .blur(8);
        waterBeachTexture.init(realWater.inflate(12).subtract(realPlateaus), 0f, 1f).blur(12);
        rockTexture.init(rock, 0f, 1f).blur(4).add(rock, 1f).blur(2).clampMax(1f);
        accentRockTexture.setSize(textureSize)
                         .addPerlinNoise(mapSize / 16, 1f)
                         .addGaussianNoise(.05f)
                         .clampMax(1f)
                         .setToValue(accentRock.copy().invert(), 0f)
                         .blur(2);
        texturesLowMask.setComponents(accentGroundTexture, accentPlateauTexture, slopesTexture, accentSlopesTexture);
        texturesHighMask.setComponents(steepHillsTexture, waterBeachTexture, rockTexture, accentRockTexture);
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
        realLand = heightmap.copyAsBooleanMask(generatorParameters.getBiome().getWaterSettings().getElevation());
        realPlateaus = heightmap.copyAsBooleanMask(
                generatorParameters.getBiome().getWaterSettings().getElevation() + 3f);
        accentGroundTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "accentGroundTexture", true);
        waterBeachTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "waterBeachTexture", true);
        accentSlopesTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "accentSlopesTexture", true);
        accentPlateauTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "accentPlateauTexture", true);
        slopesTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "slopesTexture", true);
        steepHillsTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "steepHillsTexture", true);
        rockTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "rockTexture", true);
        accentRockTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "accentRockTexture", true);
    }

    @Override
    public void setTextures() {
        Pipeline.await(texturesLowMask, texturesHighMask);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateTextures", () -> {
            map.setTextureMasksScaled(map.getTextureMasksLow(), texturesLowMask.getFinalMask());
            map.setTextureMasksScaled(map.getTextureMasksHigh(), texturesHighMask.getFinalMask());
        });
    }

    @Override
    public void setCompressedDecals() {
        Pipeline.await(normals, shadows);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "setCompressedDecals", () -> {
            map.setCompressedShadows(ImageUtil.compressShadow(shadows.getFinalMask(),
                                                              generatorParameters.getBiome().getLightingSettings()));
            map.setCompressedNormal(ImageUtil.compressNormal(normals.getFinalMask()));
        });
    }

    @Override
    public void generatePreview() {
        Pipeline.await(texturesLowMask, texturesHighMask, reflectance, heightmapPreview);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generatePreview", () -> {
            try {
                PreviewGenerator.generatePreview(heightmapPreview.getFinalMask(), reflectance.getFinalMask(), map,
                                                 texturesLowMask.getFinalMask(), texturesHighMask.getFinalMask());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void setupPipeline() {
        setupTexturePipeline();
        setupPreviewPipeline();
    }
}
