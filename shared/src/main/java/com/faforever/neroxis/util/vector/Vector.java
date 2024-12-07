package com.faforever.neroxis.util.vector;

import com.faforever.neroxis.util.functional.FloatConsumer;
import com.faforever.neroxis.util.functional.FloatSupplier;
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

    protected abstract FloatSupplier getComponentGetter(int i);

    protected abstract FloatConsumer getComponentSetter(int i);

    public abstract T copy();

    public abstract int getDimension();

    public abstract float[] toArray();

    public float get(int i) {
        return getComponentGetter(i).getAsFloat();
    }

    public void set(int i, float value) {
        getComponentSetter(i).accept(value);
    }

    public void set(T other) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; i++) {
            set(i, other.get(i));
        }
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
            set(i, random.nextFloat() * range + minValue);
        }
        return (T) this;
    }

    public T randomize(Random random, float scale) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, random.nextFloat() * scale);
        }
        return (T) this;
    }

    public T max(float value) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, StrictMath.max(get(i), value));
        }
        return (T) this;
    }

    public T max(float... values) {
        assertEqualDimension(values.length);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, StrictMath.max(get(i), values[i]));
        }
        return (T) this;
    }
    
    public T max(T other) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, StrictMath.max(get(i), other.get(i)));
        }
        return (T) this;
    }

    public T clampMin(float floor) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, StrictMath.max(get(i), floor));
        }
        return (T) this;
    }

    public T min(float value) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, StrictMath.min(get(i), value));
        }
        return (T) this;
    }

    public T min(float... values) {
        assertEqualDimension(values.length);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, StrictMath.min(get(i), values[i]));
        }
        return (T) this;
    }

    public T min(T other) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, StrictMath.min(get(i), other.get(i)));
        }
        return (T) this;
    }

    public T clampMax(float ceiling) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, StrictMath.min(get(i), ceiling));
        }
        return (T) this;
    }

    public T round() {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, StrictMath.round(get(i)));
        }
        return (T) this;
    }

    public T round(int places) {
        float magnitude = (float) StrictMath.pow(10, places);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, StrictMath.round(get(i) * magnitude) / magnitude);
        }
        return (T) this;
    }

    public T floor() {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, (float) StrictMath.floor(get(i)));
        }
        return (T) this;
    }

    public T ceil() {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, (float) StrictMath.ceil(get(i)));
        }
        return (T) this;
    }

    public T normalize() {
        float magnitude = getMagnitude();
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) / magnitude);
        }
        return (T) this;
    }

    public float getMagnitude() {
        float sum = 0;
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            sum += get(i) * get(i);
        }
        return (float) StrictMath.sqrt(sum);
    }

    public T add(T other) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) + other.get(i));
        }
        return (T) this;
    }

    public T add(float... values) {
        assertEqualDimension(values.length);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) + values[i]);
        }
        return (T) this;
    }

    public T add(float value) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) + value);
        }
        return (T) this;
    }

    public T add(float value, int component) {
        set(component, get(component) + value);
        return (T) this;
    }

    public T subtract(T other) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) - other.get(i));
        }
        return (T) this;
    }

    public T subtract(float... values) {
        assertEqualDimension(values.length);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) - values[i]);
        }
        return (T) this;
    }

    public T subtract(float value) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) - value);
        }
        return (T) this;
    }

    public T subtract(float value, int component) {
        set(component, get(component) - value);
        return (T) this;
    }

    public T multiply(T other) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) * other.get(i));
        }
        return (T) this;
    }

    public T multiply(float... values) {
        assertEqualDimension(values.length);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) * values[i]);
        }
        return (T) this;
    }

    public T multiply(float value) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) * value);
        }
        return (T) this;
    }

    public T multiply(float value, int component) {
        set(component, get(component) * value);
        return (T) this;
    }

    public T divide(T other) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) / other.get(i));
        }
        return (T) this;
    }

    public T divide(float... values) {
        assertEqualDimension(values.length);
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) / values[i]);
        }
        return (T) this;
    }

    public T divide(float value) {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, get(i) / value);
        }
        return (T) this;
    }

    public T divide(float value, int component) {
        set(component, get(component) / value);
        return (T) this;
    }

    public float getDistance(T other) {
        float sum = 0;
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            float diff = get(i) - other.get(i);
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
            sum += get(i) * other.get(i);
        }
        return sum;
    }

    public T roundToNearestHalfPoint() {
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            set(i, StrictMath.round(get(i) - .5f) + .5f);
        }
        return (T) this;
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
