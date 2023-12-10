package com.faforever.neroxis.generator.texture;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.ImageUtil;
import com.faforever.neroxis.util.Pipeline;

public class PbrTextureGenerator extends TextureGenerator {
    protected BooleanMask realLand;
    protected BooleanMask realPlateaus;
    protected FloatMask groundTexture;
    protected FloatMask groundAccentTexture;
    protected FloatMask debrisTexture;
    protected FloatMask plateauTexture;
    protected FloatMask slopesTexture;
    protected FloatMask underWaterTexture;
    protected FloatMask waterBeachTexture;
    protected FloatMask cliffAccentTexture;
    protected FloatMask roughnessModifierTexture;
    protected IntegerMask terrainType;
    protected FloatMask waterSurfaceShadows;

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                .biomes("PBR_Loki")
                .build();
    }

    @Override
    protected void setupTexturePipeline() {
        BooleanMask debris = slope.copyAsBooleanMask(.2f);
        BooleanMask cliff = slope.copyAsBooleanMask(.75f);
        BooleanMask extendedCliff = slope.copyAsBooleanMask(.75f).inflate(2f);
        BooleanMask realWater = realLand.copy().invert();
        FloatMask waterBeach = realWater.copy().inflate(11)
                .subtract(realPlateaus)
                .subtract(slope.copyAsBooleanMask(.05f).invert())
                .add(realWater)
                .copyAsFloatMask(0, 1)
                .blur(4);
        FloatMask cliffMask = cliff.copyAsFloatMask(0, 1).blur(4).add(cliff, 1f).blur(2).add(cliff, 0.5f);

        BooleanMask shadowsInWater = shadowsMask.copy().multiply(realWater.copy().setSize(map.getSize()));
        shadows.setToValue(shadowsInWater.copy(), 1f);
        shadowsInWater.add(realLand.copy().setSize(map.getSize()), shadowsInWater.copy().inflate(6));
        shadows.subtract(realWater.copy().setSize(map.getSize()),
                        shadowsInWater.copyAsFloatMask(0, 1).blur(6))
                .blur(1);
        waterSurfaceShadows = heightmap.copy()
                .clampMin(generatorParameters.biome().waterSettings().getElevation())
                .copyAsShadowMask(generatorParameters.biome().lightingSettings().getSunDirection())
                .inflate(0.5f)
                .resample(map.getSize() / 2)
                .copyAsFloatMask(1, 0)
                .blur(1);

        int textureSize = generatorParameters.mapSize() + 1;
        int mapSize = generatorParameters.mapSize();
        waterBeachTexture.setSize(textureSize)
                .add(waterBeach)
                .subtract(cliffMask);
        cliffAccentTexture.setSize(textureSize)
                .add(0.2f)
                .addPerlinNoise(mapSize / 32, 0.6f)
                .clampMax(1f)
                .setToValue(extendedCliff.copy().invert(), 0f)
                .blur(2)
                .addGaussianNoise(.05f);
        groundTexture.setSize(textureSize)
                .add(1f)
                .subtract(waterBeach)
                .subtract(cliffMask)
                .clampMin(0f);
        groundAccentTexture.setSize(textureSize)
                .add(0.1f)
                .addPerlinNoise(mapSize / 16, 0.5f)
                .addPerlinNoise(mapSize / 6, 0.2f)
                .addGaussianNoise(.05f)
                .clampMax(1f)
                .multiply(groundTexture)
                .clampMin(0f);
        slopesTexture.init(slope)
                .subtract(0.05f)
                .multiply(4f)
                .addPerlinNoise(mapSize / 40, .2f)
                .addPerlinNoise(mapSize / 8, .1f)
                .addGaussianNoise(.05f)
                .subtract(0.2f)
                .clampMax(1f)
                .subtract(waterBeach)
                .subtract(cliffMask.copy().subtract(0.2f).clampMin(0f));
        debrisTexture.init(cliff.copy().inflate(8), 0f, 1f)
                .subtract(0.25f)
                .addPerlinNoise(mapSize / 8, .2f)
                .addGaussianNoise(.05f)
                .setToValue(debris.copy().invert(), 0f)
                .setToValue(realPlateaus, 0f)
                .subtract(realWater.copyAsFloatMask(0, 1).blur(4))
                .subtract(cliff, 0.9f)
                .clampMin(0f)
                .multiply(0.7f)
                .blur(1);
        plateauTexture.setSize(textureSize)
                .add(0.5f)
                .addPerlinNoise(mapSize / 40, .4f)
                .setToValue(realPlateaus.copy().invert(), 0f)
                .multiply(slope.copyAsBooleanMask(0.01f).deflate(4), slope.copy().multiply(-8f).add(1f).clampMin(0f))
                .clampMin(0f)
                .blur(2)
                .setToValue(cliff, 0f)
                .blur(1)
                .multiply(0.8f);
        underWaterTexture.init(realWater.deflate(1), 0f, .7f)
                .add(scaledWaterDepth.copy().multiply(.3f))
                .clampMax(1f)
                .blur(1);
        roughnessModifierTexture.setSize(textureSize)
                .add(0.5f)
                .addPerlinNoise(mapSize / 70, .1f);
        
        // due to the heightmapsplatting we effectively don't see a difference in very high or low values,
        // so we compress the range a bit
        slopesTexture.multiply(0.6f).add(slopesTexture.copyAsBooleanMask(0.01f), 0.3f);
        
        texturesLowMask.setComponents(waterBeachTexture, cliffAccentTexture, groundTexture, groundAccentTexture);
        texturesHighMask.setComponents(slopesTexture, debrisTexture, plateauTexture, roughnessModifierTexture);

        setupTerrainType(mapSize);
    }

    protected void setupTerrainType(int mapSize) {
        terrainType.setSize(mapSize);

        Integer[] terrainTypes = map.getBiome().terrainMaterials().getTerrainTypes();
        terrainType.add(terrainTypes[0])
                .setToValue(waterBeachTexture.setSize(mapSize).copyAsBooleanMask(.55f), terrainTypes[1])
                .setToValue(cliffAccentTexture.setSize(mapSize).copyAsBooleanMask(.35f), terrainTypes[2])
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
        this.map.setTerrainShaderPath(SCMap.PBR_SHADER_NAME);
        
        realLand = heightmap.copyAsBooleanMask(generatorParameters.biome().waterSettings().getElevation());
        realPlateaus = heightmap.copyAsBooleanMask(
                generatorParameters.biome().waterSettings().getElevation() + 5f);
        waterBeachTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "waterBeachTexture", true);
        cliffAccentTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "cliffAccentTexture", true);
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
        Pipeline.await(texturesLowMask, texturesHighMask, terrainType,
                       normals, scaledWaterDepth, shadows, waterSurfaceShadows);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateTextures", () -> {
            map.setTextureMasksScaled(map.getTextureMasksLow(), texturesLowMask.getFinalMask());
            map.setTextureMasksScaled(map.getTextureMasksHigh(), texturesHighMask.getFinalMask());
            map.setTerrainType(map.getTerrainType(), terrainType.getFinalMask());
            map.setMapwideTexture(
                    ImageUtil.getMapwideTexture(normals.getFinalMask(), scaledWaterDepth.getFinalMask(),
                            shadows.getFinalMask()));
            map.setWaterShadowMap(map.getWaterShadowMap(), waterSurfaceShadows.getFinalMask());
        });
    }
}
