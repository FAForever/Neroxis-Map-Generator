package com.faforever.neroxis.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Arrays;

@EqualsAndHashCode
@SuppressWarnings("unchecked")
public abstract strictfp class Vector<T extends Vector<T>> {
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;
    public static final int W = 3;
    public static final int R = 0;
    public static final int G = 1;
    public static final int B = 2;
    public static final int A = 3;

    protected final float[] components;
    @Getter
    private final int dimension;

    protected Vector(int dimension) {
        this.dimension = dimension;
        components = new float[dimension];
    }

    protected Vector(float... components) {
        this.components = components;
        dimension = components.length;
    }

    public abstract T copy();

    public float get(int i) {
        return components[i];
    }

    public void set(int i, float value) {
        components[i] = value;
    }

    public T max(T other) {
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.max(components[i], other.components[i]);
        }
        return (T) this;
    }

    public T clampMin(float floor) {
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.max(components[i], floor);
        }
        return (T) this;
    }

    public T min(T other) {
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.min(components[i], other.components[i]);
        }
        return (T) this;
    }

    public T clampMax(float ceiling) {
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.min(components[i], ceiling);
        }
        return (T) this;
    }

    public T round() {
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.round(components[i]);
        }
        return (T) this;
    }

    public T normalize() {
        float magnitude = getMagnitude();
        for (int i = 0; i < dimension; ++i) {
            components[i] /= magnitude;
        }
        return (T) this;
    }

    @JsonIgnore
    public float getMagnitude() {
        float sum = 0;
        for (int i = 0; i < dimension; ++i) {
            sum += components[i] * components[i];
        }
        return (float) StrictMath.sqrt(sum);
    }

    public T add(T other) {
        for (int i = 0; i < dimension; ++i) {
            components[i] += other.components[i];
        }
        return (T) this;
    }

    public T add(float... values) {
        assertEqualDimension(values.length);
        for (int i = 0; i < dimension; ++i) {
            components[i] += values[i];
        }
        return (T) this;
    }

    public T add(float value) {
        for (int i = 0; i < dimension; ++i) {
            components[i] += value;
        }
        return (T) this;
    }

    public T add(float value, int component) {
        components[component] += value;
        return (T) this;
    }

    public T subtract(T other) {
        for (int i = 0; i < dimension; ++i) {
            components[i] -= other.components[i];
        }
        return (T) this;
    }

    public T subtract(float... values) {
        assertEqualDimension(values.length);
        for (int i = 0; i < dimension; ++i) {
            components[i] -= values[i];
        }
        return (T) this;
    }

    public T subtract(float value) {
        for (int i = 0; i < dimension; ++i) {
            components[i] -= value;
        }
        return (T) this;
    }

    public T subtract(float value, int component) {
        components[component] -= value;
        return (T) this;
    }

    public T multiply(T other) {
        for (int i = 0; i < dimension; ++i) {
            components[i] *= other.components[i];
        }
        return (T) this;
    }

    public T multiply(float... values) {
        assertEqualDimension(values.length);
        for (int i = 0; i < dimension; ++i) {
            components[i] *= values[i];
        }
        return (T) this;
    }

    public T multiply(float value) {
        for (int i = 0; i < dimension; ++i) {
            components[i] *= value;
        }
        return (T) this;
    }

    public T multiply(float value, int component) {
        components[component] -= value;
        return (T) this;
    }

    public T divide(T other) {
        for (int i = 0; i < dimension; ++i) {
            components[i] /= other.components[i];
        }
        return (T) this;
    }

    public T divide(float... values) {
        assertEqualDimension(values.length);
        for (int i = 0; i < dimension; ++i) {
            components[i] /= values[i];
        }
        return (T) this;
    }

    public T divide(float value) {
        for (int i = 0; i < dimension; ++i) {
            components[i] /= value;
        }
        return (T) this;
    }

    public T divide(float value, int component) {
        components[component] -= value;
        return (T) this;
    }

    public float getDistance(T other) {
        float sum = 0;
        for (int i = 0; i < dimension; ++i) {
            float diff = components[i] - other.components[i];
            sum += diff * diff;
        }
        return (float) StrictMath.sqrt(sum);
    }

    public float dot(T other) {
        float sum = 0;
        for (int i = 0; i < dimension; ++i) {
            sum += components[i] * other.components[i];
        }
        return sum;
    }

    public float getAngle(T other) {
        return (float) StrictMath.acos(dot(other) / getMagnitude() / other.getMagnitude());
    }

    private void assertEqualDimension(int dimension) {
        if (dimension != this.dimension) {
            throw new IllegalArgumentException(String.format("Dimensions do not match: This %d other %d", this.dimension, dimension));
        }
    }

    public float[] toArray() {
        return components;
    }

    @Override
    public String toString() {
        return Arrays.toString(components).replace("[", "").replace("]", "");
    }
}
