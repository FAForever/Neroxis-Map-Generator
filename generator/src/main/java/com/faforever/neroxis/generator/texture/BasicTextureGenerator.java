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

import static com.faforever.neroxis.map.SCMap.PBR_SHADER_NAME;

public class BasicTextureGenerator extends TextureGenerator {
    protected BooleanMask realLand;
    protected BooleanMask realPlateaus;
    protected FloatMask groundTexture;
    protected FloatMask groundAccentTexture;
    protected FloatMask debrisTexture;
    protected FloatMask plateauTexture;
    protected FloatMask slopesTexture;
    protected FloatMask underWaterTexture;
    protected FloatMask cliffTexture;
    protected FloatMask cliffAccentTexture;
    protected FloatMask roughnessModifierTexture;
    protected IntegerMask terrainType;

    @Override
    protected void setupTexturePipeline() {
        BooleanMask debris = slope.copyAsBooleanMask(.2f);
        BooleanMask cliff = slope.copyAsBooleanMask(.75f);
        BooleanMask extendedCliff = slope.copyAsBooleanMask(.75f).inflate(2f);
        BooleanMask realWater = realLand.copy().invert();
        FloatMask waterBeach = realWater.copy().inflate(10)
                .subtract(realPlateaus)
                .subtract(slope.copyAsBooleanMask(.05f).invert())
                .add(realWater)
                .copyAsFloatMask(0, 1)
                .blur(4);
        
        BooleanMask shadowsInWater = shadowsMask.copy().multiply(realWater.copy().setSize(map.getSize()));
        shadows.setToValue(shadowsInWater.copy(), 1f);
        shadowsInWater.add(realLand.copy().setSize(map.getSize()), shadowsInWater.copy().inflate(6));
        shadows.subtract(realWater.copy().setSize(map.getSize()),
                         shadowsInWater.copyAsFloatMask(0, 1).blur(6))
               .blur(1);
        
        int textureSize = generatorParameters.mapSize() + 1;
        int mapSize = generatorParameters.mapSize();
        cliffTexture.init(cliff, 0f, 1f).blur(4).add(cliff, 1f).blur(2).add(cliff, 0.5f);
        cliffAccentTexture.setSize(textureSize)
                .addPerlinNoise(32, 1f)
                .addGaussianNoise(.05f)
                .clampMax(1f)
                .setToValue(extendedCliff.copy().invert(), 0f)
                .blur(2);
        groundTexture.setSize(textureSize)
                .add(1f)
                .subtract(waterBeach)
                .subtract(cliffTexture)
                .clampMin(0f);
        groundAccentTexture.setSize(textureSize)
                           .addPerlinNoise(64, 1f)
                           .addGaussianNoise(.05f)
                           .clampMax(1f)
                           .multiply(groundTexture)
                           .blur(2)
                .clampMin(0f);
        slopesTexture.init(slope)
                .subtract(0.05f)
                .multiply(4f)
                .clampMax(1f)
                .subtract(waterBeach)
                .subtract(cliffTexture.copy().subtract(0.2f).clampMin(0f));
        debrisTexture.init(cliff.copy().inflate(8), 0f, 1f)
                .subtract(0.25f)
                .addPerlinNoise(10, .2f)
                .addGaussianNoise(.05f)
                .setToValue(debris.copy().invert(), 0f)
                .setToValue(realPlateaus, 0f)
                .subtract(realWater.copyAsFloatMask(0, 1).blur(4))
                .subtract(cliffTexture.copy().subtract(0.7f).clampMin(0f))
                .clampMin(0f)
                .multiply(0.7f)
                .blur(1);
        plateauTexture.setSize(textureSize)
                .setToValue(realPlateaus, 0.5f)
                .addPerlinNoise(40, .4f)
                .multiply(slope.copy().multiply(-15f).add(1f).clampMin(0f))
                .clampMin(0f)
                .blur(2)
                .clampMax(1f)
                .subtract(cliffTexture.copy().subtract(0.2f).clampMin(0f));
        underWaterTexture.init(realWater.deflate(1), 0f, .7f)
                         .add(scaledWaterDepth.copy().multiply(.3f))
                         .clampMax(1f)
                         .blur(1);
        roughnessModifierTexture.setSize(textureSize);
        if (map.getTerrainShaderPath().equals(PBR_SHADER_NAME)) {
            roughnessModifierTexture.add(0.5f);
//        } else if (map.getTerrainShaderPath().equals(LEGACY_SHADER_NAME)) {
//            cliffAccentTexture.multiply(0.5f).add(0.5f);
//            cliffTexture.multiply(0.5f).add(0.5f);
//            groundTexture.multiply(0.5f).add(0.5f);
//            groundAccentTexture.multiply(0.5f).add(0.5f);
//            slopesTexture.multiply(0.5f).add(0.5f);
//            debrisTexture.multiply(0.5f).add(0.5f);
//            plateauTexture.multiply(0.5f).add(0.5f);
        }
        texturesLowMask.setComponents(cliffTexture, cliffAccentTexture, groundTexture, groundAccentTexture);
        texturesHighMask.setComponents(slopesTexture, debrisTexture, plateauTexture, roughnessModifierTexture);

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
                   .setToValue(debrisTexture.setSize(mapSize).copyAsBooleanMask(.3f), terrainTypes[6])
                   .setToValue(plateauTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[7])
                   .setToValue(underWaterTexture.setSize(mapSize).copyAsBooleanMask(.7f), terrainTypes[8])
                   .setToValue(underWaterTexture.setSize(mapSize).copyAsBooleanMask(.8f), terrainTypes[9]);
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
        realLand = heightmap.copyAsBooleanMask(generatorParameters.biome().waterSettings().getElevation());
        realPlateaus = heightmap.copyAsBooleanMask(
                generatorParameters.biome().waterSettings().getElevation() + 5f);
        cliffAccentTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "cliffAccentTexture", true);
        cliffTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "cliffTexture", true);
        groundTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "groundTexture", true);
        groundAccentTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "groundAccentTexture", true);
        slopesTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "slopesTexture", true);
        debrisTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "debrisTexture", true);
        plateauTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "plateauTexture", true);
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
