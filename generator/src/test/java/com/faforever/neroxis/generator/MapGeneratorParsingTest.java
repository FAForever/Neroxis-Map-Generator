package com.faforever.neroxis.generator;

import com.faforever.neroxis.util.MathUtil;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
public class MapGeneratorParsingTest {
    String mapName = "neroxis_map_generator_snapshot_aaaaaaaaaacne_aicaebsicfsuqek5cm";
    long seed = 1234;
    byte spawnCount = 2;
    float landDensity = .56746f;
    float plateauDensity = .1324f;
    float mountainDensity = .7956f;
    float rampDensity = .5649f;
    float reclaimDensity = .1354f;
    float mexDensity = .7325f;
    float roundedLandDensity = MathUtil.discretePercentage(landDensity, 127);
    float roundedPlateauDensity = MathUtil.discretePercentage(plateauDensity, 127);
    float roundedMountainDensity = MathUtil.discretePercentage(mountainDensity, 127);
    float roundedRampDensity = MathUtil.discretePercentage(rampDensity, 127);
    float roundedReclaimDensity = MathUtil.discretePercentage(reclaimDensity, 127);
    float roundedMexDensity = MathUtil.discretePercentage(mexDensity, 127);
    int mapSize = 256;
    int numTeams = 2;
    String[] keywordArgs;
    private MapGenerator instance;

    @BeforeEach
    public void setup() {
        keywordArgs = new String[]{"--seed", Long.toString(seed), "--spawn-count", Byte.toString(spawnCount),
                                   "--land-density", Float.toString(landDensity), "--plateau-density",
                                   Float.toString(plateauDensity), "--mountain-density",
                                   Float.toString(mountainDensity), "--ramp-density", Float.toString(rampDensity),
                                   "--reclaim-density", Float.toString(reclaimDensity), "--mex-density",
                                   Float.toString(mexDensity), "--map-size", Integer.toString(mapSize), "--num-teams",
                                   Integer.toString(numTeams)};

        instance = new MapGenerator();
    }

    @Test
    public void TestParseLadderMapName() {
        new CommandLine(instance).parseArgs("--map-name", "neroxis_map_generator_snapshot_b4zeogjzndhtk_aiea");
        instance.populateGeneratorParametersAndName();

        assertEquals(instance.getSeed(), ByteBuffer.wrap(GeneratedMapNameEncoder.decode("b4zeogjzndhtk")).getLong());
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

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getOutputFolderMixin().getOutputPath(), Path.of("."));
        assertEquals(generatorParameters.landDensity(), roundedLandDensity);
        assertEquals(generatorParameters.plateauDensity(), roundedPlateauDensity);
        assertEquals(generatorParameters.mountainDensity(), roundedMountainDensity);
        assertEquals(generatorParameters.rampDensity(), roundedRampDensity);
        assertEquals(generatorParameters.reclaimDensity(), roundedReclaimDensity);
        assertEquals(generatorParameters.mexDensity(), roundedMexDensity);
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
                     () -> new CommandLine(instance).parseArgs("--unexplored", "--land-density", "1"));
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--land-density", "1", "--style", "TEST"));
    }

    private static class AllMapSizeArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return IntStream.rangeClosed(0, 2048).mapToObj(Arguments::of);
        }
    }
}


