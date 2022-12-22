package com.faforever.neroxis.util.vector;

import lombok.EqualsAndHashCode;

import java.util.Arrays;
import java.util.Random;

@EqualsAndHashCode
@SuppressWarnings("unchecked")
public abstract class Vector<T extends Vector<T>> {
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;
    public static final int W = 3;
    public static final int R = 0;
    public static final int G = 1;
    public static final int B = 2;
    public static final int A = 3;
    protected final float[] components;

    protected Vector(int dimension) {
        this(new float[dimension]);
    }

    protected Vector(float... components) {
        this.components = components;
    }

    public abstract T copy();

    public float get(int i) {
        return components[i];
    }

    public void set(int i, float value) {
        components[i] = value;
    }

    public void set(T other) {
        System.arraycopy(other.components, 0, components, 0, getDimension());
    }

    private void assertEqualDimension(int dimension) {
        int thisDimension = getDimension();
        if (dimension != thisDimension) {
            throw new IllegalArgumentException(
                    String.format("Dimensions do not match: This %d other %d", thisDimension, dimension));
        }
    }

    public T randomize(Random random, float minValue, float maxValue) {
        float range = maxValue - minValue;
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = random.nextFloat() * range + minValue;
        }
        return (T) this;
    }

    public T randomize(Random random, float scale) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = random.nextFloat() * scale;
        }
        return (T) this;
    }

    public T max(float value) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.max(components[i], value);
        }
        return (T) this;
    }

    public T max(float... values) {
        assertEqualDimension(values.length);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.max(components[i], values[i]);
        }
        return (T) this;
    }

    public int getDimension() {
        return components.length;
    }

    public T max(T other) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.max(components[i], other.components[i]);
        }
        return (T) this;
    }

    public T clampMin(float floor) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.max(components[i], floor);
        }
        return (T) this;
    }

    public T min(float value) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.min(components[i], value);
        }
        return (T) this;
    }

    public T min(float... values) {
        assertEqualDimension(values.length);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.min(components[i], values[i]);
        }
        return (T) this;
    }

    public T min(T other) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.min(components[i], other.components[i]);
        }
        return (T) this;
    }

    public T clampMax(float ceiling) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.min(components[i], ceiling);
        }
        return (T) this;
    }

    public T round() {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.round(components[i]);
        }
        return (T) this;
    }

    public T floor() {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = (float) StrictMath.floor(components[i]);
        }
        return (T) this;
    }

    public T ceil() {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = (float) StrictMath.ceil(components[i]);
        }
        return (T) this;
    }

    public T normalize() {
        float magnitude = getMagnitude();
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] /= magnitude;
        }
        return (T) this;
    }

    public float getMagnitude() {
        float sum = 0;
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            sum += components[i] * components[i];
        }
        return (float) StrictMath.sqrt(sum);
    }

    public T add(T other) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] += other.components[i];
        }
        return (T) this;
    }

    public T add(float... values) {
        assertEqualDimension(values.length);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] += values[i];
        }
        return (T) this;
    }

    public T add(float value) {
        int dimension = getDimension();
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
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] -= other.components[i];
        }
        return (T) this;
    }

    public T subtract(float... values) {
        assertEqualDimension(values.length);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] -= values[i];
        }
        return (T) this;
    }

    public T subtract(float value) {
        int dimension = getDimension();
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
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] *= other.components[i];
        }
        return (T) this;
    }

    public T multiply(float... values) {
        assertEqualDimension(values.length);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] *= values[i];
        }
        return (T) this;
    }

    public T multiply(float value) {
        int dimension = getDimension();
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
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] /= other.components[i];
        }
        return (T) this;
    }

    public T divide(float... values) {
        assertEqualDimension(values.length);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] /= values[i];
        }
        return (T) this;
    }

    public T divide(float value) {
        int dimension = getDimension();
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
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            float diff = components[i] - other.components[i];
            sum += diff * diff;
        }
        return (float) StrictMath.sqrt(sum);
    }

    public float getAngle(T other) {
        return (float) StrictMath.acos(dot(other) / getMagnitude() / other.getMagnitude());
    }

    public float dot(T other) {
        float sum = 0;
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            sum += components[i] * other.components[i];
        }
        return sum;
    }

    public T roundToNearestHalfPoint() {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            components[i] = StrictMath.round(components[i] - .5f) + .5f;
        }
        return (T) this;
    }

    public float[] toArray() {
        return components;
    }

    @Override
    public String toString() {
        String[] strings = new String[components.length];
        for (int i = 0; i < components.length; ++i) {
            strings[i] = String.format("%9f", components[i]);
        }
        return Arrays.toString(strings).replace("[", "").replace("]", "");
    }
}
