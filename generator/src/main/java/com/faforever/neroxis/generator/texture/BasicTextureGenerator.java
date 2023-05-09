package com.faforever.neroxis.generator.texture;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public class BasicTextureGenerator extends TextureGenerator {
    protected BooleanMask realLand;
    protected BooleanMask realPlateaus;
    protected FloatMask accentGroundTexture;
    protected FloatMask waterBeachTexture;
    protected FloatMask accentSlopesTexture;
    protected FloatMask accentPlateauTexture;
    protected FloatMask slopesTexture;
    protected FloatMask underWaterTexture;
    protected FloatMask rockTexture;
    protected FloatMask pbrTexture;
    protected IntegerMask terrainType;

    @Override
    protected void setupTexturePipeline() {
        BooleanMask flat = slope.copyAsBooleanMask(.05f).invert();
        BooleanMask slopes = slope.copyAsBooleanMask(.15f);
        BooleanMask accentSlopes = slope.copyAsBooleanMask(.75f).invert().subtract(flat);
        BooleanMask rock = slope.copyAsBooleanMask(.75f);
        float abyssDepth = generatorParameters.biome().waterSettings().getElevation() -
                           generatorParameters.biome().waterSettings().getElevationAbyss();
        FloatMask scaledWaterDepth = heightmap.copy()
                                              .subtract(generatorParameters.biome().waterSettings().getElevation())
                                              .multiply(-1f)
                                              .divide(abyssDepth)
                                              .clampMin(0f);

        BooleanMask realWater = realLand.copy().invert();
        BooleanMask shadowsInWater = shadowsMask.copy().multiply(realWater.copy().setSize(512));
        shadows.add(shadowsInWater, 1f)
               .blur(8, shadowsInWater.inflate(8).subtract(realLand.copy().setSize(512)))
               .clampMax(1f);
        int textureSize = generatorParameters.mapSize() + 1;
        int mapSize = generatorParameters.mapSize();
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
        underWaterTexture.init(realWater.deflate(1), 0f, .7f)
                         .add(scaledWaterDepth.multiply(.3f))
                         .clampMax(1f)
                         .blur(1);
        waterBeachTexture.init(realWater.inflate(12).subtract(realPlateaus), 0f, 1f).blur(12);
        rockTexture.init(rock, 0f, 1f).blur(4).add(rock, 1f).blur(2).clampMax(1f);
        pbrTexture.setSize(textureSize);
        texturesLowMask.setComponents(accentGroundTexture, accentPlateauTexture, slopesTexture, accentSlopesTexture);
        texturesHighMask.setComponents(waterBeachTexture, underWaterTexture, rockTexture, pbrTexture);

        setupTerrainType(mapSize);
    }

    protected void setupTerrainType(int mapSize) {
        terrainType.setSize(mapSize);

        Integer[] terrainTypes = map.getBiome().terrainMaterials().getTerrainTypes();
        terrainType.add(terrainTypes[0])
                   .setToValue(accentGroundTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[1])
                   .setToValue(accentPlateauTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[2])
                   .setToValue(slopesTexture.setSize(mapSize).copyAsBooleanMask(.3f), terrainTypes[3])
                   .setToValue(accentSlopesTexture.setSize(mapSize).copyAsBooleanMask(.3f), terrainTypes[4])
                   .setToValue(waterBeachTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[5])
                   .setToValue(rockTexture.setSize(mapSize).copyAsBooleanMask(.55f), terrainTypes[6])
                   .setToValue(underWaterTexture.setSize(mapSize).copyAsBooleanMask(.7f), terrainTypes[8])
                   .setToValue(underWaterTexture.setSize(mapSize).copyAsBooleanMask(.8f), terrainTypes[9]);
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
        realLand = heightmap.copyAsBooleanMask(generatorParameters.biome().waterSettings().getElevation());
        realPlateaus = heightmap.copyAsBooleanMask(
                generatorParameters.biome().waterSettings().getElevation() + 3f);
        accentGroundTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "accentGroundTexture", true);
        waterBeachTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "waterBeachTexture", true);
        accentSlopesTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "accentSlopesTexture", true);
        accentPlateauTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "accentPlateauTexture", true);
        slopesTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "slopesTexture", true);
        underWaterTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "underWaterTexture", true);
        rockTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "rockTexture", true);
        pbrTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "accentRockTexture", true);
        terrainType = new IntegerMask(1, random.nextLong(), symmetrySettings, "terrainType", true);
    }

    @Override
    public void setTextures() {
        Pipeline.await(texturesLowMask, texturesHighMask, terrainType);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateTextures", () -> {
            map.setTextureMasksScaled(map.getTextureMasksLow(), texturesLowMask.getFinalMask());
            map.setTextureMasksScaled(map.getTextureMasksHigh(), texturesHighMask.getFinalMask());
            map.setTerrainType(map.getTerrainType(), terrainType.getFinalMask());
        });
    }

    @Override
    public void setupPipeline() {
        setupTexturePipeline();
        setupPreviewPipeline();
    }
}
