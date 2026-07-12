package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.generator.util.serial.GeneratedMapNameEncoder;
import com.faforever.neroxis.generator.util.serial.MapNameParameters;
import com.faforever.neroxis.generator.util.serial.MapStyle;
import com.faforever.neroxis.generator.util.serial.PropStyle;
import com.faforever.neroxis.generator.util.serial.ResourceStyle;
import com.faforever.neroxis.generator.util.serial.TerrainStyle;
import com.faforever.neroxis.generator.util.serial.Visibility;
import com.faforever.neroxis.map.Symmetry;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
@Execution(ExecutionMode.CONCURRENT)
public class GeneratedMapNameEncoderTest {

    private static void assertMapNameParametersEncodable(MapNameParameters mapNameParameters) {
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters);
        assertEquals(mapNameParameters, GeneratedMapNameEncoder.decode(mapName));
    }

    private static Stream<MapStyle> styles() {
        RandomGenerator randomGenerator = new SplittableRandom();
        Stream.Builder<MapStyle.Custom> customStreamBuilder = Stream.builder();
        for (TerrainStyle terrainStyle : TerrainStyle.values()) {
            customStreamBuilder.add(
                    new MapStyle.Custom(terrainStyle, BiomeName.BRIMSTONE, PropStyle.BASIC, ResourceStyle.BASIC,
                                        randomGenerator.nextFloat(), randomGenerator.nextFloat()));
        }
        for (BiomeName biomeName : BiomeName.values()) {
            customStreamBuilder.add(
                    new MapStyle.Custom(TerrainStyle.BASIC, biomeName, PropStyle.BASIC, ResourceStyle.BASIC,
                                        randomGenerator.nextFloat(), randomGenerator.nextFloat()));
        }
        for (PropStyle propStyle : PropStyle.values()) {
            customStreamBuilder.add(
                    new MapStyle.Custom(TerrainStyle.BASIC, BiomeName.BRIMSTONE, propStyle, ResourceStyle.BASIC,
                                        randomGenerator.nextFloat(), randomGenerator.nextFloat()));
        }
        for (ResourceStyle resourceStyle : ResourceStyle.values()) {
            customStreamBuilder.add(
                    new MapStyle.Custom(TerrainStyle.BASIC, BiomeName.BRIMSTONE, PropStyle.BASIC, resourceStyle,
                                        randomGenerator.nextFloat(), randomGenerator.nextFloat()));
        }

        return Stream.concat(Arrays.stream(MapStyle.Predefined.values()), customStreamBuilder.build());
    }

    private static Stream<Arguments> symmetryStyles() {
        return Arrays.stream(Symmetry.values())
                     .flatMap(symmetry -> styles().map(mapStyle -> Arguments.of(symmetry, mapStyle)));
    }

    private static IntStream mapSizes() {
        return IntStream.iterate(0, i -> i < 4096, i -> i + 64);
    }

    private static IntStream spawnCounts() {
        return IntStream.range(0, 17);
    }

    @ParameterizedTest
    @MethodSource("mapSizes")
    public void testMapSizes(int mapSize) {
        MapNameParameters mapNameParameters = new MapNameParameters(0L, 6, mapSize, 2,
                                                                    new MapNameParameters.Casual(null, null));
        assertMapNameParametersEncodable(mapNameParameters);
    }

    @ParameterizedTest
    @MethodSource("spawnCounts")
    public void testSpawnTeam(int spawnCount) {
        MapNameParameters mapNameParameters = new MapNameParameters(0L, spawnCount, 512, spawnCount,
                                                                    new MapNameParameters.Casual(null, null));
        assertMapNameParametersEncodable(mapNameParameters);
    }

    @RepeatedTest(10)
    public void testSeeds() {
        MapNameParameters mapNameParameters = new MapNameParameters(new SplittableRandom().nextLong(), 6, 512, 2,
                                                                    new MapNameParameters.Casual(null, null));
        assertMapNameParametersEncodable(mapNameParameters);
    }

    @ParameterizedTest
    @EnumSource
    public void testTerrainSymmetryOnly(Symmetry terrainSymmetry) {
        MapNameParameters mapNameParameters = new MapNameParameters(0L, terrainSymmetry.getNumSymPoints(), 512,
                                                                    terrainSymmetry.getNumSymPoints(),
                                                                    new MapNameParameters.Casual(terrainSymmetry,
                                                                                                 null));
        assertMapNameParametersEncodable(mapNameParameters);
    }

    @ParameterizedTest
    @MethodSource("styles")
    public void testStyleOnly(MapStyle style) {
        MapNameParameters mapNameParameters = new MapNameParameters(0L, 6, 512, 2,
                                                                    new MapNameParameters.Casual(null, style));
        assertMapNameParametersEncodable(mapNameParameters);
    }

    @ParameterizedTest
    @MethodSource("symmetryStyles")
    public void testStyleWithTerrainSymmetry(Symmetry symmetry, MapStyle style) {
        MapNameParameters mapNameParameters = new MapNameParameters(0L, symmetry.getNumSymPoints(), 512,
                                                                    symmetry.getNumSymPoints(),
                                                                    new MapNameParameters.Casual(symmetry, style));
        assertMapNameParametersEncodable(mapNameParameters);
    }

    @ParameterizedTest
    @EnumSource
    public void testCompetitive(Visibility visibility) {
        MapNameParameters mapNameParameters = new MapNameParameters(0L, 6, 512, 2,
                                                                    new MapNameParameters.Competitive(1L, visibility));
        assertMapNameParametersEncodable(mapNameParameters);
    }
}
