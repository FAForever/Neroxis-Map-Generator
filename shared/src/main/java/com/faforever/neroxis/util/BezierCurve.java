package com.faforever.neroxis.util;

import com.faforever.neroxis.util.vector.Vector2;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@EqualsAndHashCode
public strictfp class BezierCurve {
    private static final Map<Integer, Integer[]> COEFFICIENTS_MAP = new HashMap<>();

    protected final Vector2[] controlPoints;
    protected final Integer[] coefficients;
    @Getter
    private final int order;

    public BezierCurve(int order, long seed) {
        this(new Vector2[order + 1]);
        Random random = new Random(seed);
        controlPoints[0] = new Vector2(-.5f, 0);
        controlPoints[order] = new Vector2(.5f, 0);
        for (int i = 1; i < order; ++i) {
            controlPoints[i] = new Vector2().randomize(random, -1f, 1f);
        }
    }

    public BezierCurve(Vector2... controlPoints) {
        this.controlPoints = controlPoints;
        order = controlPoints.length - 1;
        coefficients = COEFFICIENTS_MAP.computeIfAbsent(order, BezierCurve::computeCoefficients);
    }

    private static Integer[] computeCoefficients(int order) {
        int numControlPoints = order + 1;
        Integer[] coefficients = new Integer[numControlPoints];
        for (int i = 0; i < numControlPoints; ++i) {
            coefficients[i] = MathUtil.binomialCoefficient(order, i);
        }
        return coefficients;
    }

    public Vector2 getStart() {
        return controlPoints[0];
    }

    public Vector2 getEnd() {
        return controlPoints[controlPoints.length - 1];
    }

    public Vector2 getPoint(float t) {
        if (t < 0 || t > 1) {
            throw new IllegalArgumentException("t must be between 0 and 1");
        }
        Vector2 pointOnCurve = new Vector2();
        for (int i = 0; i < controlPoints.length; ++i) {
            pointOnCurve.add(controlPoints[i].copy().multiply((float) (coefficients[i] * StrictMath.pow((1 - t), (order - i)) * StrictMath.pow(t, i))));
        }
        return pointOnCurve;
    }

    public BezierCurve transformTo(Vector2 newStart, Vector2 newEnd) {
        Vector2 currentStart = getStart();
        Vector2 currentEnd = getEnd();
        float distanceRatio = newStart.getDistance(newEnd) / currentStart.getDistance(currentEnd);
        float angleDifference = newStart.angleTo(newEnd) - currentStart.angleTo(currentEnd);
        Vector2 positionDifference = newStart.copy().subtract(currentStart);
        return rotate(angleDifference).scale(distanceRatio).translate(positionDifference);
    }

    private BezierCurve rotate(float angle) {
        Vector2 origin = controlPoints[0].copy();
        for (Vector2 controlPoint : controlPoints) {
            controlPoint.subtract(origin).rotate(angle).add(origin);
        }
        return this;
    }

    private BezierCurve scale(float factor) {
        Vector2 origin = controlPoints[0].copy();
        for (Vector2 controlPoint : controlPoints) {
            controlPoint.subtract(origin).multiply(factor).add(origin);
        }
        return this;
    }

    private BezierCurve translate(Vector2 translationVector) {
        return translate(translationVector.getX(), translationVector.getY());
    }

    private BezierCurve translate(float xTranslation, float yTranslation) {
        for (Vector2 controlPoint : controlPoints) {
            controlPoint.add(xTranslation, yTranslation);
        }
        return this;
    }

    @Override
    public String toString() {
        String[] strings = new String[controlPoints.length];
        for (int i = 0; i < controlPoints.length; ++i) {
            strings[i] = "(" + controlPoints[i].toString() + ")";
        }
        return Arrays.toString(strings).replace("[", "").replace("]", "");
    }
}
