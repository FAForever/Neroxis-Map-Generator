package com.faforever.neroxis.generator.graph.domain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.SpawnPlacer;
import lombok.Getter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Random;

@Getter
public strictfp class GraphContext {
    private final Random random;
    private final SymmetrySettings symmetrySettings;
    private final ExpressionParser parser;
    private final EvaluationContext evalContext;
    private final SCMap map;
    private final GeneratorParameters generatorParameters;
    private final int mapSize;

    public GraphContext(long seed, GeneratorParameters generatorParameters) {
        this.generatorParameters = generatorParameters;
        map = new SCMap(generatorParameters.getMapSize(), generatorParameters.getBiome());
        mapSize = generatorParameters.getMapSize();
        symmetrySettings = generatorParameters.getSymmetrySettings();
        random = new Random(seed);
        parser = new SpelExpressionParser();
        evalContext = new StandardEvaluationContext(this);
        float spawnSeparation;
        int teamSeparation;
        if (generatorParameters.getNumTeams() < 2) {
            spawnSeparation = (float) generatorParameters.getMapSize() / generatorParameters.getSpawnCount() * 1.5f;
            teamSeparation = 0;
        } else if (generatorParameters.getNumTeams() == 2) {
            spawnSeparation = random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16f;
            teamSeparation = map.getSize() / generatorParameters.getNumTeams();
        } else {
            if (generatorParameters.getNumTeams() < 8) {
                spawnSeparation = random.nextInt(map.getSize() / 2 / generatorParameters.getNumTeams() - map.getSize() / 16) + map.getSize() / 16f;
            } else {
                spawnSeparation = 0;
            }
            teamSeparation = map.getSize() / generatorParameters.getNumTeams();
        }
        new SpawnPlacer(map, random.nextLong()).placeSpawns(generatorParameters.getSpawnCount(), spawnSeparation, teamSeparation, symmetrySettings);
    }

    public <T> T getValue(String expression, Class<T> clazz) {
        return parser.parseExpression(expression).getValue(evalContext, clazz);
    }
}
