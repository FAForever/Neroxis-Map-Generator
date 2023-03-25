package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.brushes.Brushes;
import com.faforever.neroxis.graph.GraphContext;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.SymmetrySelector;
import lombok.Getter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Random;

@Getter
public class GeneratorGraphContext implements GraphContext {
    private final Random random;
    private final SymmetrySettings symmetrySettings;
    private final ExpressionParser parser;
    private final EvaluationContext evalContext;
    private final SCMap map;
    private final Biome biome;
    private final float landDensity;
    private final float plateauDensity;
    private final float mountainDensity;
    private final float rampDensity;
    private final float reclaimDensity;
    private final float mexDensity;
    private final float plateauHeight;
    private final float landHeight;
    private final float waterHeight;
    private final int mapSize;
    private final int numSymPoints;
    private final String mountainBrush;
    private final String plateauBrush;
    private final String oceanBrush;
    private String identifier;

    public GeneratorGraphContext(long seed, GeneratorParameters generatorParameters,
                                 ParameterConstraints parameterConstraints) {
        random = new Random(seed);
        mountainBrush = Brushes.MOUNTAIN_BRUSHES.get(random.nextInt(Brushes.MOUNTAIN_BRUSHES.size()));
        plateauBrush = Brushes.MOUNTAIN_BRUSHES.get(random.nextInt(Brushes.MOUNTAIN_BRUSHES.size()));
        oceanBrush = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));
        symmetrySettings = SymmetrySelector.getSymmetrySettingsFromTerrainSymmetry(random,
                                                                                   generatorParameters.terrainSymmetry(),
                                                                                   generatorParameters.spawnCount(),
                                                                                   generatorParameters.numTeams());
        biome = generatorParameters.biome();
        numSymPoints = symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        landDensity = parameterConstraints.getLandDensityRange().normalize(generatorParameters.landDensity());
        plateauDensity = parameterConstraints.getPlateauDensityRange()
                                             .normalize(generatorParameters.plateauDensity());
        mountainDensity = parameterConstraints.getMountainDensityRange()
                                              .normalize(generatorParameters.mountainDensity());
        rampDensity = parameterConstraints.getRampDensityRange().normalize(generatorParameters.rampDensity());
        mexDensity = parameterConstraints.getMexDensityRange().normalize(generatorParameters.mexDensity());
        reclaimDensity = parameterConstraints.getReclaimDensityRange().normalize(generatorParameters.mexDensity());
        plateauHeight = 6f;
        landHeight = .25f;
        waterHeight = biome.waterSettings().getElevation();
        map = new SCMap(generatorParameters.mapSize(), generatorParameters.biome());
        mapSize = generatorParameters.mapSize();
        parser = new SpelExpressionParser();
        evalContext = new StandardEvaluationContext(this);
    }

    @Override
    public <T> T getValue(String expression, String identifier, Class<T> clazz) {
        this.identifier = identifier;
        return parser.parseExpression(expression).getValue(evalContext, clazz);
    }
}
