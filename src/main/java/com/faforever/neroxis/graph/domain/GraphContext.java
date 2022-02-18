package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.generator.placement.SpawnPlacer;
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
    private final MapParameters mapParameters;
    private final int mapSize;

    public GraphContext(long seed, MapParameters mapParameters) {
        this.mapParameters = mapParameters;
        map = new SCMap(mapParameters.getMapSize(), mapParameters.getBiome());
        mapSize = mapParameters.getMapSize();
        symmetrySettings = mapParameters.getSymmetrySettings();
        random = new Random(seed);
        parser = new SpelExpressionParser();
        evalContext = new StandardEvaluationContext(this);
        float spawnSeparation;
        int teamSeparation;
        if (mapParameters.getNumTeams() < 2) {
            spawnSeparation = (float) mapParameters.getMapSize() / mapParameters.getSpawnCount() * 1.5f;
            teamSeparation = 0;
        } else if (mapParameters.getNumTeams() == 2) {
            spawnSeparation = random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16f;
            teamSeparation = map.getSize() / mapParameters.getNumTeams();
        } else {
            if (mapParameters.getNumTeams() < 8) {
                spawnSeparation = random.nextInt(map.getSize() / 2 / mapParameters.getNumTeams() - map.getSize() / 16) + map.getSize() / 16f;
            } else {
                spawnSeparation = 0;
            }
            teamSeparation = map.getSize() / mapParameters.getNumTeams();
        }
        new SpawnPlacer(map, random.nextLong()).placeSpawns(mapParameters.getSpawnCount(), spawnSeparation, teamSeparation, symmetrySettings);
    }

    public <T> T getValue(String expression, Class<T> clazz) {
        return parser.parseExpression(expression).getValue(evalContext, clazz);
    }
}
