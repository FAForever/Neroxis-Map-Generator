package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.util.serial.MapStyle;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.FloatMask.OuterCircleTerrain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpawnTerrainGeneratorTest {
    @Test
    public void generatorsChooseOuterCircleTerrain() {
        TerrainGenerator[] oceanGenerators = {
                new BigIslandsTerrainGenerator(),
                new CenterLakeLastTerrainGenerator(), new CenterLakeTerrainGenerator(),
                new DropPlateauLastTerrainGenerator(), new FloodedTerrainGenerator(),
                new FractalNavyLastTerrainGenerator(), new FractalNoiseLastTerrainGenerator(),
                new LandBridgeTerrainGenerator(), new OneIslandTerrainGenerator(),
                new RiversAndOceansTerrainGenerator(), new RiversTerrainGenerator(),
                new SmallIslandsTerrainGenerator()
        };
        for (TerrainGenerator generator : oceanGenerators) {
            assertEquals(OuterCircleTerrain.OCEAN, generator.getOuterCircleTerrain(),
                         generator.getClass().getSimpleName());
        }
        TerrainGenerator[] mountainGenerators = {
                new DropPlateauTerrainGenerator(), new FractalLandLastTerrainGenerator(),
                new LittleMountainLastTerrainGenerator(), new LittleMountainTerrainGenerator(),
                new MountainRangeLastTerrainGenerator(), new MountainRangeTerrainGenerator(),
                new MultiLevelLastTerrainGenerator(), new SetonishLastTerrainGenerator(),
                new ValleyTerrainGenerator(), new ValleyLastTerrainGenerator()
        };
        for (TerrainGenerator generator : mountainGenerators) {
            assertEquals(OuterCircleTerrain.MOUNTAINS, generator.getOuterCircleTerrain(),
                         generator.getClass().getSimpleName());
        }
        TerrainGenerator[] flatLandGenerators = {
                new BasicLastTerrainGenerator(), new BasicTerrainGenerator(),
                new FractalPlateauLastTerrainGenerator()
        };
        for (TerrainGenerator generator : flatLandGenerators) {
            assertEquals(OuterCircleTerrain.FLAT_LAND, generator.getOuterCircleTerrain(),
                         generator.getClass().getSimpleName());
        }
        assertEquals(OuterCircleTerrain.OCEAN, new TerrainGenerator() {
            @Override
            public void setupPipeline() {}

            @Override
            public void placeSpawns() {}
        }.getOuterCircleTerrain());
    }

    private static Stream<Arguments> rotationParameters() {
        return IntStream.of(0, 1, 2, 3, 5, 7).boxed()
                        .flatMap(numTeams -> Stream.of(OuterCircleTerrain.values())
                                                   .map(terrain -> Arguments.of(numTeams, terrain)));
    }

    @ParameterizedTest
    @MethodSource("rotationParameters")
    public void bothPipelinesChooseTerrainRotation(int numTeams, OuterCircleTerrain outerCircleTerrain) {
        Symmetry symmetry = numTeams < 2 ? Symmetry.NONE : Symmetry.valueOf("POINT" + numTeams);
        SymmetrySettings symmetrySettings = new SymmetrySettings(symmetry);
        SCMap map = new SCMap(64, Biomes.loadBiome(BiomeName.BRIMSTONE));
        FloatMask expected = new FloatMask(65, new SplittableRandom(0), symmetrySettings)
                .set((x, y) -> x * y + 2f * x + 3f * y);
        if (!symmetry.isPerfectSymmetry()) {
            expected.forceSmoothTerrainRotation(numTeams, map.getBiome().waterSettings().elevation(),
                                                outerCircleTerrain);
        } else {
            expected.forceSymmetry();
        }

        AtomicInteger passableStages = new AtomicInteger();
        AtomicInteger spawnStages = new AtomicInteger();
        TerrainGenerator[] generators = {
                new SpawnFirstTerrainGenerator() {
                    @Override
                    public OuterCircleTerrain getOuterCircleTerrain() {
                        return outerCircleTerrain;
                    }

                    @Override
                    protected void setupTerrainPipeline() {
                        heightmap.set((x, y) -> x * y + 2f * x + 3f * y);
                    }

                    @Override
                    protected void setupPassablePipeline() {
                        assertHeightmapEquals(expected, heightmap);
                        passableStages.incrementAndGet();
                    }
                },
                new SpawnLastTerrainGenerator() {
                    @Override
                    public OuterCircleTerrain getOuterCircleTerrain() {
                        return outerCircleTerrain;
                    }

                    @Override
                    protected void setupTerrainPipeline() {
                        heightmap.set((x, y) -> x * y + 2f * x + 3f * y);
                    }

                    @Override
                    protected void setupPassablePipeline() {
                        assertHeightmapEquals(expected, heightmap);
                        passableStages.incrementAndGet();
                    }

                    @Override
                    protected void setupSpawnMaskPipeline() {
                        assertHeightmapEquals(expected, heightmap);
                        spawnStages.incrementAndGet();
                    }
                }
        };
        for (TerrainGenerator generator : generators) {
            generator.map = map;
            generator.generatorParameters = new GeneratorParameters(
                    numTeams == 0 ? 2 : numTeams, 64, numTeams, MapStyle.Predefined.BASIC, symmetrySettings, null);
            generator.heightmap = new FloatMask(65, new SplittableRandom(0), symmetrySettings);
            generator.setupPipeline();
            assertHeightmapEquals(expected, generator.heightmap);
        }
        assertEquals(2, passableStages.get());
        assertEquals(1, spawnStages.get());
    }

    private static void assertHeightmapEquals(FloatMask expected, FloatMask actual) {
        for (int x = 0; x < expected.getSize(); x++) {
            for (int y = 0; y < expected.getSize(); y++) {
                assertEquals(expected.getPrimitive(x, y), actual.getPrimitive(x, y));
            }
        }
    }
}
