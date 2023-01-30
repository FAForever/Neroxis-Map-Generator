package com.faforever.neroxis.generator.texture;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
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
    protected FloatMask accentGroundTexture;
    protected FloatMask waterBeachTexture;
    protected FloatMask accentSlopesTexture;
    protected FloatMask accentPlateauTexture;
    protected FloatMask slopesTexture;
    protected FloatMask steepHillsTexture;
    protected FloatMask rockTexture;
    protected FloatMask accentRockTexture;
    protected IntegerMask terrainType;

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

        setupTerrainType(mapSize);
    }

    protected void setupTerrainType(int mapSize) {
        terrainType.setSize(mapSize);
        BooleanMask realWater = realLand.copy().invert().setSize(mapSize);

        // setSize enforces symmetry, so we need to get rid of the symmetry settings first
        SymmetrySettings noSymmetry = new SymmetrySettings(Symmetry.NONE);
        accentGroundTexture.setSymmetrySettings(noSymmetry);
        accentPlateauTexture.setSymmetrySettings(noSymmetry);
        slopesTexture.setSymmetrySettings(noSymmetry);
        accentSlopesTexture.setSymmetrySettings(noSymmetry);
        steepHillsTexture.setSymmetrySettings(noSymmetry);
        waterBeachTexture.setSymmetrySettings(noSymmetry);
        accentRockTexture.setSymmetrySettings(noSymmetry);
        rockTexture.setSymmetrySettings(noSymmetry);

        Integer[] terrainTypes = map.getBiome().getTerrainMaterials().getTerrainTypes();
        terrainType.add(terrainTypes[0])
                   .setToValue(accentGroundTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[1])
                   .setToValue(accentPlateauTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[2])
                   .setToValue(slopesTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[3])
                   .setToValue(accentSlopesTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[4])
                   .setToValue(steepHillsTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[5])
                   .setToValue(waterBeachTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[6])
                   // We need to change the order here, otherwise accentRock will overwrite the rock texture completely
                   .setToValue(accentRockTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[8])
                   .setToValue(rockTexture.setSize(mapSize).copyAsBooleanMask(.5f), terrainTypes[7])
                   .setToValue(realWater, terrainTypes[9])
                   .setToValue(realWater.deflate(20), terrainTypes[10]);
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
        terrainType = new IntegerMask(1, random.nextLong(), new SymmetrySettings(Symmetry.NONE), "terrainType", true);
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
    public void setCompressedDecals() {
        Pipeline.await(normals, shadows);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "setCompressedDecals", () -> {
            map.setCompressedShadows(ImageUtil.compressShadow(shadows.getFinalMask(),
                                                              generatorParameters.getBiome().getLightingSettings()));
            map.setCompressedNormal(ImageUtil.compressNormal(normals.getFinalMask()));
        });
    }

    @Override
    public void setupPipeline() {
        setupTexturePipeline();
        setupPreviewPipeline();
    }
}
