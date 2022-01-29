package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.map.SymmetrySettings;
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

    public GraphContext(long seed, SymmetrySettings symmetrySettings) {
        this.symmetrySettings = symmetrySettings;
        random = new Random(seed);
        parser = new SpelExpressionParser();
        evalContext = new StandardEvaluationContext(this);
    }

    public <T> T getValue(String expression, Class<T> clazz) {
        return parser.parseExpression(expression).getValue(evalContext, clazz);
    }
}
