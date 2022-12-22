package com.faforever.neroxis.graph;

import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import lombok.Getter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Random;

@Getter
public class SimpleGraphContext implements GraphContext {
    private final Random random;
    private final SymmetrySettings symmetrySettings;
    private final ExpressionParser parser;
    private final EvaluationContext evalContext;
    private final SCMap map;
    private final int mapSize;
    private final int numSymPoints;
    private String identifier;

    public SimpleGraphContext(long seed, SymmetrySettings symmetrySettings, SCMap map) {
        this.symmetrySettings = symmetrySettings;
        this.map = map;
        random = new Random(seed);
        numSymPoints = symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        mapSize = map.getSize();
        parser = new SpelExpressionParser();
        evalContext = new StandardEvaluationContext(this);
    }

    @Override
    public <T> T getValue(String expression, String identifier, Class<T> clazz) {
        this.identifier = identifier;
        return parser.parseExpression(expression).getValue(evalContext, clazz);
    }
}
