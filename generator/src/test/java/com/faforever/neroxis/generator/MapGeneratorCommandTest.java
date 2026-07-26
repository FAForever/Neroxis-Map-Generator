package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.exporter.PreviewGenerator;
import com.faforever.neroxis.generator.util.serial.GeneratedMapNameEncoder;
import com.faforever.neroxis.generator.util.serial.MapNameParameters;
import com.faforever.neroxis.generator.util.serial.MapStyle;
import com.faforever.neroxis.generator.util.serial.PropStyle;
import com.faforever.neroxis.generator.util.serial.ResourceStyle;
import com.faforever.neroxis.generator.util.serial.TerrainStyle;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.ImageUtil;
import com.faforever.neroxis.util.MapSymmetryTester;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
@Execution(ExecutionMode.CONCURRENT)
public class MapGeneratorCommandTest {
    public static final int NUM_DETERMINISM_REPEATS = 3;

    private final String[] keywordArgs = new String[]{"--seed", Long.toString(1234), "--spawn-count", Byte.toString(
            (byte) 2), "--terrain-style", TerrainStyle.BIG_ISLANDS.name(), "--texture-style", BiomeName.BRIMSTONE.name(), "--resource-style", ResourceStyle.LOW_MEX.name(), "--prop-style", PropStyle.ENEMY_CIV.name(), "--terrain-symmetry", Symmetry.XZ.name(), "--map-size", Integer.toString(
            256), "--resource-density", Float.toString(.7325f), "--reclaim-density", Float.toString(
            .1354f), "--num-teams", Integer.toString(2)};

    private static Stream<Symmetry> symmetries() {
        return Arrays.stream(Symmetry.values()).mapMulti(((symmetry, consumer) -> {
            for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                consumer.accept(symmetry);
            }
        }));
    }

    private static Stream<MapStyle.Predefined> mapStyles() {
        return Arrays.stream(MapStyle.Predefined.values()).mapMulti((mapStyle, consumer) -> {
            for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                consumer.accept(mapStyle);
            }
        });
    }

    private static Stream<TerrainStyle> terrainStyles() {
        return Arrays.stream(TerrainStyle.values()).mapMulti((generator, consumer) -> {
            for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                consumer.accept(generator);
            }
        });
    }

    private static Stream<BiomeName> biomeNames() {
        return Arrays.stream(BiomeName.values()).mapMulti((generator, consumer) -> {
            for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                consumer.accept(generator);
            }
        });
    }

    private static Stream<ResourceStyle> resourceStyles() {
        return Arrays.stream(ResourceStyle.values()).mapMulti((generator, consumer) -> {
            for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                consumer.accept(generator);
            }
        });
    }

    private static Stream<PropStyle> propStyles() {
        return Arrays.stream(PropStyle.values()).mapMulti((generator, consumer) -> {
            for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                consumer.accept(generator);
            }
        });
    }

    private static IntStream validMapSize() {
        return IntStream.iterate(256, size -> size < 512, size -> size + 64);
    }

    private static Stream<Arguments> validMapSizeAndTerrain() {
        return validMapSize().boxed()
                             .flatMap(mapSize -> Arrays.stream(TerrainStyle.values())
                                                       .map(terrainStyle -> Arguments.of(terrainStyle, mapSize)));
    }

    @ParameterizedTest
    @MethodSource("validMapSizeAndTerrain")
    public void TestAllTerrainsGenerateAllSizes(TerrainStyle terrainStyle, int mapSize) {
        MapGeneratorCommand instance = new MapGeneratorCommand();
        new CommandLine(instance).parseArgs("--terrain-style", terrainStyle.toString(), "--map-size",
                                            String.valueOf(mapSize), "--spawn-count", "2");
        MapNameParameters mapNameParameters = instance.createMapNameParameters();

        GenerationResults generationResults = MapGeneratorCommand.generate(mapNameParameters, false, false);
        SCMap map = generationResults.map();

        assertTrue(map.getDescription().contains(terrainStyle.name()),
                   map.getDescription() + " doesn't contain " + terrainStyle.name());
        assertEquals(mapSize, mapNameParameters.mapSize());
        assertEquals(mapSize, map.getPlayableArea().z() - map.getPlayableArea().x());
    }

    @ParameterizedTest
    @MethodSource("validMapSize")
    public void TestMapExportedToProperSize(int mapSize) {
        MapGeneratorCommand instance = new MapGeneratorCommand();
        new CommandLine(instance).parseArgs("--map-size", String.valueOf(mapSize), "--spawn-count", "2");

        MapNameParameters mapNameParameters = instance.createMapNameParameters();
        GenerationResults generationResults = MapGeneratorCommand.generate(mapNameParameters, false, false);
        SCMap map = generationResults.map();

        assertEquals(0, (StrictMath.log(map.getSize()) / StrictMath.log(2)) % 1);
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestDeterminism() throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();
        new CommandLine(instance1).parseArgs(keywordArgs);
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();

        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        new CommandLine(instance2).parseArgs(keywordArgs);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();

        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @Test
    public void TestMultipleGenerationDeterminism() throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();
        new CommandLine(instance1).parseArgs("--num-to-generate", "2", "--map-size", "256", "--spawn-count", "2");
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();

        MapGeneratorCommand instance2 = new MapGeneratorCommand();
        new CommandLine(instance2).parseArgs("--map-name", map1.getName());
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @Test
    public void TestEqualityMapNameKeyword() throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();
        new CommandLine(instance1).parseArgs(keywordArgs);
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        String[] args = {"--map-name", map1.getName()};
        new CommandLine(instance2).parseArgs(args);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityWithDebugMapNameKeyword() throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();
        new CommandLine(instance1).parseArgs(keywordArgs);
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        String[] args = {"--map-name", map1.getName(), "--debug"};
        new CommandLine(instance2).parseArgs(args);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityTournamentStyle() throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();
        new CommandLine(instance1).parseArgs("--tournament-style", "--map-size", "256", "--spawn-count", "2");
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters1);

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        new CommandLine(instance2).parseArgs("--map-name", mapName);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityBlind() throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();
        new CommandLine(instance1).parseArgs("--blind", "--map-size", "256", "--spawn-count", "2");
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters1);

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        new CommandLine(instance2).parseArgs("--map-name", mapName);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityUnexplored() throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();
        new CommandLine(instance1).parseArgs("--unexplored", "--map-size", "256", "--spawn-count", "2");
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters1);

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        new CommandLine(instance2).parseArgs("--map-name", mapName);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @ParameterizedTest
    @MethodSource("mapStyles")
    public void TestEqualityStyleSpecified(MapStyle.Predefined style) throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();

        new CommandLine(instance1).parseArgs("--style", style.toString(), "--map-size", "256", "--spawn-count", "2");
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters1);

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        new CommandLine(instance2).parseArgs("--map-name", mapName);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @ParameterizedTest
    @MethodSource("symmetries")
    public void TestEqualitySymmetrySpecified(Symmetry symmetry) throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();

        int numTeams = switch (symmetry) {
            case Symmetry s when s.getNumSymPoints() == 1 -> 0;
            case Symmetry s when s.getNumSymPoints() % 2 == 0 -> 2;
            case Symmetry s when s.getNumSymPoints() % 3 == 0 -> 3;
            case Symmetry s -> s.getNumSymPoints();
        };

        int spawnCount = numTeams == 0 ? 4 : numTeams;
        String mapSize = spawnCount > 8 ? "512" : "256";

        new CommandLine(instance1).parseArgs("--terrain-symmetry", symmetry.toString(), "--map-size", mapSize,
                                             "--num-teams", String.valueOf(numTeams), "--spawn-count",
                                             String.valueOf(spawnCount));
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters1);

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        new CommandLine(instance2).parseArgs("--map-name", mapName);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        if (generationResults1.symmetrySettings().spawnSymmetry().isPerfectSymmetry()) {
            assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
        }
    }

    @ParameterizedTest
    @MethodSource("terrainStyles")
    public void TestEqualityTerrainStyleSpecified(TerrainStyle terrainStyle) throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();

        new CommandLine(instance1).parseArgs("--terrain-style", terrainStyle.toString(), "--map-size", "256",
                                             "--spawn-count", "2");
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters1);

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        new CommandLine(instance2).parseArgs("--map-name", mapName);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(terrainStyle, generationResults1.terrainStyle());
        assertEquals(generationResults1.terrainStyle(), generationResults2.terrainStyle());
        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @ParameterizedTest
    @MethodSource("biomeNames")
    public void TestEqualityTextureStyleSpecified(BiomeName biomeName) throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();

        new CommandLine(instance1).parseArgs("--texture-style", biomeName.toString(), "--map-size", "256",
                                             "--spawn-count", "2");
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters1);

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        new CommandLine(instance2).parseArgs("--map-name", mapName);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(biomeName, generationResults1.biomeName());
        assertEquals(generationResults1.biomeName(), generationResults2.biomeName());
        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @ParameterizedTest
    @MethodSource("resourceStyles")
    public void TestEqualityResourceStyleSpecified(ResourceStyle resourceStyle) throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();

        new CommandLine(instance1).parseArgs("--resource-style", resourceStyle.toString(), "--map-size", "256",
                                             "--spawn-count", "2");
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters1);

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        new CommandLine(instance2).parseArgs("--map-name", mapName);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(resourceStyle, generationResults1.resourceStyle());
        assertEquals(generationResults1.resourceStyle(), generationResults2.resourceStyle());
        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @ParameterizedTest
    @MethodSource("propStyles")
    public void TestEqualityPropStyleSpecified(PropStyle propStyle) throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();

        new CommandLine(instance1).parseArgs("--prop-style", propStyle.toString(), "--map-size", "256", "--spawn-count",
                                             "2");
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters1);

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        new CommandLine(instance2).parseArgs("--map-name", mapName);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(propStyle, generationResults1.propStyle());
        assertEquals(generationResults1.propStyle(), generationResults2.propStyle());
        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityReclaimDensitySpecified() throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();

        new CommandLine(instance1).parseArgs("--reclaim-density", String.valueOf(new SplittableRandom().nextFloat()),
                                             "--map-size",
                                             "256", "--spawn-count", "2");
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters1);

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        new CommandLine(instance2).parseArgs("--map-name", mapName);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityResourceDensitySpecified() throws IOException {
        MapGeneratorCommand instance1 = new MapGeneratorCommand();

        new CommandLine(instance1).parseArgs("--resource-density", String.valueOf(new SplittableRandom().nextFloat()),
                                             "--map-size",
                                             "256", "--spawn-count", "2");
        MapNameParameters mapNameParameters1 = instance1.createMapNameParameters();
        GenerationResults generationResults1 = MapGeneratorCommand.generate(mapNameParameters1, false, false);
        SCMap map1 = generationResults1.map();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        generationResults1.writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters1);

        MapGeneratorCommand instance2 = new MapGeneratorCommand();

        new CommandLine(instance2).parseArgs("--map-name", mapName);
        MapNameParameters mapNameParameters2 = instance2.createMapNameParameters();
        GenerationResults generationResults2 = MapGeneratorCommand.generate(mapNameParameters2, false, false);
        SCMap map2 = generationResults2.map();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        generationResults2.writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(mapNameParameters1, mapNameParameters2);
        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
        assertSCMapSymmetric(map1, generationResults1.symmetrySettings());
    }

    @RepeatedTest(10)
    public void TestUnexploredNoUnits() {
        MapGeneratorCommand instance = new MapGeneratorCommand();
        new CommandLine(instance).parseArgs("--unexplored", "--map-size", "256", "--spawn-count", "2");
        MapNameParameters mapNameParameters = instance.createMapNameParameters();
        GenerationResults generationResults = MapGeneratorCommand.generate(mapNameParameters, false, false);
        SCMap map = generationResults.map();

        for (Army army : map.getArmies()) {
            for (Group group : army.getGroups()) {
                assertEquals(0, group.getUnits().size());
            }
        }
    }

    @Test
    public void TestUnexploredPreview() throws Exception {
        MapGeneratorCommand instance = new MapGeneratorCommand();
        new CommandLine(instance).parseArgs("--unexplored", "--map-size", "256", "--spawn-count", "2");
        MapNameParameters mapNameParameters = instance.createMapNameParameters();
        GenerationResults generationResults = MapGeneratorCommand.generate(mapNameParameters, false, false);
        SCMap map = generationResults.map();

        BufferedImage blankPreview = ImageUtil.readImage(PreviewGenerator.BLANK_PREVIEW);
        BufferedImage mapPreview = map.getPreview();

        assertArrayEquals(blankPreview.getRGB(0, 0, 256, 256, null, 0, 256),
                          mapPreview.getRGB(0, 0, 256, 256, null, 0, 256));
    }

    private void assertSCMapSymmetric(SCMap map, SymmetrySettings symmetrySettings) {
        if (!symmetrySettings.spawnSymmetry().isPerfectSymmetry() ||
            symmetrySettings.spawnSymmetry() == Symmetry.NONE) {
            return;
        }
        MapSymmetryTester.Result symmetryResult = MapSymmetryTester.evaluate(map, symmetrySettings);
        assertTrue(symmetryResult.isSymmetric(),
                   () -> "Map %s not symmetric: %s".formatted(map.getName(), symmetryResult));
    }

    private void assertSCMapEquality(SCMap map1, SCMap map2) {
        Function<String, Supplier<@Nullable String>> messageProvider = (type) -> () -> "%s mismatched for map %s".formatted(
                type,
                map1.getName());
        assertEquals(map1.getName(), map2.getName(), messageProvider.apply("Name"));
        assertEquals(map1.getSpawns(), map2.getSpawns(), messageProvider.apply("Spawns"));
        assertEquals(map1.getMexes(), map2.getMexes(), messageProvider.apply("Mexes"));
        assertEquals(map1.getHydros(), map2.getHydros(), messageProvider.apply("Hydros"));
        assertEquals(map1.getArmies(), map2.getArmies(), messageProvider.apply("Armies"));
        assertEquals(map1.getProps(), map2.getProps(), messageProvider.apply("Props"));
        assertEquals(map1.getBiome(), map2.getBiome(), messageProvider.apply("Biome"));
        assertEquals(map1.getSize(), map2.getSize(), messageProvider.apply("Size"));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getPreview()), ImageUtil.getImagePixels(map2.getPreview()),
                          messageProvider.apply("Preview"));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getHeightmap()), ImageUtil.getImagePixels(map2.getHeightmap()),
                          messageProvider.apply("Height Map"));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getNormalMap()), ImageUtil.getImagePixels(map2.getNormalMap()),
                          messageProvider.apply("Normal Map"));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getTextureMasksHigh()),
                          ImageUtil.getImagePixels(map2.getTextureMasksHigh()),
                          messageProvider.apply("Texture Masks High"));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getTextureMasksLow()),
                          ImageUtil.getImagePixels(map2.getTextureMasksLow()),
                          messageProvider.apply("Texture Masks Low"));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getWaterMap()), ImageUtil.getImagePixels(map2.getWaterMap()),
                          messageProvider.apply("Water"));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getWaterFoamMap()),
                          ImageUtil.getImagePixels(map2.getWaterFoamMap()), messageProvider.apply("Water Foam"));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getWaterDepthBiasMap()),
                          ImageUtil.getImagePixels(map2.getWaterDepthBiasMap()), messageProvider.apply("Wated Depth"));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getWaterShadowMap()),
                          ImageUtil.getImagePixels(map2.getWaterShadowMap()), messageProvider.apply("Water Shadow"));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getTerrainType()),
                          ImageUtil.getImagePixels(map2.getTerrainType()), messageProvider.apply("Terrain Type"));
    }
}


