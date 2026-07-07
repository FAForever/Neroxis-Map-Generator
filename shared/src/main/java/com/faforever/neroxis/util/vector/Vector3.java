package com.faforever.neroxis.util.vector;

import java.util.Arrays;
import java.util.LinkedHashSet;

public record Vector3(
        float x,
        float y,
        float z
) implements Vector<Vector3> {

    public Vector3 {}

    public Vector3() {
        this(0, 0, 0);
    }

    private Vector3(float... values) {
        this(values[0], values[1], values[2]);
    }

    public Vector3(Vector2 other) {
        this(other.x(), 0f, other.y());
    }

    @Override
    public float get(int i) {
        return switch (i) {
            case Vector.X -> x();
            case Vector.Y -> y();
            case Vector.Z -> z();
            default -> throw new UnsupportedOperationException("Unsupported component: " + i);
        };
    }

    @Override
    public Vector3 transform(Transformer transformer) {
        return new Vector3(transformer.transform(Vector.X, x()), transformer.transform(Vector.Y, y()),
                           transformer.transform(Vector.Z, z()));
    }

    @Override
    public int getDimension() {
        return 3;
    }

    @Override
    public float[] toArray() {
        return new float[]{x(), y(), z()};
    }

    public Vector3 cross(Vector3 other) {
        float x = x();
        float oX = other.x();
        float y = y();
        float oY = other.y();
        float z = z();
        float oZ = other.z();
        float newX = y * oZ - z * oY;
        float newY = z * oX - x * oZ;
        float newZ = x * oY - y * oX;
        return new Vector3(newX, newY, newZ);
    }

    public float getXZDistance(Vector2 location) {
        return getXZDistance(new Vector3(location));
    }

    public float getXZDistance(Vector3 location) {
        float dx = x() - location.x();
        float dz = z() - location.z();
        return (float) StrictMath.sqrt(dx * dx + dz * dz);
    }

    public LinkedHashSet<Vector2> getXZLine(Vector3 location) {
        LinkedHashSet<Vector2> line = new LinkedHashSet<>();
        Vector2 currentPoint = new Vector2(this);
        Vector2 targetPoint = new Vector2(location);
        while (currentPoint.getDistance(targetPoint) > 1) {
            line.add(currentPoint);
            float angle = currentPoint.angleTo(location);
            currentPoint = new Vector2(StrictMath.round(currentPoint.x() + StrictMath.cos(angle)),
                                       StrictMath.round(currentPoint.y() + StrictMath.sin(angle)));
        }
        return line;
    }

    public float getAzimuth() {
        return (float) StrictMath.atan2(z(), x());
    }

    public float getElevation() {
        return (float) StrictMath.atan2(y(), StrictMath.sqrt(x() * x() + z() * z()));
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
