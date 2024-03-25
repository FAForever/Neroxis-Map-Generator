package com.faforever.neroxis.util;

import java.util.random.RandomGenerator;

public record Range(float min, float max) {

    public Range {
        if (max < min) {
            throw new IllegalArgumentException(String.format("Max %f greater than Min %f", max, min));
        }
    }

    public static Range of(float min, float max) {
        return new Range(min, max);
    }

    public boolean contains(float value) {
        return value >= min && value <= max;
    }

    public float normalize(float value) {
        return StrictMath.max(StrictMath.min((value - min) / (max - min), 1f), 0f);
    }

    public float map(float value) {
        return StrictMath.max(StrictMath.min(value * (max - min) + min, max), min);
    }

    public float getRandomFloat(RandomGenerator random) {
        return random.nextFloat() * (max - min) + min;
    }
}
