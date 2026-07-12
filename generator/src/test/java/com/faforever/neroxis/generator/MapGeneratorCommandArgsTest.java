package com.faforever.neroxis.generator;


import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.generator.util.serial.GeneratedMapNameEncoder;
import com.faforever.neroxis.generator.util.serial.MapNameParameters;
import com.faforever.neroxis.generator.util.serial.MapStyle;
import com.faforever.neroxis.generator.util.serial.PropStyle;
import com.faforever.neroxis.generator.util.serial.ResourceStyle;
import com.faforever.neroxis.generator.util.serial.TerrainStyle;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.util.MathUtil;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
@Execution(ExecutionMode.CONCURRENT)
public class MapGeneratorCommandArgsTest {
    String mapName = "neroxis_map_generator_snapshot_aaaaaaaaaacne_aicaedyaaiaqeek5";
    long seed = 1234;
    byte spawnCount = 2;
    TerrainStyle terrainStyle = TerrainStyle.BIG_ISLANDS;
    BiomeName biomeName = BiomeName.BRIMSTONE;
    ResourceStyle resourceStyle = ResourceStyle.LOW_MEX;
    PropStyle propStyle = PropStyle.ENEMY_CIV;
    float reclaimDensity = .1354f;
    float resourceDensity = .7325f;
    float roundedReclaimDensity = MathUtil.discretePercentage(reclaimDensity, 127);
    float roundedResourceDensity = MathUtil.discretePercentage(resourceDensity, 127);
    Symmetry symmetry = Symmetry.XZ;
    int mapSize = 256;
    int numTeams = 2;
    String[] keywordArgs;
    private MapGeneratorCommand instance;

    private static IntStream allMapSizes() {
        return IntStream.rangeClosed(0, 2048);
    }

    @BeforeEach
    public void setup() {
        keywordArgs = new String[]{"--seed", Long.toString(seed), "--spawn-count", Byte.toString(
                spawnCount), "--terrain-style", terrainStyle.name(), "--texture-style", biomeName.name(), "--resource-style", resourceStyle.name(), "--prop-style", propStyle.name(), "--terrain-symmetry", symmetry.name(), "--map-size", Integer.toString(
                mapSize), "--resource-density", Float.toString(resourceDensity), "--reclaim-density", Float.toString(
                reclaimDensity), "--num-teams", Integer.toString(numTeams)};

        instance = new MapGeneratorCommand();
    }

    @Test
    public void TestParseLadderMapName() {
        new CommandLine(instance).parseArgs("--map-name", "neroxis_map_generator_snapshot_aaaaaaaaaacne_aiea");
        MapNameParameters mapNameParameters = instance.createMapNameParameters();

        assertEquals(1234, mapNameParameters.seed());
        assertEquals(512, mapNameParameters.mapSize());
        assertEquals(2, mapNameParameters.spawnCount());
        assertEquals(2, mapNameParameters.numTeams());
    }

    @Test
    public void TestParseMapName() {
        new CommandLine(instance).parseArgs("--map-name", mapName);
        MapNameParameters mapNameParameters = instance.createMapNameParameters();

        assertInstanceOf(MapNameParameters.Casual.class, mapNameParameters.mode());
        MapNameParameters.Casual casual = (MapNameParameters.Casual) mapNameParameters.mode();
        assertInstanceOf(MapStyle.Custom.class, casual.mapStyle());
        MapStyle.Custom customStyle = (MapStyle.Custom) casual.mapStyle();

        assertEquals(seed, mapNameParameters.seed());
        assertEquals(mapName, GeneratedMapNameEncoder.encode(mapNameParameters));
        assertEquals(spawnCount, mapNameParameters.spawnCount());
        assertEquals(mapSize, mapNameParameters.mapSize());
        assertEquals(numTeams, mapNameParameters.numTeams());
        assertEquals(symmetry, casual.terrainSymmetry());
        assertEquals(terrainStyle, customStyle.terrainStyle());
        assertEquals(biomeName, customStyle.biomeName());
        assertEquals(resourceStyle, customStyle.resourceStyle());
        assertEquals(propStyle, customStyle.propStyle());
        assertEquals(roundedReclaimDensity, customStyle.reclaimDensity());
        assertEquals(roundedResourceDensity, customStyle.resourceDensity());
    }

    @Test
    public void TestParseKeywordArgs() {
        new CommandLine(instance).parseArgs(keywordArgs);
        MapNameParameters mapNameParameters = instance.createMapNameParameters();

        assertInstanceOf(MapNameParameters.Casual.class, mapNameParameters.mode());
        MapNameParameters.Casual casual = (MapNameParameters.Casual) mapNameParameters.mode();
        assertInstanceOf(MapStyle.Custom.class, casual.mapStyle());
        MapStyle.Custom customStyle = (MapStyle.Custom) casual.mapStyle();

        assertEquals(seed, mapNameParameters.seed());
        assertEquals(mapName, GeneratedMapNameEncoder.encode(mapNameParameters));
        assertEquals(spawnCount, mapNameParameters.spawnCount());
        assertEquals(mapSize, mapNameParameters.mapSize());
        assertEquals(numTeams, mapNameParameters.numTeams());
        assertEquals(symmetry, casual.terrainSymmetry());
        assertEquals(terrainStyle, customStyle.terrainStyle());
        assertEquals(biomeName, customStyle.biomeName());
        assertEquals(resourceStyle, customStyle.resourceStyle());
        assertEquals(propStyle, customStyle.propStyle());
        assertEquals(roundedReclaimDensity, customStyle.reclaimDensity());
        assertEquals(roundedResourceDensity, customStyle.resourceDensity());
    }

    @ParameterizedTest
    @MethodSource("allMapSizes")
    public void TestParseMapSizesInteger(int mapSize) {
        MapGeneratorCommand command = new MapGeneratorCommand();
        String sizeStringValue = String.valueOf(mapSize);

        if (mapSize % 64 == 0) {
            new CommandLine(command).parseArgs("--map-size", sizeStringValue);

            assertEquals(StrictMath.round(mapSize / 64f) * 64, command.createMapNameParameters().mapSize());
        } else {
            assertThrows(CommandLine.ParameterException.class,
                         () -> new CommandLine(command).parseArgs("--map-size", sizeStringValue));
        }
    }

    @ParameterizedTest
    @MethodSource("allMapSizes")
    public void TestParseMapSizesString(int mapSize) {
        MapGeneratorCommand command = new MapGeneratorCommand();
        String sizeStringValue = mapSize / 51.2f + "km";

        if (mapSize % 64 == 0) {
            new CommandLine(command).parseArgs("--map-size", sizeStringValue);

            assertEquals(StrictMath.round(mapSize / 64f) * 64, command.createMapNameParameters().mapSize());
        } else {
            assertThrows(CommandLine.ParameterException.class,
                         () -> new CommandLine(command).parseArgs("--map-size", sizeStringValue));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(SymmetryNumTeamsSpawnCountProvider.class)
    public void TestParseNumTeamsSpawnSymmetry(Symmetry symmetry, int numTeams, int spawnCount) {
        MapGeneratorCommand command = new MapGeneratorCommand();
        String[] args = new String[]{"--terrain-symmetry", symmetry.name(), "--num-teams", String.valueOf(
                numTeams), "--spawn-count", String.valueOf(spawnCount)};
        if (numTeams == 0 || (symmetry.getNumSymPoints() % numTeams == 0 && spawnCount % numTeams == 0)) {
            new CommandLine(command).parseArgs(args);
            MapNameParameters mapNameParameters = command.createMapNameParameters();

            assertInstanceOf(MapNameParameters.Casual.class, mapNameParameters.mode());
            MapNameParameters.Casual casual = (MapNameParameters.Casual) mapNameParameters.mode();

            assertEquals(symmetry, casual.terrainSymmetry());
            assertEquals(numTeams, mapNameParameters.numTeams());
            assertEquals(spawnCount, mapNameParameters.spawnCount());
        } else {
            assertThrows(CommandLine.ParameterException.class, () -> {
                new CommandLine(command).parseArgs(args);
                command.createMapNameParameters();
            });
        }
    }

    @Test
    public void TestMultiVisibilityOptionsFail() {
        instance = new MapGeneratorCommand();
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

    private static class SymmetryNumTeamsSpawnCountProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters,
                                                            ExtensionContext context) {
            return Arrays.stream(Symmetry.values()).mapMulti((symmetry, consumer) -> {
                for (int i = 0; i <= 16; i++) {
                    for (int j = 1; j <= 16; j++) {
                        consumer.accept(Arguments.of(symmetry, i, j));
                    }
                }
            });
        }
    }

    @Test
    public void TestMultiTuningOptionsFail() {
        instance = new MapGeneratorCommand();
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--unexplored", "--style", "TEST"));
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--tournament", "--seed", "1"));
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--blind", "--terrain-symmetry", "XZ"));
        assertThrows(CommandLine.ParameterException.class,
                     () -> new CommandLine(instance).parseArgs("--texture-generator", "TEST", "--style", "TEST"));
    }
}


