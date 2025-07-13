package com.faforever.neroxis.util.vector;

import io.avaje.jsonb.Json;

import java.util.Arrays;

@Json
public record Vector4(float x, float y, float z, float w) implements Vector<Vector4> {

    @Json.Creator
    public Vector4 {}

    public Vector4() {
        this(0, 0, 0, 0);
    }

    private Vector4(float... values) {
        this(values[0], values[1], values[2], values[3]);
    }

    @Override
    public float get(int i) {
        return switch (i) {
            case Vector.X -> x();
            case Vector.Y -> y();
            case Vector.Z -> z();
            case Vector.W -> w();
            default -> throw new UnsupportedOperationException("Unsupported component: " + i);
        };
    }

    @Override
    public Vector4 transform(Transformer transformer) {
        return new Vector4(transformer.transform(Vector.X, x()), transformer.transform(Vector.Y, y()),
                           transformer.transform(Vector.Z, z()), transformer.transform(Vector.W, w()));
    }

    @Override
    public int getDimension() {
        return 4;
    }

    @Override
    public float[] toArray() {
        return new float[]{x(), y(), z(), w()};
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
