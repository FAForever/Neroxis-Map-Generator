package com.faforever.neroxis.util.vector;

import com.faforever.neroxis.util.functional.FloatSupplier;
import com.faforever.neroxis.util.functional.FloatUnaryOperator;

import java.util.random.RandomGenerator;

public sealed interface Vector<T extends Vector<T>> permits Vector2, Vector3, Vector4 {
    int X = 0;
    int Y = 1;
    int Z = 2;
    int W = 3;
    int R = 0;
    int G = 1;
    int B = 2;
    int A = 3;

    int getDimension();

    float[] toArray();

    T transform(Transformer transformer);

    float get(int i);

    private void assertEqualDimension(int dimension) {
        int thisDimension = getDimension();
        if (dimension != thisDimension) {
            throw new IllegalArgumentException(
                    String.format("Dimensions do not match: This %d other %d", thisDimension, dimension));
        }
    }

    default T withComponent(int component, float value) {
        return transform(Transformer.matchingComponent(component, () -> value));
    }

    default T randomize(RandomGenerator random, float minValue, float maxValue) {
        return transform(Transformer.fromSupplier(() -> random.nextFloat(minValue, maxValue)));
    }

    default T randomize(RandomGenerator random, float scale) {
        return transform(Transformer.fromSupplier(() -> random.nextFloat(scale)));
    }

    default T max(float value) {
        return transform(Transformer.fromOldValue(oldValue -> StrictMath.max(value, oldValue)));
    }

    default T max(float... values) {
        assertEqualDimension(values.length);
        return transform((index, oldValue) -> StrictMath.max(values[index], oldValue));
    }

    default T max(T other) {
        return transform((index, oldValue) -> StrictMath.max(other.get(index), oldValue));
    }

    default T clampMin(float floor) {
        return transform(Transformer.fromOldValue(oldValue -> StrictMath.max(floor, oldValue)));
    }

    default T min(float value) {
        return transform(Transformer.fromOldValue(oldValue -> StrictMath.min(value, oldValue)));
    }

    default T min(float... values) {
        assertEqualDimension(values.length);
        return transform((index, oldValue) -> StrictMath.min(values[index], oldValue));
    }

    default T min(T other) {
        return transform((index, oldValue) -> StrictMath.min(other.get(index), oldValue));
    }

    default T clampMax(float ceiling) {
        return transform(Transformer.fromOldValue(oldValue -> StrictMath.min(ceiling, oldValue)));
    }

    default T round() {
        return transform(Transformer.fromOldValue(StrictMath::round));
    }

    default T round(int places) {
        float placesFactor = (float) StrictMath.pow(10, places);
        return transform(
                Transformer.fromOldValue(oldValue -> StrictMath.round(oldValue * placesFactor) / placesFactor));
    }

    default T floor() {
        return transform(Transformer.fromOldValue(oldValue -> (float) StrictMath.floor(oldValue)));
    }

    default T ceil() {
        return transform(Transformer.fromOldValue(oldValue -> (float) StrictMath.ceil(oldValue)));
    }

    default T normalize() {
        return divide(getMagnitude());
    }

    default T add(T other) {
        return transform((index, oldValue) -> oldValue + other.get(index));
    }

    default T add(float... values) {
        assertEqualDimension(values.length);
        return transform((index, oldValue) -> oldValue + values[index]);
    }

    default T add(float value) {
        return transform(Transformer.fromOldValue(oldValue -> oldValue + value));
    }

    default T add(float value, int component) {
        return transform(Transformer.matchingComponent(component, oldValue -> oldValue + value));
    }

    default T subtract(T other) {
        return transform((index, oldValue) -> oldValue - other.get(index));
    }

    default T subtract(float... values) {
        assertEqualDimension(values.length);
        return transform((index, oldValue) -> oldValue - values[index]);
    }

    default T subtract(float value) {
        return transform(Transformer.fromOldValue(oldValue -> oldValue - value));
    }

    default T subtract(float value, int component) {
        return transform(Transformer.matchingComponent(component, oldValue -> oldValue - value));
    }

    default T multiply(T other) {
        return transform((index, oldValue) -> oldValue * other.get(index));
    }

    default T multiply(float... values) {
        assertEqualDimension(values.length);
        return transform((index, oldValue) -> oldValue * values[index]);
    }

    default T multiply(float value) {
        return transform(Transformer.fromOldValue(oldValue -> oldValue * value));
    }

    default T multiply(float value, int component) {
        return transform(Transformer.matchingComponent(component, oldValue -> oldValue * value));
    }

    default T divide(T other) {
        return transform((index, oldValue) -> oldValue / other.get(index));
    }

    default T divide(float... values) {
        return transform((index, oldValue) -> oldValue / values[index]);
    }

    default T divide(float value) {
        return transform(Transformer.fromOldValue(oldValue -> oldValue / value));
    }

    default T divide(float value, int component) {
        return transform(Transformer.matchingComponent(component, oldValue -> oldValue / value));
    }

    default T roundToNearestHalfPoint() {
        return transform(Transformer.fromOldValue(oldValue -> StrictMath.round(oldValue - .5f) + .5f));
    }

    default float getMagnitude() {
        float sum = 0;
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            sum += get(i) * get(i);
        }
        return (float) StrictMath.sqrt(sum);
    }

    default float getDistance(T other) {
        return (float) StrictMath.sqrt(getDistanceSquared(other));
    }

    default float getDistanceSquared(T other) {
        float sum = 0;
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            float diff = get(i) - other.get(i);
            sum += diff * diff;
        }
        return sum;
    }

    default float getAngle(T other) {
        return (float) StrictMath.acos(dot(other) / getMagnitude() / other.getMagnitude());
    }

    default float dot(T other) {
        float sum = 0;
        int dimension = getDimension();
        for (int i = 0; i < dimension; ++i) {
            sum += get(i) * other.get(i);
        }
        return sum;
    }

    interface Transformer {
        float transform(int component, float currentValue);

        static Transformer fromOldValue(FloatUnaryOperator operator) {
            return (index, oldValue) -> operator.applyAsFloat(oldValue);
        }

        static Transformer fromSupplier(FloatSupplier supplier) {
            return (index, oldValue) -> supplier.getAsFloat();
        }

        static Transformer matchingComponent(int component, FloatUnaryOperator operator) {
            return (index, oldValue) -> index == component ? operator.applyAsFloat(oldValue) : oldValue;
        }

        static Transformer matchingComponent(int component, FloatSupplier supplier) {
            return (index, oldValue) -> index == component ? supplier.getAsFloat() : oldValue;
        }
    }
}
