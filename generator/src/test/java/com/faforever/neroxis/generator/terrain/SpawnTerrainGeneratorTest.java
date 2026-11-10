package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.util.serial.MapStyle;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpawnTerrainGeneratorTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 5, 7})
    public void bothPipelinesChooseRotationForOddTeams(int numTeams) {
        Symmetry symmetry = numTeams < 2 ? Symmetry.NONE : Symmetry.valueOf("POINT" + numTeams);
        SymmetrySettings symmetrySettings = new SymmetrySettings(symmetry);
        SCMap map = new SCMap(16, Biomes.loadBiome(BiomeName.BRIMSTONE));
        FloatMask expected = new FloatMask(17, null, symmetrySettings).set((x, y) -> x * y + 2f * x + 3f * y);
        if (numTeams % 2 != 0) {
            expected.forceSmoothTerrainRotation(numTeams, map.getBiome().waterSettings().elevation());
        } else {
            expected.forceSymmetry();
        }

        AtomicInteger passableStages = new AtomicInteger();
        AtomicInteger spawnStages = new AtomicInteger();
        TerrainGenerator[] generators = {
                new SpawnFirstTerrainGenerator() {
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
                    numTeams == 0 ? 2 : numTeams, 16, numTeams, MapStyle.Predefined.BASIC, symmetrySettings, null);
            generator.heightmap = new FloatMask(17, null, symmetrySettings);
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
