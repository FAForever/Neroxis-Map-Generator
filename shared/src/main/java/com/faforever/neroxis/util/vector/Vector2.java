package com.faforever.neroxis.util.vector;

import com.faforever.neroxis.map.Symmetry;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Arrays;
import java.util.LinkedHashSet;

public record Vector2(float x, float y) implements Vector<Vector2> {

    public Vector2 {}

    public Vector2() {
        this(0, 0);
    }

    private Vector2(float... values) {
        this(values[0], values[1]);
    }

    public Vector2(Vector3 location) {
        this(location.x(), location.z());
    }

    public Vector2(Dimension other) {
        this((float) other.getWidth(), (float) other.getHeight());
    }

    public Vector2(Point other) {
        this((float) other.getX(), (float) other.getY());
    }

    @Override
    public float get(int i) {
        return switch (i) {
            case Vector.X -> x();
            case Vector.Y -> y();
            default -> throw new UnsupportedOperationException("Unsupported component: " + i);
        };
    }

    @Override
    public Vector2 transform(Transformer transformer) {
        return new Vector2(transformer.transform(Vector.X, x()), transformer.transform(Vector.Y, y()));
    }

    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    public float[] toArray() {
        return new float[]{x(), y()};
    }

    public float angleTo(Vector3 location) {
        return angleTo(new Vector2(location));
    }

    public float angleTo(Vector2 location) {
        float dx = location.x() - x();
        float dy = location.y() - y();
        return (float) StrictMath.atan2(dy, dx);
    }

    public LinkedHashSet<Vector2> getLine(Vector2 location) {
        LinkedHashSet<Vector2> line = new LinkedHashSet<>();
        Vector2 currentPoint = this;
        while (currentPoint.getDistance(location) > 1) {
            line.add(currentPoint);
            float angle = currentPoint.angleTo(location);
            currentPoint = new Vector2((float) (currentPoint.x() + StrictMath.cos(angle)),
                                       (float) (currentPoint.y() + StrictMath.sin(angle)));
        }
        return line;
    }

    public Vector2 addPolar(float angle, float magnitude) {
        return add((float) (magnitude * StrictMath.cos(angle)), (float) (magnitude * StrictMath.sin(angle)));
    }

    public Vector2 flip(Vector2 center, Symmetry symmetry) {
        return switch (symmetry) {
            case X -> new Vector2(2 * center.x() - x(), y);
            case Z -> new Vector2(x, 2 * center.y() - y());
            case XZ, ZX, POINT2 -> new Vector2(2 * center.x() - x(), 2 * center.y() - y());
            case POINT3, NONE, DIAG, QUAD, POINT16, POINT15, POINT14, POINT13, POINT12, POINT11, POINT10, POINT9,
                 POINT8, POINT7, POINT6, POINT5, POINT4 -> this;
        };
    }

    public Vector2 rotate(float angle) {
        float oldX = x();
        float oldY = y();
        float cos = (float) StrictMath.cos(angle);
        float sin = (float) StrictMath.sin(angle);
        return new Vector2(oldX * cos - oldY * sin, oldX * sin + oldY * cos);
    }

    @Override
    public String toString() {
        float[] values = toArray();
        String[] strings = new String[values.length];
        for (int i = 0; i < values.length; ++i) {
            strings[i] = String.format("%9f", get(i));
        }
        return Arrays.toString(strings).replace("[", "").replace("]", "");
    }
}
