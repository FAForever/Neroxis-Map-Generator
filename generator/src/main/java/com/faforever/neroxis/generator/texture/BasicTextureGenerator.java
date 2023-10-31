package com.faforever.neroxis.generator.texture;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.ImageUtil;
import com.faforever.neroxis.util.Pipeline;

public class BasicTextureGenerator extends TextureGenerator {
    protected BooleanMask realLand;
    protected BooleanMask realPlateaus;
    protected FloatMask groundTexture;
    protected FloatMask groundAccentTexture;
    protected FloatMask slopesAccentTexture;
    protected FloatMask plateauAccentTexture;
    protected FloatMask slopesTexture;
    protected FloatMask underWaterTexture;
    protected FloatMask cliffTexture;
    protected FloatMask cliffAccentTexture;
    protected FloatMask roughnessModifierTexture;
    protected IntegerMask terrainType;

    @Override
    protected void setupTexturePipeline() {
        BooleanMask flat = slope.copyAsBooleanMask(.05f).invert();
        BooleanMask slopes = slope.copyAsBooleanMask(.15f);
        BooleanMask steeperSlopes = slope.copyAsBooleanMask(.75f).invert().subtract(flat);
        BooleanMask cliff = slope.copyAsBooleanMask(.75f);
        BooleanMask extendedCliff = slope.copyAsBooleanMask(.75f).inflate(2f);
        BooleanMask realWater = realLand.copy().invert();
        FloatMask waterBeach = realWater.copy().inflate(11).subtract(realPlateaus).copyAsFloatMask(0, 1).blur(12);
        
        BooleanMask shadowsInWater = shadowsMask.copy().multiply(realWater.copy().setSize(map.getSize()));
        shadows.setToValue(shadowsInWater.copy(), 1f);
        shadowsInWater.add(realLand.copy().setSize(map.getSize()), shadowsInWater.copy().inflate(6));
        shadows.subtract(realWater.copy().setSize(map.getSize()),
                         shadowsInWater.copyAsFloatMask(0, 1).blur(6))
               .blur(1);
        
        int textureSize = generatorParameters.mapSize() + 1;
        int mapSize = generatorParameters.mapSize();
        cliffAccentTexture.setSize(textureSize)
                .addPerlinNoise(mapSize / 16, 1f)
                .addGaussianNoise(.05f)
                .clampMax(1f)
                .setToValue(extendedCliff.copy().invert(), 0f)
                .blur(2);
        cliffTexture.init(cliff, 0f, 1f).blur(4).add(cliff, 1f).blur(2).clampMax(1f);
        groundTexture.setSize(textureSize)
                .add(1f)
                .subtract(waterBeach)
                .subtract(cliffTexture); // or accentCliff?
        groundAccentTexture.setSize(textureSize)
                           .addPerlinNoise(mapSize / 8, 1f)
                           .addGaussianNoise(.05f)
                           .clampMax(1f)
                           .multiply(groundTexture)
                           .blur(2);
        plateauAccentTexture.setSize(textureSize)
                            .addPerlinNoise(mapSize / 16, 1f)
                            .addGaussianNoise(.05f)
                            .clampMax(1f)
                            .setToValue(realPlateaus.copy().invert(), 0f)
                            .blur(8);
        slopesTexture.init(slopes, 0f, .75f).blur(16).add(slopes, .5f).blur(16).clampMax(1f);
        slopesAccentTexture.setSize(textureSize)
                           .addPerlinNoise(mapSize / 16, .5f)
                           .addGaussianNoise(.05f)
                           .clampMax(1f)
                           .setToValue(steeperSlopes.copy().invert(), 0f)
                           .blur(16);
        underWaterTexture.init(realWater.deflate(1), 0f, .7f)
                         .add(scaledWaterDepth.copy().multiply(.3f))
                         .clampMax(1f)
                         .blur(1);
        roughnessModifierTexture.setSize(textureSize).add(0.5f);
        texturesLowMask.setComponents(cliffAccentTexture, cliffTexture, groundTexture, groundAccentTexture);
        texturesHighMask.setComponents(slopesTexture, slopesAccentTexture, plateauAccentTexture, roughnessModifierTexture);

        setupTerrainType(mapSize);
    }

    protected void setupTerrainType(int mapSize) {
        terrainType.setSize(mapSize);

        Integer[] terrainTypes = map.getBiome().terrainMaterials().getTerrainTypes();
        terrainType.add(terrainTypes[0])
                   .setToValue(cliffAccentTexture.setSize(mapSize).copyAsBooleanMask(.35f), terrainTypes[1])
                   .setToValue(cliffTexture.setSize(mapSize).copyAsBooleanMask(.55f), terrainTypes[2])
                   .setToValue(groundTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[3])
                   .setToValue(groundAccentTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[4])
                   .setToValue(slopesTexture.setSize(mapSize).copyAsBooleanMask(.3f), terrainTypes[5])
                   .setToValue(slopesAccentTexture.setSize(mapSize).copyAsBooleanMask(.3f), terrainTypes[6])
                   .setToValue(plateauAccentTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[7])
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
        cliffAccentTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "cliffAccentTexture", true);
        cliffTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "cliffTexture", true);
        groundTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "groundTexture", true);
        groundAccentTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "groundAccentTexture", true);
        slopesTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "slopesTexture", true);
        slopesAccentTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "slopesAccentTexture", true);
        plateauAccentTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "plateauAccentTexture", true);
        underWaterTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "underWaterTexture", true);
        roughnessModifierTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "roughnessModifierTexture", true);
        terrainType = new IntegerMask(1, random.nextLong(), symmetrySettings, "terrainType", true);
    }

    @Override
    public void setTextures() {
        Pipeline.await(texturesLowMask, texturesHighMask, terrainType, normals, scaledWaterDepth, shadows);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateTextures", () -> {
            map.setTextureMasksScaled(map.getTextureMasksLow(), texturesLowMask.getFinalMask());
            map.setTextureMasksScaled(map.getTextureMasksHigh(), texturesHighMask.getFinalMask());
            map.setTerrainType(map.getTerrainType(), terrainType.getFinalMask());
            map.setRawMapTexture(ImageUtil.getMapwideTextureBytes(normals.getFinalMask(), scaledWaterDepth.getFinalMask(), shadows.getFinalMask()));
        });
    }

    @Override
    public void setupPipeline() {
        setupTexturePipeline();
        setupPreviewPipeline();
    }
}
