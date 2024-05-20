package com.faforever.neroxis.generator;

import com.faforever.neroxis.map.Symmetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import picocli.CommandLine;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
public class MapGeneratorParsingTest {
    String mapName = "neroxis_map_generator_snapshot_aaaaaaaaaacne_aicaeey";
    long seed = 1234;
    byte spawnCount = 2;
    int mapSize = 256;
    int numTeams = 2;
    String[] keywordArgs;
    private MapGenerator instance;

    @BeforeEach
    public void setup() {
        keywordArgs = new String[]{"--seed", Long.toString(seed), "--spawn-count", Byte.toString(spawnCount),
                                   "--map-size", Integer.toString(mapSize), "--num-teams",
                                   Integer.toString(numTeams)};

        instance = new MapGenerator();
    }

    @Test
    public void TestParseLadderMapName() {
        new CommandLine(instance).parseArgs("--map-name", "neroxis_map_generator_snapshot_b4zeogjzndhtk_aiea");
        instance.populateGeneratorParametersAndName();

        assertEquals(instance.getBasicOptions().getSeed(),
                     ByteBuffer.wrap(GeneratedMapNameEncoder.decode("b4zeogjzndhtk")).getLong());
        assertEquals(instance.getOutputFolderMixin().getOutputPath(), Path.of("."));
        assertEquals(instance.getGeneratorParameters().mapSize(), 512);
        assertEquals(instance.getGeneratorParameters().spawnCount(), 2);
        assertEquals(instance.getGeneratorParameters().numTeams(), 2);
    }

    @Test
    public void TestParseKeywordArgs() {
        new CommandLine(instance).parseArgs(keywordArgs);
        instance.populateGeneratorParametersAndName();
        GeneratorParameters generatorParameters = instance.getGeneratorParameters();

        assertEquals(instance.getBasicOptions().getSeed(), seed);
        assertEquals(instance.getOutputFolderMixin().getOutputPath(), Path.of("."));
        assertEquals(generatorParameters.numTeams(), numTeams);
        assertEquals(generatorParameters.mapSize(), mapSize);
        assertEquals(instance.getMapName(), mapName);
    }

    @ParameterizedTest
    @ArgumentsSource(AllMapSizeArgumentProvider.class)
    public void TestParseMapSizesInteger(int mapSize) {
        MapGenerator command = new MapGenerator();
        String sizeStringValue = String.valueOf(mapSize);

        if (mapSize % 64 == 0) {
            new CommandLine(command).parseArgs("--map-size", sizeStringValue);
            command.populateGeneratorParametersAndName();
            GeneratorParameters generatorParameters = command.getGeneratorParameters();

            assertEquals(StrictMath.round(mapSize / 64f) * 64, generatorParameters.mapSize());
        } else {
            assertThrows(CommandLine.ParameterException.class,
                         () -> new CommandLine(command).parseArgs("--map-size", sizeStringValue));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(AllMapSizeArgumentProvider.class)
    public void TestParseMapSizesString(int mapSize) {
        MapGenerator command = new MapGenerator();
        String sizeStringValue = mapSize / 51.2f + "km";

        if (mapSize % 64 == 0) {
            new CommandLine(command).parseArgs("--map-size", sizeStringValue);
            command.populateGeneratorParametersAndName();
            GeneratorParameters generatorParameters = command.getGeneratorParameters();

            assertEquals(StrictMath.round(mapSize / 64f) * 64, generatorParameters.mapSize());
        } else {
            assertThrows(CommandLine.ParameterException.class,
                         () -> new CommandLine(command).parseArgs("--map-size", sizeStringValue));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(SymmetryNumTeamsSpawnCountProvider.class)
    public void TestParseNumTeamsSpawnSymmetry(Symmetry symmetry, int numTeams, int spawnCount) {
        MapGenerator command = new MapGenerator();
        String[] args = new String[]{"--terrain-symmetry", symmetry.name(), "--num-teams", String.valueOf(numTeams),
                                     "--spawn-count", String.valueOf(spawnCount)};
        if (numTeams == 0 || (symmetry.getNumSymPoints() % numTeams == 0 && spawnCount % numTeams == 0)) {
            new CommandLine(command).parseArgs(args);
            command.populateGeneratorParametersAndName();
            GeneratorParameters generatorParameters = command.getGeneratorParameters();

            assertEquals(symmetry, generatorParameters.terrainSymmetry());
            assertEquals(numTeams, generatorParameters.numTeams());
            assertEquals(spawnCount, generatorParameters.spawnCount());
        } else {
            assertThrows(CommandLine.ParameterException.class,
                         () -> {
                             new CommandLine(command).parseArgs(args);
                             command.populateGeneratorParametersAndName();
                         });
        }
    }

    @Test
    public void TestMultiVisibilityOptionsFail() {
        instance = new MapGenerator();
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--unexplored", "--blind"));
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--tournament-style", "--blind"));
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--unexplored", "--tournament-style"));
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--visibility", "BLIND", "--blind"));
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--visibility", "TOURNAMENT_STYLE",
                                                               "--tournament-style"));
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--visibility", "UNEXPLORED", "--unexplored"));
    }

    @Test
    public void TestMultiTuningOptionsFail() {
        instance = new MapGenerator();
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--unexplored", "--style", "TEST"));
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--unexplored", "--terrain-symmetry", "XZ"));
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--texture-generator", "TEST", "--style", "TEST"));
    }

    private static class SymmetryNumTeamsSpawnCountProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(Symmetry.values()).mapMulti(((symmetry, consumer) -> {
                for (int i = 0; i <= 16; i++) {
                    for (int j = 1; j <= 16; j++) {
                        consumer.accept(Arguments.of(symmetry, i, j));
                    }
                }
            }));
        }
    }

    private static class AllMapSizeArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return IntStream.rangeClosed(0, 2048).mapToObj(Arguments::of);
        }
    }
}


