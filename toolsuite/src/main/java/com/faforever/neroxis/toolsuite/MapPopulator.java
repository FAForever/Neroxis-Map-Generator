package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.OutputFolderMixin;
import com.faforever.neroxis.cli.RequiredMapPathMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.exporter.MapExporter;
import com.faforever.neroxis.importer.MapImporter;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.map.placement.*;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.NormalMask;
import com.faforever.neroxis.mask.Vector4Mask;
import com.faforever.neroxis.util.ImageUtil;
import com.faforever.neroxis.util.serial.biome.PropMaterials;
import lombok.Getter;
import picocli.CommandLine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "populate", mixinStandardHelpOptions = true, description = "Populate various map properties based on the heightmap", versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public class MapPopulator implements Callable<Integer> {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;
    @CommandLine.Mixin
    private RequiredMapPathMixin requiredMapPathMixin;
    @CommandLine.Mixin
    private OutputFolderMixin outputFolderMixin;
    @CommandLine.Mixin
    private DebugMixin debugMixin;
    @CommandLine.Option(names = "--ai", description = "Populate AI markers")
    private boolean populateAI;
    @CommandLine.Option(names = "--textures", defaultValue = "1,2,3,4,5,6,7,8", split = ",", description = """
                                                                                                           populate textures arg determines which layers are populated (1, 2, 3, 4, 5, 6, 7, 8)
                                                                                                           default is to populate all 8 layers
                                                                                                           ie: to populate all texture layers except layer 7, use: --textures 1,2,3,4,5,6,8
                                                                                                           texture  layers definitions: 1 Accent Ground, 2 Accent Plateaus, 3 Slopes, 4 Accent Slopes, 5 Steep Hills, 6 Water/Beach, 7 Rock, 8 Accent Rock
                                                                                                           """)
    private Set<Integer> texturesToPopulate;
    @CommandLine.Option(names = "--texture-size", description = "Size of the textures in pixels to use")
    private Integer textureImageSize;
    @CommandLine.Option(names = "--erosion", description = "Resolution in pixels to use for an erosion map. If not specified no erosion map will be generated")
    private Integer erosionResolution;
    @CommandLine.Option(names = "--shadow", description = "Resolution in pixels to use for a shadow map. If not specified no shadow map will be generated")
    private Integer shadowResolution;
    @CommandLine.ArgGroup(heading = "Options that require a symmetry be specified%n", exclusive = false)
    private SymmetryRequiredSettings symmetryRequiredSettings;
    private SCMap map;
    private BooleanMask resourceMask;
    private BooleanMask waterResourceMask;

    @Override
    public Integer call() throws IOException {
        map = MapImporter.importMap(requiredMapPathMixin.getMapPath());
        populate();
        MapExporter.exportMap(outputFolderMixin.getOutputPath(), map, true, erosionResolution != null);
        return 0;
    }

    public void populate() throws IOException {
        /*SupComSlopeValues
        const float FlatHeight = 0.002f;
        const float NonFlatHeight = 0.30f;
        const float AlmostUnpassableHeight = 0.75f;
        const float UnpassableHeight = 0.75f;
        const float ScaleHeight = 256;
        */
        SymmetrySettings symmetrySettings = new SymmetrySettings(symmetryRequiredSettings.getTerrainSymmetry(),
                                                                 symmetryRequiredSettings.getTeamSymmetry(),
                                                                 symmetryRequiredSettings.getTerrainSymmetry());

        Random random = new Random();
        boolean waterPresent = map.getBiome().getWaterSettings().isWaterPresent();
        FloatMask heightmapBase = new FloatMask(map.getHeightmap(), random.nextLong(), symmetrySettings,
                                                map.getHeightMapScale(), "heightmapBase");
        heightmapBase = heightmapBase.copy();
        heightmapBase.forceSymmetry(SymmetryType.SPAWN);
        heightmapBase.writeToImage(map.getHeightmap(), 1 / map.getHeightMapScale());
        float waterHeight;
        if (waterPresent) {
            waterHeight = map.getBiome().getWaterSettings().getElevation();
        } else {
            waterHeight = heightmapBase.getMin();
        }

        BooleanMask land = heightmapBase.copyAsBooleanMask(waterHeight);
        BooleanMask plateaus = heightmapBase.copyAsBooleanMask(waterHeight + 3f);
        FloatMask slope = heightmapBase.copy().gradient();
        BooleanMask impassable = slope.copyAsBooleanMask(.9f);
        BooleanMask ramps = slope.copyAsBooleanMask(.25f).subtract(impassable);
        BooleanMask passable = impassable.copy().invert();
        BooleanMask passableLand = land.copy();
        BooleanMask passableWater = land.copy().invert();

        Integer spawnCount = symmetryRequiredSettings.getSpawnCount();
        if (spawnCount != null) {
            if (spawnCount > 0) {
                SpawnPlacer spawnPlacer = new SpawnPlacer(map, random.nextLong());
                float spawnSeparation = StrictMath.max(
                        random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16, 24);
                BooleanMask spawns = land.copy();
                spawns.multiply(passable).subtract(ramps).deflate(16);
                spawnPlacer.placeSpawns(spawnCount, spawns, spawnSeparation);
            } else {
                map.getSpawns().clear();
            }
        }

        Integer mexCountPerPlayer = symmetryRequiredSettings.getMexCountPerPlayer();
        Integer hydroCountPerPlayer = symmetryRequiredSettings.getHydroCountPerPlayer();
        if (mexCountPerPlayer != null || hydroCountPerPlayer != null) {
            resourceMask = land.copy();
            waterResourceMask = land.copy().invert();

            resourceMask.subtract(impassable).deflate(8).subtract(ramps);
            resourceMask.fillEdge(16, false).fillCenter(16, false);
            waterResourceMask.subtract(ramps).deflate(16).fillEdge(16, false).fillCenter(16, false);
        }

        if (mexCountPerPlayer != null) {
            if (mexCountPerPlayer > 0) {
                MexPlacer mexPlacer = new MexPlacer(map, random.nextLong());

                mexPlacer.placeMexes(mexCountPerPlayer * map.getSpawnCount(), resourceMask, waterResourceMask);
            } else {
                map.getMexes().clear();
            }
        }

        if (hydroCountPerPlayer != null) {
            if (hydroCountPerPlayer > 0) {
                HydroPlacer hydroPlacer = new HydroPlacer(map, random.nextLong());

                hydroPlacer.placeHydros(hydroCountPerPlayer * map.getSpawnCount(), resourceMask.deflate(4));
            } else {
                map.getHydros().clear();
            }
        }

        if (!texturesToPopulate.isEmpty()) {

            int smallWaterSizeLimit = 9000;

            FloatMask[] textureMasksLow = new Vector4Mask(map.getTextureMasksLow(), random.nextLong(), symmetrySettings,
                                                          1f, "TextureMasksLow").subtractScalar(128f)
                                                                                .divideScalar(127f)
                                                                                .clampComponentMin(0f)
                                                                                .clampComponentMax(1f)
                                                                                .splitComponentMasks();
            FloatMask[] textureMasksHigh = new Vector4Mask(map.getTextureMasksHigh(), random.nextLong(),
                                                           symmetrySettings, 1f, "TextureMasksHigh").subtractScalar(
                    128f).divideScalar(127f).clampComponentMin(0f).clampComponentMax(1f).splitComponentMasks();

            if (textureImageSize == null) {
                textureImageSize = map.getSize();
            }

            map.setTextureMasksLow(new BufferedImage(textureImageSize, textureImageSize, BufferedImage.TYPE_INT_ARGB));
            map.setTextureMasksHigh(new BufferedImage(textureImageSize, textureImageSize, BufferedImage.TYPE_INT_ARGB));

            BooleanMask water = land.copy().invert();
            BooleanMask flat = slope.copyAsBooleanMask(.05f).invert();
            BooleanMask inland = land.copy();
            BooleanMask highGround = heightmapBase.copyAsBooleanMask(waterHeight + 3f);
            BooleanMask aboveBeach = heightmapBase.copyAsBooleanMask(waterHeight + 1.5f);
            BooleanMask aboveBeachEdge = heightmapBase.copyAsBooleanMask(waterHeight + 3f);
            BooleanMask flatAboveCoast = heightmapBase.copyAsBooleanMask(waterHeight + .29f);
            BooleanMask higherFlatAboveCoast = heightmapBase.copyAsBooleanMask(waterHeight + 1.2f);
            BooleanMask lowWaterBeach = heightmapBase.copyAsBooleanMask(waterHeight);
            BooleanMask tinyWater = water.copy()
                                         .removeAreasBiggerThan(StrictMath.min(smallWaterSizeLimit / 4 + 750,
                                                                               smallWaterSizeLimit * 2 / 3));
            BooleanMask smallWater = water.copy().removeAreasBiggerThan(smallWaterSizeLimit);
            BooleanMask smallWaterBeach = smallWater.copy().subtract(tinyWater).inflate(2).add(tinyWater);
            FloatMask smallWaterBeachTexture = new FloatMask(textureImageSize, random.nextLong(), symmetrySettings);

            inland.deflate(2);
            flatAboveCoast.multiply(flat);
            higherFlatAboveCoast.multiply(flat);
            lowWaterBeach.invert().subtract(smallWater).inflate(6).subtract(aboveBeach);
            smallWaterBeach.subtract(flatAboveCoast)
                           .blur(2, 0.5f)
                           .subtract(aboveBeach)
                           .subtract(higherFlatAboveCoast)
                           .blur(1);
            smallWaterBeach.setSize(textureImageSize);

            smallWaterBeachTexture.init(smallWaterBeach, 0f, 1f)
                                  .blur(8)
                                  .clampMax(0.35f)
                                  .add(smallWaterBeach, 1f)
                                  .blur(4)
                                  .clampMax(0.65f)
                                  .add(smallWaterBeach, 1f)
                                  .blur(1)
                                  .add(smallWaterBeach, 1f)
                                  .clampMax(1f);

            BooleanMask waterBeach = heightmapBase.copyAsBooleanMask(waterHeight + 1f);
            BooleanMask accentGround = land.copy();
            BooleanMask accentPlateau = plateaus.copy();
            BooleanMask slopes = slope.copyAsBooleanMask(.1f);
            BooleanMask accentSlopes = slope.copyAsBooleanMask(.75f).invert();
            BooleanMask steepHills = slope.copyAsBooleanMask(.55f);
            BooleanMask rock = slope.copyAsBooleanMask(1.25f);
            BooleanMask accentRock = slope.copyAsBooleanMask(1.25f);
            FloatMask waterBeachTexture = new FloatMask(textureImageSize, random.nextLong(), symmetrySettings);
            FloatMask accentGroundTexture = new FloatMask(textureImageSize, random.nextLong(), symmetrySettings);
            FloatMask accentPlateauTexture = new FloatMask(textureImageSize, random.nextLong(), symmetrySettings);
            FloatMask slopesTexture = new FloatMask(textureImageSize, random.nextLong(), symmetrySettings);
            FloatMask accentSlopesTexture = new FloatMask(textureImageSize, random.nextLong(), symmetrySettings);
            FloatMask steepHillsTexture = new FloatMask(textureImageSize, random.nextLong(), symmetrySettings);
            FloatMask rockTexture = new FloatMask(textureImageSize, random.nextLong(), symmetrySettings);
            FloatMask accentRockTexture = new FloatMask(textureImageSize, random.nextLong(), symmetrySettings);

            accentGround.subtract(highGround).acid(.05f, 0).erode(.85f).blur(2, .75f).acid(.45f, 0);
            accentPlateau.acid(.05f, 0).erode(.85f).blur(2, .75f).acid(.45f, 0);
            slopes.multiply(land).flipValues(.95f).erode(.5f).acid(.3f, 0).erode(.2f);
            accentSlopes.subtract(flat).multiply(land).acid(.1f, 0).erode(.5f).blur(4, .75f).acid(.55f, 0);
            steepHills.acid(.3f, 0).erode(.2f);
            if (waterPresent) {
                waterBeach.invert()
                          .subtract(smallWater)
                          .subtract(flatAboveCoast)
                          .subtract(inland)
                          .inflate(1)
                          .add(lowWaterBeach)
                          .blur(5, 0.5f)
                          .subtract(aboveBeach)
                          .subtract(higherFlatAboveCoast)
                          .blur(2)
                          .blur(1);
            } else {
                waterBeach.clear();
            }
            accentRock.acid(.2f, 0).erode(.3f).acid(.2f, 0).blur(2, .5f).multiply(rock);

            accentGround.setSize(textureImageSize);
            accentPlateau.setSize(textureImageSize);
            slopes.setSize(textureImageSize);
            accentSlopes.setSize(textureImageSize);
            steepHills.setSize(textureImageSize);
            waterBeach.setSize(textureImageSize);
            aboveBeachEdge.setSize(textureImageSize);
            rock.setSize(textureImageSize);
            accentRock.setSize(textureImageSize);
            smallWater.setSize(textureImageSize);
            accentGroundTexture.init(accentGround, 0f, 1f)
                               .blur(8)
                               .add(accentGround, .65f)
                               .blur(4)
                               .add(accentGround, .5f)
                               .blur(1)
                               .clampMax(1f);
            accentPlateauTexture.init(accentPlateau, 0f, 1f)
                                .blur(8)
                                .add(accentPlateau, .65f)
                                .blur(4)
                                .add(accentPlateau, .5f)
                                .blur(1)
                                .clampMax(1f);
            slopesTexture.init(slopes, 0f, 1f).blur(8).add(slopes, .65f).blur(4).add(slopes, .5f).blur(1).clampMax(1f);
            accentSlopesTexture.init(accentSlopes, 0f, 1f)
                               .blur(8)
                               .add(accentSlopes, .65f)
                               .blur(4)
                               .add(accentSlopes, .5f)
                               .blur(1)
                               .clampMax(1f);
            steepHillsTexture.init(steepHills, 0f, 1f)
                             .blur(8)
                             .clampMax(0.35f)
                             .add(steepHills, .65f)
                             .blur(4)
                             .clampMax(0.65f)
                             .add(steepHills, .5f)
                             .blur(1)
                             .add(steepHills, 1f)
                             .clampMax(1f);
            waterBeachTexture.init(waterBeach, 0f, 1f)
                             .subtract(rock, 1f)
                             .subtract(aboveBeachEdge, 1f)
                             .clampMin(0f)
                             .blur(2, rock.copy().invert())
                             .add(waterBeach, 1f)
                             .subtract(rock, 1f);
            waterBeachTexture.subtract(aboveBeachEdge, .9f)
                             .clampMin(0f)
                             .blur(2, rock.copy().invert())
                             .subtract(rock, 1f)
                             .subtract(aboveBeachEdge, .8f)
                             .clampMin(0f)
                             .add(waterBeach, .65f)
                             .blur(2, rock.copy().invert());
            waterBeachTexture.subtract(rock, 1f)
                             .subtract(aboveBeachEdge, 0.7f)
                             .clampMin(0f)
                             .add(waterBeach, .5f)
                             .blur(2, rock.copy().invert())
                             .blur(2, rock.copy().invert())
                             .subtract(rock, 1f)
                             .clampMin(0f)
                             .blur(2, rock.copy().invert());
            waterBeachTexture.blur(2, rock.copy().invert())
                             .subtract(rock, 1f)
                             .clampMin(0f)
                             .blur(2, rock.copy().invert())
                             .blur(1, rock.copy().invert())
                             .blur(1, rock.copy().invert())
                             .clampMax(1f);
            waterBeachTexture.removeAreasOfSpecifiedSizeWithLocalMaximums(0, smallWaterSizeLimit, 15, 1f)
                             .blur(1)
                             .blur(1);
            waterBeachTexture.add(smallWaterBeachTexture).clampMax(1f);
            rockTexture.init(rock, 0f, 1f)
                       .blur(8)
                       .clampMax(0.2f)
                       .add(rock, .65f)
                       .blur(4)
                       .clampMax(0.3f)
                       .add(rock, .5f)
                       .blur(1)
                       .add(rock, 1f)
                       .clampMax(1f);
            accentRockTexture.init(accentRock, 0f, 1f)
                             .clampMin(0f)
                             .blur(8)
                             .add(accentRock, .65f)
                             .blur(4)
                             .add(accentRock, .5f)
                             .blur(1)
                             .clampMax(1f);

            if (!texturesToPopulate.contains(1)) {
                accentGroundTexture = textureMasksLow[0].copy().resample(textureImageSize);
            }
            if (!texturesToPopulate.contains(2)) {
                accentPlateauTexture = textureMasksLow[1].copy().resample(textureImageSize);
            }
            if (!texturesToPopulate.contains(3)) {
                slopesTexture = textureMasksLow[2].copy().resample(textureImageSize);
            }
            if (!texturesToPopulate.contains(4)) {
                accentSlopesTexture = textureMasksLow[3].copy().resample(textureImageSize);
            }
            if (!texturesToPopulate.contains(5)) {
                steepHillsTexture = textureMasksHigh[0].copy().resample(textureImageSize);
            }
            if (!texturesToPopulate.contains(6)) {
                waterBeachTexture = textureMasksHigh[1].copy().resample(textureImageSize);
            }
            if (!texturesToPopulate.contains(7)) {
                rockTexture = textureMasksHigh[2].copy().resample(textureImageSize);
            }
            if (!texturesToPopulate.contains(8)) {
                accentRockTexture = textureMasksHigh[3].copy().resample(textureImageSize);
            }

            map.setTextureMasksScaled(map.getTextureMasksLow(), accentGroundTexture, accentPlateauTexture,
                                      slopesTexture, accentSlopesTexture);
            map.setTextureMasksScaled(map.getTextureMasksHigh(), steepHillsTexture, waterBeachTexture, rockTexture,
                                      accentRockTexture);
        }

        if (erosionResolution != null) {
            FloatMask erosionHeightMask = heightmapBase.copy()
                                                       .resample(erosionResolution)
                                                       .subtractAvg()
                                                       .multiply(10f)
                                                       .addPerlinNoise(erosionResolution / 16, 4f);
            erosionHeightMask.waterErode(100000, 100, .1f, .1f, 1f, 1f, 1, .25f);
            NormalMask normal = erosionHeightMask.copyAsNormalMask();
            map.setCompressedNormal(ImageUtil.compressNormal(normal));
        }

        Biome biome = symmetryRequiredSettings.getBiome();
        if (biome != null) {
            map.getProps().clear();
            PropPlacer propPlacer = new PropPlacer(map, random.nextLong());
            PropMaterials propMaterials = biome.getPropMaterials();

            BooleanMask flatEnough = slope.copyAsBooleanMask(.02f);
            BooleanMask flatish = slope.copyAsBooleanMask(.042f);
            BooleanMask nearSteepHills = slope.copyAsBooleanMask(.55f);
            BooleanMask flatEnoughNearRock = slope.copyAsBooleanMask(1.25f);
            flatEnough.invert().multiply(land).erode(.15f);
            flatish.invert().multiply(land).erode(.15f);
            nearSteepHills.inflate(6)
                          .add(flatEnoughNearRock.copy()
                                                 .inflate(16)
                                                 .multiply(nearSteepHills.copy().inflate(11).subtract(flatEnough)))
                          .multiply(land.copy().deflate(10));
            flatEnoughNearRock.inflate(7).add(nearSteepHills).multiply(flatish);

            BooleanMask treeMask = flatEnough.copy();
            BooleanMask cliffRockMask = land.copy();
            BooleanMask fieldStoneMask = land.copy();
            BooleanMask largeRockFieldMask = land.copy();
            BooleanMask smallRockFieldMask = land.copy();

            treeMask.deflate(6)
                    .erode(0.5f)
                    .multiply(land.copy().deflate(15).acid(.05f, 0).erode(.85f).blur(2, .75f).acid(.45f, 0));
            cliffRockMask.randomize(.017f).multiply(flatEnoughNearRock);
            fieldStoneMask.randomize(.00145f).multiply(flatEnoughNearRock.copy().deflate(1));
            largeRockFieldMask.randomize(.015f).multiply(flatEnoughNearRock);
            smallRockFieldMask.randomize(.015f).multiply(flatEnoughNearRock);

            BooleanMask noProps = impassable.copy();

            for (int i = 0; i < map.getSpawnCount(); i++) {
                noProps.fillCircle(map.getSpawn(i).getPosition(), 30, true);
            }
            for (int i = 0; i < map.getMexCount(); i++) {
                noProps.fillCircle(map.getMex(i).getPosition(), 10, true);
            }
            for (int i = 0; i < map.getHydroCount(); i++) {
                noProps.fillCircle(map.getHydro(i).getPosition(), 16, true);
            }

            if (propMaterials.getTreeGroups() != null && propMaterials.getTreeGroups().length > 0) {
                propPlacer.placeProps(treeMask.subtract(noProps), propMaterials.getTreeGroups(), 3f);
            }
            if (propMaterials.getRocks() != null && propMaterials.getRocks().length > 0) {
                propPlacer.placeProps(cliffRockMask.subtract(noProps), propMaterials.getRocks(), 1.5f);
                propPlacer.placeProps(largeRockFieldMask.subtract(noProps), propMaterials.getRocks(), 1.5f);
                propPlacer.placeProps(smallRockFieldMask.subtract(noProps), propMaterials.getRocks(), 1.5f);
            }
            if (propMaterials.getBoulders() != null && propMaterials.getBoulders().length > 0) {
                propPlacer.placeProps(fieldStoneMask.subtract(noProps), propMaterials.getBoulders(), 30f);
            }

            map.setBiome(biome);
        }

        if (populateAI) {
            map.getAmphibiousAIMarkers().clear();
            map.getLandAIMarkers().clear();
            map.getNavyAIMarkers().clear();
            map.getAirAIMarkers().clear();
            BooleanMask passableAI = passable.copy().add(land.copy().invert()).fillEdge(8, false);
            passableLand.multiply(passableAI);
            passableWater.deflate(16).multiply(passableAI).fillEdge(8, false);
            AIMarkerPlacer.placeAIMarkers(passableAI, map.getAmphibiousAIMarkers(), "AmphPN%d");
            AIMarkerPlacer.placeAIMarkers(passableLand, map.getLandAIMarkers(), "LandPN%d");
            AIMarkerPlacer.placeAIMarkers(passableWater, map.getNavyAIMarkers(), "NavyPN%d");
            AIMarkerPlacer.placeAirAIMarkers(map);
        }

        map.setHeights();
    }

    @Getter
    private static class SymmetryRequiredSettings {
        @CommandLine.Option(names = "--terrain-symmetry", required = true, description = "symmetry of the terrain. Values: ${COMPLETION-CANDIDATES}")
        private Symmetry terrainSymmetry;
        @CommandLine.Option(names = "--team-symmetry", required = true, description = "symmetry of the teams. Values: ${COMPLETION-CANDIDATES}")
        private Symmetry teamSymmetry;
        @CommandLine.Option(names = "--spawns", description = "Populate X spawns on the map")
        private Integer spawnCount;
        @CommandLine.Option(names = "--mexes-per-player", description = "Populate X mexes per player on the map")
        private Integer mexCountPerPlayer;
        @CommandLine.Option(names = "--hydros-per-player", description = "Populate X hydros per player on the map")
        private Integer hydroCountPerPlayer;
        private Biome biome;

        @CommandLine.Option(names = "--biome", description = "Name of included biome or path to custom biome folder")
        private void setBiome(String pathOrBiome) {
            biome = Biomes.loadBiome(pathOrBiome);
        }
    }
}
