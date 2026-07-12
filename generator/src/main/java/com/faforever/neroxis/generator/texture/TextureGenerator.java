package com.faforever.neroxis.generator.texture;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.exporter.PreviewGenerator;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.DecalPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.NormalMask;
import com.faforever.neroxis.mask.Vector4Mask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.ImageUtil;
import lombok.Getter;

import java.io.IOException;
import java.util.random.RandomGenerator;

@Getter
public abstract class TextureGenerator {
    protected SCMap map;
    protected Biome biome;
    protected RandomGenerator.SplittableGenerator random;
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
    protected Vector4Mask texturesLowPreviewMask;
    protected Vector4Mask texturesHighPreviewMask;
    protected FloatMask heightmapPreview;
    protected FloatMask irradiance;

    protected DecalPlacer decalPlacer;
    protected BooleanMask passableLand;
    protected BooleanMask fieldDecal;
    protected BooleanMask slopeDecal;

    protected abstract void setupTexturePipeline();

    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        this.map = map;
        this.biome = loadBiome();
        this.random = random.split();
        this.generatorParameters = generatorParameters;
        this.symmetrySettings = symmetrySettings;
        heightmap = new FloatMask(1, random.split(), symmetrySettings, "heightmap");
        slope = new FloatMask(1, random.split(), symmetrySettings, "slope");
        this.passableLand = new BooleanMask(1, random.split(), symmetrySettings, "passableLand");

        passableLand.init(terrainGenerator.getPassableLand());
        slope.init(terrainGenerator.getSlope());
        fieldDecal = new BooleanMask(1, random.split(), symmetrySettings, "fieldDecal");
        slopeDecal = new BooleanMask(1, random.split(), symmetrySettings, "slopeDecal");
        decalPlacer = new DecalPlacer(map, random.split());
        heightmap.init(terrainGenerator.getHeightmap());
        slope.init(terrainGenerator.getSlope());

        normals = heightmap.copy()
                           .addGaussianNoise(.025f)
                           .blur(1)
                           .copyAsNormalMask(1f);
        FloatMask heightMapSize = heightmap.copy().resample(map.getSize());
        shadowsMask = heightMapSize
                .copyAsShadowMask(biome.lightingSettings().sunDirection()).inflate(1);
        shadows = shadowsMask.copyAsFloatMask(1, 0);
        float abyssDepth = biome.waterSettings().elevation() - biome.waterSettings().elevationAbyss();
        scaledWaterDepth = heightmap.copy()
                                    .subtract(biome.waterSettings().elevation())
                                    .multiply(-1f)
                                    .divide(abyssDepth)
                                    .clampMin(0f);

        texturesLowMask = new Vector4Mask(map.getSize() + 1, random.split(), symmetrySettings, "texturesLow");
        texturesHighMask = new Vector4Mask(map.getSize() + 1, random.split(), symmetrySettings, "texturesHigh");
    }

    public abstract Biome loadBiome();

    public void setTextures() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateTextures", () -> {
            map.setTextureMasksScaled(map.getTextureMasksLow(), texturesLowMask.getFinalMask());
            map.setTextureMasksScaled(map.getTextureMasksHigh(), texturesHighMask.getFinalMask());
            map.setMapNormalTexture(ImageUtil.getMapNormalTexture(normals.getFinalMask()));
            map.setMapInfoTexture(ImageUtil.getMapInfoTexture(scaledWaterDepth.getFinalMask(),
                                                              shadows.getFinalMask()));
        });
    }

    public void placeDecals() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeDecals", () -> {
            decalPlacer.placeDecals(fieldDecal.getFinalMask(),
                                    map.getBiome().decalMaterials().fieldNormals(), 32, 32, 24, 32);
            decalPlacer.placeDecals(fieldDecal.getFinalMask(),
                                    map.getBiome().decalMaterials().fieldAlbedos(), 64, 128, 24, 32);
            decalPlacer.placeDecals(slopeDecal.getFinalMask(),
                                    map.getBiome().decalMaterials().slopeNormals(), 16, 32, 16, 32);
            decalPlacer.placeDecals(slopeDecal.getFinalMask(),
                                    map.getBiome().decalMaterials().slopeAlbedos(), 64, 128, 32, 48);
        });
    }

    public void setCompressedDecals() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "setCompressedDecals", () -> {
            map.setCompressedShadows(ImageUtil.compressShadow(shadows.getFinalMask(), biome.lightingSettings()));
            map.setCompressedNormal(ImageUtil.compressNormal(normals.getFinalMask()));
        });
    }

    public void generatePreview() {
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

    public void setupDecalPipeline() {
        fieldDecal.init(passableLand);
        slopeDecal.init(slope, .25f);
        fieldDecal.subtract(slopeDecal.copy().inflate(16));
    }

    private void setupPreviewPipeline() {
        texturesLowPreviewMask = texturesLowMask.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        texturesHighPreviewMask = texturesHighMask.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        heightmapPreview = heightmap.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        irradiance = heightmap.copy()
                              .copyAsNormalMask(8f)
                              .resample(PreviewGenerator.PREVIEW_SIZE)
                              .copyAsDotProduct(map.getBiome().lightingSettings().sunDirection())
                              .clampMin(0f);
    }

    public final void setupPipeline() {
        setupTexturePipeline();
        setupDecalPipeline();
        setupPreviewPipeline();
    }
}