package com.faforever.neroxis.generator;

import com.faforever.neroxis.map.Symmetry;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
@Execution(ExecutionMode.CONCURRENT)
public class GeneratedMapNameEncoderTest {

    private static void assertGenerationDetailsReproducible(GeneratorParameters generatorParameters) {
        String mapName = GeneratedMapNameEncoder.encode(generatorParameters);
        assertEquals(generatorParameters, GeneratedMapNameEncoder.decode(mapName));
    }

    @Test
    public void testBasicDetails() {
        GeneratorParameters generatorParameters = new GeneratorParameters(0L, 6, 512, 2,
                                                                          new GeneratorParameters.Casual(null, null));
        assertGenerationDetailsReproducible(generatorParameters);
    }

    @Test
    public void testTerrainSymmetryOnly() {
        GeneratorParameters generatorParameters = new GeneratorParameters(0L, 6, 512, 2, new GeneratorParameters.Casual(
                Symmetry.POINT2, null));
        assertGenerationDetailsReproducible(generatorParameters);
    }

    @Test
    public void testPredefinedStyleOnly() {
        GeneratorParameters generatorParameters = new GeneratorParameters(0L, 6, 512, 2,
                                                                          new GeneratorParameters.Casual(null,
                                                                                                         MapStyle.Predefined.BASIC));
        assertGenerationDetailsReproducible(generatorParameters);
    }

    @Test
    public void testCustomStyleOnly() {
        GeneratorParameters generatorParameters = new GeneratorParameters(0L, 6, 512, 2,
                                                                          new GeneratorParameters.Casual(null,
                                                                                                         new MapStyle.Custom(
                                                                                                                 TerrainStyle.BASIC,
                                                                                                                 TextureStyle.BRIMSTONE,
                                                                                                                 PropStyle.BASIC,
                                                                                                                 ResourceStyle.BASIC,
                                                                                                                 .22356457243f,
                                                                                                                 .3602441f)));
        assertGenerationDetailsReproducible(generatorParameters);
    }

    @Test
    public void testPredefinedStyleWithTerrainSymmetry() {
        GeneratorParameters generatorParameters = new GeneratorParameters(0L, 6, 512, 2, new GeneratorParameters.Casual(
                Symmetry.POINT2, MapStyle.Predefined.BASIC));
        assertGenerationDetailsReproducible(generatorParameters);
    }

    @Test
    public void testCustomStyleWithTerrainSymmetry() {
        GeneratorParameters generatorParameters = new GeneratorParameters(0L, 6, 512, 2, new GeneratorParameters.Casual(
                Symmetry.POINT2,
                new MapStyle.Custom(TerrainStyle.BASIC, TextureStyle.BRIMSTONE, PropStyle.BASIC, ResourceStyle.BASIC,
                                    .22356457243f, .3602441f)));
        assertGenerationDetailsReproducible(generatorParameters);
    }

    @Test
    public void testCompetitive() {
        GeneratorParameters generatorParameters = new GeneratorParameters(0L, 6, 512, 2,
                                                                          new GeneratorParameters.Competitive(1L,
                                                                                                              Visibility.TOURNAMENT));
        assertGenerationDetailsReproducible(generatorParameters);
    }

}
